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

package com.novell.ldapchai.impl.openldap;

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ErrorMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Maps from OpenLDAP error codes to an appropriate exception, where possible.
 * Replaces previous implementation, {@link com.novell.ldapchai.impl.edir.EdirErrorMap}.
 *
 * @author Hayden Sartoris
 * @see <a href="https://www.openldap.org/doc/admin24/appendix-ldap-result-codes.html">LDAP Result Codes</a>
 * @since 0.8.1
 */
public class OpenLDAPErrorMap implements ErrorMap
{
    private static final OpenLDAPErrorMap SINGLETON = new OpenLDAPErrorMap();

    private OpenLDAPErrorMap()
    {
    }

    public static OpenLDAPErrorMap instance()
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
        return forMessage( message ).isPermanent();
    }

    @Override
    public boolean isAuthenticationRelated( final String message )
    {
        return forMessage( message ).isAuthRelated();
    }

    private static OpenLDAPError forMessage( final String message )
    {
        if ( message == null || message.length() < 1 )
        {
            return OpenLDAPError.OTHER;
        }

        for ( final OpenLDAPError error : OpenLDAPError.values() )
        {
            if ( message.contains( error.getErrorCodeString() ) )
            {
                return error;
            }

            for ( final String errorString : error.getErrorStrings() )
            {
                if ( message.contains( errorString ) )
                {
                    return error;
                }
            }
        }
        
        return OpenLDAPError.OTHER;
    }

    enum OpenLDAPError
    {
        // commented out lines are included for comprehensive coverage, but are
        // not considered errors.
        //SUCCESS( 0),
        OPERATIONS_ERROR( 1 ),
        PROTOCOL_ERROR( 2 ),
        /**
         * It's possible that a request exceeding a time limit might succeed on
         * retry, given lower overall server load.
         */
        TIME_LIMIT_EXCEEDED( 3, ChaiError.UNKNOWN, false, false ),
        SIZE_LIMIT_EXCEEDED( 4 ),
        //COMPARE_FALSE( 5 ),
        //COMPARE_TRUE( 6 ),
        AUTH_METHOD_NOT_SUPPORTED( 7, ChaiError.UNSUPPORTED_OPERATION, true, true ),
        STRONGER_AUTH_REQUIRED( 8 ),
        //REFERRAL( 10 ),
        ADMIN_LIMIT_EXCEEDED( 11 ),
        UNAVAILABLE_CRITICAL_EXTENSION( 12 ),
        CONFIDENTIALITY_REQUIRED( 13 ),
        //SASL_BIND_IN_PROGRESS( 14 ),
        NO_SUCH_ATTRIBUTE( 16, ChaiError.NO_SUCH_ATTRIBUTE ),
        UNDEFINED_ATTRIBUTE_TYPE( 17, ChaiError.NO_SUCH_ATTRIBUTE ),
        INAPPROPRIATE_MATCHING( 18 ),
        CONSTRAINT_VIOLATION( 19 ),
        ATTRIBUTE_OR_VALUE_EXISTS( 20 ),
        INVALID_ATTRIBUTE_SYNTAX( 21 ),
        NO_SUCH_OBJECT( 32, ChaiError.NO_SUCH_ENTRY ),
        ALIAS_PROBLEM( 33 ),
        INVALID_DN_SYNTAX( 34 ),
        ALIAS_DEREFERENCING_PROBLEM( 36 ),
        INAPPROPRIATE_AUTHENTICATION( 48, ChaiError.FAILED_AUTHENTICATION, true, true ),
        INVALID_CREDENTIALS( 49, ChaiError.FAILED_AUTHENTICATION, true, true ),
        /**
         * Special case of error 50, in which the user has been allowed to bind
         * but is then disallowed from performing even a search until their
         * password has been changed.
         */
        NEW_PASSWORD_REQUIRED( 50, ChaiError.NEW_PASSWORD_REQUIRED, true, true,
                "Operations are restricted to bind/unbind/abandon/StartTLS/modify password" ),
        INSUFFICIENT_ACCESS_RIGHTS( 50, ChaiError.NO_ACCESS ),
        BUSY( 51 ),
        UNAVAILABLE( 52 ),
        UNWILLING_TO_PERFORM( 53 ),
        LOOP_DETECT( 54 ),
        NAMING_VIOLATION( 64 ),
        OBJECT_CLASS_VIOLATION( 65 ),
        NOT_ALLOWED_ON_NON_LEAF( 66 ),
        NOT_ALLOWED_ON_RDN( 67 ),
        ENTRY_ALREAD_EXISTS( 68 ),
        OBJECT_CLASS_MODS_PROHIBITED( 69 ),
        AFFECTS_MULTIPLE_DSAS( 71 ),
        OTHER( 80 );

        private final int errorCode;
        private final String errorCodeString;
        private final ChaiError chaiError;
        private final boolean isPermanent;
        private final boolean isAuthRelated;
        private final List<String> errorStrings;

        /**
         * Constructor for error without good mapping to a {@link ChaiError}.
         *
         * @param errorCode the numeric code given in the error message
         */
        OpenLDAPError( final int errorCode )
        {
            this( errorCode, ChaiError.UNKNOWN );
        }

        /**
         * Constructor for error for which good mapping to {@link ChaiError}
         * exists.
         *
         * @param errorCode the numeric code given in the error message
         * @param chaiError the equivalent ChaiError
         */
        OpenLDAPError( final int errorCode, final ChaiError chaiError )
        {
            this( errorCode, chaiError, true, false );
        }

        /**
         * Constructor specifying all aspects of error.
         *
         * @param errorCode the numeric code given in the error message
         * @param chaiError the equivalent ChaiError
         * @param isPermanent false if same operation might succeed on retry
         * @param isAuthRelated error is due to improper authentication
         * @param errorStrings optional message strings to fall back on
         */
        OpenLDAPError( 
                final int errorCode,
                final ChaiError chaiError,
                final boolean isPermanent,
                final boolean isAuthRelated,
                final String... errorStrings
        )
        {
            this.errorCode = errorCode;
            this.errorCodeString = String.valueOf( errorCode );
            this.chaiError = chaiError;
            this.isPermanent = isPermanent;
            this.isAuthRelated = isAuthRelated;
            this.errorStrings = Collections.unmodifiableList(
                    Arrays.asList( errorStrings ) );
        }

        public int getErrorCode()
        {
            return errorCode;
        }

        public String getErrorCodeString()
        {
            return errorCodeString;
        }

        public ChaiError getChaiError()
        {
            return chaiError;
        }

        public boolean isPermanent()
        {
            return isPermanent;
        }

        public boolean isAuthRelated()
        {
            return isAuthRelated;
        }

        public List<String> getErrorStrings()
        {
            return errorStrings;
        }
    }
}
