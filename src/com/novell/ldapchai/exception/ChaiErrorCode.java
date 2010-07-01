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
public enum ChaiErrorCode {
    UNKNOWN                     (-1),
    CHAI_INTERNAL_ERROR         (1),
    COMMUNICATION               (2),
    NO_SUCH_ENTRY               (3),
    NO_SUCH_VALUE               (4),
    NO_SUCH_ATTRIBUTE           (5),
    NO_SUCH_OBJECT              (6),
    FAILED_AUTHENTICATION       (7),
    BAD_PASSWORD                (8),
    PASSWORD_TOO_SHORT          (9),
    PASSWORD_EXPIRED            (10),
    UNSUPPORTED_OPERATION       (11),
    NO_ACCESS                   (12),
    READ_ONLY_VIOLATION(13),

    DUPLICATE_PASSWORD          (-215), //@todo this should be removed and placed elsewhere...
    ;

// ------------------------------ FIELDS ------------------------------

    private int errorCode;

// -------------------------- STATIC METHODS --------------------------


    // --------------------------- CONSTRUCTORS ---------------------------

    private ChaiErrorCode(final int errorCode)
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
