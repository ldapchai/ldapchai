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

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.edir.entry.EdirEntries;

import java.util.Date;

public class OracleDSEntries {
    public static Date convertZuluToDate(final String dateString)
    {
        return EdirEntries.convertZuluToDate(dateString);
    }

    public static String convertDateToZulu(final Date date)
    {
        return EdirEntries.convertDateToZulu(date);
    }

    static OracleDSPasswordPolicy readPasswordPolicy(final InetOrgPerson person)
            throws ChaiUnavailableException, ChaiOperationException
    {
        ChaiEntry searchEntry = new OracleDSEntry(person.getEntryDN(), person.getChaiProvider());
        OracleDSEntry discoveredPolicy = null;
        int saftyCounter = 0;

        while (saftyCounter < 50 && searchEntry != null && discoveredPolicy == null) {
            saftyCounter++;
            if (searchEntry.isValid()) {
                final String pwdPolicySubentryValue = searchEntry.readStringAttribute(
                        ChaiConstant.ATTR_ORACLEDS_PASSWORD_SUB_ENTRY);
                if (pwdPolicySubentryValue != null && !pwdPolicySubentryValue.isEmpty()) {
                    final OracleDSEntry policyEntry = new OracleDSEntry(pwdPolicySubentryValue,
                            person.getChaiProvider());
                    if (policyEntry.isValid()) {
                        discoveredPolicy = policyEntry;
                    }
                }
            }
            searchEntry = searchEntry.getParentEntry();
        }

        if (discoveredPolicy != null) {
            return new OracleDSPasswordPolicy(discoveredPolicy.getEntryDN(), person.getChaiProvider());
        }

        final OracleDSPasswordPolicy defaultPolicy = new OracleDSPasswordPolicy("cn=Password Policy,cn=config", person.getChaiProvider());
        if (defaultPolicy.isValid()) {
            return defaultPolicy;
        }

        return defaultPolicy;
    }
}
