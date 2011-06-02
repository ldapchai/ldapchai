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

import com.novell.ldapchai.exception.ChaiException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * LDAP Chai API
 * Example Code
 * ----
 *
 * Create a new user in the ldap directory.
 */
public class CreateUser {
// --------------------------- main() method ---------------------------

    public static void main(final String[] args) {
        String ldapURL =      "ldap://ldaphost:389";
        String ldapBindDN =   "cn=admin,ou=ou,o=o";
        String ldapBindPW =   "novell";

        // create a provider using the standard JNDI factory.
        ChaiProvider provider = null;
        try {
            provider = ChaiProviderFactory.createProvider(ldapURL,ldapBindDN,ldapBindPW);
        } catch (ChaiUnavailableException e) {
            System.out.println("LDAP error while connecting: " + e);
            System.exit(-1);
        }

        // setup string values to use for the creation
        String createDN = "cn=gwashington,ou=ou,o=o";
        String createClass = "inetOrgPerson";

        // create a Properties to set the initial attribute values for the new user.
        Map<String,String> createAttributes = new HashMap<String, String>();
        createAttributes.put("givenName","George");
        createAttributes.put("sn","Washingon");
        createAttributes.put("title","President");
        createAttributes.put("mail","president@whitehouse.gov");

        try {
            // perform the create operation
            provider.createEntry(createDN, createClass, createAttributes);
            System.out.println("created user " + createDN);
        } catch (ChaiException e) {
            System.out.println("error creating user: " + e.getMessage());
        }
    }
}