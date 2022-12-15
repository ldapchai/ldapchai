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

package com.novell.ldapchai.impl.openldap.entry;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.impl.openldap.OpenLDAPErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderImplementor;
import com.novell.ldapchai.provider.DirectoryVendor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenLDAPVendorFactory implements VendorFactory
{
    @Override
    public ChaiUser newChaiUser( final String entryDN, final ChaiProvider provider )
    {
        return new OpenLDAPUser( entryDN, provider );
    }

    @Override
    public ChaiGroup newChaiGroup( final String entryDN, final ChaiProvider provider )
    {
        return new OpenLDAPGroup( entryDN, provider );
    }

    @Override
    public ChaiEntry newChaiEntry( final String entryDN, final ChaiProvider provider )
    {
        return new OpenLDAPEntry( entryDN, provider );
    }

    @Override
    public DirectoryVendor getDirectoryVendor()
    {
        return DirectoryVendor.OPEN_LDAP;
    }

    @Override
    public ErrorMap getErrorMap()
    {
        return OpenLDAPErrorMap.instance();
    }

    @Override
    public Set<String> interestedDseAttributes()
    {
        return Collections.singleton( ChaiConstant.ATTR_LDAP_OBJECTCLASS );
    }

    @Override
    public boolean detectVendorFromRootDSEData( final Map<String, List<String>> rootDseAttributeValues )
    {
        final String objectClassAttribute = ChaiConstant.ATTR_LDAP_OBJECTCLASS;
        if ( rootDseAttributeValues != null && rootDseAttributeValues.containsKey( objectClassAttribute ) )
        {
            for ( final String objectClassVaue : rootDseAttributeValues.get( objectClassAttribute ) )
            {
                if ( objectClassVaue.contains( "OpenLDAProotDSE" ) )
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
        return OpenLDAPEntries.convertZuluToDate( input );
    }

    @Override
    public String instantToString( final Instant input )
    {
        return OpenLDAPEntries.convertDateToZulu( input );
    }

    @Override
    public boolean allowWatchdogDisconnect( final ChaiProviderImplementor chaiProvider )
    {
        return true;
    }
}
