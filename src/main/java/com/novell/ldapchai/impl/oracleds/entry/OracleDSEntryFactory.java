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

import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;

public class OracleDSEntryFactory implements ChaiFactory.ChaiEntryFactory {

    private static ErrorMap errorMap;

    public InetOrgPerson createChaiUser(final String userDN, final ChaiProvider chaiProvider) {
        return new InetOrgPerson(userDN, chaiProvider);
    }

    public GroupOfUniqueNames createChaiGroup(final String userDN, final ChaiProvider chaiProvider) {
        return new GroupOfUniqueNames(userDN, chaiProvider);
    }

    public OracleDSEntry createChaiEntry(final String userDN, final ChaiProvider chaiProvider) {
        return new OracleDSEntry(userDN, chaiProvider);
    }

    public ChaiProvider.DIRECTORY_VENDOR getDirectoryVendor() {
        return ChaiProvider.DIRECTORY_VENDOR.ORACLE_DS;
    }

    public ErrorMap getErrorMap() {
        if (errorMap == null) {
            errorMap = new OracleDSErrorMap();
        }
        return errorMap;
    }
}
