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

package com.novell.ldapchai.util;

import com.novell.ldapchai.*;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderFactory;
import com.novell.ldapchai.provider.ChaiSetting;

import java.util.*;

/**
 * A collection of static helper methods used by the LDAP Chai API.
 * <p/>
 * Generally, consumers of the LDAP Chai API should avoid calling these methods directly.  Where possible,
 * use the {@link com.novell.ldapchai.ChaiEntry} wrappers instead.
 *
 * @author Jason D. Rivard
 */
public class ChaiUtility {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(ChaiUtility.class);

// -------------------------- STATIC METHODS --------------------------

    /**
     * Creates a new group entry in the ldap directory.  A new "groupOfNames" object is created.
     * The "cn" and "description" ldap attributes are set to the supplied name.
     *
     * @param parentDN the entryDN of the new group.
     * @param name     name of the group
     * @param provider a ldap provider be used to create the group.
     * @return an instance of the ChaiGroup entry
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    public static ChaiGroup createGroup(final String parentDN, final String name, final ChaiProvider provider)
            throws ChaiOperationException, ChaiUnavailableException
    {
        //Get a good CN for it
        final String objectCN = findUniqueName(name, parentDN, provider);

        //Concantonate the entryDN
        final StringBuilder entryDN = new StringBuilder();
        entryDN.append("cn=");
        entryDN.append(objectCN);
        entryDN.append(',');
        entryDN.append(parentDN);

        //First create the base group.
        provider.createEntry(entryDN.toString(), ChaiConstant.OBJECTCLASS_BASE_LDAP_GROUP, new Properties());

        //Now build an ldapentry object to add attributes to it
        final ChaiEntry theObject = ChaiFactory.createChaiEntry(entryDN.toString(), provider);

        //Add the description
        theObject.writeStringAttribute(ChaiConstant.ATTR_LDAP_DESCRIPTION, name);

        //Return the newly created group.
        return ChaiFactory.createChaiGroup(entryDN.toString(), provider);
    }

    /**
     * Derives a unique entry name for an ldap container.  Assumes CN as the naming attribute.
     *
     * @param baseName    A text name that will be used for the base of the obejct name. Punctuation and spaces will be stripped.
     * @param containerDN Directory container in which to check for a unique name
     * @param provider    ChaiProvider to use for ldap connection
     * @return Fully qualified unique object name for the container specified.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    public static String findUniqueName(String baseName, final String containerDN, final ChaiProvider provider)
            throws ChaiOperationException, ChaiUnavailableException
    {
        char ch;
        final StringBuilder cnStripped = new StringBuilder();

        if (baseName == null) {
            baseName = "";
        }

        // First boil down the root name. Preserve only the alpha-numerics.
        for (int i = 0; i < baseName.length(); i++) {
            ch = baseName.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                cnStripped.append(ch);
            }
        }

        if (cnStripped.length() == 0) {
            // Generate a random seed to runServer with, how about the current date
            cnStripped.append(System.currentTimeMillis());
        }

        // Now we have a base name, let's runServer testing it...
        String uniqueCN;
        StringBuilder filter;

        final Random randomNumber = new Random();
        int iExt = randomNumber.nextInt() % 1000; // Start with a random 3 digit number
        String sExt = null;

        while (true) {
            // Initialize the String Buffer and Unique DN.
            filter = new StringBuilder(64);

            if (sExt != null) {
                uniqueCN = cnStripped.append(sExt).toString();
            } else {
                uniqueCN = cnStripped.toString();
            }
            filter.append("(").append(ChaiConstant.ATTR_LDAP_COMMON_NAME).append("=").append(uniqueCN).append(")");

            final Map<String, Properties> results = provider.search(containerDN, filter.toString(), null, ChaiProvider.SEARCH_SCOPE.ONE);
            if (results.size() == 0) {
                // No object found!
                break;
            } else {
                // Increment it every time
                sExt = Integer.toString(iExt++);
            }
        }

        return uniqueCN;
    }

    /**
     * Creates a new user entry in the ldap directory.  A new "inetOrgPerson" object is created in the
     * ldap directory.  Generally, calls to this method will typically be followed by a call to the returned
     * {@link ChaiUser}'s write methods to add additional data to the ldap user entry.
     *
     * @param userDN   the new userDN.
     * @param sn       the last name of
     * @param provider a ldap provider be used to create the group.
     * @return an instance of the ChaiUser entry
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    public static ChaiUser createUser(final String userDN, final String sn, final ChaiProvider provider)
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Properties createAttributes = new Properties();

        createAttributes.setProperty(ChaiConstant.ATTR_LDAP_SURNAME, sn);

        provider.createEntry(userDN, ChaiConstant.OBJECTCLASS_BASE_LDAP_USER, createAttributes);

        //lets create a user object
        return ChaiFactory.createChaiUser(userDN, provider);
    }

    /**
     * Convert to an LDIF format.  Useful for debugging or other purposes
     *
     * @param theEntry A valid {@code ChaiEntry}
     * @return A string containing a properly formated LDIF view of the entry.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    public static String entryToLDIF(final ChaiEntry theEntry)
            throws ChaiUnavailableException, ChaiOperationException
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("dn: ").append(theEntry.getEntryDN()).append("\n");

        final Map<String, Map<String, List<String>>> results = theEntry.getChaiProvider().searchMultiValues(theEntry.getEntryDN(), "(objectClass=*)", null, ChaiProvider.SEARCH_SCOPE.BASE);
        final Map<String, List<String>> props = results.get(theEntry.getEntryDN());

        for (final String attrName : props.keySet()) {
            final List<String> values = props.get(attrName);
            for (final String value : values) {
                sb.append(attrName).append(": ").append(value).append('\n');
            }
        }

        return sb.toString();
    }

    private static ChaiEntry findPartitionRoot(final ChaiEntry theEntry)
            throws ChaiUnavailableException, ChaiOperationException
    {
        ChaiEntry loopEntry = theEntry;

        while (loopEntry != null) {
            final Set<String> objClasses = loopEntry.readMultiStringAttribute(ChaiConstant.ATTR_LDAP_OBJECTCLASS);
            if (objClasses.contains(ChaiConstant.OBJECTCLASS_BASE_LDAP_PARTITION)) {
                return loopEntry;
            }
            loopEntry = loopEntry.getParentEntry();
        }
        return null;
    }

    /**
     * Test the replication of an attribute.  It is left to the implementation to determine the means and criteria for
     * this operation.  Typically this method would be used just after a write operation in some type of time delayed loop.
     * <p/>
     * Typical implementations will do the following:
     * <ul>
     * <li>issue {@link com.novell.ldapchai.ChaiEntry#readStringAttribute(String)} to read a value</li>
     * <li>establish an LDAP connection to all known replicas</li>
     * <li>issue {@link com.novell.ldapchai.ChaiEntry#compareStringAttribute(String, String)} to to each server directly</li>
     * <li>return true if each server contacted has the same value, false if not</li>
     * </ul>
     * <p/>
     * Target servers that are unreachable or return errors are ignored, and do not influence the results. It is entirely
     * possible that no matter how many times this method is called, false will always be returned, so the caller should
     * take care not to repeat a test indefinitly.
     * <p/>
     * This operation is potentially expensive, as it may establish new LDAP level connections to each target server each
     * time it is invoked.
     * <p/>
     * The following sample shows how this method might be used.  There are a few important attributes of the sample:
     * <ul>
     * <li>Multiple ldap servers are specified</li>
     * <li>There is a pause time between each replication check (the test can be expensive)</li>
     * <li>There is a timeout period (the test may never successfully complete)</li>
     * </ul>
     * <hr/><blockquote><pre>
     *   ChaiUser theUser =                                                                     // create a new chai user.
     *      ChaiFactory.quickProvider("ldap://ldaphost,ldap://ldaphost2","cn=admin,ou=ou,o=o","novell");
     * <p/>
     *   theUser.writeStringAttributes("description","testValue" + (new Random()).nextInt());    // write a random value to an attribute
     * <p/>
     *   final int maximumWaitTime = 120 * 1000;                                                // maximum time to wait for replication
     *   final int pauseTime = 3 * 1000;                                                        // time between iterations
     * <p/>
     *   final long startTime = System.currentTimeMillis();                                     // timestamp of beginning of wait
     *   boolean replicated = false;
     *   while (System.currentTimeMillis() - startTime < maximumWaitTime) {                     // loop until
     *       try { Thread.sleep(pauseTime); } catch (InterruptedException e)  {}                // sleep between iterations
     *       replicated = ChaiUtility.testAttributeReplication(theUser,"description",null);     // check if data replicated yet
     *       if (replicated) {
     *           break;                                                                         // break if data has replicated
     *       }
     *   }
     *   System.out.println("Attribute replication successful: " + replicated);                 // report success
     * </pre></blockquote><hr/>
     *
     * @param chaiEntry A valid entry
     * @param attribute A valid attribute on the entry
     * @param value     The value to test for.  If {@code null}, a value is read from the active server
     * @return true if the attribute is the same on all servers
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     */
    public static boolean testAttributeReplication(final ChaiEntry chaiEntry, final String attribute, String value)
            throws ChaiOperationException, ChaiUnavailableException
    {
        if (value == null || value.length() < 1) {
            value = chaiEntry.readStringAttribute(attribute);
        }

        if (value == null) {
            throw ChaiOperationException.forErrorMessage("unreadable to read test attribute from primary ChaiProvider");
        }

        final ChaiConfiguration chaiConfiguration = chaiEntry.getChaiProvider().getChaiConfiguration();

        final List<String> ldapURLs = chaiConfiguration.bindURLsAsList();

        LOGGER.trace("testAttributeReplication, will test the following ldap urls: " + ldapURLs);

        int testCount = 0;
        int successCount = 0;

        for (final String loopURL : ldapURLs) {
            ChaiProvider loopProvider = null;
            try {
                final ChaiConfiguration loopConfig = (ChaiConfiguration) chaiConfiguration.clone();
                loopConfig.setSetting(ChaiSetting.BIND_URLS, loopURL);
                loopConfig.setSetting(ChaiSetting.FAILOVER_CONNECT_RETRIES, "1");

                loopProvider = ChaiProviderFactory.createProvider(loopConfig);

                if (loopProvider.compareStringAttribute(chaiEntry.getEntryDN(), attribute, value)) {
                    successCount++;
                }

                testCount++;
            } catch (ChaiUnavailableException e) {
                //disregard
            } catch (ChaiOperationException e) {
                //disregard
            } catch (CloneNotSupportedException e) {
                //disregard
            } finally {
                try {
                    loopProvider.close();
                } catch (Exception e) {
                    //already closed, whatever.
                }
            }
        }

        if (LOGGER.isDebugEnabled()) {
            final StringBuilder debugMsg = new StringBuilder();
            debugMsg.append("testAttributeReplication for ").append(chaiEntry).append(":").append(attribute);
            debugMsg.append(" ").append(testCount).append(" up,");
            debugMsg.append(" ").append(ldapURLs.size() - testCount).append(" down,");
            debugMsg.append(" ").append(successCount).append(" in sync");
            LOGGER.debug(debugMsg);
        }

        return testCount > 0 && testCount == successCount;
    }


// --------------------------- CONSTRUCTORS ---------------------------

    private ChaiUtility()
    {
    }

    public static String passwordPolicyToString(final ChaiPasswordPolicy policy) {
        if (policy == null) {
            throw new NullPointerException("null ChaiPasswordPolicy can not be converted to string");
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("ChaiPasswordPolicy: ");

        if (!policy.getKeys().isEmpty()) {
            sb.append("{");
            for (final String key : policy.getKeys()) {
                final ChaiPasswordRule rule = ChaiPasswordRule.forKey(key);
                sb.append(rule == null ? key : rule);
                sb.append("=");
                sb.append(policy.getValue(key));
                sb.append(", ");
            }
            sb.delete(sb.length() - 2,sb.length());
            sb.append("}");
        } else {
            sb.append("[empty]");
        }

        return sb.toString();
    }

    /**
     * Determines the vendor of a the ldap directory by reading RootDSE attributes
     * @param rootDSE A valid entry  
     * @return the proper directory vendor, or {@link com.novell.ldapchai.provider.ChaiProvider.DIRECTORY_VENDOR#GENERIC} if the vendor can not be determined.
     * @throws ChaiUnavailableException If the directory is unreachable
     * @throws ChaiOperationException If there is an error reading values from the Root DSE entry
     */
    public static ChaiProvider.DIRECTORY_VENDOR determineDirectoryVendor(final ChaiEntry rootDSE)
            throws ChaiUnavailableException, ChaiOperationException
    {
        final String[] interestingAttributes = {
                "vendorVersion",
                "vendorName",
                "rootDomainNamingContext",
                "objectClass"
        };

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setAttributes(interestingAttributes);
        searchHelper.setFilter("(objectClass=*)");
        searchHelper.setMaxResults(1);
        searchHelper.setSearchScope(ChaiProvider.SEARCH_SCOPE.BASE);

        final Map<String,Map<String,List<String>>> results = rootDSE.getChaiProvider().searchMultiValues("",searchHelper);

        if (results != null && results.size() == 1) {
            final Map<String,List<String>> rootDseSearchResults = results.get("");
            if (rootDseSearchResults != null) {
                final List<String> vendorVersions = rootDseSearchResults.get("vendorVersion") == null ? Collections.<String>emptyList() : rootDseSearchResults.get("vendorVersion");
                final List<String> vendorNames = rootDseSearchResults.get("vendorName") == null ? Collections.<String>emptyList() : rootDseSearchResults.get("vendorName");
                final List<String> rootDomainNamingContexts = rootDseSearchResults.get("rootDomainNamingContext") == null ? Collections.<String>emptyList() : rootDseSearchResults.get("rootDomainNamingContext");
                final List<String> objectClasses = rootDseSearchResults.get("objectClass") == null ? Collections.<String>emptyList() : rootDseSearchResults.get("objectClass");

                { // try to detect Novell eDirectory
                    for (final String vendorVersionValue : vendorVersions) {
                        if (vendorVersionValue.contains("Novell eDirectory")) {
                            return ChaiProvider.DIRECTORY_VENDOR.NOVELL_EDIRECTORY;
                        }
                    }
                }

                { // try to detect ms-active directory
                    for (final String rootDomainNamingContextValue : rootDomainNamingContexts) {
                        if (rootDomainNamingContextValue.contains("DC=")) {
                            return ChaiProvider.DIRECTORY_VENDOR.MICROSOFT_ACTIVE_DIRECTORY;
                        }
                    }
                }


                { // try to detect 389 Directory
                    for (final String vendorNamesValue : vendorNames) {
                        if (vendorNamesValue.contains("389 Project")) {
                            return ChaiProvider.DIRECTORY_VENDOR.DIRECTORY_SERVER_389;
                        }
                    }
                    for (final String vendorVersionsValue : vendorVersions) {
                        if (vendorVersionsValue.contains("389-Directory")) {
                            return ChaiProvider.DIRECTORY_VENDOR.DIRECTORY_SERVER_389;
                        }
                    }
                }

                { // try to detect openLDAP
                    for (final String objectClassValue : objectClasses) {
                        if (objectClassValue.contains("OpenLDAProotDSE")) {
                            return ChaiProvider.DIRECTORY_VENDOR.OPEN_LDAP;
                        }
                    }
                }
            }
        }

        return ChaiProvider.DIRECTORY_VENDOR.GENERIC;
    }
}