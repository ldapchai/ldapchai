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

package com.novell.ldapchai;

import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.util.SearchHelper;

import java.net.InetAddress;
import java.util.*;

/**
 * {@code ChaiEntry} instances represent ldap entries.  Most other {@code Chai*} interfaces in this package inherit from
 * {@code ChaiEntry}. Each instance wraps a ldap distinguished name (DN) value that it then uses for all subsequent operations.  Methods are
 * provided to easily access or modify attributes of the entry, as well as to find related objects.  Clients can consume
 * {@code ChaiEntry} instances as if they were beans baacked by some persistant store.
 * <p/>
 * This interface is extended by several sub-interfaces that provide added functionality for specific
 * entry types such as users and groups.
 * <p/>
 * This interface and its sub-interfaces are intended to be the primary interface that callers to the
 * LDAP Chai API reference in their code.
 * <p/>
 * Instances of ChaiEntry can be obtained by using {@link com.novell.ldapchai.ChaiFactory}.
 *
 * @author Jason D. Rivard
 */

public interface ChaiEntry {
// ----------------------------- CONSTANTS ----------------------------


// -------------------------- OTHER METHODS --------------------------

    /**
     * Writes an additional attribute value to an attribute on an ldap entry.  This operation
     * will not overwrite any existing values.  If the attribute has no values already, the
     * supplied value will be set.
     * <p/>
     * Duplicate values are not permitted by ldap.  An attempt to add duplicate values will
     * result in an {@link ChaiOperationException} with {@link com.novell.ldapchai.exception.ChaiError}.
     *
     * @param attributeName  A valid attribute for the entry
     * @param attributeValue A string value to be added to the ldap entry
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#writeStringAttribute(String, String, java.util.List, boolean)
     */
    void addAttribute(String attributeName, String attributeValue)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Writes an additional attribute value to an attribute on an ldap entry.  This operation
     * will not overwrite any existing values.  If the attribute has no values already, the
     * supplied value will be set.
     * <p/>
     * Duplicate values are not permitted by ldap.  An attempt to add duplicate values will
     * result in an {@link ChaiOperationException} with {@link com.novell.ldapchai.exception.ChaiError}.
     *
     * @param attributeName   A valid attribute for the entry
     * @param attributeValues A set of string values to be added to the ldap entry
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#writeStringAttribute(String, String, java.util.List, boolean)
     */
    void addAttribute(String attributeName, Set<String> attributeValues)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Writes an additional attribute value to an attribute on an ldap entry.  This operation
     * will not overwrite any existing values.  If the attribute has no values already, the
     * supplied value will be set.
     * <p/>
     * Duplicate values are not permitted by ldap.  An attempt to add duplicate values will
     * result in an {@link ChaiOperationException} with {@link com.novell.ldapchai.exception.ChaiError}.
     *
     * @param attributeName   A valid attribute for the entry
     * @param attributeValues A set of string values to be added to the ldap entry
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#writeStringAttribute(String, String, java.util.List, boolean)
     */
    void addAttribute(String attributeName, String... attributeValues)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Compares a pre-existing attribute value in the ldap directory.  Most ChaiProvider implementations
     * will do this using the ldap <i>COMPARE</i> command, so the check is done on the server side.
     *
     * @param attributeName  A valid attribute for the entry
     * @param attributeValue A string value to be tested against the ldap entry
     * @return true if the value exists in the ldap directory
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#compareStringAttribute(String,String,String)
     */
    boolean compareStringAttribute(String attributeName, String attributeValue)
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Deletes a value from the ldap directory.
     *
     * @param attributeName  The name of the attribute to delete from
     * @param attributeValue A particular value to delete, or null for all values
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#deleteStringAttributeValue(String,String,String)
     */
    void deleteAttribute(String attributeName, String attributeValue)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Gets the underlying ChaiProvider instance this ChaiEntry is using.
     *
     * @return the underlying ChaiProvider instance
     */
    ChaiProvider getChaiProvider();

    /**
     * Finds any decendents of this entry in the ldap heirarchy
     *
     * @return A set of all decendents.  If no decendants are found, an empty set is returned.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    Set<ChaiEntry> getChildObjects()
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Gets the ldap DN of the object this ChaiEntry instance is representing
     *
     * @return DN of the object.
     */
    String getEntryDN();

    /**
     * Finds the parent entry of this instance's entry in the directory.
     *
     * @return An ChaiEntryImpl representing the parent entry.  If there is no parent, then null is returned.
     */
    ChaiEntry getParentEntry()
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Indicates if the instance of this object actually references a live object in the directory.  If any error
     * happens during this operation (such as a ChaiUnavailableException is thrown), the error is swallowed and
     * false is returned.
     *
     * @return true if the object exists in the directory.
     */
    boolean isValid();

    /**
     * A convience method for reading boolean values from the ldap directory.  Note that ldap booleans actually
     * have three states: true, false and does not exist.  This method uses the general convention that does not
     * exist is treated as false.
     *
     * @param attributeName The name of the attribute
     * @return true if value is "true", false if it is any other value or does not exist.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    boolean readBooleanAttribute(String attributeName)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Read the canonical distinguish name (DN). This
     * method will issue an LDAP search operation to retrieve the DN value of the the entry
     * as defined by the LDAP server.  The returned value may differ from {@link ChaiEntry#getEntryDN()}
     * in terms of case, spacing, or other syntax differences, however they should resolve to the
     * same entry by the ldap server.
     * <p/>
     * Canonical values are particularly useful when storing in a collection, as you
     * can gaurentee that two
     *
     * @return The canonical DN of the object, as returned by the LDAP server
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    String readCanonicalDN()
            throws ChaiOperationException, ChaiUnavailableException;

   /**
    * Read an attribute with timestamp value.  Automatically converts the timestamp to a {@code Date} object.
    * If there are any problems with the conversion, null is returned.
    *
    * @param attributeName Name of the attribute to read.  Date must be in Zulu string format.
    * @return A valid {@code Date} object, or null if a date can not be determined.
    * @throws ChaiOperationException   If there is an error during the operation
    * @throws ChaiUnavailableException If the directory server(s) are unavailable
    */
    Date readDateAttribute(String attributeName)
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * A convience method for reading int values.
     *
     * @param attributeName The name of the attribute.
     * @return int value of attribute, or zero if it does not exist or is not numeric
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    int readIntAttribute(String attributeName)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Performs a read operation against the directory and returns the binary results.
     * Useful only for ldap attributes using binary syntax type.
     *
     * @param attributeName A valid attribute on the object.
     * @return A byte array where the first dimension is each ldap value, and the second diminsion
     *         is the actual byte values.
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @see com.novell.ldapchai.provider.ChaiProvider#readMultiByteAttribute(String,String)
     */
    byte[][] readMultiByteAttribute(String attributeName)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Retreives an attribute value from the LDAP entry represented by the
     * instance of this class.  If the attribute syntax is not a string, the value will be
     * converted to a string.
     * <p/>
     * Callers of this method should use the values specified in the {@link ChaiConstant} class
     * when possible.
     *
     * @param attributeName The name of the atttribute
     * @return Values of the attribute selected.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    Set<String> readMultiStringAttribute(String attributeName)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Read an attribute with network address value.  Reads attributes with network address syntax, and
     * returns any IP addresses found there.  Addresses of other types are ignored.
     *
     * @param attributeName Name of the attribute to read.  Date must be in Zulu string format.
     * @return A valid {@code Date} object, or null if a date can not be determined.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    List<InetAddress> readNetAddressAttribute(String attributeName)
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Convienence method to read this ChaiEntry instance's {@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_OBJECTCLASS} attribute.
     *
     * @return The value(s) of the attribute, or null if no value
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    Set<String> readObjectClass()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Retreives an attribute value from the LDAP entry represented by the
     * instance of this class.  If the attribute has multiple values then only a single value
     * is returned.  If the attribute does not have a string syntax, the value will be converted
     * to a string.
     * <p/>
     * Callers of this method should use the values specified in the {@link ChaiConstant} class
     * when possible.
     *
     * @param attributeName The name of the attribute
     * @return Value of the attributeName attribute
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#readStringAttribute(String,String)
     */
    String readStringAttribute(String attributeName)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Read an array of specified attributes.  If any of the attributes has multiple values, only the first value
     * returned by the directory is returned.  If the attribute does not have a string syntax, the value will be converted
     * to a string.
     * <p/>
     * Callers of this method are encouraged to use the values specified in {@link ChaiConstant} when possible for attribute names.
     *
     * @param attributes Valid  attributes on the object.
     * @return A map containing the result of the read
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see #readStringAttribute(String)
     * @see com.novell.ldapchai.provider.ChaiProvider#readStringAttributes(String, java.util.Set)
     */
    Map<String,String> readStringAttributes(Set<String> attributes)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Replace an existing an attriubte value with a new value.  This operation is useful for manipulating
     * multi-valued attributes, and is less expensive then first deleting an existing value and then adding
     * a new value.
     *
     * @param attributeName The name of the attribute
     * @param oldValue      The old value
     * @param newValue      The new value
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#replaceStringAttribute(String,String,String,String)
     */
    void replaceAttribute(String attributeName, String oldValue, String newValue)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Perform an ldap search using this entry as the search root.  A subtree search is implied, and no attribute values
     * are requested.  The result is a collection of {@code ChaiEntry} instances that can then be further minipulated.
     *
     * @param filter A valid RFC2254 ldap search filter
     * @return Set<ChaiEntry>           ChaiEntry objects that mean the search criteria
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#search(String, com.novell.ldapchai.util.SearchHelper)
     * @see com.novell.ldapchai.provider.ChaiProvider#searchMultiValues(String, com.novell.ldapchai.util.SearchHelper)
     */
    Set<ChaiEntry> search(String filter)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Perform an ldap search using this entry as the search root.  A {@link com.novell.ldapchai.util.SearchHelper} is required
     * and its values are used to perform the search.  The result is a collection of {@code ChaiEntry} instances that
     * can then be further minipulated.
     * <p/>
     * Note, although the {@code SearchHelper} can specify return attributes, the attributes are not requested using
     * this search method.  Instead, the result ChaiEntries can be used to read attribute values.
     *
     * @param searchHelper A search helper instance
     * @return Set<ChaiEntry>           ChaiEntry objects that mean the search criteria
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#search(String, com.novell.ldapchai.util.SearchHelper)
     * @see com.novell.ldapchai.provider.ChaiProvider#searchMultiValues(String, com.novell.ldapchai.util.SearchHelper)
     */
    Set<ChaiEntry> search(SearchHelper searchHelper)
            throws ChaiOperationException, ChaiUnavailableException;


    /**
     * Perform an ldap search using this entry as the search root.  No attribute values
     * are requested.  The result is a collection of {@code ChaiEntry} instances that can then be further minipulated.
     *
     * @param filter      A valid RFC2254 ldap search filter
     * @param searchScope The scope to use during the search operation
     * @return Set<ChaiEntry>           ChaiEntry objects that mean the search criteria
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#search(String, com.novell.ldapchai.util.SearchHelper)
     * @see com.novell.ldapchai.provider.ChaiProvider#searchMultiValues(String, com.novell.ldapchai.util.SearchHelper)
     */
    Set<ChaiEntry> search(String filter, ChaiProvider.SEARCH_SCOPE searchScope)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Removes all existing values, if any, and sets the new value.
     *
     * @param attributeName  Name of the attribute
     * @param attributeValue New value for the attribute
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#writeStringAttribute(String, String, java.util.List, boolean)
     */
    void writeStringAttribute(String attributeName, String attributeValue)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Removes all existing values, if any, and sets the new values
     *
     * @param attributeName   Name of the attribute
     * @param attributeValues New values for the attribute
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#writeStringAttribute(String, String, java.util.List, boolean)
     */
    void writeStringAttribute(String attributeName, Set<String> attributeValues)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Write an attribute with timestamp value.  Automatically converts the {@code Date} object to a timestamp.
     *
     * @param attributeName Name of the attribute to read.  Date must be in Zulu string format.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    public void writeDateAttribute(final String attributeName, final Date date)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Removes all existing values, if any, and sets the new value.
     *
     * @param attributeValueProps  Name of the attributes and values to set on the entry.  Any existing values will
     * be replaced.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#writeStringAttributes(String, java.util.Map, boolean)
     */
    void writeStringAttributes(Map<String,String> attributeValueProps)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Removes all existing values, if any, and sets the new values.
     *
     * @param attributeName   Name of the attribute
     * @param attributeValues New values for the attribute
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    void writeStringAttribute(String attributeName, String... attributeValues)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Removes all existing values, if any, and sets the new values.
     *
     * @param attributeName   Name of the attribute
     * @param attributeValues New values for the attribute
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    void writeBinaryAttribute(final String attributeName, final byte[]... attributeValues)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Replace an existing an attriubte value with a new value.  This operation is useful for manipulating
     * multi-valued attributes, and is less expensive then first deleting an existing value and then adding
     * a new value.
     *
     * @param attributeName The name of the attribute
     * @param oldValue      The old value
     * @param newValue      The new value
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see com.novell.ldapchai.provider.ChaiProvider#replaceStringAttribute(String,String,String,String)
     */
    void replaceBinaryAttribute(final String attributeName, final byte[] oldValue, final byte[] newValue)
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Read a GUID (Globally Unique Identifier) from the entry.  Chai will attempt to use vendor-specific
     * attributes for the entry.  Many vendor implementations store GUID values as binary, or octet-length
     * syntax so this method will automatically convert to a string format of some type, typically a hex or base64 string.
     *
     * Thus, the value returned by this method can be assumed to be unique per entry and reliably read across
     * time, however the value may not be easily reversable to the original vendor format.
     *
     * @return Value of the entry's vendor-specific GUID format in some type of string format, or null if not available
     *
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws UnsupportedOperationException If the vendor implementation does not have a GUID
     */
    String readGUID()
            throws ChaiOperationException, ChaiUnavailableException;
}

