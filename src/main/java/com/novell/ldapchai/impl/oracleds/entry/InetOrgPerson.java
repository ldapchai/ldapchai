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

package com.novell.ldapchai.impl.oracleds.entry;

import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiErrors;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;

import java.time.Instant;

public class InetOrgPerson extends AbstractChaiUser implements ChaiUser
{
    public InetOrgPerson( final String entryDN, final ChaiProvider chaiProvider )
    {
        super( entryDN, chaiProvider );
    }

    @Override
    public Instant readLastLoginTime()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readDateAttribute( "pwdLastAuthTime" );
    }


    @Override
    public Instant readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return this.readDateAttribute( "passwordExpirationTime" );
    }

    @Override
    public boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Instant passwordExpiration = this.readPasswordExpirationDate();
        return passwordExpiration != null
                && Instant.now().isAfter( passwordExpiration )
                || readBooleanAttribute( "pwdReset" );
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
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        try
        {
            replaceAttribute( ATTR_PASSWORD, oldPassword, newPassword );
        }
        catch ( ChaiOperationException e )
        {
            final ChaiError error = ChaiErrors.getErrorForMessage( e.getMessage() );
            throw new ChaiPasswordPolicyException( e.getMessage(), error );
        }
    }

    @Override
    public Instant readPasswordModificationDate()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readDateAttribute( "pwdChangedTime" );
    }

    @Override
    public ChaiPasswordPolicy getPasswordPolicy()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return OracleDSEntries.readPasswordPolicy( this );
    }

    @Override
    public void expirePassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.writeStringAttribute( "passwordExpirationTime", "19800101010101Z" );
    }
}
