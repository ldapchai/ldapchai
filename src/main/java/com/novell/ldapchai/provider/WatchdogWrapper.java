/*
 * LDAP Chai API
 * Copyright (c) 2006-2017 Novell, Inc.
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
import java.time.Instant;
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

    private static final AtomicInteger ID_COUNTER = new AtomicInteger( 0 );
    private final int counter = ID_COUNTER.getAndIncrement();

    // the real provider and it's associated configuration
    private volatile ChaiProviderImplementor realProvider;

    private final ChaiConfiguration originalProviderConfig;
    private final ChaiProviderFactory chaiProviderFactory;
    private final WatchdogService watchdogService;

    private final Lock statusChangeLock = new ReentrantLock( );

    private boolean allowDisconnect;

    static class Settings
    {
        private int operationTimeout = Integer.parseInt( ChaiSetting.WATCHDOG_OPERATION_TIMEOUT.getDefaultValue() );
        private int idleTimeout = Integer.parseInt( ChaiSetting.WATCHDOG_IDLE_TIMEOUT.getDefaultValue() );

        private Settings()
        {
        }

        private Settings( final int operationTimeout, final int idleTimeout )
        {
            this.operationTimeout = operationTimeout;
            this.idleTimeout = idleTimeout;
        }

        public int getOperationTimeout()
        {
            return operationTimeout;
        }

        public int getIdleTimeout()
        {
            return idleTimeout;
        }
    }

    private Settings settings = new Settings();

    /**
     * number of outstanding ldap operations.  If the value is non-zero, then the provider is considered "in-use"
     */
    private final AtomicInteger outstandingOperations = new AtomicInteger( 0 );

    /**
     * last time an ldap operation was initiated.
     */
    private volatile Instant lastBeginTime = Instant.now();

    /**
     * last time an ldap operation was completed.
     */
    private volatile Instant lastFinishTime = Instant.now();

    /**
     * current wdStatus of this WatchdogWrapper.
     */
    private volatile STATUS wdStatus = STATUS.ACTIVE;

    /**
     * Wrap a pre-existing ChaiProvider with a WatchdogWrapper instance.
     *
     * @param chaiProviderFactory the factory used to create the provider
     * @param chaiProvider a pre-existing {@code ChaiProvider}
     * @return a wrapped {@code ChaiProvider} instance.
     */
    static ChaiProviderImplementor forProvider(
            final ChaiProviderFactory chaiProviderFactory,
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
                new WatchdogWrapper( chaiProvider, chaiProviderFactory ) );
    }


    private WatchdogWrapper(
            final ChaiProviderImplementor realProvider,
            final ChaiProviderFactory chaiProviderFactory
    )
    {
        this.realProvider = realProvider;
        this.originalProviderConfig = realProvider.getChaiConfiguration();

        {
            final int operationTimeout = Integer.parseInt( originalProviderConfig.getSetting( ChaiSetting.WATCHDOG_OPERATION_TIMEOUT ) );
            final int idleTimeout = Integer.parseInt( originalProviderConfig.getSetting( ChaiSetting.WATCHDOG_IDLE_TIMEOUT ) );
            settings = new Settings( operationTimeout, idleTimeout );
        }

        this.chaiProviderFactory = chaiProviderFactory;
        this.watchdogService = chaiProviderFactory.getCentralService().getWatchdogService();
        watchdogService.registerInstance( this );

        allowDisconnect = !checkForPwExpiration( realProvider );
    }

    private static boolean checkForPwExpiration(
            final ChaiProvider chaiProvider
    )

    {
        final boolean doPwExpCheck = chaiProvider.getChaiConfiguration().getBooleanSetting( ChaiSetting.WATCHDOG_DISABLE_IF_PW_EXPIRED );
        if ( !doPwExpCheck )
        {
            return false;
        }

        LOGGER.trace( "checking for user password expiration to adjust watchdog timeout" );

        boolean userPwExpired;
        try
        {
            final String bindUserDN = chaiProvider.getChaiConfiguration().getSetting( ChaiSetting.BIND_DN );
            final ChaiUser bindUser = chaiProvider.getEntryFactory().newChaiUser( bindUserDN );
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
            return true;
        }

        return false;
    }

    public Object invoke(
            final Object proxy,
            final Method method,
            final Object[] args
    )
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

        if ( method.getName().equals( "getProviderFactory" ) )
        {
            return chaiProviderFactory;
        }

        if ( method.getName().equals( "getIdentifier" ) )
        {
            return getIdentifier();
        }

        final Object returnObject;
        try
        {
            outstandingOperations.incrementAndGet();
            lastBeginTime = Instant.now();

            if ( wdStatus == STATUS.IDLE )
            {
                reopenRealProvider();
                lastBeginTime = Instant.now();
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
            lastFinishTime = Instant.now();
        }


        if ( "setPassword".equals( method.getName() ) )
        {
            allowDisconnect = !checkForPwExpiration( realProvider );
        }

        return returnObject;
    }

    void checkStatus()
    {
        try
        {
            statusChangeLock.lock();

            if ( wdStatus == STATUS.ACTIVE )
            {
                // if current wdStatus us normal, then check to see if timed out
                if ( outstandingOperations.get() > 0 )
                {
                    // check for operation timeout if we have outstanding
                    final long operationDuration = Instant.now().toEpochMilli() - lastBeginTime.toEpochMilli();
                    if ( operationDuration > settings.getOperationTimeout() )
                    {
                        handleOperationTimeout();
                    }
                }
                else
                {
                    final long idleDuration = Instant.now().toEpochMilli() - lastFinishTime.toEpochMilli();
                    if ( idleDuration > settings.getIdleTimeout() )
                    {
                        handleIdleTimeout();
                    }
                }
            }
        }
        finally
        {
            statusChangeLock.unlock();
        }
    }

    private void handleOperationTimeout()
    {
        if ( !allowDisconnect )
        {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append( "ldap operation timeout detected, discarding questionable connection" );
        if ( realProvider != null )
        {
            sb.append( " for " );
            sb.append( realProvider.toString() );
        }
        LOGGER.warn( sb.toString() );

        if ( realProvider != null )
        {
            this.realProvider.close();
        }
        wdStatus = STATUS.IDLE;
        watchdogService.deRegisterInstance( this );
    }

    private void handleIdleTimeout()
    {
        if ( !allowDisconnect )
        {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append( "ldap idle timeout detected, closing ldap connection" );
        if ( realProvider != null )
        {
            sb.append( " for " );
            sb.append( realProvider.toString() );
        }

        LOGGER.debug( sb.toString() );

        if ( realProvider != null )
        {
            this.realProvider.close();
        }
        wdStatus = STATUS.IDLE;
        watchdogService.deRegisterInstance( this );
    }

    private void handleClientCloseRequest()
    {
        try
        {
            statusChangeLock.lock();

            wdStatus = STATUS.CLOSED;
            if ( realProvider != null )
            {
                realProvider.close();
            }
            watchdogService.deRegisterInstance( this );
        }
        finally
        {
            statusChangeLock.unlock();
        }
    }

    private void reopenRealProvider()
            throws Exception
    {
        if ( wdStatus != STATUS.IDLE )
        {
            return;
        }

        {
            final String msg = "reopening ldap connection for "
                    + originalProviderConfig.getSetting( ChaiSetting.BIND_DN );
            LOGGER.debug( msg );
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
                final String msg = "error during pre-close connection for "
                        + originalProviderConfig.getSetting( ChaiSetting.BIND_DN );
                LOGGER.debug( msg );
            }
            finally
            {
                realProvider = null;
            }
        }

        try
        {
            realProvider = chaiProviderFactory.newProviderImpl( originalProviderConfig );
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

        lastBeginTime = Instant.now();
        wdStatus = STATUS.ACTIVE;
        watchdogService.registerInstance( this );
    }

    public boolean isIdle()
    {
        return STATUS.IDLE == wdStatus;
    }

    public String getIdentifier()
    {
        final ChaiProviderImplementor copiedProvider = realProvider;
        return counter
                + ( copiedProvider == null ? ".x" : "." + copiedProvider.getIdentifier() );

    }
}
