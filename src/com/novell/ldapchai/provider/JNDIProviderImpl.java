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

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.SearchHelper;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Default {@code ChaiProvider} implementation; wraps the standard JNDI ldap API.  Runs in a standard Java SE 1.5 (or greater) environment.  Supports
 * fail-over to multiple servers.  It does not however, support load balancing.
 * <p/>
 * This implementation will use the list of servers in the {@code ChaiConfiguration} in order, meaning that
 * all requests will go the the first server in the configured list as long as it is available.  If that server fails it
 * will go to the next in the list until it finds an available server.  Afterwords it will periodicaly retry the servers
 * at the top of the list to see if they have returned to life.
 * <p/>
 * Instances can be obtained using {@link com.novell.ldapchai.provider.ChaiProviderFactory}.
 * <p/>
 * During initialization, {@link com.novell.ldapchai.provider.ChaiConfiguration#getImplementationConfiguration()} is called, and if a Hashtable is
 * found, the settings within are applied during the construction of the underlying {@link javax.naming.ldap.LdapContext}
 * instance(s) used by {@code JNDIProviderImpl}.
 *
 * @author Jason D. Rivard
 */
public class JNDIProviderImpl extends AbstractProvider implements ChaiProviderImplementor {
// ----------------------------- CONSTANTS ----------------------------

    /**
     * The default initial pool size to create when communicating with an individual server. *
     */
    public static final int DEFAULT_INITIAL_POOL_SIZE = 1;

    /**
     * The default preferred pool size to create when communicating with an individual server. *
     */
    public static final int DEFAULT_PREFERRED_POOL_SIZE = 10;

    /**
     * The default maximum pool size to create when communicating with an individual server. *
     */
    public static final int DEFAULT_MAXIMUM_POOL_SIZE = 50;

// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(JNDIProviderImpl.class);

    private LdapContext jndiConnection;


// -------------------------- STATIC METHODS --------------------------

    private static LdapContext generateNewJndiContext(final Hashtable environment)
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String url = String.valueOf(environment.get(Context.PROVIDER_URL));
        final String bindDN = String.valueOf(environment.get(Context.SECURITY_PRINCIPAL));

        try {
            final long startTime = System.currentTimeMillis();
            final LdapContext newDirContext;
            newDirContext = new InitialLdapContext(environment, null);
            LOGGER.trace("bind successful as " + bindDN + " (" + (System.currentTimeMillis() - startTime) + "ms)");
            return newDirContext;
        } catch (NamingException e) {
            final StringBuilder logMsg = new StringBuilder();
            logMsg.append("unable to bind to ");
            logMsg.append(url);
            logMsg.append(" as ");
            logMsg.append(bindDN);
            logMsg.append(" reason: ");
            if (e instanceof CommunicationException) {
                logMsg.append("CommunicationException (").append(e.getMessage());
                final Throwable rootCause = e.getRootCause();
                if (rootCause != null) {
                    logMsg.append("; ").append(rootCause.getMessage());
                }
                logMsg.append(")");
                throw new ChaiUnavailableException(logMsg.toString(), ChaiError.COMMUNICATION, false, true);
            } else {
                logMsg.append(e.getMessage());

                //check for bad password or intruder detection
                throw ChaiUnavailableException.forErrorMessage(logMsg.toString());
            }
        }
    }

    /**
     * <p>Converts an array of primitive bytes to objects.</p>
     * <p/>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     * <p/>
     * <p>From Jakarta Commons project</p>
     *
     * @param array a <code>byte</code> array
     * @return a <code>Byte</code> array, <code>null</code> if null array input
     */
    private static Byte[] toObject(final byte[] array)
    {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return new Byte[0];
        }
        final Byte[] result = new Byte[array.length];
        int i = 0;
        while (i < array.length) {
            result[i] = array[i];
            i++;
        }
        return result;
    }

    /**
     * <p>Converts an array of object Bytes to primitives.</p>
     * <p/>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     * <p/>
     * <p>From Jakarta Commons project</p>
     *
     * @param array a <code>Byte</code> array, may be <code>null</code>
     * @return a <code>byte</code> array, <code>null</code> if null array input
     * @throws NullPointerException if array content is <code>null</code>
     */
    private static byte[] toPrimitive(final Byte[] array)
    {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return new byte[0];
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    JNDIProviderImpl()
    {
        super();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ChaiProvider ---------------------


    public void close()
    {
        super.close();
        if (jndiConnection != null) {
            try {
                jndiConnection.close();
            } catch (Exception e) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.warn("unexpected error during jndi connection close " + e.getMessage(), e);
                } else {
                    LOGGER.warn("unexpected error during jndi connection close " + e.getMessage());
                }
            } finally {
                jndiConnection = null;
            }
        }
    }

    @LdapOperation
    public final boolean compareStringAttribute(final String entryDN, final String attributeName, final String value)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.compareStringAttribute(entryDN, attributeName, value);

        final byte[] ba;
        try {
            ba = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }

        // Set up the search controls
        final SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(new String[0]);       // Return no attrs
        ctls.setSearchScope(SearchControls.OBJECT_SCOPE); // Search object only

        final LdapContext ldapConnection = getLdapConnection();
        NamingEnumeration<SearchResult> answer = null;
        boolean result = false;
        try {
            answer = ldapConnection.search(addJndiEscape(entryDN), "(" + attributeName + "={0})", new Object[]{ba}, ctls);
            result = answer.hasMore();
        } catch (NamingException e) {
            convertNamingException(e);
        } finally {
            if (answer != null) {
                try {
                    answer.close();
                } catch (Exception e) { /* action not required */ }
            }
        }

        return result;
    }

    @LdapOperation
    @ModifyOperation
    public final void createEntry(final String entryDN, final String baseObjectClass, final Map<String,String> stringAttributes)
            throws ChaiUnavailableException, ChaiOperationException
    {
        INPUT_VALIDATOR.createEntry(entryDN, baseObjectClass, stringAttributes);
        this.createEntry(entryDN, Collections.singleton(baseObjectClass),stringAttributes);
    }

    @LdapOperation
    @ModifyOperation
    public final void createEntry(final String entryDN, final Set<String> baseObjectClasses, final Map<String,String> stringAttributes)
            throws ChaiOperationException, ChaiUnavailableException
    {
        activityPreCheck();
        INPUT_VALIDATOR.createEntry(entryDN, baseObjectClasses, stringAttributes);

        final Attributes attrs = new BasicAttributes();

        //Put in the base object class an attribute
        final BasicAttribute objectClassAttr = new BasicAttribute(ChaiConstant.ATTR_LDAP_OBJECTCLASS);
        for (final String loopClass : baseObjectClasses) {
            objectClassAttr.add(loopClass);
        }
        attrs.put(objectClassAttr);

        //Add each of the attributes required.
        for (final String key : stringAttributes.keySet()) {
            attrs.put(key, stringAttributes.get(key));
        }

        // Create the object.
        final DirContext ldapConnection = getLdapConnection();
        try {
            ldapConnection.createSubcontext(addJndiEscape(entryDN), attrs);
        } catch (NamingException e) {
            convertNamingException(e);
        }
    }

    @LdapOperation
    @ModifyOperation
    public final void deleteEntry(final String entryDN)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.deleteEntry(entryDN);

        final LdapContext ldapConnection = getLdapConnection();
        try {
            ldapConnection.destroySubcontext(addJndiEscape(entryDN));
        } catch (NamingException e) {
            convertNamingException(e);
        }
    }

    @LdapOperation
    @ModifyOperation
    public final void deleteStringAttributeValue(final String entryDN, final String attributeName, final String attributeValue)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.deleteStringAttributeValue(entryDN, attributeName, attributeValue);

        // Create a BasicAttribute for the object.
        final BasicAttribute attributeToReplace = new BasicAttribute(attributeName, attributeValue);

        // Create the ModificationItem
        final ModificationItem[] modificationItem = new ModificationItem[1];

        // Populate the ModificationItem object with the flag & the attribute to replace.
        modificationItem[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attributeToReplace);

        // Modify the Attributes.
        final LdapContext ldapConnection = getLdapConnection();
        try {
            ldapConnection.modifyAttributes(addJndiEscape(entryDN), modificationItem);
        } catch (NamingException e) {
            convertNamingException(e);
        }
    }

    @LdapOperation
    @ModifyOperation
    public final ExtendedResponse extendedOperation(final ExtendedRequest request)
            throws ChaiUnavailableException, ChaiOperationException

    {
        activityPreCheck();

        INPUT_VALIDATOR.extendedOperation(request);

        preCheckExtendedOperation(request);

        final LdapContext ldapConnection = getLdapConnection();
        try {
            return ldapConnection.extendedOperation(request);
        } catch (NamingException e) {
            cacheExtendedOperationException(request, e);
            convertNamingException(e); // guarenteed to throw ChaiException
        }
        return null;  // can't actually be reached
    }


    public ChaiConfiguration getChaiConfiguration()
    {
        return chaiConfig;
    }

    public ProviderStatistics getProviderStatistics()
    {
        return null;
    }

    @LdapOperation
    public final byte[][] readMultiByteAttribute(final String entryDN, final String attributeName)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.readMultiByteAttribute(entryDN, attributeName);

        final List<Byte[]> returnValues = new ArrayList<Byte[]>();
        final String jndiBinarySetting = "java.naming.ldap.attributes.binary";
        NamingEnumeration<?> namingEnum = null;

        // Get only the Attribute that is passed in.
        final String[] attributesArray = {attributeName};

        try {
            final LdapContext ldapConnection = (LdapContext)getLdapConnection().lookup("");

            // inform jndi the attribute is binary.
            ldapConnection.addToEnvironment(jndiBinarySetting, attributeName);

            // Get the Enumeration of attribute values.
            namingEnum = ldapConnection.getAttributes(addJndiEscape(entryDN), attributesArray).get(attributeName).getAll();
            while (namingEnum.hasMore()) {
                final Object value = namingEnum.next();

                if (value instanceof byte[]) {
                    final Byte[] objectValue = toObject((byte[])value);
                    returnValues.add(objectValue);
                }
            }

            // Return the list as a set of primimtives.
            final byte[][] returnArray = new byte[returnValues.size()][];
            for (int i = 0; i < returnValues.size(); i++) {
                returnArray[i] = toPrimitive(returnValues.get(i));
            }

            return returnArray;
        } catch (NullPointerException e) {
            return new byte[0][0];
        } catch (NamingException e) {
            convertNamingException(e);
            return null;
        } finally {
            // close the enumeration
            try {
                namingEnum.close();
            } catch (Exception e) {
                //doesnt matter
            }
        }
    }

    @LdapOperation
    public final Set<String> readMultiStringAttribute(final String entryDN, final String attributeName)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.readMultiStringAttribute(entryDN, attributeName);

        final Set<String> attributeValues = new HashSet<String>();
        NamingEnumeration namingEnum = null;

        try {
            // Get only the Attribute that is passed in.
            final String[] attributesArray = {attributeName};

            // Get the Enumeration of attribute values.
            final LdapContext ldapConnection = getLdapConnection();

            namingEnum = ldapConnection.getAttributes(addJndiEscape(entryDN), attributesArray).get(attributeName).getAll();
            while (namingEnum.hasMore()) {
                attributeValues.add(namingEnum.next().toString());
            }

            // Return the list as an array.
            return attributeValues;
        } catch (NullPointerException e) {
            // to be consistent with nps impl.
            return Collections.emptySet();
        } catch (NamingException e) {
            convertNamingException(e);
            return null;
        } finally {
            try {
                namingEnum.close();
            } catch (Exception e) {
                // nothing to do
            }
        }
    }

    @LdapOperation
    public final String readStringAttribute(final String entryDN, final String attributeName)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.readStringAttribute(entryDN, attributeName);

        return readStringAttributes(entryDN, Collections.singleton(attributeName)).get(attributeName);
    }

    @LdapOperation
    public final Map<String,String> readStringAttributes(final String entryDN, final Set<String> attributes)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.readStringAttributes(entryDN, attributes);

        // Allocate a return object
        final Map<String,String> returnObj = new LinkedHashMap<String,String>();

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Get only the Attribute that is passed in.
        final Attributes returnedAttribs;

        NamingEnumeration attrEnumeration = null;

        try {
            if (attributes == null || attributes.isEmpty()) {
                returnedAttribs = ldapConnection.getAttributes(addJndiEscape(entryDN), null);
                attrEnumeration = returnedAttribs.getAll();
                while (attrEnumeration.hasMoreElements()) {
                    final Attribute attribute = (Attribute) attrEnumeration.nextElement();

                    // Put an entry in the map, if there are no values insert null, otherwise, insert the first value
                    if (attribute != null) {
                        returnObj.put(attribute.getID(), attribute.get().toString());
                    }
                }
            } else { // Loop through each requested attribute
                returnedAttribs = ldapConnection.getAttributes(addJndiEscape(entryDN), attributes.toArray(new String[attributes.size()]));
                for (final String loopAttr : attributes) {
                    // Ask JNDI for the attribute (which actually includes all the values)
                    final Attribute attribute = returnedAttribs.get(loopAttr);

                    // Put an entry in the map, if there are no values insert null, otherwise, insert the first value
                    if (attribute != null) {
                        returnObj.put(loopAttr, attribute.get().toString());
                    }
                }
            }
        } catch (NamingException e) {
            convertNamingException(e);
            return null;
        } finally {
            if (attrEnumeration != null) {
                try {
                    attrEnumeration.close();
                } catch (NamingException e) {
                    // nothing to do
                }
            }
        }
        return returnObj;
    }

    @LdapOperation
    @ModifyOperation
    public final void replaceStringAttribute(final String entryDN, final String attributeName, final String oldValue, final String newValue)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.replaceStringAttribute(entryDN, attributeName, oldValue, newValue);

        // Create the ModificationItem
        final ModificationItem[] mods = new ModificationItem[2];

        // Mark the flag to remover the existing attribute.
        mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(attributeName, oldValue));

        // Mark the flag to add the new attribute
        mods[1] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(attributeName, newValue));

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Modify the Attributes.
        try {
            ldapConnection.modifyAttributes(addJndiEscape(entryDN), mods);
        } catch (NamingException e) {
            convertNamingException(e);
        }
    }

    @LdapOperation
    public final Map<String, Map<String,String>> search(final String baseDN, final SearchHelper searchHelper)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.search(baseDN, searchHelper);

        // perform search
        final Map<String, Map<String, List<String>>> results = this.searchImplementation(baseDN, searchHelper, false);

        // convert to <String, Properties> return set.
        final Map<String, Map<String,String>> returnMap = new HashMap<String, Map<String,String>>(results.size());
        for (final String entryDN : results.keySet()) {
            final Map<String, List<String>> attributeMap = results.get(entryDN);
            final Map<String, String> newProps = new LinkedHashMap<String, String>();
            for (final String attrName : attributeMap.keySet()) {
                final List<String> values = attributeMap.get(attrName);
                newProps.put(attrName, values.get(0));
            }
            returnMap.put(entryDN, Collections.unmodifiableMap(newProps));
        }
        return Collections.unmodifiableMap(returnMap);
    }

    @LdapOperation
    public final Map<String, Map<String,String>> search(final String baseDN, final String filter, final Set<String> attributes, final SEARCH_SCOPE searchScope)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.search(baseDN, filter, attributes, searchScope);

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter(filter);
        searchHelper.setAttributes(attributes);
        searchHelper.setSearchScope(searchScope);

        return this.search(baseDN, searchHelper);
    }

    public final Map<String, Map<String, List<String>>> searchMultiValues(final String baseDN, final SearchHelper searchHelper)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.searchMultiValues(baseDN, searchHelper);

        return this.searchImplementation(baseDN, searchHelper, true);
    }

    public final Map<String, Map<String, List<String>>> searchMultiValues(final String baseDN, final String filter, final Set<String> attributes, final SEARCH_SCOPE searchScope)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.searchMultiValues(baseDN, filter, attributes, searchScope);

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter(filter);
        searchHelper.setAttributes(attributes);
        searchHelper.setSearchScope(searchScope);

        return this.searchImplementation(baseDN, searchHelper, true);
    }

    @LdapOperation
    @ModifyOperation
    public final void writeBinaryAttribute(
            final String entryDN,
            final String attributeName,
            final byte[][] values,
            final boolean overwrite
    )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.writeBinaryAttribute(entryDN, attributeName, values, overwrite);

        final String jndiBinarySetting = "java.naming.ldap.attributes.binary";

        // Create the ModificationItem
        final ModificationItem[] modificationItem = new ModificationItem[values.length];
        for (int i = 0; i < values.length; i++) {
            // Create a BasicAttribute for the object.
            final BasicAttribute attributeToReplace = new BasicAttribute(attributeName, values[i]);

            // Determine the modification type, if replace, only replace on the first attribute, the rest just get added.
            final int modType = (i == 0 && overwrite) ? DirContext.REPLACE_ATTRIBUTE : DirContext.ADD_ATTRIBUTE;

            // Populate the ModificationItem object with the flag & the attribute to replace.
            modificationItem[i] = new ModificationItem(modType, attributeToReplace);
        }

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Modify the Attributes.
        try {
            ldapConnection.modifyAttributes(addJndiEscape(entryDN), modificationItem);
            // inform jndi the attribute is binary.
            ldapConnection.addToEnvironment(jndiBinarySetting, attributeName);
        } catch (NamingException e) {
            convertNamingException(e);
        } finally {
            // clean up jndi environment
            try {
                ldapConnection.removeFromEnvironment(jndiBinarySetting);
            } catch (Exception e) {
                //doesnt matter
            }
        }
    }

    @LdapOperation
    @ModifyOperation
    public final void replaceBinaryAttribute(
            final String entryDN,
            final String attributeName,
            final byte[] oldValue,
            final byte[] newValue
    )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.replaceBinaryAttribute(entryDN, attributeName, oldValue, newValue);

        final String jndiBinarySetting = "java.naming.ldap.attributes.binary";

        // Create the ModificationItem
        final ModificationItem[] modificationItem = new ModificationItem[2];
        {
            // Create a BasicAttribute for the old value.
            final BasicAttribute oldValueOperation = new BasicAttribute(attributeName, oldValue);

            // Populate the ModificationItem array with the removal of the old value.
            modificationItem[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE , oldValueOperation);

            // Create a BasicAttribute for the new value.
            final BasicAttribute newValueOperation = new BasicAttribute(attributeName, newValue);

            // Populate the ModificationItem array with the removal of the old value.
            modificationItem[1] = new ModificationItem(DirContext.ADD_ATTRIBUTE , newValueOperation);
        }

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Modify the Attributes.
        try {
            ldapConnection.modifyAttributes(addJndiEscape(entryDN), modificationItem);
            // inform jndi the attribute is binary.
            ldapConnection.addToEnvironment(jndiBinarySetting, attributeName);
        } catch (NamingException e) {
            convertNamingException(e);
        } finally {
            // clean up jndi environment
            try {
                ldapConnection.removeFromEnvironment(jndiBinarySetting);
            } catch (Exception e) {
                //doesnt matter
            }
        }
    }

    @LdapOperation
    @ModifyOperation
    public final void writeStringAttribute(final String entryDN, final String attributeName, final Set<String> values, final boolean overwrite)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.writeStringAttribute(entryDN, attributeName, values, overwrite);


        // Create the ModificationItem
        final ModificationItem[] modificationItem = new ModificationItem[values.size()];

        int loopCounter = 0;
        for (final String value : values) {
            // Create a BasicAttribute for the object.
            final BasicAttribute attributeToReplace = new BasicAttribute(attributeName, value);

            // Determine the modification type, if replace, only replace on the first attribute, the rest just get added.
            final int modType = (loopCounter == 0 && overwrite) ? DirContext.REPLACE_ATTRIBUTE : DirContext.ADD_ATTRIBUTE;

            // Populate the ModificationItem object with the flag & the attribute to replace.
            modificationItem[loopCounter] = new ModificationItem(modType, attributeToReplace);
            loopCounter++;
        }

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Modify the Attributes.
        try {
            ldapConnection.modifyAttributes(addJndiEscape(entryDN), modificationItem);
        } catch (NamingException e) {
            convertNamingException(e);
        }
    }

    @LdapOperation
    @ModifyOperation
    public final void writeStringAttributes(final String entryDN, final Map<String,String> attributeValueProps, final boolean overwrite)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.writeStringAttributes(entryDN, attributeValueProps, overwrite);

        // Determine the modification type, if replace, only replace on the first attribute, the rest just get added.
        final int modType = overwrite ? DirContext.REPLACE_ATTRIBUTE : DirContext.ADD_ATTRIBUTE;

        // Create the ModificationItem
        final List<ModificationItem> modificationItems = new ArrayList<ModificationItem>();
        for (final String attrName : attributeValueProps.keySet()) {
            // Create a BasicAttribute for the object.
            final BasicAttribute attributeToReplace = new BasicAttribute(attrName, attributeValueProps.get(attrName));

            // Populate the ModificationItem object with the flag & the attribute to replace.
            modificationItems.add(new ModificationItem(modType, attributeToReplace));
        }

        // convert to array
        final ModificationItem[] modificationItemArray = modificationItems.toArray(new ModificationItem[modificationItems.size()]);

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Modify the Attributes.
        try {
            ldapConnection.modifyAttributes(addJndiEscape(entryDN), modificationItemArray);
        } catch (NamingException e) {
            convertNamingException(e);
        }
    }

// --------------------- Interface ChaiProviderImplementor ---------------------

    /**
     * Return the current underlying {@link javax.naming.ldap.LdapContext} for this connection.  This
     * should be used with extreme caution, as any changes to the state of the {@code LdapContext} made could
     * cause problems with Chai.  Similarly, chai may elect to invalidate, close or otherwise change
     * the state of the returned {@code LdapContext} at any time.  Specifically, if
     * {@link ChaiSetting#WATCHDOG_ENABLE} or {@link com.novell.ldapchai.provider.ChaiSetting#FAILOVER_ENABLE} are
     * set to true, Chai will periodically abandon and recreate the underlying connection objet leaving it
     * in an undefined state.
     *
     * @return the underlying {@code LdapContext} used by this {@code JNDIProviderImpl}.
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException
     *          if no valid {@code LdapContext} is currently allocated.
     */
    public Object getConnectionObject()
            throws Exception
    {
        return getLdapConnection();
    }

    public String getCurrentConnectionURL()
    {
        return this.getChaiConfiguration().bindURLsAsList().get(0);
    }

    public void init(final ChaiConfiguration chaiConfig)
            throws ChaiUnavailableException, IllegalStateException
    {
        this.chaiConfig = chaiConfig;
        final String connectionURL = chaiConfig.bindURLsAsList().get(0);
        final Hashtable env = generateJndiEnvironment(connectionURL);
        try {
            jndiConnection = generateNewJndiContext(env);
        } catch (ChaiOperationException e) {
            throw new ChaiUnavailableException("bind failed (" + e.getMessage() + ")", e.getErrorCode());
        }

        super.init(chaiConfig);
    }

// -------------------------- OTHER METHODS --------------------------

    private synchronized Hashtable generateJndiEnvironment(final String ldapURL)
    {
        // Populate the hashtable with the attributes to connect to eDirectory.
        final Hashtable<String, String> env = new Hashtable<String, String>();

        // add in basic connection info
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapURL);
        env.put(Context.SECURITY_PRINCIPAL, addJndiEscape(chaiConfig.getSetting(ChaiSetting.BIND_DN)));
        env.put(Context.SECURITY_CREDENTIALS, chaiConfig.getBindPassword());

        // set the JNDI pooler up
        final boolean jndiConnectionPoolEnable = Boolean.valueOf(chaiConfig.getSetting(ChaiSetting.JNDI_ENABLE_POOL));
        if (jndiConnectionPoolEnable) {
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            env.put("com.sun.jndi.ldap.connect.pool.initsize", String.valueOf(DEFAULT_INITIAL_POOL_SIZE));
            env.put("com.sun.jndi.ldap.connect.pool.maxsize", String.valueOf(DEFAULT_MAXIMUM_POOL_SIZE));
            env.put("com.sun.jndi.ldap.connect.pool.prefsize", String.valueOf(DEFAULT_PREFERRED_POOL_SIZE));
        }

        // connect using plaintext or plaintext/ssl
        env.put("com.sun.jndi.ldap.connect.pool.protocol", "plain ssl");

        // Set the ldap timeout time.
        env.put("com.sun.jndi.ldap.connect.timeout", chaiConfig.getSetting(ChaiSetting.LDAP_CONNECT_TIMEOUT));

        //set alias dereferencing
        env.put("java.naming.ldap.derefAliases", chaiConfig.getSetting(ChaiSetting.LDAP_DEREFENCE_ALIAS));

        //set referrals
        if (chaiConfig.getBooleanSetting(ChaiSetting.LDAP_FOLLOW_REFERRALS)) {
            env.put(Context.REFERRAL,"follow");
        }

        final boolean isSecureLdapURL = (URI.create(ldapURL)).getScheme().equalsIgnoreCase("ldaps");

        //setup blind SSL socket factory
        final boolean promiscuousMode = Boolean.valueOf(chaiConfig.getSetting(ChaiSetting.PROMISCUOUS_SSL));
        if (isSecureLdapURL) {
            if (promiscuousMode) {
                try {
                    final SSLContext sc = SSLContext.getInstance("SSL");
                    sc.init(null, new X509TrustManager[]{new PromiscuousTrustManager()}, new java.security.SecureRandom());
                    ThreadLocalSocketFactory.set(sc.getSocketFactory());
                    env.put("java.naming.ldap.factory.socket", ThreadLocalSocketFactory.class.getName());
                } catch (Exception e) {
                    LOGGER.error("error configuring promiscuous socket factory");
                }

            } else if (chaiConfig.getTrustManager() != null) {
                try {
                    final SSLContext sc = SSLContext.getInstance("SSL");
                    sc.init(null, chaiConfig.getTrustManager(), new java.security.SecureRandom());
                    ThreadLocalSocketFactory.set(sc.getSocketFactory());
                    env.put("java.naming.ldap.factory.socket", ThreadLocalSocketFactory.class.getName());
                } catch (Exception e) {
                    LOGGER.error("error configuring promiscuous socket factory");
                }
            }
        }

        // mix in default environment settings
        if (chaiConfig.getImplementationConfiguration() != null && chaiConfig.getImplementationConfiguration() instanceof Map) {
            final Map defaultEnvironment = (Map) chaiConfig.getImplementationConfiguration();
            for (final Object key : defaultEnvironment.keySet()) {
                if (key instanceof String && defaultEnvironment.get(key) instanceof String) {
                    env.put((String) key, (String) defaultEnvironment.get(key));
                }
            }
        }

        return env;
    }
    private Map<String, Map<String, List<String>>> searchImplementation(
            String baseDN,
            SearchHelper searchHelper,
            final boolean returnAllValues
    )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();

        try { // make a copy so if it changes somewhere else we won't be affected.
            searchHelper = (SearchHelper) searchHelper.clone();
        } catch (CloneNotSupportedException e) {
            LOGGER.fatal("unexpected clone of SearchHelper failed during chai search", e);
            throw new ChaiOperationException("unexpected clone of SearchHelper failed during chai search", ChaiError.UNKNOWN);
        }

        // replace a null dn with an empty string
        baseDN = baseDN != null ? baseDN : "";

        final Map<String, List<String>> emptyMap = Collections.emptyMap();

        // Define the Search Controls object.
        final SearchControls ctls = new SearchControls();
        ctls.setReturningObjFlag(false);
        ctls.setReturningAttributes(new String[0]);
        ctls.setSearchScope(searchHelper.getSearchScope().getJndiScopeInt());
        final String[] returnAttributes = searchHelper.getAttributes() == null ? null : searchHelper.getAttributes().toArray(new String[searchHelper.getAttributes().size()]);
        ctls.setReturningAttributes(returnAttributes);
        ctls.setTimeLimit(searchHelper.getTimeLimit());
        ctls.setCountLimit(searchHelper.getMaxResults());

        final Map<String, Map<String, List<String>>> results = new HashMap<String, Map<String, List<String>>>();

        NamingEnumeration<SearchResult> answer = null;

        try {
            // Search in the tree.
            final LdapContext ldapConnection = getLdapConnection();
            answer = ldapConnection.search(addJndiEscape(baseDN), searchHelper.getFilter(), ctls);
            while (answer.hasMore()) {
                final SearchResult searchResult = answer.next();
                final StringBuilder entryDN = new StringBuilder();
                entryDN.append(removeJndiEscapes(searchResult.getName()));
                if (baseDN != null && baseDN.length() > 0) {
                    if (entryDN.length() > 0) {
                        entryDN.append(',');
                    }
                    entryDN.append(baseDN);
                }

                final NamingEnumeration attributeEnum = searchResult.getAttributes().getAll();
                if (attributeEnum.hasMore()) {
                    final Map<String, List<String>> attrValues = new HashMap<String, List<String>>();
                    while (attributeEnum.hasMore()) {
                        final Attribute loopAttribute = (Attribute) attributeEnum.next();
                        final String attrName = loopAttribute.getID();
                        final List<String> valueList = new ArrayList<String>();
                        for (NamingEnumeration attrValueEnum = loopAttribute.getAll(); attrValueEnum.hasMore();) {
                            final Object value = attrValueEnum.next();
                            valueList.add(value.toString());
                            if (!returnAllValues) {
                                attrValueEnum.close();
                                break;
                            }
                        }
                        attrValues.put(attrName, Collections.unmodifiableList(valueList));
                    }
                    results.put(entryDN.toString(), Collections.unmodifiableMap(attrValues));
                } else {
                    results.put(entryDN.toString(), emptyMap);
                }
            }

            return Collections.unmodifiableMap(results);
        } catch (NullPointerException e) {
            return null;
        } catch (NamingException e) {
            // check if any results have been returned, if so return the result.  Otherwise,
            // throw an error.  Common cause is a search size/time limit exceeded.
            if (results != null && results.size() > 0) {
                return results;
            }

            convertNamingException(e);
            return null;
        } finally {
            if (answer != null) {
                try {
                    answer.close();
                } catch (NamingException e) {
                    // nothing to do
                }
            }
        }
    }

    private LdapContext getLdapConnection()
            throws ChaiUnavailableException
    {
        {
            return jndiConnection;
        }
    }

    private void convertNamingException(final NamingException e)
            throws ChaiOperationException, ChaiUnavailableException
    {
        if (errorIsRetryable(e)) {
            throw new ChaiUnavailableException(e.getMessage(), ChaiError.COMMUNICATION, false, false);
        }

        throw ChaiOperationException.forErrorMessage(e.getMessage());
    }


    public boolean errorIsRetryable(final Exception e)
    {
        if (e instanceof CommunicationException || e instanceof ServiceUnavailableException) {
            final String msgText = e.getMessage();
            if (msgText != null && !msgText.toLowerCase().contains("unrecognized extended operation")) {
                return true;
            }
        }

        return super.errorIsRetryable(e);
    }

    public boolean isConnected() {
        return jndiConnection != null;
    }

    protected static String removeJndiEscapes(final String input) {
        if (input == null) {
            return null;
        }

        // remove surrounding quotes if the internal value contains a / charachter
        final String slashEscapePattern = "^\".*/.*\"$";
        if (input.matches(slashEscapePattern)) {
            return input.replaceAll("^\"|\"$","");
        }
        return input;
    }

    protected static String addJndiEscape(final String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("/", "\\\\2f");
    }
}