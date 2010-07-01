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

package com.novell.ldapchai.impl.edir.entry;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiPasswordRule;

public interface NspmPasswordPolicy extends Top, ChaiPasswordPolicy {
    /**
     * Return the source of the challenge set policy information
     *
     * @return A string with the ldap syntax DN of the policy.  May be null if the policy was
     *         not read from ldap.
     */
    String getSourceDN();

// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    /**
     * Enumeration that describes the selected mode for password recovery.
     */
    static enum FORGOTTEN_MODE {
        CHANGE_PASSWORD("ChangePassword"),
        EMAIL_PASSWORD("EmailPassword"),
        EMAIL_HINT("EmailHint"),
        SHOW_HINT("ShowHint");

        protected String xmlName;

        FORGOTTEN_MODE(final String xmlName)
        {
            this.xmlName = xmlName;
        }

        protected String getXmlName()
        {
            return xmlName;
        }

        public static FORGOTTEN_MODE forXmlString(final String xmlString)
        {
            final FORGOTTEN_MODE[] allErrorCodes = FORGOTTEN_MODE.values();
            for (final FORGOTTEN_MODE loopCode : allErrorCodes) {
                if (loopCode.getXmlName().equalsIgnoreCase(xmlString)) {
                    return loopCode;
                }
            }
            return null;
        }
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * All attributes used by the password policy.  Several "helper" values for each attribute are available, such as the ldap attribute name,
     * and default values.
     */
    enum Attribute {
        /**
         * Maximum number of times a character can be consecutively repeated.
         */
        MAX_CONSECUTIVE(
                TYPE.MAX,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MAX_CONSECUTIVE_CHARACTERS,
                "0",
                ChaiPasswordRule.MaximumSequentialRepeat),
        /**
         * Maximum number of times a lower case character may appear in the password.
         */
        MAX_LOWER(
                TYPE.MAX,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MAX_LOWER_CHARACTERS,
                "0",
                ChaiPasswordRule.MaximumLowerCase),

        /**
         * Maximum number of times a numeric character may appear in the password.
         */
        MAX_NUMERIC(
                TYPE.MAX,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MAX_NUMERIC_CHARACTERS,
                "0",
                ChaiPasswordRule.MaximumNumeric),

        /**
         * Maximum number of times a character may be repeated in the password.
         */
        MAX_REPEATED(
                TYPE.MAX,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MAX_REPEATED_CHARACTERS,
                "0",
                ChaiPasswordRule.MaximumNumeric),

        /**
         * Maximum number of times a special (non alphanumeric) character may appear in the password
         */
        MAX_SPECIAL(
                TYPE.MAX,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MAX_SPECIAL_CHARACTERS,
                "0",
                ChaiPasswordRule.MaximumSpecial),


        /**
         * Maximum total length of the password.
         */
        MAX_LENGTH(
                TYPE.MAX,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MAX_LENGTH,
                "512",  //default eDir value
                ChaiPasswordRule.MaximumLength),

        /**
         * Minimim total length of the password.
         */
        MIN_LOWER(
                TYPE.MIN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MIN_LOWER_CHARACTERS,
                "0",
                ChaiPasswordRule.MinimumLowerCase),

        /**
         * Minimum number of times a numeric character mayappear in the password.
         */
        MIN_NUMERIC(
                TYPE.MIN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MIN_NUMERIC_CHARACTERS,
                "0",
                ChaiPasswordRule.MinimumNumeric),

        /**
         * Minimum number of times a special (non-alphanumeric) character may appear in the password.
         */
        MIN_SPECIAL(
                TYPE.MIN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MIN_SPECIAL_CHARACTERS,
                "0",
                ChaiPasswordRule.MinimumSpecial),

        /**
         * Minimum number of unique characters in the password.
         */
        MIN_UNIQUE(
                TYPE.MIN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MIN_UNIQUE_CHARACTERS,
                "0",
                ChaiPasswordRule.MinimumUnique),

        /**
         * Minimum number of upper case characters in the password.
         */
        MIN_UPPER(
                TYPE.MIN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MIN_UPPER_CHARACTERS,
                "0",
                ChaiPasswordRule.MinimumUpperCase),

        /**
         * Maximum number of upper case characters in the password.
         */
        MAX_UPPER(
                TYPE.MIN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MAX_UPPER_CHARACTERS,
                "0",
                ChaiPasswordRule.MaximumUpperCase),
        /**
         * Minimum total length of the password.
         */
        MIN_LENGTH(
                TYPE.MIN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_MIN_LENGTH,
                "0",
                ChaiPasswordRule.MinimumLength),

        /**
         * If numeric characters are allowed (true/false).
         */
        NUMERIC_ALLOWED(
                TYPE.BOOLEAN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_NUMERIC_ALLOWED,
                "TRUE",
                ChaiPasswordRule.AllowNumeric),

        /**
         * If the first character in the password is permitted to be a numeric character (true/false).
         */
        NUMERIC_FIRST_ALLOWED(
                TYPE.BOOLEAN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_NUMERIC_FIRST_ALLOWED,
                "TRUE",
                ChaiPasswordRule.AllowFirstCharNumeric),

        /**
         * If the last character in the password is permitted to be a numeric character (true/false).
         */
        NUMERIC_LAST_ALLOWED(
                TYPE.BOOLEAN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_NUMERIC_LAST_ALLOWED,
                "TRUE",
                ChaiPasswordRule.AllowLastCharNumeric),

        /**
         * If special (non-alphanumeric) characters are allowed (true/false).
         */
        SPECIAL_ALLOWED(
                TYPE.BOOLEAN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_SPECIAL_ALLOWED,
                "TRUE",
                ChaiPasswordRule.AllowSpecial),

        /**
         * If the first character in the password is permitted to be a numeric character (true/false).
         */
        SPECIAL_FIRST_ALLOWED(
                TYPE.BOOLEAN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_SPECIAL_FIRST_ALLOWED,
                "TRUE",
                ChaiPasswordRule.AllowFirstCharSpecial),

        /**
         * If the last character in the password is permitted to be a numeric character (true/false).
         */
        SPECIAL_LAST_ALLOWED(
                TYPE.BOOLEAN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_SPECIAL_LAST_ALLOWED,
                "TRUE",
                ChaiPasswordRule.AllowLastCharSpecial),

        /**
         * If the password should be recongnized as case sensitive (true/false).
         */
        CASE_SENSITIVE(
                TYPE.BOOLEAN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_CASE_SENSITIVE,
                "TRUE",
                ChaiPasswordRule.CaseSensitive),

        /**
         * If the password must be unique when compared to previously used passwords (true/false).  This rule
         * is not directly enforcable by the Chai API.
         */
        PASSWORD_UNIQUE_REQUIRED(
                TYPE.BOOLEAN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_PASSWORD_UNIQUE_REQUIRED,
                "FALSE",
                ChaiPasswordRule.UniqueRequired),

        /**
         * If this policy is enabled (true/false).
         */
        PASSWORD_RULE_ENFORCEMENT(
                TYPE.BOOLEAN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_PASSWORD_RULE_ENFORCEMENT,
                "FALSE",
                null),


        /**
         * The time interval between required password changes (true/false).
         * This rule is not directly enforced by the Chai API.
         */
        EXPIRATION_INTERVAL(
                TYPE.MIN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_EXPIRATION_INTERVAL,
                "0",
                ChaiPasswordRule.ExpirationInterval),

        /**
         * The maximum number of passwords allowed in the user's password history list(true/false).
         * This rule is not directly enforced by the Chai API.
         */
        PASSWORD_HISTORY_LIMIT(
                TYPE.MAX,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_PASSWORD_HISTORY_LIMIT,
                "0",
                null),

        /**
         * While stored in this {@code PasswordPolicy}, values are separated by {@link Character#LINE_SEPARATOR}.
         * This rule is not directly enforced by the Chai API.
         */
        PASSWORD_EXCLUDE_LIST(
                TYPE.OTHER,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_PASSWORD_EXCLUDE_LIST,
                "",
                ChaiPasswordRule.DisallowedValues),

        /**
         * Bitmask of policy options.
         *
         * @see com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicyImpl.PolicyOptions
         */
        PASSWORD_POLICY_OPTIONS(
                TYPE.OTHER,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_OPTIONS,
                "884",
                null),

        /**
         * Message to display to user during password change.
         */
        PASSWORD_CHANGE_MESSAGE(
                TYPE.OTHER,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_CHANGE_MESSAGE,
                "",
                ChaiPasswordRule.ChangeMessage),

        /**
         * The ldap DN of the entry from which this {@code PasswordPolicy} was built.  There are no gaurentees made about
         * this value.  {@code PasswordPolicy} objects may be built without respect to an ldap entry, in which case
         * this value will be empty.
         *
         * @see com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicy.Attribute#CHALLENGE_SET_GUID
         */
        CHALLENGE_SET_DN(
                TYPE.OTHER,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_CHALLENGE_SET_DN,
                "",
                null),

        /**
         * The "guid" of the entry from which this {@code PasswordPolicy} was built.  There are no gaurentees made about
         * this value.  {@code PasswordPolicy} objects may be built without respect to an ldap entry, in which case
         * this value will be empty.
         *
         * @see com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicy.Attribute#CHALLENGE_SET_DN
         */
        CHALLENGE_SET_GUID(
                TYPE.OTHER,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_CHALLENGE_SET_GUID,
                "",
                null),

        /**
         * The configured action to take when a user is trying to recover a forgotten password.
         * As defined by eDirectory password policies, this value will be an XML document.
         */
        FORGOTTEN_ACTION(
                TYPE.OTHER,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_FORGOTTEN_ACTION,
                "",
                null),

        /**
         * The configuration for forgotten password.
         * As defined by eDirectory password policies, this value will be an XML document.
         */
        FORGOTTEN_CONFIG(
                TYPE.OTHER,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_FORGOTTEN_LOGIN_CONFIG,
                "TRUE",
                null),

        /**
         * Values of the user's password that are not permitted inside the user's password.
         * While stored in this {@code PasswordPolicy}, values are seperated by {@code ","}.
         */
        DISALLOWED_ATTRIBUTES(
                TYPE.OTHER,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_DISALLOWED_ATTRIBUTES,
                "",
                ChaiPasswordRule.DisallowedAttributes),

        /**
         * Mininum lifetime of the user's password.  Once set, the user will not be able to modify
         * their password until this amount of time has passed.  Value is in seconds.
         */
        MIN_LIFETIME(
                TYPE.MIN,
                ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_PASSWORD_LIFETIME,
                "0",
                ChaiPasswordRule.MinimumLifetime);

        private final TYPE type;
        private final String ldapAttr;
        private final String defaultValue;
        private final ChaiPasswordRule ruleName;

        Attribute(final TYPE type, final String ldapAttr, final String defaultValue, final ChaiPasswordRule ruleName)
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
         * Get the type of value that should be expected when working with this attribute's values.
         *
         * @return An enumeration indicating the type of value to be expected when working with this attributes values.
         */
        public TYPE getType()
        {
            return type;
        }

        /**
         * The string key value used in this {@link com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicyImpl} object's backing {@code Properties}.
         * Typically the same as {@link com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicy.Attribute#getLdapAttribute()}, but this is not gaurenteed
         *
         * @return A String useful for managing a map of {@link com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicy.Attribute} values.
         */
        public String getKey()
        {
            return ldapAttr;
        }

        /**
         * The appropriate ldap attribute name of the attribute.
         * Typically the same as {@link com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicy.Attribute#getKey()}, but this is not gaurenteed
         *
         * @return An ldap attribute name
         */
        public String getLdapAttribute()
        {
            return ldapAttr;
        }

        /**
         * Default value used by a {@link com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicyImpl} for this attribute
         *
         * @return The String value of the default value
         */
        public String getDefaultValue()
        {
            return defaultValue;
        }

        /**
         * An enumeration indicating what type of setting is expected for this attribute's value.
         */
        public enum TYPE {
            /**
             * An integer representing a maximum limit of a value
             */MAX,
            /**
             * An integer representing a minimum limit of a value
             */MIN,
            /**
             * An boolean representing an on/off value
             */BOOLEAN,
            /**
             * Some other type of value
             */OTHER
        }

        public static Attribute attributeForRule(final ChaiPasswordRule rule) {
            if (rule == null) {
                return null;
            }

            for (final Attribute attr : Attribute.values()) {
                if (rule.equals(attr.getRuleName())) {
                    return attr;
                }
            }

            return null;
        }
    }

    public String getChallengeSetDN();    
}
