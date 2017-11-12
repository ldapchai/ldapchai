package com.novell.ldapchai.impl;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.DirectoryVendor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface VendorFactory
{
    ChaiUser createChaiUser( String entryDN, ChaiProvider provider );

    ChaiGroup createChaiGroup( String entryDN, ChaiProvider provider );

    ChaiEntry createChaiEntry( String entryDN, ChaiProvider provider );

    DirectoryVendor getDirectoryVendor();

    ErrorMap getErrorMap();

    Set<String> interestedDseAttributes();

    boolean detectVendorFromRootDSEData( Map<String, List<String>> rootDseAttributeValues );
}
