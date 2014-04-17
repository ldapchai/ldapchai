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

package com.novell.ldapchai.impl.oracleds.entry;

import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.*;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;

import java.util.Date;

public class InetOrgPerson extends AbstractChaiUser implements ChaiUser {
    public InetOrgPerson(String entryDN, ChaiProvider chaiProvider) {
        super(entryDN, chaiProvider);
    }

    @Override
    public Date readLastLoginTime()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readDateAttribute("pwdLastAuthTime");
    }


    @Override
    public Date readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return this.readDateAttribute("passwordExpirationTime");
    }

    @Override
    public boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Date passwordExpiration = this.readPasswordExpirationDate();
        return passwordExpiration != null
                && new Date().after(passwordExpiration)
                || readBooleanAttribute("pwdReset");
    }

    public void setPassword(final String newPassword, final boolean enforcePasswordPolicy)
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        try {
            writeStringAttribute(ATTR_PASSWORD, newPassword);
        } catch (ChaiOperationException e) {
            throw ChaiPasswordPolicyException.forErrorMessage(e.getMessage());
        }
    }

    public void changePassword(final String oldPassword, final String newPassword)
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        try {
            replaceAttribute(ATTR_PASSWORD, oldPassword, newPassword);
        } catch (ChaiOperationException e) {
            final ChaiError error = ChaiErrors.getErrorForMessage(e.getMessage());
            throw new ChaiPasswordPolicyException(e.getMessage(), error);
        }
    }


    @Override
    public Date readPasswordModificationDate()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readDateAttribute("pwdChangedTime");
    }

    @Override
    public Date readDateAttribute(final String attributeName)
            throws ChaiUnavailableException, ChaiOperationException
    {
        final String value = readStringAttribute(attributeName);
        return value != null ? OracleDSEntries.convertZuluToDate(value) : null;
    }

    @Override
    public void writeDateAttribute(final String attributeName, final Date date)
            throws ChaiUnavailableException, ChaiOperationException
    {
        if (date == null) {
            return;
        }

        final String dateString = OracleDSEntries.convertDateToZulu(date);
        writeStringAttribute(attributeName, dateString);
    }

    @Override
    public ChaiPasswordPolicy getPasswordPolicy()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return OracleDSEntries.readPasswordPolicy(this);
    }
}
