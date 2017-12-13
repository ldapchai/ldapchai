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

import com.novell.ldapchai.util.ChaiLogger;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class WatchdogService
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( ChaiLogger.class );

    private static final String THREAD_NAME = "LDAP Chai WatchdogWrapper timer thread";

    private final Set<WeakReference<WatchdogWrapper>> activeWrappers = ConcurrentHashMap.newKeySet();

    private final long watchdogFrequency;

    private final Lock serviceThreadLock = new ReentrantLock();

    /**
     * timer instance used to watch all the outstanding providers.
     */
    private volatile Timer watchDogTimer = null;

    WatchdogService( final ChaiProviderFactory chaiProviderFactory )
    {
        watchdogFrequency = Integer.parseInt(
                chaiProviderFactory.getChaiProviderFactorySettings().getOrDefault(
                        ChaiProviderFactorySetting.WATCHDOG_CHECK_FREQUENCY,
                        ChaiProviderFactorySetting.WATCHDOG_CHECK_FREQUENCY.getDefaultValue()
                )
        );
    }

    void registerInstance( final WatchdogWrapper wdWrapper )
    {
        activeWrappers.add( new WeakReference<>( wdWrapper ) );
        checkTimer();
    }

    void deRegisterInstance( final WatchdogWrapper wdWrapper )
    {
        for ( final WeakReference<WatchdogWrapper> reference : activeWrappers )
        {
            final WatchdogWrapper watchdogWrapper = reference.get();
            if ( watchdogWrapper != null && watchdogWrapper.equals( wdWrapper ) )
            {
                activeWrappers.remove( reference );
            }
        }
    }

    /**
     * Regulate the timer.  This is important because the timer task creates its own thread,
     * and if the task isn't cleaned up, there could be a thread leak.
     */
    private void checkTimer()
    {
        try
        {
            serviceThreadLock.lock();

            if ( watchDogTimer == null )
            {
                // if there is NOT an active timer
                if ( !activeWrappers.isEmpty() )
                {
                    // if there are active providers.
                    LOGGER.debug( "starting up " + THREAD_NAME + ", " + watchdogFrequency + "ms check frequency" );

                    // create a new timer
                    watchDogTimer = new Timer( THREAD_NAME, true );
                    watchDogTimer.schedule( new WatchdogTask(), watchdogFrequency, watchdogFrequency );
                }
            }
        }
        finally
        {
            serviceThreadLock.unlock();
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
            sb.append( "error during watchdog provider idle check of connection " );
            sb.append( wdWrapper.getIdentifier() );
            sb.append( ", error: " );
            sb.append( e.getMessage() );

            LOGGER.warn( sb );
        }
    }

    private Set<WatchdogWrapper> getWrappers()
    {
        final Set<WatchdogWrapper> copyCollection = new HashSet<>();
        for ( final Reference<WatchdogWrapper> reference : activeWrappers )
        {
            final WatchdogWrapper wrapper = reference.get();
            if ( wrapper != null )
            {
                copyCollection.add( wrapper );
            }
        }
        return copyCollection;
    }

    private class WatchdogTask extends TimerTask implements Runnable
    {
        public void run()
        {
            final Set<WatchdogWrapper> copyCollection = getWrappers();

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
                    serviceThreadLock.lock();

                    // kill the timer.
                    watchDogTimer.cancel();
                    watchDogTimer = null;
                }
                finally
                {
                    serviceThreadLock.unlock();
                }
            }

        }
    }
}
