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

package com.novell.ldapchai.impl.apacheds.entry;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderImplementor;
import com.novell.ldapchai.provider.DirectoryVendor;
import com.novell.ldapchai.util.internal.StringHelper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ApacheDSVendorFactory implements VendorFactory
{
    private static final String LDAP_ATTR_VENDOR_NAME = "vendorName";
    private static final String APACHE_DS_VENDOR_NAME = "Apache Software Foundation";

    public static final String APACHE_DS_TIMESTAMP_PATTERN = "uuuuMMddHHmmss.SSSX";

    @Override
    public ChaiUser newChaiUser( final String entryDN, final ChaiProvider provider )
    {
        return new ApacheDSUser( entryDN, provider );
    }

    @Override
    public ChaiGroup newChaiGroup( final String entryDN, final ChaiProvider provider )
    {
        return new ApacheDSGroup( entryDN, provider );
    }

    @Override
    public ChaiEntry newChaiEntry( final String entryDN, final ChaiProvider provider )
    {
        return new ApacheDSEntry( entryDN, provider );
    }

    @Override
    public DirectoryVendor getDirectoryVendor()
    {
        return DirectoryVendor.APACHE_DS;
    }

    @Override
    public ErrorMap getErrorMap()
    {
        return ApacheDSErrorMap.instance();
    }

    @Override
    public Set<String> interestedDseAttributes()
    {
        return Collections.singleton( LDAP_ATTR_VENDOR_NAME );
    }

    @Override
    public boolean detectVendorFromRootDSEData( final Map<String, List<String>> rootDseAttributeValues )
    {
        if ( rootDseAttributeValues != null )
        {
            final List<String> vendorNames = rootDseAttributeValues.get( LDAP_ATTR_VENDOR_NAME );
            if ( vendorNames != null )
            {
                for ( final String vendorName : vendorNames )
                {
                    if ( APACHE_DS_VENDOR_NAME.equals( vendorName ) )
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public Instant stringToInstant( final String input )
    {
        if ( StringHelper.isEmpty( input ) )
        {
            throw new NullPointerException();
        }

        try
        {
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern ( APACHE_DS_TIMESTAMP_PATTERN );
            final OffsetDateTime offsetDateTime = OffsetDateTime.parse ( input, dateTimeFormatter );
            return offsetDateTime.toInstant();
        }
        catch ( DateTimeParseException e )
        {
            throw new IllegalArgumentException( "unable to parse apacheDS time-string: " + e.getMessage() );
        }
    }

    @Override
    public String instantToString( final Instant input )
    {
        Objects.requireNonNull( input );

        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern( APACHE_DS_TIMESTAMP_PATTERN );
        final OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant( input, ZoneOffset.UTC );
        return dateTimeFormatter.format( offsetDateTime );
    }

    @Override
    public boolean allowWatchdogDisconnect( final ChaiProviderImplementor chaiProvider )
    {
        return true;
    }
}
