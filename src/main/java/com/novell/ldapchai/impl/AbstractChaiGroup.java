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
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A complete implementation of {@code ChaiGroup} interface.
 * Clients looking to obtain a {@code ChaiGroup} instance should look to {@link com.novell.ldapchai.ChaiEntryFactory}.
 *
 * @author Jason D. Rivard
 */
public abstract class AbstractChaiGroup extends AbstractChaiEntry implements ChaiGroup
{

    /**
     * This constructor is used to instantiate an ChaiUserImpl instance representing an inetOrgPerson user object in ldap.
     *
     * @param groupDN      The DN of the user
     * @param chaiProvider Helper to connect to LDAP.
     */
    public AbstractChaiGroup( final String groupDN, final ChaiProvider chaiProvider )
    {
        super( groupDN, chaiProvider );
    }

    @Override
    public Set<ChaiUser> getMembers()
            throws ChaiOperationException, ChaiUnavailableException
    {
        // Search for the User DN object.
        final Set<String> memberDNs = this.readMultiStringAttribute( ChaiConstant.ATTR_LDAP_MEMBER );

        // Search for the User DN object.
        final Set<ChaiUser> returnSet = new LinkedHashSet<>( memberDNs.size() );

        for ( final String userDN : memberDNs )
        {
            // Create the ChaiUserImpl object and add it to the ArrayList.
            returnSet.add( getChaiProvider().getEntryFactory().newChaiUser( userDN ) );
        }
        return Collections.unmodifiableSet( returnSet );
    }

    @Override
    public String readGroupName()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute( ChaiConstant.ATTR_LDAP_DESCRIPTION );
    }

    @Override
    public void addMember( final ChaiUser theUser )
            throws ChaiUnavailableException, ChaiOperationException
    {
        this.addAttribute( ChaiConstant.ATTR_LDAP_MEMBER, theUser.getEntryDN() );
        theUser.addAttribute( ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, this.getEntryDN() );
    }

    @Override
    public void removeMember( final ChaiUser theUser )
            throws ChaiUnavailableException, ChaiOperationException
    {
        this.deleteAttribute( ChaiConstant.ATTR_LDAP_MEMBER, theUser.getEntryDN() );
        theUser.deleteAttribute( ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, this.getEntryDN() );
    }
}
