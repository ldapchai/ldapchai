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

package com.novell.ldapchai.impl.edir.entry;


import com.novell.ldapchai.*;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderFactory;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.DefaultChaiPasswordPolicy;
import com.novell.ldapchai.util.SearchHelper;
import com.novell.security.nmas.jndi.ldap.ext.GetPwdPolicyInfoRequest;
import com.novell.security.nmas.jndi.ldap.ext.GetPwdPolicyInfoResponse;

import javax.naming.ldap.ExtendedResponse;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A collection of static helper methods used by the LDAP Chai API.
 * <p/>
 * Generally, consumers of the LDAP Chai API should avoid calling these methods directly.  Where possible,
 * use the {@link com.novell.ldapchai.ChaiEntry} wrappers instead.
 *
 * @author Jason D. Rivard
 */
public class EdirEntries {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(EdirEntries.class);

// -------------------------- STATIC METHODS --------------------------

    /**
     * Convert a Date to the Zulu String format.
     * See the <a href="http://developer.novell.com/documentation/ndslib/schm_enu/data/sdk5701.html">eDirectory Time attribute syntax definition</a> for more details.
     *
     * @param date The Date to be converted
     * @return A string formated such as "199412161032Z".
     */
    public static String convertDateToZulu(final Date date)
    {
        if (date == null) {
            throw new NullPointerException();
        }

        final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        timeFormat.setTimeZone(TimeZone.getTimeZone("Zulu"));
        return timeFormat.format(date);
    }

    static boolean convertStrToBoolean(final String string)
    {
        return !(string == null || string.length() < 1) && (string.equalsIgnoreCase("true") ||
                string.equalsIgnoreCase("1") ||
                string.equalsIgnoreCase("yes") ||
                string.equalsIgnoreCase("y"));
    }

    static int convertStrToInt(final String string, final int defaultValue)
    {
        if (string == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Convert the commonly used eDirectory zulu time string to java Date object.
     * See the <a href="http://developer.novell.com/documentation/ndslib/schm_enu/data/sdk5701.html">eDirectory Time attribute syntax definition</a> for more details.
     *
     * @param dateString a date string in the format of "yyyyMMddHHmmss'Z'", for example "199412161032Z"
     * @return A Date object representing the string date
     * @throws IllegalArgumentException if dateString is incorrectly formatted
     */
    public static Date convertZuluToDate(final String dateString)
    {
        if (dateString == null) {
            throw new NullPointerException();
        }

        if (dateString.length() < 15) {
            throw new IllegalArgumentException("zulu date too short");
        }

        if (!"Z".equalsIgnoreCase(String.valueOf(dateString.charAt(14)))) {
            throw new IllegalArgumentException("zulu date must end in 'Z'");
        }

        // Zulu TimeZone is same as GMT or UTC
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Zulu"));

        cal.set(Calendar.YEAR, Integer.parseInt(dateString.substring(0, 4)));
        cal.set(Calendar.MONTH, Integer.parseInt(dateString.substring(4, 6)) - 1);
        cal.set(Calendar.DATE, Integer.parseInt(dateString.substring(6, 8)));
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(dateString.substring(8, 10)));
        cal.set(Calendar.MINUTE, Integer.parseInt(dateString.substring(10, 12)));
        cal.set(Calendar.SECOND, Integer.parseInt(dateString.substring(12, 14)));

        return cal.getTime();
    }

    /**
     * Creates a new group entry in the ldap directory.  A new "groupOfNames" object is created.
     * The "cn" and "description" ldap attributes are set to the supplied name.
     *
     * @param parentDN the entryDN of the new group.
     * @param name     name of the group
     * @param provider a ldap provider be used to create the group.
     * @return an instance of the ChaiGroup entry
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the directory server(s) are unavailable
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
        provider.createEntry(entryDN.toString(), ChaiConstant.OBJECTCLASS_BASE_LDAP_GROUP, Collections.<String, String>emptyMap());

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
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the directory server(s) are unavailable
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

            final Map<String, Map<String,String>> results = provider.search(containerDN, filter.toString(), null, ChaiProvider.SEARCH_SCOPE.ONE);
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
     * {@link com.novell.ldapchai.ChaiUser}'s write methods to add additional data to the ldap user entry.
     *
     * @param userDN   the new userDN.
     * @param sn       the last name of
     * @param provider a ldap provider be used to create the group.
     * @return an instance of the ChaiUser entry
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the directory server(s) are unavailable
     */
    public static ChaiUser createUser(final String userDN, final String sn, final ChaiProvider provider)
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Map<String,String> createAttributes = new HashMap<String, String>();

        createAttributes.put(ChaiConstant.ATTR_LDAP_SURNAME, sn);

        provider.createEntry(userDN, ChaiConstant.OBJECTCLASS_BASE_LDAP_USER, createAttributes);

        //lets create a user object
        return ChaiFactory.createChaiUser(userDN, provider);
    }

    /**
     * Convert to an LDIF format.  Useful for debugging or other purposes
     *
     * @param theEntry A valid {@code ChaiEntry}
     * @return A string containing a properly formated LDIF view of the entry.
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the directory server(s) are unavailable
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
     * Find the appropriate password policy for the given user.  The ChaiProvider
     * must have appropriate rights to read the policy and policy assignment attributes.
     *
     * @param theUser The user to find the the password policy for
     * @return A valid PasswordPolicy for the user
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the ldap server(s) are not available
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     * @see com.novell.ldapchai.ChaiUser#getPasswordPolicy()
     */
    public static ChaiPasswordPolicy readPasswordPolicy(final ChaiUser theUser)
            throws ChaiUnavailableException, ChaiOperationException
    {
        return UserPasswordPolicyReader.readPasswordPolicy(theUser);
    }

    /**
     * Remove a group membership for the supplied user and group.  This implementation
     * takes care of all four attributes used in eDirectory static group associations:
     * <ul>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_GROUP_MEMBERSHIP}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_MEMBER}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_SECURITY_EQUALS}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_EQUIVALENT_TO_ME}</li>
     * </ul>
     *
     * @param user  A valid {@code ChaiUser}
     * @param group A valid {@code ChaiGroup}
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the ldap server(s) are not available
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     */
    public static void removeGroupMembership(final ChaiUser user, final ChaiGroup group)
            throws ChaiOperationException, ChaiUnavailableException
    {
        if (user == null) {
            throw new NullPointerException("user cannot be null");
        }

        if (group == null) {
            throw new NullPointerException("group cannot be null");
        }

        //Delete the attribs off of the user
        user.deleteAttribute(ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, group.getEntryDN());
        user.deleteAttribute(ChaiConstant.ATTR_LDAP_SECURITY_EQUALS, group.getEntryDN());

        //Delete the attribs off of the group
        group.deleteAttribute(ChaiConstant.ATTR_LDAP_MEMBER, user.getEntryDN());
        group.deleteAttribute(ChaiConstant.ATTR_LDAP_EQUIVALENT_TO_ME, user.getEntryDN());
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
     * <li>There is a timeout period (the test may never successfully complete</li>
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
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If an error is encountered during the operation
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If no directory servers are reachable
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
            debugMsg.append("testReplication for ").append(chaiEntry).append(":").append(attribute);
            debugMsg.append(" ").append(testCount).append(" up,");
            debugMsg.append(" ").append(ldapURLs.size() - testCount).append(" down,");
            debugMsg.append(" ").append(successCount).append(" in sync");
            LOGGER.debug(debugMsg);
        }

        return testCount > 0 && testCount == successCount;
    }

    /**
     * Add a group membership for the supplied user and group.  This implementation
     * takes care of all four attributes used in eDirectory static group associations:
     * <ul>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_GROUP_MEMBERSHIP}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_MEMBER}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_SECURITY_EQUALS}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_EQUIVALENT_TO_ME}</li>
     * </ul>
     *
     * @param user  A valid {@code ChaiUser}
     * @param group A valid {@code ChaiGroup}
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the ldap server(s) are not available
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     */
    public static void writeGroupMembership(final ChaiUser user, final ChaiGroup group)
            throws ChaiOperationException, ChaiUnavailableException
    {
        if (user == null) {
            throw new NullPointerException("user cannot be null");
        }

        if (group == null) {
            throw new NullPointerException("group cannot be null");
        }

        user.addAttribute(ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, group.getEntryDN());
        user.addAttribute(ChaiConstant.ATTR_LDAP_SECURITY_EQUALS, group.getEntryDN());

        group.addAttribute(ChaiConstant.ATTR_LDAP_MEMBER, user.getEntryDN());
        group.addAttribute(ChaiConstant.ATTR_LDAP_EQUIVALENT_TO_ME, user.getEntryDN());
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private EdirEntries()
    {
    }

    // -------------------------- INNER CLASSES --------------------------
    private static final class UserPasswordPolicyReader {
        private static final Set<String> TRADITIONAL_PASSWORD_ATTRIBUTES;
        private static final SearchHelper NSPM_ENTRY_SEARCH_HELPER = new SearchHelper();

        static {
            {
                final Set<String> tempSet = new HashSet<String>();
                tempSet.add(ChaiConstant.ATTR_LDAP_PASSWORD_MINIMUM_LENGTH);
                tempSet.add(ChaiConstant.ATTR_LDAP_PASSWORD_EXPIRE_INTERVAL);
                tempSet.add(ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_PASSWORD_UNIQUE_REQUIRED);
                TRADITIONAL_PASSWORD_ATTRIBUTES = Collections.unmodifiableSet(tempSet);
            }

            final Set<String> nspm_password_attributes;
            {
                final Set<String> tempSet = new HashSet<String>();
                for (final NspmPasswordPolicy.Attribute attr : NspmPasswordPolicy.Attribute.values()) {
                    tempSet.add(attr.getLdapAttribute());
                }
                nspm_password_attributes = Collections.unmodifiableSet(tempSet);
            }

            {
                NSPM_ENTRY_SEARCH_HELPER.setSearchScope(ChaiProvider.SEARCH_SCOPE.BASE);
                NSPM_ENTRY_SEARCH_HELPER.setAttributes(nspm_password_attributes);
            }
        }


        static ChaiPasswordPolicy readPasswordPolicy(final ChaiUser theUser)
                throws ChaiUnavailableException, ChaiOperationException
        {
            ChaiPasswordPolicy pwordPolicy = DefaultChaiPasswordPolicy.createDefaultChaiPasswordPolicy();

            boolean usedUniversalPolicy = false;

            try {
                // fetch the user's associated password policy
                final ChaiEntry policyEntry = findNspmPolicyForUser(theUser);
                if (policyEntry != null) {
                    pwordPolicy = new NspmPasswordPolicyImpl(policyEntry.getEntryDN(),policyEntry.getChaiProvider());

                    // check to see if the advanced rules on the password policy are "enabled"
                    if (pwordPolicy.getRuleHelper().isPolicyEnabled()) {
                        // we've got a policy, and rules are enabled, now read the policy.
                        LOGGER.trace("using active universal password policy for user " + theUser.getEntryDN() + " at " + policyEntry.getEntryDN());
                        usedUniversalPolicy = true;
                    } else {
                        LOGGER.debug("ignoring unenabled nspm password policy for user " + theUser.getEntryDN() + " at " + policyEntry.getEntryDN());
                    }
                }
            } catch (ChaiOperationException e) {
                LOGGER.error("ldap error reading universal password policy: " + e.getMessage());
                throw e;
            }

            // if there is no universal password policy then fall back to reading user object attrs
            if (!usedUniversalPolicy) {
                try {
                    pwordPolicy = readTraditionalPasswordRules(theUser);
                    LOGGER.trace("read traditional (non-nmas) password attributes from user entry " + theUser.getEntryDN());
                } catch (ChaiOperationException e) {
                    LOGGER.error("ldap error reading traditional password policy: " + e.getMessage());
                }
            }

            return pwordPolicy;
        }

        private static ChaiEntry findNspmPolicyForUser(final ChaiUser theUser)
                throws ChaiUnavailableException, ChaiOperationException
        {
            final boolean useNmasSetting = theUser.getChaiProvider().getChaiConfiguration().getBooleanSetting(ChaiSetting.EDIRECTORY_ENABLE_NMAS);

            if (useNmasSetting) {
                final GetPwdPolicyInfoRequest request = new GetPwdPolicyInfoRequest();
                request.setObjectDN(theUser.getEntryDN());
                final ExtendedResponse response = theUser.getChaiProvider().extendedOperation(request);
                if (response != null) {
                    final GetPwdPolicyInfoResponse polcyInfoResponse = (GetPwdPolicyInfoResponse) response;
                    final String policyDN = polcyInfoResponse.getPwdPolicyDNStr();
                    if (policyDN != null) {
                        return ChaiFactory.createChaiEntry(policyDN, theUser.getChaiProvider());
                    }
                }
                return null;
            } else {
                // look at user object first
                {
                    final String policyDN = theUser.readStringAttribute("nspmPasswordPolicyDN");
                    if (policyDN != null && policyDN.length() > 0) {
                        return ChaiFactory.createChaiEntry(policyDN, theUser.getChaiProvider());
                    }
                }

                final ChaiEntry parentObject = theUser.getParentEntry();

                // look at parent next
                {
                    if (parentObject != null) {
                        final String policyDN = parentObject.readStringAttribute("nspmPasswordPolicyDN");
                        if (policyDN != null && policyDN.length() > 0) {
                            return ChaiFactory.createChaiEntry(policyDN, theUser.getChaiProvider());
                        }
                    }
                }

                // look at partition root
                {
                    if (parentObject != null) {
                        final ChaiEntry partitonRoot = findPartitionRoot(parentObject);
                        if (partitonRoot != null) {
                            final String policyDN = partitonRoot.readStringAttribute("nspmPasswordPolicyDN");
                            if (policyDN != null && policyDN.length() > 0) {
                                return ChaiFactory.createChaiEntry(policyDN, theUser.getChaiProvider());
                            }
                        }
                    }
                }

                // look at policy object
                {
                    final ChaiEntry securityContainer = ChaiFactory.createChaiEntry("cn=Security", theUser.getChaiProvider());
                    final String loginPolicyDN = securityContainer.readStringAttribute("sASLoginPolicyDN");
                    if (loginPolicyDN != null && loginPolicyDN.length() > 0) {
                        final ChaiEntry loginPolicy = ChaiFactory.createChaiEntry(loginPolicyDN, theUser.getChaiProvider());
                        final String policyDN = loginPolicy.readStringAttribute("nspmPasswordPolicyDN");
                        if (policyDN != null && policyDN.length() > 0) {
                            return ChaiFactory.createChaiEntry(policyDN, theUser.getChaiProvider());
                        }
                    }
                }
            }
            return null;
        }

        /**
         * Reads and applys the user's traditional (non-UP) password rules to the policy.
         * <p/>
         * If the user does not have an associated universal password policy, this
         * method can be used to read the old style password rules.
         *
         * @param theUser a valid chaiUser instance
         * @return a PasswordPolicy instance representing the users policy
         * @throws ChaiUnavailableException If the ldap server(s) are not available
         * @throws ChaiOperationException   If there is an error during the operation
         */
        private static ChaiPasswordPolicy readTraditionalPasswordRules(final ChaiUser theUser)
                throws ChaiUnavailableException, ChaiOperationException
        {
            final Map<String,String> values = theUser.readStringAttributes(TRADITIONAL_PASSWORD_ATTRIBUTES);

            final int minLength = convertStrToInt(values.get("passwordMinimumLength"), 0);
            final int expireInterval = convertStrToInt(values.get("passwordExpirationInterval"), 0);
            final boolean uniqueRequired = convertStrToBoolean(values.get("passwordUniqueRequired"));

            final Map<ChaiPasswordRule, String> policyMap = new HashMap<ChaiPasswordRule, String>();
            policyMap.put(ChaiPasswordRule.MaximumLength, String.valueOf(16));  // default for legacy passwords;
            policyMap.put(ChaiPasswordRule.MinimumLength, String.valueOf(minLength));
            policyMap.put(ChaiPasswordRule.ExpirationInterval, String.valueOf(expireInterval));
            policyMap.put(ChaiPasswordRule.UniqueRequired, String.valueOf(uniqueRequired));

            //other defaults for non up-policy.
            policyMap.put(ChaiPasswordRule.AllowNumeric, String.valueOf(true));
            policyMap.put(ChaiPasswordRule.AllowSpecial, String.valueOf(true));
            policyMap.put(ChaiPasswordRule.CaseSensitive, String.valueOf(false));

            return DefaultChaiPasswordPolicy.createDefaultChaiPasswordPolicyByRule(policyMap);
        }
    }

    static String readGuid(final ChaiEntry entry)
            throws ChaiUnavailableException, ChaiOperationException
    {
        final byte[] st = entry.getChaiProvider().readMultiByteAttribute(entry.getEntryDN(),"guid")[0];
        final BigInteger bigInt = new BigInteger(1,st);
        return bigInt.toString(16);
    }

}