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

package com.novell.ldapchai.impl.openldap.entry;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;

import javax.naming.NamingException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class OpenLDAPUser extends AbstractChaiUser implements ChaiUser
{

    public OpenLDAPUser( final String userDN, final ChaiProvider chaiProvider )
    {
        super( userDN, chaiProvider );
    }

    @Override
    public Set<ChaiGroup> getGroups()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<ChaiGroup> returnGroups = new HashSet<>();
        final Set<String> groups = this.readMultiStringAttribute( ChaiConstant.ATTR_LDAP_MEMBER_OF );
        for ( final String group : groups )
        {
            returnGroups.add( chaiProvider.getEntryFactory().newChaiGroup( group ) );
        }
        return Collections.unmodifiableSet( returnGroups );
    }

    @Override
    public void addGroupMembership( final ChaiGroup theGroup )
            throws ChaiOperationException, ChaiUnavailableException
    {
        theGroup.addAttribute( ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN() );
    }

    @Override
    public void removeGroupMembership( final ChaiGroup theGroup )
            throws ChaiOperationException, ChaiUnavailableException
    {
        theGroup.deleteAttribute( ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN() );
    }

    @Override
    public Instant readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Instant passwordChangedTime = this.readDateAttribute( ChaiConstant.ATTR_OPENLDAP_PASSWORD_CHANGED_TIME );
        if ( passwordChangedTime != null && this.getPasswordPolicy() != null )
        {
            final String expirationInterval = this.getPasswordPolicy().getValue( ChaiPasswordRule.ExpirationInterval );
            if ( expirationInterval != null && expirationInterval.trim().length() > 0 )
            {
                final long expInt = Long.parseLong( expirationInterval ) * 1000L;
                final long pwExpireTimeMs = passwordChangedTime.toEpochMilli() + expInt;
                return Instant.ofEpochMilli( pwExpireTimeMs );
            }
        }

        return null;
    }

    @Override
    public boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Instant passwordExpiration = this.readPasswordExpirationDate();
        return passwordExpiration != null
                && Instant.now().isAfter( passwordExpiration )
                || readBooleanAttribute( ChaiConstant.ATTR_OPENLDAP_PASSWORD_RESET );
    }

    @Override
    public void setPassword( final String newPassword, final boolean enforcePasswordPolicy )
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        try
        {
            getChaiProvider().extendedOperation( new OpenLDAPModifyPasswordRequest( this.getEntryDN(), newPassword, getChaiProvider().getChaiConfiguration() ) );
        }
        catch ( NamingException | ChaiOperationException e )
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
            getChaiProvider().extendedOperation( new OpenLDAPModifyPasswordRequest( this.getEntryDN(), newPassword, getChaiProvider().getChaiConfiguration() ) );
        }
        catch ( NamingException | ChaiOperationException e )
        {
            throw ChaiPasswordPolicyException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void unlockPassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.deleteAttribute( ChaiConstant.ATTR_OPENLDAP_PASSWORD_ACCOUNT_LOCKED_TIME, null );
    }

    @Override
    public boolean isPasswordLocked()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Instant passwordAccountLockedTime = this.readDateAttribute( ChaiConstant.ATTR_OPENLDAP_PASSWORD_ACCOUNT_LOCKED_TIME );
        return passwordAccountLockedTime != null
                && Instant.now().isAfter( passwordAccountLockedTime );
    }

    @Override
    public Instant readPasswordModificationDate()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readDateAttribute( ChaiConstant.ATTR_OPENLDAP_PASSWORD_CHANGED_TIME );
    }

    @Override
    public ChaiPasswordPolicy getPasswordPolicy()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return OpenLDAPEntries.readPasswordPolicy( this );
    }

    @Override
    public void expirePassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.writeStringAttribute( ChaiConstant.ATTR_OPENLDAP_PASSWORD_RESET, "TRUE" );
    }
}
