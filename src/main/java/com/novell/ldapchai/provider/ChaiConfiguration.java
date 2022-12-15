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

import com.novell.ldapchai.util.internal.StringHelper;

import javax.net.ssl.X509TrustManager;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * LDAP Chai API configuration settings.  This class represents the primary means
 * of controlling Chai behavior.
 *
 * @author Jason D. Rivard
 * @see ChaiSetting
 */
public class ChaiConfiguration
{
    // comma <or> space (regex)
    public static final String LDAP_URL_SEPARATOR_REGEX_PATTERN = ",| ";

    private static final Map<String, String> DEFAULT_SETTINGS;

    private final Serializable implementationConfiguration;
    private final Map<String, String> settings;
    private final X509TrustManager[] trustManager;

    static
    {
        final Map<String, String> settings = new LinkedHashMap<>();
        for ( final ChaiSetting s : ChaiSetting.values() )
        {
            settings.put( s.getKey(), s.getDefaultValue() );
        }
        DEFAULT_SETTINGS = Collections.unmodifiableMap( settings );
    }

    private ChaiConfiguration(
            final Serializable implementationConfiguration,
            final Map<String, String> settings,
            final X509TrustManager[] trustManager
    )
    {
        this.implementationConfiguration = implementationConfiguration;
        this.settings = settings;
        this.trustManager = trustManager == null ? null : Arrays.copyOf( trustManager, trustManager.length );
    }

    /**
     * Construct a default {@code ChaiConfiguration}.
     *
     * @param bindDN       ldap bind DN, in ldap fully qualified syntax.  Also used as the DN of the returned ChaiUser.
     * @param bindPassword password for the bind DN.
     * @param ldapURL      ldap server and port in url format, example: <i>ldap://127.0.0.1:389</i>
     * @return a new configuration instance.
     */
    public static ChaiConfiguration newConfiguration( final String ldapURL, final String bindDN, final String bindPassword )
    {
        return new ChaiConfigurationBuilder( ldapURL, bindDN, bindPassword ).build();
    }

    /**
     * Construct a default {@code ChaiConfiguration}.
     *
     * @param bindDN       ldap bind DN, in ldap fully qualified syntax.  Also used as the DN of the returned ChaiUser.
     * @param bindPassword password for the bind DN.
     * @param ldapURLs      ldap server and port in url format, example: <i>ldap://127.0.0.1:389</i>
     * @return A new {@code ChaiConfiguration} instance.
     */
    public static ChaiConfiguration newConfiguration(
            final List<String> ldapURLs,
            final String bindDN,
            final String bindPassword
    )
    {
        return new ChaiConfigurationBuilder( ldapURLs, bindDN, bindPassword ).build();
    }

    /**
     * Return the current implementation configuration object.
     *
     * @return current implementation configuration object.
     */
    public Object getImplementationConfiguration()
    {
        return implementationConfiguration;
    }

    /**
     * Returns a string value suitable for debugging.  Sensitive values such as passwords are
     * not included.
     *
     * @return a string value suitable for debugging
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( ChaiConfiguration.class.getSimpleName() );
        sb.append( ": " );
        sb.append( " settings: {" );

        for ( final ChaiSetting s : ChaiSetting.values() )
        {
            sb.append( s.getKey() );
            sb.append( "=" );
            if ( s.isVisible() )
            {
                sb.append( getSetting( s ) );
            }
            else
            {
                sb.append( "**stripped**" );
            }
            sb.append( ", " );
        }

        //remove the last ", " from the buffer
        if ( sb.toString().endsWith( ", " ) )
        {
            sb.delete( sb.length() - 2, sb.length() );
        }

        sb.append( "}" );

        return sb.toString();
    }

    /**
     * Get an individual setting value.
     *
     * @param setting the setting to return
     * @return the value or the default value if no value exists.
     */
    public String getSetting( final ChaiSetting setting )
    {
        return settings.get( setting.getKey() );
    }

    /**
     * Get an individual setting value and test it as a boolean.
     *
     * @param setting the setting to return
     * @return the value or the default value if no value exists.
     */
    public boolean getBooleanSetting( final ChaiSetting setting )
    {
        final String settingValue = getSetting( setting );
        return StringHelper.convertStrToBoolean( settingValue );
    }

    /**
     * Returns an immutable list of the ldap URLs.
     *
     * @return an immutable list of the ldapURLS.
     */
    public List<String> bindURLsAsList()
    {
        final List<String> splitUrls = Arrays.asList( getSetting( ChaiSetting.BIND_URLS ).split( LDAP_URL_SEPARATOR_REGEX_PATTERN ) );
        return Collections.unmodifiableList( splitUrls );
    }

    public String getDebugUrl()
    {
        return bindURLsAsList().get( 0 ) + "/" + getSetting( ChaiSetting.BIND_DN );
    }

    String getBindPassword()
    {
        return settings.get( ChaiSetting.BIND_PASSWORD.getKey() );
    }

    Optional<DirectoryVendor> getDefaultVendor()
    {
        final String defaultVendor = this.getSetting( ChaiSetting.DEFAULT_VENDOR );
        if ( defaultVendor != null )
        {
            for ( final DirectoryVendor vendor : DirectoryVendor.values() )
            {
                if ( vendor.toString().equals( defaultVendor ) )
                {
                    return Optional.of( vendor );
                }
            }
        }

        return Optional.empty();
    }

    int getIntSetting( final ChaiSetting name )
    {
        try
        {
            return Integer.parseInt( getSetting( name ) );
        }
        catch ( Exception e )
        {
            // doesnt matter, we're throwing anyway.
        }
        throw new IllegalArgumentException( "misconfigured value; " + name + " should be Integer, but is not" );
    }

    /**
     * Get the current settings of the {@code ChaiProvider}.
     *
     * @return a copy of the settings for this ChaiConfiguration
     */
    public Properties getSettings()
    {
        // make a defensive copy
        final Properties propCopy = new Properties();
        propCopy.putAll( settings );
        return propCopy;
    }

    /**
     * Get the current settings of the {@code ChaiProvider}.
     *
     * @return a copy of the settings for this ChaiConfiguration
     */
    public X509TrustManager[] getTrustManager()
    {
        return trustManager == null ? null : Arrays.copyOf( trustManager, trustManager.length );
    }

    public static ChaiConfigurationBuilder builder()
    {
        return new ChaiConfigurationBuilder(  );
    }

    public static ChaiConfigurationBuilder builder(
            final String ldapURLs,
            final String bindDN,
            final String bindPassword
    )
    {
        return new ChaiConfigurationBuilder( ldapURLs, bindDN, bindPassword );
    }

    public static ChaiConfigurationBuilder builder(
            final List<String> ldapURLs,
            final String bindDN,
            final String bindPassword
    )
    {
        return new ChaiConfigurationBuilder( ldapURLs, bindDN, bindPassword );
    }

    public static ChaiConfigurationBuilder builder( final ChaiConfiguration chaiConfiguration )
    {
        return new ChaiConfigurationBuilder( chaiConfiguration );
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        final ChaiConfiguration that = ( ChaiConfiguration ) o;
        return Objects.equals( implementationConfiguration, that.implementationConfiguration )
                && Objects.equals( settings, that.settings )
                && Arrays.equals( trustManager, that.trustManager );
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash( implementationConfiguration, settings );
        result = 31 * result + Arrays.hashCode( trustManager );
        return result;
    }

    /**
     * A convenience Builder for {@link ChaiConfiguration} instances.
     */
    public static class ChaiConfigurationBuilder
    {
        private Serializable implementationConfiguration = null;
        private Map<String, String> settings = new LinkedHashMap<>( DEFAULT_SETTINGS );
        private X509TrustManager[] trustManager = null;

        private ChaiConfigurationBuilder()
        {
        }

        /**
         * Construct a default {@code ChaiConfiguration}.
         *
         * @param bindDN       ldap bind DN, in ldap fully qualified syntax.  Also used as the DN of the returned ChaiUser.
         * @param bindPassword password for the bind DN.
         * @param ldapURLs     an ordered list fo ldap server and port in url format, example: <i>ldap://127.0.0.1:389</i>
         */
        private ChaiConfigurationBuilder( final String ldapURLs, final String bindDN, final String bindPassword )
        {
            this( Collections.singletonList( ldapURLs ), bindDN, bindPassword );
        }

        /**
         * Construct a default {@code ChaiConfiguration}.
         *
         * @param bindDN       ldap bind DN, in ldap fully qualified syntax.  Also used as the DN of the returned ChaiUser.
         * @param bindPassword password for the bind DN.
         * @param ldapURLs     an ordered list fo ldap server and port in url format, example: <i>ldap://127.0.0.1:389</i>
         */
        private ChaiConfigurationBuilder( final List<String> ldapURLs, final String bindDN, final String bindPassword )
        {
            setSetting( ChaiSetting.BIND_PASSWORD, bindPassword );
            setSetting( ChaiSetting.BIND_DN, bindDN );

            {
                final StringBuilder sb = new StringBuilder();
                for ( final String s : ldapURLs )
                {
                    sb.append( s );
                    sb.append( "," );
                }
                this.setSetting( ChaiSetting.BIND_URLS, sb.toString() );
            }
        }

        /**
         * Construct a new configuration based on the input configuration settings, including the bind DN, password and ldap URLs.  The
         * new instance will be unlocked, regardless of the lock status of the input configuration.
         *
         * @param existingConfiguration A configuration instance to copy from.
         */
        public ChaiConfigurationBuilder( final ChaiConfiguration existingConfiguration )
        {
            settings = new LinkedHashMap<>( existingConfiguration.settings );
            trustManager = existingConfiguration.trustManager;
            implementationConfiguration = existingConfiguration.implementationConfiguration;
        }

        /**
         * <p>Set an object to be used for the {@link ChaiProvider} implementation to be used for its configuration.  Depending
         * on the implementation, this could be any type of object such as a Properties, Map, or even an implementation specific object.</p>
         *
         * <p>When used with the default provider, {@code JNDIProviderImpl}, this object must be a {@link java.util.Hashtable} environment as specified by the
         * {@link javax.naming.ldap.InitialLdapContext}.</p>
         *
         * @param implementationConfiguration an object suitable to be used as a configuration for whatever {@code ChaiProvider} implementation is to be used.
         * @return this instance of the {@link ChaiConfiguration} to facilitate chaining
         */
        public ChaiConfigurationBuilder setImplementationConfiguration( final Serializable implementationConfiguration )
        {
            this.implementationConfiguration = implementationConfiguration;
            return this;
        }

        /**
         * Add a TrustManager to be used when connecting to ssl ldap servers.
         *
         * @param trustManager A serializable {@link X509TrustManager} to be used for connecting to ldap servers.
         * @return this instance of the {@link ChaiConfiguration} to facilitate chaining
         */
        public ChaiConfigurationBuilder setTrustManager( final X509TrustManager[] trustManager )
        {
            this.trustManager = trustManager == null ? null : Arrays.copyOf( trustManager, trustManager.length );
            return this;
        }

        /**
         * Set the settings in the {@code ChaiConfiguration}.  Each setting key is available as a constant publicly defined by
         * ChaiConfiguration.
         *
         * @param settings a Properties containing settings to be used by the provider.  If a setting is missing in the
         *                 supplied Properties, the current setting will be unchanged.
         * @return This builder instance.
         */
        public ChaiConfigurationBuilder setSettings( final Properties settings )
        {
            for ( final Map.Entry<Object, Object> entry : settings.entrySet() )
            {
                final String key = (String) entry.getKey();
                final ChaiSetting setting = ChaiSetting.forKey( key );
                if ( setting != null )
                {
                    final String value = (String) entry.getValue();
                    setSetting( setting, value );
                }
            }
            return this;
        }

        public ChaiConfigurationBuilder setSettings( final Map<ChaiSetting, String> settings )
        {
            for ( final Map.Entry<ChaiSetting, String> entry : settings.entrySet() )
            {
                setSetting( entry.getKey(), entry.getValue() );
            }
            return this;
        }

        /**
         * Set a single settings.  Each setting is available in the {@link ChaiSetting} enumeration.
         *
         * @param setting the setting to set
         * @param value   the value to set
         * @return this instance of the {@link ChaiConfiguration} to facilitate chaining
         * @throws IllegalArgumentException if the value is not syntactically correct
         * @see ChaiSetting#validateValue(String)
         */
        public ChaiConfigurationBuilder setSetting( final ChaiSetting setting, final String value )
        {
            setting.validateValue( value );
            this.settings.put( setting.getKey(), value == null ? setting.getDefaultValue() : value );
            return this;
        }

        public ChaiConfiguration build()
        {
            return new ChaiConfiguration( implementationConfiguration, settings, trustManager );
        }
    }
}
