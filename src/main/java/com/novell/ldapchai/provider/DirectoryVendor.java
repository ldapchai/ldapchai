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

package com.novell.ldapchai.provider;

import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.impl.ad.entry.ADVendorFactory;
import com.novell.ldapchai.impl.apacheds.entry.ApacheDSVendorFactory;
import com.novell.ldapchai.impl.directoryServer389.entry.DirectoryServer389VendorFactory;
import com.novell.ldapchai.impl.edir.entry.EDirectoryVendorFactory;
import com.novell.ldapchai.impl.generic.entry.GenericEntryFactory;
import com.novell.ldapchai.impl.openldap.entry.OpenLDAPVendorFactory;
import com.novell.ldapchai.impl.oracleds.entry.OracleDSVendorFactory;
import com.novell.ldapchai.impl.freeipa.entry.FreeIPAVendorFactory;

/**
 * Indicates the Vendor (product/organization) of an LDAP directory server
 * implementation.
 */
public enum DirectoryVendor
{
    ACTIVE_DIRECTORY ( new ADVendorFactory() ),
    EDIRECTORY( new EDirectoryVendorFactory() ),
    OPEN_LDAP ( new OpenLDAPVendorFactory() ),
    DIRECTORY_SERVER_389( new DirectoryServer389VendorFactory() ),
    ORACLE_DS ( new OracleDSVendorFactory() ),
    FREEIPA ( FreeIPAVendorFactory.getInstance() ),
    APACHE_DS( new ApacheDSVendorFactory() ),
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
