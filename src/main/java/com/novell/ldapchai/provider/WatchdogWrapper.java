/*
 * LDAP Chai API
 * Copyright (c) 2006-2010 Novell, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.novell.ldapchai.provider;

import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiException;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.ChaiLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link ChaiProvider} implementation wrapper that handles automatic idle disconnects.
 *
 * @author Jason D. Rivard
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_ENABLE
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_IDLE_TIMEOUT
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_OPERATION_TIMEOUT
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_CHECK_FREQUENCY
 */
class WatchdogWrapper implements InvocationHandler
{

    private enum STATUS
    {
        ACTIVE,
        IDLE,
        CLOSED,
    }

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( WatchdogWrapper.class );

    private final WatchdogManager watchdogManager = new WatchdogManager();

    // the real provider and it's associated configuration
    private volatile ChaiProvider realProvider;
    private final ChaiProvider originalProvider;
    private final ChaiConfiguration originalProviderConfig;

    private static class Settings
    {
        private long watchdogFrequency = Integer.parseInt( ChaiSetting.WATCHDOG_CHECK_FREQUENCY.getDefaultValue() );
        private int operationTimeout = Integer.parseInt( ChaiSetting.WATCHDOG_OPERATION_TIMEOUT.getDefaultValue() );
        private int idleTimeout = Integer.parseInt( ChaiSetting.WATCHDOG_IDLE_TIMEOUT.getDefaultValue() );

        private Settings()
        {
        }

        private Settings( final long watchdogFrequency, final int operationTimeout, final int idleTimeout )
        {
            this.watchdogFrequency = watchdogFrequency;
            this.operationTimeout = operationTimeout;
            this.idleTimeout = idleTimeout;
        }

        public long getWatchdogFrequency()
        {
            return watchdogFrequency;
        }

        public int getOperationTimeout()
        {
            return operationTimeout;
        }

        public int getIdleTimeout()
        {
            return idleTimeout;
        }

        public void setIdleTimeout( final int idleTimeout )
        {
            this.idleTimeout = idleTimeout;
        }
    }

    private Settings settings = new Settings();

    /**
     * number of outsanding ldap operations.  If the value is non-zero, then the provider is considered "in-use"
     */
    private final AtomicInteger outstandingOperations = new AtomicInteger( 0 );

    /**
     * last time an ldap operation was initiated
     */
    private volatile long lastBeginTimestamp = System.currentTimeMillis();

    /**
     * last time an ldap operation was completed
     */
    private volatile long lastFinishTimestamp = System.currentTimeMillis();

    /**
     * current wdStatus of this WatchdogWrapper
     */
    private volatile STATUS wdStatus = STATUS.ACTIVE;

    /**
     * Wrap a pre-existing ChaiProvider with a WatchdogWrapper instance.
     *
     * @param chaiProvider a pre-existing {@code ChaiProvider}
     * @return a wrapped {@code ChaiProvider} instance.
     */
    static ChaiProviderImplementor forProvider(
            final ChaiProviderImplementor chaiProvider
    )
    {
        //check to make sure watchdog ise enabled;
        final boolean watchDogEnabled = Boolean.parseBoolean( chaiProvider.getChaiConfiguration().getSetting( ChaiSetting.WATCHDOG_ENABLE ) );
        if ( !watchDogEnabled )
        {
            final String errorStr = "attempt to obtain WatchdogWrapper wrapper when watchdog is not enabled in chai config";
            LOGGER.warn( errorStr );
            throw new IllegalStateException( errorStr );
        }

        if ( Proxy.isProxyClass( chaiProvider.getClass() ) && chaiProvider instanceof WatchdogWrapper )
        {
            LOGGER.warn( "attempt to obtain WatchdogWrapper wrapper for already wrapped Provider." );
            return chaiProvider;
        }

        return ( ChaiProviderImplementor ) Proxy.newProxyInstance(
                chaiProvider.getClass().getClassLoader(),
                chaiProvider.getClass().getInterfaces(),
                new WatchdogWrapper( chaiProvider ) );
    }


    private WatchdogWrapper(
            final ChaiProviderImplementor realProvider
    )
    {
        this.originalProvider = realProvider;
        this.realProvider = realProvider;
        this.originalProviderConfig = realProvider.getChaiConfiguration();

        {
            final int watchdogFrequency = Integer.parseInt( originalProviderConfig.getSetting( ChaiSetting.WATCHDOG_CHECK_FREQUENCY ) );
            final int operationTimeout = Integer.parseInt( originalProviderConfig.getSetting( ChaiSetting.WATCHDOG_OPERATION_TIMEOUT ) );
            final int idleTimeout = Integer.parseInt( originalProviderConfig.getSetting( ChaiSetting.WATCHDOG_IDLE_TIMEOUT ) );
            settings = new Settings( watchdogFrequency, operationTimeout, idleTimeout );
        }

        watchdogManager.registerInstance( this );

        checkForPwExpiration();
    }

    private void checkForPwExpiration()

    {
        final boolean doPwExpCheck = realProvider.getChaiConfiguration().getBooleanSetting( ChaiSetting.WATCHDOG_DISABLE_IF_PW_EXPIRED );
        if ( !doPwExpCheck )
        {
            return;
        }

        LOGGER.trace( "checking for user password expiration to adjust watchdog timeout" );

        boolean userPwExpired;
        try
        {
            final String bindUserDN = realProvider.getChaiConfiguration().getSetting( ChaiSetting.BIND_DN );
            final ChaiUser bindUser = realProvider.getEntryFactory().createChaiUser( bindUserDN );
            userPwExpired = bindUser.isPasswordExpired();
        }
        catch ( ChaiException e )
        {
            LOGGER.error( "unexpected error attempting to read user password expiration value during watchdog initialization, will assume expiration, error: " + e.getMessage() );
            userPwExpired = true;
        }

        if ( userPwExpired )
        {
            LOGGER.info( "connection user account password is currently expired.  Disabling watchdog timeout." );
            settings.setIdleTimeout( Integer.MAX_VALUE );
        }
        else
        {
            settings.setIdleTimeout( realProvider.getChaiConfiguration().getIntSetting( ChaiSetting.WATCHDOG_IDLE_TIMEOUT ) );
        }
    }

    @SuppressWarnings( value = "NoFinalizer" )
    protected void finalize()
            throws Throwable
    {
        super.finalize();

        //safegaurd, this should be done in #handleClient
        watchdogManager.deRegisterInstance( this );
    }

    public Object invoke(
            final Object proxy,
            final Method method,
            final Object[] args )
            throws Throwable
    {
        // before performing any operation, check to see what the current watchdog wdStatus is.
        if ( method.getName().equals( "close" ) )
        {
            handleClientCloseRequest();
            return Void.TYPE;
        }

        if ( method.getName().equals( "isConnected" ) )
        {
            if ( wdStatus != STATUS.ACTIVE )
            {
                return false;
            }
        }


        final boolean isLdap = method.getAnnotation( ChaiProviderImplementor.LdapOperation.class ) != null;

        if ( wdStatus == STATUS.CLOSED || !isLdap )
        {
            return AbstractWrapper.invoker( originalProvider, method, args );
        }

        final Object returnObject;
        try
        {
            outstandingOperations.incrementAndGet();
            lastBeginTimestamp = System.currentTimeMillis();

            if ( wdStatus == STATUS.IDLE )
            {
                reopenRealProvider();
                lastBeginTimestamp = System.currentTimeMillis();
            }

            returnObject = AbstractWrapper.invoker( realProvider, method, args );
        }
        catch ( Exception e )
        {
            if ( e instanceof ChaiOperationException )
            {
                throw ( ChaiOperationException ) e;
            }
            else if ( e instanceof ChaiUnavailableException )
            {
                throw ( ChaiUnavailableException ) e;
            }
            else
            {
                LOGGER.warn( "unexpected chai api error", e );
                throw new IllegalStateException( e.getMessage(), e );
            }
        }
        finally
        {
            outstandingOperations.decrementAndGet();
            lastFinishTimestamp = System.currentTimeMillis();
        }


        if ( "setPassword".equals( method.getName() ) )
        {
            checkForPwExpiration();
        }

        return returnObject;
    }

    private synchronized void checkStatus()
    {
        if ( wdStatus == STATUS.ACTIVE )
        {
            // if current wdStatus us normal, then check to see if timed out
            if ( outstandingOperations.get() > 0 )
            {
                // check for operation timeout if we have outstanding
                final long operationDuration = System.currentTimeMillis() - lastBeginTimestamp;
                if ( operationDuration > settings.getOperationTimeout() )
                {
                    handleOperationTimeout();
                }
            }
            else
            {
                final long idleDuration = System.currentTimeMillis() - lastFinishTimestamp;
                if ( idleDuration > settings.getIdleTimeout() )
                {
                    handleIdleTimeout();
                }
            }
        }
    }

    private synchronized void handleOperationTimeout()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ldap operation timeout detected, discarding questionable connection" );
        if ( realProvider != null )
        {
            sb.append( " for " );
            sb.append( realProvider.toString() );
        }
        LOGGER.warn( sb.toString() );
        synchronized ( this )
        {
            if ( realProvider != null )
            {
                this.realProvider.close();
            }
            wdStatus = STATUS.IDLE;
            watchdogManager.deRegisterInstance( this );
        }
    }

    private synchronized void handleIdleTimeout()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ldap idle timeout detected, closing ldap connection" );
        if ( realProvider != null )
        {
            sb.append( " for " );
            sb.append( realProvider.toString() );
        }

        LOGGER.debug( sb.toString() );
        synchronized ( this )
        {
            if ( realProvider != null )
            {
                this.realProvider.close();
            }
            wdStatus = STATUS.IDLE;
            watchdogManager.deRegisterInstance( this );
        }
    }

    private synchronized void handleClientCloseRequest()
    {
        wdStatus = STATUS.CLOSED;
        if ( realProvider != null )
        {
            realProvider.close();
        }
        watchdogManager.deRegisterInstance( this );
    }

    private synchronized void reopenRealProvider()
            throws Exception
    {
        if ( wdStatus != STATUS.IDLE )
        {
            return;
        }

        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "reopening ldap connection for " );
            sb.append( originalProviderConfig.getSetting( ChaiSetting.BIND_DN ) );
            LOGGER.debug( sb.toString() );
        }

        // if old provider exists, try to close it first.
        if ( realProvider != null )
        {
            try
            {
                realProvider.close();
            }
            catch ( Exception e )
            {
                final StringBuilder sb = new StringBuilder();
                sb.append( "error during pre-close connection for " );
                sb.append( originalProviderConfig.getSetting( ChaiSetting.BIND_DN ) );
                LOGGER.debug( sb.toString() );
            }
            finally
            {
                realProvider = null;
            }
        }

        try
        {
            realProvider = realProvider.getProviderFactory().newProvider( originalProviderConfig );
        }
        catch ( Exception e )
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "error reopening ldap connection for " );
            sb.append( originalProviderConfig.getSetting( ChaiSetting.BIND_DN ) );
            sb.append( " " );
            sb.append( e.toString() );
            LOGGER.debug( sb.toString() );
            throw e;
        }

        lastBeginTimestamp = System.currentTimeMillis();
        wdStatus = STATUS.ACTIVE;
        watchdogManager.registerInstance( this );
    }

    private class WatchdogManager
    {
        private static final String THREAD_NAME = "LDAP Chai WatchdogWrapper timer thread";

        private final Map<WatchdogWrapper, Object> activeWrappers = new ConcurrentHashMap<>();

        final Object filler = new Object();
        final Lock lock = new ReentrantLock();

        /**
         * timer instance used to watch all the outstanding providers
         */
        private volatile Timer watchDogTimer = null;

        private void registerInstance( final WatchdogWrapper wdWrapper )
        {
            activeWrappers.put( wdWrapper, filler );
            checkTimer();
        }

        private void deRegisterInstance( final WatchdogWrapper wdWrapper )
        {
            activeWrappers.remove( wdWrapper );
        }

        /**
         * Regulate the timer.  This is important because the timer task creates its own thread,
         * and if the task isn't cleaned up, there could be a thread leak.
         */
        private void checkTimer()
        {
            try
            {
                lock.lock();

                if ( watchDogTimer == null )
                {
                    // if there is NOT an active timer
                    if ( !activeWrappers.isEmpty() )
                    {
                        // if there are active providers.
                        LOGGER.debug( "starting up " + THREAD_NAME + ", " + settings.getWatchdogFrequency() + "ms check frequency" );

                        // create a new timer
                        watchDogTimer = new Timer( THREAD_NAME, true );
                        watchDogTimer.schedule( new WatchdogTask(), settings.getWatchdogFrequency(), settings.getWatchdogFrequency() );
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
        }

        private void checkProvider( final WatchdogWrapper wdWrapper )
        {
            try
            {
                if ( wdWrapper != null )
                {
                    wdWrapper.checkStatus();
                }
            }
            catch ( Exception e )
            {
                final StringBuilder sb = new StringBuilder();
                sb.append( "error during watchdog provider idle check: " );
                sb.append( e.getMessage() );
                if ( wdWrapper.realProvider != null )
                {
                    sb.append( " for " );
                    sb.append( wdWrapper.toString() );
                }
                LOGGER.warn( sb );
            }
        }

        private class WatchdogTask extends TimerTask implements Runnable
        {
            public void run()
            {
                final Collection<WatchdogWrapper> copyCollection = new HashSet<WatchdogWrapper>();
                copyCollection.addAll( activeWrappers.keySet() );

                for ( final WatchdogWrapper wdWrapper : copyCollection )
                {
                    try
                    {
                        checkProvider( wdWrapper );
                    }
                    catch ( Throwable e )
                    {
                        LOGGER.error( "error during watchdog timer check: " + e.getMessage() );
                    }
                }

                if ( copyCollection.isEmpty() )
                {
                    // if there are no active providers
                    LOGGER.debug( "exiting " + THREAD_NAME + ", no connections requiring monitoring are in use" );
                    try
                    {
                        lock.lock();

                        // kill the timer.
                        watchDogTimer.cancel();
                        watchDogTimer = null;
                    }
                    finally
                    {
                        lock.unlock();
                    }
                }

            }
        }
    }

    public boolean isIdle()
    {
        return STATUS.IDLE.equals( wdStatus );
    }
}
