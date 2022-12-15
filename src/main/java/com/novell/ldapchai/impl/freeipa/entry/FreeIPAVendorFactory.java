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
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.impl.edir.entry.EdirEntries;
import com.novell.ldapchai.impl.openldap.OpenLDAPErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderImplementor;
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
    private static final String ROOT_DSE_ATTRIBUTE_IPA_TOPOLOGY_PLUGIN_VERSION = "ipaTopologyPluginVersion";

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
        return OpenLDAPErrorMap.instance();
    }

    @Override
    public Set<String> interestedDseAttributes()
    {
        return Collections.unmodifiableSet( new HashSet<>( Arrays.asList(
                ROOT_DSE_ATTRIBUTE_IPA_TOPOLOGY_PLUGIN_VERSION
        ) ) );
    }

    @Override
    public boolean detectVendorFromRootDSEData( final Map<String, List<String>> rootDseAttributeValues )
    {
        if ( rootDseAttributeValues != null && rootDseAttributeValues.containsKey( ROOT_DSE_ATTRIBUTE_IPA_TOPOLOGY_PLUGIN_VERSION ) )
        {
            for ( final String ipaTopologyPluginVersion : rootDseAttributeValues.get( ROOT_DSE_ATTRIBUTE_IPA_TOPOLOGY_PLUGIN_VERSION ) )
            {
                if ( Double.parseDouble( ipaTopologyPluginVersion ) >= 1.0 )
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

    @Override
    public boolean allowWatchdogDisconnect( final ChaiProviderImplementor chaiProvider )
    {
        return true;
    }
}
