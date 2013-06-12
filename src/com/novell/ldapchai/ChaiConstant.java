
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

import java.util.*;

/**
 * Constants used by the Chai API.  In general, it is encouraged to use the constants defined
 * here for attribute names instead of arbitrary strings. For example:
 * <h3>Discouraged</h3>
 * <pre>
 *    chaiUser.readStringAttribute("givenName");
 * </pre>
 * <h3>Recommended</h3>
 * <pre>
 *    chaiUser.readStringAttribute(ChaiConstant.ATTR_LDAP_GIVEN_NAME);
 * </pre>
 * <p/>
 * See the Constant Field Definitions for a list of actual values.
 *
 * @author Jason D. Rivard
 */
public interface ChaiConstant {
// ----------------------------- CONSTANTS ----------------------------

    /**
     * Attribute name to define the Object Class
     */
    public static final String ATTR_LDAP_OBJECTCLASS = "objectClass";

    /**
     * Attribute name to define the CN
     */
    public static final String ATTR_LDAP_COMMON_NAME = "cn";

    /**
     * Attribute name to define the UID or uniqueID
     */
    public static final String ATTR_LDAP_UID = "uid";

    /**
     * Attribute name to define the Company Name
     */
    public static final String ATTR_LDAP_DESCRIPTION = "description";

    /**
     * Attribute name to define the user password (Write Only)
     */
    public static final String ATTR_LDAP_USER_PASSWORD = "userPassword";

    /**
     * Attribute name to define when the user's password will expire.
     */
    public static final String ATTR_LDAP_PASSWORD_EXPIRE_TIME = "passwordExpirationTime";

    /**
     * Attribute name to define the user's password expiration interval
     */
    public static final String ATTR_LDAP_PASSWORD_EXPIRE_INTERVAL = "passwordExpirationInterval";

    /**
     * Attribute name to define the user's minimum allowed password length
     */
    public static final String ATTR_LDAP_PASSWORD_MINIMUM_LENGTH = "passwordMinimumLength";


    /**
     * Attribute name to define the first name
     */
    public static final String ATTR_LDAP_GIVEN_NAME = "givenName";

    /**
     * Attribute name to define the last name
     */
    public static final String ATTR_LDAP_SURNAME = "sn";

    /**
     * Attribute name to define the (middle) initials
     */
    public static final String ATTR_LDAP_INITIAL = "initials";

    /**
     * Attribute name to define the email of the user
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
     * Attribute name to define the group membership
     */
    public static final String ATTR_LDAP_GROUP_MEMBERSHIP = "groupMembership";

    /**
     * Attribute name to define security equality
     */
    public static final String ATTR_LDAP_SECURITY_EQUALS = "securityEquals";

    /**
     * login grace remaining count *
     */
    public static final String ATTR_LDAP_LOGIN_GRACE_REMAINING = "loginGraceRemaining";

    /**
     * login grace limit
     */
    public static final String ATTR_LDAP_LOGIN_GRACE_LIMIT = "loginGraceLimit";

    /**
     * login intruder attempts
     */
    public static final String ATTR_LDAP_LOGIN_INTRUDER_ATTEMPTS = "loginIntruderAttempts";

    /**
     * login intruder reset time
     */
    public static final String ATTR_LDAP_LOGIN_INTRUDER_RESET_TIME = "loginIntruderResetTime";

    /**
     * Attribute name to define the City.
     */
    public static final String ATTR_LDAP_CITY = "physicalDeliveryOfficeName";

    /**
     * Last time user logged in
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
     * Attribute name to define the date format
     */
    public static final String ATTR_LDAP_PREFERRED_LANGUAGE = "preferredLanguage";

    /**
     * Attribute name to define the date format
     */
    public static final String ATTR_LDAP_TIMEZONE = "Timezone";

    /**
     * Attribute name to define the Telephone Number
     */
    public static final String ATTR_LDAP_TELEPHONE_NUMBER = "telephoneNumber";

    /**
     * Attribute name to define the Workforce ID
     */
    public static final String ATTR_LDAP_WORFORCE_ID = "workforceID";

    /**
     * Attribute name to define the Fax number
     */
    public static final String ATTR_LDAP_FACSIMILE_NUMBER = "facsimileTelephoneNumber";

    /**
     * Attribute name to define the Mobile number
     */
    public static final String ATTR_LDAP_MOBILE_NUMBER = "mobile";

    /**
     * Attribute name to define the pager number
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
     * Attribute name to define the Security Equivalnce.
     */
    public static final String ATTR_LDAP_EQUIVALENT_TO_ME = "equivalentToMe";


    public static final String ATTR_LDAP_LOCKED_BY_INTRUDER = "lockedByIntruder";


    /**
     * RBAC schema
     */
    public static final String ATTR_EDIR_ROLE_OCCUPANT = "roleOccupant";

    // eDirectory operational attributes (probably read ony)
    /**
     * Creator of an object
     */
    public static final String ATTR_EDIR_CREATORS_NAME = "creatorsName";

    /**
     * Timestamp of when the object was created
     */
    public static final String ATTR_EDIR_CREATE_TIMESTAMP = "createTimestamp";

    /**
     * Last modifier of an object
     */
    public static final String ATTR_EDIR_MODIFIED_NAME = "modifiersName";

    /**
     * Timestamp of when the object was last modified
     */
    public static final String ATTR_EDIR_MODIFIED_TIMESTAMP = "modifyTimestamp";

    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_CONSECUTIVE_CHARACTERS = "nspmMaxConsecutiveCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_LOWER_CHARACTERS = "nspmMaxLowerCaseCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_NUMERIC_CHARACTERS = "nspmMaxNumericCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_REPEATED_CHARACTERS = "nspmMaxRepeatedCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_SPECIAL_CHARACTERS = "nspmMaxSpecialCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_UPPER_CHARACTERS = "nspmMaxUpperCaseCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MAX_LENGTH = "nspmMaximumLength";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_LOWER_CHARACTERS = "nspmMinLowerCaseCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_NUMERIC_CHARACTERS = "nspmMinNumericCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_SPECIAL_CHARACTERS = "nspmMinSpecialCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_UNIQUE_CHARACTERS = "nspmMinUniqueCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_UPPER_CHARACTERS = "nspmMinUpperCaseCharacters";
    public static final String ATTR_EDIR_PASSWORD_POLICY_MIN_LENGTH = ATTR_LDAP_PASSWORD_MINIMUM_LENGTH;
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

    public static final String OBJECTCLASS_BASE_LDAP_USER = "inetOrgPerson";
    public static final String OBJECTCLASS_BASE_LDAP_GROUP = "groupOfNames";
    public static final String OBJECTCLASS_BASE_LDAP_ORGANIZATIONAL_UNIT = "organizationalUnit";
    public static final String OBJECTCLASS_BASE_LDAP_PARTITION = "Partition";
    public static final String OBJECTCLASS_BASS_LDAP_SERVER = "Server";

    public static final String OBJECTCLASS_AUX_LDAP_DYNAMIC_GROUP = "dynamicGroupAux";

    public static final Map<String, String> CHAI_API_BUILD_PROPERTIES = ChaiConstant_BuildPropertiesPopulator.BUILD_PROPERTIES;

    public static final String CHAI_API_VERSION = CHAI_API_BUILD_PROPERTIES.get("chai.version");
    public static final String CHAI_API_BUILD_INFO = CHAI_API_BUILD_PROPERTIES.get("build.number");

// -------------------------- INNER CLASSES --------------------------
}

/**
 * Added as a second class in the {@code ChaiConstant} .java file to hide the implementation
 * from public javadocs.
 */
abstract class ChaiConstant_BuildPropertiesPopulator {
    static final Map<String, String> BUILD_PROPERTIES;

    static {
        final ResourceBundle theBundle = ResourceBundle.getBundle("com.novell.ldapchai.BuildInformation");
        final Map<String, String> theProps = new HashMap<String, String>();
        for (Enumeration<String> keyEnum = theBundle.getKeys(); keyEnum.hasMoreElements();) {
            final String key = keyEnum.nextElement();
            theProps.put(key, theBundle.getString(key));
        }
        BUILD_PROPERTIES = Collections.unmodifiableMap(theProps);
    }
}
