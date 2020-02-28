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

package com.novell.ldapchai;


import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Constants used by the Chai API.
 *
 * @author Jason D. Rivard
 */
public abstract class ChaiConstant
{


    /**
     * Attribute name to define the Object Class.
     */
    public static final String ATTR_LDAP_OBJECTCLASS = "objectClass";

    /**
     * Attribute name to define the CN.
     */
    public static final String ATTR_LDAP_COMMON_NAME = "cn";

    /**
     * Attribute name to define the UID or uniqueID.
     */
    public static final String ATTR_LDAP_UID = "uid";

    /**
     * Attribute name to define the Company Name.
     */
    public static final String ATTR_LDAP_DESCRIPTION = "description";

    /**
     * Attribute name to define the user password (Write Only).
     */
    public static final String ATTR_LDAP_USER_PASSWORD = "userPassword";

    /**
     * Attribute name to define when the user's password will expire.
     */
    public static final String ATTR_LDAP_PASSWORD_EXPIRE_TIME = "passwordExpirationTime";

    /**
     * Attribute name to define the user's password expiration interval.
     */
    public static final String ATTR_LDAP_PASSWORD_EXPIRE_INTERVAL = "passwordExpirationInterval";

    /**
     * Attribute name to define the user's minimum allowed password length.
     */
    public static final String ATTR_LDAP_PASSWORD_MINIMUM_LENGTH = "passwordMinimumLength";

    /**
     * Attribute name to define the first name.
     */
    public static final String ATTR_LDAP_GIVEN_NAME = "givenName";

    /**
     * Attribute name to define the last name.
     */
    public static final String ATTR_LDAP_SURNAME = "sn";

    /**
     * Attribute name to define the (middle) initials.
     */
    public static final String ATTR_LDAP_INITIAL = "initials";

    /**
     * Attribute name to define the email of the user.
     */
    public static final String ATTR_LDAP_EMAIL = "mail";

    /**
     * Attribute name to define the user's login is disabled or not.
     */
    public static final String ATTR_LDAP_LOGIN_DISABLED = "loginDisabled";

    public static final String ATTR_LDAP_MANAGER = "manager";

    public static final String ATTR_LDAP_DIRECT_REPORTS = "directReports";

    public static final String ATTR_LDAP_ASSISTANT = "assistant";

    /**
     * Attribute name to define the group membership.
     */
    public static final String ATTR_LDAP_GROUP_MEMBERSHIP = "groupMembership";

    /**
     * Attribute name to define security equality.
     */
    public static final String ATTR_LDAP_SECURITY_EQUALS = "securityEquals";

    /**
     * login grace remaining count.
     */
    public static final String ATTR_LDAP_LOGIN_GRACE_REMAINING = "loginGraceRemaining";

    /**
     * login grace limit.
     */
    public static final String ATTR_LDAP_LOGIN_GRACE_LIMIT = "loginGraceLimit";

    /**
     * login intruder attempts.
     */
    public static final String ATTR_LDAP_LOGIN_INTRUDER_ATTEMPTS = "loginIntruderAttempts";

    /**
     * login intruder reset time.
     */
    public static final String ATTR_LDAP_LOGIN_INTRUDER_RESET_TIME = "loginIntruderResetTime";

    /**
     * Attribute name to define the City.
     */
    public static final String ATTR_LDAP_CITY = "physicalDeliveryOfficeName";

    /**
     * Last time user logged in.
     */
    public static final String ATTR_LDAP_LAST_LOGIN_TIME = "loginTime";

    /**
     * Attribute name to define the Province/State.
     */
    public static final String ATTR_LDAP_STATE = "st";

    /**
     * Attribute name to define the Country.
     */
    public static final String ATTR_LDAP_COUNTRY = "co";

    /**
     * Attribute name to define the Postal Code.
     */
    public static final String ATTR_LPAP_POSTAL_CODE = "postalCode";

    /**
     * Attribute name to define the date format.
     */
    public static final String ATTR_LDAP_PREFERRED_LANGUAGE = "preferredLanguage";

    /**
     * Attribute name to define the date format.
     */
    public static final String ATTR_LDAP_TIMEZONE = "Timezone";

    /**
     * Attribute name to define the Telephone Number.
     */
    public static final String ATTR_LDAP_TELEPHONE_NUMBER = "telephoneNumber";

    /**
     * Attribute name to define the Workforce ID.
     */
    public static final String ATTR_LDAP_WORFORCE_ID = "workforceID";

    /**
     * Attribute name to define the Fax number.
     */
    public static final String ATTR_LDAP_FACSIMILE_NUMBER = "facsimileTelephoneNumber";

    /**
     * Attribute name to define the Mobile number.
     */
    public static final String ATTR_LDAP_MOBILE_NUMBER = "mobile";

    /**
     * Attribute name to define the pager number.
     */
    public static final String ATTR_LDAP_PAGER_NUMBER = "pager";

    /**
     * Attribute name to define the Address Line 1.
     */
    public static final String ATTR_LDAP_ADDRESS = "siteLocation";

    /**
     * Attribute name to define the Group Member.
     */
    public static final String ATTR_LDAP_MEMBER = "member";

    /**
     * Attribute name to define the User Member Of.
     */
    public static final String ATTR_LDAP_MEMBER_OF = "memberOf";

    /**
     * Attribute name to define the Security Equivalence.
     */
    public static final String ATTR_LDAP_EQUIVALENT_TO_ME = "equivalentToMe";

    public static final String ATTR_LDAP_LOCKED_BY_INTRUDER = "lockedByIntruder";

    /**
     * RBAC schema.
     */
    public static final String ATTR_EDIR_ROLE_OCCUPANT = "roleOccupant";

    // eDirectory operational attributes (probably read ony)
    /**
     * Creator of an object.
     */
    public static final String ATTR_EDIR_CREATORS_NAME = "creatorsName";

    /**
     * Timestamp of when the object was created.
     */
    public static final String ATTR_EDIR_CREATE_TIMESTAMP = "createTimestamp";

    /**
     * Last modifier of an object.
     */
    public static final String ATTR_EDIR_MODIFIED_NAME = "modifiersName";

    /**
     * Timestamp of when the object was last modified.
     */
    public static final String ATTR_EDIR_MODIFIED_TIMESTAMP = "modifyTimestamp";

    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_CONSECUTIVE_CHARACTERS = "nspmMaxConsecutiveCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_LOWER_CHARACTERS = "nspmMaxLowerCaseCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_NONALPHA_CHARACTERS = "nspmMaxNonAlphaCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_NUMERIC_CHARACTERS = "nspmMaxNumericCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_REPEATED_CHARACTERS = "nspmMaxRepeatedCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_SPECIAL_CHARACTERS = "nspmMaxSpecialCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_UPPER_CHARACTERS = "nspmMaxUpperCaseCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_LENGTH = "nspmMaximumLength";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_LOWER_CHARACTERS = "nspmMinLowerCaseCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_NONALPHA_CHARACTERS = "nspmMinNonAlphaCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_NUMERIC_CHARACTERS = "nspmMinNumericCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_SPECIAL_CHARACTERS = "nspmMinSpecialCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_UNIQUE_CHARACTERS = "nspmMinUniqueCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_UPPER_CHARACTERS = "nspmMinUpperCaseCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_LENGTH = ATTR_LDAP_PASSWORD_MINIMUM_LENGTH;
    public static final String ATTR_EDIR_PASSWORD_POLICY_NONALPHA_ALLOWED = "nspmNonAlphaCharactersAllowed";
    public static final String ATTR_EDIR_PASSWORD_POLICY_NUMERIC_ALLOWED = "nspmNumericCharactersAllowed";
    public static final String ATTR_EDIR_PASSWORD_POLICY_NUMERIC_FIRST_ALLOWED = "nspmNumericAsFirstCharacter";
    public static final String ATTR_EDIR_PASSWORD_POLICY_NUMERIC_LAST_ALLOWED = "nspmNumericAsLastCharacter";
    public static final String ATTR_EDIR_PASSWORD_POLICY_SPECIAL_ALLOWED = "nspmSpecialCharactersAllowed";
    public static final String ATTR_EDIR_PASSWORD_POLICY_SPECIAL_FIRST_ALLOWED = "nspmSpecialAsFirstCharacter";
    public static final String ATTR_EDIR_PASSWORD_POLICY_SPECIAL_LAST_ALLOWED = "nspmSpecialAsLastCharacter";
    public static final String ATTR_EDIR_PASSWORD_POLICY_EXPIRATION_INTERVAL = "passwordExpirationInterval";
    public static final String ATTR_EDIR_PASSWORD_POLICY_PASSWORD_UNIQUE_REQUIRED = "passwordUniqueRequired";
    public static final String ATTR_EDIR_PASSWORD_POLICY_PASSWORD_HISTORY_LIMIT = "nspmPasswordHistoryLimit";
    public static final String ATTR_EDIR_PASSWORD_POLICY_PASSWORD_EXCLUDE_LIST = "nspmExcludeList";
    public static final String ATTR_EDIR_PASSWORD_POLICY_DISALLOWED_ATTRIBUTES = "nspmDisallowedAttributeValues";
    public static final String ATTR_EDIR_PASSWORD_POLICY_CASE_SENSITIVE = "nspmCaseSensitive";
    public static final String ATTR_EDIR_PASSWORD_POLICY_PASSWORD_RULE_ENFORCEMENT = "nsimPwdRuleEnforcement";
    public static final String ATTR_EDIR_PASSWORD_POLICY_OPTIONS = "nspmConfigurationOptions";
    public static final String ATTR_EDIR_PASSWORD_POLICY_CHANGE_MESSAGE = "nspmChangePasswordMessage";
    public static final String ATTR_EDIR_PASSWORD_POLICY_CHALLENGE_SET_DN = "nsimChallengeSetDN";
    public static final String ATTR_EDIR_PASSWORD_POLICY_CHALLENGE_SET_GUID = "nsimChallengeSetGUID";
    public static final String ATTR_EDIR_PASSWORD_POLICY_FORGOTTEN_ACTION = "nsimForgottenAction";
    public static final String ATTR_EDIR_PASSWORD_POLICY_FORGOTTEN_LOGIN_CONFIG = "nsimForgottenLoginConfig";
    public static final String ATTR_EDIR_PASSWORD_POLICY_PASSWORD_LIFETIME = "nspmMinPasswordLifetime";
    public static final String ATTR_EDIR_PASSWORD_POLICY_DN = "nspmPasswordPolicyDN";

    public static final String ATTR_AD_PASSWORD_POLICY_PRECEDENCE = "msDS-PasswordSettingsPrecedence";
    public static final String ATTR_AD_PASSWORD_POLICY_REVERSIBLE_ENCRYPTION = "msDS-PasswordReversibleEncryptionEnabled";
    public static final String ATTR_AD_PASSWORD_POLICY_HISTORY_LENGTH = "msDS-PasswordHistoryLength";
    public static final String ATTR_AD_PASSWORD_POLICY_COMPLEXITY_ENABLED = "msDS-PasswordComplexityEnabled";
    public static final String ATTR_AD_PASSWORD_POLICY_MIN_PASSWORD_LENGTH = "msDS-MinimumPasswordLength";
    public static final String ATTR_AD_PASSWORD_POLICY_MIN_PASSWORD_AGE = "msDS-MinimumPasswordAge";
    public static final String ATTR_AD_PASSWORD_POLICY_MAX_PASSWORD_AGE = "msDS-MaximumPasswordAge";
    public static final String ATTR_AD_PASSWORD_POLICY_LOCKOUT_THRESHOLD = "msDS-LockoutThreshold";
    public static final String ATTR_AD_PASSWORD_POLICY_LOCKOUT_WINDOW = "msDS-LockoutObservationWindow";
    public static final String ATTR_AD_PASSWORD_POLICY_LOCKOUT_DURATION = "msDS-LockoutDuration";
    public static final String ATTR_AD_PASSWORD_POLICY_APPLIES_TO = "msDS-PSOAppliesTo";
    public static final String ATTR_AD_PASSWORD_POLICY_RESULTANT_PSO = "msDS-ResultantPSO";

    public static final String ATTR_ORACLEDS_PASSWORD_POLICY_MIN_LENGTH = "pwdMinLength";
    public static final String ATTR_ORACLEDS_PASSWORD_POLICY_HISTORY_COUNT = "pwdInHistory";
    public static final String ATTR_ORACLEDS_PASSWORD_POLICY_MAX_PASSWORD_AGE = "pwdMaxAge";
    public static final String ATTR_ORACLEDS_PASSWORD_POLICY_MIN_PASSWORD_AGE = "pwdMinAge";
    public static final String ATTR_ORACLEDS_PASSWORD_SUB_ENTRY = "passwordPolicySubentry";

    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_CHECK_QUALITY = "pwdCheckQuality";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_EXPIRE_WARNING = "pwdExpireWarning";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_FAILURE_COUNT_INTERVAL = "pwdFailureCountInterval";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_GRACE_AUTHN_LIMIT = "pwdGraceAuthNLimit";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_HISTORY_COUNT = "pwdInHistory";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_LOCKOUT = "pwdLockout";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_LOCKOUT_DURATION = "pwdLockoutDuration";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_MAX_PASSWORD_AGE = "pwdMaxAge";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_MAX_PASSWORD_FAILURE = "pwdMaxFailure";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_MAX_PASSWORD_MAX_TOTAL_ATTEMPTS = "pwdMaxTotalAttempts";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_PASSWORD_AGE = "pwdMinAge";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_LENGTH = "pwdMinLength";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_MUST_CHANGE = "pwdMustChange";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_SAFE_MODIFY = "pwdSafeModify";
    public static final String ATTR_OPENLDAP_PASSWORD_SUB_ENTRY = "pwdPolicySubentry";

    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_UPPER_CHARACTERS = "minUpper";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_LOWER_CHARACTERS = "minLower";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_NUMERIC_CHARACTERS = "minNumeric";
    public static final String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_SPECIAL_CHARACTERS = "minPunct";

    public static final String ATTR_OPENLDAP_PASSWORD_ACCOUNT_LOCKED_TIME = "pwdAccountLockedTime";
    public static final String ATTR_OPENLDAP_PASSWORD_CHANGED_TIME = "pwdChangedTime";
    public static final String ATTR_OPENLDAP_PASSWORD_FAILURE_TIME = "pwdFailureTime";
    public static final String ATTR_OPENLDAP_PASSWORD_GRACE_USE_TIME = "pwdGraceUseTime";
    public static final String ATTR_OPENLDAP_PASSWORD_HISTORY = "pwdHistory";
    public static final String ATTR_OPENLDAP_PASSWORD_RESET = "pwdReset";
    public static final String ATTR_OPENLDAP_PASSWORD_UNIQUE_ATTEMPTS = "pwdUniqueAttempts";

    public static final String OBJECTCLASS_BASE_LDAP_USER = "inetOrgPerson";
    public static final String OBJECTCLASS_BASE_LDAP_GROUP = "groupOfNames";
    public static final String OBJECTCLASS_BASE_LDAP_ORGANIZATIONAL_UNIT = "organizationalUnit";
    public static final String OBJECTCLASS_BASE_LDAP_PARTITION = "Partition";
    public static final String OBJECTCLASS_BASS_LDAP_SERVER = "Server";

    public static final String OBJECTCLASS_AUX_LDAP_DYNAMIC_GROUP = "dynamicGroupAux";

    public static final Map<String, String> BUILD_MANIFEST = readBuildManifest();
    public static final String CHAI_API_VERSION = BUILD_MANIFEST.getOrDefault( "Implementation-Version", "unknown" );
    public static final String CHAI_API_WEBSITE = BUILD_MANIFEST.getOrDefault( "Implementation-URL", "unknown" );

    public static final String FILTER_OBJECTCLASS_ANY = "(" + ATTR_LDAP_OBJECTCLASS + "=" + "*" + ")";


    private static Map<String, String> readBuildManifest( )
    {
        final String interestedArchiveNonce = "854FF0D1B8B9E20E9476A6658AEF997E0ACB09ED6F9B593E086D2C8FBD83DBA8";
        final String manifestKeyName = "Archive-UID";
        final String manifestFileName = "META-INF/MANIFEST.MF";

        final Map<String, String> returnMap = new TreeMap<>();
        try
        {
            final Enumeration<URL> resources = ChaiConstant.class.getClassLoader().getResources( manifestFileName );
            while ( resources.hasMoreElements() )
            {
                try ( InputStream inputStream = resources.nextElement().openStream() )
                {
                    final Manifest manifest = new Manifest( inputStream );
                    final Attributes attributes = manifest.getMainAttributes();
                    final String archiveNonce = attributes.getValue( manifestKeyName );
                    try
                    {
                        if ( interestedArchiveNonce.equals( archiveNonce ) )
                        {
                            for ( Map.Entry<Object, Object> entry : attributes.entrySet() )
                            {
                                final Object keyObject = entry.getKey();
                                final Object valueObject = entry.getValue();
                                if ( keyObject != null && valueObject != null )
                                {
                                    returnMap.put( keyObject.toString(), valueObject.toString() );
                                }
                            }
                        }
                    }
                    catch ( final Throwable t )
                    {
                        System.out.println( t );
                    }
                }
            }
        }
        catch ( final Throwable t )
        {
            System.out.println( t );
        }

        return Collections.unmodifiableMap( returnMap );
    }
}


