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

import com.novell.ldapchai.util.internal.ChaiLogger;

import java.io.Closeable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class WatchdogService implements Closeable
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( WatchdogService.class );

    private static final String THREAD_NAME = "LDAP Chai WatchdogWrapper timer thread";

    private final long watchdogFrequency;

    private final ChaiProviderFactory chaiProviderFactory;

    private final ChaiProviderFactory.WeakReferenceHolder<WatchdogWrapper> issuedWatchdogWrappers = new ChaiProviderFactory.WeakReferenceHolder<>();

    private final Lock serviceThreadLock = new ReentrantLock();

    private volatile ScheduledExecutorService watchdogTimer = null;

    WatchdogService( final ChaiProviderFactory chaiProviderFactory )
    {
        watchdogFrequency = Integer.parseInt(
                chaiProviderFactory.getChaiProviderFactorySettings().getOrDefault(
                        ChaiProviderFactorySetting.WATCHDOG_CHECK_FREQUENCY,
                        ChaiProviderFactorySetting.WATCHDOG_CHECK_FREQUENCY.getDefaultValue()
                )
        );
        this.chaiProviderFactory = chaiProviderFactory;
    }

    void registerInstance( final WatchdogWrapper wdWrapper )
    {
        issuedWatchdogWrappers.add( wdWrapper );
        checkTimer();
    }

    /**
     * Regulate the timer.  This is important because the timer task creates its own thread,
     * and if the task isn't cleaned up, there could be a thread leak.
     */
    private void checkTimer()
    {
        serviceThreadLock.lock();
        try
        {
            if ( watchdogTimer == null )
            {
                // if there is NOT an active timer
                if ( !issuedWatchdogWrappers.allValues().isEmpty() )
                {
                    // if there are active providers.
                    LOGGER.debug( () -> "starting up " + THREAD_NAME + ", "
                            + ChaiLogger.format( Duration.of( watchdogFrequency, ChronoUnit.MILLIS ) ) + " check frequency" );

                    // create a new timer
                    startWatchdogThread();
                }
            }
        }
        finally
        {
            serviceThreadLock.unlock();
        }
    }

    private void startWatchdogThread()
    {
        final ThreadFactory threadFactory = new ThreadFactory()
        {
            private final ThreadFactory realThreadFactory = Executors.defaultThreadFactory();

            @Override
            public Thread newThread( final Runnable runnable )
            {
                final Thread t = realThreadFactory.newThread( runnable );
                t.setDaemon( true );
                t.setName( THREAD_NAME );
                return t;
            }
        };

        watchdogTimer = Executors.newSingleThreadScheduledExecutor( threadFactory );
        watchdogTimer.scheduleWithFixedDelay( new WatchdogTask(),  watchdogFrequency, watchdogFrequency, TimeUnit.MILLISECONDS );
    }

    private void stopWatchdogThread()
    {
        watchdogTimer.shutdown();
        watchdogTimer = null;
    }

    private void checkProvider( final WatchdogWrapper wdWrapper )
    {
        try
        {
            if ( wdWrapper != null )
            {
                wdWrapper.periodicStatusCheck();
            }
        }
        catch ( Exception e )
        {
            final String errorMsg = "error during watchdog provider idle check of connection "
                    + wdWrapper.getIdentifier()
                    + ", error: " + e.getMessage();

            LOGGER.warn( () -> errorMsg );
        }
    }

    private class WatchdogTask implements Runnable
    {
        @Override
        public void run()
        {
            final Set<WatchdogWrapper> copyCollection = new HashSet<>( issuedWatchdogWrappers.allValues() );

            for ( final WatchdogWrapper wdWrapper : copyCollection )
            {
                try
                {
                    checkProvider( wdWrapper );
                }
                catch ( Throwable e )
                {
                    LOGGER.error( () -> "error during watchdog timer check: " + e.getMessage() );
                }
            }

            final int currentCollectionSize = issuedWatchdogWrappers.allValues().size();
            if ( copyCollection.size() != currentCollectionSize )
            {
                LOGGER.trace( () -> "outstanding providers: " + currentCollectionSize );
            }

            if ( copyCollection.isEmpty() )
            {
                // if there are no active providers
                LOGGER.debug( () -> "exiting " + THREAD_NAME + ", no connections requiring monitoring are in use" );

                serviceThreadLock.lock();
                try
                {
                    // kill the timer.
                    stopWatchdogThread();
                }
                finally
                {
                    serviceThreadLock.unlock();
                }
            }
        }
    }

    @Override
    public void close()
    {
        try
        {
            serviceThreadLock.lock();
            if ( watchdogTimer != null )
            {
                watchdogTimer.shutdown();
                watchdogTimer = null;
            }

            final Collection<WatchdogWrapper> wrappers = issuedWatchdogWrappers.allValues();
            for ( final WatchdogWrapper watchdogWrapper : wrappers )
            {
                watchdogWrapper.close();
                issuedWatchdogWrappers.remove( watchdogWrapper );
            }
        }
        finally
        {
            serviceThreadLock.unlock();
        }
    }
}
