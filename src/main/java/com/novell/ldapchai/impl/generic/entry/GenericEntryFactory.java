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

package com.novell.ldapchai.impl.generic.entry;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenericEntryFactory implements VendorFactory
{

    private static ErrorMap errorMap;

    public ChaiUser newChaiUser( final String entryDN, final ChaiProvider provider )
    {
        return new GenericChaiUser( entryDN, provider );
    }

    public ChaiGroup newChaiGroup( final String entryDN, final ChaiProvider provider )
    {
        return new GenericChaiGroup( entryDN, provider );
    }

    public ChaiEntry newChaiEntry( final String entryDN, final ChaiProvider provider )
    {
        return new GenericChaiEntry( entryDN, provider );
    }

    public DirectoryVendor getDirectoryVendor()
    {
        return DirectoryVendor.GENERIC;
    }

    public ErrorMap getErrorMap()
    {
        if ( errorMap == null )
        {
            errorMap = new EdirErrorMap();
        }
        return errorMap;
    }

    @Override
    public Set<String> interestedDseAttributes()
    {
        return Collections.emptySet();
    }

    @Override
    public boolean detectVendorFromRootDSEData( final Map<String, List<String>> rootDseAttributeValues )
    {
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
