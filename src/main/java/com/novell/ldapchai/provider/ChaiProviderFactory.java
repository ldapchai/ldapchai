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
import com.novell.ldapchai.exception.ChaiErrors;
import com.novell.ldapchai.exception.ChaiException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.internal.ChaiLogger;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Factory for obtaining {@link ChaiProvider} instances.  Most applications should open and hold
 * one and only one instance of {@code ChaiProviderFactory}.  {@link ChaiProviderFactory} instances
 * provide common connection, thread, statistics, and other services for their child {@link ChaiProvider}
 * instances.</p>
 *
 * <p>{@link ChaiProviderFactory} instances have a lifecycle.  Once created, the chaiFactory should be held while
 * any outstanding {@link ChaiProvider} instances are open.  The {@link #close()} method will close the
 * factory and any resources it has opened including any outstanding {@link ChaiProvider} instances.
 *
 * <p>If there are no specific requirements for how to establish a connection, the
 * "quick" factory method can be used:</p>
 *
 * <pre>
 *   ChaiProviderFactory chaiProviderFactory = ChaiProviderFactory.newProviderFactory();
 *   ChaiProvider provider = chaiProviderFactory.createProvider("ldap://host:port","cn=admin,o=org","password");
 * </pre>
 *
 * <p>If a more control is required, allocate a {@code ChaiConfiguration} first, and then
 * use this factory to generate a provider:</p>
 *
 * <pre>
 *   // create a new factory
 *   ChaiProviderFactory chaiProviderFactory = ChaiProviderFactory.newProviderFactory();
 *
 *   // setup connection variables
 *   final String bindUsername = "cn=admin,o=org";
 *   final String bindPassword = "password";
 *   final List &lt;String&gt; serverURLs = new ArrayList&lt;&gt;();
 *   serverURLs.add("ldap://server1:port");
 *   serverURLs.add("ldaps://server2:port");
 *   serverURLs.add("ldap://server3");
 *
 *   // allocate a new configuration
 *   ChaiConfiguration chaiConfig = new ChaiConfiguration(
 *            serverURLs,
 *            bindUsername,
 *            bindPassword);
 *
 *   // set any desired settings.
 *   chaiConfig.setSettings(ChaiConfiguration.SETTING_LDAP_TIMEOUT,"9000");
 *   chaiConfig.setSettings(ChaiConfiguration.SETTING_PROMISCUOUS_SSL,"true");
 *
 *   // generate the new provider
 *   ChaiProvider provider = chaiProviderFactory.createProvider(chaiConfig);
 * </pre>
 *
 * @author Jason D. Rivard
 */
public final class ChaiProviderFactory implements Closeable
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( ChaiProviderFactory.class );

    private final Map<ChaiProviderFactorySetting, String> chaiProviderFactorySettingStringMap;

    private ChaiProviderFactory( final Map<ChaiProviderFactorySetting, String> chaiProviderFactorySettingStringMap )
    {
        this.chaiProviderFactorySettingStringMap = Collections.unmodifiableMap( chaiProviderFactorySettingStringMap );
        this.centralService = new CentralService( this );
    }

    private final CentralService centralService;

    private boolean closed = false;

    /**
     * Maintains the global chai provider statistics.  All {@code com.novell.ldapchai.provider.ChaiProvider} instances
     * that have their {@link ChaiSetting#STATISTICS_ENABLE} set to <i>true</i> will register statistics in
     * this global tracker.
     *
     * @return a ProviderStatistics instance containing global statistics for the Chai API
     */
    public Map<String, String> getGlobalStatistics()
    {
        final Map<String, String> debugProperties = new LinkedHashMap<>();
        final ProviderStatistics providerStatistics = getCentralService().getStatsBean();
        if ( providerStatistics != null )
        {
            debugProperties.putAll( providerStatistics.allStatistics() );
        }

        return Collections.unmodifiableMap( debugProperties );
    }

    /**
     * Create a {@code ChaiProvider} using a standard (default) JNDI ChaiProvider.
     *
     * @param bindDN   ldap bind DN, in ldap fully qualified syntax.  Also used as the DN of the returned ChaiUser.
     * @param password password for the bind DN.
     * @param ldapURL  ldap server and port in url format, example: <i>ldap://127.0.0.1:389</i>
     * @return A ChaiProvider with an active connection to the ldap directory
     * @throws ChaiUnavailableException If the directory server(s) are not reachable.
     */
    public ChaiProvider newProvider( final String ldapURL, final String bindDN, final String password )
            throws ChaiUnavailableException
    {
        final ChaiConfiguration chaiConfig = ChaiConfiguration.builder( ldapURL, bindDN, password ).build();
        return newProvider( chaiConfig );
    }

    /**
     * Create a {@code ChaiProvider} using the specified <i>chaiConfiguration</i>.  A ChaiProvider will be created
     * according to the implementation class and configuration settings contained within the configuration.
     * All other factory methods are simply convenience wrappers around this method.
     *
     * @param chaiConfiguration A completed, lockable configuration
     * @return A functioning ChaiProvider generated according to <i>chaiConfiguration</i>
     * @throws ChaiUnavailableException If the directory server(s) are not reachable.
     */
    public ChaiProvider newProvider( final ChaiConfiguration chaiConfiguration )
            throws ChaiUnavailableException
    {
        return newProviderImpl( chaiConfiguration );
    }

    private ChaiProviderImplementor newProviderImpl(
            final ChaiConfiguration chaiConfiguration
    )
            throws ChaiUnavailableException
    {
        checkStatus();

        ChaiProviderImplementor providerImpl;
        try
        {
            providerImpl = createFailOverOrConcreteProvider( chaiConfiguration );
        }
        catch ( Exception e )
        {
            final String errorMsg = "unable to create connection: " + e.getClass().getName() + ":" + e.getMessage();
            if ( e instanceof ChaiException || e instanceof IOException )
            {
                LOGGER.debug( () -> errorMsg );
            }
            else
            {
                LOGGER.debug( () -> errorMsg, e );
            }
            throw new ChaiUnavailableException( "unable to create connection: " + e.getMessage(), ChaiErrors.getErrorForMessage( e.getMessage() ), e );
        }

        providerImpl = addProviderWrappers( providerImpl );
        getCentralService().registerProvider( providerImpl );

        return providerImpl;
    }

    ChaiProviderImplementor createFailOverOrConcreteProvider(
            final ChaiConfiguration chaiConfiguration
    )
            throws ChaiUnavailableException
    {
        final boolean enableFailover = chaiConfiguration.getBooleanSetting( ChaiSetting.FAILOVER_ENABLE );

        if ( enableFailover )
        {
            return FailOverWrapper.forConfiguration( this, chaiConfiguration );
        }
        else
        {
                LOGGER.trace( () -> "creating new ldap connection to "
                        + chaiConfiguration.getSetting( ChaiSetting.BIND_URLS )
                        + " as "
                        + chaiConfiguration.getSetting( ChaiSetting.BIND_DN ) );

            return createConcreteProvider( this, chaiConfiguration, true );
        }
    }

    static ChaiProviderImplementor createConcreteProvider(
            final ChaiProviderFactory providerFactory,
            final ChaiConfiguration chaiConfiguration,
            final boolean initialize
    )
            throws ChaiUnavailableException, IllegalStateException
    {
        try
        {
            final String className = chaiConfiguration.getSetting( ChaiSetting.PROVIDER_IMPLEMENTATION );

            final ChaiProviderImplementor providerImpl;

            final Class providerClass = Class.forName( className );
            final Object impl = providerClass.newInstance();
            if ( !( impl instanceof ChaiProvider ) )
            {
                final String msg = "unable to create new ChaiProvider, "
                        + className + " is not instance of "
                        + ChaiProvider.class.getName();
                throw new ChaiUnavailableException( msg, ChaiError.UNKNOWN );
            }
            if ( !( impl instanceof ChaiProviderImplementor ) )
            {
                final String msg = "unable to create new ChaiProvider, "
                        + className + " is not instance of "
                        + ChaiProviderImplementor.class.getName();
                throw new ChaiUnavailableException( msg, ChaiError.UNKNOWN );
            }
            providerImpl = ( ChaiProviderImplementor ) impl;

            if ( initialize )
            {
                providerImpl.init( chaiConfiguration, providerFactory );
            }

            return providerImpl;
        }
        catch ( ClassNotFoundException | IllegalAccessException | InstantiationException e )
        {
            final String msg = "unexpected error creating new concrete ChaiProvider instance: " + e.getMessage();
            LOGGER.error( () -> msg, e );
            throw new IllegalStateException( msg );
        }
    }

    private ChaiProviderImplementor addProviderWrappers( final ChaiProviderImplementor providerImpl )
    {
        final ChaiConfiguration chaiConfiguration = providerImpl.getChaiConfiguration();

        final boolean enableWatchdog = chaiConfiguration.getBooleanSetting( ChaiSetting.WATCHDOG_ENABLE );
        final boolean enableReadOnly = chaiConfiguration.getBooleanSetting( ChaiSetting.READONLY );
        final boolean enableWireTrace = chaiConfiguration.getBooleanSetting( ChaiSetting.WIRETRACE_ENABLE );
        final boolean enableStatistics = chaiConfiguration.getBooleanSetting( ChaiSetting.STATISTICS_ENABLE );
        final boolean enableCaching = chaiConfiguration.getBooleanSetting( ChaiSetting.CACHE_ENABLE );
        final boolean threadSafeEnabled = chaiConfiguration.getBooleanSetting( ChaiSetting.THREAD_SAFE_ENABLE );

        ChaiProviderImplementor outputProvider = providerImpl;

        if ( enableWatchdog && !( outputProvider instanceof WatchdogWrapper ) )
        {
            LOGGER.trace( () -> "adding WatchdogWrapper to provider instance" );
            outputProvider = WatchdogWrapper.forProvider( this, outputProvider );
        }

        if ( enableReadOnly && !( outputProvider instanceof ReadOnlyWrapper ) )
        {
            LOGGER.trace( () -> "adding ReadOnlyWrapper to provider instance" );
            outputProvider = ReadOnlyWrapper.forProvider( outputProvider );
        }

        if ( enableWireTrace && !( outputProvider instanceof WireTraceWrapper ) )
        {
            LOGGER.trace( () -> "adding WireTraceWrapper to provider instance" );
            outputProvider = WireTraceWrapper.forProvider( outputProvider );
        }

        if ( enableStatistics && !( outputProvider instanceof StatisticsWrapper ) )
        {
            LOGGER.trace( () -> "adding StatisticsWrapper to provider instance" );
            outputProvider = StatisticsWrapper.forProvider( outputProvider );
        }

        if ( enableCaching && !( outputProvider instanceof CachingWrapper ) )
        {
            LOGGER.trace( () -> "adding CachingWrapper to provider instance" );
            outputProvider = CachingWrapper.forProvider( outputProvider );
        }

        if ( threadSafeEnabled && !( outputProvider instanceof ThreadSafeWrapper ) )
        {
            LOGGER.trace( () -> "adding ThreadSafeWrapper to provider instance" );
            outputProvider = ThreadSafeWrapper.forProvider( outputProvider );
        }

        return outputProvider;
    }

    public static ChaiProviderFactory newProviderFactory()
    {
        return new ChaiProviderFactory( ChaiProviderFactorySetting.getDefaultSettings() );
    }

    public static ChaiProviderFactory newProviderFactory( final Map<ChaiProviderFactorySetting, String> settings )
    {
        final Map<ChaiProviderFactorySetting, String> effectiveSettings = new LinkedHashMap<>( ChaiProviderFactorySetting.getDefaultSettings() );
        if ( settings != null )
        {
            effectiveSettings.putAll( settings );
        }
        return new ChaiProviderFactory( Collections.unmodifiableMap( effectiveSettings ) );
    }

    public Map<ChaiProviderFactorySetting, String> getChaiProviderFactorySettings()
    {
        return chaiProviderFactorySettingStringMap;
    }

    CentralService getCentralService()
    {
        return centralService;
    }

    public Set<ChaiProvider> activeProviders()
    {
        return getCentralService().activeProviders();
    }

    public void close()
    {
        this.closed = true;

        this.centralService.close();

        for ( final ChaiProvider chaiProvider : activeProviders() )
        {
            chaiProvider.close();
        }
    }

    private void checkStatus()
    {
        if ( this.closed )
        {
            throw new IllegalStateException( "ChaiProviderFactory instance is closed, new providers can not be created" );
        }

    }

    static class CentralService implements Closeable
    {
        private final WatchdogService watchdogService;

        private final StatisticsWrapper.StatsBean globalStats = new StatisticsWrapper.StatsBean();

        // cache thread access isn't strictly locked but should be good enough for cache usage
        private final Map<String, VendorCacheInfo> vendorCacheMap = new ConcurrentHashMap<>();

        private final int maxVendorCacheAgeMs;

        private final WeakReferenceHolder<ChaiProviderImplementor> activeProviders = new WeakReferenceHolder<>();

        private CentralService( final ChaiProviderFactory chaiProviderFactory )
        {
            maxVendorCacheAgeMs = Integer.parseInt(
                    chaiProviderFactory.getChaiProviderFactorySettings().getOrDefault(
                            ChaiProviderFactorySetting.VENDOR_CACHE_MAX_AGE_MS,
                            ChaiProviderFactorySetting.VENDOR_CACHE_MAX_AGE_MS.getDefaultValue()
                    )
            );
            watchdogService = new WatchdogService( chaiProviderFactory );
        }

        void addVendorCache( final ChaiConfiguration chaiConfiguration, final DirectoryVendor vendor )
        {
            if ( maxVendorCacheAgeMs > 0 )
            {
                final String cacheKey = chaiConfiguration.getSetting( ChaiSetting.BIND_URLS );

                if ( !vendorCacheMap.containsKey( cacheKey ) )
                {
                    vendorCacheMap.put( cacheKey, new VendorCacheInfo( Instant.now(), vendor ) );
                }

                // safety check
                while ( vendorCacheMap.size() > 100 )
                {
                    vendorCacheMap.entrySet().iterator().remove();
                }
            }
        }

        @Override
        public void close()
        {
            watchdogService.close();
        }

        DirectoryVendor getVendorCache( final ChaiConfiguration chaiConfiguration )
        {
            final String cacheKey = chaiConfiguration.getSetting( ChaiSetting.BIND_URLS );
            final VendorCacheInfo vendorCacheInfo = vendorCacheMap.get( cacheKey );
            if ( vendorCacheInfo != null )
            {
                if ( vendorCacheInfo.getTimestamp().plusMillis( maxVendorCacheAgeMs ).isBefore( Instant.now() ) )
                {
                    vendorCacheMap.remove( cacheKey );
                }
                else
                {
                    return vendorCacheInfo.getVendor();
                }
            }
            return null;
        }

        StatisticsWrapper.StatsBean getStatsBean()
        {
            return globalStats;
        }

        WatchdogService getWatchdogService()
        {
            return watchdogService;
        }

        Set<ChaiProvider> activeProviders()
        {
            final Set<ChaiProvider> returnSet = new HashSet<>( activeProviders.allValues() );
            return Collections.unmodifiableSet( returnSet );
        }

        void registerProvider( final ChaiProviderImplementor chaiProviderImplementor )
        {
            activeProviders.add( chaiProviderImplementor );
        }

        void deRegisterProvider( final ChaiProviderImplementor chaiProviderImplementor )
        {
            activeProviders.remove( chaiProviderImplementor );
        }
    }

    private static class VendorCacheInfo
    {
        private final Instant timestamp;
        private final DirectoryVendor vendor;

        VendorCacheInfo( final Instant timestamp, final DirectoryVendor vendor )
        {
            this.timestamp = timestamp;
            this.vendor = vendor;
        }

        public Instant getTimestamp()
        {
            return timestamp;
        }

        public DirectoryVendor getVendor()
        {
            return vendor;
        }
    }

    static class WeakReferenceHolder<E>
    {
        private WeakHashMap<E, Object> internalWeakMap = new WeakHashMap<>();

        private final Object lock = new Object();

        void add( final E reference )
        {
            synchronized ( lock )
            {
                internalWeakMap.put( reference, null );
            }
        }

        void remove( final E reference )
        {
            synchronized ( lock )
            {
                internalWeakMap.remove( reference, null );
            }
        }

        Collection<E> allValues()
        {
            final Set<E> newSet;
            synchronized ( lock )
            {
                newSet = new HashSet<>( internalWeakMap.keySet() );
                newSet.remove( null );
            }
            return newSet;
        }
    }
}

