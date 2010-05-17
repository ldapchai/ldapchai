/*
 * LDAP Chai API
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009 Jason D. Rivard
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

package com.novell.ldapchai;

import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.ad.entry.ADEntryFactory;
import com.novell.ldapchai.impl.directoryServer389.entry.DirectoryServer389EntryFactory;
import com.novell.ldapchai.impl.edir.entry.EdirEntryFactory;
import com.novell.ldapchai.impl.generic.entry.GenericEntryFactory;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderFactory;
import com.novell.ldapchai.util.ChaiLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for {@link ChaiEntry} and its subclasses.  Instances are returned based
 * on the supplied {@link ChaiProvider}'s settings and state.   In particular,
 * {@link com.novell.ldapchai.provider.ChaiProvider#getDirectoryVendor()} is called, and
 * an implementing class appropriate for the vendor type is used.
 * </p>
 * For clarity the "create"
 * methods in this class create <i>instances</i> of {@code ChaiEntry}.  They do not
 * actualy create a new entry in the ldap directory.
 * <p/>
 * {@code ChaiProvider} instances can be obtained using the
 * {@link com.novell.ldapchai.provider.ChaiProviderFactory} factory.
 *
 * @author Jason D. Rivard
 */
public final class ChaiFactory {
// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(ChaiFactory.class);
    private static final Map<ChaiProvider.DIRECTORY_VENDOR,ChaiEntryFactory> ENTRY_FACTORY_MAP = new HashMap<ChaiProvider.DIRECTORY_VENDOR,ChaiEntryFactory>();

    static {
        ENTRY_FACTORY_MAP.put(ChaiProvider.DIRECTORY_VENDOR.NOVELL_EDIRECTORY, new EdirEntryFactory());
        ENTRY_FACTORY_MAP.put(ChaiProvider.DIRECTORY_VENDOR.MICROSOFT_ACTIVE_DIRECTORY, new ADEntryFactory());
        ENTRY_FACTORY_MAP.put(ChaiProvider.DIRECTORY_VENDOR.DIRECTORY_SERVER_389, new DirectoryServer389EntryFactory());
        ENTRY_FACTORY_MAP.put(ChaiProvider.DIRECTORY_VENDOR.GENERIC, new GenericEntryFactory());
    }
// -------------------------- STATIC METHODS --------------------------

    /**
     * Returns a {@code ChaiEntry} instance representing the supplied <i>entryDN</i>.
     *
     * @param entryDN  A valid ldap entry DN (Distinguished Name) of an entry
     * @param provider A valid and functioning {@code ChaiProvider}.  The {@code ChaiProvider}'s ldap
     *                 connection will be used by the {@code ChaiEntry}.
     * @return A valid {@code ChaiEntry}
     */
    public static ChaiEntry createChaiEntry(final String entryDN, final ChaiProvider provider)
            throws ChaiUnavailableException
    {
        final ChaiEntryFactory entryFactory = getChaiEntryFactory(provider.getDirectoryVendor());
        return entryFactory.createChaiEntry(entryDN, provider);
    }

    /**
     * Returns a {@code ChaiGroup} instance representing the supplied <i>groupDN</i>.
     *
     * @param groupDN  A valid ldap entry DN (Distinguished Name) of an entry
     * @param provider A valid and functioning {@code ChaiProvider}.  The {@code ChaiProvider}'s ldap
     *                 connection will be used by the {@code ChaiGroup}.
     * @return A valid {@code ChaiGroup}
     */
    public static ChaiGroup createChaiGroup(final String groupDN, final ChaiProvider provider)
            throws ChaiUnavailableException
    {
        final ChaiEntryFactory entryFactory = getChaiEntryFactory(provider.getDirectoryVendor());
        return entryFactory.createChaiGroup(groupDN, provider);
    }

    /**
     * Returns a {@code ChaiUser} instance representing the supplied <i>userDN</i>.
     *
     * @param userDN   A valid ldap entry DN (Distinguished Name) of an entry
     * @param provider A valid and functioning {@code ChaiProvider}.  The {@code ChaiProvider}'s ldap
     *                 connection will be used by the {@code ChaiGroup}.
     * @return A valid {@code ChaiUser}
     */
    public static ChaiUser createChaiUser(final String userDN, final ChaiProvider provider)
            throws ChaiUnavailableException
    {
        final ChaiEntryFactory entryFactory = getChaiEntryFactory(provider.getDirectoryVendor());
        return entryFactory.createChaiUser(userDN, provider);
    }

    /**
     * Convenience method for {@link com.novell.ldapchai.provider.ChaiProviderFactory#quickProvider(String,String,String)}.
     * <p/>
     * Get a ChaiUser using a standard JNDI ChaiProvider with default settings.
     *
     * @param bindDN   ldap bind DN, in ldap fully qualified syntax.  Also used as the DN of the returned ChaiUser.
     * @param password password for the bind DN.
     * @param ldapURL  ldap server and port in url format, example: <i>ldap://127.0.0.1:389</i>
     * @return A ChaiUser instance of the bindDN with an underlying ChaiProvider connected using the supplied parameters.
     * @throws ChaiUnavailableException If the directory server(s) are not reachable.
     * @see com.novell.ldapchai.provider.ChaiProviderFactory#quickProvider (String, String, String)
     */
    public static ChaiUser quickProvider(final String ldapURL, final String bindDN, final String password)
            throws ChaiUnavailableException
    {
        return ChaiProviderFactory.quickProvider(ldapURL, bindDN, password);
    }

    private static ChaiEntryFactory getChaiEntryFactory(final ChaiProvider.DIRECTORY_VENDOR vendor) {
        final ChaiEntryFactory returnEntryFactory =  ENTRY_FACTORY_MAP.get(vendor);
        if (returnEntryFactory == null) {
            return ENTRY_FACTORY_MAP.get(ChaiProvider.DIRECTORY_VENDOR.GENERIC);
        }
        return returnEntryFactory;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private ChaiFactory()
    {
    }

    public interface ChaiEntryFactory {
        ChaiUser createChaiUser(final String entryDN, final ChaiProvider provider);

        ChaiGroup createChaiGroup(final String entryDN, final ChaiProvider provider);

        ChaiEntry createChaiEntry(final String entryDN, final ChaiProvider provider);

        ChaiProvider.DIRECTORY_VENDOR getDirectoryVendor();
    }
}

