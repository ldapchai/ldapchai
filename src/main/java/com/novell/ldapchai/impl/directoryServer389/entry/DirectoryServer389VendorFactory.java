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

package com.novell.ldapchai.impl.directoryServer389.entry;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.impl.edir.EdirErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.DirectoryVendor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DirectoryServer389VendorFactory implements VendorFactory
{
    private static final String ROOT_DSE_ATTRIBUTE_VENDOR_NAME = "vendorName";
    private static final String ROOT_DSE_ATTRIBUTE_VENDOR_VERSION = "vendorVersion";

    private static final ErrorMap ERROR_MAP = new EdirErrorMap();

    public ChaiUser createChaiUser( final String entryDN, final ChaiProvider provider )
    {
        return new DirectoryServer389User( entryDN, provider );
    }

    public ChaiGroup createChaiGroup( final String entryDN, final ChaiProvider provider )
    {
        return new DirectoryServer389Group( entryDN, provider );
    }

    public ChaiEntry createChaiEntry( final String entryDN, final ChaiProvider provider )
    {
        return new DirectoryServer389Entry( entryDN, provider );
    }

    public DirectoryVendor getDirectoryVendor()
    {
        return DirectoryVendor.GENERIC;
    }

    public ErrorMap getErrorMap()
    {
        return ERROR_MAP;
    }

    @Override
    public Set<String> interestedDseAttributes()
    {
        return Collections.unmodifiableSet( new HashSet<>( Arrays.asList(
                ROOT_DSE_ATTRIBUTE_VENDOR_NAME,
                ROOT_DSE_ATTRIBUTE_VENDOR_VERSION
        ) ) );
    }

    @Override
    public boolean detectVendorFromRootDSEData( final Map<String, List<String>> rootDseAttributeValues )
    {
        if ( rootDseAttributeValues != null && rootDseAttributeValues.containsKey( ROOT_DSE_ATTRIBUTE_VENDOR_NAME ) )
        {
            for ( final String vendorName : rootDseAttributeValues.get( ROOT_DSE_ATTRIBUTE_VENDOR_NAME ) )
            {
                if ( vendorName.contains( "389 Project" ) )
                {
                    return true;
                }
            }
        }

        if ( rootDseAttributeValues != null && rootDseAttributeValues.containsKey( ROOT_DSE_ATTRIBUTE_VENDOR_VERSION ) )
        {
            for ( final String vendorName : rootDseAttributeValues.get( ROOT_DSE_ATTRIBUTE_VENDOR_VERSION ) )
            {
                if ( vendorName.contains( "389-Directory" ) )
                {
                    return true;
                }
            }
        }

        return false;
    }
}
