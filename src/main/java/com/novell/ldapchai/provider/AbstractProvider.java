/*
 * LDAP Chai API
 * Copyright (c) 2006-2017 Novell, Inc.
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
import com.novell.ldapchai.ChaiEntryFactory;
import com.novell.ldapchai.ChaiRequestControl;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiException;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.ChaiUtility;
import com.novell.ldapchai.util.SearchHelper;
import com.novell.ldapchai.util.internal.ChaiLogger;

import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

abstract class AbstractProvider implements ChaiProvider, ChaiProviderImplementor
{
    private static final ChaiProviderInputValidator INPUT_VALIDATOR = new ChaiProviderInputValidator();

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( AbstractProvider.class );

    protected ChaiConfiguration chaiConfig;
    private ChaiProviderFactory providerFactory;
    protected volatile ConnectionState state = ConnectionState.NEW;

    private final Map<String, Object> cacheFailureMap = new HashMap<>();
    private DirectoryVendor cachedDirectoryVendor;

    private static final AtomicInteger ID_COUNTER = new AtomicInteger( 0 );
    private final int counter = ID_COUNTER.getAndIncrement();

    AbstractProvider()
    {
    }

    /**
     * Return a debug string with connection and state information.
     *
     * @return a string suitable for debug logging
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ChaiProvider " );
        sb.append( "#" ).append( counter ).append( " " );
        sb.append( "(" ).append( this.getClass().getSimpleName() ).append( "), " );
        sb.append( getConnectionState() ).append( " " );
        if ( getConnectionState().equals( ChaiProviderImplementor.ConnectionState.OPEN ) )
        {
            sb.append( this.getCurrentConnectionURL() );
            final String bindDN = this.getChaiConfiguration().getSetting( ChaiSetting.BIND_DN );
            if ( bindDN != null && bindDN.length() > 1 )
            {
                sb.append( " " );
                sb.append( bindDN );
            }
        }
        return sb.toString();
    }

    @Override
    public void close()
    {
        this.state = ConnectionState.CLOSED;
        this.providerFactory.getCentralService().deRegisterProvider( this );
    }

    @Override
    public ChaiConfiguration getChaiConfiguration()
    {
        return chaiConfig;
    }

    @Override
    public ConnectionState getConnectionState()
    {
        return state;
    }

    @Override
    public void init( final ChaiConfiguration chaiConfiguration, final ChaiProviderFactory providerFactory )
            throws ChaiUnavailableException
    {
        if ( state == ConnectionState.OPEN )
        {
            throw new IllegalStateException( "provider already initialized" );
        }
        if ( state == ConnectionState.CLOSED )
        {
            throw new IllegalStateException( "instance has been closed" );
        }
        this.providerFactory = providerFactory;
        this.chaiConfig = chaiConfiguration;
        state = ConnectionState.OPEN;
        incrementBindStatistic();
    }

    protected void activityPreCheck()
    {
        if ( state == ConnectionState.NEW )
        {
            throw new IllegalStateException( "ChaiProvider instance is not yet initialized" );
        }

        if ( state == ConnectionState.CLOSED )
        {
            throw new IllegalStateException( "ChaiProvider instance has been closed" );
        }
    }

    @Override
    public boolean errorIsRetryable( final Exception e )
    {
        if ( e instanceof IOException )
        {
            return true;
        }
        else if ( e instanceof ChaiException )
        {
            return !( ( ( ChaiException ) e ).isPermanent() );
        }

        return false;
    }

    static class PromiscuousTrustManager implements X509TrustManager
    {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }

        @Override
        public void checkClientTrusted(
                final java.security.cert.X509Certificate[] certs, final String authType )
        {
        }

        @Override
        public void checkServerTrusted(
                final java.security.cert.X509Certificate[] certs, final String authType )
        {
        }
    }

    public static class ChaiProviderInputValidator implements ChaiProvider
    {
        @Override
        public final void close()
        {
        }

        @Override
        public boolean isConnected()
        {
            return false;
        }

        @Override
        public ProviderStatistics getProviderStatistics()
        {
            return null;
        }

        @Override
        public final boolean compareStringAttribute( final String entryDN, final String attributeName, final String value )
        {
            if ( attributeName == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }

            if ( value == null )
            {
                throw new NullPointerException( "value must not be null" );
            }

            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }

            return false;
        }

        @Override
        public final void createEntry( final String entryDN, final String baseObjectClass, final Map<String, String> stringAttributes )
        {
            if ( baseObjectClass == null )
            {
                throw new NullPointerException( "baseObjectClass must not be null" );
            }

            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
        }

        @Override
        public final void createEntry( final String entryDN, final Set<String> baseObjectClasses, final Map<String, String> stringAttributes )
        {
            if ( baseObjectClasses == null )
            {
                throw new NullPointerException( "baseObjectClass must not be null" );
            }

            if ( baseObjectClasses.isEmpty() )
            {
                throw new NullPointerException( "baseObjectClass must not be empty" );
            }

            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
        }

        @Override
        public final void renameEntry( final String entryDN, final String newRDN, final String newParentDN )
        {
            if ( newParentDN == null )
            {
                throw new NullPointerException( "newParentDN must not be null" );
            }

            if ( newRDN == null )
            {
                throw new NullPointerException( "newRDN must not be null" );
            }

            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
        }

        @Override
        public final void deleteEntry( final String entryDN )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
        }

        @Override
        public final void deleteStringAttributeValue( final String entryDN, final String attributeName, final String attributeValue )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
            if ( attributeName == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }
        }

        @Override
        public final ExtendedResponse extendedOperation( final ExtendedRequest request )
        {
            if ( request == null )
            {
                throw new NullPointerException( "request must not be null" );
            }

            return null;
        }

        @Override
        public ChaiConfiguration getChaiConfiguration()
        {
            return null;
        }

        public void init( final ChaiConfiguration chaiConfig )
                throws ChaiUnavailableException, IllegalStateException
        {
        }

        @Override
        public final byte[][] readMultiByteAttribute( final String entryDN, final String attributeName )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
            if ( attributeName == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }
            return null;
        }

        @Override
        public final Set<String> readMultiStringAttribute( final String entryDN, final String attributeName )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
            if ( attributeName == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }

            return null;
        }

        @Override
        public final String readStringAttribute( final String entryDN, final String attributeName )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }

            if ( attributeName == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }

            return null;
        }

        @Override
        public final Map<String, String> readStringAttributes( final String entryDN, final Set<String> attributes )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }

            return null;
        }

        @Override
        public final void replaceStringAttribute( final String entryDN, final String attributeName, final String oldValue, final String newValue )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
            if ( attributeName == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }
            if ( oldValue == null )
            {
                throw new NullPointerException( "oldValue must not be null" );
            }
            if ( newValue == null )
            {
                throw new NullPointerException( "newValue must not be null" );
            }
        }

        @Override
        public final Map<String, Map<String, String>> search( final String baseDN, final SearchHelper searchHelper )
        {
            if ( baseDN == null )
            {
                throw new NullPointerException( "baseDN must not be null" );
            }
            if ( searchHelper == null )
            {
                throw new NullPointerException( "searchHelper must not be null" );
            }
            return null;
        }

        @Override
        public final Map<String, Map<String, String>> search( final String baseDN, final String filter, final Set<String> attributes, final SearchScope searchScope )
        {
            if ( baseDN == null )
            {
                throw new NullPointerException( "baseDN must not be null" );
            }
            if ( filter == null )
            {
                throw new NullPointerException( "filter must not be null" );
            }
            if ( searchScope == null )
            {
                throw new NullPointerException( "searchScope must not be null" );
            }
            return null;
        }

        @Override
        public final Map<String, Map<String, List<String>>> searchMultiValues( final String baseDN, final SearchHelper searchHelper )
        {
            if ( baseDN == null )
            {
                throw new NullPointerException( "baseDN must not be null" );
            }
            if ( searchHelper == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }
            return null;
        }

        @Override
        public final Map<String, Map<String, List<String>>> searchMultiValues(
                final String baseDN, final String filter,
                final Set<String> attributes,
                final SearchScope searchScope
        )
        {
            if ( baseDN == null )
            {
                throw new NullPointerException( "baseDN must not be null" );
            }
            if ( filter == null )
            {
                throw new NullPointerException( "filter must not be null" );
            }
            if ( searchScope == null )
            {
                throw new NullPointerException( "searchScope must not be null" );
            }
            return null;
        }

        @Override
        public final void writeBinaryAttribute(
                final String entryDN,
                final String attributeName,
                final byte[][] values,
                final boolean overwrite
        )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
            if ( attributeName == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }
            if ( values == null )
            {
                throw new NullPointerException( "values must not be null" );
            }
        }

        @Override
        public final void writeBinaryAttribute(
                final String entryDN,
                final String attributeName,
                final byte[][] values,
                final boolean overwrite,
                final ChaiRequestControl[] controls
        )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
            if ( attributeName == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }
            if ( values == null )
            {
                throw new NullPointerException( "values must not be null" );
            }
        }

        @Override
        public final void writeStringAttribute( final String entryDN, final String attributeName, final Set<String> values, final boolean overwrite )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
            if ( attributeName == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }
            if ( values == null )
            {
                throw new NullPointerException( "value must not be null" );
            }
        }

        @Override
        public final void writeStringAttributes( final String entryDN, final Map<String, String> attributeValues, final boolean overwrite )
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
            if ( attributeValues == null )
            {
                throw new NullPointerException( "attributeValues must not be null" );
            }
            if ( attributeValues.isEmpty() )
            {
                throw new NullPointerException( "attributeValues must not be empty" );
            }
        }

        @Override
        public DirectoryVendor getDirectoryVendor()
                throws ChaiUnavailableException
        {
            return null;
        }

        @Override
        public void replaceBinaryAttribute(
                final String entryDN,
                final String attributeName,
                final byte[] oldValue,
                final byte[] newValue
        )
                throws ChaiUnavailableException, ChaiOperationException
        {
            if ( entryDN == null )
            {
                throw new NullPointerException( "entryDN must not be null" );
            }
            if ( attributeName == null )
            {
                throw new NullPointerException( "attributeName must not be null" );
            }
            if ( oldValue == null )
            {
                throw new NullPointerException( "oldValue must not be null" );
            }
            if ( newValue == null )
            {
                throw new NullPointerException( "newValue must not be null" );
            }
        }

        @Override
        public ChaiEntryFactory getEntryFactory()
        {
            return null;
        }

        @Override
        public ChaiProviderFactory getProviderFactory()
        {
            return null;
        }
    }

    protected void preCheckExtendedOperation( final ExtendedRequest request )
            throws ChaiOperationException
    {
        final boolean cacheFailures = "true".equalsIgnoreCase( this.getChaiConfiguration().getSetting( ChaiSetting.EXTENDED_OPERATION_FAILURE_CACHE ) );
        if ( cacheFailures )
        {
            final String requestID = request.getID();
            if ( cacheFailureMap.containsKey( requestID ) )
            {
                LOGGER.debug( () -> "previous extended operation request for " + requestID + " has failed, reissuing cached exception without attempting operation" );
                throw ( ChaiOperationException ) cacheFailureMap.get( requestID );
            }
        }
    }

    protected void cacheExtendedOperationException( final ExtendedRequest request, final Exception e )
            throws ChaiOperationException
    {
        final boolean cacheFailures = this.getChaiConfiguration().getBooleanSetting( ChaiSetting.EXTENDED_OPERATION_FAILURE_CACHE );
        if ( cacheFailures )
        {
            final ChaiOperationException opExcep = ChaiOperationException.forErrorMessage( e.getMessage(), e );
            if ( opExcep.getErrorCode() == ChaiError.UNSUPPORTED_OPERATION )
            {
                final String requestID = request.getID();
                cacheFailureMap.put( requestID, opExcep );
                LOGGER.trace( () -> "caching extended operation for " + requestID );
                throw opExcep;
            }
        }
    }

    @Override
    public DirectoryVendor getDirectoryVendor()
            throws ChaiUnavailableException
    {
        if ( cachedDirectoryVendor != null )
        {
            return cachedDirectoryVendor;
        }

        {
            final DirectoryVendor centralCachedVendor = getProviderFactory().getCentralService().getVendorCache( this.chaiConfig );
            if ( centralCachedVendor != null )
            {
                return centralCachedVendor;
            }
        }

        final Optional<DirectoryVendor> optionalDefaultVendor = getChaiConfiguration().getDefaultVendor();
        if ( optionalDefaultVendor.isPresent() )
        {
            cachedDirectoryVendor = optionalDefaultVendor.get();
        }
        else
        {
            try
            {
                final ChaiEntry rootDseEntry = ChaiUtility.getRootDSE( this );
                cachedDirectoryVendor = ChaiUtility.determineDirectoryVendor( rootDseEntry );
            }
            catch ( ChaiOperationException e )
            {
                LOGGER.warn( () -> "error while attempting to determine directory vendor: " + e.getMessage(), e );
                return DirectoryVendor.GENERIC;
            }
        }

        getProviderFactory().getCentralService().addVendorCache( this.chaiConfig, cachedDirectoryVendor );
        return cachedDirectoryVendor;
    }

    @Override
    public String getIdentifier()
    {
        return "providerId=" + counter;
    }

    @Override
    public ChaiProviderFactory getProviderFactory()
    {
        return this.providerFactory;
    }

    @Override
    public ChaiEntryFactory getEntryFactory()
    {
        return ChaiEntryFactory.newChaiFactory( this );
    }

    protected ChaiProviderInputValidator getInputValidator()
    {
        return INPUT_VALIDATOR;
    }

    static ChaiException convertInvocationExceptionToChaiException( final Exception e )
    {
        if ( e instanceof ChaiOperationException )
        {
            return ( ChaiOperationException ) e;
        }
        else if ( e instanceof ChaiUnavailableException )
        {
            return ( ChaiUnavailableException ) e;
        }
        else
        {
            LOGGER.warn( () -> "unexpected chai api error: " + e.getMessage(), e );
            throw new IllegalStateException( e.getMessage(), e );
        }
    }

    private void incrementBindStatistic()
    {
        if ( getChaiConfiguration().getBooleanSetting( ChaiSetting.STATISTICS_ENABLE ) )
        {
            getProviderFactory().getCentralService().getStatsBean().incrementStatistic( ProviderStatistics.IncrementerStatistic.BIND_COUNT );
        }
    }
}
