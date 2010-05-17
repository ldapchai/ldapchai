/*
 * LDAP Chai API
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2010 The LDAP Chai Project
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

package com.novell.ldapchai.impl.generic.entry;

import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.cr.ChallengeSet;
import com.novell.ldapchai.cr.ResponseSet;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;

import java.util.Date;

class GenericChaiUser extends AbstractChaiUser implements ChaiUser {
    public GenericChaiUser(final String userDN, final ChaiProvider chaiProvider) {
        super(userDN, chaiProvider);
    }
}
