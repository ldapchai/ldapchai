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

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.impl.edir.EdirErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.DirectoryVendor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Factory for creating instances of all Novell eDirectory objects when {@link com.novell.ldapchai.provider.ChaiProvider#getDirectoryVendor()}
 * returns {@link DirectoryVendor#EDIRECTORY}.
 * }
 * In most cases, this factory should not
 * be used directly.  Instead, use {@link ChaiFactory}.
 */
public class EDirectoryVendorFactory implements VendorFactory
{
    private static final String ROOT_DSE_ATTRIBUTE_VENDOR_VERSION = "vendorVersion";

    private static final ErrorMap ERROR_MAP = new EdirErrorMap();

    public InetOrgPerson createChaiUser( final String userDN, final ChaiProvider chaiProvider )
    {
        return new InetOrgPersonImpl( userDN, chaiProvider );
    }

    public GroupOfNames createChaiGroup( final String userDN, final ChaiProvider chaiProvider )
    {
        return new GroupOfNamesImpl( userDN, chaiProvider );
    }

    public ChaiEntry createChaiEntry( final String userDN, final ChaiProvider chaiProvider )
    {
        return new ChaiEntryImpl( userDN, chaiProvider );
    }

    public DirectoryVendor getDirectoryVendor()
    {
        return DirectoryVendor.EDIRECTORY;
    }

    public ErrorMap getErrorMap()
    {
        return ERROR_MAP;
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
                if ( vendorVersionValue.contains( "eDirectory" ) )
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
        return EdirEntries.convertZuluToInstant( input );
    }

    @Override
    public String instantToString( final Instant input )
    {
        return EdirEntries.convertInstantToZulu( input );
    }
}
