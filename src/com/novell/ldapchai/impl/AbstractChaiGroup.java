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

package com.novell.ldapchai.impl;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
* A complete implementation of {@code ChaiGroup} interface.
* <p/>
* Clients looking to obtain a {@code ChaiGroup} instance should look to {@link com.novell.ldapchai.ChaiFactory}.
* <p/>
 * @author Jason D. Rivard
*/
public abstract class AbstractChaiGroup extends AbstractChaiEntry implements ChaiGroup {
    
    /**
     * This construtor is used to instantiate an ChaiUserImpl instance representing an inetOrgPerson user object in ldap.
     *
     * @param groupDN       The DN of the user
     * @param chaiProvider Helper to connect to LDAP.
     */
    public AbstractChaiGroup(final String groupDN, final ChaiProvider chaiProvider)
    {
        super(groupDN, chaiProvider);
    }

    public Set<ChaiUser> getMembers()
            throws ChaiOperationException, ChaiUnavailableException
    {
        // Search for the User DN object.
        final Set<String> memberDNs = this.readMultiStringAttribute(ChaiConstant.ATTR_LDAP_MEMBER);

        // Search for the User DN object.
        final Set<ChaiUser> returnSet = new HashSet<ChaiUser>(memberDNs.size());

        for (final String userDN : memberDNs) {
            // Create the ChaiUserImpl object and add it to the ArrayList.
            returnSet.add(ChaiFactory.createChaiUser(userDN, this.getChaiProvider()));
        }
        return Collections.unmodifiableSet(returnSet);
    }

    public String readGroupName()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute(ChaiConstant.ATTR_LDAP_DESCRIPTION);
    }

    public void addMember(final ChaiUser theUser) throws ChaiUnavailableException, ChaiOperationException {
        this.addAttribute(ChaiConstant.ATTR_LDAP_MEMBER, theUser.getEntryDN());
        theUser.addAttribute(ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, this.getEntryDN());
    }

    public void removeMember(final ChaiUser theUser) throws ChaiUnavailableException, ChaiOperationException {
        this.deleteAttribute(ChaiConstant.ATTR_LDAP_MEMBER, theUser.getEntryDN());
        theUser.deleteAttribute(ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, this.getEntryDN());
    }
}
