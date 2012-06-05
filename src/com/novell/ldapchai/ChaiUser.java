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

import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;

import java.util.Date;
import java.util.Set;

/**
 * Represents an ldap user entry.
 * <p/>
 * This interface should be the primary means by which the LDAP Chai API is used to interact with ldap user entries.
 * <p/>
 * Instances of ChaiUser can be obtained by using {@link com.novell.ldapchai.ChaiFactory}.
 *
 * @author Jason D. Rivard
 */
public interface ChaiUser extends ChaiEntry {
// ----------------------------- CONSTANTS ----------------------------

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_COMMON_NAME = ChaiConstant.ATTR_LDAP_COMMON_NAME;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_UID = ChaiConstant.ATTR_LDAP_UID;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_GIVEN_NAME = ChaiConstant.ATTR_LDAP_GIVEN_NAME;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_SURNAME = ChaiConstant.ATTR_LDAP_SURNAME;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_INITIAL = ChaiConstant.ATTR_LDAP_INITIAL;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_PASSWORD = ChaiConstant.ATTR_LDAP_USER_PASSWORD;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_EMAIL = ChaiConstant.ATTR_LDAP_EMAIL;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_PASSWORD_MINIMUM_LENGTH = ChaiConstant.ATTR_LDAP_PASSWORD_MINIMUM_LENGTH;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_LOGIN_DISABLED = ChaiConstant.ATTR_LDAP_LOGIN_DISABLED;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_INTRUDER_RESET_TIME = ChaiConstant.ATTR_LDAP_LOGIN_INTRUDER_RESET_TIME;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_LOCKED_BY_INTRUDER = ChaiConstant.ATTR_LDAP_LOCKED_BY_INTRUDER;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_PASSWORD_EXPIRE_INTERVAL = ChaiConstant.ATTR_LDAP_PASSWORD_EXPIRE_INTERVAL;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_PASSWORD_EXPIRE_TIME = ChaiConstant.ATTR_LDAP_PASSWORD_EXPIRE_TIME;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_MANAGER = ChaiConstant.ATTR_LDAP_MANAGER;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_ASSISTANT = ChaiConstant.ATTR_LDAP_ASSISTANT;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_DIRECT_REPORTS = ChaiConstant.ATTR_LDAP_DIRECT_REPORTS;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_GROUP_MEMBERSHIP = ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_TELEPHONE = ChaiConstant.ATTR_LDAP_TELEPHONE_NUMBER;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_WORKFORCEID = ChaiConstant.ATTR_LDAP_WORFORCE_ID;

// -------------------------- OTHER METHODS --------------------------

    /**
     * Make this user a member of the specified group.  This method takes care of all four attribute assignments
     * used in eDirectory static groups.
     *
     * @param theGroup The group to assign the user to.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see ChaiGroup#addMember(ChaiUser)
     */
    void addGroupMembership(ChaiGroup theGroup)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Changes this ChaiUser's password.  This uses the normal change password method in ldap (remove the old, add the new).
     * If the old password is not correct, or the new password does not meet the server's requirements, a ChaiOperationException exception
     * will be thrown.
     * <p/>
     * This method should only be used when the user is the one changing his or her *own* password.  For admin
     * password changes, use {@link #setPassword(String)}.
     * <p/>
     * This method does <i>not</i> directly set the users password expiration time attribute, but the ldap directory
     * will typically future date the expiration time during the change operation.
     * <p/>
     * It would be prudent to check the password first using the {@link #testPasswordPolicy(String)} method before attempting the password
     * set.
     *
     * @param oldPassword Old password value, must be correct or the ldap directory will prohibt the change
     * @param newPassword A new password value that conforms to the users password policy
     * @throws ChaiPasswordPolicyException If the new password does not meet the user's password policy
     * @throws ChaiUnavailableException    If the directory server(s) are unavailable
     * @see #setPassword(String)
     */
    void changePassword(String oldPassword, String newPassword)
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException;

    /**
     * Mark the user's password as being expired.
     *
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    void expirePassword()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Convenience method to find the this ChaiUser's assistant.  Evaluates the {@link #ATTR_ASSISTANT} attribute
     * and returns the equivalent ChaiUser.
     *
     * @return a ChaiUser instance referencing the assistant of the user or null if there is no assistant
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    ChaiUser getAssistant()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Convenience method to find this ChaiUser's direct reports.  Evaluates the {@link #ATTR_DIRECT_REPORTS} attribute
     * and returns the equivalent ChaiUser.
     *
     * @return A collection of ChaiUser instances that represent this ChaiUsers's direct reports.  If this ChaiUser does not have any direct reports, then an empty collection is retured.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    Set<ChaiUser> getDirectReports()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Convenience method to find this ChaiUser's group memberships.  Evaluates the {@link #ATTR_GROUP_MEMBERSHIP} attribute
     * and returns the equivalent ChaiGroups.
     *
     * @return A collection of {@link ChaiGroup} instances that represent this ChaiUsers's direct reports.  If this ChaiUser does not have any group memberships, then an empty collection is retured.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    Set<ChaiGroup> getGroups()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Convenience method to find this ChaiUser's manager.  Evaluates the {@link #ATTR_MANAGER} attribute
     * and returns the equivalent ChaiUser.
     *
     * @return a ChaiUser instance referencing the manager of the user or null if there is no manager
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    ChaiUser getManager()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Retreive this ChaiUser's password policy.  The implementation evaluates both
     * Universal Password policies as well as legacy policy settings to determine
     * the password policy.
     *
     * @return a valid password policy for the user.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    ChaiPasswordPolicy getPasswordPolicy()
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Check to see if a user's password is expired.
     *
     * @return true if the password is expired
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Convienence method to read this ChaiUser instance's {@link #ATTR_GIVEN_NAME} attribute.
     *
     * @return The value of the attribute, or null if no value
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    String readGivenName()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Convenience method to read this ChaiUser last login time.
     *
     * @return The value of the attribute, or null if no value
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    Date readLastLoginTime()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Retrieve the user's current password.  This method is likely to fail if a variety of conditions are not met:
     * <ol>
     * <li>The connection to the server is SSL</li>
     * <li>The ChaiProvider's {@link com.novell.ldapchai.provider.ChaiSetting#EDIRECTORY_ENABLE_NMAS}
     * is set to true.
     * <li>The user entry's assigned password policy supports user or admin retrieval of passwords
     * <li>The connection has rights to retrieve the password</li>
     * </ol>
     *
     * @return the users password
     * @throws UnsupportedOperationException If the configuration of the provider is not suitable for retrieving passwords.
     * @throws ChaiOperationException        If there is an error during the operation
     * @throws ChaiUnavailableException      If the directory server(s) are unavailable
     */
    String readPassword()
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Read the user's password expiration date.  The implementation will attempt to read the user's defined or calculated
     * password date.
     * <ol>
     *
     * @return the date at which the password is expired, or the current time if the password is expired but a date cannot be determined
     * @throws UnsupportedOperationException If the configuration of the provider is not suitable for retreiving passwords.
     * @throws ChaiOperationException        If there is an error during the operation
     * @throws ChaiUnavailableException      If the directory server(s) are unavailable
     */
    Date readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Convienence method to read this ChaiUser instance's {@link #ATTR_SURNAME} attribute.
     *
     * @return The value of the attribute, or null if no value
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    String readSurname()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Convienence method to read this ChaiUser instance's {@link #ATTR_COMMON_NAME} attribute.
     *
     * @return The value of the attribute, or null if no value
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    String readUsername()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Remove this ChaiUser from the specified group.  This method takes care of all four attribute assignments
     * used in eDirectory static groups.
     *
     * @param theGroup The group to assign the user to.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.ChaiGroup#removeMember(ChaiUser)
     */
    void removeGroupMembership(ChaiGroup theGroup)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Sets this ChaiUser's password.  This uses the normal administrative set password method in ldap.
     * If the old password is not correct, or the new password does not meet the server's requirements, a discriptive exception
     * will be thrown.
     * <p/>
     * This method should only be used for administators setting a different user's password.  For self
     * changes, use {@link #changePassword(String,String)}.
     * <p/>
     * This method does <i>not</i> directly set the users password expiration time attribute, but the ldap directory
     * will typically mark the current date as the password expiration time during the set operation, causing the password to
     * be expired (and changed) for the user during the next authentication.
     * <p/>
     * It would be prudent to check the password first using the {@link #testPasswordPolicy(String)} method before attempting the password
     * set.
     * <p/>
     *
     * @param newPassword A new password value that conforms to the users password policy
     * @throws ChaiPasswordPolicyException If the new password does not meet the user's password policy
     * @throws ChaiUnavailableException    If the directory server(s) are unavailable
     * @throws com.novell.ldapchai.exception.ChaiOperationException If there is an error while setting the password
     * @see #changePassword(String,String)
     */
    void setPassword(String newPassword)
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException;

    /**
     * Test a users value for this ChaiUser's password.  Appropriate rights are required for this to work properly.  This call
     * generally only tests the password value itself, and not any other authentication meta attributes such as account
     * disabled, or other authentication restrictions.
     * <p/>
     * Thus, a succcessfull test does not neccessarily mean that an authentication (BIND) would work with this password.
     * <p/>
     * <i>Implementation Note:</i> Calling this method is essentially the same as calling {@link #compareStringAttribute(String USER_PASSWORD, String value)} and converting
     * {@code ChaiOperationalException} to {@code ChaiPasswordPolicyException}.
     *
     * @param passwordValue A new password to be tested against the ldap directory
     * @return true if password is correct.
     * @throws ChaiPasswordPolicyException If the password does not meet the user's password policy
     * @throws ChaiUnavailableException    If the directory server(s) are unavailable
     * @see #changePassword(String,String)
     * @see #setPassword(String)
     */
    boolean testPassword(String passwordValue)
            throws ChaiUnavailableException, ChaiPasswordPolicyException;

    /**
     * Test a new value for this ChaiUser's password.  This method does not cause a change or set to actually occur.  This
     * is useful before calling the {@link #setPassword(String)} or {@link #changePassword(String, String)} methods.
     * <p/>
     *
     * @param testPassword A new password value that conforms to the users password policy
     * @return true if password meets the user's policy.  Never returns false (returns {@code ChaiPasswordPolicyException} instead)
     * @throws ChaiPasswordPolicyException If the password does not meet the user's password policy
     * @throws ChaiUnavailableException    If the directory server(s) are unavailable
     * @see #changePassword(String,String)
     * @see #setPassword(String)
     */
    boolean testPasswordPolicy(String testPassword)
            throws ChaiUnavailableException, ChaiPasswordPolicyException;

    boolean isAccountEnabled()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Unlocks a user from many conditions that would prevent the user from logging in.
     * Primarily, intruder detection triggers will be unlocked on the user.
     *
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    void unlock()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Checks if a user account is locked.
     * Primarily, this checks for states similar to intruder detection or account lock due to
     * incorrect login attempts.
     *
     * @return true if the account is in a locked state
     *
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    boolean isLocked()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Attempts to read the modification timestamp of the user's password.  Depending on the {@link com.novell.ldapchai.provider.ChaiProvider.DIRECTORY_VENDOR},
     * the implementation may or may not be able to reliably read this value.
     *
     * @return Date of the user's last password modification time, or null if unable to read.
     *
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    Date readPasswordModificationDate()
        throws ChaiOperationException, ChaiUnavailableException;
}

