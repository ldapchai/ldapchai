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

import com.novell.ldapchai.cr.Answer;

import java.net.URI;

/**
 * <p>All settings for LDAP Chai used by {@link ChaiConfiguration}.</p>
 *
 * <p>Each setting contains:</p>
 * <ul>
 * <li>A key value useful in external configurations</li>
 * <li>A default value for the setting</li>
 * <li>A validator to ensure that an assocated value is syntactically correct</li>
 * </ul>
 *
 * @author Jason D. Rivard
 * @see com.novell.ldapchai.provider.ChaiConfiguration
 */
public enum ChaiSetting
{
    /**
     * <p>A list of URLs to be used for ldap connections.  {@link #validateValue(String)} will be used to to ensure that
     * each provided value is a valid URL.</p>
     *
     * <p>Multiple ldap URLs can be specified by separating with a comma or space character.</p>
     *
     * <p>A value that does not conform to {@link URI#create(String)} requirements or has a scheme other than "ldap" or "ldaps"
     * will result in an {@link IllegalArgumentException}.</p>
     *
     * <table border="1"><caption>LDAP URL Examples</caption>
     * <tr><td><b>Examples</b></td></tr>
     * <tr><td>ldap://192.168.10.1</td></tr>
     * <tr><td>ldaps://192.168.10.2</td></tr>
     * <tr><td>ldaps://192.168.10.3:6322</td></tr>
     * <tr><td>ldap://host1.example.com:382</td></tr>
     * <tr><td>ldap://host1.example.com,ldaps://host2.example.com,ldaps://10.8.31.1:6322</td></tr>
     * <tr><td>ldaps://host1.example.com ldaps://10.8.31.1:6322</td></tr>
     * </table>
     *
     * <p>Unless {@link #FAILOVER_ENABLE} is set to <i>true</i>, the use of values by a {@code ChaiProvider}
     * beyond the first value is unspecified.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.bind.URLs</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>ldap://localhost</td></tr>
     * </table>
     *
     * @see #FAILOVER_ENABLE
     */
    BIND_URLS( "chai.bind.URLs", "ldap://localhost", true, ( SettingValidator.Validator ) value ->
    {
        if ( value == null || value.length() < 0 )
        {
            throw new IllegalArgumentException( "at least one valid ldap URL is required" );
        }

        for ( final String s : value.split( ChaiConfiguration.LDAP_URL_SEPARATOR_REGEX_PATTERN ) )
        {
            final URI theURI;
            try
            {
                // test uri
                theURI = URI.create( s );

                if ( !"ldap".equalsIgnoreCase( theURI.getScheme() ) && !"ldaps".equalsIgnoreCase( theURI.getScheme() ) )
                {
                    throw new IllegalArgumentException();
                }
            }
            catch ( IllegalArgumentException e )
            {
                final StringBuilder sb = new StringBuilder();
                sb.append( "ldap server url \"" ).append( s );
                sb.append( "\" does not have proper url syntax such as \"ldap://127.0.0.1\" or \"ldaps://ldap.example.com\" " );
                sb.append( "[" ).append( e.getMessage() ).append( "]" );
                throw new IllegalArgumentException( sb.toString() );
            }
        }
    } ),

    /**
     * <p>Set the bind DN to be used during connection establishment.  If empty, an anonymous connection
     * will be attempted.  Valid values should be fully qualified ldap syntax, such as:</p>
     *
     * <code>cn=administrator,ou=container,o=Organization</code>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.bind.dn</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>(none)</td></tr>
     * </table>
     */
    BIND_DN( "chai.bind.dn", "", true, null ),

    /**
     * <p>Password to use during chai LDAP bind.  An empty password will cause an anonymous connection
     * to be used.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.bind.password</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>(none)</td></tr>
     * </table>
     */
    BIND_PASSWORD( "chai.bind.password", "", false, null ),

    /**
     * <p>Enables ldap data caching.  Calls to the provider will
     * be cached and subsequent calls will be returned from memory instead of being fetched
     * from the ldap server.</p>
     *
     * <p>The wrapped provider is intended for "request scope" type operations, and allows routines that make
     * redundant ldap calls to be more efficient.</p>
     *
     * <p>The cache is limited in size, and beyond a specific amount of cache entries, results are moved
     * to a collection using {@link java.lang.ref.WeakReference}s that are subject to garbage collection.  This provides some protection
     * against memory leaks.</p>
     *
     * <p>Only read operations are cached.  Performing any operation which may cause a modify (such as
     * {@link ChaiProvider#createEntry(String, String, java.util.Map)} or
     * {@link ChaiProvider#writeStringAttribute(String, String, java.util.Set, boolean)} ) will cause
     * the cache to be cleared.</p>
     *
     * <p>There are many ways for this behavior to cause problems.  Enabling this setting should be done
     * with caution and extensive testing.
     * In particular, no cache synchronization is performed between providers, which can cause unexpected
     * results if not carefully guarded against.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.cache.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    CACHE_ENABLE( "chai.cache.enable", "false", true, null ),

    /**
     * <p>Maximum number of cached results to retain in memory.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.cache.maximumSize</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>128</td></tr>
     * </table>
     */
    CACHE_MAXIMUM_SIZE( "chai.cache.maximumSize", "128", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Maximum amount of time to cache results.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.cache.maximumAge</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>1000</td></tr>
     * </table>
     */
    CACHE_MAXIMUM_AGE( "chai.cache.maximumAge", "1000", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Enable chai statistics.  If enabled, each ChaiProvider will maintain statistics and make them
     * available via {@link com.novell.ldapchai.provider.ChaiProvider#getProviderStatistics()}.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.statistics.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     *
     * @see ProviderStatistics
     */
    STATISTICS_ENABLE( "chai.statistics.enable", "true", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Enable watchdog idle protection.  If this value is set to "true", then the {@code ChaiProvider} will automatically
     * close the ldap connection after their has been no activity for some duration of time.  If another
     * ldap api is called after the idle connection has caused a close, the {@code ChaiProvider} will automatically
     * re-open a new connection using the originally supplied connection settings and credentials.</p>
     *
     * <p>Note that if this setting is enabled, a lightweight watchdog thread will be running for each parent {@link ChaiProviderFactory}.
     * So it is best to have only a single {@code ChaiProviderFactory} instance if possible.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.watchdog.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     */
    WATCHDOG_ENABLE( "chai.watchdog.enable", "true", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Enable thread safe locking API.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.threadSafe.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     */
    THREAD_SAFE_ENABLE( "chai.threadSafe.enable", "true", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Maximum time an operation can be in progress (in ms).  If this time is exceeded, the connection will
     * be closed.  Future ldap api's called to the ChaiProvider will attempt to re-open a new
     * ldap connection.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.watchdog.operationTimeout</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>60000</td></tr>
     * </table>
     */
    WATCHDOG_OPERATION_TIMEOUT( "chai.watchdog.operationTimeout", "60000", true, SettingValidator.INTEGER_VALIDATOR ),
    
    /**
     * <p>Maximum time a ChaiProvider can remain inactive (in ms).  If this time is exceeded, the connection will
     * be closed.  Future methods called to the ChaiProvider will attempt to re-open a new
     * ldap connection.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.watchdog.idleTimeout</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>30000</td></tr>
     * </table>
     */
    WATCHDOG_IDLE_TIMEOUT( "chai.watchdog.idleTimeout", "30000", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Maximum time a ChaiProvider can remain in use(in ms).  If this time is exceeded, the connection will
     * be closed and re-opened.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.watchdog.maxConnectionLifetime</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>30000</td></tr>
     * </table>
     */
    WATCHDOG_MAX_CONNECTION_LIFETIME( "chai.watchdog.maxConnectionLifetime", "3300000", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Disable watchdog timeout on providers where the bind URL's principal has its password expired.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.connection.watchdog.disableIfPwExpired</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>5000</td></tr>
     * </table>
     */
    WATCHDOG_DISABLE_IF_PW_EXPIRED( "chai.watchdog.disableIfPwExpired", "true", true, SettingValidator.BOOLEAN_VALIDATOR ),


    /**
     * <p>Sets the Chai Provider to use a promiscuous SSL socket factory when making ldaps connections.  By default
     * this settings is false, which means that when making an SSL connection, the remote certificate must be
     * trusted by the local JVM keystore.</p>
     *
     * <p>This setting is indented for use with development environments only, and should not be enabled for
     * production usage.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.connection.promiscuousSSL</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    PROMISCUOUS_SSL( "chai.connection.promiscuousSSL", "false", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Enable wire trace debugging.  This will cause all data in/out of chai to be output to the
     * log4j TRACE debug level.  This will generate a large volume of debug messages.</p>
     *
     * <p><b>WARNING:</b> Enabling this setting will cause all data values to be output to debug mode, including
     * sensitive values such as passwords.  Enable this setting with care!</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.wireDebug.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    WIRETRACE_ENABLE( "chai.wireDebug.enable", "false", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Enable fail over when multiple servers are present.  Also allows retries to a single server
     * in case of connection problems.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.failover.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     */
    FAILOVER_ENABLE( "chai.failover.enable", "true", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Indicates if the failover engine will use it's "last known good" mechanism.  When a new connection is made, if
     * this setting is enabled, the provider will start with the last known good server instead of the first server in
     * the connection url list.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.failover.useLastKnownGoodHint</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     */
    FAILOVER_USE_LAST_KNOWN_GOOD_HINT( "chai.failover.useLastKnownGoodHint", "true", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Minimum time Chai will wait before retrying a server marked as down.  Time is in milliseconds.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.failover.failBackTime</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>90000</td></tr>
     * </table>
     */
    FAILOVER_MINIMUM_FAILBACK_TIME( "chai.failover.failBackTime", "90000", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Minimum number of attempts Chai will make to contact a server if there is a communication problem.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.failover.connectRetries</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>4</td></tr>
     * </table>
     */
    FAILOVER_CONNECT_RETRIES( "chai.failover.connectRetries", "4", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Configure alias handling.  By default, alias de-referencing is set to "never", so aliases
     * are effectively ignored.  Valid settings are the same as those supported by JNDI:</p>
     *
     * <ul><li>always</li><li>never</li><li>finding</li><li>searching</li></ul>
     * See <a href="http://java.sun.com/products/jndi/tutorial/ldap/misc/aliases.html">JNDI alias
     * documentation</a>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ldap.dereferenceAliases</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>never</td></tr>
     * </table>
     */
    LDAP_DEREFENCE_ALIAS( "chai.ldap.dereferenceAliases", "never", true, null ),


    /**
     * <p>Ldap socket timeout, if supported by the ChaiProvider implementation.  Time is in milliseconds.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ldap.ldapTimeout</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>5000</td></tr>
     * </table>
     */
    LDAP_CONNECT_TIMEOUT( "chai.ldap.ldapTimeout", "5000", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Ldap read timeout, if supported by the ChaiProvider implementation.  Time is in milliseconds.
     * A value of zero will leave the default value of the implementation.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ldap.ldapReadTimeout</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>5000</td></tr>
     * </table>
     */
    LDAP_READ_TIMEOUT( "chai.ldap.ldapReadTimeout", "0", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Enable LDAP referral following.  Valid settings are "true" or "false".</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ldap.followReferrals</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    LDAP_FOLLOW_REFERRALS( "chai.ldap.followReferrals", "false", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Set the fully qualified class name of the {@code ChaiProvider} class name to be used.  By default this is
     * the class name for the {@link JNDIProviderImpl} class.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.provider.implementation</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>com.novell.ldapchai.provider.JNDIProviderImpl</td></tr>
     * </table>
     *
     * @see com.novell.ldapchai.provider.JNDIProviderImpl
     * @see com.novell.ldapchai.provider.JLDAPProviderImpl
     * @see ApacheLdapProviderImpl
     */
    PROVIDER_IMPLEMENTATION( "chai.provider.implementation", JNDIProviderImpl.class.getName(), true, null ),

    /**
     * <p>Enable NMAS support for Novell eDirectory.  NMAS support makes some operations more efficient,
     * and provides more descriptive error messages.  NMAS support requires libraries from the NMAS SDK.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.edirectory.enableNMAS</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    EDIRECTORY_ENABLE_NMAS( "chai.edirectory.enableNMAS", "false", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Cache failed operations due to unknown extended operations.  Once an unknown extended operation for a
     * given {@link ChaiProvider} occurs it will not be retried.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.provider.extendedOperation.failureCache</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     */
    EXTENDED_OPERATION_FAILURE_CACHE( "chai.provider.extendedOperation.failureCache", "true", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Set to read-only mode.   When enabled, no "write" opertions will be permitted, and will fail with an {@link com.novell.ldapchai.exception.ChaiOperationException} of
     * type {@link com.novell.ldapchai.exception.ChaiError#READ_ONLY_VIOLATION}.  This error will also be occurred for operations that are only potentially "write"
     * operations such s {@link ChaiProvider#extendedOperation(javax.naming.ldap.ExtendedRequest)}.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.provider.readonly</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    READONLY( "chai.provider.readonly", "false", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Specify a default directory vendor.  If not empty, {@link ChaiProvider} implementations <b>should</b> always
     * return the configured value regardless of the actual directory type when {@link ChaiProvider#getDirectoryVendor()}
     * is called.</p>
     *
     * <p>The value must exactly match a known value for {@link DirectoryVendor}.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.vendor.default</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>(none)</td></tr>
     * </table>
     */
    DEFAULT_VENDOR( "chai.vendor.default", "", true, ( SettingValidator.Validator ) value ->
    {
        if ( value == null || value.length() < 1 )
        {
            return;
        }

        for ( final DirectoryVendor vendor : DirectoryVendor.values() )
        {
            if ( vendor.toString().equals( value ) )
            {
                return;
            }
        }

        throw new IllegalArgumentException( "value must match a known directory vendor" );
    }
    ),

    JNDI_ENABLE_POOL( "chai.provider.jndi.enablePool", "false", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Case insensitive challenge/responses.  If true, the case of the responses will be ignored when tested.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.crsetting.caseInsensitive</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     */
    CR_CASE_INSENSITIVE( "chai.crsetting.caseInsensitive", "true", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * Allow duplicate response values to be used.
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.crsetting.allowDuplicateResponses</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    CR_ALLOW_DUPLICATE_RESPONSES( "chai.crsetting.allowDuplicateResponses", "false", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * Default Chai CR Format Type.  Must be a valid string value of {@link com.novell.ldapchai.cr.Answer.FormatType}
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.crsetting.defaultFormatType</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>SHA1_SALT</td></tr>
     * </table>
     */
    CR_DEFAULT_FORMAT_TYPE( "chai.crsetting.defaultFormatType", Answer.FormatType.PBKDF2_SHA512.toString(), true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Setting key to control the ldap attribute name used when reading/writing Chai Challenge/Response formats.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.com.novell.ldapchai.cr.chai.attributeName</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>carLicense</td></tr>
     * </table>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     * @see com.novell.ldapchai.util.ConfigObjectRecord
     */
    CR_CHAI_STORAGE_ATTRIBUTE( "chai.com.novell.ldapchai.cr.chai.attributeName", "carLicense", true, null ),

    /**
     * <p>Setting key to control the {@link com.novell.ldapchai.util.ConfigObjectRecord COR}
     * RecordType used when reading/writing Chai Challenge/Response formats.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.com.novell.ldapchai.cr.chai.recordId</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>0002</td></tr>
     * </table>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     * @see com.novell.ldapchai.util.ConfigObjectRecord
     */
    CR_CHAI_STORAGE_RECORD_ID( "chai.com.novell.ldapchai.cr.chai.recordId", "0002", true, null ),

    /**
     * <p>Setting key to control the number of iterations to perform the CR Salt when the
     * format type is set to a hash type that allows for multiple iterations such as {@link com.novell.ldapchai.cr.Answer.FormatType#SHA1_SALT}.</p>
     *
     * <p>Each {@link Answer.FormatType} of answer has a {@link Answer.FormatType#getDefaultIterations() value.   This setting can override
     * the default iteration count.  If this setting value is a positive integer, it will override the default.}</p>
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.com.novell.ldapchai.cr.chai.iterations</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>0</td></tr>
     * </table>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     * @see com.novell.ldapchai.util.ConfigObjectRecord
     */
    CR_CHAI_ITERATIONS( "chai.com.novell.ldapchai.cr.chai.iterations", "0", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Setting key to control the number of salt alphanumeric characters to use for the response salt. to perform the CR Salt when the
     * format type is set to a hash type that allows for multiple iterations such as {@link com.novell.ldapchai.cr.Answer.FormatType#SHA1_SALT}.</p>
     *
     * <p>Each {@link Answer.FormatType} of answer has a {@link Answer.FormatType#getDefaultIterations() value.   This setting can override
     * the default iteration count.  If this setting value is a positive integer, it will override the default.}</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.com.novell.ldapchai.cr.chai.iterations</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>0</td></tr>
     * </table>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     * @see com.novell.ldapchai.util.ConfigObjectRecord
     */
    CR_CHAI_SALT_CHAR_COUNT( "chai.com.novell.ldapchai.cr.chai.saltCharCount", "0", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Number of threads to use during multi-hash operations, such as creating a new response set via
     * {@link com.novell.ldapchai.cr.ChaiCrFactory#newChaiResponseSet} and other functions.</p>
     *
     * <p>If zero, then {@link Runtime#availableProcessors()} will be used.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.com.novell.ldapchai.cr.chai.hashTreadCount</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>0</td></tr>
     * </table>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     * @see com.novell.ldapchai.util.ConfigObjectRecord
     */
    CR_CHAI_HASH_THREAD_COUNT( "chai.com.novell.ldapchai.cr.chai.hashTreadCount", "0", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>If true, then during the {@link com.novell.ldapchai.ChaiUser#setPassword(String)} operation, the control for
     * OID LDAP_SERVER_POLICY_HINTS_OID - 1.2.840.113556.1.4.2066 will be sent, causing AD to enforce password policy
     * rules including history rule requirements on the operation.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ad.setPolicyHintsOnPwSet</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>false</td></tr>
     * </table>
     */
    AD_SET_POLICY_HINTS_ON_PW_SET( "chai.ad.setPolicyHintsOnPwSet", "false", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Enable support for search result paging.  If set to "auto" then the RootDSE will be checked to see if it
     * lists OID 1.2.840.113556.1.4.319.  If it does then paging will be used. Otherwise setting to true or false
     * will explicitly disable LDAPChai's search result paging functionality.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ldap.paging.enable</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>auto</td></tr>
     * </table>
     */
    LDAP_SEARCH_PAGING_ENABLE( "chai.ldap.paging.enable", "auto", true, SettingValidator.AUTO_OR_BOOLEAN_VALIDATOR ),

    /**
     * <p>If {@link #LDAP_SEARCH_PAGING_ENABLE} is enabled, then this setting will control the page size.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ldap.paging.size</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>500</td></tr>
     * </table>
     */
    LDAP_SEARCH_PAGING_SIZE( "chai.ldap.paging.size", "500", true, SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Distinguished name of the default password policy in OpenLDAP.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.openldap.passwordPolicyDn</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>(none)</td></tr>
     * </table>
     */
    OPENLDAP_PASSWORD_POLICY_DN( "chai.openldap.passwordPolicyDn", "", true, null ),

    /**
     * <p>OpenLDAP Password Policies can be read from the local file system.  This setting allows specifying the local
     * file URL of the password file.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.openldap.passwordPolicy.url</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>file:/etc/openldap/check_password.conf</td></tr>
     * </table>
     */
    OPENLDAP_LOCAL_PASSWORD_POLICY_URL( "chai.openldap.passwordPolicy.url", "file:/etc/openldap/check_password.conf", true, null ),

    /**
     * <p>Use the jndi 'resolve-in-name-space' api for canonical LDAP DN's.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.jndi.resolveInNamespace</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>true</td></tr>
     * </table>
     */
    JNDI_RESOLVE_IN_NAMESPACE( "chai.jndi.resolveInNamespace", "true", true, SettingValidator.BOOLEAN_VALIDATOR ),

    /**
     * <p>Set the LDAP character encoding type to use during text/binary conversions.</p>
     *
     * <table border="1"><caption><b>Setting Information</b></caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.ldap.characterEncoding</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>UTF8</td></tr>
     * </table>
     */
    LDAP_CHARACTER_ENCODING( "chai.ldap.characterEncoding", "UTF8", true, null ),

    /**
     * <p>This method is deprecated and should not be used because it was incorrectly implemented
     * and ambiguous.  Setting this value has no effect.</p>
     * <p>See instead {@link ChaiSetting#CR_CHAI_SALT_CHAR_COUNT} and {@link ChaiSetting#CR_CHAI_ITERATIONS}.</p>
     *
     * @deprecated Thie setting has no effect.
     */
    @Deprecated
    CR_CHAI_SALT_COUNT( "chai.cr.chai.saltCount", "1000", true, SettingValidator.INTEGER_VALIDATOR ),;


    private final String key;
    private final String defaultValue;
    private final boolean visible;
    private final SettingValidator.Validator validator;

    /**
     * For a given key, find the associated setting.  If no setting matches
     * the supplied key, null is returned.
     *
     * @param key string value of the setting's key.
     * @return the setting associated witht the <i>key</i>, or <i>null</i>.
     * @see #getKey()
     */
    public static ChaiSetting forKey( final String key )
    {
        for ( final ChaiSetting setting : ChaiSetting.values() )
        {
            if ( setting.getKey().equals( key ) )
            {
                return setting;
            }
        }
        return null;
    }

    ChaiSetting( final String key, final String defaultValue, final boolean visible, final SettingValidator.Validator validator )
    {
        this.key = key;
        this.defaultValue = defaultValue;
        this.validator = validator;
        this.visible = visible;
    }

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
     * Get the key name, suitable for use in a {@link java.util.Properties} instance.
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

    /**
     * Validates the syntactical structure of the value.  Useful for pre-testing a value to see
     * if it meets requirements.
     *
     * @param value the value to test
     * @throws IllegalArgumentException if the value is not syntactically correct
     */
    public void validateValue( final String value )
    {
        if ( this.validator == null )
        {
            return;
        }
        this.validator.validate( value );
    }

}
