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

package com.novell.ldapchai.exception;

import com.novell.ldapchai.provider.DirectoryVendor;


/**
 * Static methods useful for error handling.
 */
public final class ChaiErrors
{
    private ChaiErrors()
    {
    }

    public static ChaiError getErrorForMessage ( final String message )
    {
        for ( final DirectoryVendor vendor : DirectoryVendor.values() )
        {
            final ErrorMap errorMap = vendor.getVendorFactory().getErrorMap();
            final ChaiError errorCode = errorMap.errorForMessage( message );
            if ( errorCode != null && errorCode != ChaiError.UNKNOWN )
            {
                return errorCode;
            }
        }

        return ChaiError.UNKNOWN;
    }

    /**
     * Indicates if the error is related to authentication.
     *
     * @param message error message debug text
     * @return true if the error is defined as being related to authentication.
     */
    static boolean isAuthenticationRelated ( final String message )
    {
        for ( final DirectoryVendor vendor : DirectoryVendor.values() )
        {
            final ErrorMap errorMap = vendor.getVendorFactory().getErrorMap();
            if ( errorMap.isAuthenticationRelated( message ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>Indicates if the error is deemed permanent or temporary.</p>
     *
     * <p>Permanent errors are those that, if retried, the error would most likely
     * occur indefinitely.  This would generally include any error that if repeated against
     * a different server, the error returned would be the same.</p>
     *
     * <p>Fail-over mechanisms use this indication to decide if an operation that
     * generated an error should be retried.</p>
     *
     * @param message error message debug text
     * @return true if the error is defined as permanent
     */
    static boolean isPermanent ( final String message )
    {
        for ( final DirectoryVendor vendor : DirectoryVendor.values() )
        {
            final ErrorMap errorMap = vendor.getVendorFactory().getErrorMap();
            if ( !errorMap.isPermanent( message ) )
            {
                return false;
            }
        }

        return true;
    }
}
