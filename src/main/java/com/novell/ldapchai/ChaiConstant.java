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

package com.novell.ldapchai;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Constants used by the Chai API. In general, it is encouraged to use the
 * constants defined here for attribute names instead of arbitrary strings. For
 * example: <h3>Discouraged</h3>
 * 
 * <pre>
 * chaiUser.readStringAttribute(&quot;givenName&quot;);
 * </pre>
 * 
 * <h3>Recommended</h3>
 * 
 * <pre>
 * chaiUser.readStringAttribute(ChaiConstant.ATTR_LDAP_GIVEN_NAME);
 * </pre>
 *
 * See the Constant Field Definitions for a list of actual values.
 *
 * @author Jason D. Rivard
 */
public interface ChaiConstant {


    /**
     * Attribute name to define the Object Class
     */
    String ATTR_LDAP_OBJECTCLASS = "objectClass";

    /**
     * Attribute name to define the CN
     */
    String ATTR_LDAP_COMMON_NAME = "cn";

    /**
     * Attribute name to define the UID or uniqueID
     */
    String ATTR_LDAP_UID = "uid";

    /**
     * Attribute name to define the Company Name
     */
    String ATTR_LDAP_DESCRIPTION = "description";

    /**
     * Attribute name to define the user password (Write Only)
     */
    String ATTR_LDAP_USER_PASSWORD = "userPassword";

    /**
     * Attribute name to define when the user's password will expire.
     */
    String ATTR_LDAP_PASSWORD_EXPIRE_TIME = "passwordExpirationTime";

    /**
     * Attribute name to define the user's password expiration interval
     */
    String ATTR_LDAP_PASSWORD_EXPIRE_INTERVAL = "passwordExpirationInterval";

    /**
     * Attribute name to define the user's minimum allowed password length
     */
    String ATTR_LDAP_PASSWORD_MINIMUM_LENGTH = "passwordMinimumLength";

    /**
     * Attribute name to define the first name
     */
    String ATTR_LDAP_GIVEN_NAME = "givenName";

    /**
     * Attribute name to define the last name
     */
    String ATTR_LDAP_SURNAME = "sn";

    /**
     * Attribute name to define the (middle) initials
     */
    String ATTR_LDAP_INITIAL = "initials";

    /**
     * Attribute name to define the email of the user
     */
    String ATTR_LDAP_EMAIL = "mail";

    /**
     * Attribute name to define the user's login is disabled or not.
     */
    String ATTR_LDAP_LOGIN_DISABLED = "loginDisabled";

    String ATTR_LDAP_MANAGER = "manager";

    String ATTR_LDAP_DIRECT_REPORTS = "directReports";

    String ATTR_LDAP_ASSISTANT = "assistant";

    /**
     * Attribute name to define the group membership
     */
    String ATTR_LDAP_GROUP_MEMBERSHIP = "groupMembership";

    /**
     * Attribute name to define security equality
     */
    String ATTR_LDAP_SECURITY_EQUALS = "securityEquals";

    /**
     * login grace remaining count *
     */
    String ATTR_LDAP_LOGIN_GRACE_REMAINING = "loginGraceRemaining";

    /**
     * login grace limit
     */
    String ATTR_LDAP_LOGIN_GRACE_LIMIT = "loginGraceLimit";

    /**
     * login intruder attempts
     */
    String ATTR_LDAP_LOGIN_INTRUDER_ATTEMPTS = "loginIntruderAttempts";

    /**
     * login intruder reset time
     */
    String ATTR_LDAP_LOGIN_INTRUDER_RESET_TIME = "loginIntruderResetTime";

    /**
     * Attribute name to define the City.
     */
    String ATTR_LDAP_CITY = "physicalDeliveryOfficeName";

    /**
     * Last time user logged in
     */
    String ATTR_LDAP_LAST_LOGIN_TIME = "loginTime";

    /**
     * Attribute name to define the Province/State.
     */
    String ATTR_LDAP_STATE = "st";

    /**
     * Attribute name to define the Country.
     */
    String ATTR_LDAP_COUNTRY = "co";

    /**
     * Attribute name to define the Postal Code.
     */
    String ATTR_LPAP_POSTAL_CODE = "postalCode";

    /**
     * Attribute name to define the date format
     */
    String ATTR_LDAP_PREFERRED_LANGUAGE = "preferredLanguage";

    /**
     * Attribute name to define the date format
     */
    String ATTR_LDAP_TIMEZONE = "Timezone";

    /**
     * Attribute name to define the Telephone Number
     */
    String ATTR_LDAP_TELEPHONE_NUMBER = "telephoneNumber";

    /**
     * Attribute name to define the Workforce ID
     */
    String ATTR_LDAP_WORFORCE_ID = "workforceID";

    /**
     * Attribute name to define the Fax number
     */
    String ATTR_LDAP_FACSIMILE_NUMBER = "facsimileTelephoneNumber";

    /**
     * Attribute name to define the Mobile number
     */
    String ATTR_LDAP_MOBILE_NUMBER = "mobile";

    /**
     * Attribute name to define the pager number
     */
    String ATTR_LDAP_PAGER_NUMBER = "pager";

    /**
     * Attribute name to define the Address Line 1.
     */
    String ATTR_LDAP_ADDRESS = "siteLocation";

    /**
     * Attribute name to define the Group Member.
     */
    String ATTR_LDAP_MEMBER = "member";

    /**
     * Attribute name to define the User Member Of.
     */
    String ATTR_LDAP_MEMBER_OF = "memberOf";

    /**
     * Attribute name to define the Security Equivalence.
     */
    String ATTR_LDAP_EQUIVALENT_TO_ME = "equivalentToMe";

    String ATTR_LDAP_LOCKED_BY_INTRUDER = "lockedByIntruder";

    /**
     * RBAC schema
     */
    String ATTR_EDIR_ROLE_OCCUPANT = "roleOccupant";

    // eDirectory operational attributes (probably read ony)
    /**
     * Creator of an object
     */
    String ATTR_EDIR_CREATORS_NAME = "creatorsName";

    /**
     * Timestamp of when the object was created
     */
    String ATTR_EDIR_CREATE_TIMESTAMP = "createTimestamp";

    /**
     * Last modifier of an object
     */
    String ATTR_EDIR_MODIFIED_NAME = "modifiersName";

    /**
     * Timestamp of when the object was last modified
     */
    String ATTR_EDIR_MODIFIED_TIMESTAMP = "modifyTimestamp";

    String ATTR_EDIR_PASSWORD_POLICY_MAX_CONSECUTIVE_CHARACTERS = "nspmMaxConsecutiveCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MAX_LOWER_CHARACTERS = "nspmMaxLowerCaseCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MAX_NUMERIC_CHARACTERS = "nspmMaxNumericCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MAX_REPEATED_CHARACTERS = "nspmMaxRepeatedCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MAX_SPECIAL_CHARACTERS = "nspmMaxSpecialCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MAX_UPPER_CHARACTERS = "nspmMaxUpperCaseCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MAX_LENGTH = "nspmMaximumLength";
    String ATTR_EDIR_PASSWORD_POLICY_MIN_LOWER_CHARACTERS = "nspmMinLowerCaseCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MIN_NUMERIC_CHARACTERS = "nspmMinNumericCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MIN_SPECIAL_CHARACTERS = "nspmMinSpecialCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MIN_UNIQUE_CHARACTERS = "nspmMinUniqueCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MIN_UPPER_CHARACTERS = "nspmMinUpperCaseCharacters";
    String ATTR_EDIR_PASSWORD_POLICY_MIN_LENGTH = ATTR_LDAP_PASSWORD_MINIMUM_LENGTH;
    String ATTR_EDIR_PASSWORD_POLICY_NUMERIC_ALLOWED = "nspmNumericCharactersAllowed";
    String ATTR_EDIR_PASSWORD_POLICY_NUMERIC_FIRST_ALLOWED = "nspmNumericAsFirstCharacter";
    String ATTR_EDIR_PASSWORD_POLICY_NUMERIC_LAST_ALLOWED = "nspmNumericAsLastCharacter";
    String ATTR_EDIR_PASSWORD_POLICY_SPECIAL_ALLOWED = "nspmSpecialCharactersAllowed";
    String ATTR_EDIR_PASSWORD_POLICY_SPECIAL_FIRST_ALLOWED = "nspmSpecialAsFirstCharacter";
    String ATTR_EDIR_PASSWORD_POLICY_SPECIAL_LAST_ALLOWED = "nspmSpecialAsLastCharacter";
    String ATTR_EDIR_PASSWORD_POLICY_EXPIRATION_INTERVAL = "passwordExpirationInterval";
    String ATTR_EDIR_PASSWORD_POLICY_PASSWORD_UNIQUE_REQUIRED = "passwordUniqueRequired";
    String ATTR_EDIR_PASSWORD_POLICY_PASSWORD_HISTORY_LIMIT = "nspmPasswordHistoryLimit";
    String ATTR_EDIR_PASSWORD_POLICY_PASSWORD_EXCLUDE_LIST = "nspmExcludeList";
    String ATTR_EDIR_PASSWORD_POLICY_DISALLOWED_ATTRIBUTES = "nspmDisallowedAttributeValues";
    String ATTR_EDIR_PASSWORD_POLICY_CASE_SENSITIVE = "nspmCaseSensitive";
    String ATTR_EDIR_PASSWORD_POLICY_PASSWORD_RULE_ENFORCEMENT = "nsimPwdRuleEnforcement";
    String ATTR_EDIR_PASSWORD_POLICY_OPTIONS = "nspmConfigurationOptions";
    String ATTR_EDIR_PASSWORD_POLICY_CHANGE_MESSAGE = "nspmChangePasswordMessage";
    String ATTR_EDIR_PASSWORD_POLICY_CHALLENGE_SET_DN = "nsimChallengeSetDN";
    String ATTR_EDIR_PASSWORD_POLICY_CHALLENGE_SET_GUID = "nsimChallengeSetGUID";
    String ATTR_EDIR_PASSWORD_POLICY_FORGOTTEN_ACTION = "nsimForgottenAction";
    String ATTR_EDIR_PASSWORD_POLICY_FORGOTTEN_LOGIN_CONFIG = "nsimForgottenLoginConfig";
    String ATTR_EDIR_PASSWORD_POLICY_PASSWORD_LIFETIME = "nspmMinPasswordLifetime";

    String ATTR_AD_PASSWORD_POLICY_PRECEDENCE = "msDS-PasswordSettingsPrecedence";
    String ATTR_AD_PASSWORD_POLICY_REVERSIBLE_ENCRYPTION = "msDS-PasswordReversibleEncryptionEnabled";
    String ATTR_AD_PASSWORD_POLICY_HISTORY_LENGTH = "msDS-PasswordHistoryLength";
    String ATTR_AD_PASSWORD_POLICY_COMPLEXITY_ENABLED = "msDS-PasswordComplexityEnabled";
    String ATTR_AD_PASSWORD_POLICY_MIN_PASSWORD_LENGTH = "msDS-MinimumPasswordLength";
    String ATTR_AD_PASSWORD_POLICY_MIN_PASSWORD_AGE = "msDS-MinimumPasswordAge";
    String ATTR_AD_PASSWORD_POLICY_MAX_PASSWORD_AGE = "msDS-MaximumPasswordAge";
    String ATTR_AD_PASSWORD_POLICY_LOCKOUT_THRESHOLD = "msDS-LockoutThreshold";
    String ATTR_AD_PASSWORD_POLICY_LOCKOUT_WINDOW = "msDS-LockoutObservationWindow";
    String ATTR_AD_PASSWORD_POLICY_LOCKOUT_DURATION = "msDS-LockoutDuration";
    String ATTR_AD_PASSWORD_POLICY_APPLIES_TO = "msDS-PSOAppliesTo";
    String ATTR_AD_PASSWORD_POLICY_RESULTANT_PSO = "msDS-ResultantPSO";

    String ATTR_ORACLEDS_PASSWORD_POLICY_MIN_LENGTH = "pwdMinLength";
    String ATTR_ORACLEDS_PASSWORD_POLICY_HISTORY_COUNT = "pwdInHistory";
    String ATTR_ORACLEDS_PASSWORD_POLICY_MAX_PASSWORD_AGE = "pwdMaxAge";
    String ATTR_ORACLEDS_PASSWORD_POLICY_MIN_PASSWORD_AGE = "pwdMinAge";
    String ATTR_ORACLEDS_PASSWORD_SUB_ENTRY = "passwordPolicySubentry";

    String ATTR_OPENLDAP_PASSWORD_POLICY_CHECK_QUALITY = "pwdCheckQuality";
    String ATTR_OPENLDAP_PASSWORD_POLICY_EXPIRE_WARNING = "pwdExpireWarning";
    String ATTR_OPENLDAP_PASSWORD_POLICY_FAILURE_COUNT_INTERVAL = "pwdFailureCountInterval";
    String ATTR_OPENLDAP_PASSWORD_POLICY_GRACE_AUTHN_LIMIT = "pwdGraceAuthNLimit";
    String ATTR_OPENLDAP_PASSWORD_POLICY_HISTORY_COUNT = "pwdInHistory";
    String ATTR_OPENLDAP_PASSWORD_POLICY_LOCKOUT = "pwdLockout";
    String ATTR_OPENLDAP_PASSWORD_POLICY_LOCKOUT_DURATION = "pwdLockoutDuration";
    String ATTR_OPENLDAP_PASSWORD_POLICY_MAX_PASSWORD_AGE = "pwdMaxAge";
    String ATTR_OPENLDAP_PASSWORD_POLICY_MAX_PASSWORD_FAILURE = "pwdMaxFailure";
    String ATTR_OPENLDAP_PASSWORD_POLICY_MAX_PASSWORD_MAX_TOTAL_ATTEMPTS = "pwdMaxTotalAttempts";
    String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_PASSWORD_AGE = "pwdMinAge";
    String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_LENGTH = "pwdMinLength";
    String ATTR_OPENLDAP_PASSWORD_POLICY_MUST_CHANGE = "pwdMustChange";
    String ATTR_OPENLDAP_PASSWORD_POLICY_SAFE_MODIFY = "pwdSafeModify";
    String ATTR_OPENLDAP_PASSWORD_SUB_ENTRY = "pwdPolicySubentry";

    String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_UPPER_CHARACTERS = "minUpper";
    String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_LOWER_CHARACTERS = "minLower";
    String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_NUMERIC_CHARACTERS = "minNumeric";
    String ATTR_OPENLDAP_PASSWORD_POLICY_MIN_SPECIAL_CHARACTERS = "minPunct";

    String ATTR_OPENLDAP_PASSWORD_ACCOUNT_LOCKED_TIME = "pwdAccountLockedTime";
    String ATTR_OPENLDAP_PASSWORD_CHANGED_TIME = "pwdChangedTime";
    String ATTR_OPENLDAP_PASSWORD_FAILURE_TIME = "pwdFailureTime";
    String ATTR_OPENLDAP_PASSWORD_GRACE_USE_TIME = "pwdGraceUseTime";
    String ATTR_OPENLDAP_PASSWORD_HISTORY = "pwdHistory";
    String ATTR_OPENLDAP_PASSWORD_RESET = "pwdReset";
    String ATTR_OPENLDAP_PASSWORD_UNIQUE_ATTEMPTS = "pwdUniqueAttempts";

    String OBJECTCLASS_BASE_LDAP_USER = "inetOrgPerson";
    String OBJECTCLASS_BASE_LDAP_GROUP = "groupOfNames";
    String OBJECTCLASS_BASE_LDAP_ORGANIZATIONAL_UNIT = "organizationalUnit";
    String OBJECTCLASS_BASE_LDAP_PARTITION = "Partition";
    String OBJECTCLASS_BASS_LDAP_SERVER = "Server";

    String OBJECTCLASS_AUX_LDAP_DYNAMIC_GROUP = "dynamicGroupAux";

    Map<String, String> CHAI_API_BUILD_PROPERTIES = ChaiConstant_BuildPropertiesPopulator.BUILD_PROPERTIES;

    String CHAI_API_VERSION = CHAI_API_BUILD_PROPERTIES.get("chai.version");

    String FILTER_OBJECTCLASS_ANY = "(" + ATTR_LDAP_OBJECTCLASS + "=" + "*" + ")";



}

/**
 * Added as a second class in the {@code ChaiConstant} .java file to hide the
 * implementation from public javadocs.
 */
@SuppressWarnings("checkstyle:OneTopLevelClass")
abstract class ChaiConstant_BuildPropertiesPopulator {
    static final Map<String, String> BUILD_PROPERTIES;

    static {
        final ResourceBundle theBundle = ResourceBundle.getBundle("com.novell.ldapchai.BuildInformation");
        final Map<String, String> theProps = new HashMap<String, String>();
        for (Enumeration<String> keyEnum = theBundle.getKeys(); keyEnum.hasMoreElements(); ) {
            final String key = keyEnum.nextElement();
            theProps.put(key, theBundle.getString(key));
        }
        BUILD_PROPERTIES = Collections.unmodifiableMap(theProps);
    }
}
