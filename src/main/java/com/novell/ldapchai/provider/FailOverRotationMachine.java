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

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.internal.ChaiLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>The rotation machine manages which URL is currently active.  It does this by
 * setting up a "Slot" for each of the available URLs.  Visually it might look something
 * like this:</p>
 * <pre>
 *     Slot#   ProviderSlot instance       state
 *   +-------+--------------------------+----------+
 *   |   0   | URL #1, config, provider |  closed  |
 *   +-------+--------------------------+----------+
 *   |   1   | URL #2, config, provider |  active  |
 *   +-------+--------------------------+----------+
 *   |   2   | URL #3, config, provider |  null    |
 *   +-------+--------------------------+----------+
 * </pre>
 * <p>The machine keeps track of a which slot is "active".  If the machie is informed that the
 * current provider is broken, the machine will advance the active marker to the next slot
 * and attempt to make it's provider active.  All other slots will have their provider's inactivated.</p>
 *
 * <p>If a slot other than #0 is active for a duration longer than the
 * {@link FailOverSettings#getMinFailBackTime()}, the
 * machine will rotate back to slot #0 then next time it is accessed.</p>
 *
 * <p>For any given unique urlList used in the settings, a global (static) "last known good" cache is maintained
 * with the last known good slot.  In this way, if a new provider is created, it will start with a good
 * slot instead of the dead "0"th slot.  This makes initial connection times much faster in cases
 * where the 0th slot is a consistently dead server.</p>
 *
 * <p>Despite the last known good cache, every rotation machine maintains an unrelated state.  The cache
 * is only used for setting the initial slot used when a new rotation machine is created.</p>
 */
class FailOverRotationMachine
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( FailOverRotationMachine.class );

    private enum FailState
    {
        NEW, OKAY, SEEKING, FAILED
    }

    private Instant lastFailureTime = Instant.now();

    private final List<ProviderSlot> providerSlots = new CopyOnWriteArrayList<>();
    private final AtomicInteger activeSlot = new AtomicInteger( 0 );
    private final FailOverSettings settings;
    private final ChaiProviderFactory providerFactory;
    private final ChaiConfiguration originalConfiguration;

    private final int urlListHashCode;

    private static final Map<Integer, Integer> LAST_KNOWN_GOOD_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_SIZE_LNG_CACHE = 50;
    private static long lngLastPopulateTime = System.currentTimeMillis();

    private Exception lastConnectionException;

    private volatile FailState failState = FailState.NEW;

    FailOverRotationMachine(
            final ChaiProviderFactory chaiProviderFactory,
            final ChaiConfiguration chaiConfig,
            final FailOverSettings settings
    )
            throws ChaiUnavailableException
    {
        urlListHashCode = chaiConfig.bindURLsAsList().hashCode();
        this.settings = settings;
        this.providerFactory = chaiProviderFactory;
        this.originalConfiguration = chaiConfig;
        configureInitialState( chaiConfig );
    }

    private void setActiveSlot( final int activeSlot )
    {
        this.activeSlot.set( activeSlot );

        if ( activeSlot != 0 )
        {
            if ( originalConfiguration.getBooleanSetting( ChaiSetting.FAILOVER_USE_LAST_KNOWN_GOOD_HINT ) )
            {
                LAST_KNOWN_GOOD_CACHE.put( urlListHashCode, activeSlot );
                lngLastPopulateTime = System.currentTimeMillis();


                while ( LAST_KNOWN_GOOD_CACHE.size() > MAX_SIZE_LNG_CACHE )
                {
                    LAST_KNOWN_GOOD_CACHE.keySet().iterator().remove();
                    LOGGER.warn( () -> "RotationMachine maximum Last Known Good cache size ("
                            + MAX_SIZE_LNG_CACHE + ") exceeded, reducing cached entries " );
                }
            }
        }
    }

    private void configureInitialState( final ChaiConfiguration chaiConfig )
    {
        for ( final String loopUrl : chaiConfig.bindURLsAsList() )
        {
            final ChaiConfiguration loopConfig = ChaiConfiguration.builder( chaiConfig )
                    .setSetting( ChaiSetting.BIND_URLS, loopUrl )
                    .build();
            providerSlots.add( new ProviderSlot( loopConfig, loopUrl ) );
        }

        if ( originalConfiguration.getBooleanSetting( ChaiSetting.FAILOVER_USE_LAST_KNOWN_GOOD_HINT ) )
        {
            if ( !LAST_KNOWN_GOOD_CACHE.isEmpty() )
            {
                if ( ( System.currentTimeMillis() - lngLastPopulateTime ) > settings.getMinFailBackTime() )
                {
                    LAST_KNOWN_GOOD_CACHE.clear();
                }
            }

            if ( LAST_KNOWN_GOOD_CACHE.containsKey( urlListHashCode ) )
            {
                activeSlot.set( LAST_KNOWN_GOOD_CACHE.get( urlListHashCode ) );
                LOGGER.debug( () -> "using slot #" + activeSlot.get() + " (" + providerSlots.get(
                        activeSlot.get() ).getUrl() + ") as initial bind URL due to Last Known Good cache" );
            }
        }
    }

    ChaiProvider getCurrentProvider()
            throws ChaiUnavailableException
    {
        failbackCheck();

        ChaiUnavailableException lastException = null;
        if ( failState == FailState.NEW )
        {
            try
            {
                makeNewProvider( activeSlot.get() );
                failState = FailState.OKAY;
            }
            catch ( ChaiUnavailableException e )
            {
                lastException = e;
                if ( settings.errorIsRetryable( e ) )
                {
                    failState = FailState.FAILED;
                }
                else
                {
                    throw e;
                }
            }
        }

        if ( failState == FailState.OKAY )
        {
            return providerSlots.get( activeSlot.get() ).getProvider();
        }

        if ( failState == FailState.FAILED )
        {
            currentServerIsBroken( lastException );

            if ( failState == FailState.OKAY )
            {
                return providerSlots.get( activeSlot.get() ).getProvider();
            }
        }

        final StringBuilder errorMsg = new StringBuilder();
        if ( originalConfiguration.bindURLsAsList().size() > 1 )
        {
            errorMsg.append( "unable to connect to any configured ldap url" );

            if ( lastConnectionException != null )
            {
                errorMsg.append( ", last error: " );
                errorMsg.append( lastConnectionException.getMessage() );
            }
        }
        else
        {
            errorMsg.append( ( "unable to connect to ldap url" ) );
            if ( lastConnectionException != null )
            {
                errorMsg.append( ", error: " );
                errorMsg.append( lastConnectionException.getMessage() );
            }
        }

        throw new ChaiUnavailableException( errorMsg.toString(), ChaiError.COMMUNICATION );
    }

    void reportBrokenProvider( final ChaiProvider provider, final Exception e )
    {
        //no point doing anything if state is already reported as broken.
        if ( failState != FailState.OKAY )
        {
            return;
        }

        //make sure the reported provider is the one thats actually currently active, otherwise ignore the report
        final ChaiProvider presumedCurrentProvider = providerSlots.get( activeSlot.get() ).getProvider();
        if ( presumedCurrentProvider != null && presumedCurrentProvider.equals( provider ) )
        {
            currentServerIsBroken( e );
        }
    }

    private void failbackCheck()
    {
        if ( failState == FailState.OKAY && activeSlot.get() != 0 )
        {
            final Duration msSinceLastFailure = Duration.between( lastFailureTime, Instant.now() );
            if ( msSinceLastFailure.toMillis() > settings.getMinFailBackTime() )
            {
                failState = FailState.NEW;
                setActiveSlot( 0 );
            }
        }
    }

    private void currentServerIsBroken( final Exception errorCause )
    {
        if ( providerSlots.size() > 1 )
        {
            LOGGER.warn( () -> "current server " + providerSlots.get( activeSlot.get() ).getUrl()
                    + " has failed, failing over to next server in list"
                    + ( ( errorCause != null ) ? ", last error: " + errorCause.getMessage() : "" ) );
        }
        else
        {
            LOGGER.warn( () -> "unable to reach ldap server " + providerSlots.get( activeSlot.get() ).getUrl()
                    + ( ( errorCause != null ) ? ", last error: " + errorCause.getMessage() : "" ) );
        }
        lastFailureTime = Instant.now();
        boolean success = false;

        try
        {
            failState = FailState.SEEKING;

            final int maxRetries = providerSlots.size();
            int retryCounter = 0;
            while ( !success && retryCounter < maxRetries )
            {

                if ( activeSlot.get() + 1 > providerSlots.size() - 1 )
                {
                    setActiveSlot( 0 );
                    pause( settings.getRotateDelay() );
                }
                else
                {
                    setActiveSlot( activeSlot.get() + 1 );
                }


                if ( providerSlots.size() > 1 )
                {
                    LOGGER.debug( () -> "failing over to " + providerSlots.get( activeSlot.get() ).getUrl() );
                }

                final Instant startTime = Instant.now();
                try
                {
                    makeNewProvider( activeSlot.get() );
                    success = true;
                }
                catch ( ChaiUnavailableException e )
                {
                    lastConnectionException = e;
                    if ( settings.getFailOverHelper().errorIsRetryable( e ) )
                    {
                        LOGGER.debug( () -> "error connecting to ldap server, will retry, " + e.getMessage() );
                    }
                    else
                    {
                        LOGGER.debug( () -> "detected un-retryable error while rotating servers: " + e.getMessage() );
                        break;
                    }
                }
                final Duration duration = Duration.between( startTime, Instant.now() );
                LOGGER.debug( () -> "failed over to " + providerSlots.get( activeSlot.get() ).getUrl(), duration );
                retryCounter++;
            }
        }
        finally
        {
            failState = success ? FailState.OKAY : FailState.FAILED;
        }
    }

    private void makeNewProvider( final int forSlot )
            throws ChaiUnavailableException
    {
        destoryAllConnections();

        //create a new connection
        final ProviderSlot slot = providerSlots.get( forSlot );
        try
        {
            final ChaiProviderImplementor newProvider = ChaiProviderFactory.createConcreteProvider(
                    providerFactory,
                    slot.getConfig(),
                    true );
            slot.setProvider( newProvider );
        }
        catch ( ChaiUnavailableException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            final String errorMsg = "unexpected error creating new FailOver ChaiProvider: " + e.getMessage();
            LOGGER.error( () -> errorMsg );
            throw new IllegalStateException( errorMsg, e );
        }
    }

    public void destoryAllConnections()
    {
        for ( final ProviderSlot loopSlot : providerSlots )
        {
            final ChaiProvider loopProvider = loopSlot.getProvider();
            if ( loopProvider != null )
            {
                loopProvider.close();
            }
        }
    }

    private static class ProviderSlot
    {
        ChaiConfiguration config;
        String url;
        ChaiProviderImplementor provider;

        private ProviderSlot( final ChaiConfiguration config, final String url )
        {
            this.config = config;
            this.url = url;
        }

        public ChaiConfiguration getConfig()
        {
            return config;
        }

        public String getUrl()
        {
            return url;
        }

        public ChaiProviderImplementor getProvider()
        {
            return provider;
        }

        public void setProvider( final ChaiProviderImplementor provider )
        {
            this.provider = provider;
        }
    }

    /**
     * Causes the executing thread to pause for a period of time.
     *
     * @param time in ms
     */
    private static void pause( final long time )
    {
        final long startTime = System.currentTimeMillis();
        do
        {
            try
            {
                final long sleepTime = time - ( System.currentTimeMillis() - startTime );
                Thread.sleep( sleepTime > 0 ? sleepTime : 10 );
            }
            catch ( InterruptedException e )
            {
                //don't care
            }
        } while ( ( System.currentTimeMillis() - startTime ) < time );
    }

}
