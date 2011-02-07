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

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.generic.entry.GenericEntryFactory;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.ChaiUtility;
import com.novell.ldapchai.util.SearchHelper;

import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.util.*;

abstract class AbstractProvider implements ChaiProvider, ChaiProviderImplementor {
// ----------------------------- CONSTANTS ----------------------------

    public static final ChaiProviderInputValidator INPUT_VALIDATOR = new ChaiProviderInputValidator();

// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(AbstractProvider.class.getName());

    private volatile static long instanceCounter;

    protected ChaiConfiguration chaiConfig;
    protected volatile ConnectionState state = ConnectionState.NEW;
    protected long instanceCount;

    protected Map<String,Object> providerProperties = new HashMap<String,Object>();
    private DIRECTORY_VENDOR cachedDirectoryVendor;


// -------------------------- STATIC METHODS --------------------------

    static String methodToDebugStr(final Method theMethod, final Object... parameters)
    {
        final StringBuilder debugStr = new StringBuilder();
        debugStr.append(theMethod.getName());
        debugStr.append('(');
        for (Iterator iter = Arrays.asList(parameters).iterator(); iter.hasNext();) {
            final Object nextValue = iter.next();
            if (nextValue == null) {
                debugStr.append("null");
            } else if (nextValue.getClass() == (new Object[0]).getClass()) {
                debugStr.append("array");
            } else {
                debugStr.append(nextValue.toString());
            }
            if (iter.hasNext()) {
                debugStr.append(',');
            }
        }
        debugStr.append(')');

        return debugStr.toString();
    }

    public Map<String, Object> getProviderProperties()
    {
        return providerProperties;
    }

    // --------------------------- CONSTRUCTORS ---------------------------

    public AbstractProvider()
    {
        instanceCounter++;
        instanceCount = instanceCounter;

        {  // populate the extended for 
            final Map<String, Exception> cacheFailureMap = new HashMap<String,Exception>();
            getProviderProperties().put(EXTENDED_FAILURE_CACHE_KEY, cacheFailureMap);
        }

    }

// ------------------------ CANONICAL METHODS ------------------------

    /**
     * Return a debug string with connection and state information.
     *
     * @return a string suitable for debug logging
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ChaiProvider ");
        sb.append("#").append(instanceCount).append(" ");
        sb.append("(").append(this.getClass().getSimpleName()).append("), ");
        sb.append(getConnectionState()).append(" ");
        if (getConnectionState().equals(ChaiProviderImplementor.ConnectionState.OPEN)) {
            sb.append(this.getCurrentConnectionURL());
            final String bindDN = this.getChaiConfiguration().getSetting(ChaiSetting.BIND_DN);
            if (bindDN != null && bindDN.length() > 1) {
                sb.append(" ");
                sb.append(bindDN);
            }
        }
        return sb.toString();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ChaiProvider ---------------------

    public void close()
    {
        this.state = ConnectionState.CLOSED;
    }

    public ChaiConfiguration getChaiConfiguration()
    {
        return chaiConfig;
    }

// --------------------- Interface ChaiProviderImplementor ---------------------

    public ConnectionState getConnectionState()
    {
        return state;
    }



    public void init(final ChaiConfiguration chaiConfiguration)
            throws ChaiUnavailableException
    {
        if (state == ConnectionState.OPEN) {
            throw new IllegalStateException("provider already initialized");
        }
        if (state == ConnectionState.CLOSED) {
            throw new IllegalStateException("instance has been closed");
        }
        this.chaiConfig = chaiConfiguration;
        state = ConnectionState.OPEN;
    }

// -------------------------- OTHER METHODS --------------------------

    protected void activityPreCheck()
    {
        if (state == ConnectionState.NEW) {
            throw new IllegalStateException("ChaiProvider instance is not yet initialized");
        }

        if (state == ConnectionState.CLOSED) {
            throw new IllegalStateException("ChaiProvider instance has been closed");
        }
    }

// -------------------------- INNER CLASSES --------------------------

        public boolean errorIsRetryable(final Exception e)
        {

            if (e instanceof ChaiUnavailableException) {
                return !((ChaiUnavailableException)e).isPermenant();
            } else if (e instanceof ChaiOperationException) {
                return !((ChaiOperationException)e).isPermenant();
            } else {
                final String errorMsg = e.getMessage();
                return !new ChaiOperationException(errorMsg, ChaiError.UNKNOWN).isPermenant();
            }
        }

    protected static class PromiscuousSSLSocketFactory extends SSLSocketFactory {
        static SSLSocketFactory wrappedSSLSocketFactory = null;

        static {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers()
                        {
                            return null;
                        }

                        public void checkClientTrusted(
                                final java.security.cert.X509Certificate[] certs, final String authType)
                        {
                        }

                        public void checkServerTrusted(
                                final java.security.cert.X509Certificate[] certs, final String authType)
                        {
                        }
                    }
            };

            try {
                final SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                wrappedSSLSocketFactory = sc.getSocketFactory();
            } catch (Exception e) {
                // nothing to do
            }
        }

        public PromiscuousSSLSocketFactory()
        {
            super();
        }

        public Socket createSocket()
                throws IOException
        {
            return wrappedSSLSocketFactory.createSocket();
        }

        public String[] getDefaultCipherSuites()
        {
            return wrappedSSLSocketFactory.getDefaultCipherSuites();
        }

        public String[] getSupportedCipherSuites()
        {
            return wrappedSSLSocketFactory.getSupportedCipherSuites();
        }

        public Socket createSocket(final Socket socket, final String string, final int i, final boolean b)
                throws IOException
        {
            return wrappedSSLSocketFactory.createSocket(socket, string, i, b);
        }

        public Socket createSocket(final String string, final int i)
                throws IOException
        {
            return wrappedSSLSocketFactory.createSocket(string, i);
        }

        public Socket createSocket(final String string, final int i, final InetAddress inetAddress, final int i1)
                throws IOException
        {
            return wrappedSSLSocketFactory.createSocket(string, i, inetAddress, i1);
        }

        public Socket createSocket(final InetAddress inetAddress, final int i)
                throws IOException
        {
            return wrappedSSLSocketFactory.createSocket(inetAddress, i);
        }

        public Socket createSocket(final InetAddress inetAddress, final int i, final InetAddress inetAddress1, final int i1)
                throws IOException
        {
            return wrappedSSLSocketFactory.createSocket(inetAddress, i, inetAddress1, i1);
        }

        public static javax.net.SocketFactory getDefault()
        {
            return new PromiscuousSSLSocketFactory();
        }
    }

    public static class ChaiProviderInputValidator implements ChaiProvider {
        public final void close()
        {
        }

        public ProviderStatistics getProviderStatistics()
        {
            return null;
        }

        public final boolean compareStringAttribute(final String entryDN, final String attributeName, final String value)
        {
            if (attributeName == null) {
                throw new NullPointerException("attributeName must not be null");
            }

            if (value == null) {
                throw new NullPointerException("value must not be null");
            }

            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }

            return false;
        }

        public final void createEntry(final String entryDN, final String baseObjectClass, final Properties stringAttributes)
        {
            if (baseObjectClass == null) {
                throw new NullPointerException("baseObjectClass must not be null");
            }

            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }
        }

        public final void deleteEntry(final String entryDN)
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }
        }

        public final void deleteStringAttributeValue(final String entryDN, final String attributeName, final String attributeValue)
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }
            if (attributeName == null) {
                throw new NullPointerException("attributeName must not be null");
            }
        }

        public final ExtendedResponse extendedOperation(final ExtendedRequest request)
        {
            if (request == null) {
                throw new NullPointerException("request must not be null");
            }

            return null;
        }

        public ChaiConfiguration getChaiConfiguration()
        {
            return null;
        }

        public void init(final ChaiConfiguration chaiConfig)
                throws ChaiUnavailableException, IllegalStateException
        {
        }

        public final byte[][] readMultiByteAttribute(final String entryDN, final String attributeName)
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }
            if (attributeName == null) {
                throw new NullPointerException("attributeName must not be null");
            }
            return null;
        }

        public final Set<String> readMultiStringAttribute(final String entryDN, final String attributeName)
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }
            if (attributeName == null) {
                throw new NullPointerException("attributeName must not be null");
            }

            return null;
        }

        public final String readStringAttribute(final String entryDN, final String attributeName)
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }

            if (attributeName == null) {
                throw new NullPointerException("attributeName must not be null");
            }

            return null;
        }

        public final Properties readStringAttributes(final String entryDN, final String[] attributes)
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }

            return null;
        }

        public final void replaceStringAttribute(final String entryDN, final String attributeName, final String oldValue, final String newValue)
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }
            if (attributeName == null) {
                throw new NullPointerException("attributeName must not be null");
            }
            if (oldValue == null) {
                throw new NullPointerException("oldValue must not be null");
            }
            if (newValue == null) {
                throw new NullPointerException("newValue must not be null");
            }
        }

        public final Map<String, Properties> search(final String baseDN, final String filter)
                throws ChaiUnavailableException, ChaiOperationException
        {
            if (baseDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }

            return null;
        }

        public final Map<String, Properties> search(final String baseDN, final SearchHelper searchHelper)
        {
            return null;
        }

        public final Map<String, Properties> search(final String baseDN, final String filter, final String[] attributes)
        {
            return null;
        }

        public final Map<String, Properties> search(final String baseDN, final String filter, final String[] attributes, final ChaiProvider.SEARCH_SCOPE searchScope)
        {
            return null;
        }

        public final Map<String, Map<String, List<String>>> searchMultiValues(final String baseDN, final String filter, final String[] attributes, final ChaiProvider.SEARCH_SCOPE searchScope)
        {
            return null;
        }

        public final Map<String, Map<String, List<String>>> searchMultiValues(final String baseDN, final SearchHelper searchHelper)
        {
            return null;
        }

        public final void writeBinaryAttribute(
                final String entryDN,
                final String attributeName,
                final byte[][] values,
                final boolean overwrite
        )
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }
            if (attributeName == null) {
                throw new NullPointerException("attributeName must not be null");
            }
            if (values == null) {
                throw new NullPointerException("values must not be null");
            }
        }

        public final void writeStringAttribute(final String entryDN, final String attributeName, final String[] values, final boolean overwrite)
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }
            if (attributeName == null) {
                throw new NullPointerException("attributeName must not be null");
            }
            if (values == null) {
                throw new NullPointerException("value must not be null");
            }
        }

        public final void writeStringAttributes(final String entryDN, final Properties attributeValueProps, final boolean overwrite)
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }
            if (attributeValueProps == null) {
                throw new NullPointerException("attributeValueProps must not be null");
            }
            if (attributeValueProps.isEmpty()) {
                throw new NullPointerException("attributeValueProps must not be empty");
            }
        }

        public DIRECTORY_VENDOR getDirectoryVendor() throws ChaiUnavailableException {
            return null;
        }

        public void replaceBinaryAttribute(
                String entryDN,
                String attributeName,
                byte[] oldValue,
                byte[] newValue
        )
                throws ChaiUnavailableException, ChaiOperationException
        {
            if (entryDN == null) {
                throw new NullPointerException("entryDN must not be null");
            }
            if (attributeName == null) {
                throw new NullPointerException("attributeName must not be null");
            }
            if (oldValue == null) {
                throw new NullPointerException("oldValue must not be null");
            }
            if (newValue == null) {
                throw new NullPointerException("newValue must not be null");
            }
        }
    }

    protected static final String EXTENDED_FAILURE_CACHE_KEY = "extendedFailureCache";

    protected void preCheckExtendedOperation(final ExtendedRequest request)
            throws ChaiOperationException
    {
        final boolean cacheFailures = "true".equalsIgnoreCase(this.getChaiConfiguration().getSetting(ChaiSetting.EXTENDED_OPERATION_FAILURE_CACHE));
        if (cacheFailures) {
            final Map<String, Object> providerProps = this.getProviderProperties();
            final Map<String, Exception> cacheFailureMap = (Map<String,Exception>)providerProps.get(EXTENDED_FAILURE_CACHE_KEY);
            final String requestID = request.getID();
            if (cacheFailureMap.containsKey(requestID)) {
                LOGGER.debug("previous extended operation request for " + requestID + " has failed, reissuing cached exception without attempting operation");
                throw (ChaiOperationException)cacheFailureMap.get(requestID);
            }
        }
    }

    protected void cacheExtendedOperationException(final ExtendedRequest request, final Exception e)
            throws ChaiOperationException
    {
        final boolean cacheFailures = "true".equalsIgnoreCase(this.getChaiConfiguration().getSetting(ChaiSetting.EXTENDED_OPERATION_FAILURE_CACHE));
        if (cacheFailures) {
            final ChaiOperationException opExcep = ChaiOperationException.forErrorMessage(e.getMessage());
            if (opExcep.getErrorCode() == ChaiError.UNSUPPORTED_OPERATION) {
                final Map<String, Object> providerProps = this.getProviderProperties();
                final Map<String, Exception> cacheFailureMap = (Map<String,Exception>)providerProps.get(EXTENDED_FAILURE_CACHE_KEY);
                final String requestID = request.getID();
                cacheFailureMap.put(requestID, opExcep);
                LOGGER.trace("caching extended operation for " + requestID);
                throw opExcep;
            }
        }
    }

    public DIRECTORY_VENDOR getDirectoryVendor()
            throws ChaiUnavailableException
    {
        if (cachedDirectoryVendor == null) {

            final String defaultVendor = this.getChaiConfiguration().getSetting(ChaiSetting.DEFAULT_VENDOR);
            if (defaultVendor != null) {
                for (final ChaiProvider.DIRECTORY_VENDOR vendor : ChaiProvider.DIRECTORY_VENDOR.values()) {
                    if (vendor.toString().equals(defaultVendor)) {
                        cachedDirectoryVendor = vendor;
                        return vendor;
                    }
                }
            }

            try {
                final ChaiConfiguration rootDSEChaiConfig = (ChaiConfiguration)this.getChaiConfiguration().clone();
                final String ldapUrls = rootDSEChaiConfig.getSetting(ChaiSetting.BIND_URLS);
                final String[] splitUrls = ldapUrls.split(ChaiConfiguration.LDAP_URL_SEPERATOR_REGEX_PATTERN);
                final StringBuilder newUrlConfig = new StringBuilder();
                boolean currentURLsHavePath = false;

                for (int i = 0; i < splitUrls.length; i++) {
                    final URI uri = URI.create(splitUrls[i]);
                    final String newURI = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
                    newUrlConfig.append(newURI);
                    if (uri.getPath() != null && uri.getPath().length() > 0) {
                        currentURLsHavePath = true;
                    }

                    if (i != splitUrls.length) {
                        newUrlConfig.append(",");
                    }
                }

                rootDSEChaiConfig.setSetting(ChaiSetting.BIND_URLS,newUrlConfig.toString());
                final ChaiProvider rootDseProvider = currentURLsHavePath ? ChaiProviderFactory.createProvider(rootDSEChaiConfig) : this;

                // can not call the ChaiFactory here, because ChaiFactory in turn calls this method to get the
                // directory vendor.  Instead, we will go directly to the Generic ChaiFactory

                final GenericEntryFactory genericEntryFactory = new GenericEntryFactory();
                final ChaiEntry rootDseEntry = genericEntryFactory.createChaiEntry("",rootDseProvider);
                cachedDirectoryVendor = ChaiUtility.determineDirectoryVendor(rootDseEntry);
            } catch (ChaiOperationException e) {
                LOGGER.warn("error while attempting to determine directory vendor: " + e.getMessage());
                cachedDirectoryVendor = DIRECTORY_VENDOR.GENERIC;
            } catch (CloneNotSupportedException e) {
                LOGGER.warn("error while attempting to determine directory vendor: " + e.getMessage());
                cachedDirectoryVendor = DIRECTORY_VENDOR.GENERIC;
            }
        }

        return cachedDirectoryVendor;
    }

}
