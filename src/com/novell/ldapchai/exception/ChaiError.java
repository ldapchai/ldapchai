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

/**
 * An enumeration of all the known LDAP and Chai ErrorCodes
 *
 * @author Jason D. Rivard
 */
package com.novell.ldapchai.exception;

/**
 * An enumerarion of error codes known to the LDAP Chai API.  When a {@link ChaiException} is thrown,
 * it will be issued with one of the values of this enumeration.
 *
 * @author Jason D. Rivard
 */
public enum ChaiError {
    UNKNOWN                     ("Unknown",                     -1),
    CHAI_INTERNAL_ERROR         ("Chai_InternalError",          1),
    COMMUNICATION               ("Communication_Error",         2),
    NO_SUCH_ENTRY               ("NoSuchEntry",                 3),
    NO_SUCH_VALUE               ("NoSuchValue",                 4),
    NO_SUCH_ATTRIBUTE           ("NoSuchAttribute",             5),
    FAILED_AUTHENTICATION       ("FailedAuthentication",        7),
    INTRUDER_LOCKOUT            ("IntruderLockout",             8),
    NEW_PASSWORD_REQUIRED       ("NewPasswordRequired",         9),
    PASSWORD_EXPIRED            ("PasswordExpired",             10),
    UNSUPPORTED_OPERATION       ("UnsupportedOperation",        11),
    NO_ACCESS                   ("NoAccess",                    12),
    READ_ONLY_VIOLATION         ("ReadOnlyViolation",           13),
    ACCOUNT_DISABLED            ("AccountDisabled",             14),
    ACCOUNT_EXPIRED             ("AccountExpired",              15),


    PASSWORD_PREVIOUSLYUSED     ("Password_PreviouslyUsed",     500),
    PASSWORD_BADOLDPASSWORD     ("Password_BadOldPassword",     501),
    PASSWORD_BADPASSWORD        ("Password_BadPassword",        502),
    PASSWORD_TOO_SHORT          ("Password_TooShort",           503),
    PASSWORD_TOO_LONG           ("Password_TooLong",            504),
    PASSWORD_NOT_ENOUGH_NUM     ("Password_NotEnoughNum",       505),
    PASSWORD_NOT_ENOUGH_SPECIAL ("Password_NotEnoughSpecial",   506),
    PASSWORD_NOT_ENOUGH_ALPHA   ("Password_NotEnoughAlpha",     507),
    PASSWORD_NOT_ENOUGH_LOWER   ("Password_NotEnoughLower",     508),
    PASSWORD_NOT_ENOUGH_UPPER   ("Password_NotEnoughUpper",     509),
    PASSWORD_NOT_ENOUGH_UNIQUE  ("Password_NotEnoughUnique",    509),
    PASSWORD_TOO_MANY_REPEAT    ("Password_TooManyRepeat",      510),
    PASSWORD_TOO_MANY_NUMERIC   ("Password_TooManyNumeric",     511),
    PASSWORD_TOO_MANY_ALPHA     ("Password_TooManyAlpha",       512),
    PASSWORD_TOO_MANY_LOWER     ("Password_TooManyLower",       513),
    PASSWORD_TOO_MANY_UPPER     ("Password_TooManyUpper",       514),
    PASSWORD_FIRST_IS_NUMERIC   ("Password_FirstIsNumeric",     515),
    PASSWORD_LAST_IS_NUMERIC    ("Password_LastIsNumeric",      516),
    PASSWORD_FIRST_IS_SPECIAL   ("Password_FirstIsSpecial",     517),
    PASSWORD_LAST_IS_SPECIAL    ("Password_LastIsSpecial",      518),
    PASSWORD_TOO_MANY_SPECIAL   ("Password_TooManySpecial",     519),
    PASSWORD_INVALID_CHAR       ("Password_InvalidChar",        520),
    PASSWORD_INWORDLIST         ("Password_InWordlist",         521),
    PASSWORD_SAMEASATTR         ("Password_SameAsAttr",         522),
    PASSWORD_HISTORY_FULL       ("Password_HistoryFull",        523),
    PASSWORD_NUMERIC_DISALLOWED ("Password_NumericDisallowed",  524),
    PASSWORD_SPECIAL_DISALLOWED ("Password_SpecialDisallowed",  525),
    PASSWORD_TOO_SOON           ("Password_TooSoon",            526),

    CR_NOT_ENOUGH_RANDOM_RESPONSES     ("NotEnoughRandom",             600),
    CR_TOO_FEW_RANDOM_RESPONSES        ("TooFewRandom",                601),
    CR_MISSING_REQUIRED_CHALLENGE_TEXT ("MissingRequiredChallenge",    602),
    CR_MISSING_REQUIRED_RESPONSE_TEXT  ("MissingRequiredResponse",     603),
    CR_RESPONSE_TOO_SHORT              ("ResponseTooShort",            604),
    CR_RESPONSE_TOO_LONG               ("ResponseTooLong",             605),
    CR_TOO_FEW_CHALLENGES              ("TooFewChallenges",            606),
    CR_DUPLICATE_RESPONSES             ("DuplicateResponses",          607),


    ;

// ------------------------------ FIELDS ------------------------------

    private final int errorCode;
    private String errorKey;

// -------------------------- STATIC METHODS --------------------------


    // --------------------------- CONSTRUCTORS ---------------------------

    private ChaiError(final String errorKey, final int errorCode)
    {
        this.errorCode = errorCode;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * Returns the underlying error code's numeric value.
     *
     * @return numeric version of the error code.
     */
    public int getErrorCode()
    {
        return errorCode;
    }
}
