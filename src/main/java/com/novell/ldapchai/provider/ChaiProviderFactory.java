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

import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiErrors;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.ChaiLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * Static factory methods for obtaining an {@code ChaiProvider}.  {@code ChaiProvider} instances can be
 * used directly, but they are more commonly used to obtain an {@link com.novell.ldapchai.ChaiEntry}
 * instance.
 * <p/>
 * Access to "wrapped" {@code ChaiProvider}s are also availble, including wrappers for caching
 * or synchronization.
 * <p/>
 * If there are no specific requirements for how to establish a connection, the
 * "quick" factory method can be used to get a Provider with just one line of code:
 * <p/>
 * <hr/><blockquote><pre>
 *   ChaiProvider provider = ChaiProviderFactory.createProvider("ldap://host:port","cn=admin,o=org","password");
 * </pre></blockquote><hr/>
 * <p/>
 * If a more control is required, allocate a {@code ChaiConfiguration} first, and then
 * use this factory to generate a provider:
 * <p/>
 * <hr/><blockquote><pre>
 *   // setup connecton variables
 *   final String bindUsername = "cn=admin,o=org";
 *   final String bindPassword = "password";
 *   final List<String> serverURLs = new ArrayList<String>();
 *   serverURLs.add("ldap://server1:port");
 *   serverURLs.add("ldaps://server2:port");
 *   serverURLs.add("ldap://server3");
 * <p/>
 *   // allocate a new configuration
 *   ChaiConfiguration chaiConfig = new ChaiConfiguration(
 *            serverURLs,
 *            bindUsername,
 *            bindPassword);
 * <p/>
 *   // set any desired settings.
 *   chaiConfig.setSettings(ChaiConfiguration.SETTING_LDAP_TIMEOUT,"9000");
 *   chaiConfig.setSettings(ChaiConfiguration.SETTING_PROMISCUOUS_SSL,"true");
 * <p/>
 *   // generate the new provider
 *   ChaiProvider provider = ChaiProviderFactory.createProvider(chaiConfig);
 * </pre></blockquote><hr/>
 *
 * @author Jason D. Rivard
 */
public final class ChaiProviderFactory {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    /**
     * Log4j logger
     */
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(ChaiProviderFactory.class.getName());

// -------------------------- STATIC METHODS --------------------------

    /**
     * Maintains the global chai provider statistics.  All {@code com.novell.ldapchai.provider.ChaiProvider} instaces
     * that have their {@link ChaiSetting#STATISTICS_ENABLE} set to <i>true</i> will register statistics in
     * this global tracker.
     *
     * @return a ProviderStatistics instance containing global statistics for the Chai API
     */
    public static ProviderStatistics getChaiStatistics()
    {
        return StatisticsWrapper.getGlobalStatistics();
    }

    /**
     * Create a {@code ChaiUser} using a standard JNDI ChaiProvider.  If access to the ChaiProvider is also required, it can be had
     * using the {@link com.novell.ldapchai.ChaiUser#getChaiProvider()} method of the returned ChaiUser instance.
     *
     * @param bindDN   ldap bind DN, in ldap fully qualified syntax.  Also used as the DN of the returned ChaiUser.
     * @param password password for the bind DN.
     * @param ldapURL  ldap server and port in url format, example: <i>ldap://127.0.0.1:389</i> (multiple addresses can be specified by comma seperating)
     * @return A {@code ChaiUser} instance of the bindDN with an underlying ChaiProvider connected using the supplied parameters.
     * @throws ChaiUnavailableException If the directory server(s) are not reachable.
     * @see com.novell.ldapchai.ChaiFactory#quickProvider(String,String,String)
     */
    public static ChaiUser quickProvider(final String ldapURL, final String bindDN, final String password)
            throws ChaiUnavailableException
    {
        final ChaiProvider provider = createProvider(ldapURL, bindDN, password);
        return ChaiFactory.createChaiUser(bindDN, provider);
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
    public static ChaiProvider createProvider(final String ldapURL, final String bindDN, final String password)
            throws ChaiUnavailableException
    {
        final ChaiConfiguration chaiConfig = new ChaiConfiguration(ldapURL, bindDN, password);
        chaiConfig.setSetting(ChaiSetting.PROVIDER_IMPLEMENTATION, JNDIProviderImpl.class.getName());

        return createProvider(chaiConfig);
    }

    /**
     * Create a {@code ChaiProvider} using the specified <i>chaiConfiguration</i>.  A ChaiProvider will be created
     * according to the implementation class and configuration settings contained within the configuration.
     * <p/>
     * All other factory methods are simply convenience wrappers around this method.
     *
     * @param chaiConfiguration A completed, lockable configuration
     * @return A functioning ChaiProvider generated according to <i>chaiConfiguration</i>
     * @throws ChaiUnavailableException If the directory server(s) are not reachable.
     */
    public static ChaiProvider createProvider(final ChaiConfiguration chaiConfiguration)
            throws ChaiUnavailableException
    {
        chaiConfiguration.lock();

        ChaiProviderImplementor providerImpl;
        try {
            final boolean enableFailover = chaiConfiguration.getSetting(ChaiSetting.FAILOVER_ENABLE).equalsIgnoreCase("true");

            if (enableFailover) {
                providerImpl = FailOverWrapper.forConfiguration(chaiConfiguration);
            } else {
                if (LOGGER.isTraceEnabled()) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("creating new jndi ldap connection to ");
                    sb.append(chaiConfiguration.getSetting(ChaiSetting.BIND_URLS));
                    sb.append(" as ");
                    sb.append(chaiConfiguration.getSetting(ChaiSetting.BIND_DN));
                    LOGGER.trace(sb.toString());
                }

                providerImpl = createConcreateProvider(chaiConfiguration, true);
            }
        } catch (Exception e) {
            LOGGER.debug("unable to create connection: " + e.getClass().getName() + ":" + e.getMessage());
            throw new ChaiUnavailableException("unable to create connection: " + e.getMessage(), ChaiErrors.getErrorForMessage(e.getMessage()));
        }

        providerImpl = addProviderWrappers(providerImpl);

        return providerImpl;
    }

    static ChaiProviderImplementor createConcreateProvider(
            final ChaiConfiguration chaiConfiguration,
            final boolean initialize
    )
            throws Exception
    {
        final String className = chaiConfiguration.getSetting(ChaiSetting.PROVIDER_IMPLEMENTATION);

        final ChaiProviderImplementor providerImpl;

        final Class providerClass = Class.forName(className);
        final Object impl = providerClass.newInstance();
        if (!(impl instanceof ChaiProvider)) {
            throw new ChaiUnavailableException("unable to create new ChaiProvider, " + className + " is not instance of " + ChaiProvider.class.getName(), ChaiError.UNKNOWN);
        }
        if (!(impl instanceof ChaiProviderImplementor)) {
            throw new ChaiUnavailableException("unable to create new ChaiProvider, " + className + " is not instance of " + ChaiProviderImplementor.class.getName(), ChaiError.UNKNOWN);
        }
        providerImpl = (ChaiProviderImplementor) impl;

        if (initialize) {
            providerImpl.init(chaiConfiguration);
        }

        return providerImpl;
    }

    static ChaiProviderImplementor addProviderWrappers(ChaiProviderImplementor providerImpl)
    {
        final ChaiConfiguration chaiConfiguration = providerImpl.getChaiConfiguration();

        final boolean enableReadOnly =      "true".equalsIgnoreCase(chaiConfiguration.getSetting(ChaiSetting.READONLY));
        final boolean enableWatchdog =      "true".equalsIgnoreCase(chaiConfiguration.getSetting(ChaiSetting.WATCHDOG_ENABLE));
        final boolean enableWireTrace =     "true".equalsIgnoreCase(chaiConfiguration.getSetting(ChaiSetting.WIRETRACE_ENABLE));
        final boolean enableStatistics =    "true".equalsIgnoreCase(chaiConfiguration.getSetting(ChaiSetting.STATISTICS_ENABLE));
        final boolean enableCaching =       "true".equalsIgnoreCase(chaiConfiguration.getSetting(ChaiSetting.CACHE_ENABLE));

        if (enableReadOnly && !(providerImpl instanceof ReadOnlyWrapper)) {
            LOGGER.trace("adding ReadOnlyWrapper to provider instance");
            providerImpl = ReadOnlyWrapper.forProvider(providerImpl);
        }

        if (enableWatchdog && !(providerImpl instanceof WatchdogWrapper)) {
            LOGGER.trace("adding WatchdogWrapper to provider instance");
            providerImpl = WatchdogWrapper.forProvider(providerImpl);
        }

        if (enableWireTrace && !(providerImpl instanceof WireTraceWrapper)) {
            LOGGER.trace("adding WireTraceWrapper to provider instance");
            providerImpl = WireTraceWrapper.forProvider(providerImpl);
        }

        if (enableStatistics && !(providerImpl instanceof StatisticsWrapper)) {
            LOGGER.trace("adding StatisticsWrapper to provider instance");
            providerImpl = StatisticsWrapper.forProvider(providerImpl);
        }

        if (enableCaching && !(providerImpl instanceof CachingWrapper)) {
            LOGGER.trace("adding CachingWrapper to provider instance");
            providerImpl = CachingWrapper.forProvider(providerImpl);
        }

        return providerImpl;
    }

    /**
     * Returns a thread-safe "wrapped" {@code ChaiProvider}.  All ldap operations will be forced through a single
     * lock and then sent to the backing provider.
     * <p/>
     * Note that depending on the ldap server and the configured timeouts, calling methods on a synchronized
     * provider may result in significant blocking delays.
     *
     * @param theProvider The provider to be "wrapped" in a synchronized provider.
     * @return A synchronized view of the specified provider
     */
    public static ChaiProvider synchronizedProvider(final ChaiProvider theProvider)
    {
        if (theProvider instanceof SynchronizedProvider) {
            return theProvider;
        } else {
            return (ChaiProvider) Proxy.newProxyInstance(
                    theProvider.getClass().getClassLoader(),
                    theProvider.getClass().getInterfaces(),
                    new SynchronizedProvider(theProvider));
        }
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private ChaiProviderFactory()
    {
    }

// -------------------------- INNER CLASSES --------------------------

    private static class SynchronizedProvider implements InvocationHandler {
        private final ChaiProvider realProvider;
        private final Object lock = new Object();

        public SynchronizedProvider(final ChaiProvider realProvider)
        {
            this.realProvider = realProvider;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args)
                throws Throwable
        {
            if (method.getAnnotation(ChaiProviderImplementor.LdapOperation.class) != null) {
                synchronized (lock) {
                    return doInvocation(method, args);
                }
            } else {
                return doInvocation(method, args);
            }
        }

        public Object doInvocation(final Method method, final Object[] args)
                throws Throwable
        {
            try {
                return method.invoke(realProvider, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}

