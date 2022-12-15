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

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiRequestControl;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.internal.ChaiLogger;
import com.novell.ldapchai.util.SearchHelper;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddRequestImpl;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.DeleteRequest;
import org.apache.directory.api.ldap.model.message.DeleteRequestImpl;
import org.apache.directory.api.ldap.model.message.DeleteResponse;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyDnResponse;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.ResultResponse;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Apache LDAP API Wrapper.
 *
 * <p>This implementation can be used by setting {@link ChaiSetting#PROVIDER_IMPLEMENTATION}
 * to {@code com.novell.ldapchai.provider.ApacheLdapProviderImpl}.</p>
 */
public class ApacheLdapProviderImpl extends AbstractProvider implements ChaiProviderImplementor
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( ApacheLdapProviderImpl.class );

    private String currentLdapUrl;

    private LdapConnection connection;

    ApacheLdapProviderImpl()
    {
        super();
    }

    @Override
    public Object getConnectionObject()
            throws Exception
    {
        return null;
    }

    @Override
    public String getCurrentConnectionURL()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return super.toString();
    }

    @Override
    public void close()
    {
        super.close();
        try
        {
            connection.close();
        }
        catch ( IOException e )
        {
            LOGGER.error( () -> "error during connection close: " + e.getMessage() );
        }
    }

    @Override
    public ChaiConfiguration getChaiConfiguration()
    {
        return super.getChaiConfiguration();
    }

    @Override
    public ConnectionState getConnectionState()
    {
        return super.getConnectionState();
    }

    @Override
    public void init( final ChaiConfiguration chaiConfig, final ChaiProviderFactory providerFactory )
            throws ChaiUnavailableException
    {
        this.chaiConfig = chaiConfig;
        super.init( chaiConfig, providerFactory );

        // grab the first URL from the list.
        currentLdapUrl = chaiConfig.bindURLsAsList().get( 0 );
        final URI ldapURL = URI.create( currentLdapUrl );

        final LdapConnectionConfig ldapConnectionConfig = new LdapConnectionConfig();
        ldapConnectionConfig.setLdapHost( ldapURL.getHost() );
        ldapConnectionConfig.setLdapPort( ldapURL.getPort() );

        if ( ldapURL.getScheme().equalsIgnoreCase( "ldaps" ) )
        {
            ldapConnectionConfig.setUseSsl( true );
            final boolean usePromiscuousSSL = Boolean.parseBoolean( chaiConfig.getSetting( ChaiSetting.PROMISCUOUS_SSL ) );
            if ( usePromiscuousSSL )
            {
                try
                {
                    final PromiscuousTrustManager promiscuousTrustManager = new PromiscuousTrustManager();
                    ldapConnectionConfig.setTrustManagers( promiscuousTrustManager );
                }
                catch ( Exception e )
                {
                    LOGGER.error( () -> "error creating promiscuous ssl ldap socket factory: " + e.getMessage() );
                }
            }
            else if ( chaiConfig.getTrustManager() != null )
            {
                try
                {
                    final X509TrustManager[] trustManager = chaiConfig.getTrustManager();
                    ldapConnectionConfig.setTrustManagers( trustManager );
                }
                catch ( Exception e )
                {
                    LOGGER.error( () -> "error creating configured ssl ldap socket factory: " + e.getMessage() );
                }
            }
        }

        final LdapConnection newConnection;
        try
        {

            newConnection = new LdapNetworkConnection( ldapConnectionConfig );
            newConnection.connect();
            final String bindPassword = chaiConfig.getSetting( ChaiSetting.BIND_PASSWORD );
            final String bindDN = chaiConfig.getSetting( ChaiSetting.BIND_DN );
            newConnection.bind( bindDN, bindPassword );
        }
        catch ( LdapException e )
        {
            final String message = e.getMessage();
            if ( message.contains( "Cannot connect on the server" ) )
            {
                throw new ChaiUnavailableException( message, ChaiError.COMMUNICATION, false, false, e );
            }
            throw ChaiUnavailableException.forErrorMessage( message, e );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            final String message = e.getMessage();
            throw new ChaiUnavailableException( message, ChaiError.UNKNOWN, false, false, e );
        }

        connection = newConnection;
    }

    @Override
    protected void activityPreCheck()
    {
        super.activityPreCheck();
    }

    @Override
    public boolean errorIsRetryable( final Exception e )
    {
        return super.errorIsRetryable( e );
    }

    @Override
    protected void preCheckExtendedOperation( final ExtendedRequest request )
            throws ChaiOperationException
    {
        super.preCheckExtendedOperation( request );
    }

    @Override
    protected void cacheExtendedOperationException( final ExtendedRequest request, final Exception e )
            throws ChaiOperationException
    {
        super.cacheExtendedOperationException( request, e );
    }

    @Override
    public boolean compareStringAttribute( final String entryDN, final String attributeName, final String value )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().compareStringAttribute( entryDN, attributeName, value );

        try
        {
            return connection.compare( entryDN, attributeName, value );
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void createEntry( final String entryDN, final String baseObjectClass, final Map<String, String> stringAttributes )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().createEntry( entryDN, baseObjectClass, stringAttributes );

        createEntry( entryDN, Collections.singleton( baseObjectClass ), stringAttributes );
    }

    @Override
    public void createEntry( final String entryDN, final Set<String> baseObjectClasses, final Map<String, String> stringAttributes )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().createEntry( entryDN, baseObjectClasses, stringAttributes );

        try
        {
            final AddRequest addRequest = new AddRequestImpl();
            final Entry entry = new DefaultEntry();
            entry.setDn( entryDN );
            for ( final String baseObjectClass : baseObjectClasses )
            {
                entry.add( ChaiConstant.ATTR_LDAP_OBJECTCLASS, baseObjectClass );
            }

            for ( final Map.Entry<String, String> entryIter : stringAttributes.entrySet() )
            {
                final String name = entryIter.getKey();
                final String value = entryIter.getValue();
                entry.add( name, value );
            }

            final AddResponse response = connection.add( addRequest );
            processResponse( response );
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void renameEntry( final String entryDN, final String newRDN, final String newParentDN )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        try
        {
            final ModifyDnRequest modifyDnRequest = new ModifyDnRequestImpl();
            modifyDnRequest.setName( new Dn(  entryDN ) );
            modifyDnRequest.setDeleteOldRdn( true );
            modifyDnRequest.setNewRdn( new Rdn( newRDN ) );
            modifyDnRequest.setNewSuperior( new Dn( newParentDN ) );
            final ModifyDnResponse response = connection.modifyDn( modifyDnRequest );
            processResponse( response );
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void deleteEntry( final String entryDN )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().deleteEntry( entryDN );

        try
        {
            final DeleteRequest deleteRequest = new DeleteRequestImpl();
            deleteRequest.setName( new Dn( entryDN ) );
            final DeleteResponse response = connection.delete( deleteRequest );
            processResponse( response );
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void deleteStringAttributeValue( final String entryDN, final String attributeName, final String value )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().deleteStringAttributeValue( entryDN, attributeName, value );

        try
        {
            final ModifyRequest modifyRequest = new ModifyRequestImpl();
            modifyRequest.setName( new Dn( entryDN ) );
            if ( value == null )
            {
                final Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attributeName );
                modifyRequest.addModification( modification );
            }
            else
            {
                final Modification modification = new DefaultModification();
                modification.setOperation( ModificationOperation.REMOVE_ATTRIBUTE );
                modification.setAttribute( new DefaultAttribute( attributeName, value ) );
            }
            final ModifyResponse response = connection.modify( modifyRequest );
            processResponse( response );
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public ExtendedResponse extendedOperation( final ExtendedRequest request )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        final org.apache.directory.api.ldap.model.message.ExtendedRequest apacheRequest = new org.apache.directory.api.ldap.model.message.ExtendedRequest()
        {
            @Override
            public String getRequestName()
            {
                return request.getID();
            }

            @Override
            public org.apache.directory.api.ldap.model.message.ExtendedRequest setRequestName( final String oid )
            {
                return this;
            }

            @Override
            public org.apache.directory.api.ldap.model.message.ExtendedRequest setMessageId( final int messageId )
            {
                return this;
            }

            @Override
            public org.apache.directory.api.ldap.model.message.ExtendedRequest addControl( final Control control )
            {
                return null;
            }

            @Override
            public org.apache.directory.api.ldap.model.message.ExtendedRequest addAllControls( final Control[] controls )
            {
                return null;
            }

            @Override
            public org.apache.directory.api.ldap.model.message.ExtendedRequest removeControl( final Control control )
            {
                return null;
            }

            @Override
            public MessageTypeEnum getResponseType()
            {
                return null;
            }

            @Override
            public ResultResponse getResultResponse()
            {
                return null;
            }

            @Override
            public boolean hasResponse()
            {
                return false;
            }

            @Override
            public MessageTypeEnum getType()
            {
                return null;
            }

            @Override
            public Map<String, Control> getControls()
            {
                return null;
            }

            @Override
            public Control getControl( final String oid )
            {
                return null;
            }

            @Override
            public boolean hasControl( final String oid )
            {
                return false;
            }

            @Override
            public int getMessageId()
            {
                return 0;
            }

            @Override
            public Object get( final Object key )
            {
                return null;
            }

            @Override
            public Object put( final Object key, final Object value )
            {
                return null;
            }
        };
        try
        {
            final org.apache.directory.api.ldap.model.message.ExtendedResponse apacheResponse = connection.extended( apacheRequest );
            final ExtendedResponse extendedResponse = new ExtendedResponse()
            {
                @Override
                public String getID()
                {
                    return apacheResponse.getResponseName();
                }

                @Override
                public byte[] getEncodedValue()
                {
                    return null;
                }
            };
            return extendedResponse;
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public ProviderStatistics getProviderStatistics()
    {
        return null;
    }

    @Override
    public byte[][] readMultiByteAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().readStringAttribute( entryDN, attribute );

        final List<Value> values = readMultiAttribute( entryDN, attribute );
        if ( values == null )
        {
            return null;
        }
        final byte[][] bytes = new byte[values.size()][];
        for ( int i = 0; i < values.size(); i++ )
        {
            bytes[i] = values.get( i ).getBytes();
        }
        return bytes;
    }


    @Override
    public Set<String> readMultiStringAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().readStringAttribute( entryDN, attribute );

        final List<Value> values = readMultiAttribute( entryDN, attribute );
        if ( values == null )
        {
            return Collections.emptySet();
        }
        final Set<String> returnSet = new LinkedHashSet<>();
        for ( final Value value : values )
        {
            returnSet.add( value.getString() );
        }
        return Collections.unmodifiableSet( returnSet );
    }

    private List<Value> readMultiAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException
    {
        try
        {
            final EntryCursor entries = connection.search(
                    entryDN,
                    ChaiConstant.FILTER_OBJECTCLASS_ANY,
                    org.apache.directory.api.ldap.model.message.SearchScope.OBJECT,
                    attribute
            );
            final Entry entry = entries.iterator().next();
            final List<Value> returnSet = new ArrayList<>();
            final Attribute attr = entry.get( attribute );
            if ( attr == null )
            {
                return null;
            }
            for ( final Value value : attr )
            {
                if ( value != null )
                {
                    returnSet.add( value );
                }
            }
            return Collections.unmodifiableList( returnSet );

        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }

    }

    @Override
    public String readStringAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().readStringAttribute( entryDN, attribute );

        try
        {
            final EntryCursor entries = connection.search(
                    entryDN,
                    ChaiConstant.FILTER_OBJECTCLASS_ANY,
                    org.apache.directory.api.ldap.model.message.SearchScope.OBJECT,
                    attribute
            );
            final Entry entry = entries.iterator().next();
            final Attribute attr = entry.get( attribute );
            return attr == null ? null : attr.getString();

        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public Map<String, String> readStringAttributes( final String entryDN, final Set<String> attributes )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().readStringAttributes( entryDN, attributes );

        try
        {
            final EntryCursor entries = connection.search(
                    entryDN,
                    ChaiConstant.FILTER_OBJECTCLASS_ANY,
                    org.apache.directory.api.ldap.model.message.SearchScope.OBJECT,
                    attributes.toArray( new String[0] )
            );
            final Entry entry = entries.iterator().next();
            final Collection<Attribute> attrs = entry.getAttributes();
            final Map<String, String> returnMap = new LinkedHashMap<>();
            for ( final Attribute attr : attrs )
            {
                final String name = attr.getId();
                final String value = attr.getString();
                returnMap.put( name, value );
            }

            return returnMap;

        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void replaceStringAttribute( final String entryDN, final String attributeName, final String oldValue, final String newValue )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().replaceStringAttribute( entryDN, attributeName, oldValue, newValue );

        replaceAttributeImpl( entryDN, attributeName, new Value( oldValue ), new Value( newValue ) );
    }

    private void replaceAttributeImpl( final String entryDN, final String attributeName, final Value oldValue, final Value newValue )
            throws ChaiOperationException
    {
        try
        {
            final ModifyRequest modifyRequest = new ModifyRequestImpl();
            modifyRequest.setName( new Dn( entryDN ) );
            {
                final Modification modification = new DefaultModification();
                modification.setOperation( ModificationOperation.REMOVE_ATTRIBUTE );
                modification.setAttribute( new DefaultAttribute( attributeName, oldValue ) );
                modifyRequest.addModification( modification );
            }
            {
                final Modification modification = new DefaultModification();
                modification.setOperation( ModificationOperation.ADD_ATTRIBUTE );
                modification.setAttribute( new DefaultAttribute( attributeName, newValue ) );
                modifyRequest.addModification( modification );
            }
            final ModifyResponse response = connection.modify( modifyRequest );
            processResponse( response );
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public Map<String, Map<String, String>> search( final String baseDN, final SearchHelper searchHelper )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().search( baseDN, searchHelper );

        final Map<String, Map<String, List<String>>> results = searchImpl( baseDN, searchHelper, false );
        final Map<String, Map<String, String>> returnObj = new LinkedHashMap<>();
        for ( final Map.Entry<String, Map<String, List<String>>> resultEntry : results.entrySet() )
        {
            final String dn = resultEntry.getKey();
            final Map<String, List<String>> entryMap = resultEntry.getValue();
            final Map<String, String> newEntryMap = new LinkedHashMap<>();
            for ( final Map.Entry<String, List<String>> attributeEntry : entryMap.entrySet() )
            {
                final String attr = attributeEntry.getKey();
                final String value = attributeEntry.getValue().iterator().next();
                newEntryMap.put( attr, value );
            }
            returnObj.put( dn, Collections.unmodifiableMap( newEntryMap ) );
        }
        return Collections.unmodifiableMap( returnObj );
    }

    @Override
    public Map<String, Map<String, String>> search(
            final String baseDN,
            final String filter,
            final Set<String> attributes,
            final SearchScope searchScope
    )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().search( baseDN, filter, attributes, searchScope );

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter( filter );
        searchHelper.setAttributes( attributes );
        searchHelper.setSearchScope( searchScope );

        return search( baseDN, searchHelper );
    }

    @Override
    public Map<String, Map<String, List<String>>> searchMultiValues( final String baseDN, final SearchHelper searchHelper )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().searchMultiValues( baseDN, searchHelper );

        return searchImpl( baseDN, searchHelper, true );
    }

    @Override
    public Map<String, Map<String, List<String>>> searchMultiValues(
            final String baseDN,
            final String filter,
            final Set<String> attributes,
            final SearchScope searchScope
    )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().searchMultiValues( baseDN, filter, attributes, searchScope );

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter( filter );
        searchHelper.setAttributes( attributes );
        searchHelper.setSearchScope( searchScope );

        return searchImpl( baseDN, searchHelper, true );
    }

    private Map<String, Map<String, List<String>>> searchImpl( final String baseDN, final SearchHelper searchHelper, final boolean multivalued )
            throws ChaiUnavailableException, ChaiOperationException
    {
        try
        {
            final SearchRequest searchRequest = new SearchRequestImpl();
            searchRequest.setBase( new Dn( baseDN ) );
            searchRequest.setFilter( searchHelper.getFilter() );
            searchRequest.setScope( figureSearchScope( searchHelper.getSearchScope() ) );
            searchRequest.setSizeLimit( searchHelper.getMaxResults() );
            searchRequest.setTimeLimit( searchHelper.getTimeLimit() );

            final SearchCursor searchCursor = connection.search( searchRequest );

            final Map<String, Map<String, List<String>>> returnObj = new LinkedHashMap<>();

            while ( searchCursor.next() )
            {
                final Entry entry = searchCursor.getEntry();
                final String dnValue = entry.getDn().getName();
                final Map<String, List<String>> entryMap = new HashMap<>();
                for ( Attribute returnAttr : entry )
                {
                    final String attrName = returnAttr.getId();
                    final List<String> valueList = new ArrayList<>();
                    if ( multivalued )
                    {
                        for ( Value value : returnAttr )
                        {
                            valueList.add( value.getString() );
                        }
                    }
                    else
                    {
                        final String value = returnAttr.iterator().next().getString();
                        valueList.add( value );
                    }
                    entryMap.put( attrName, Collections.unmodifiableList( valueList ) );
                }
                returnObj.put( dnValue, Collections.unmodifiableMap( entryMap ) );
            }

            return Collections.unmodifiableMap( returnObj );

        }
        catch ( final CursorException | LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void writeBinaryAttribute( final String entryDN, final String attributeName, final byte[][] values, final boolean overwrite )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().writeBinaryAttribute( entryDN, attributeName, values, overwrite );

        try
        {
            final ModifyRequest modifyRequest = new ModifyRequestImpl();
            modifyRequest.setName( new Dn( entryDN ) );
            {
                final Modification modification = new DefaultModification();
                modification.setOperation( overwrite ? ModificationOperation.REPLACE_ATTRIBUTE : ModificationOperation.ADD_ATTRIBUTE );
                modification.setAttribute( new DefaultAttribute( attributeName, values ) );
                modifyRequest.addModification( modification );
            }
            final ModifyResponse response = connection.modify( modifyRequest );
            processResponse( response );
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void writeBinaryAttribute( final String entryDN, final String attributeName, final byte[][] values, final boolean overwrite, final ChaiRequestControl[] controls )
            throws ChaiUnavailableException, ChaiOperationException
    {
        try
        {
            final ModifyRequest modifyRequest = new ModifyRequestImpl();
            modifyRequest.setName( new Dn( entryDN ) );
            modifyRequest.addAllControls( figureControls( controls ) );
            {
                final Modification modification = new DefaultModification();
                modification.setOperation( overwrite ? ModificationOperation.REPLACE_ATTRIBUTE : ModificationOperation.ADD_ATTRIBUTE );
                modification.setAttribute( new DefaultAttribute( attributeName, values ) );
                modifyRequest.addModification( modification );
            }
            final ModifyResponse response = connection.modify( modifyRequest );
            processResponse( response );
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }

    }

    @Override
    public void writeStringAttribute( final String entryDN, final String attributeName, final Set<String> values, final boolean overwrite )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().writeStringAttribute( entryDN, attributeName, values, overwrite );

        try
        {
            final ModifyRequest modifyRequest = new ModifyRequestImpl();
            modifyRequest.setName( new Dn( entryDN ) );
            {
                final Modification modification = new DefaultModification();
                modification.setOperation( overwrite ? ModificationOperation.REPLACE_ATTRIBUTE : ModificationOperation.ADD_ATTRIBUTE );
                modification.setAttribute( new DefaultAttribute( attributeName, values.toArray( new String[0] ) ) );
                modifyRequest.addModification( modification );
            }
            final ModifyResponse response = connection.modify( modifyRequest );
            processResponse( response );
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void writeStringAttributes( final String entryDN, final Map<String, String> attributeValueProps, final boolean overwrite )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().writeStringAttributes( entryDN, attributeValueProps, overwrite );

        try
        {
            final ModifyRequest modifyRequest = new ModifyRequestImpl();
            modifyRequest.setName( new Dn( entryDN ) );
            for ( final Map.Entry<String, String> entry : attributeValueProps.entrySet() )
            {
                final String name = entry.getKey();
                final String value = entry.getValue();
                final Modification modification = new DefaultModification();
                modification.setOperation( overwrite ? ModificationOperation.REPLACE_ATTRIBUTE : ModificationOperation.ADD_ATTRIBUTE );
                modification.setAttribute( new DefaultAttribute( name, value ) );
                modifyRequest.addModification( modification );
            }
            final ModifyResponse response = connection.modify( modifyRequest );
            processResponse( response );
        }
        catch ( LdapException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }
    }

    @Override
    public void replaceBinaryAttribute( final String entryDN, final String attributeName, final byte[] oldValue, final byte[] newValue )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().replaceBinaryAttribute( entryDN, attributeName, oldValue, newValue );

        replaceAttributeImpl( entryDN, attributeName, new Value( oldValue ), new Value( newValue ) );
    }

    @Override
    public boolean isConnected()
    {
        return connection != null && connection.isConnected();
    }

    private static void processResponse( final ResultResponse response )
            throws ChaiOperationException
    {
        final boolean success = response.getLdapResult().getResultCode() == ResultCodeEnum.SUCCESS;
        if ( !success )
        {
            final String msg = response.getLdapResult().getDiagnosticMessage();
            throw ChaiOperationException.forErrorMessage( msg );
        }
    }

    private static org.apache.directory.api.ldap.model.message.SearchScope figureSearchScope( final SearchScope searchScope )
    {
        switch ( searchScope )
        {
            case BASE:
                return org.apache.directory.api.ldap.model.message.SearchScope.OBJECT;

            case ONE:
                return org.apache.directory.api.ldap.model.message.SearchScope.ONELEVEL;

            case SUBTREE:
                return org.apache.directory.api.ldap.model.message.SearchScope.SUBTREE;

            default:
                throw new IllegalArgumentException( "unknown SearchScope type" );
        }
    }

    private static Control[] figureControls( final ChaiRequestControl[] chaiControls )
    {
        final List<Control> returnObj = new ArrayList<>();
        for ( final ChaiRequestControl chaiControl : chaiControls )
        {
            final Control control = new Control()
            {
                @Override
                public String getOid()
                {
                    return chaiControl.getId();
                }

                @Override
                public boolean isCritical()
                {
                    return chaiControl.isCritical();
                }

                @Override
                public void setCritical( final boolean isCritical )
                {

                }
            };
            returnObj.add( control );
        }
        return returnObj.toArray( new Control[0] );
    }
}
