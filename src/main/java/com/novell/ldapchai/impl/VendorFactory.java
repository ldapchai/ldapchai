package com.novell.ldapchai.impl;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
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
}
