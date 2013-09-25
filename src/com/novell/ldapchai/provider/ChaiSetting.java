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

import com.novell.ldapchai.cr.Answer;

import java.io.Serializable;
import java.net.URI;

/**
 * All settings for LDAP Chai used by {@link ChaiConfiguration}.
 * <p>
 * Each setting contains:
 * <ul>
 * <li>A key value useful in external configurations</li>
 * <li>A default value for the setting</li>
 * <li>A validator to ensure that an assocated value is syntactically correct</li>
 * </ul>
 *
 * @see com.novell.ldapchai.provider.ChaiConfiguration
 * @author Jason D. Rivard
 */
public enum ChaiSetting {
    /**
     * A list of URLs to be used for ldap connections.  {@link #validateValue(String)} will be used to to ensure that
     * each provided value is a valid URL.
     * <p/>
     * Multiple ldap URLs can be specified by separating with a comma or space character.
     * <p/>
     * A value that does not conform to {@link URI#create(String)} requirements or has a scheme other than "ldap" or "ldaps"
     * will result in an {@link IllegalArgumentException}.
     * <table border="1">
     *  <tr><td><b>Examples</b></tr></td>
     *  <tr><td>ldap://192.168.10.1</tr></td>
     *  <tr><td>ldaps://192.168.10.2</tr></td>
     *  <tr><td>ldaps://192.168.10.3:6322</tr></td>
     *  <tr><td>ldap://host1.example.com:382</tr></td>
     *  <tr><td>ldap://host1.example.com,ldaps://host2.example.com,ldaps://10.8.31.1:6322</tr></td>
     *  <tr><td>ldaps://host1.example.com ldaps://10.8.31.1:6322</tr></td>
     * </table>
     * <p/>
     * Unless {@link #FAILOVER_ENABLE} is set to <i>true</i>, the use of values by a {@code ChaiProvider}
     * beyond the first value is unspecified.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.bind.URLs</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>ldap://localhost</td></tr>
     * </table>
     * @see #FAILOVER_ENABLE
     */
    BIND_URLS("chai.bind.URLs", "ldap://localhost", true, new Validator() {
        public void validate(final String value)
        {
            if (value == null || value.length() < 0) {
                throw new IllegalArgumentException("at least one valid ldap URL is required");
            }

            for (final String s : value.split(ChaiConfiguration.LDAP_URL_SEPERATOR_REGEX_PATTERN)) {
                final URI theURI;
                try {
                    theURI = URI.create(s);  // test uri
                    if (!"ldap".equalsIgnoreCase(theURI.getScheme()) && !"ldaps".equalsIgnoreCase(theURI.getScheme())) {
                        throw new IllegalArgumentException();
                    }
                } catch (IllegalArgumentException e) {
                    final StringBuffer sb = new StringBuffer();
                    sb.append("ldap server url \"").append(s);
                    sb.append("\" does not have proper url syntax such as \"ldap://127.0.0.1\" or \"ldaps://ldap.example.com\" ");
                    sb.append("[").append(e.getMessage()).append("]");
                    throw new IllegalArgumentException(sb.toString());
                }
            }
        }
    }),

    /**
     * Set the bind DN to be used during connection establishment.  If empty, an anonymous connection
     * will be attempted.  Valid values should be fully qualified ldap syntax, such as:
     * <pre>
     * cn=administrator,ou=container,o=Organization
     * </pre>
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.bind.dn</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>(blank)</td></tr>
     * </table>
     */
    BIND_DN("chai.bind.dn", "", true, null),

    /**
     * Password to use during chai LDAP bind.  An empty password will cause an anonymous connection
     * to be used.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.bind.password</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>(blank)</td></tr>
     * </table>
     */
    BIND_PASSWORD("chai.bind.password", "", false, null),

    /**
     * Enables ldap data caching.  Calls to the provider will
     * be cached and subsequent calls will be returned from memory instead of being fetched
     * from the ldap server.
     * <p/>
     * The wrapped provider is intended for "request scope" type operations, and allows routines that make
     * redundent ldap calls to be more efficient.
     * <p/>
     * The cache is limited in size, and beyond a specific amount of cache entries, results are moved
     * to a collection using {@link java.lang.ref.WeakReference}s that are subject to garbage collection.  This provides some protection
     * against memory leaks.
     * <p/>
     * Only read operations are cached.  Performing any operation which may cause a modify (such as
     * {@link ChaiProvider#createEntry(String, String, java.util.Map)} or
     * {@link ChaiProvider#writeStringAttribute(String, String, java.util.Set, boolean)} ) will cause
     * the cache to be cleared.
     * <p/>
     * There are many ways for this behavior to cause problems.  Enabling this setting should be done
     * with caution and extensive testing.
     * In particular, no cache synchronization is performed between providers, which can cause unexpected
     * results if not carefully gaurded against.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.cache.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    CACHE_ENABLE("chai.cache.enable", "false", true, null), 

    /**
     * Maximum number of cached results to retain in memory.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.cache.maximumSize</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>128</td></tr>
     * </table>
     */
    CACHE_MAXIMUM_SIZE("chai.cache.maximumSize", "128", true, Validator.INTEGER_VALIDATOR),

    /**
     * Maximum amount of time to cache results.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.cache.maximumAge</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>1000</td></tr>
     * </table>
     */
    CACHE_MAXIMUM_AGE("chai.cache.maximumAge", "1000", true, Validator.INTEGER_VALIDATOR),

    /**
     * Enable chai statistics.  If enabled, each ChaiProvider will maintain statistics and make them
     * available via {@link com.novell.ldapchai.provider.ChaiProvider#getProviderStatistics()}.
     * <p/>
     * <i>Default: </i><b>true</b>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.statistics.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     *
     * @see ProviderStatistics
     */
    STATISTICS_ENABLE("chai.statistics.enable", "true", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Enable watchdog idle protection.  If this value is set to "true", then the {@code ChaiProvider} will automatically
     * close the ldap connection after their has been no activity for some duration of time.  If another
     * ldap api is called after the idle connection has caused a close, the {@code ChaiProvider} will automatically
     * re-open a new connection using the originally supplied connection settings and credentials.
     * <p/>
     * Note that if this setting is enabled, a lightweight watchdog thread will be running so long as their are any
     * {@code ChaiProvider} instances open.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.watchdog.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     */
    WATCHDOG_ENABLE("chai.watchdog.enable", "true", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Maximum time an operation can be in progress (in ms).  If this time is exceeded, the connection will
     * be closed.  Future ldap api's called to the ChaiProvider will attempt to re-open a new
     * ldap connection.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.watchdog.operationTimeout</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>60000</td></tr>
     * </table>
     */
    WATCHDOG_OPERATION_TIMEOUT("chai.watchdog.operationTimeout", "60000", true, Validator.INTEGER_VALIDATOR),


    /**
     * Maximum time a ChaiProvider can remain inactive (in ms).  If this time is exceeded, the connection will
     * be closed.  Future ldap api's called to the ChaiProvider will attempt to re-open a new
     * ldap connection.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.watchdog.idleTimeout</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>30000</td></tr>
     * </table>
     */
    WATCHDOG_IDLE_TIMEOUT("chai.watchdog.idleTimeout", "30000", true, Validator.INTEGER_VALIDATOR),

    /**
     * The frequency that watchdog timeouts are checked (in ms).  The watchdog implementation is only guarenteed
     * to check timeouts at this frequency.  This will have a direct impact on the enforcement of timeouts.  For
     * example, if the idle timeout is 30 seconds, and the frequency is 30 seconds, then connections may actually
     * be able to remain idle between 30 and 60 seconds.
     * <p/>
     * Note that this setting MUST be set before any ChaiProvider instances are created.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.connection.watchdog.frequency</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>5000</td></tr>
     * </table>
     */
    WATCHDOG_DISABLE_IF_PW_EXPIRED("chai.watchdog.disableIfPwExpired", "true", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * The frequency that watchdog timeouts are checked (in ms).  The watchdog implementation is only guarenteed
     * to check timeouts at this frequency.  This will have a direct impact on the enforcement of timeouts.  For
     * example, if the idle timeout is 30 seconds, and the frequency is 30 seconds, then connections may actually
     * be able to remain idle between 30 and 60 seconds.
     * <p/>
     * Note that this setting MUST be set before any ChaiProvider instances are created.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.connection.watchdog.frequency</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>5000</td></tr>
     * </table>
     */
    WATCHDOG_CHECK_FREQUENCY("chai.connection.watchdog.frequency", "5000", true, Validator.INTEGER_VALIDATOR),

    /**
     * Sets the Chai Provider to use a promiscuous SSL socket factory when making ldaps connections.  By default
     * this settings is false, which means that when making an SSL connection, the remote certificate must be
     * trusted by the local JVM keystore.
     * <p/>
     * This setting is intented for use with development environments only, and should not be enabled for
     * production usage.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.connection.promiscuousSSL</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    PROMISCUOUS_SSL("chai.connection.promiscuousSSL", "false", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Enable wire trace debugging.  This will cause all data in/out of chai to be output to the
     * log4j TRACE debug level.  This will generate a large volume of debug messages.
     * <p/>
     * <b>WARNING:</b> Enabling this setting will cause all data values to be output to debug mode, including
     * sensitive values such as passwords.  Enable this setting with care!
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.wireDebug.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    WIRETRACE_ENABLE("chai.wireDebug.enable", "false", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Enable fail over when multiple servers are present.  Also allows retries to a single server
     * in case of connection problems.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.failover.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     */
    FAILOVER_ENABLE("chai.failover.enable", "true", true, Validator.BOOLEAN_VALIDATOR),


    /**
     * Minimum time Chai will wait before retrying a server marked as down.  Time is in milliseconds.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.failover.failBackTime</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>90000</td></tr>
     * </table>
     */
    FAILOVER_MINIMUM_FAILBACK_TIME("chai.failover.failBackTime", "90000", true, Validator.INTEGER_VALIDATOR),

    /**
     * Minimum number of attempts Chai will make to contact a server if there is a communication problem.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.failover.connectRetries</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>4</td></tr>
     * </table>
     */
    FAILOVER_CONNECT_RETRIES("chai.failover.connectRetries", "4", true, Validator.INTEGER_VALIDATOR),

    /**
     * Configure alias handling.  By default, alias dereferencing is set to "never", so aliases
     * are effectively ignored.  Valid settings are the same as those supported by JNDI:
     * <ul><li>always</li><li>never</li><li>finding</li><li>searching</li></ul>
     * See <a href="http://java.sun.com/products/jndi/tutorial/ldap/misc/aliases.html">JNDI alias
     * documentation</a>
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ldap.dereferenceAliases</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>never</td></tr>
     * </table>
     */
    LDAP_DEREFENCE_ALIAS("chai.ldap.dereferenceAliases", "never", true, null),


    /**
     * Ldap socket timeout, if supported by the ChaiProvider implementation.  Time is in milliseconds.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ldap.ldapTimeout</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>5000</td></tr>
     * </table>
     */
    LDAP_CONNECT_TIMEOUT("chai.ldap.ldapTimeout", "5000", true, Validator.INTEGER_VALIDATOR),

    /**
     * Enable LDAP referral following.  Valid settings are "true" or "false".
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ldap.followReferrals</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    LDAP_FOLLOW_REFERRALS("chai.ldap.followReferrals", "false", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Set the fully qualified class name of the {@code ChaiProvider} class name to be used.  By default this is
     * the class name for the {@link JNDIProviderImpl} class.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.provider.implementation</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>com.novell.ldapchai.provider.JNDIProviderImpl</td></tr>
     * </table>
     *
     * @see com.novell.ldapchai.provider.JNDIProviderImpl, com.novell.ldapchai.provider.JLDAPProviderImpl
     */
    PROVIDER_IMPLEMENTATION("chai.provider.implementation", JNDIProviderImpl.class.getName(), true, null),

    /**
     * Enable NMAS support for Novell eDirectory.  NMAS support makes some operations more efficient,
     * and provides more descriptive error messages.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.edirectory.enableNMAS</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    EDIRECTORY_ENABLE_NMAS("chai.edirectory.enableNMAS", "false", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Cache failed operations due to unknown extended operations.  Once an unknown extended operation for a
     * given {@link ChaiProvider} occurs it will not be retried.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.provider.extendedOperation.failureCache</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     */
    EXTENDED_OPERATION_FAILURE_CACHE("chai.provider.extendedOperation.failureCache", "true", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Set to read-only mode.   When enabled, no "write" opertions will be permitted, and will fail with an {@link com.novell.ldapchai.exception.ChaiOperationException} of
     * type {@link com.novell.ldapchai.exception.ChaiError#READ_ONLY_VIOLATION}.  This error will also be occured for operations that are only potentially "write"
     * operations such s {@link ChaiProvider#extendedOperation(javax.naming.ldap.ExtendedRequest)}.
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.provider.readonly</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    READONLY("chai.provider.readonly", "false", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Specify a default directory vendor.  If not empty, {@link ChaiProvider} implementations <b>should</b> always
     * return the configured value regardless of the actual directory type when {@link ChaiProvider#getDirectoryVendor()} 
     * is called.
     * <p/>
     * The value must exactly match a known value for {@link com.novell.ldapchai.provider.ChaiProvider.DIRECTORY_VENDOR}.
     * <p/>
     * <i>Default: </i><b>(blank)</b>
     * <p/>
     * <table border="0">
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.vendor.default</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>(blank)</td></tr>
     * </table>
     */
    DEFAULT_VENDOR("chai.vendor.default", "", true, new Validator() {
        public void validate(final String value) {
            if (value == null || value.length() < 1) {
                return;
            }

            for (final ChaiProvider.DIRECTORY_VENDOR vendor : ChaiProvider.DIRECTORY_VENDOR.values()) {
                if (vendor.toString().equals(value)) {
                    return;
                }
            }

            throw new IllegalArgumentException("value must match a known directory vendor");
        }
    }
    ),

    JNDI_ENABLE_POOL("chai.provider.jndi.enablePool", "true", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Case insensitive flag.  If true, the case of the responses will be ignored when tested.  Default is true.
     */
    CR_CASE_INSENSITIVE("chai.crsetting.caseInsensitive", "true",true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Allow duplicate response values to be used.  Default is false.
     */
    CR_ALLOW_DUPLICATE_RESPONSES("chai.crsetting.allowDuplicateResponses", "false", true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Default Chai CR Format Type.  Must be a valid string value of {@link com.novell.ldapchai.cr.Answer.FormatType}
     */
    CR_DEFAULT_FORMAT_TYPE("chai.crsetting.defaultFormatType", Answer.FormatType.SHA1_SALT.toString(), true, Validator.BOOLEAN_VALIDATOR),

    /**
     * Setting key to control the ldap attribute name used when reading/writing Chai Challenge/Response formats.
     * <p/>
     * <i>Default: </i><b>carLicense</b>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     * @see com.novell.ldapchai.util.ConfigObjectRecord
     */
    CR_CHAI_STORAGE_ATTRIBUTE("chai.cr.chai.attributeName", "carLicense", true, null),

    /**
     * Setting key to control the {@link com.novell.ldapchai.util.ConfigObjectRecord COR}
     * RecordType used when reading/writing Chai Challenge/Response formats.
     * <p/>
     * <i>Default: </i><b>0002</b>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     * @see com.novell.ldapchai.util.ConfigObjectRecord
     */
    CR_CHAI_STORAGE_RECORD_ID("chai.cr.chai.recordId", "0002", true, null),

    /**
     * Setting key to control the number of iterations to perform the CR Salt when the
     * format type is set to a hash type that allows for multiple iterations such as {@link com.novell.ldapchai.cr.Answer.FormatType#SHA1_SALT}
     * <p/>
     * <i>Default: </i><b>1000</b>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     * @see com.novell.ldapchai.util.ConfigObjectRecord
     */
    CR_CHAI_SALT_COUNT("chai.cr.chai.saltCount", "1000", true, Validator.INTEGER_VALIDATOR),


    ;

// ------------------------------ FIELDS ------------------------------

    private final String key;
    private final String defaultValue;
    private final boolean visible;
    private final Validator validator;

// -------------------------- STATIC METHODS --------------------------

    /**
     *
     * For a given key, find the associated setting.  If no setting matches
     * the supplied key, null is returned.
     * @param key string value of the setting's key.
     * @return the setting associated witht the <i>key</i>, or <i>null</i>.
     * @see #getKey()
     */
    public static ChaiSetting forKey(final String key)
    {
        for (final ChaiSetting setting : ChaiSetting.values()) {
            if (setting.getKey().equals(key)) {
                return setting;
            }
        }
        return null;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    ChaiSetting(final String key, final String defaultValue, final boolean visible, final Validator validator)
    {
        this.key = key;
        this.defaultValue = defaultValue;
        this.validator = validator;
        this.visible = visible;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * Returns the default value for this setting.  If no other value is configured, then the
     * default value is used
     *
     * @return the default value
     */
    public String getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Get the key name, suitable for use in a {@link java.util.Properties} instance
     *
     * @return key name
     */
    public String getKey()
    {
        return key;
    }

    boolean isVisible()
    {
        return this.visible;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Validates the syntactical structure of the value.  Useful for pre-testing a value to see
     * if it meets requirements.
     *
     * @param value the value to test
     * @throws IllegalArgumentException if the value is not syntactically correct
     */
    public void validateValue(final String value)
    {
        if (this.validator == null) {
            return;
        }
        this.validator.validate(value);
    }

// -------------------------- INNER CLASSES --------------------------

    private interface Validator extends Serializable {
        void validate(String value);

        static final Validator INTEGER_VALIDATOR = new Validator() {
            public void validate(final String value)
            {
                try {
                    Integer.parseInt(value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        };

        static final Validator BOOLEAN_VALIDATOR = new Validator() {
            public void validate(final String value)
            {
                try {
                    Boolean.parseBoolean(value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        };

        static final Validator CR_FORMAT_VALIDATOR = new Validator() {
            public void validate(final String value) {
                try {
                    if (Answer.FormatType.valueOf(value) == null) {
                        throw new IllegalArgumentException("unknown ChaiResponseSet.FormatType");
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        };
    }
}
