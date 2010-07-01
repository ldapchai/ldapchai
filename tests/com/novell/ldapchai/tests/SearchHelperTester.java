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

package com.novell.ldapchai.tests;

import com.novell.ldapchai.util.SearchHelper;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.LinkedHashMap;

public class SearchHelperTester extends TestCase {
// -------------------------- OTHER METHODS --------------------------

    public void testAndFilter()
            throws Exception
    {
        final LinkedHashMap<String, String> props = new LinkedHashMap<String, String>();
        props.put("objectClass", "inetOrgPerson");
        props.put("cn", "joe");

        final SearchHelper sh = new SearchHelper();
        sh.setFilterAnd(props);

        final String expectedFilter = "(&(objectClass=inetOrgPerson)(cn=joe))";
        final String filterFromHelper = sh.getFilter();

        Assert.assertEquals(expectedFilter, filterFromHelper);
    }
}
