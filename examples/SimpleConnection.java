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

import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiException;

/**
 * LDAP Chai API
 * Example Code
 * ----
 *
 * Make a simple connection using one of the quick methods for generating a ChaiProvider.
 */
public class SimpleConnection {
// --------------------------- main() method ---------------------------

    public static void main(final String[] args) throws ChaiException {
        // create a provider using the quick chai factory
        final ChaiUser user = ChaiFactory.quickProvider("ldap://ldaphost:389","cn=admin,ou=ou,o=o","novell");

        // read the value of the bindDN's cn attribute, and print it to stdout.
        final String cnValue = user.readStringAttribute("cn");

        //output the CN of the user
        System.out.println("cnValue = " + cnValue);
    }
}


