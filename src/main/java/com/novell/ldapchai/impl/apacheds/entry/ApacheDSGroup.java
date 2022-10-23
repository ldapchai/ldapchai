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

package com.novell.ldapchai.impl.apacheds.entry;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiGroup;
import com.novell.ldapchai.provider.ChaiProvider;

class ApacheDSGroup extends AbstractChaiGroup implements ChaiGroup
{
    ApacheDSGroup( final String groupDN, final ChaiProvider chaiProvider )
    {
        super( groupDN, chaiProvider );
    }

    @Override
    public void addMember( final ChaiUser theUser )
            throws ChaiUnavailableException, ChaiOperationException
    {
        this.addAttribute( ChaiConstant.ATTR_LDAP_MEMBER, theUser.getEntryDN() );
        theUser.addAttribute( ChaiConstant.ATTR_LDAP_MEMBER_OF, this.getEntryDN() );
    }

    @Override
    public void removeMember( final ChaiUser theUser )
            throws ChaiUnavailableException, ChaiOperationException
    {
        this.deleteAttribute( ChaiConstant.ATTR_LDAP_MEMBER, theUser.getEntryDN() );
        theUser.deleteAttribute( ChaiConstant.ATTR_LDAP_MEMBER_OF, this.getEntryDN() );
    }

    @Override
    public String readGUID()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return ApacheDSEntry.readGUIDImpl( this.getChaiProvider(), this.getEntryDN() );
    }
}
