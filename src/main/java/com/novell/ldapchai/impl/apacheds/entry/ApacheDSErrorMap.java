/*
 * LDAP Chai API
 * Copyright (c) 2006-2017 Novell, Inc.
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

package com.novell.ldapchai.impl.apacheds.entry;

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.util.internal.StringHelper;

class ApacheDSErrorMap implements ErrorMap
{
    private static final ApacheDSErrorMap SINGLETON = new ApacheDSErrorMap();

    private ApacheDSErrorMap()
    {
    }

    public static ApacheDSErrorMap instance()
    {
        return SINGLETON;
    }

    @Override
    public ChaiError errorForMessage( final String message )
    {
        return forMessage( message ).getChaiError();
    }

    @Override
    public boolean isPermanent( final String message )
    {
        return !forMessage( message ).isTempoary();
    }

    @Override
    public boolean isAuthenticationRelated( final String message )
    {
        return forMessage( message ).isAuthentication();
    }

    private static ApacheDSError forMessage( final String message )
    {
        if ( StringHelper.isEmpty( message ) )
        {
            return ApacheDSError.UNKNOWN;
        }
        for ( final ApacheDSError apacheDSError : ApacheDSError.values() )
        {
            if ( message.contains( apacheDSError.name() ) )
            {
                return apacheDSError;
            }
        }

        return ApacheDSError.UNKNOWN;
    }

    /**
     * Inspired by {@code org.apache.directory.api.ldap.model.message.ResultCodeEnum}.
     */
    enum ApacheDSError
    {
        SUCCESS( 0, ChaiError.UNKNOWN ),
        PARTIAL_RESULTS( 9, ChaiError.UNKNOWN ),
        COMPARE_FALSE( 5, ChaiError.UNKNOWN ),
        COMPARE_TRUE( 6, ChaiError.UNKNOWN ),
        REFERRAL( 10, ChaiError.UNKNOWN ),
        SASL_BIND_IN_PROGRESS( 14, ChaiError.UNKNOWN, Flag.Authentication ),
        AUTH_METHOD_NOT_SUPPORTED( 7, ChaiError.UNKNOWN, Flag.Authentication ),
        STRONG_AUTH_REQUIRED( 8, ChaiError.UNKNOWN ),
        CONFIDENTIALITY_REQUIRED( 13, ChaiError.UNKNOWN ),
        ALIAS_DEREFERENCING_PROBLEM( 36, ChaiError.UNKNOWN ),
        INAPPROPRIATE_AUTHENTICATION( 48, ChaiError.FAILED_AUTHENTICATION, Flag.Authentication ),
        INVALID_CREDENTIALS( 49, ChaiError.FAILED_AUTHENTICATION ),
        INSUFFICIENT_ACCESS_RIGHTS( 50, ChaiError.NO_ACCESS ),
        OPERATIONS_ERROR( 1, ChaiError.UNKNOWN ),
        PROTOCOL_ERROR( 2, ChaiError.UNKNOWN ),
        TIME_LIMIT_EXCEEDED( 3, ChaiError.UNKNOWN, Flag.Temporary ),
        SIZE_LIMIT_EXCEEDED( 4, ChaiError.UNKNOWN ),
        ADMIN_LIMIT_EXCEEDED( 11, ChaiError.UNKNOWN ),
        UNAVAILABLE_CRITICAL_EXTENSION( 12, ChaiError.UNKNOWN ),
        BUSY( 51, ChaiError.UNKNOWN, Flag.Temporary ),
        UNAVAILABLE( 52, ChaiError.UNKNOWN, Flag.Temporary ),
        UNWILLING_TO_PERFORM( 53, ChaiError.UNKNOWN ),
        LOOP_DETECT( 54, ChaiError.UNKNOWN ),
        NO_SUCH_ATTRIBUTE( 16, ChaiError.NO_SUCH_ATTRIBUTE ),
        UNDEFINED_ATTRIBUTE_TYPE( 17, ChaiError.NO_SUCH_ATTRIBUTE ),
        INAPPROPRIATE_MATCHING( 18, ChaiError.UNKNOWN ),
        CONSTRAINT_VIOLATION( 19, ChaiError.UNKNOWN ),
        ATTRIBUTE_OR_VALUE_EXISTS( 20, ChaiError.UNKNOWN ),
        INVALID_ATTRIBUTE_SYNTAX( 21, ChaiError.UNKNOWN ),
        NO_SUCH_OBJECT( 32, ChaiError.NO_SUCH_ENTRY ),
        ALIAS_PROBLEM( 33, ChaiError.UNKNOWN ),
        INVALID_DN_SYNTAX( 34, ChaiError.UNKNOWN ),
        NAMING_VIOLATION( 64, ChaiError.UNKNOWN ),
        OBJECT_CLASS_VIOLATION( 65, ChaiError.UNKNOWN ),
        NOT_ALLOWED_ON_NON_LEAF( 66, ChaiError.UNKNOWN ),
        NOT_ALLOWED_ON_RDN( 67, ChaiError.UNKNOWN ),
        ENTRY_ALREADY_EXISTS( 68, ChaiError.UNKNOWN ),
        OBJECT_CLASS_MODS_PROHIBITED( 69, ChaiError.UNKNOWN ),
        AFFECTS_MULTIPLE_DSAS( 71, ChaiError.UNKNOWN ),
        OTHER( 80, ChaiError.UNKNOWN ),
        CANCELED( 118, ChaiError.UNKNOWN, Flag.Temporary ),
        NO_SUCH_OPERATION( 119, ChaiError.UNKNOWN ),
        TOO_LATE( 120, ChaiError.UNKNOWN ),
        CANNOT_CANCEL( 121, ChaiError.UNKNOWN ),
        E_SYNC_REFRESH_REQUIRED( 4096, ChaiError.UNKNOWN ),
        UNKNOWN( 122, ChaiError.UNKNOWN ),;

        private final int apacheDsErrorNumber;
        private final ChaiError chaiError;

        private final boolean tempoary;
        private final boolean authentication;

        private enum Flag
        {
            Temporary,
            Authentication,
        }

        ApacheDSError( final int apacheDsErrorNumber, final ChaiError chaiError, final Flag... flags )
        {
            this.apacheDsErrorNumber = apacheDsErrorNumber;
            this.chaiError = chaiError;
            this.tempoary = StringHelper.enumArrayContainsValue( flags, Flag.Temporary );
            this.authentication = StringHelper.enumArrayContainsValue( flags, Flag.Authentication );
        }

        public int getApacheDsErrorNumber()
        {
            return apacheDsErrorNumber;
        }

        public ChaiError getChaiError()
        {
            return chaiError;
        }

        public boolean isTempoary()
        {
            return tempoary;
        }

        public boolean isAuthentication()
        {
            return authentication;
        }
    }
}
