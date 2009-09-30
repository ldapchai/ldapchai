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

package com.novell.ldapchai.impl.edir.entry;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.provider.ChaiProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating instances of all edir entry objects in  
 */
public class EdirEntryFactory {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    static private final Map<String, Class> implementingClasses = new HashMap<String, Class>();

// -------------------------- STATIC METHODS --------------------------

    static void registerImplementation(final String forObjectClassName, final Class classImpl) {
        implementingClasses.put(forObjectClassName, classImpl);
    }

    //@todo finish implementing and test
    /*
    public static Top createEntryForObjectClass(final ChaiProvider chaiProvider, final String... objectClass) {
        final Set<String> objectClasses = new HashSet<String>(Arrays.asList(objectClass));
        Class returnClass;
        for (final String key : implementingClasses.keySet()) {
            if (objectClasses.contains(key)) {
                returnClass = implementingClasses.get(key);
            }
        }

        return null;
    }
    */

    public static InetOrgPerson createInetOrgPerson(final String userDN, final ChaiProvider chaiProvider) {
        return new InetOrgPersonImpl(userDN, chaiProvider);
    }

    public static GroupOfNames createGroupOfNames(final String userDN, final ChaiProvider chaiProvider) {
        return new GroupOfNamesImpl(userDN, chaiProvider);
    }

    public static ChaiEntry createEntry(final String userDN, final ChaiProvider chaiProvider) {
        return new ChaiEntryImpl(userDN, chaiProvider);
    }
}
