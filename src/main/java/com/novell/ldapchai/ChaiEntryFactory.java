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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSEersistant .  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.novell.ldapchai;

import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.impl.VendorFactory;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.DirectoryVendor;
import com.novell.ldapchai.util.ChaiLogger;


/**
 * Factory for {@link ChaiEntry} and its subclasses.  Instances are returned based
 * on the supplied {@link ChaiProvider}'s settings and state.   In particular,
 * {@link com.novell.ldapchai.provider.ChaiProvider#getDirectoryVendor()} is called, and
 * an implementing class appropriate for the vendor type is used.
 * </p>
 * For clarity the "create"
 * methods in this class create <i>instances</i> of {@code ChaiEntry}.  They do not
 * actualy create a new entry in the ldap directory.
 * <p>
 * {@code ChaiProvider} instances can be obtained using the
 * {@link com.novell.ldapchai.provider.ChaiProviderFactory} factory.
 *
 * @author Jason D. Rivard
 */
public final class ChaiEntryFactory
{

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( com.novell.ldapchai.ChaiEntryFactory.class );

    private final ChaiProvider chaiProvider;

    /**
     * Returns a {@code ChaiEntry} instance representing the supplied <i>entryDN</i>.
     *
     * @param entryDN A valid ldap entry DN (Distinguished Name) of an entry
     * @return A valid {@code ChaiEntry}
     */
    public ChaiEntry newChaiEntry( final String entryDN )
            throws ChaiUnavailableException
    {
        final VendorFactory entryFactory = getChaiProvider().getDirectoryVendor().getVendorFactory();
        return entryFactory.newChaiEntry( entryDN, getChaiProvider() );
    }

    /**
     * Returns a {@code ChaiGroup} instance representing the supplied <i>groupDN</i>.
     *
     * @param groupDN A valid ldap entry DN (Distinguished Name) of an entry
     * @return A valid {@code ChaiGroup}
     */
    public ChaiGroup newChaiGroup( final String groupDN )
            throws ChaiUnavailableException
    {
        final VendorFactory entryFactory = getChaiProvider().getDirectoryVendor().getVendorFactory();
        return entryFactory.newChaiGroup( groupDN, getChaiProvider() );
    }

    /**
     * Returns a {@code ChaiUser} instance representing the supplied <i>userDN</i>.
     *
     * @param userDN A valid ldap entry DN (Distinguished Name) of an entry
     * @return A valid {@code ChaiUser}
     */
    public ChaiUser newChaiUser( final String userDN )
            throws ChaiUnavailableException
    {
        final VendorFactory entryFactory = getChaiProvider().getDirectoryVendor().getVendorFactory();
        return entryFactory.newChaiUser( userDN, getChaiProvider() );
    }

    /**
     * Returns a {@code ChaiUser} instance representing the supplied <i>userDN</i>.
     *
     * @return A valid {@code ChaiUser}
     */
    public ErrorMap getErrorMap()
            throws ChaiUnavailableException
    {
        final VendorFactory entryFactory = getChaiProvider().getDirectoryVendor().getVendorFactory();
        return entryFactory.getErrorMap();
    }

    /**
     * Returns a {@code ChaiUser} instance representing the supplied <i>userDN</i>.
     *
     * @param vendor A valid and functioning {@code ChaiProvider}.  The {@code ChaiProvider}'s ldap
     *               connection will be used by the {@code ChaiGroup}.
     * @return A valid {@code ChaiUser}
     */
    public ErrorMap getErrorMap( final DirectoryVendor vendor )
            throws ChaiUnavailableException
    {
        final VendorFactory entryFactory = getChaiProvider().getDirectoryVendor().getVendorFactory();
        return entryFactory.getErrorMap();
    }


    private ChaiEntryFactory( final ChaiProvider chaiProvider )
    {
        this.chaiProvider = chaiProvider;
    }

    public static ChaiEntryFactory newChaiFactory( final ChaiProvider chaiProvider )
    {
        return new ChaiEntryFactory( chaiProvider );
    }


    public ChaiProvider getChaiProvider()
    {
        return chaiProvider;
    }
}

