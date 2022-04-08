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

package com.novell.ldapchai.impl.freeipa.entry;

import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;

import java.time.Instant;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collections;

class FreeIPAUser extends AbstractChaiUser implements ChaiUser
{
    private static final String ATTR_ACCOUNT_LOCK = "nsAccountLock";
    private static final String ATTR_PASSWORD_EXPIRATION_TIME = "krbPasswordExpiration";
    private static final String ATTR_LAST_PASSWORD_CHANGE_TIME = "krbLastPwdChange";
    private static final String ATTR_LAST_FAILED_AUTH_TIME = "krbLastFailedAuth";
    private static final String ATTR_LAST_ADMIN_UNLOCK_TIME = "krbLastAdminUnlock";
    private static final String ATTR_LOGIN_FAILED_COUNT = "krbLoginFailedCount";

    FreeIPAUser( final String userDN, final ChaiProvider chaiProvider )
    {
        super( userDN, chaiProvider );
    }

    @Override
    public Set<ChaiGroup> getGroups()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<ChaiGroup> returnGroups = new LinkedHashSet<>();
        final Set<String> groups = this.readMultiStringAttribute( ChaiConstant.ATTR_LDAP_MEMBER_OF );
        for ( final String group : groups )
        {
            returnGroups.add( getChaiProvider().getEntryFactory().newChaiGroup( group ) );
        }
        return Collections.unmodifiableSet( returnGroups );
    }

    @Override
    public void addGroupMembership( final ChaiGroup theGroup )
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.addAttribute( ChaiConstant.ATTR_LDAP_MEMBER_OF, theGroup.getEntryDN() );
        theGroup.addAttribute( ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN() );
    }

    @Override
    public void removeGroupMembership( final ChaiGroup theGroup )
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.deleteAttribute( ChaiConstant.ATTR_LDAP_MEMBER_OF, theGroup.getEntryDN() );
        theGroup.deleteAttribute( ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN() );
    }

    @Override
    public Instant readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return readDateAttribute( ATTR_PASSWORD_EXPIRATION_TIME );
    }

    @Override
    public boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Instant expireDate = readPasswordExpirationDate();

        return expireDate == null ? false : expireDate.isBefore( Instant.now() );

    }

    @Override
    public Instant readPasswordModificationDate()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return readDateAttribute( ATTR_LAST_PASSWORD_CHANGE_TIME );
    }

    @Override
    public boolean isAccountEnabled()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return !this.isLocked();
    }

    @Override
    public boolean isLocked()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return readBooleanAttribute( ATTR_ACCOUNT_LOCK );
    }

    @Override
    public String readGUID()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return FreeIPAEntry.readGUIDImpl( this.getChaiProvider(), this.getEntryDN() );
    }
}
