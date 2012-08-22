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

import com.novell.ldapchai.*;
import com.novell.ldapchai.exception.ChaiErrors;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * A complete implementation of {@code ChaiUser} interface.
 * <p/>
 * Clients looking to obtain a {@code ChaiUser} instance should look to {@link com.novell.ldapchai.ChaiFactory}.
 * <p/>
 * @author Jason D. Rivard
 */

public abstract class AbstractChaiUser extends AbstractChaiEntry implements ChaiUser {
    /**
     * This construtor is used to instantiate an ChaiUserImpl instance representing an inetOrgPerson user object in ldap.
     *
     * @param userDN       The DN of the user
     * @param chaiProvider Helper to connect to LDAP.
     */
    public AbstractChaiUser(final String userDN, final ChaiProvider chaiProvider)
    {
        super(userDN, chaiProvider);
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ChaiUser ---------------------


    public final ChaiUser getAssistant()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String mgrDN = this.readStringAttribute(ATTR_MANAGER);
        if (mgrDN == null) {
            return null;
        }
        return ChaiFactory.createChaiUser(mgrDN, this.getChaiProvider());
    }

    public final Set<ChaiUser> getDirectReports()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<String> reportDNs = this.readMultiStringAttribute(ATTR_MANAGER);
        final Set<ChaiUser> reports = new HashSet<ChaiUser>(reportDNs.size());
        for (final String reporteeDN : reportDNs) {
            reports.add(ChaiFactory.createChaiUser(reporteeDN, this.getChaiProvider()));
        }
        return Collections.unmodifiableSet(reports);
    }

    public final Set<ChaiGroup> getGroups()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<ChaiGroup> returnGroups = new HashSet<ChaiGroup>();
        final Set<String> groups = this.readMultiStringAttribute(ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP);
        for (final String group : groups) {
            returnGroups.add(ChaiFactory.createChaiGroup(group, this.getChaiProvider()));
        }
        return Collections.unmodifiableSet(returnGroups);
    }

    public final ChaiUser getManager()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String mgrDN = this.readStringAttribute(ATTR_MANAGER);
        if (mgrDN == null) {
            return null;
        }
        return ChaiFactory.createChaiUser(mgrDN, this.getChaiProvider());
    }


    public boolean testPassword(final String password)
            throws ChaiUnavailableException, ChaiPasswordPolicyException
    {
        try {
            return this.compareStringAttribute(ATTR_PASSWORD, password);
        } catch (ChaiOperationException e) {
            throw new ChaiPasswordPolicyException(e.getMessage(), ChaiErrors.getErrorForMessage(e.getMessage()));
        }
    }

    public final String readSurname()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute(ATTR_SURNAME);
    }

    public String readUsername()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute(ATTR_COMMON_NAME);
    }

    public void addGroupMembership(final ChaiGroup theGroup) throws ChaiOperationException, ChaiUnavailableException {
        this.addAttribute(ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, theGroup.getEntryDN());
        theGroup.addAttribute(ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN());
    }

    public void removeGroupMembership(final ChaiGroup theGroup) throws ChaiOperationException, ChaiUnavailableException {
        this.deleteAttribute(ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, theGroup.getEntryDN());
        theGroup.deleteAttribute(ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN());
    }

    public String readGivenName()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute(ATTR_GIVEN_NAME);
    }

    public void setPassword(final String newPassword)
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
            writeStringAttribute(ATTR_PASSWORD, newPassword);
        } catch (ChaiOperationException e) {
            throw ChaiPasswordPolicyException.forErrorMessage(e.getMessage());
        }
    }

    public void expirePassword() throws ChaiOperationException, ChaiUnavailableException {
    }

    public ChaiPasswordPolicy getPasswordPolicy() throws ChaiUnavailableException, ChaiOperationException {
        return null;
    }

    public boolean isPasswordExpired() throws ChaiUnavailableException, ChaiOperationException {
        return false;
    }

    public Date readLastLoginTime() throws ChaiOperationException, ChaiUnavailableException {
        return null;
    }

    public String readPassword() throws ChaiUnavailableException, ChaiOperationException {
        return null;
    }

    public Date readPasswordExpirationDate() throws ChaiUnavailableException, ChaiOperationException {
        return null;
    }

    public boolean testPasswordPolicy(final String testPassword) throws ChaiUnavailableException, ChaiPasswordPolicyException {
        return true;
    }

    public void unlock() throws ChaiOperationException, ChaiUnavailableException {
    }

    public boolean isLocked() throws ChaiOperationException, ChaiUnavailableException {
        return false;
    }

    public Date readPasswordModificationDate() throws ChaiOperationException, ChaiUnavailableException {
        return null;
    }

    public boolean isAccountEnabled() throws ChaiOperationException, ChaiUnavailableException {
        return !readBooleanAttribute(ATTR_LOGIN_DISABLED);
    }
}
