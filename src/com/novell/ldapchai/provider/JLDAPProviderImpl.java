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

import com.novell.ldap.*;
import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.SearchHelper;

import javax.naming.NamingException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.util.*;

/**
 * JLDAP {@code ChaiProvider} implementation.  This
 * class wraps the JLDAP api at <a href="http://www.openldap.org/jldap/">OpenLDAP</a>.
 * <p/>
 * Instances can be obtained using {@link ChaiProviderFactory}.
 * <p/>
 * The current implementation does not support fail-over.  If multiple LDAP urls are specified
 * in the configuration, only the first one is used.
 *
 * @author Jason D. Rivard
 */

public class JLDAPProviderImpl extends AbstractProvider implements ChaiProviderImplementor {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(JLDAPProviderImpl.class.getName());
    private LDAPConnection ldapConnection;

// -------------------------- STATIC METHODS --------------------------

    //@todo test case needed
    static JLDAPProviderImpl createUsingExistingConnection(final LDAPConnection ldapConnection, final ChaiConfiguration chaiConfig)
            throws Exception
    {
        //@todo stub to be used for nmas c/r, this should be more robust.
        final JLDAPProviderImpl newImpl = new JLDAPProviderImpl();
        newImpl.init(chaiConfig);
        newImpl.ldapConnection = ldapConnection;
        return newImpl;
    }

    public void init(final ChaiConfiguration chaiConfig)
            throws ChaiUnavailableException, IllegalStateException
    {
        super.init(chaiConfig);
        try {
            // grab the first URL from the list.
            final URI ldapURL = URI.create(chaiConfig.bindURLsAsList().get(0));

            if (ldapURL.getScheme().equalsIgnoreCase("ldaps")) {
                final boolean usePromiscuousSSL = Boolean.parseBoolean(chaiConfig.getSetting(ChaiSetting.PROMISCUOUS_SSL));
                if (usePromiscuousSSL) {
                    try {
                        final SSLContext sc = SSLContext.getInstance("SSL");
                        sc.init(null, new X509TrustManager[]{new PromiscuousTrustManager()}, new java.security.SecureRandom());
                        ldapConnection = new LDAPConnection(new LDAPJSSESecureSocketFactory(sc.getSocketFactory()));
                    } catch (Exception e) {
                        LOGGER.error("error creating promiscuous ssl ldap socket factory: " + e.getMessage());
                    }
                } else if (chaiConfig.getTrustManager() != null) {
                    try {
                        final SSLContext sc = SSLContext.getInstance("SSL");
                        sc.init(null, chaiConfig.getTrustManager(), new java.security.SecureRandom());
                        ldapConnection = new LDAPConnection(new LDAPJSSESecureSocketFactory(sc.getSocketFactory()));
                    } catch (Exception e) {
                        LOGGER.error("error creating configured ssl ldap socket factory: " + e.getMessage());
                    }
                } else {
                    ldapConnection = new LDAPConnection(new LDAPJSSESecureSocketFactory());
                }
            } else {
                ldapConnection = new LDAPConnection();
            }

            ldapConnection.connect(ldapURL.getHost(), ldapURL.getPort());
            if (chaiConfig.getBooleanSetting(ChaiSetting.LDAP_FOLLOW_REFERRALS)) {
                final LDAPConstraints ldapConstraints = new LDAPConstraints();
                ldapConstraints.setReferralFollowing(true);
                ldapConnection.setConstraints(ldapConstraints);
            }
            final byte[] bindPassword = chaiConfig.getSetting(ChaiSetting.BIND_PASSWORD).getBytes();
            final String bindDN = chaiConfig.getSetting(ChaiSetting.BIND_DN);
            ldapConnection.bind(LDAPConnection.LDAP_V3, bindDN, bindPassword);
        } catch (LDAPException e) {
            final String message = e.getMessage();
            if (message.contains("Connect Error")) {
                throw new ChaiUnavailableException(message, ChaiError.COMMUNICATION, false, false);
            }
            throw ChaiUnavailableException.forErrorMessage(message);
        }
    }

// --------------------------- CONSTRUCTORS ---------------------------

    JLDAPProviderImpl()
    {
        super();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ChaiProvider ---------------------

    public void close()
    {
        if (ldapConnection != null) {
            try {
                ldapConnection.disconnect();
            } catch (LDAPException e) {
                LOGGER.warn("error closing connection", e);
            }
        }
        super.close();
    }

    @ChaiProviderImplementor.LdapOperation
    public boolean compareStringAttribute(final String entryDN, final String attribute, final String value)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.compareStringAttribute(entryDN, attribute, value);

        final LDAPAttribute ldapAttr = new LDAPAttribute(attribute, value);
        try {
            return ldapConnection.compare(entryDN, ldapAttr);
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void createEntry(final String entryDN, final String baseObjectClass, final Map<String,String> stringAttributes)
            throws ChaiOperationException
    {
        INPUT_VALIDATOR.createEntry(entryDN, baseObjectClass, stringAttributes);
        this.createEntry(entryDN, Collections.singleton(baseObjectClass), stringAttributes);
    }

    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void createEntry(final String entryDN, final Set<String> baseObjectClasses, final Map<String,String> stringAttributes)
            throws ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.createEntry(entryDN, baseObjectClasses, stringAttributes);

        final LDAPAttributeSet ldapAttributeSet = new LDAPAttributeSet();
        ldapAttributeSet.add(new LDAPAttribute(ChaiConstant.ATTR_LDAP_OBJECTCLASS, baseObjectClasses.toArray(new String[baseObjectClasses.size()])));
        if (stringAttributes != null) {
            for (final String attrName : stringAttributes.keySet()) {
                ldapAttributeSet.add(new LDAPAttribute(attrName, stringAttributes.get(attrName)));
            }
        }
        final LDAPEntry newEntry = new LDAPEntry(entryDN, ldapAttributeSet);
        try {
            ldapConnection.add(newEntry);
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void deleteEntry(final String entryDN)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.deleteEntry(entryDN);

        try {
            ldapConnection.delete(entryDN);
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void deleteStringAttributeValue(final String entryDN, final String attribute, final String value)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.deleteStringAttributeValue(entryDN, attribute, value);

        final LDAPAttribute ldapAttr = new LDAPAttribute(attribute, value);
        final LDAPModification mod = new LDAPModification(LDAPModification.DELETE, ldapAttr);

        try {
            ldapConnection.modify(entryDN, mod);
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @ChaiProviderImplementor.LdapOperation
    public ExtendedResponse extendedOperation(final ExtendedRequest request)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.extendedOperation(request);
        preCheckExtendedOperation(request);

        final String oid = request.getID();
        final byte[] value = request.getEncodedValue();
        final LDAPExtendedOperation ldapOper = new LDAPExtendedOperation(oid, value);
        final LDAPExtendedResponse ldapResponse;
        try {
            ldapResponse = ldapConnection.extendedOperation(ldapOper);
        } catch (LDAPException e) {
            cacheExtendedOperationException(request,e);
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }

        try {
            return request.createExtendedResponse(ldapResponse.getID(), ldapResponse.getValue(), 0, ldapResponse.getValue().length);
        } catch (NamingException e) {
            throw new RuntimeException("unknown error while converting ldap extended response " + e.getMessage(), e);
        }
    }

    public ProviderStatistics getProviderStatistics()
    {
        return null;
    }

    @ChaiProviderImplementor.LdapOperation
    public byte[][] readMultiByteAttribute(final String entryDN, final String attribute)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.readMultiByteAttribute(entryDN, attribute);

        try {
            final LDAPEntry entry = ldapConnection.read(entryDN, new String[]{attribute});
            final LDAPAttribute ldapAttribute = entry.getAttribute(attribute);
            return ldapAttribute != null ? ldapAttribute.getByteValueArray() : new byte[0][0];
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @ChaiProviderImplementor.LdapOperation
    public Set<String> readMultiStringAttribute(final String entryDN, final String attribute)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.readMultiStringAttribute(entryDN, attribute);

        try {
            final LDAPEntry entry = ldapConnection.read(entryDN, new String[]{attribute});
            final LDAPAttribute ldapAttribute = entry.getAttribute(attribute);
            if (ldapAttribute == null) {
                return Collections.emptySet();
            } else {
                return new HashSet<String>(Arrays.asList(ldapAttribute.getStringValueArray()));
            }
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @ChaiProviderImplementor.LdapOperation
    public String readStringAttribute(final String entryDN, final String attribute)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.readMultiStringAttribute(entryDN, attribute);

        return readStringAttributes(entryDN, Collections.singleton(attribute)).get(attribute);
    }

    @ChaiProviderImplementor.LdapOperation
    public Map<String,String> readStringAttributes(final String entryDN, final Set<String> attributes)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.readStringAttributes(entryDN, attributes);

        final Map<String,String> returnProps = new LinkedHashMap<String, String>();
        try {
            final LDAPEntry entry = ldapConnection.read(entryDN, attributes.toArray(new String[attributes.size()]));

            for (final Object attr : entry.getAttributeSet()) {
                final LDAPAttribute lAttr = (LDAPAttribute) attr;
                returnProps.put(lAttr.getName(), lAttr.getStringValue());
            }

            return returnProps;
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void replaceStringAttribute(final String entryDN, final String attributeName, final String oldValue, final String newValue)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.replaceStringAttribute(entryDN, attributeName, oldValue, newValue);

        final LDAPModification[] modifications;

        if (oldValue == null) {
            modifications = new LDAPModification[]{new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute(attributeName, newValue))};
        } else {
            modifications = new LDAPModification[2];
            modifications[0] = new LDAPModification(LDAPModification.DELETE, new LDAPAttribute(attributeName, oldValue));
            modifications[1] = new LDAPModification(LDAPModification.ADD, new LDAPAttribute(attributeName, newValue));
        }

        try {
            ldapConnection.modify(entryDN, modifications);
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @ChaiProviderImplementor.LdapOperation
    public Map<String, Map<String,String>> search(final String baseDN, final SearchHelper searchHelper)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.search(baseDN, searchHelper);

        final Map<String, Map<String, List<String>>> firstMap = searchImpl(baseDN, searchHelper, true);

        final Map<String, Map<String,String>> returnMap = new LinkedHashMap<String, Map<String,String>>();
        for (final String dn : firstMap.keySet()) {
            final Map<String, List<String>> loopAttrs = firstMap.get(dn);
            final Map<String, String> attrProps = new LinkedHashMap<String, String>();
            for (final String loopAttr : loopAttrs.keySet()) {
                attrProps.put(loopAttr, loopAttrs.get(loopAttr).get(0));
            }
            returnMap.put(dn, attrProps);
        }
        return returnMap;
    }

    @ChaiProviderImplementor.LdapOperation
    public Map<String, Map<String,String>> search(final String baseDN, final String filter, final Set<String> attributes, final SEARCH_SCOPE searchScope)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.search(baseDN, filter, attributes, searchScope);

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter(filter);
        searchHelper.setAttributes(attributes);
        searchHelper.setSearchScope(searchScope);

        return search(baseDN, searchHelper);
    }

    @ChaiProviderImplementor.LdapOperation
    public Map<String, Map<String, List<String>>> searchMultiValues(
            final String baseDN,
            final SearchHelper searchHelper)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.searchMultiValues(baseDN, searchHelper);

        return searchImpl(baseDN, searchHelper, false);
    }

    @ChaiProviderImplementor.LdapOperation
    public Map<String, Map<String, List<String>>> searchMultiValues(
            final String baseDN,
            final String filter,
            final Set<String> attributes,
            final SEARCH_SCOPE searchScope)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.searchMultiValues(baseDN, filter, attributes, searchScope);

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter(filter);
        searchHelper.setAttributes(attributes);
        searchHelper.setSearchScope(searchScope);

        return searchImpl(baseDN, searchHelper, false);
    }

    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void writeBinaryAttribute(final String entryDN, final String attribute, final byte[][] values, final boolean overwrite)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.writeBinaryAttribute(entryDN, attribute, values, overwrite);

        final LDAPAttribute ldapAttr = new LDAPAttribute(attribute);

        for (final byte[] value : values) {
            ldapAttr.addValue(value);
        }

        final LDAPModification mod = new LDAPModification(overwrite ? LDAPModification.REPLACE : LDAPModification.ADD, ldapAttr);
        try {
            ldapConnection.modify(entryDN, mod);
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void replaceBinaryAttribute(final String entryDN, final String attribute, final byte[] oldValue, final byte[] newValue)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.replaceBinaryAttribute(entryDN, attribute, oldValue, newValue);

        final LDAPModification[] modifications;

        if (oldValue == null) {
            modifications = new LDAPModification[]{new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute(attribute, newValue))};
        } else {
            modifications = new LDAPModification[2];
            modifications[0] = new LDAPModification(LDAPModification.DELETE, new LDAPAttribute(attribute, oldValue));
            modifications[1] = new LDAPModification(LDAPModification.ADD, new LDAPAttribute(attribute, newValue));
        }

        try {
            ldapConnection.modify(entryDN, modifications);
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @ChaiProviderImplementor.LdapOperation
    @ChaiProviderImplementor.ModifyOperation
    public void writeStringAttribute(final String entryDN, final String attribute, final Set<String> values, final boolean overwrite)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        INPUT_VALIDATOR.writeStringAttribute(entryDN, attribute, values, overwrite);

        final LDAPAttribute ldapAttr = new LDAPAttribute(attribute, values.toArray(new String[values.size()]));
        final LDAPModification mod = new LDAPModification(overwrite ? LDAPModification.REPLACE : LDAPModification.ADD, ldapAttr);
        try {
            ldapConnection.modify(entryDN, mod);
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

    @LdapOperation
    @ModifyOperation
    public final void writeStringAttributes(final String entryDN, final Map<String,String> attributeValues, final boolean overwrite)
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        INPUT_VALIDATOR.writeStringAttributes(entryDN, attributeValues, overwrite);


        final int modOption = overwrite ? LDAPModification.REPLACE : LDAPModification.ADD;

        final List<LDAPModification> modifications = new ArrayList<LDAPModification>();
        for (final String attrName : attributeValues.keySet()) {
            final LDAPAttribute ldapAttr = new LDAPAttribute(attrName, attributeValues.get(attrName));
            final LDAPModification mod = new LDAPModification(modOption, ldapAttr);
            modifications.add(mod);
        }

        final LDAPModification[] modificationArray = modifications.toArray(new LDAPModification[modifications.size()]);

        try {
            ldapConnection.modify(entryDN, modificationArray);
        } catch (LDAPException e) {
            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
    }

// --------------------- Interface ChaiProviderImplementor ---------------------

    public Object getConnectionObject()
            throws Exception
    {
        return ldapConnection;
    }

    public String getCurrentConnectionURL()
    {
        if (ldapConnection == null || !ldapConnection.isConnected()) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("ldap");
        if (ldapConnection.isTLS()) {
            sb.append("s");
        }
        sb.append("://");
        sb.append(ldapConnection.getHost());
        sb.append(":");
        sb.append(ldapConnection.getPort());
        return sb.toString();
    }

// -------------------------- OTHER METHODS --------------------------

    public Map<String, Map<String, List<String>>> searchImpl(
            String baseDN,
            SearchHelper searchHelper,
            final boolean onlyFirstValue)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
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


        final int ldapScope;
        switch (searchHelper.getSearchScope()) {
            case ONE:
                ldapScope = LDAPConnection.SCOPE_ONE;
                break;
            case BASE:
                ldapScope = LDAPConnection.SCOPE_BASE;
                break;
            case SUBTREE:
                ldapScope = LDAPConnection.SCOPE_SUB;
                break;
            default:
                ldapScope = -1;
        }

        final Map<String, Map<String, List<String>>> returnMap = new LinkedHashMap<String, Map<String, List<String>>>();

        final LDAPSearchConstraints constraints = new LDAPSearchConstraints();
        constraints.setMaxResults(searchHelper.getMaxResults());
        constraints.setTimeLimit(searchHelper.getTimeLimit());

        final String[] returnAttributes = searchHelper.getAttributes() == null ? null : searchHelper.getAttributes().toArray(new String[searchHelper.getAttributes().size()]);

        final LDAPSearchResults results;
        try {
            results = ldapConnection.search(
                    baseDN,
                    ldapScope, searchHelper.getFilter(),
                    returnAttributes,
                    false,
                    constraints
            );

            while (results.hasMore()) {
                final LDAPEntry loopEntry = results.next();
                final String loopDN = loopEntry.getDN();
                final Map<String, List<String>> loopAttributes = new LinkedHashMap<String, List<String>>();
                final LDAPAttributeSet attrSet = loopEntry.getAttributeSet();
                for (final Object anAttrSet : attrSet) {
                    final LDAPAttribute loopAttr = (LDAPAttribute) anAttrSet;
                    if (onlyFirstValue) {
                        loopAttributes.put(loopAttr.getName(), Collections.singletonList(loopAttr.getStringValue()));
                    } else {
                        loopAttributes.put(loopAttr.getName(), Arrays.asList(loopAttr.getStringValueArray()));
                    }
                }
                returnMap.put(loopDN, loopAttributes);
            }
        } catch (LDAPException e) {
            // check to see if there any results. If there are results, then
            // return them.  If no results, then throw the exception.  Most likely
            // cause of results+exception is search size/time exceeded.

            if (!returnMap.isEmpty()) {
                return Collections.unmodifiableMap(returnMap);
            }

            throw ChaiOperationException.forErrorMessage(e.getLDAPErrorMessage());
        }
        return Collections.unmodifiableMap(returnMap);
    }

    public boolean isConnected() {
        return ldapConnection != null && ldapConnection.isConnected();
    }
}
