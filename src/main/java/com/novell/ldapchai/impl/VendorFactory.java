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

package com.novell.ldapchai.impl;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderImplementor;
import com.novell.ldapchai.provider.DirectoryVendor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface VendorFactory
{
    ChaiUser newChaiUser( String entryDN, ChaiProvider provider );

    ChaiGroup newChaiGroup( String entryDN, ChaiProvider provider );

    ChaiEntry newChaiEntry( String entryDN, ChaiProvider provider );

    DirectoryVendor getDirectoryVendor();

    ErrorMap getErrorMap();

    Set<String> interestedDseAttributes();

    boolean detectVendorFromRootDSEData( Map<String, List<String>> rootDseAttributeValues );

    Instant stringToInstant( String input );

    String instantToString( Instant input );

    boolean allowWatchdogDisconnect( ChaiProviderImplementor chaiProvider );
}
