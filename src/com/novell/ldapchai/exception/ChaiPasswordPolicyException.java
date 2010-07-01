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

package com.novell.ldapchai.exception;

import com.novell.security.nmas.NMASConstants;

/**
 * Password policy exceptions.  Thrown when a password does not match a password policy
 * for some reason.
 *
 * @author Jason D. Rivard
 */
public class ChaiPasswordPolicyException extends Exception {
// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    /**
     * List of possible password policy errors.  Most of these errors map directly to eDirectory errors.
     */
    public enum PASSWORD_ERROR {
        UNKNOWN_ERROR("Password_UnknownError", Integer.MIN_VALUE),
        PREVIOUSLYUSED("Password_PreviouslyUsed", ChaiErrorCode.DUPLICATE_PASSWORD.getErrorCode()),
        BADOLDPASSWORD("Password_BadOldPassword", ChaiErrorCode.FAILED_AUTHENTICATION.getErrorCode()),
        BADPASSWORD("Password_BadPassword", ChaiErrorCode.BAD_PASSWORD.getErrorCode()),
        TOO_SHORT("Password_TooShort", ChaiErrorCode.PASSWORD_TOO_SHORT.getErrorCode()),
        TOO_LONG("Password_TooLong", NMASConstants.NMAS_E_PASSWORD_TOO_LONG),
        NOT_ENOUGH_NUM("Password_NotEnoughNum", NMASConstants.NMAS_E_PASSWORD_NUMERIC_MIN),
        NOT_ENOUGH_SPECIAL("Password_NotEnoughSpecial", NMASConstants.NMAS_E_PASSWORD_SPECIAL_MIN),
        NOT_ENOUGH_ALPHA("Password_NotEnoughAlpha", -1),
        NOT_ENOUGH_LOWER("Password_NotEnoughLower", NMASConstants.NMAS_E_PASSWORD_LOWER_MIN),
        NOT_ENOUGH_UPPER("Password_NotEnoughUpper", NMASConstants.NMAS_E_PASSWORD_UPPER_MIN),
        NOT_ENOUGH_UNIQUE("Password_NotEnoughUnique", NMASConstants.NMAS_E_PASSWORD_UNIQUE_MIN),
        TOO_MANY_REPEAT("Password_TooManyRepeat", NMASConstants.NMAS_E_PASSWORD_REPEAT_CHAR_MAX),
        TOO_MANY_NUMERIC("Password_TooManyNumeric", NMASConstants.NMAS_E_PASSWORD_NUMERIC_MAX),
        TOO_MANY_ALPHA("Password_TooManyAlpha", -1),
        TOO_MANY_LOWER("Password_TooManyLower", NMASConstants.NMAS_E_PASSWORD_LOWER_MAX),
        TOO_MANY_UPPER("Password_TooManyUpper", NMASConstants.NMAS_E_PASSWORD_UPPER_MAX),
        FIRST_IS_NUMERIC("Password_FirstIsNumeric", NMASConstants.NMAS_E_PASSWORD_NUMERIC_FIRST),
        LAST_IS_NUMERIC("Password_LastIsNumeric", NMASConstants.NMAS_E_PASSWORD_NUMERIC_LAST),
        FIRST_IS_SPECIAL("Password_FirstIsSpecial", NMASConstants.NMAS_E_PASSWORD_SPECIAL_FIRST),
        LAST_IS_SPECIAL("Password_LastIsSpecial", NMASConstants.NMAS_E_PASSWORD_SPECIAL_LAST),
        TOO_MANY_SPECIAL("Password_TooManySpecial", NMASConstants.NMAS_E_PASSWORD_SPECIAL_MAX),
        INVALID_CHAR("Password_InvalidChar", NMASConstants.NMAS_E_PASSWORD_EXTENDED_DISALLOWED),
        INWORDLIST("Password_InWordlist", NMASConstants.NMAS_E_PASSWORD_EXCLUDE),
        SAMEASATTR("Password_SameAsAttr", NMASConstants.NMAS_E_PASSWORD_ATTR_VALUE),
        HISTORY_FULL("Password_HistoryFull", -1696),
        NUMERIC_DISALLOWED("Password_NumericDisallowed", NMASConstants.NMAS_E_PASSWORD_NUMERIC_DISALLOWED),
        SPECIAL_DISALLOWED("Password_SpecialDisallowed", NMASConstants.NMAS_E_PASSWORD_SPECIAL_DISALLOWED),
        TOO_SOON("Password_TooSoon", NMASConstants.NMAS_E_PASSWORD_LIFE_MIN)

        ;

        /**
         * An error key suitable for using as a key for a ResourceBundle
         */
        private String errorKey;
        private int errorCode;

        private PASSWORD_ERROR(final String errorKey, final int nmasErrorCode)
        {
            this.errorKey = errorKey;
            this.errorCode = nmasErrorCode;
        }

        /**
         * @return An error key suitable for using as a key for a ResourceBundle
         */
        public String getErrorKey()
        {
            return errorKey;
        }

        /**
         * @return An associated error number, typically an eDirectory ldap error code.
         */
        public int getErrorCode()
        {
            return errorCode;
        }

        private static PASSWORD_ERROR forErrorCode(final int code)
        {
            PASSWORD_ERROR result = UNKNOWN_ERROR;
            for (final PASSWORD_ERROR loopError : PASSWORD_ERROR.values()) {
                if (loopError.getErrorCode() == code) {
                    result = loopError;
                    break;
                }
            }
            return result;
        }

        private static PASSWORD_ERROR forErrorString(final String errorString)
        {
            PASSWORD_ERROR result = UNKNOWN_ERROR;
            for (final PASSWORD_ERROR loopError : PASSWORD_ERROR.values()) {
                if (errorString.indexOf(String.valueOf(loopError.getErrorCode())) != -1) {
                    result = loopError;
                    break;
                }
            }
            return result;
        }
    }

// ------------------------------ FIELDS ------------------------------

    private PASSWORD_ERROR passwordError;

// -------------------------- STATIC METHODS --------------------------

    /**
     * Creates a {@code ChaiPasswordPolicyException} based on an eDirectory error code.
     *
     * @param errorCode an eDirectory error code
     * @return A {@code ChaiPasswordPolicyException} with a {@code PASSWORD_ERROR} that matches the supplied error code,
     *         or {@link com.novell.ldapchai.exception.ChaiPasswordPolicyException.PASSWORD_ERROR#UNKNOWN_ERROR} if the
     *         error can not be matched.
     */
    public static ChaiPasswordPolicyException forErrorCode(final int errorCode)
    {
        return new ChaiPasswordPolicyException(PASSWORD_ERROR.forErrorCode(errorCode));
    }

    /**
     * Creates a {@code ChaiPasswordPolicyException} based on an eDirectory found inside the String error.
     *
     * @param errorString a string with an eDirectory error code contained inside the string
     * @return A {@code ChaiPasswordPolicyException} with a {@code PASSWORD_ERROR} that matches the supplied error code,
     *         or {@link com.novell.ldapchai.exception.ChaiPasswordPolicyException.PASSWORD_ERROR#UNKNOWN_ERROR} if the
     *         error can not be matched.
     */
    public static ChaiPasswordPolicyException forErrorMessage(final String errorString)
    {
        return new ChaiPasswordPolicyException(PASSWORD_ERROR.forErrorString(errorString));
    }

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Create a {@code ChaiPasswordPolicyException} using a {@code PASSWORD_ERROR}.
     *
     * @param passwordError Error that describes the cause of the exception
     */
    public ChaiPasswordPolicyException(final PASSWORD_ERROR passwordError)
    {
        super(passwordError.getErrorKey());
        this.passwordError = passwordError;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * Get the cause of the error
     *
     * @return the cause of the error
     */
    public PASSWORD_ERROR getPasswordError()
    {
        return passwordError;
    }
}

