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

package com.novell.ldapchai.impl.oracleds.entry;

import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderImplementor;
import com.novell.ldapchai.provider.DirectoryVendor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OracleDSVendorFactory implements VendorFactory
{
    private static final String ROOT_DSE_ATTRIBUTE_VENDOR_VERSION = "vendorVersion";

    @Override
    public InetOrgPerson newChaiUser( final String userDN, final ChaiProvider chaiProvider )
    {
        return new InetOrgPerson( userDN, chaiProvider );
    }

    @Override
    public GroupOfUniqueNames newChaiGroup( final String userDN, final ChaiProvider chaiProvider )
    {
        return new GroupOfUniqueNames( userDN, chaiProvider );
    }

    @Override
    public OracleDSEntry newChaiEntry( final String userDN, final ChaiProvider chaiProvider )
    {
        return new OracleDSEntry( userDN, chaiProvider );
    }

    @Override
    public DirectoryVendor getDirectoryVendor()
    {
        return DirectoryVendor.ORACLE_DS;
    }

    @Override
    public ErrorMap getErrorMap()
    {
        return OracleDSErrorMap.errorMap();
    }

    @Override
    public Set<String> interestedDseAttributes()
    {
        return Collections.singleton( ROOT_DSE_ATTRIBUTE_VENDOR_VERSION );
    }

    @Override
    public boolean detectVendorFromRootDSEData( final Map<String, List<String>> rootDseAttributeValues )
    {
        if ( rootDseAttributeValues != null && rootDseAttributeValues.containsKey( ROOT_DSE_ATTRIBUTE_VENDOR_VERSION ) )
        {
            for ( final String vendorVersionValue : rootDseAttributeValues.get( ROOT_DSE_ATTRIBUTE_VENDOR_VERSION ) )
            {
                if ( vendorVersionValue.contains( "Sun-Directory-Server" ) || vendorVersionValue.contains( "Oracle-Directory-Server" ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Instant stringToInstant( final String input )
    {
        return OracleDSEntries.convertZuluToDate( input );
    }

    @Override
    public String instantToString( final Instant input )
    {
        return OracleDSEntries.convertDateToZulu( input );
    }

    @Override
    public boolean allowWatchdogDisconnect( final ChaiProviderImplementor chaiProvider )
    {
        return true;
    }
}
