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

package com.novell.ldapchai.impl.ad.entry;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.impl.ad.ADErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderImplementor;
import com.novell.ldapchai.provider.DirectoryVendor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ADVendorFactory implements VendorFactory
{
    private static final String ROOT_DSE_ATTRIBUTE_NAMING_CONTEXT = "rootDomainNamingContext";

    @Override
    public User newChaiUser( final String userDN, final ChaiProvider chaiProvider )
    {
        return new UserImpl( userDN, chaiProvider );
    }

    @Override
    public ChaiGroup newChaiGroup( final String entryDN, final ChaiProvider provider )
    {
        return new GroupImpl( entryDN, provider );
    }

    @Override
    public ChaiEntry newChaiEntry( final String entryDN, final ChaiProvider provider )
    {
        return new TopImpl( entryDN, provider );
    }

    @Override
    public DirectoryVendor getDirectoryVendor()
    {
        return DirectoryVendor.ACTIVE_DIRECTORY;
    }

    @Override
    public ErrorMap getErrorMap()
    {
        return ADErrorMap.instance();
    }

    @Override
    public Set<String> interestedDseAttributes()
    {
        return Collections.singleton( ROOT_DSE_ATTRIBUTE_NAMING_CONTEXT );
    }

    @Override
    public boolean detectVendorFromRootDSEData( final Map<String, List<String>> rootDseAttributeValues )
    {
        if ( rootDseAttributeValues != null && rootDseAttributeValues.containsKey( ROOT_DSE_ATTRIBUTE_NAMING_CONTEXT ) )
        {
            for ( final String namingContext : rootDseAttributeValues.get( ROOT_DSE_ATTRIBUTE_NAMING_CONTEXT ) )
            {
                if ( namingContext.contains( "DC=" ) )
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
        return ADEntries.convertWinEpochToDate( input );
    }

    @Override
    public String instantToString( final Instant input )
    {
        return ADEntries.convertDateToWinEpoch( input );
    }

    @Override
    public boolean allowWatchdogDisconnect( final ChaiProviderImplementor chaiProvider )
    {
        return true;
    }
}
