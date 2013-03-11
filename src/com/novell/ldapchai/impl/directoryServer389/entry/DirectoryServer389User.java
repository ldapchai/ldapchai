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

package com.novell.ldapchai.impl.directoryServer389.entry;

import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;

import java.util.Date;

class DirectoryServer389User extends AbstractChaiUser implements ChaiUser {
    public DirectoryServer389User(final String userDN, final ChaiProvider chaiProvider) {
        super(userDN, chaiProvider);
    }

    @Override
    public void setPassword(final String newPassword)
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        try {
            writeStringAttribute(ATTR_PASSWORD, newPassword);
        } catch (ChaiOperationException e) {
            throw ChaiPasswordPolicyException.forErrorMessage(e.getMessage());
        }
    }

    @Override
    public void changePassword(final String oldPassword, final String newPassword) throws
            ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        try {
            writeStringAttribute(ATTR_PASSWORD, newPassword);
        } catch (ChaiOperationException e) {
            throw ChaiPasswordPolicyException.forErrorMessage(e.getMessage());
        }
    }

    @Override
    public Date readPasswordExpirationDate() throws ChaiUnavailableException, ChaiOperationException {
        return readDateAttribute(ATTR_PASSWORD_EXPIRE_TIME);
    }

    @Override
    public boolean isPasswordExpired() throws ChaiUnavailableException, ChaiOperationException {
        final Date expireDate = readPasswordExpirationDate();

        if (expireDate == null) {
            return false;
        }

        return expireDate.before(new Date());
    }

    @Override
    public void unlock() throws ChaiOperationException, ChaiUnavailableException {
        this.deleteAttribute("pwdAccountLockedTime",null);
    }

    @Override
    public boolean isLocked()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return readBooleanAttribute("pwdLockout");
    }
}