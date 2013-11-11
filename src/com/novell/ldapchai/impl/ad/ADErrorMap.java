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

package com.novell.ldapchai.impl.ad;

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;

public class ADErrorMap implements ErrorMap {

    public ChaiProvider.DIRECTORY_VENDOR forDirectoryVendor() {
        return ChaiProvider.DIRECTORY_VENDOR.MICROSOFT_ACTIVE_DIRECTORY;
    }

    public ChaiError errorForMessage(final String message)
    {
        return forMessage(message).chaiErrorCode;
    }

    public boolean isPermanent(final String message) {
        return forMessage(message).isPermanent();
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

                final String[] additionalStrings = error.getErrorStrings();
                if (additionalStrings == null || additionalStrings.length == 0) {
                    return error;
                }

                boolean matchesAll = true;
                for (final String additionalString : additionalStrings) {
                    if (!message.contains(additionalString)) {
                        matchesAll = false;
                    }
                }

                if (matchesAll) {
                    return error;
                }

            }

        }

        return ADError.UNKNOWN;
    }

    enum ADError {
        NO_SUCH_OBJECT          ("0x20",                ChaiError.NO_SUCH_ENTRY,                true, false),
        NO_SUCH_ATTRIBUTE       ("0x10",                ChaiError.NO_SUCH_ATTRIBUTE,            true, false),
        INTRUDER_LOCKOUT        ("80090308",            ChaiError.INTRUDER_LOCKOUT,             true, true, "data 775"),
        NEW_PASSWORD_REQUIRED   ("80090308",            ChaiError.NEW_PASSWORD_REQUIRED,        true, true, "data 773"),
        FAILED_AUTHENTICATION   ("80090308",            ChaiError.FAILED_AUTHENTICATION,        true, true, "data 52e"),
        USER_NOT_FOUND          ("80090308",            ChaiError.FAILED_AUTHENTICATION,        true, true, "data 525"),

        ACCOUNT_RESTRICTION     ("80090308",            ChaiError.FAILED_AUTHENTICATION,        true, true, "data 52f"),
        INVALID_LOGON_HOURS     ("80090308",            ChaiError.FAILED_AUTHENTICATION,        true, true, "data 530"),
        INVALID_WORKSTATION     ("80090308",            ChaiError.FAILED_AUTHENTICATION,        true, true, "data 531"),

        PASSWORD_EXPIRED        ("80090308",            ChaiError.PASSWORD_EXPIRED,             true, true, "data 532"),
        ACCOUNT_DISABLED        ("80090308",            ChaiError.ACCOUNT_DISABLED,             true, true, "data 533"),
        ACCOUNT_EXPIRED         ("80090308",            ChaiError.ACCOUNT_EXPIRED,              true, true, "data 701"),
        BAD_PASSWORD            ("error code 19",       ChaiError.PASSWORD_BADPASSWORD,         true, true),

        UNKNOWN                 ("-999",                ChaiError.UNKNOWN,                      true, false),
        ;


        private String errorCodeString;
        private ChaiError chaiErrorCode;
        private boolean permanent;
        private boolean authentication;
        private String[] errorStrings;

        ADError(
                final String errorCodeString,
                final ChaiError chaiErrorCode,
                final boolean permanent,
                final boolean authentication,
                final String... errorStrings
        )
        {
            this.errorCodeString = errorCodeString;
            this.chaiErrorCode = chaiErrorCode;
            this.permanent = permanent;
            this.authentication = authentication;
            this.errorStrings = errorStrings;
        }

        public String getErrorCodeString()
        {
            return errorCodeString;
        }

        public boolean isPermanent()
        {
            return permanent;
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