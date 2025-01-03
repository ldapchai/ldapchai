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

package com.novell.ldapchai.impl.lldap;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.impl.TimeFormatConverter;
import com.novell.ldapchai.impl.TimeFormatConverterFactory;
import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.impl.openldap.OpenLDAPErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderImplementor;
import com.novell.ldapchai.provider.DirectoryVendor;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LldapVendorFactory implements VendorFactory
{

    private static final String LDAP_ATTR_VENDOR_NAME = "vendorName";
    private static final String LLDAP_DS_VENDOR_NAME = "LLDAP";

    private static final String TIMESTASMP_PATTERN_RFC_3339 = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSxxx";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern( TIMESTASMP_PATTERN_RFC_3339 );


    private static final TimeFormatConverter TIME_FORMAT_CONVERTER
            = TimeFormatConverterFactory.custom( DATE_TIME_FORMATTER, DATE_TIME_FORMATTER );

    @Override
    public ChaiUser newChaiUser( final String entryDN, final ChaiProvider provider )
    {
        return new LldapLdapUser( entryDN, provider );
    }

    @Override
    public ChaiGroup newChaiGroup( final String entryDN, final ChaiProvider provider )
    {
        return new LldapGroup( entryDN, provider );
    }

    @Override
    public ChaiEntry newChaiEntry( final String entryDN, final ChaiProvider provider )
    {
        return new LldapLdapEntry( entryDN, provider );
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
        return Collections.singleton( LDAP_ATTR_VENDOR_NAME );
    }

    @Override
    public boolean detectVendorFromRootDSEData( final Map<String, List<String>> rootDseAttributeValues )
    {
        final String venderNameAttribute = LDAP_ATTR_VENDOR_NAME;
        if ( venderNameAttribute != null && rootDseAttributeValues.containsKey( LDAP_ATTR_VENDOR_NAME ) )
        {
            for ( final String vendorNames : rootDseAttributeValues.get( LDAP_ATTR_VENDOR_NAME ) )
            {
                if ( vendorNames.startsWith( LLDAP_DS_VENDOR_NAME ) )
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
        return TIME_FORMAT_CONVERTER.parseStringToInstant( input ).orElse( null );
    }

    @Override
    public String instantToString( final Instant input )
    {
        return TIME_FORMAT_CONVERTER.outputInstantToString( input );
    }

    @Override
    public boolean allowWatchdogDisconnect( final ChaiProviderImplementor chaiProvider )
    {
        return true;
    }
}
