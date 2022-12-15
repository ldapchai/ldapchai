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

package com.novell.ldapchai.impl.directoryServer389.entry;

import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;

import java.time.Instant;

class DirectoryServer389User extends AbstractChaiUser implements ChaiUser
{
    public static final String ATTR_PASSWORD_RETRY_COUNT = "passwordRetryCount";
    public static final String ATTR_ACCOUNT_UNLOCK_TIME = "accountUnlockTime";

    DirectoryServer389User( final String userDN, final ChaiProvider chaiProvider )
    {
        super( userDN, chaiProvider );
    }

    @Override
    public void setPassword( final String newPassword, final boolean enforcePasswordPolicy )
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        try
        {
            writeStringAttribute( ATTR_PASSWORD, newPassword );
        }
        catch ( ChaiOperationException e )
        {
            throw ChaiPasswordPolicyException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void changePassword( final String oldPassword, final String newPassword )
            throws
            ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        try
        {
            writeStringAttribute( ATTR_PASSWORD, newPassword );
        }
        catch ( ChaiOperationException e )
        {
            throw ChaiPasswordPolicyException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public Instant readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return readDateAttribute( ATTR_PASSWORD_EXPIRE_TIME );
    }

    @Override
    public boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Instant expireDate = readPasswordExpirationDate();
        return expireDate != null && expireDate.isBefore( Instant.now() );
    }

    @Override
    public void unlockPassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.writeStringAttribute( ATTR_PASSWORD_RETRY_COUNT, "0" );

        // Only attempt to remove the attribute if it already
        // exists to avoid exceptions trying to remove a
        // non-existent attribute
        final Instant unlockDate = readDateAttribute( ATTR_ACCOUNT_UNLOCK_TIME );
        if ( unlockDate != null )
        {
            this.deleteAttribute( ATTR_ACCOUNT_UNLOCK_TIME, null );
        }
    }

    @Override
    public boolean isPasswordLocked()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Instant unlockDate = readDateAttribute( ATTR_ACCOUNT_UNLOCK_TIME );
        if ( unlockDate == null )
        {
            return false;
        }

        return unlockDate.isAfter( Instant.now() );
    }

    @Override
    public void expirePassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.writeStringAttribute( ATTR_PASSWORD_EXPIRE_TIME, "19800101010101Z" );
    }

    @Override
    public String readGUID()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return DirectoryServer389Entry.readGUIDImpl( this.getChaiProvider(), this.getEntryDN() );
    }
}
