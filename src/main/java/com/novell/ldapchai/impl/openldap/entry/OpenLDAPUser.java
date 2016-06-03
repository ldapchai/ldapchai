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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;

public class OpenLDAPUser extends AbstractChaiUser implements ChaiUser
{

    public OpenLDAPUser(String userDN, ChaiProvider chaiProvider) {
        super(userDN, chaiProvider);
    }

    public Set<ChaiGroup> getGroups()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<ChaiGroup> returnGroups = new HashSet<ChaiGroup>();
        final Set<String> groups = this.readMultiStringAttribute(ChaiConstant.ATTR_LDAP_MEMBER_OF);
        for (final String group : groups) {
            returnGroups.add(ChaiFactory.createChaiGroup(group, this.getChaiProvider()));
        }
        return Collections.unmodifiableSet(returnGroups);
    }

    public void addGroupMembership(ChaiGroup theGroup) throws ChaiOperationException, ChaiUnavailableException {
        theGroup.addAttribute(ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN());
    }

    public void removeGroupMembership(ChaiGroup theGroup) throws ChaiOperationException, ChaiUnavailableException {
        theGroup.deleteAttribute(ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN());
    }

    @Override
    public Date readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Date passwordChangedTime = this.readDateAttribute(ChaiConstant.ATTR_OPENLDAP_PASSWORD_CHANGED_TIME);
        if (passwordChangedTime != null && this.getPasswordPolicy() != null) {
            final String expirationInterval = this.getPasswordPolicy().getValue(ChaiPasswordRule.ExpirationInterval);
            if (expirationInterval != null && expirationInterval.trim().length() > 0) {
                final long expInt = Long.parseLong(expirationInterval) * 1000L;
                final long pwExpireTimeMs = passwordChangedTime.getTime() + expInt;
                return new Date(pwExpireTimeMs);
            }
        }

        return null;
    }

    @Override
    public boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Date passwordExpiration = this.readPasswordExpirationDate();
        return passwordExpiration != null
                && new Date().after(passwordExpiration)
                || readBooleanAttribute(ChaiConstant.ATTR_OPENLDAP_PASSWORD_RESET);
    }

    @Override
    public void setPassword(final String newPassword, final boolean enforcePasswordPolicy)
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

    @Override
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
    
    @Override
    public void unlockPassword() throws ChaiOperationException, ChaiUnavailableException {
        this.deleteAttribute(ChaiConstant.ATTR_OPENLDAP_PASSWORD_ACCOUNT_LOCKED_TIME, null);
    }

    @Override
    public boolean isPasswordLocked() throws ChaiOperationException, ChaiUnavailableException {
        final Date passwordAccountLockedTime = this.readDateAttribute(ChaiConstant.ATTR_OPENLDAP_PASSWORD_ACCOUNT_LOCKED_TIME);
        return passwordAccountLockedTime != null
                && new Date().after(passwordAccountLockedTime);
    }

    @Override
    public Date readPasswordModificationDate()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readDateAttribute(ChaiConstant.ATTR_OPENLDAP_PASSWORD_CHANGED_TIME);
    }

    @Override
    public Date readDateAttribute(final String attributeName)
            throws ChaiUnavailableException, ChaiOperationException
    {
        final String value = readStringAttribute(attributeName);
        return value != null ? OpenLDAPEntries.convertZuluToDate(value) : null;
    }

    @Override
    public void writeDateAttribute(final String attributeName, final Date date)
            throws ChaiUnavailableException, ChaiOperationException
    {
        if (date == null) {
            return;
        }

        final String dateString = OpenLDAPEntries.convertDateToZulu(date);
        writeStringAttribute(attributeName, dateString);
    }

    @Override
    public ChaiPasswordPolicy getPasswordPolicy()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return OpenLDAPEntries.readPasswordPolicy(this);
    }

    @Override
    public void expirePassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.writeStringAttribute(ChaiConstant.ATTR_OPENLDAP_PASSWORD_RESET, "TRUE");
    }
}
