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

import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.internal.ChaiLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A ProviderHolder holds the underlying connection used by a <code>WatchdogWrapper</code>
 * instance.
 */
class WatchdogProviderHolder
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( WatchdogProviderHolder.class );

    /**
     * Lock for consumers of a provider, used to tell if any consumers of the provider are active.
     */
    private final ReadWriteLock usageLock = new ReentrantReadWriteLock();

    /**
     * Lock around mutations of the provider (via the {@link InternalHolder}).
     */
    private final Lock providerChangeLock = new ReentrantLock();

    private final String wrapperIdentifier;
    private final ChaiProviderFactory chaiProviderFactory;
    private final WatchdogWrapper.Settings settings;
    private final ChaiConfiguration chaiConfiguration;

    private volatile InternalHolder miniHolder;
    private volatile Instant lastActivity = Instant.now();
    private volatile boolean closed = false;

    WatchdogProviderHolder(
            final String wrapperIdentifier,
            final ChaiProviderImplementor chaiProviderImplementor,
            final WatchdogWrapper.Settings settings
    )
    {
        this.wrapperIdentifier = wrapperIdentifier;
        this.chaiConfiguration = chaiProviderImplementor.getChaiConfiguration();
        this.settings = settings;
        this.chaiProviderFactory = chaiProviderImplementor.getProviderFactory();

        this.miniHolder = new InternalHolder( chaiProviderImplementor );

        LOGGER.trace( () -> "created WatchdogProviderHolder " + wrapperIdentifier );
    }

    public boolean isConnected()
    {
        return !closed && miniHolder != null;
    }

    public void close()
    {
        // when close is called, locks aren't necessary.  we just close the real provider (if it exists) and null it.
        closed = true;

        try
        {

            final InternalHolder localMiniHolder = miniHolder;
            if ( localMiniHolder != null )
            {
                localMiniHolder.getRealProvider().close();
            }
        }
        catch ( final Exception e )
        {
            LOGGER.debug( () -> "error while closing connection: " + e.getMessage(), e );
        }

        miniHolder = null;
    }

    Object getConnectionObject()
            throws Exception
    {
        usageLock.readLock().lock();
        try
        {
            return getProvider().getConnectionObject();
        }
        finally
        {
            usageLock.readLock().unlock();
        }
    }

    <T> T execute( final LdapFunction<T> ldapFunction )
            throws ChaiOperationException, ChaiUnavailableException
    {
        usageLock.readLock().lock();
        try
        {
            lastActivity = Instant.now();
            final T result = ldapFunction.execute( getProvider() );
            lastActivity = Instant.now();
            return result;
        }
        catch ( final ChaiUnavailableException | ChaiOperationException e )
        {
            if ( closed )
            {
                LOGGER.debug( () -> " execution exception occurred while closed: " + e.getMessage() );
            }
            throw e;
        }
        finally
        {
            usageLock.readLock().unlock();
        }
    }

    private ChaiProviderImplementor getProvider( )
            throws ChaiUnavailableException
    {
        providerChangeLock.lock();
        try
        {
            if ( miniHolder != null && !miniHolder.getRealProvider().isConnected() )
            {
                disconnectRealProvider( () -> "underlying connection has already been closed" );
            }

            if ( closed )
            {
                throw new IllegalStateException( "ChaiProvider instance has been closed" );
            }

            if ( miniHolder != null )
            {
                return miniHolder.getRealProvider();
            }

            return restoreRealProvider();
        }
        finally
        {
            providerChangeLock.unlock();
        }
    }

    /**
     * This method is invoked periodically by the {@link WatchdogService} to check watchdog instances.
     */
    void periodicStatusCheck()
    {
        if ( miniHolder == null )
        {
            return;
        }

        // write lock success indicates the provider is not in use, if there
        // is an active connection then no need to check these statuses
        if ( usageLock.writeLock().tryLock() )
        {
            providerChangeLock.lock();
            try
            {
                checkMaxLifetimeDuration();
                checkIdleTimeout();
            }
            finally
            {
                providerChangeLock.unlock();
                usageLock.writeLock().unlock();
            }
        }
        else
        {
            checkOperationTimeout();
        }
    }

    private void checkIdleTimeout()
    {
        final Duration idleDuration = Duration.between( lastActivity, Instant.now() );
        if ( idleDuration.toMillis() > settings.getIdleTimeoutMS() )
        {
            final Supplier<String> msg = () -> "watchdog idle timeout detected ("
                    + ChaiLogger.format( idleDuration )
                    + "), closing connection id="
                    + wrapperIdentifier;

            disconnectRealProvider( msg );
        }
    }

    private void checkMaxLifetimeDuration()
    {
        final Duration maxConnectionLifetime = settings.getMaxConnectionLifetime();
        if ( !miniHolder.getAllowDisconnectSupplier().getAsBoolean() || maxConnectionLifetime == null )
        {
            return;
        }

        final Duration ageOfConnection = Duration.between( miniHolder.getConnectionEstablishedTime(), Instant.now() );
        if ( ageOfConnection.compareTo( maxConnectionLifetime ) > 0 )
        {
            final Supplier<String> msg = () -> "connection lifetime ("
                    + ChaiLogger.format( ageOfConnection )
                    + ") exceeded maximum configured lifetime ("
                    + ChaiLogger.format( maxConnectionLifetime ) + ")";

            disconnectRealProvider( msg );
        }
    }

    private void checkOperationTimeout()
    {
        if ( miniHolder == null )
        {
            return;
        }

        final Duration operationDuration = Duration.between( lastActivity, Instant.now() );
        if ( operationDuration.toMillis() > settings.getOperationTimeoutMS() )
        {
            final Supplier<String> msg = () -> "ldap operation timeout detected ("
                    + ChaiLogger.format( operationDuration )
                    + "), closing questionable connection id="
                    + wrapperIdentifier;

            disconnectRealProvider( msg );
        }

    }

    private ChaiProviderImplementor restoreRealProvider()
            throws ChaiUnavailableException
    {
        try
        {
            final Instant startTime = Instant.now();
            final ChaiProviderImplementor newProvider = chaiProviderFactory.createFailOverOrConcreteProvider( chaiConfiguration );
            miniHolder = new InternalHolder( newProvider );
            LOGGER.trace( () -> "re-opened ldap connection id=" + wrapperIdentifier, Duration.between( startTime, Instant.now() ) );

            return newProvider;
        }
        catch ( ChaiUnavailableException e )
        {
            final String msg = "error reopening ldap connection for id="
                    + wrapperIdentifier
                    + ", error: "
                    + e.getMessage();
            LOGGER.debug( () -> msg );
            throw e;
        }
    }

    private void disconnectRealProvider(
            final Supplier<String> debugMsg
    )
    {
        final InternalHolder localHolder = miniHolder;

        if ( localHolder == null )
        {
            return;
        }

        if ( !localHolder.getAllowDisconnectSupplier().getAsBoolean() )
        {
            return;
        }

        localHolder.getRealProvider().close();
        this.miniHolder = null;

        LOGGER.trace( debugMsg );
    }

    interface LdapFunction<T>
    {
        T execute( ChaiProvider chaiProvider ) throws ChaiOperationException, ChaiUnavailableException;
    }

    /**
     * Internal holder, a basic wrapper around a {@code ChaiProviderImplementor}, this forces the accompanying
     * members of this class to get recreated when the {@code #realProvider} is recreated.
     */
    private static class InternalHolder
    {
        private final ChaiProviderImplementor realProvider;
        private final DetectAllowDisconnectSupplier allowDisconnectSupplier = new DetectAllowDisconnectSupplier();
        private final Instant connectionEstablishedTime = Instant.now();

        InternalHolder( final ChaiProviderImplementor realProvider )
        {
            this.realProvider = realProvider;
        }

        public ChaiProviderImplementor getRealProvider()
        {
            return realProvider;
        }

        public DetectAllowDisconnectSupplier getAllowDisconnectSupplier()
        {
            return allowDisconnectSupplier;
        }

        public Instant getConnectionEstablishedTime()
        {
            return connectionEstablishedTime;
        }

        class DetectAllowDisconnectSupplier implements BooleanSupplier
        {
            private boolean supplied;
            private boolean result;

            @Override
            public boolean getAsBoolean()
            {
                if ( !supplied )
                {
                    result = impl();
                }
                return result;
            }

            public boolean impl()
            {
                final ChaiProviderImplementor provider = realProvider;
                if ( provider != null )
                {
                    try
                    {
                        final DirectoryVendor vendor = provider.getDirectoryVendor();
                        if ( vendor != null )
                        {
                            supplied = true;
                            return vendor.getVendorFactory().allowWatchdogDisconnect( provider );
                        }
                    }
                    catch ( final ChaiUnavailableException e )
                    {
                        LOGGER.debug( () -> "error while determining if watchdog disconnect is allowed for provider: " + e.getMessage(), e );
                    }
                }
                return true;
            }
        }
    }
}
