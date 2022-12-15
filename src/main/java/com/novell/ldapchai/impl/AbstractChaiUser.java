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

package com.novell.ldapchai.impl;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiErrors;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A complete implementation of {@code ChaiUser} interface.
 * Clients looking to obtain a {@code ChaiUser} instance should look to {@link com.novell.ldapchai.ChaiEntryFactory}.
 *
 * @author Jason D. Rivard
 */

public abstract class AbstractChaiUser extends AbstractChaiEntry implements ChaiUser
{
    /**
     * This constructor is used to instantiate an ChaiUserImpl instance representing an inetOrgPerson user object in ldap.
     *
     * @param userDN       The DN of the user
     * @param chaiProvider Helper to connect to LDAP.
     */
    public AbstractChaiUser( final String userDN, final ChaiProvider chaiProvider )
    {
        super( userDN, chaiProvider );
    }

    @Override
    public final ChaiUser getAssistant()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String mgrDN = this.readStringAttribute( ATTR_MANAGER );
        if ( mgrDN == null )
        {
            return null;
        }
        return getChaiProvider().getEntryFactory().newChaiUser( mgrDN );
    }

    @Override
    public final Set<ChaiUser> getDirectReports()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<String> reportDNs = this.readMultiStringAttribute( ATTR_MANAGER );
        final Set<ChaiUser> reports = new LinkedHashSet<>( reportDNs.size() );
        for ( final String reporteeDN : reportDNs )
        {
            reports.add( getChaiProvider().getEntryFactory().newChaiUser( reporteeDN ) );
        }
        return Collections.unmodifiableSet( reports );
    }

    @Override
    public Set<ChaiGroup> getGroups()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<ChaiGroup> returnGroups = new LinkedHashSet<>();
        final Set<String> groups = this.readMultiStringAttribute( ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP );
        for ( final String group : groups )
        {
            returnGroups.add( getChaiProvider().getEntryFactory().newChaiGroup( group ) );
        }
        return Collections.unmodifiableSet( returnGroups );
    }

    @Override
    public final ChaiUser getManager()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String mgrDN = this.readStringAttribute( ATTR_MANAGER );
        if ( mgrDN == null )
        {
            return null;
        }
        return getChaiProvider().getEntryFactory().newChaiUser( mgrDN );
    }


    @Override
    public boolean testPassword( final String password )
            throws ChaiUnavailableException, ChaiPasswordPolicyException
    {
        try
        {
            return this.compareStringAttribute( ATTR_PASSWORD, password );
        }
        catch ( ChaiOperationException e )
        {
            throw new ChaiPasswordPolicyException( e.getMessage(), ChaiErrors.getErrorForMessage( e.getMessage() ) );
        }
    }

    @Override
    public final String readSurname()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute( ATTR_SURNAME );
    }

    @Override
    public String readUsername()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute( ATTR_COMMON_NAME );
    }

    @Override
    public void addGroupMembership( final ChaiGroup theGroup )
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.addAttribute( ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, theGroup.getEntryDN() );
        theGroup.addAttribute( ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN() );
    }

    @Override
    public void removeGroupMembership( final ChaiGroup theGroup )
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.deleteAttribute( ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, theGroup.getEntryDN() );
        theGroup.deleteAttribute( ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN() );
    }

    @Override
    public String readGivenName()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute( ATTR_GIVEN_NAME );
    }

    @Override
    public void setPassword( final String newPassword )
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        this.setPassword( newPassword, false );
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
            writeStringAttribute( ATTR_PASSWORD, newPassword );
        }
        catch ( ChaiOperationException e )
        {
            throw ChaiPasswordPolicyException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void expirePassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
    }

    @Override
    public ChaiPasswordPolicy getPasswordPolicy()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return null;
    }

    @Override
    public boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return false;
    }

    @Override
    public Instant readLastLoginTime()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return null;
    }

    @Override
    public String readPassword()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return null;
    }

    @Override
    public Instant readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return null;
    }

    @Override
    public boolean testPasswordPolicy( final String testPassword )
            throws ChaiUnavailableException, ChaiPasswordPolicyException
    {
        return true;
    }

    @Override
    public void unlockPassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
    }

    @Override
    public boolean isPasswordLocked()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return false;
    }

    @Override
    public Instant readPasswordModificationDate()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return null;
    }

    @Override
    public boolean isAccountEnabled()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return !readBooleanAttribute( ATTR_LOGIN_DISABLED );
    }

    @Override
    public boolean isLocked()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.isPasswordLocked();
    }

    @Override
    public Instant readAccountExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return null;
    }

    @Override
    public boolean isAccountExpired()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Instant accountExpirationDate = readAccountExpirationDate();
        return accountExpirationDate != null && accountExpirationDate.isBefore( Instant.now() );
    }
}
