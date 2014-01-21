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

package com.novell.ldapchai.provider;

import com.novell.ldapchai.ChaiRequestControl;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.impl.ad.ADErrorMap;
import com.novell.ldapchai.impl.edir.EdirErrorMap;
import com.novell.ldapchai.util.SearchHelper;

import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code ChaiProvider} is the foundation interface for the LDAP Chai API.  {@code ChaiProvider} provides
 * methods to access an ldap directory.
 * <p/>
 * Use the {@link ChaiProviderFactory} factory to obtain a {@code ChaiProvider} instance.
 * <p/>
 * {@code ChaiProvider}s can be used directly for raw ldap access. However, it is generally desirable to use the
 * {@link com.novell.ldapchai.ChaiEntry} wrappers instead.  Using a {@code ChaiProvider} requires the caller
 * to keep track of ldap distinguished names (DN) and be aware of context and other ldap concepts.
 * <p/>
 * It is helpful to think of a {@code ChaiProvider} as a logical connection to the ldap server.
 * Implementations may provide some type of pooling, or an instance of {@code ChaiProvider} may actually
 * represent a single physical connection to the server.
 * <p/>
 * {@code ChaiProvider} does not support any notion of asynchronous or non-blocking requests.  Every method call
 * will block until a result or error is returned from the server, or some other type of Exception
 * occurs.  
 * <p/>
 * The underlying implementations of this interface may use a variety of strategies for actually reaching the ldap directory,
 * including the standard JNDI interface {@link javax.naming.directory} , and Novell's JLDAP API.  Different implementations may or may not provide support
 * for server fail-over or other failure recovery.
 * <p/>
 * {@code ChaiProvider} implementations are <i>not</i> guarenteed to be thread safe by LDAP Chai.  Individual implementations
 * may provide thread safety.  Check with the implementation before sharing an {@code ChaiProvider} instance accross multiple
 * threads.  For a guaranteed thread safe ChaiProvider, use {@link com.novell.ldapchai.provider.ChaiProviderFactory#synchronizedProvider(ChaiProvider)}.
 * <p/>
 * To prevent leaks the {@link #close()} method should be called when a {@code ChaiProvider} instance is no longer
 * used.  Once closed, any operation annotated with {@link com.novell.ldapchai.provider.ChaiProviderImplementor.LdapOperation}  will throw an {@link IllegalStateException}.
 *
 * @author Jason D. Rivard                                                          b
 * @see com.novell.ldapchai.ChaiEntry
 */
public interface ChaiProvider {
// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    /**
     * LDAP search scope of BASE, ONE or SUBTREE.
     */
    public static enum SEARCH_SCOPE {
        /**
         * Search the container below the specified context, but not any children of the
         */
        ONE(javax.naming.directory.SearchControls.ONELEVEL_SCOPE),
        /**
         * Search the specified object, but not any descendants
         */
        BASE(javax.naming.directory.SearchControls.OBJECT_SCOPE),

        /**
         * Search the descendants below the specified context, and all lower descendants
         */
        SUBTREE(javax.naming.directory.SearchControls.SUBTREE_SCOPE);

        private final int jndiScopeInt;

        private SEARCH_SCOPE(final int jndiScopeInt)
        {
            this.jndiScopeInt = jndiScopeInt;
        }

        /**
         * Get the JNDI equivalent constant.
         *
         * @return the equivalent JNDI {@link javax.naming.directory.SearchControls} scope constant.
         */
        public int getJndiScopeInt()
        {
            return jndiScopeInt;
        }
    }

    public static enum DIRECTORY_VENDOR {
        GENERIC,
        NOVELL_EDIRECTORY,
        MICROSOFT_ACTIVE_DIRECTORY,
        OPEN_LDAP,
        DIRECTORY_SERVER_389,
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Close the connection to ldap.  All other operational methods (those that are marked with
     * {@code com.novell.ldapchai.provider.ChaiProviderImplementor.LdapOperation})
     * should throw an exception if called after this method.
     */
    public void close();

    /**
     * Compares the value of a string to an ldap entry's specified attribute.  Implementers
     * of this method will actually perform an COMPARE operation, as opposed to doing a java .equals()
     * style compare.
     *
     * @param entryDN   A valid entryDN
     * @param attributeName The attribute of the object DN to do the comparison to.
     * @param value     The value to compare against the value in the directory
     * @return true if the value comparison is true, false otherwise
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#compareStringAttribute(String, String)
     */
    @ChaiProviderImplementor.LdapOperation
    public boolean compareStringAttribute(String entryDN, String attributeName, String value)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Create a new entry in the directory
     *
     * @param entryDN          A valid entryDN
     * @param baseObjectClass  The base class of the entry (objectClass)
     * @param stringAttributes a Map object containing
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void createEntry(String entryDN, String baseObjectClass, Map<String,String> stringAttributes)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Create a new entry in the directory
     *
     * @param entryDN          A valid entryDN
     * @param baseObjectClasses The base classes of the entry (objectClass)
     * @param stringAttributes a Map object containing
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void createEntry(String entryDN, Set<String> baseObjectClasses, Map<String,String> stringAttributes)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Delete the specified entry
     *
     * @param entryDN A valid entryDN
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void deleteEntry(String entryDN)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Delete the string values of the specifed objects attributes.
     *
     * @param entryDN   A valid entryDN
     * @param attributeName A valid attribute of the entryDN
     * @param value     The value to delete
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#deleteAttribute(String, String)
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void deleteStringAttributeValue(String entryDN, String attributeName, String value)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Performs an extended operation against the server.  The extended operation must be understood by the server.
     *
     * @param request An ExtendedRequest bean that can be
     * @return An ExtendedResponse created in response to the request.
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see ExtendedRequest
     * @see ExtendedResponse
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public ExtendedResponse extendedOperation(ExtendedRequest request)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;


    /**
     * Get the configruation object used with this provider.
     *
     * @return A locked copy of the working {@code ChaiConfiguration}
     */
    public ChaiConfiguration getChaiConfiguration();


    /**
     * Retreive the statistics of the provider.
     *
     * @return a bean containing statistics of the provider, or null if statistics tracking is not enabled.
     * @see ChaiSetting#STATISTICS_ENABLE
     */
    public ProviderStatistics getProviderStatistics();


    /**
     * Performs a read operation against the directory and returns the binary results.
     * Useful only for ldap attributes using binary syntax type.
     *
     * @param entryDN   A valid object
     * @param attribute A valid attribute on the object.
     * @return A byte array where the first dimension is each ldap value, and the second diminsion
     *         is the actual byte values.
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#readMultiByteAttribute(String)
     */
    @ChaiProviderImplementor.LdapOperation
    public byte[][] readMultiByteAttribute(String entryDN, String attribute)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Read all string values of the specified attribute.
     *
     * @param entryDN   The full DN of the object to read
     * @param attribute A valid attribute on the object
     * @return An array of all values of the attribute, or null if there are no values for the attribute
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#readMultiStringAttribute(String)
     */
    @ChaiProviderImplementor.LdapOperation
    public Set<String> readMultiStringAttribute(String entryDN, String attribute)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Read a single string value of the specifed attribute.  If the attribute has multiple values, only the first
     * value returned by the directory is returned.
     *
     * @param entryDN   The full DN of the object to read
     * @param attribute A valid attribute on the object
     * @return The value of the attribute, or null if there are no values for the attribute
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#readStringAttribute(String)
     */
    @ChaiProviderImplementor.LdapOperation
    public String readStringAttribute(String entryDN, String attribute)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;


    /**
     * Read an array of specified attributes.  If any of the attributes has multiple values, only the first value
     * returned by the directory is returned.
     *
     * @param entryDN    The full DN of the object to read
     * @param attributes An array of valid attributes on the object.
     * @return attributes A Map where the keys are the specifed attributes, and the value is a string containing the value or null if no value is found
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#readStringAttributes(java.util.Set<java.lang.String>)
     */
    @ChaiProviderImplementor.LdapOperation
    public Map<String,String> readStringAttributes(String entryDN, Set<String> attributes)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Replace an existing value for the specifed attribute.
     *
     * @param entryDN       A valid entryDN
     * @param attributeName A valid attribute of the entryDN
     * @param oldValue      The value to be replaced
     * @param newValue      The new value to add
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#replaceAttribute(String, String, String)
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void replaceStringAttribute(String entryDN, String attributeName, String oldValue, String newValue)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Performs a search against the directory for the objects, given the specified basedn and {@link SearchHelper}.
     * A subtree search is implied.  Attribute values are returned according to the attributes specifed in
     * the {@code SearchHelper}.
     *
     * @param baseDN       A valid object DN for the top of the search, or an empty string if the whole ldap namespace is to be searched
     * @param searchHelper A Chai searchHelper
     * @return A Map containing strings of entryDNs as keys, and values of Properties objects.  Within each properties object the key
     *         is a specified attribute name and its associated value.
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#search(String)
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.SearchOperation
    Map<String, Map<String,String>> search(String baseDN, SearchHelper searchHelper)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;


    /**
     * Performs a search against the directory for the objects, given the specified baseDN and filter.  Additionally, the specified
     * attributes are returned for any matches.  Only the a single value of each attribute is returned.
     *
     * @param baseDN      A valid object DN for the top of the search, or an empty string if the whole ldap namespace is to be searched
     * @param filter      A valid ldap search filter
     * @param attributes  An array containing the desired attributes to be returned.  Set to null for all attributes, set to an empty String[] for no attributes
     * @param searchScope Scope of the search
     * @return A Map containing strings of entryDNs as keys, and values of Map objects.  Within each map object the key
     *         is a specified attribute name and its associated value.
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#search(String)
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.SearchOperation
    Map<String, Map<String,String>> search(String baseDN, String filter, Set<String> attributes, SEARCH_SCOPE searchScope)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;


    /**
     * Perform a search where multiple values of an object are returned.  Care should be taken to avoid very large
     * search results, as the entire result set is held in memory before being returned to the caller.
     *
     * @param baseDN       A valid entryDN
     * @param searchHelper A Chai searchHelper
     * @return A map containing Strings of DNs for keys, and a Map for values.  The value map itself contains attribute name Strings as keys, and a List of values for each attribute.
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#search(String)
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.SearchOperation
    public Map<String, Map<String, List<String>>> searchMultiValues(String baseDN, SearchHelper searchHelper)
            throws ChaiUnavailableException, ChaiOperationException;


    /**
     * Perform a search where multiple values of an object are returned.  Care should be taken to avoid very large
     * search results, as the entire result set is held in memory before being returned to the caller.
     *
     * @param baseDN      A valid entryDN
     * @param filter      Search filter
     * @param attributes  attribugtes to return.  Null indicates all values, An empty array will return no values.
     * @param searchScope The LDAP scope of the search
     * @return A map containing Strings of DNs for keys, and a Map for values.  The value map itself contains attribute name Strings as keys, and a List of values for each attribute.
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#search(String)
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.SearchOperation
    public Map<String, Map<String, List<String>>> searchMultiValues(final String baseDN, final String filter, final Set<String> attributes, SEARCH_SCOPE searchScope)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Write the binary values to the specified object's specified attribute.
     *
     * @param entryDN       A valid entryDN
     * @param attributeName A valid attribute of the entryDN
     * @param values        An array of values to add
     * @param overwrite     Overwrite existing values
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#writeStringAttribute(String, String)
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    void writeBinaryAttribute(String entryDN, String attributeName, byte[][] values, boolean overwrite)
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Write the binary values to the specified object's specified attribute.
     *
     * @param entryDN       A valid entryDN
     * @param attributeName A valid attribute of the entryDN
     * @param values        An array of values to add
     * @param overwrite     Overwrite existing values
     * @param controls      additional request directives to the ldap server
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#writeStringAttribute(String, String)
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    void writeBinaryAttribute(String entryDN, String attributeName, byte[][] values, boolean overwrite, ChaiRequestControl[] controls)
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Write the string values to the specified object's specified attribute.
     *
     * @param entryDN   A valid entryDN
     * @param attributeName A valid attribute of the entryDN
     * @param values    An array of values to add
     * @param overwrite Overwrite existing values
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     * @see com.novell.ldapchai.ChaiEntry#writeStringAttribute(String, String)
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void writeStringAttribute(String entryDN, String attributeName, Set<String> values, boolean overwrite)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Write multiple string values to the specified object's specified attributes.
     *
     * @param entryDN   A valid entryDN
     * @param attributeValueProps A map of attribute names and values
     * @param overwrite Overwrite existing values
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void writeStringAttributes(String entryDN, Map<String,String> attributeValueProps, boolean overwrite)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException;

    /**
     * Discovers and returns the Chai API's interpretation of the vendor of the configured directory.
     * {@code ChaiProvider} implementations will typically cache this value for the life of the instance.
     *  
     * @return The Chai API's interpretation of the vendor of the configured directory.
     * @see com.novell.ldapchai.util.ChaiUtility#determineDirectoryVendor(com.novell.ldapchai.ChaiEntry)
     * @see com.novell.ldapchai.provider.ChaiSetting#DEFAULT_VENDOR
     * @throws ChaiUnavailableException If no directory servers are reachable
     */
    @ChaiProviderImplementor.LdapOperation
    public DIRECTORY_VENDOR getDirectoryVendor()
            throws ChaiUnavailableException;

    /**
     * Replace binary value of the specified object's specified attribute.
     *
     * @param entryDN       A valid entryDN
     * @param attributeName A valid attribute of the entryDN
     * @param oldValue      A existing value to replace (the value must pre-exist in the directory)
     * @param newValue      New value to write to the directory
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     */
    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    void replaceBinaryAttribute(String entryDN, String attributeName, byte[] oldValue, byte[] newValue)
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Indicates if the provider has a current, active connection to the ldap directory.
     * @return true if the connection has a valid, active connection to the ldap directory.
     */
    boolean isConnected();
}

