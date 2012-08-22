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

package com.novell.ldapchai.impl.openldap.entry;

import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;

class OpenLDAPUser extends AbstractChaiUser implements ChaiUser
{

    public OpenLDAPUser(String userDN, ChaiProvider chaiProvider) {
        super(userDN, chaiProvider);
    }

    public void setPassword(final String newPassword)
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        try {
            getChaiProvider().extendedOperation(new OpenLDAPModifyPasswordRequest(this.getEntryDN(), newPassword));
        } catch (javax.naming.NamingException e) {
            throw ChaiPasswordPolicyException.forErrorMessage(e.getMessage());
        } catch (ChaiOperationException e) {
            throw ChaiPasswordPolicyException.forErrorMessage(e.getMessage());
        }
    }

    public void changePassword(final String oldPassword, final String newPassword)
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        try {
            getChaiProvider().extendedOperation(new OpenLDAPModifyPasswordRequest(this.getEntryDN(), newPassword));
        } catch (javax.naming.NamingException e) {
            throw ChaiPasswordPolicyException.forErrorMessage(e.getMessage());
        } catch (ChaiOperationException e) {
            throw ChaiPasswordPolicyException.forErrorMessage(e.getMessage());
        }
    }


}
