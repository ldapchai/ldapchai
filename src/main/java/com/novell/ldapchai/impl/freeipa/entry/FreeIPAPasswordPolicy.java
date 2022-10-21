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

package com.novell.ldapchai.impl.freeipa.entry;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.util.GenericRuleHelper;
import com.novell.ldapchai.util.PasswordRuleHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FreeIPAPasswordPolicy extends FreeIPAEntry implements ChaiPasswordPolicy
{
    private static final String ATTR_IPA_PWD_POLICY_MIN_LIFE = "krbMinPwdLife";
    private static final String ATTR_IPA_PWD_POLICY_MAX_LIFE = "krbMaxPwdLife";
    private static final String ATTR_IPA_PWD_POLICY_MIN_LENGTH = "krbPwdMinLength";
    private static final String ATTR_IPA_PWD_POLICY_PWD_HISTORY_LENGTH = "krbPwdHistoryLength";

    private static final String ATTR_IPA_PWD_POLICY_MIN_DIFF_CHARS = "krbPwdMinDiffChars";

    private static final String ATTR_IPA_PWD_POLICY_MAX_FAILURE = "krbPwdMaxFailure";
    private static final String ATTR_IPA_PWD_POLICY_LOCKOUT_DURATION = "krbPwdLockoutDuration";
    private static final String ATTR_IPA_PWD_POLICY_FAIL_COUNT_INTERVAL = "krbPwdFailureCountInterval";

    enum PwdPolicyAttribute
    {
        /**
         * The minimum period of time, in seconds, that a user's password must be in effect
         * before the user can change it.
         */
        MIN_LIFETIME( ATTR_IPA_PWD_POLICY_MIN_LIFE, "0", ChaiPasswordRule.MinimumLifetime ),

        /**
         * The maximum period of time, in seconds, that a user's password can be in effect
         * before it must be changed.
         */
        MAX_LIFETIME( ATTR_IPA_PWD_POLICY_MAX_LIFE, "0", ChaiPasswordRule.ExpirationInterval ),

        /**
         * The minimum number of characters that must exist in a password
         * before it is considered valid.
         */
        MIN_LENGTH( ATTR_IPA_PWD_POLICY_MIN_LENGTH, "0", ChaiPasswordRule.MinimumLength ),

        /**
         * Minimum number of character classes.
         * This rule is not directly enforceable by the Chai API.
         */
        MIN_DIFF_CHARS( ATTR_IPA_PWD_POLICY_MIN_DIFF_CHARS, "0", null ),

        /**
         * The number of previous passwords that IPA stores, and which a user
         * is prevented from using.
         * This rule is not directly enforceable by the Chai API.
         */
        PASSWORD_HISTORY_LENGTH( ATTR_IPA_PWD_POLICY_PWD_HISTORY_LENGTH, "0", null ),

        /**
         * Consecutive failures before lockout.
         * This rule is not directly enforceable by the Chai API.
         */
        MAX_FAILURE( ATTR_IPA_PWD_POLICY_MAX_FAILURE, "0", null ),

        /**
         * Period, in seconds, for which lockout is enforced.
         * This rule is not directly enforceable by the Chai API.
         */
        LOCKOUT_DURATION( ATTR_IPA_PWD_POLICY_LOCKOUT_DURATION, "0", null ),

        /**
         * Period, in seconds, after which failure count will be reset.
         * This rule is not directly enforceable by the Chai API.
         */
        FAIL_COUNT_INTERVAL( ATTR_IPA_PWD_POLICY_FAIL_COUNT_INTERVAL, "0", null );

        private final String ldapAttribute;
        private final String defaultValue;
        private final ChaiPasswordRule chaiPwdRule;

        PwdPolicyAttribute( final String ldapAttribute, final String defaultValue, final ChaiPasswordRule chaiPwdRule )
        {
            this.ldapAttribute = ldapAttribute;
            this.defaultValue = defaultValue;
            this.chaiPwdRule = chaiPwdRule;
        }

        public String getLdapAttribute()
        {
            return ldapAttribute;
        }

        public String getDefaultValue()
        {
            return defaultValue;
        }

        public ChaiPasswordRule getRuleName()
        {
            return chaiPwdRule;
        }

        public String getKey()
        {
            return ldapAttribute;
        }

        public static PwdPolicyAttribute lookupAttribute( final ChaiPasswordRule rule )
        {
            if ( rule != null )
            {
                for ( final PwdPolicyAttribute item : PwdPolicyAttribute.values() )
                {
                    if ( rule.equals( item.getRuleName() ) )
                    {
                        return item;
                    }
                }
            }

            return null;
        }
    }

    static final Set<String> PWD_POLICY_LDAP_ATTRIBUTES;

    static
    {
        final Set<String> ldapAttributes = new HashSet<>();
        for ( final PwdPolicyAttribute item : PwdPolicyAttribute.values() )
        {
            ldapAttributes.add( item.getLdapAttribute() );
        }
        PWD_POLICY_LDAP_ATTRIBUTES = Collections.unmodifiableSet( ldapAttributes );
    }

    private final Map<String, String> ruleMap = new HashMap<>();

    public FreeIPAPasswordPolicy( final String entryDN, final ChaiProvider chaiProvider )
            throws ChaiUnavailableException, ChaiOperationException
    {
        super( entryDN, chaiProvider );

        final Map<String, String> ldapAttributeMap = new HashMap<>();
        ldapAttributeMap.putAll( readStringAttributes( PWD_POLICY_LDAP_ATTRIBUTES ) );

        // Set defaults for some rules to implicit values in FreeIPA
        ruleMap.put( ChaiPasswordRule.AllowNumeric.getKey(), "true" );
        ruleMap.put( ChaiPasswordRule.AllowSpecial.getKey(), "true" );

        // Emulate minimums for some rules unsupported by FreeIPA
        if ( ldapAttributeMap.containsKey( ATTR_IPA_PWD_POLICY_MIN_DIFF_CHARS ) )
        {
            try
            {
                final int minDiffChars = Integer.parseInt( ldapAttributeMap.get( ATTR_IPA_PWD_POLICY_MIN_DIFF_CHARS ) );
                if ( minDiffChars >= 1 )
                {
                    ruleMap.put( ChaiPasswordRule.MinimumLowerCase.getKey(), "1" );
                }
                if ( minDiffChars >= 2 )
                {
                    ruleMap.put( ChaiPasswordRule.MinimumUpperCase.getKey(), "1" );
                }
                if ( minDiffChars >= 3 )
                {
                    ruleMap.put( ChaiPasswordRule.MinimumNumeric.getKey(), "1" );
                }
                if ( minDiffChars >= 4 )
                {
                    ruleMap.put( ChaiPasswordRule.MinimumSpecial.getKey(), "1" );
                }
            }
            catch ( NumberFormatException e )
            {
                LOGGER.trace( () -> "failed to parse " + ATTR_IPA_PWD_POLICY_MIN_DIFF_CHARS + " value as integer" );
            }
        }

        // Require unique password according to FreeIPA password history length
        if ( ldapAttributeMap.containsKey( ATTR_IPA_PWD_POLICY_PWD_HISTORY_LENGTH ) )
        {
            try
            {
                final int historyLength = Integer.parseInt( ldapAttributeMap.get( ATTR_IPA_PWD_POLICY_PWD_HISTORY_LENGTH ) );
                if ( historyLength > 0 )
                {
                    ruleMap.put( ChaiPasswordRule.UniqueRequired.getKey(), "true" );
                }
            }
            catch ( NumberFormatException e )
            {
                LOGGER.trace( () -> "failed to parse " + ATTR_IPA_PWD_POLICY_PWD_HISTORY_LENGTH + " value as integer" );
            }
        }

        // Add rules directly supported by FreeIPA
        for ( final ChaiPasswordRule rule : ChaiPasswordRule.values() )
        {
            final String key = rule.getKey();

            final PwdPolicyAttribute attribute = PwdPolicyAttribute.lookupAttribute( rule );
            if ( attribute != null )
            {
                final String attributeName = attribute.getLdapAttribute();
                if ( attributeName != null && ldapAttributeMap.containsKey( attributeName ) )
                {
                    ruleMap.put( key, ldapAttributeMap.get( attributeName ) );
                }
            }

            if ( !ruleMap.containsKey( key ) )
            {
                ruleMap.put( key, rule.getDefaultValue() );
            }
        }
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
}
