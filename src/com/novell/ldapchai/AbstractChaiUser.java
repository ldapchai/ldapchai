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

import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiSetting;

import java.util.*;

/**
* A complete implementation of {@code ChaiUser} interface.
* <p/>
* Clients looking to obtain a {@code ChaiUser} instance should look to {@link ChaiFactory}.
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


    /*
    public final PasswordPolicy getPasswordPolicy()
            throws ChaiUnavailableException, ChaiOperationException
    {

        return ChaiUtility.readPasswordPolicy(this);
    }
    */


    public Properties readStandardIdentityAttributes()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String[] standardIdentityAttrs = this.getChaiProvider().getChaiConfiguration().getSetting(ChaiSetting.STANDARD_IDENTITY_ATTRS).split(",");
        return this.readStringAttributes(new HashSet<String>(Arrays.asList(standardIdentityAttrs)));
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

}