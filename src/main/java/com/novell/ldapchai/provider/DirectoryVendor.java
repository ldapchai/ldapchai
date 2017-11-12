package com.novell.ldapchai.provider;

import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.impl.ad.entry.ADVendorFactory;
import com.novell.ldapchai.impl.directoryServer389.entry.DirectoryServer389VendorFactory;
import com.novell.ldapchai.impl.edir.entry.EDirectoryVendorFactory;
import com.novell.ldapchai.impl.generic.entry.GenericEntryFactory;
import com.novell.ldapchai.impl.openldap.entry.OpenLDAPVendorFactory;
import com.novell.ldapchai.impl.oracleds.entry.OracleDSVendorFactory;


public enum DirectoryVendor
{
    ACTIVE_DIRECTORY ( new ADVendorFactory() ),
    EDIRECTORY( new EDirectoryVendorFactory() ),
    OPEN_LDAP ( new OpenLDAPVendorFactory() ),
    DIRECTORY_SERVER_389( new DirectoryServer389VendorFactory() ),
    ORACLE_DS ( new OracleDSVendorFactory() ),
    GENERIC( new GenericEntryFactory() ),;

    private final VendorFactory vendorFactory;

    DirectoryVendor( final VendorFactory vendorFactory )
    {
        this.vendorFactory = vendorFactory;
    }

    public VendorFactory getVendorFactory()
    {
        return vendorFactory;
    }
}
