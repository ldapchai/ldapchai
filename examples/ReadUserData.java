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

import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiException;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderFactory;

import java.util.Map;

/**
 * LDAP Chai API
 * Example Code
 * ----
 *
 * Read some user data out of the directory and print to stdout.
 */
public class ReadUserData {

    public static void main(final String[] args) {
        String ldapURL =      "ldap://ldaphost:389";
        String ldapBindDN =   "cn=admin,ou=ou,o=o";
        String ldapBindPW =   "password";

        if (args.length == 3) {
            ldapURL = args[0];
            ldapBindDN = args[1];
            ldapBindPW = args[2];
        }

        try {
            // create provider factory
            final ChaiProviderFactory chaiProviderFactory = ChaiProviderFactory.newProviderFactory();

            // create a provider using the standard JNDI factory.
            ChaiProvider chaiProvider = chaiProviderFactory.newProvider(ldapURL,ldapBindDN,ldapBindPW);

            ChaiUser user = chaiProvider.getEntryFactory().newChaiUser( ldapBindDN );

            // read the value of the bindDN's cn attribute, and print it to stdout.
            Map<String,String> allUserAttributes = user.readStringAttributes(null);

            System.out.println("UserDN: " + user.getEntryDN());

            // Output each of the user's attributes, and one value for each attribute:
            for (String key : allUserAttributes.keySet()) {
                String value = allUserAttributes.get(key);
                System.out.println(key + ": " + value);
            }

            // Detect the user's password and output the debug string
            ChaiPasswordPolicy pwdPolicy = user.getPasswordPolicy();
            System.out.println("PasswordPolicy = " + pwdPolicy);

            System.out.println("PasswordModificationDate = " + user.readPasswordModificationDate());
            System.out.println("PasswordExpirationDate = " + user.readPasswordExpirationDate());
            System.out.println("PasswordExpired = " + user.isPasswordExpired());
            System.out.println("PasswordLocked = " + user.isPasswordLocked());

            // Read the user's group membership, and output each group DN.
            System.out.println(user.getEntryDN() + " groups: ");
            for (ChaiGroup group : user.getGroups()) {
                System.out.println(group.getEntryDN());
            }
            System.out.println("");


        } catch (ChaiException e) {
            System.out.println("LDAP error: " + e.getMessage());
        }
    }
}
