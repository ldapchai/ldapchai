/*
 * LDAP Chai API
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2010 The LDAP Chai Project
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

class ADErrorMap implements ErrorMap {

    public ChaiErrorCode errorForMessage(final String message)
    {
        return forMessage(message).chaiErrorCode;
    }

    public boolean isPermenant(final String message) {
        return forMessage(message).isPermenant();
    }

    public boolean isAuthenticationRelated(final String message) {
        return forMessage(message).isAuthentication();
    }

    private static ADError forMessage(final String message) {
        if (message == null || message.length() < 1) {
            return ADError.UNKNOWN;
        }

        for (final ADError error : ADError.values()) {
            if (message.contains(error.getErrorCodeString())) {
                return error;
            }

            for (final String errorString : error.getErrorStrings()) {
                if (message.contains(String.valueOf(errorString))) {
                    return error;
                }
            }
        }

        return ADError.UNKNOWN;
    }

    enum ADError {
        UNKNOWN                                        ("-999", ChaiErrorCode.UNKNOWN,                       true, false),
        NO_SUCH_OBJECT                                 ("0x20", ChaiErrorCode.NO_SUCH_OBJECT,                true, false),
        NO_SUCH_ATTRIBUTE                              ("0x10", ChaiErrorCode.NO_SUCH_ATTRIBUTE,             true, false),
        FAILED_AUTHENTICATION                          ("80090308", ChaiErrorCode.FAILED_AUTHENTICATION,         true, true),
//      DUPLICATE_PASSWORD                             ("",     ChaiErrorCode.DUPLICATE_PASSWORD,            true, false),
//      PASSWORD_TOO_SHORT                             ("",     ChaiErrorCode.PASSWORD_TOO_SHORT,            true, false),
//      BAD_PASSWORD                                   ("",     ChaiErrorCode.BAD_PASSWORD,                  true, true),
//      PASSWORD_EXPIRED                               ("",     ChaiErrorCode.PASSWORD_EXPIRED,              true, true),
//      NO_SUCH_ENTRY                                  ("",     ChaiErrorCode.NO_SUCH_ENTRY,                 true, false),
//      NO_SUCH_VALUE                                  ("",     ChaiErrorCode.NO_SUCH_VALUE,                 true, false),
//      NO_ACCESS                                      ("",     ChaiErrorCode.NO_ACCESS,                     true, false),

        ;

        private String errorCodeString;
        private ChaiErrorCode chaiErrorCode;
        private boolean permenant;
        private boolean authentication;
        private String[] errorStrings;

        ADError(
                final String errorCodeString,
                final ChaiErrorCode chaiErrorCode,
                final boolean permenant,
                final boolean authentication,
                final String... errorStrings
        )
        {
            this.errorCodeString = errorCodeString;
            this.chaiErrorCode = chaiErrorCode;
            this.permenant = permenant;
            this.authentication = authentication;
            this.errorStrings = errorStrings;
        }

        public String getErrorCodeString()
        {
            return errorCodeString;
        }

        public boolean isPermenant()
        {
            return permenant;
        }

        public boolean isAuthentication()
        {
            return authentication;
        }

        public ChaiErrorCode getChaiErrorCode()
        {
            return chaiErrorCode;
        }

        public String[] getErrorStrings()
        {
            return errorStrings;
        }
    }
}