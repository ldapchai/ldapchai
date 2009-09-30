/*
 * LDAP Chai API
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009 Jason D. Rivard
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

package com.novell.ldapchai.impl.edir.entry;

import com.novell.ldapchai.AbstractChaiGroup;
import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * This class performs various actions on the LDAP Group object.
 */
class GroupOfNamesImpl extends AbstractChaiGroup implements GroupOfNames {

    static {
        EdirEntryFactory.registerImplementation(GroupOfNames.OBJECT_CLASS_VALUE, GroupOfNamesImpl.class);
    }

    public String getLdapObjectClassName()
    {
        return GroupOfNames.OBJECT_CLASS_VALUE;
    }

    // --------------------------- CONSTRUCTORS ---------------------------

    GroupOfNamesImpl(final String entryDN, final ChaiProvider chaiHelper)
    {
        super(entryDN, chaiHelper);
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ChaiGroup ---------------------


    public final boolean isDynamic()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return this.compareStringAttribute(ChaiConstant.ATTR_LDAP_OBJECTCLASS, ChaiConstant.OBJECTCLASS_AUX_LDAP_DYNAMIC_GROUP);
    }


    public final Date readDateAttribute(final String attributeName)
            throws ChaiUnavailableException, ChaiOperationException
    {
        final String lastLoginTimeStr = this.readStringAttribute(attributeName);
        if (lastLoginTimeStr != null) {
            return EdirEntries.convertZuluToDate(lastLoginTimeStr);
        }
        return null;
    }

    public void addMember(final ChaiUser theUser)
            throws ChaiUnavailableException, ChaiOperationException
    {
        EdirEntries.writeGroupMembership(theUser, this);
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

    /**
     * This function returns name of the Group
     *
     * @return String name of the group.
     * @throws com.novell.ldapchai.exception.ChaiOperationException
     */
    public final String readGroupName()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute(ChaiConstant.ATTR_LDAP_DESCRIPTION);
    }

    public void removeMember(final ChaiUser theUser)
            throws ChaiUnavailableException, ChaiOperationException
    {
        EdirEntries.removeGroupMembership(theUser, this);
    }


    
}