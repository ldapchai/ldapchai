/*
 * LDAP Chai API
 * Copyright (c) 2006-2017 Novell, Inc.
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

package com.novell.ldapchai.util.internal;

import com.novell.ldapchai.ChaiConstant;

import javax.swing.JOptionPane;
import java.util.Map;

/**
 * Main class to display the chai version info and about when the ldapChai.jar
 * is executed.
 */
class MainHandler
{
    private static final String CHAI_VERSION = ChaiConstant.CHAI_API_VERSION;
    private static final String CHAI_WEBSITE = ChaiConstant.CHAI_API_WEBSITE;

    public static void main( final String[] args )
    {
        System.out.println( buildInfoString() );

        JOptionPane.showMessageDialog
                ( null,
                        buildInfoString(),
                        "About",
                        JOptionPane.INFORMATION_MESSAGE );
    }

    private static String buildInfoString()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append( "LDAP Chai API v" ).append( CHAI_VERSION ).append( " library\n" );
        sb.append( "\n" );
        sb.append( "Build Information: \n" );

        for ( final Map.Entry<String, String> entry : ChaiConstant.BUILD_MANIFEST.entrySet() )
        {
            sb.append( "    " ).append( entry.getKey() ).append( "=" ).append( entry.getValue() );
            sb.append( "\n" );
        }

        sb.append( "\n" );
        sb.append( "LDAP Chai project page: " ).append( CHAI_WEBSITE ).append( "\n" );
        sb.append( "\n" );
        sb.append( "source files are included inside jar archive" );

        return sb.toString();
    }
}
