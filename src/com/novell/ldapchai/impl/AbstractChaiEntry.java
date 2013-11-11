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

package com.novell.ldapchai.impl;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.edir.entry.EdirEntries;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.SearchHelper;
import com.novell.ldapchai.util.internal.Base64Util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * A complete implementation of {@code ChaiEntry} interface.
 * <p/>
 * Clients looking to obtain a {@code ChaiEntry} instance should look to {@link com.novell.ldapchai.ChaiFactory}.
 * <p/>
 * @author Jason D. Rivard
 */
public abstract class AbstractChaiEntry implements ChaiEntry {
// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    private enum networkAddressType {
        IPv4(9);

        private int typeIdentifier;

        networkAddressType(final int typeIdentifier)
        {
            this.typeIdentifier = typeIdentifier;
        }

        public int getTypeIdentifier()
        {
            return typeIdentifier;
        }

        public static networkAddressType forIdentifier(final int typeIdentifier)
        {
            for (final networkAddressType type : networkAddressType.values()) {
                if (type.getTypeIdentifier() == typeIdentifier) {
                    return type;
                }
            }
            return null;
        }
    }

// ------------------------------ FIELDS ------------------------------

    protected static final ChaiLogger LOGGER = ChaiLogger.getLogger(AbstractChaiEntry.class);
    // ------------------------- PUBLIC CONSTANTS -------------------------
    /**
     * Stores the original dn, used in the constuctor.
     */
    protected String entryDN;

    /**
     * Attribute to store the LDAP Provider.
     */
    protected ChaiProvider chaiProvider;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Standard constructor
     *
     * @param entryDN ldap DN in String format
     * @param chaiProvider an active {@code ChaiProvider} instance
     */
    public AbstractChaiEntry(final String entryDN, final ChaiProvider chaiProvider)
    {
        this.chaiProvider = chaiProvider;
        this.entryDN = entryDN == null ? "" : entryDN;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public final ChaiProvider getChaiProvider()
    {
        return this.chaiProvider;
    }

    public final String getEntryDN()
    {
        return this.entryDN;
    }

// ------------------------ CANONICAL METHODS ------------------------

    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ChaiEntry)) return false;

        final AbstractChaiEntry chaiEntry = (AbstractChaiEntry) o;

        return !(chaiProvider != null ? !chaiProvider.equals(chaiEntry.chaiProvider) : chaiEntry.chaiProvider != null) && !(entryDN != null ? !entryDN.equals(chaiEntry.entryDN) : chaiEntry.entryDN != null);
    }

    public int hashCode()
    {
        int result;
        result = (entryDN != null ? entryDN.hashCode() : 0);
        result = 29 * result + (chaiProvider != null ? chaiProvider.hashCode() : 0);
        return result;
    }

    /**
     * This function would display the object and its attributes.
     *
     * @return String
     */
    public String toString()
    {
        // Allocate the String Buffer
        final StringBuilder sb = new StringBuilder();

        sb.append("EntryDN: ").append(this.entryDN);

        // Return the String.
        return sb.toString();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ChaiEntry ---------------------

    public final void addAttribute(final String attributeName, final String attributeValue)
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.writeStringAttribute(entryDN, attributeName, Collections.singleton(attributeValue), false);
    }

    public final void addAttribute(final String attributeName, final Set<String> attributeValues)
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.writeStringAttribute(entryDN, attributeName, attributeValues, false);
    }

    public final void addAttribute(final String attributeName, final String... attributeValues)
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.writeStringAttribute(entryDN, attributeName, new HashSet<String>(Arrays.asList(attributeValues)), false);
    }

    public final boolean compareStringAttribute(final String attributeName, final String attributeValue)
            throws ChaiUnavailableException, ChaiOperationException
    {
        return chaiProvider.compareStringAttribute(this.getEntryDN(), attributeName, attributeValue);
    }

    public final void deleteAttribute(final String attributeName, final String attributeValue)
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.deleteStringAttributeValue(this.entryDN, attributeName, attributeValue);
    }

    public final Set<ChaiEntry> getChildObjects()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Set<ChaiEntry> returnSet = new HashSet<ChaiEntry>();
        final String filter = "(" + ChaiConstant.ATTR_LDAP_OBJECTCLASS + "=*)";
        final Map<String, Map<String,String>> results = this.getChaiProvider().search(this.getEntryDN(), filter, Collections.<String>emptySet(), ChaiProvider.SEARCH_SCOPE.ONE);
        for (final String dn : results.keySet()) {
            returnSet.add(ChaiFactory.createChaiEntry(dn, this.getChaiProvider()));
        }
        return returnSet;
    }

    public final ChaiEntry getParentEntry()
            throws ChaiUnavailableException
    {
        final String parentDNString = getParentDNString(this.getEntryDN());
        if (parentDNString == null) {
            return null;
        }
        return ChaiFactory.createChaiEntry(parentDNString,getChaiProvider());
    }

    private static String getParentDNString(final String inputDN) {
        if (inputDN == null || inputDN.length() < 0) {
            return null;
        }
        final String dnSeparatorRegex = "(?<!\\\\),";
        final String[] dnSegments = inputDN.split(dnSeparatorRegex);
        if (dnSegments.length < 2) {
            return null;
        }
        final StringBuilder parentDN = new StringBuilder();
        for (int i = 1; i < (dnSegments.length); i++) {
            parentDN.append(dnSegments[i]);
            if (i < (dnSegments.length - 1)) {
                parentDN.append(",");
            }
        }
        return parentDN.toString();
    }

    public final boolean isValid()
    {
        try {
            final Set<String> results = this.readMultiStringAttribute(ChaiConstant.ATTR_LDAP_OBJECTCLASS);
            if (results != null && results.size() >= 1) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.trace("unexpected error during isValid check: " + e.getMessage());
        }

        return false;
    }

    public final boolean readBooleanAttribute(final String attributeName)
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String value = this.readStringAttribute(attributeName);
        return value != null && value.equalsIgnoreCase("TRUE");
    }

    public String readCanonicalDN()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.returnNoAttributes();
        searchHelper.setSearchScope(ChaiProvider.SEARCH_SCOPE.BASE);
        searchHelper.setFilter(SearchHelper.DEFAULT_FILTER);

        final Map<String, Map<String,String>> results = this.getChaiProvider().search(this.getEntryDN(), searchHelper);
        if (results.size() == 1) {
            return results.keySet().iterator().next();
        }

        if (results.isEmpty()) {
            throw new ChaiOperationException("search for canonical DN resulted in no results", ChaiError.UNKNOWN);
        }

        throw new ChaiOperationException("search for canonical DN resulted in multiple results", ChaiError.UNKNOWN);
    }

    public final int readIntAttribute(final String attributeName)
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String value = this.readStringAttribute(attributeName);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public final byte[][] readMultiByteAttribute(final String attributeName)
            throws ChaiOperationException, ChaiUnavailableException
    {
        return chaiProvider.readMultiByteAttribute(this.getEntryDN(), attributeName);
    }

    public final Set<String> readMultiStringAttribute(final String attributeName)
            throws ChaiOperationException, ChaiUnavailableException
    {
        return chaiProvider.readMultiStringAttribute(entryDN, attributeName);
    }

    public List<InetAddress> readNetAddressAttribute(final String attributeName)
            throws ChaiUnavailableException, ChaiOperationException
    {
        final byte[][] values = this.readMultiByteAttribute(attributeName);
        final List<InetAddress> returnValues = new ArrayList<InetAddress>();

        for (final byte[] value : values) {
            final String strValue = new String(value);
            final int sepPos = strValue.indexOf('#');
            final int typeInt = Integer.valueOf(strValue.substring(0, sepPos));
            final networkAddressType type = networkAddressType.forIdentifier(typeInt);
            switch (type) {
                case IPv4:
                    final StringBuilder sb = new StringBuilder();
                    try {
                        sb.append(256 + value[sepPos + 3] % 256).append(".");
                        sb.append(256 + value[sepPos + 4] % 256).append(".");
                        sb.append(256 + value[sepPos + 5] % 256).append(".");
                        sb.append(256 + value[sepPos + 6] % 256);
                        returnValues.add(InetAddress.getByName(sb.toString()));
                    } catch (UnknownHostException e) {
                        LOGGER.error("error while parsing network address '" + strValue + "' " + e.getMessage());
                    } catch (ArrayIndexOutOfBoundsException e) {
                        LOGGER.error("error while parsing network address '" + strValue + "' " + e.getMessage());
                    }
            }
        }
        return returnValues;
    }

    public final Set<String> readObjectClass()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readMultiStringAttribute(ChaiConstant.ATTR_LDAP_OBJECTCLASS);
    }

    public final String readStringAttribute(final String attributeName)
            throws ChaiOperationException, ChaiUnavailableException
    {
        // Using the LDAP Helper, get the attribute for the selected attribute name.
        return chaiProvider.readStringAttribute(entryDN, attributeName);
    }

    public final Map<String,String> readStringAttributes(final Set<String> attributes)
            throws ChaiOperationException, ChaiUnavailableException
    {
        return chaiProvider.readStringAttributes(this.entryDN, attributes);
    }

    public final void replaceAttribute(final String attributeName, final String oldValue, final String newValue)
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.replaceStringAttribute(this.entryDN, attributeName, oldValue, newValue);
    }

    public final Set<ChaiEntry> search(final String filter)
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.search(new SearchHelper(filter));
    }

    public Set<ChaiEntry> search(final SearchHelper searchHelper)
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<ChaiEntry> resultSet = new HashSet<ChaiEntry>();
        final Map<String, Map<String,String>> results = chaiProvider.search(this.getEntryDN(), searchHelper.getFilter(), searchHelper.getAttributes(), searchHelper.getSearchScope());
        for (final String dn : results.keySet()) {
            resultSet.add(ChaiFactory.createChaiEntry(dn, this.getChaiProvider()));
        }
        return resultSet;
    }

    public final Set<ChaiEntry> search(final String filter, final ChaiProvider.SEARCH_SCOPE searchScope)
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.search(new SearchHelper(filter, searchScope));
    }

    public final void writeStringAttribute(final String attributeName, final String attributeValue)
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.writeStringAttribute(this.entryDN, attributeName, attributeValue == null ? null : Collections.<String>singleton(attributeValue), true);
    }

    public final void writeStringAttributes(final Map<String,String> attributeValueProps)
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.writeStringAttributes(this.entryDN, attributeValueProps, true);
    }

    public final void writeStringAttribute(final String attributeName, final Set<String> attributeValues)
            throws ChaiOperationException, ChaiUnavailableException
    {
        // Using the LDAP Helper, set the attributes.
        chaiProvider.writeStringAttribute(this.entryDN, attributeName, attributeValues, true);
    }

    public void writeStringAttribute(final String attributeName, final String... attributeValues)
            throws ChaiOperationException, ChaiUnavailableException
    {
        // Using the LDAP Helper, set the attributes.
        chaiProvider.writeStringAttribute(this.entryDN, attributeName, attributeValues == null ? null : new HashSet<String>(Arrays.asList(attributeValues)), true);
    }

    public void writeBinaryAttribute(final String attributeName, final byte[]... attributeValues)
            throws ChaiOperationException, ChaiUnavailableException
    {
        // Using the LDAP Helper, set the attributes.
        chaiProvider.writeBinaryAttribute(this.entryDN, attributeName, attributeValues, true);
    }

    public void replaceBinaryAttribute(final String attributeName, final byte[] oldValue, final byte[] newValue)
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.replaceBinaryAttribute(this.entryDN, attributeName, oldValue, newValue);
    }

    public Date readDateAttribute(final String attributeName)
            throws ChaiUnavailableException, ChaiOperationException
    {
        final String value = this.readStringAttribute(attributeName);
        if (value != null) {
            return EdirEntries.convertZuluToDate(value);
        }
        return null;
    }

    public void writeDateAttribute(final String attributeName, final Date date)
            throws ChaiUnavailableException, ChaiOperationException
    {
        if (date == null) {
            throw new NullPointerException("date cannot be null");
        }
        writeStringAttribute(attributeName, EdirEntries.convertDateToZulu(date));
    }

    public String readGUID() throws ChaiOperationException, ChaiUnavailableException {
        final byte[][] guidValues = this.readMultiByteAttribute("guid");
        if (guidValues == null || guidValues.length < 1) {
            return null;
        }
        final byte[] guidValue = guidValues[0];
        return Base64Util.encodeBytes(guidValue);
    }
}