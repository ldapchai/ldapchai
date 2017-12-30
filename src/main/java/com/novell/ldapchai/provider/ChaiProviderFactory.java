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
import com.novell.ldapchai.util.ChaiLogger;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
public final class ChaiProviderFactory
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( ChaiProviderFactory.class.getName() );

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
        return newProviderImpl( chaiConfiguration, false );
    }

    ChaiProviderImplementor newProviderImpl(
            final ChaiConfiguration chaiConfiguration,
            final boolean failOverWrapperChild
    )
            throws ChaiUnavailableException
    {
        checkStatus();

        ChaiProviderImplementor providerImpl;
        try
        {
            final boolean enableFailover = "true".equalsIgnoreCase( chaiConfiguration.getSetting( ChaiSetting.FAILOVER_ENABLE ) );

            if ( enableFailover && !failOverWrapperChild )
            {
                providerImpl = FailOverWrapper.forConfiguration( this, chaiConfiguration );
            }
            else
            {
                if ( LOGGER.isTraceEnabled() )
                {
                    final String debugMsg = "creating new jndi ldap connection to "
                            + chaiConfiguration.getSetting( ChaiSetting.BIND_URLS )
                            + " as "
                            + chaiConfiguration.getSetting( ChaiSetting.BIND_DN );
                    LOGGER.trace( debugMsg );
                }

                providerImpl = createConcreteProvider( this, chaiConfiguration, true );
            }
        }
        catch ( Exception e )
        {
            final String errorMsg = "unable to create connection: " + e.getClass().getName() + ":" + e.getMessage();
            if ( e instanceof ChaiException || e instanceof IOException )
            {
                LOGGER.debug( errorMsg );
            }
            else
            {
                LOGGER.debug( errorMsg, e );
            }
            throw new ChaiUnavailableException( "unable to create connection: " + e.getMessage(), ChaiErrors.getErrorForMessage( e.getMessage() ) );
        }

        if ( !failOverWrapperChild )
        {
            providerImpl = addProviderWrappers( providerImpl );
            getCentralService().registerProvider( providerImpl );
        }

        return providerImpl;
    }

    static ChaiProviderImplementor createConcreteProvider(
            final ChaiProviderFactory providerFactory,
            final ChaiConfiguration chaiConfiguration,
            final boolean initialize
    )
            throws Exception
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

    ChaiProviderImplementor addProviderWrappers( final ChaiProviderImplementor providerImpl )
    {
        final ChaiConfiguration chaiConfiguration = providerImpl.getChaiConfiguration();

        final boolean enableReadOnly = "true".equalsIgnoreCase( chaiConfiguration.getSetting( ChaiSetting.READONLY ) );
        final boolean enableWatchdog = "true".equalsIgnoreCase( chaiConfiguration.getSetting( ChaiSetting.WATCHDOG_ENABLE ) );
        final boolean enableWireTrace = "true".equalsIgnoreCase( chaiConfiguration.getSetting( ChaiSetting.WIRETRACE_ENABLE ) );
        final boolean enableStatistics = "true".equalsIgnoreCase( chaiConfiguration.getSetting( ChaiSetting.STATISTICS_ENABLE ) );
        final boolean enableCaching = "true".equalsIgnoreCase( chaiConfiguration.getSetting( ChaiSetting.CACHE_ENABLE ) );

        ChaiProviderImplementor outputProvider = providerImpl;
        if ( enableReadOnly && !( outputProvider instanceof ReadOnlyWrapper ) )
        {
            LOGGER.trace( "adding ReadOnlyWrapper to provider instance" );
            outputProvider = ReadOnlyWrapper.forProvider( outputProvider );
        }

        if ( enableWatchdog && !( outputProvider instanceof WatchdogWrapper ) )
        {
            LOGGER.trace( "adding WatchdogWrapper to provider instance" );
            outputProvider = WatchdogWrapper.forProvider( this, outputProvider );
        }

        if ( enableWireTrace && !( outputProvider instanceof WireTraceWrapper ) )
        {
            LOGGER.trace( "adding WireTraceWrapper to provider instance" );
            outputProvider = WireTraceWrapper.forProvider( outputProvider );
        }

        if ( enableStatistics && !( outputProvider instanceof StatisticsWrapper ) )
        {
            LOGGER.trace( "adding StatisticsWrapper to provider instance" );
            outputProvider = StatisticsWrapper.forProvider( outputProvider );
        }

        if ( enableCaching && !( outputProvider instanceof CachingWrapper ) )
        {
            LOGGER.trace( "adding CachingWrapper to provider instance" );
            outputProvider = CachingWrapper.forProvider( outputProvider );
        }

        return outputProvider;
    }

    /**
     * Returns a thread-safe "wrapped" {@code ChaiProvider}.  All ldap operations will be forced through a single
     * lock and then sent to the backing provider.
     * Depending on the ldap server and the configured timeouts, calling methods on a synchronized
     * provider may result in significant blocking delays.
     *
     * @param theProvider The provider to be "wrapped" in a synchronized provider.
     * @return A synchronized view of the specified provider
     */
    private static ChaiProvider synchronizedProvider( final ChaiProvider theProvider )
    {
        if ( theProvider instanceof SynchronizedProvider )
        {
            return theProvider;
        }
        else
        {
            return ( ChaiProvider ) Proxy.newProxyInstance(
                    theProvider.getClass().getClassLoader(),
                    theProvider.getClass().getInterfaces(),
                    new SynchronizedProvider( theProvider ) );
        }
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

    private static class SynchronizedProvider implements InvocationHandler
    {
        private final ChaiProvider realProvider;
        private final Object lock = new Object();

        SynchronizedProvider( final ChaiProvider realProvider )
        {
            this.realProvider = realProvider;
        }

        public Object invoke( final Object proxy, final Method method, final Object[] args )
                throws Throwable
        {
            if ( method.getAnnotation( ChaiProviderImplementor.LdapOperation.class ) != null )
            {
                synchronized ( lock )
                {
                    return doInvocation( method, args );
                }
            }
            else
            {
                return doInvocation( method, args );
            }
        }

        public Object doInvocation( final Method method, final Object[] args )
                throws Throwable
        {
            try
            {
                return method.invoke( realProvider, args );
            }
            catch ( InvocationTargetException e )
            {
                throw e.getCause();
            }
        }
    }

    public Map<ChaiProviderFactorySetting, String> getChaiProviderFactorySettings()
    {
        return Collections.unmodifiableMap( chaiProviderFactorySettingStringMap );
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

    static class CentralService
    {
        private final WatchdogService watchdogService;

        private final StatisticsWrapper.StatsBean globalStats = new StatisticsWrapper.StatsBean();

        private final Map<ChaiConfiguration, DirectoryVendor> vendorCacheMap = new LinkedHashMap<>();

        private final Set<WeakReference<ChaiProviderImplementor>> activeProviders = ConcurrentHashMap.newKeySet();

        private CentralService( final ChaiProviderFactory chaiProviderFactory )
        {
            watchdogService = new WatchdogService( chaiProviderFactory );
        }

        void addVendorCache( final ChaiConfiguration chaiConfiguration, final DirectoryVendor vendor )
        {
            vendorCacheMap.put( chaiConfiguration, vendor );
        }

        DirectoryVendor getVendorCache( final ChaiConfiguration chaiConfiguration )
        {
            return vendorCacheMap.get( chaiConfiguration );
        }

        StatisticsWrapper.StatsBean getStatsBean()
        {
            return globalStats;
        }

        WatchdogService getWatchdogService()
        {
            return watchdogService;
        }

        private Set<ChaiProvider> activeProviders()
        {
            final Set<ChaiProvider> returnSet = new HashSet<>( );
            for ( final WeakReference<ChaiProviderImplementor> reference : activeProviders )
            {
                final ChaiProviderImplementor implementor = reference.get();
                if ( implementor != null )
                {
                    returnSet.add( implementor );
                }
            }
            return Collections.unmodifiableSet( returnSet );
        }

        private void registerProvider( final ChaiProviderImplementor chaiProviderImplementor )
        {
            final WeakReference<ChaiProviderImplementor> reference = new WeakReference<>( chaiProviderImplementor );
            activeProviders.add( reference );
        }

        void deRegisterProvider( final ChaiProviderImplementor chaiProviderImplementor )
        {
            for ( final WeakReference<ChaiProviderImplementor> reference : activeProviders )
            {
                final ChaiProviderImplementor implementor = reference.get();
                if ( implementor == chaiProviderImplementor )
                {
                    activeProviders.remove( reference );
                }
            }
        }

    }
}

