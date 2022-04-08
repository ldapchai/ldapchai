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

package com.novell.ldapchai.impl.freeipa.entry;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.impl.edir.EdirErrorMap;
import com.novell.ldapchai.impl.edir.entry.EdirEntries;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.DirectoryVendor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FreeIPAVendorFactory implements VendorFactory
{
    private static final String ROOT_DSE_ATTRIBUTE_VENDOR_NAME = "vendorName";
    private static final String ROOT_DSE_ATTRIBUTE_VENDOR_VERSION = "vendorVersion";
    private static final String ROOT_DSE_ATTRIBUTE_SUPPORTED_EXTENSION = "supportedExtension";

    private static final ErrorMap ERROR_MAP = new EdirErrorMap();

    private static final FreeIPAVendorFactory SINGLETON = new FreeIPAVendorFactory();

    public static FreeIPAVendorFactory getInstance()
    {
        return SINGLETON;
    }

    private FreeIPAVendorFactory()
    {
    }

    @Override
    public ChaiUser newChaiUser( final String entryDN, final ChaiProvider provider )
    {
        return new FreeIPAUser( entryDN, provider );
    }

    @Override
    public ChaiGroup newChaiGroup( final String entryDN, final ChaiProvider provider )
    {
        return new FreeIPAGroup( entryDN, provider );
    }

    @Override
    public ChaiEntry newChaiEntry( final String entryDN, final ChaiProvider provider )
    {
        return new FreeIPAEntry( entryDN, provider );
    }

    @Override
    public DirectoryVendor getDirectoryVendor()
    {
        return DirectoryVendor.FREEIPA;
    }

    @Override
    public ErrorMap getErrorMap()
    {
        return ERROR_MAP;
    }

    @Override
    public Set<String> interestedDseAttributes()
    {
        return Collections.unmodifiableSet( new HashSet<>( Arrays.asList(
                ROOT_DSE_ATTRIBUTE_VENDOR_NAME,
                ROOT_DSE_ATTRIBUTE_VENDOR_VERSION,
                ROOT_DSE_ATTRIBUTE_SUPPORTED_EXTENSION
        ) ) );
    }

    @Override
    public boolean detectVendorFromRootDSEData( final Map<String, List<String>> rootDseAttributeValues )
    {
        boolean nameMatch = false;
        if ( rootDseAttributeValues != null && rootDseAttributeValues.containsKey( ROOT_DSE_ATTRIBUTE_VENDOR_NAME ) )
        {
            for ( final String vendorName : rootDseAttributeValues.get( ROOT_DSE_ATTRIBUTE_VENDOR_NAME ) )
            {
                if ( vendorName.contains( "389 Project" ) )
                {
                    nameMatch = true;
                    break;
                }
            }
        }
        if ( !nameMatch )
        {
            return false;
        }

        boolean versionMatch = false;
        if ( rootDseAttributeValues != null && rootDseAttributeValues.containsKey( ROOT_DSE_ATTRIBUTE_VENDOR_VERSION ) )
        {
            for ( final String vendorVersion : rootDseAttributeValues.get( ROOT_DSE_ATTRIBUTE_VENDOR_VERSION ) )
            {
                if ( vendorVersion.contains( "389-Directory" ) )
                {
                    versionMatch = true;
                    break;
                }
            }
        }
        if ( !versionMatch )
        {
            return false;
        }

        boolean supportedExtensionMatch = false;
        if ( rootDseAttributeValues != null && rootDseAttributeValues.containsKey( ROOT_DSE_ATTRIBUTE_SUPPORTED_EXTENSION ) )
        {
            for ( final String supportedExtension : rootDseAttributeValues.get( ROOT_DSE_ATTRIBUTE_SUPPORTED_EXTENSION ) )
            {
                if ( supportedExtension.startsWith( "2.16.840.1.113730.3.8." ) )
                {
                    supportedExtensionMatch = true;
                    break;
                }
            }
        }
        if ( !supportedExtensionMatch )
        {
            return false;
        }

        return true;
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
