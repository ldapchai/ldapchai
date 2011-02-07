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
 * The parent of all LDAP Chai API exceptions.  {@code ChaiException} will never be
 * thrown, however it is useful to catch if you do not need to differentiate between
 * ChaiOperationException and ChaiUnavailableException.
 *
 * @author Jason D. Rivard
 * @see ChaiError
 */
public class ChaiException extends Exception {
// ------------------------------ FIELDS ------------------------------

    private boolean permenant = true;
    private boolean authentication = false;

    private ChaiError errorCode;

// -------------------------- STATIC METHODS --------------------------

    static ChaiException createChaiException(final String message)
    {
        final ChaiError detectedCode = ChaiErrors.getErrorForMessage(message);

        if (ChaiErrors.isPermanent(message)) {
            return new ChaiUnavailableException(message, detectedCode);
        } else {
            return new ChaiOperationException(message, detectedCode);
        }
    }

// --------------------------- CONSTRUCTORS ---------------------------

    protected ChaiException(final String message, final ChaiError errorCode)
    {
        this(message, errorCode, ChaiErrors.isPermanent(message), ChaiErrors.isAuthenticationRelated(message));
    }

    public ChaiException(final String message, final ChaiError errorCode, final boolean permenant, final boolean authentication)
    {
        super(message);
        this.permenant = permenant;
        this.authentication = authentication;
        this.errorCode = errorCode;
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    public ChaiError getErrorCode()
    {
        return errorCode;
    }

    public boolean isPermenant()
    {
        return permenant;
    }

    public boolean isAuthentication()
    {
        return authentication;
    }
}
