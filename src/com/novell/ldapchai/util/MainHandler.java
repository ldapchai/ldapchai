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

package com.novell.ldapchai.util;

import javax.swing.*;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

/**
 * Main class to display the chai version info and about when the ldapChai.jar
 * is executed.
 */
class MainHandler {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ResourceBundle buildInfoBundle = ResourceBundle.getBundle("com.novell.ldapchai.BuildInformation");
    private static final String CHAI_VERSION = buildInfoBundle.getString("chai.version");
    private static final String CHAI_WEBSITE = buildInfoBundle.getString("chai.website");

// --------------------------- main() method ---------------------------

    public static void main(final String[] args)
    {
        System.out.println(buildInfoString());

        JOptionPane.showMessageDialog
                (null,
                        buildInfoString(),
                        "About",
                        JOptionPane.INFORMATION_MESSAGE);
    }

    private static String buildInfoString()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append("LDAP Chai API v").append(CHAI_VERSION).append(" library\n");
        sb.append("\n");
        sb.append("Build Information: \n");

        final Set<String> keySet = new TreeSet<String>();
        for (Enumeration<String> keyEnum = buildInfoBundle.getKeys(); keyEnum.hasMoreElements();) {
            keySet.add(keyEnum.nextElement());
        }

        for (final String key : keySet) {
            final String property = buildInfoBundle.getString(key);
            sb.append("    ").append(key).append("=").append(property);
            sb.append("\n");
        }

        sb.append("\n");
        sb.append("LDAP Chai project page: " + CHAI_WEBSITE + "\n");
        sb.append("\n");
        sb.append("source files are included inside jar archive");

        return sb.toString();
    }
}
