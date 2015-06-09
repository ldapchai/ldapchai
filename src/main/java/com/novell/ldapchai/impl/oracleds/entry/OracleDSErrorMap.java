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

package com.novell.ldapchai.impl.oracleds.entry;

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;

public class OracleDSErrorMap implements ErrorMap {
    public ChaiProvider.DIRECTORY_VENDOR forDirectoryVendor()
    {
        return ChaiProvider.DIRECTORY_VENDOR.ORACLE_DS;
    }

    public ChaiError errorForMessage(String message)

    {
        return forMessage(message).getChaiErrorCode();
    }

    public boolean isPermanent(final String message) {
        return forMessage(message).isPermenant();
    }

    public boolean isAuthenticationRelated(final String message) {
        return forMessage(message).isAuthentication();
    }

    private static OracleDSError forMessage(final String message) {
        if (message == null || message.length() < 1) {
            return OracleDSError.UNKNOWN;
        }

        for (final OracleDSError error : OracleDSError.values()) {
            for (final String errorString : error.getErrorStrings()) {
                if (message.contains(String.valueOf(errorString))) {
                    return error;
                }
            }
        }

        return OracleDSError.UNKNOWN;
    }

    enum OracleDSError {
        NO_SUCH_ENTRY           (ChaiError.NO_SUCH_ENTRY,true, false, "error code 32"),
        USER_INTRUDER_LOCK      (ChaiError.INTRUDER_LOCKOUT, true, false, "error code 19 - Exceed password retry limit. Account locked"),
        PASSWORD_TOO_EARLY      (ChaiError.PASSWORD_TOO_SOON, true, false, "error code 19 - within password minimum age"),
        INVALID_CREDENTIALS     (ChaiError.FAILED_AUTHENTICATION, true, false, "error code 49 - Invalid Credentials"),
        PASSWORD_IN_HISTORY     (ChaiError.PASSWORD_PREVIOUSLYUSED, true, false, "error code 19 - password in history"),
        PASSWORD_REQ_CHANGES    (ChaiError.NEW_PASSWORD_REQUIRED, true, false, "error code 53 - Password was reset and must be changed"),

        UNKNOWN                 (ChaiError.UNKNOWN, true, false),
        ;

        private ChaiError chaiErrorCode;
        private boolean permenant;
        private boolean authentication;
        private String[] errorStrings;

        OracleDSError(
                final ChaiError chaiErrorCode,
                final boolean permenant,
                final boolean authentication,
                final String... errorStrings
        )
        {
            this.chaiErrorCode = chaiErrorCode;
            this.permenant = permenant;
            this.authentication = authentication;
            this.errorStrings = errorStrings;
        }

        public boolean isPermenant()
        {
            return permenant;
        }

        public boolean isAuthentication()
        {
            return authentication;
        }

        public ChaiError getChaiErrorCode()
        {
            return chaiErrorCode;
        }

        public String[] getErrorStrings()
        {
            return errorStrings;
        }
    }
}
