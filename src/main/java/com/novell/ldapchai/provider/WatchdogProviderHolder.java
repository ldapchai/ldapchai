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
import com.novell.ldapchai.util.ChaiLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A ProviderHolder holds the underlying connection used by a <code>WatchdogWrapper</code> instance.
 */
class WatchdogProviderHolder
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( WatchdogProviderHolder.class );

    private volatile ChaiProviderImplementor realProvider;
    private volatile HolderStatus wdStatus = HolderStatus.ACTIVE;
    private volatile Instant lastActivity = Instant.now();
    private volatile Instant connectionEstablishedTime = Instant.now();

    private static final AtomicInteger ID_COUNTER = new AtomicInteger( 0 );
    private final int counter = ID_COUNTER.getAndIncrement();

    private final AtomicInteger activeOpCounter = new AtomicInteger();

    private final boolean allowDisconnect;
    private final ReadWriteLock statusChangeLock = new ReentrantReadWriteLock(  );

    private final WatchdogWrapper watchdogWrapper;
    private final ChaiProviderFactory chaiProviderFactory;
    private final WatchdogWrapper.Settings settings;
    private final ChaiConfiguration chaiConfiguration;

    enum HolderStatus
    {
        ACTIVE,
        IDLE,
        CLOSED,
    }

    WatchdogProviderHolder( final WatchdogWrapper watchdogWrapper, final ChaiProviderImplementor chaiProviderImplementor )
    {
        this.watchdogWrapper = watchdogWrapper;
        this.chaiConfiguration = chaiProviderImplementor.getChaiConfiguration();
        this.settings = watchdogWrapper.getSettings();
        this.realProvider = chaiProviderImplementor;
        this.chaiProviderFactory = chaiProviderImplementor.getProviderFactory();
        allowDisconnect = !watchdogWrapper.checkForPwExpiration( realProvider );

        chaiProviderFactory.getCentralService().getWatchdogService().registerInstance( watchdogWrapper );

        LOGGER.trace( "created WatchdogProviderHolder " + getIdentifier() );
    }

    HolderStatus getStatus()
    {
        return wdStatus;
    }

    public String getIdentifier()
    {
        statusChangeLock.readLock().lock();

        try
        {
            if ( wdStatus != HolderStatus.CLOSED && realProvider != null )
            {
                return "w" + counter + "-" + realProvider.getIdentifier();
            }
            else
            {
                return "w" + counter;
            }
        }
        finally
        {
            statusChangeLock.readLock().unlock();
        }
    }

    public void close()
    {
        statusChangeLock.writeLock().lock();

        try
        {
            wdStatus = HolderStatus.CLOSED;
            if ( realProvider != null )
            {
                realProvider.close();
            }
            realProvider = null;
            lastActivity = Instant.now();
        }
        finally
        {
            statusChangeLock.writeLock().unlock();
        }
    }

    <T> T execute( final LdapFunction<T> ldapFunction )
            throws ChaiOperationException, ChaiUnavailableException
    {
        statusChangeLock.writeLock().lock();
        try
        {
            activeOpCounter.incrementAndGet();
        }
        finally
        {
            statusChangeLock.writeLock().unlock();
        }

        try
        {
            lastActivity = Instant.now();
            return ldapFunction.execute( getProvider() );
        }
        catch ( ChaiUnavailableException | ChaiOperationException e )
        {
            e.fillInStackTrace();
            throw e;
        }
        finally
        {
            statusChangeLock.writeLock().lock();
            try
            {
                lastActivity = Instant.now();
                activeOpCounter.decrementAndGet();
            }
            finally
            {
                statusChangeLock.writeLock().unlock();
            }
        }

    }


    ChaiProviderImplementor getProvider( )
            throws ChaiUnavailableException
    {
        statusChangeLock.writeLock().lock();
        try
        {
            return getProviderImpl();
        }
        finally
        {
            lastActivity = Instant.now();
            statusChangeLock.writeLock().unlock();
        }
    }

    void checkStatus()
    {
        statusChangeLock.writeLock().lock();

        try
        {
            if ( activeOpCounter.get() == 0 )
            {
                checkMaxLifetimeDuration();

                checkIdleTimeout();
            }
            else
            {
                checkOperationTimeout();
            }

        }
        finally
        {
            statusChangeLock.writeLock().unlock();
        }
    }

    /* internal un-synchronized methods below */

    private ChaiProviderImplementor getProviderImpl( )
            throws ChaiUnavailableException
    {
        if ( realProvider != null && !realProvider.isConnected() )
        {
            disconnectRealProvider( "underlying connection has already been closed" );
        }

        switch ( wdStatus )
        {
            case CLOSED:
                throw new IllegalStateException( "ChaiProvider instance has been closed" );

            case ACTIVE:
                return realProvider;

            case IDLE:
                return restoreRealProvider();

            default:
                throw new IllegalStateException( "unexpected internal ProviderState encountered: " + wdStatus );
        }

    }

    private void checkIdleTimeout()
    {
        if ( wdStatus == HolderStatus.ACTIVE )
        {
            final Duration idleDuration = Duration.between( lastActivity, Instant.now() );
            if ( idleDuration.toMillis() > settings.getIdleTimeoutMS() )
            {
                final String msg = "ldap idle timeout detected ("
                        + idleDuration.toString()
                        + "), closing connection id="
                        + watchdogWrapper.getIdentifier();

                disconnectRealProvider( msg );
            }
        }
    }

    private void checkMaxLifetimeDuration()
    {
        if ( wdStatus == HolderStatus.ACTIVE )
        {
            final Duration maxConnectionLifetime = settings.getMaxConnectionLifetime();
            if ( !allowDisconnect || maxConnectionLifetime == null )
            {
                return;
            }

            final Duration ageOfConnection = Duration.between( connectionEstablishedTime, Instant.now() );
            if ( ageOfConnection.compareTo( maxConnectionLifetime ) > 0 )
            {
                final String msg = "connection lifetime (" + ageOfConnection.toString()
                        + ") exceeded maximum configured lifetime (" + maxConnectionLifetime.toString() + ")";

                disconnectRealProvider( msg );
            }
        }
    }

    private void checkOperationTimeout()
    {
        if ( wdStatus == HolderStatus.ACTIVE )
        {
            final Duration operationDuration = Duration.between( lastActivity, Instant.now() );
            if ( operationDuration.toMillis() > settings.getOperationTimeoutMS() )
            {
                final String msg = "ldap operation timeout detected ("
                        + operationDuration.toString()
                        + "), closing questionable connection id="
                        + watchdogWrapper.getIdentifier();
                disconnectRealProvider( msg );
            }
        }
    }

    private ChaiProviderImplementor restoreRealProvider()
            throws ChaiUnavailableException
    {
        try
        {
            realProvider = chaiProviderFactory.createFailOverOrConcreteProvider( chaiConfiguration );
            wdStatus = HolderStatus.ACTIVE;
            connectionEstablishedTime = Instant.now();

            LOGGER.debug( "re-opened ldap connection id=" + getIdentifier() );

            return realProvider;
        }
        catch ( ChaiUnavailableException e )
        {
            final String msg = "error reopening ldap connection for id="
                    + getIdentifier()
                    + ", error: "
                    + e.getMessage();
            LOGGER.debug( msg );
            throw e;
        }
    }

    private void disconnectRealProvider(
            final String debugMsg
    )
    {
        if ( !allowDisconnect )
        {
            return;
        }

        wdStatus = HolderStatus.IDLE;

        LOGGER.debug( "disconnecting underlying connection: " + debugMsg );

        if ( realProvider != null )
        {
            this.realProvider.close();
            this.realProvider = null;
        }
    }

    interface LdapFunction<T>
    {
        T execute( ChaiProvider chaiProvider ) throws ChaiOperationException, ChaiUnavailableException;
    }
}
