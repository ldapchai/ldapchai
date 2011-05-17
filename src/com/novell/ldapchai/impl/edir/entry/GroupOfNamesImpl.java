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

package com.novell.ldapchai.impl.edir.entry;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiGroup;
import com.novell.ldapchai.provider.ChaiProvider;


/**
 * This class performs various actions on the LDAP Group object.
 */
class GroupOfNamesImpl extends AbstractChaiGroup implements GroupOfNames {

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

    public void addMember(final ChaiUser theUser)
            throws ChaiUnavailableException, ChaiOperationException
    {
        EdirEntries.writeGroupMembership(theUser, this);
    }

    public void removeMember(final ChaiUser theUser)
            throws ChaiUnavailableException, ChaiOperationException
    {
        EdirEntries.removeGroupMembership(theUser, this);
    }

    @Override
    public String readGUID()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return EdirEntries.readGuid(this);
    }
}