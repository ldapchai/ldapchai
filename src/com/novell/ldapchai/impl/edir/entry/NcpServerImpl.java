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

package com.novell.ldapchai.impl.edir.entry;

import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

class NcpServerImpl extends TopImpl implements NcpServer {

    public String getLdapObjectClassName()
    {
        return NcpServer.OBJECT_CLASS_VALUE;
    }

    // --------------------------- CONSTRUCTORS ---------------------------

    public NcpServerImpl(final String entryDN, final ChaiProvider chaiProvider)
    {
        super(entryDN, chaiProvider);
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ChaiNcpServer ---------------------

    public Set<URI> getLdapAddresses()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Set<String> strAddresses = this.getNetworkAddressAttrValue();
        final Set<URI> addresses = new HashSet<URI>();

        for (final String str : strAddresses) {
            if (str.toLowerCase().startsWith("ldap")) {
                try {
                    final URI uri = new URI(str);
                    addresses.add(uri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        return addresses;
    }

// -------------------------- OTHER METHODS --------------------------

    private Set<String> getNetworkAddressAttrValue()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final byte[][] addies = this.readMultiByteAttribute("networkAddress");

        final Set<String> strings = new HashSet<String>();

        for (final byte[] addy : addies) {
            final StringBuilder sb = new StringBuilder();
            int i = 0;
            for (final byte b : addy) {
                i++;
                if (i % 2 == 0) {
                    sb.append((char) b);
                }
            }
            if (sb.length() > 0) {
                sb.delete(0, 1);
            }
            if (sb.length() > 0) {
                sb.delete(sb.length() - 1, sb.length());
            }
            strings.add(sb.toString());
        }

        return strings;
    }
}