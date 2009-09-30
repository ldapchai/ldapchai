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

package com.novell.ldapchai;

import com.novell.ldapchai.provider.ChaiProvider;

/**
* A complete implementation of {@code ChaiGroup} interface.
* <p/>
* Clients looking to obtain a {@code ChaiGroup} instance should look to {@link ChaiFactory}.
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
}
