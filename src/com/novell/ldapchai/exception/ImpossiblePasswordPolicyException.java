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

/**
 * Indicates that the policy is not possible to fulfill.  For example, if the
 * policy's minimum length is larger then the maximum length
 * <p/>
 * The contents of the message will include a brief english discription of the
 * error, suitable for debugging.
 *
 * @author Jason D. Rivard
 */
public class ImpossiblePasswordPolicyException extends RuntimeException {
// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    /**
     * Enumeration of problems with a password policy
     */
    public enum ErrorEnum {
        MAX_LENGTH_GREATER_THEN_MINIMUM_LENGTH,
        MAX_LOWER_GREATER_THEN_MIN_LOWER,
        MAX_UPPER_GREATER_THEN_MIN_UPPER,
        MAX_NUMERIC_GREATER_THAN_MIN_NUMERIC,
        MAX_SPECIAL_GREATER_THAN_MIN_SPECIAL,
        MIN_LOWER_GREATER_THAN_MAX_LENGTH,
        MIN_NUMERIC_GREATER_THAN_MAX_LENGTH,
        MIN_SPECIAL_GREATER_THAN_MAX_LENGTH,
        MIN_UNIQUE_GREATER_THAN_MAX_LENGTH,
        MIN_UPPER_GREATER_THAN_MAX_LENGTH,
        REQUIRED_CHAR_NOT_ALLOWED,
        PASSWORD_TOO_COMPLEX_TO_GENERATE,
        UNEXPECTED_ERROR
    }

// ------------------------------ FIELDS ------------------------------

    private ErrorEnum error;

// --------------------------- CONSTRUCTORS ---------------------------

    public ImpossiblePasswordPolicyException(final ErrorEnum error)
    {
        super(error.toString());
        this.error = error;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public ErrorEnum getError()
    {
        return error;
    }
}
