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

package com.novell.ldapchai.impl.openldap.entry;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.util.GenericRuleHelper;
import com.novell.ldapchai.util.PasswordRuleHelper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public class OpenLDAPPasswordPolicy extends OpenLDAPEntry implements ChaiPasswordPolicy
{

    /**
     * All attributes used by the password policy. Several "helper" values for
     * each attribute are available, such as the ldap attribute name, and
     * default values.
     */
    enum Attribute
    {

        /**
         * Minimum total length of the password.
         */
        MIN_LENGTH(
                TYPE.MIN,
                ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_MIN_LENGTH,
                "0",
                ChaiPasswordRule.MinimumLength ),

        /**
         * Minimum number of upper case characters in the password.
         */
        MIN_UPPER(
                TYPE.MIN,
                ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_MIN_UPPER_CHARACTERS,
                "0",
                ChaiPasswordRule.MinimumUpperCase ),

        /**
         * Minimim total length of the password.
         */
        MIN_LOWER(
                TYPE.MIN,
                ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_MIN_LOWER_CHARACTERS,
                "0",
                ChaiPasswordRule.MinimumLowerCase ),

        /**
         * Minimum number of times a numeric character may appear in the password.
         */
        MIN_NUMERIC(
                TYPE.MIN,
                ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_MIN_NUMERIC_CHARACTERS,
                "0",
                ChaiPasswordRule.MinimumNumeric ),

        /**
         * Minimum number of times a special (non-alphanumeric) character may appear in the password.
         */
        MIN_SPECIAL(
                TYPE.MIN,
                ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_MIN_SPECIAL_CHARACTERS,
                "0",
                ChaiPasswordRule.MinimumSpecial ),

        /**
         * If the password must be unique when compared to previously used
         * passwords (true/false). This rule is not directly enforceable by the
         * Chai API.
         */
        PASSWORD_HISTORY_COUNT(
                TYPE.MIN,
                ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_HISTORY_COUNT,
                "0",
                null ),

        /**
         * The time interval between required password changes (true/false).
         * This rule is not directly enforced by the Chai API.
         */
        EXPIRATION_INTERVAL(
                TYPE.MAX,
                ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_MAX_PASSWORD_AGE,
                "0",
                ChaiPasswordRule.ExpirationInterval ),

        /**
         * Minimum lifetime of the user's password. Once set, the user will not
         * be able to modify their password until this amount of time has
         * passed. Value is in seconds.
         */
        MIN_LIFETIME(
                TYPE.MIN,
                ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_MIN_PASSWORD_AGE,
                "0",
                ChaiPasswordRule.MinimumLifetime );

        private final TYPE type;
        private final String ldapAttr;
        private final String defaultValue;
        private final ChaiPasswordRule ruleName;

        Attribute( final TYPE type, final String ldapAttr, final String defaultValue, final ChaiPasswordRule ruleName )
        {
            this.type = type;
            this.ldapAttr = ldapAttr;
            this.defaultValue = defaultValue;
            this.ruleName = ruleName;
        }

        /**
         * A string value useful for debugging.
         *
         * @return A string value useful for debugging.
         */
        public ChaiPasswordRule getRuleName()
        {
            return ruleName;
        }

        /**
         * Get the type of value that should be expected when working with this
         * attribute's values.
         *
         * @return An enumeration indicating the type of value to be expected
         *     when working with this attributes values.
         */
        public TYPE getType()
        {
            return type;
        }

        /**
         * The string key value used in this
         * {@link OpenLDAPPasswordPolicy}
         * object's backing {@code Properties}. Typically the same as
         * {@link #getLdapAttribute()}
         * , but this is not guaranteed
         *
         * @return A String useful for managing a map of
         *     {@link OpenLDAPPasswordPolicy}
         *     values.
         */
        public String getKey()
        {
            return ldapAttr;
        }

        /**
         * The appropriate ldap attribute name of the attribute. Typically the
         * same as
         * {@link #getKey()}
         * , but this is not guaranteed
         *
         * @return An ldap attribute name
         */
        public String getLdapAttribute()
        {
            return ldapAttr;
        }

        /**
         * Default value used by this attribute.
         *
         * @return The String value of the default value
         */
        public String getDefaultValue()
        {
            return defaultValue;
        }

        /**
         * An enumeration indicating what type of setting is expected for this
         * attribute's value.
         */
        public enum TYPE
        {
            /**
             * An integer representing a maximum limit of a value.
             */
            MAX,
            /**
             * An integer representing a minimum limit of a value.
             */
            MIN,
            /**
             * An boolean representing an on/off value.
             */
            BOOLEAN,
            /**
             * Some other type of value.
             */
            OTHER,
        }

        public static Attribute attributeForRule( final ChaiPasswordRule rule )
        {
            if ( rule == null )
            {
                return null;
            }

            for ( final Attribute attr : Attribute.values() )
            {
                if ( rule.equals( attr.getRuleName() ) )
                {
                    return attr;
                }
            }

            return null;
        }
    }

    static final Set<String> LDAP_PASSWORD_ATTRIBUTES;

    static
    {
        final Set<String> ldapPasswordAttributes = new HashSet<>();
        ldapPasswordAttributes.add( ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_CHECK_QUALITY );
        for ( final Attribute attribute : Attribute.values() )
        {
            ldapPasswordAttributes.add( attribute.getLdapAttribute() );
        }
        LDAP_PASSWORD_ATTRIBUTES = Collections.unmodifiableSet( ldapPasswordAttributes );
    }

    private final Map<String, String> ruleMap = new HashMap<>();
    private final Map<String, String> allEntryValues = new HashMap<>();
    private final ChaiProvider provider;

    @SuppressFBWarnings( "EI_EXPOSE_REP2" )
    public OpenLDAPPasswordPolicy( final String entryDN, final ChaiProvider chaiProvider )
            throws ChaiUnavailableException,
            ChaiOperationException
    {
        super( entryDN, chaiProvider );

        // read all attribute values from entry.
        allEntryValues.putAll( readStringAttributes( LDAP_PASSWORD_ATTRIBUTES ) );
        LOGGER.trace( () -> "allEntryValues = " + allEntryValues );
        final String pwdCheckQuality = allEntryValues.get( ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_CHECK_QUALITY );
        LOGGER.debug( () -> "pwdCheckQuality = " + pwdCheckQuality );
        if ( Objects.equals( "1", pwdCheckQuality ) || Objects.equals( "2", pwdCheckQuality ) )
        {
            allEntryValues.putAll( readCheckPasswordAttributes() );
        }
        LOGGER.trace( () -> "allEntryValues = " + allEntryValues );
        ruleMap.putAll( createRuleMapUsingAttributeValues( allEntryValues ) );
        LOGGER.trace( () -> "ruleMap = " + ruleMap );

        this.provider = chaiProvider;
    }

    private Map<String, String> readCheckPasswordAttributes()
    {
        final String policyFileUrl = this.chaiProvider.getChaiConfiguration().getSetting( ChaiSetting.OPENLDAP_LOCAL_PASSWORD_POLICY_URL );
        if ( policyFileUrl == null || policyFileUrl.length() < 1 )
        {
            return Collections.emptyMap();
        }

        InputStream inputStream = null;
        try
        {
            final URL url = new URL( policyFileUrl );
            inputStream = url.openStream();
            final Properties properties = new Properties();
            properties.load( inputStream );

            final Map<String, String> returnMap = new HashMap<>();
            for ( final Object key : properties.keySet() )
            {
                returnMap.put( ( String ) key, properties.getProperty( ( String ) key ) );
            }
            return returnMap;
        }
        catch ( IOException e )
        {
            LOGGER.debug( () -> "unable to read openldap password policy configuration attributes from " + policyFileUrl + ", error=" + e.getMessage() );
        }
        finally
        {
            if ( inputStream != null )
            {
                try
                {
                    inputStream.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }

        return Collections.emptyMap();
    }

    private static Map<String, String> createRuleMapUsingAttributeValues( final Map<String, String> entryValues )
    {
        final Map<String, String> returnMap = new HashMap<>();

        // defaults for OpenLDAP policy
        returnMap.put( ChaiPasswordRule.AllowNumeric.getKey(), String.valueOf( true ) );
        returnMap.put( ChaiPasswordRule.AllowSpecial.getKey(), String.valueOf( true ) );
        returnMap.put( ChaiPasswordRule.CaseSensitive.getKey(), String.valueOf( true ) );

        // convert the standard attributes to chai rules
        for ( final ChaiPasswordRule rule : ChaiPasswordRule.values() )
        {
            final Attribute attribute = Attribute.attributeForRule( rule );
            if ( attribute != null )
            {
                returnMap.put( rule.getKey(), attribute.getDefaultValue() );
                final String attributeName = attribute.getLdapAttribute();
                LOGGER.trace( () -> "attributeName = " + attributeName );
                if ( attributeName != null && entryValues != null && entryValues.containsKey( attributeName ) )
                {
                    returnMap.put( rule.getKey(), entryValues.get( attributeName ) );
                }
            }

            if ( !returnMap.containsKey( rule.getKey() ) )
            {
                returnMap.put( rule.getKey(), rule.getDefaultValue() );
            }
        }

        if ( entryValues != null && entryValues.containsKey( ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_HISTORY_COUNT ) )
        {
            try
            {
                final int historyCount = Integer.parseInt( entryValues.get( ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_HISTORY_COUNT ) );
                if ( historyCount > 0 )
                {
                    returnMap.put( ChaiPasswordRule.UniqueRequired.getKey(), "true" );
                }
            }
            catch ( Exception e )
            {
                LOGGER.error( () -> "error while parsing " + ChaiConstant.ATTR_OPENLDAP_PASSWORD_POLICY_HISTORY_COUNT + " value: "
                        + e.getMessage() );
            }
        }

        return returnMap;
    }

    @Override
    public String getValue( final String key )
    {
        return ruleMap.get( key );
    }

    @Override
    public String getValue( final ChaiPasswordRule rule )
    {
        return ruleMap.get( rule.getKey() );
    }

    @Override
    public Set<String> getKeys()
    {
        return Collections.unmodifiableSet( ruleMap.keySet() );
    }

    @Override
    public ChaiEntry getPolicyEntry()
    {
        return this;
    }

    @Override
    public PasswordRuleHelper getRuleHelper()
    {
        return new GenericRuleHelper( this );
    }

    public String getLdapObjectClassName()
    {
        return "pwdPolicy";
    }

    public String getSourceDN()
    {
        return this.getEntryDN();
    }
}
