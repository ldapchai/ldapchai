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

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPControl;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPExtendedOperation;
import com.novell.ldap.LDAPExtendedResponse;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPModification;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;
import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiRequestControl;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.internal.ChaiLogger;
import com.novell.ldapchai.util.SearchHelper;

import javax.naming.NamingException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>JLDAP {@code ChaiProvider} implementation.  This
 * class wraps the JLDAP api at <a href="http://www.openldap.org/jldap/">OpenLDAP JLDAP API</a></p>
 *
 * <p>This implementation can be used by setting {@link ChaiSetting#PROVIDER_IMPLEMENTATION}
 * to {@code com.novell.ldapchai.provider.JLDAPProverImpl}.</p>
 *
 * @author Jason D. Rivard
 */

public class JLDAPProviderImpl extends AbstractProvider implements ChaiProviderImplementor
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( JLDAPProviderImpl.class );
    private LDAPConnection ldapConnection;

    //@todo test case needed
    static JLDAPProviderImpl createUsingExistingConnection( final LDAPConnection ldapConnection, final ChaiConfiguration chaiConfig )
            throws Exception
    {
        //@todo stub to be used for nmas c/r, this should be more robust.
        final JLDAPProviderImpl newImpl = new JLDAPProviderImpl();
        newImpl.init( chaiConfig, null );
        newImpl.ldapConnection = ldapConnection;
        return newImpl;
    }

    @Override
    public void init( final ChaiConfiguration chaiConfig, final ChaiProviderFactory providerFactory )
            throws ChaiUnavailableException, IllegalStateException
    {
        super.init( chaiConfig, providerFactory );
        try
        {
            // grab the first URL from the list.
            final URI ldapURL = URI.create( chaiConfig.bindURLsAsList().get( 0 ) );

            if ( ldapURL.getScheme().equalsIgnoreCase( "ldaps" ) )
            {
                final boolean usePromiscuousSSL = Boolean.parseBoolean( chaiConfig.getSetting( ChaiSetting.PROMISCUOUS_SSL ) );
                if ( usePromiscuousSSL )
                {
                    try
                    {
                        final SSLContext sc = SSLContext.getInstance( "SSL" );
                        sc.init( null, new X509TrustManager[] {new PromiscuousTrustManager()}, new java.security.SecureRandom() );
                        ldapConnection = new LDAPConnection( new LDAPJSSESecureSocketFactory( sc.getSocketFactory() ) );
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
                        final SSLContext sc = SSLContext.getInstance( "SSL" );
                        sc.init( null, chaiConfig.getTrustManager(), new java.security.SecureRandom() );
                        ldapConnection = new LDAPConnection( new LDAPJSSESecureSocketFactory( sc.getSocketFactory() ) );
                    }
                    catch ( Exception e )
                    {
                        LOGGER.error( () -> "error creating configured ssl ldap socket factory: " + e.getMessage() );
                    }
                }
                else
                {
                    ldapConnection = new LDAPConnection( new LDAPJSSESecureSocketFactory() );
                }
            }
            else
            {
                ldapConnection = new LDAPConnection();
            }

            ldapConnection.connect( ldapURL.getHost(), ldapURL.getPort() );
            if ( chaiConfig.getBooleanSetting( ChaiSetting.LDAP_FOLLOW_REFERRALS ) )
            {
                final LDAPConstraints ldapConstraints = new LDAPConstraints();
                ldapConstraints.setReferralFollowing( true );
                ldapConnection.setConstraints( ldapConstraints );
            }
            final String characterEncoding = chaiConfig.getSetting( ChaiSetting.LDAP_CHARACTER_ENCODING );
            final byte[] bindPassword = chaiConfig.getSetting( ChaiSetting.BIND_PASSWORD ).getBytes( Charset.forName( characterEncoding ) );
            final String bindDN = chaiConfig.getSetting( ChaiSetting.BIND_DN );
            ldapConnection.bind( LDAPConnection.LDAP_V3, bindDN, bindPassword );
        }
        catch ( LDAPException e )
        {
            final String message = e.getMessage();
            if ( message.contains( "Connect Error" ) )
            {
                throw new ChaiUnavailableException( message, ChaiError.COMMUNICATION, false, false );
            }
            throw ChaiUnavailableException.forErrorMessage( message );
        }
    }

    JLDAPProviderImpl()
    {
        super();
    }

    @Override
    public void close()
    {
        if ( ldapConnection != null )
        {
            try
            {
                ldapConnection.disconnect();
            }
            catch ( LDAPException e )
            {
                LOGGER.warn( () -> "error closing connection", e );
            }
        }
        super.close();
    }

    @Override
    @ChaiProvider.LdapOperation
    public boolean compareStringAttribute( final String entryDN, final String attribute, final String value )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().compareStringAttribute( entryDN, attribute, value );

        final LDAPAttribute ldapAttr = new LDAPAttribute( attribute, value );
        try
        {
            return ldapConnection.compare( entryDN, ldapAttr );
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void createEntry( final String entryDN, final String baseObjectClass, final Map<String, String> stringAttributes )
            throws ChaiOperationException
    {
        getInputValidator().createEntry( entryDN, baseObjectClass, stringAttributes );
        this.createEntry( entryDN, Collections.singleton( baseObjectClass ), stringAttributes );
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void createEntry( final String entryDN, final Set<String> baseObjectClasses, final Map<String, String> stringAttributes )
            throws ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().createEntry( entryDN, baseObjectClasses, stringAttributes );

        final LDAPAttributeSet ldapAttributeSet = new LDAPAttributeSet();
        ldapAttributeSet.add( new LDAPAttribute( ChaiConstant.ATTR_LDAP_OBJECTCLASS, baseObjectClasses.toArray( new String[0] ) ) );
        if ( stringAttributes != null )
        {
            for ( final Map.Entry<String, String> entry : stringAttributes.entrySet() )
            {
                final String attrName = entry.getKey();
                ldapAttributeSet.add( new LDAPAttribute( attrName, entry.getValue() ) );
            }
        }
        final LDAPEntry newEntry = new LDAPEntry( entryDN, ldapAttributeSet );
        try
        {
            ldapConnection.add( newEntry );
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
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
            ldapConnection.rename( entryDN, newRDN, newParentDN, true );
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void deleteEntry( final String entryDN )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().deleteEntry( entryDN );

        try
        {
            ldapConnection.delete( entryDN );
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void deleteStringAttributeValue( final String entryDN, final String attribute, final String value )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().deleteStringAttributeValue( entryDN, attribute, value );

        final LDAPAttribute ldapAttr = new LDAPAttribute( attribute, value );
        final LDAPModification mod = new LDAPModification( LDAPModification.DELETE, ldapAttr );

        try
        {
            ldapConnection.modify( entryDN, mod );
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    public ExtendedResponse extendedOperation( final ExtendedRequest request )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().extendedOperation( request );
        preCheckExtendedOperation( request );

        final String oid = request.getID();
        final byte[] value = request.getEncodedValue();
        final LDAPExtendedOperation ldapOper = new LDAPExtendedOperation( oid, value );
        final LDAPExtendedResponse ldapResponse;
        try
        {
            ldapResponse = ldapConnection.extendedOperation( ldapOper );
        }
        catch ( LDAPException e )
        {
            cacheExtendedOperationException( request, e );
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }

        try
        {
            return request.createExtendedResponse( ldapResponse.getID(), ldapResponse.getValue(), 0, ldapResponse.getValue().length );
        }
        catch ( NamingException e )
        {
            throw new RuntimeException( "unknown error while converting ldap extended response " + e.getMessage(), e );
        }
    }

    @Override
    public ProviderStatistics getProviderStatistics()
    {
        return null;
    }

    @Override
    @ChaiProvider.LdapOperation
    public byte[][] readMultiByteAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().readMultiByteAttribute( entryDN, attribute );

        try
        {
            final LDAPEntry entry = ldapConnection.read( entryDN, new String[] {attribute} );
            final LDAPAttribute ldapAttribute = entry.getAttribute( attribute );
            return ldapAttribute != null ? ldapAttribute.getByteValueArray() : new byte[0][0];
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    public Set<String> readMultiStringAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().readMultiStringAttribute( entryDN, attribute );

        try
        {
            final LDAPEntry entry = ldapConnection.read( entryDN, new String[] {attribute} );
            final LDAPAttribute ldapAttribute = entry.getAttribute( attribute );
            if ( ldapAttribute == null )
            {
                return Collections.emptySet();
            }
            else
            {
                return new HashSet<>( Arrays.asList( ldapAttribute.getStringValueArray() ) );
            }
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    public String readStringAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().readMultiStringAttribute( entryDN, attribute );

        return readStringAttributes( entryDN, Collections.singleton( attribute ) ).get( attribute );
    }

    @Override
    @ChaiProvider.LdapOperation
    public Map<String, String> readStringAttributes( final String entryDN, final Set<String> attributes )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().readStringAttributes( entryDN, attributes );

        final Map<String, String> returnProps = new LinkedHashMap<>();
        try
        {
            final LDAPEntry entry = ldapConnection.read( entryDN, attributes.toArray( new String[0] ) );

            for ( final Object attr : entry.getAttributeSet() )
            {
                final LDAPAttribute lAttr = ( LDAPAttribute ) attr;
                returnProps.put( lAttr.getName(), lAttr.getStringValue() );
            }

            return returnProps;
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void replaceStringAttribute( final String entryDN, final String attributeName, final String oldValue, final String newValue )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().replaceStringAttribute( entryDN, attributeName, oldValue, newValue );

        final LDAPModification[] modifications;

        modifications = new LDAPModification[2];
        modifications[0] = new LDAPModification( LDAPModification.DELETE, new LDAPAttribute( attributeName, oldValue ) );
        modifications[1] = new LDAPModification( LDAPModification.ADD, new LDAPAttribute( attributeName, newValue ) );

        try
        {
            ldapConnection.modify( entryDN, modifications );
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    public Map<String, Map<String, String>> search( final String baseDN, final SearchHelper searchHelper )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().search( baseDN, searchHelper );

        final Map<String, Map<String, List<String>>> results = searchImpl( baseDN, searchHelper, true );

        final Map<String, Map<String, String>> returnMap = new LinkedHashMap<>();
        for ( final Map.Entry<String, Map<String, List<String>>> resultEntry : results.entrySet() )
        {
            final String dn = resultEntry.getKey();
            final Map<String, List<String>> loopAttrs = resultEntry.getValue();
            final Map<String, String> attrProps = new LinkedHashMap<>();
            for ( final Map.Entry<String, List<String>> attrEntry : loopAttrs.entrySet() )
            {
                final String loopAttr = attrEntry.getKey();
                attrProps.put( loopAttr, attrEntry.getValue().iterator().next() );
            }
            returnMap.put( dn, attrProps );
        }
        return returnMap;
    }

    @Override
    @ChaiProvider.LdapOperation
    public Map<String, Map<String, String>> search( final String baseDN, final String filter, final Set<String> attributes, final SearchScope searchScope )
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
    @ChaiProvider.LdapOperation
    public Map<String, Map<String, List<String>>> searchMultiValues(
            final String baseDN,
            final SearchHelper searchHelper )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().searchMultiValues( baseDN, searchHelper );

        return searchImpl( baseDN, searchHelper, false );
    }

    @Override
    @ChaiProvider.LdapOperation
    public Map<String, Map<String, List<String>>> searchMultiValues(
            final String baseDN,
            final String filter,
            final Set<String> attributes,
            final SearchScope searchScope )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().searchMultiValues( baseDN, filter, attributes, searchScope );

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter( filter );
        searchHelper.setAttributes( attributes );
        searchHelper.setSearchScope( searchScope );

        return searchImpl( baseDN, searchHelper, false );
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void writeBinaryAttribute( final String entryDN, final String attribute, final byte[][] values, final boolean overwrite )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        writeBinaryAttribute( entryDN, attribute, values, overwrite, null );
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void writeBinaryAttribute(
            final String entryDN,
            final String attribute,
            final byte[][] values,
            final boolean overwrite,
            final ChaiRequestControl[] controls
    )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().writeBinaryAttribute( entryDN, attribute, values, overwrite );

        final LDAPAttribute ldapAttr = new LDAPAttribute( attribute );

        for ( final byte[] value : values )
        {
            ldapAttr.addValue( value );
        }

        final LDAPModification mod = new LDAPModification( overwrite ? LDAPModification.REPLACE : LDAPModification.ADD, ldapAttr );
        try
        {
            if ( controls != null && controls.length > 0 )
            {
                final LDAPConstraints constraints = new LDAPConstraints();
                constraints.setControls( convertControls( controls ) );
                ldapConnection.modify( entryDN, mod, constraints );
            }
            else
            {
                ldapConnection.modify( entryDN, mod );
            }
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void replaceBinaryAttribute( final String entryDN, final String attribute, final byte[] oldValue, final byte[] newValue )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().replaceBinaryAttribute( entryDN, attribute, oldValue, newValue );

        final LDAPModification[] modifications;

        modifications = new LDAPModification[2];
        modifications[0] = new LDAPModification( LDAPModification.DELETE, new LDAPAttribute( attribute, oldValue ) );
        modifications[1] = new LDAPModification( LDAPModification.ADD, new LDAPAttribute( attribute, newValue ) );

        try
        {
            ldapConnection.modify( entryDN, modifications );
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void writeStringAttribute( final String entryDN, final String attribute, final Set<String> values, final boolean overwrite )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().writeStringAttribute( entryDN, attribute, values, overwrite );

        final LDAPAttribute ldapAttr = new LDAPAttribute( attribute, values.toArray( new String[0] ) );
        final LDAPModification mod = new LDAPModification( overwrite ? LDAPModification.REPLACE : LDAPModification.ADD, ldapAttr );
        try
        {
            ldapConnection.modify( entryDN, mod );
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    @LdapOperation
    @ModifyOperation
    public final void writeStringAttributes( final String entryDN, final Map<String, String> attributeValues, final boolean overwrite )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().writeStringAttributes( entryDN, attributeValues, overwrite );


        final int modOption = overwrite ? LDAPModification.REPLACE : LDAPModification.ADD;

        final List<LDAPModification> modifications = new ArrayList<>();
        for ( final Map.Entry<String, String> entry : attributeValues.entrySet() )
        {
            final String attrName = entry.getKey();
            final LDAPAttribute ldapAttr = new LDAPAttribute( attrName, entry.getValue() );
            final LDAPModification mod = new LDAPModification( modOption, ldapAttr );
            modifications.add( mod );
        }

        final LDAPModification[] modificationArray = modifications.toArray( new LDAPModification[0] );

        try
        {
            ldapConnection.modify( entryDN, modificationArray );
        }
        catch ( LDAPException e )
        {
            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
    }

    @Override
    public Object getConnectionObject()
            throws Exception
    {
        return ldapConnection;
    }

    @Override
    public String getCurrentConnectionURL()
    {
        if ( ldapConnection == null || !ldapConnection.isConnected() )
        {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append( "ldap" );
        if ( ldapConnection.isTLS() )
        {
            sb.append( "s" );
        }
        sb.append( "://" );
        sb.append( ldapConnection.getHost() );
        sb.append( ":" );
        sb.append( ldapConnection.getPort() );
        return sb.toString();
    }

    public Map<String, Map<String, List<String>>> searchImpl(
            final String baseDN,
            final SearchHelper searchHelper,
            final boolean onlyFirstValue
    )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();

        // make a copy so if it changes somewhere else we won't be affected.
        final SearchHelper effectiveSearchHelper = new SearchHelper( searchHelper );

        // replace a null dn with an empty string
        final String effectiveBaseDN = baseDN != null
                ? baseDN
                : "";


        final int ldapScope;
        switch ( effectiveSearchHelper.getSearchScope() )
        {
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

        final Map<String, Map<String, List<String>>> returnMap = new LinkedHashMap<>();

        final LDAPSearchConstraints constraints = new LDAPSearchConstraints();
        constraints.setMaxResults( effectiveSearchHelper.getMaxResults() );
        constraints.setTimeLimit( effectiveSearchHelper.getTimeLimit() );

        final String[] returnAttributes = effectiveSearchHelper.getAttributes() == null
                ? null
                : effectiveSearchHelper.getAttributes().toArray( new String[0] );

        final LDAPSearchResults results;
        try
        {
            results = ldapConnection.search(
                    effectiveBaseDN,
                    ldapScope, effectiveSearchHelper.getFilter(),
                    returnAttributes,
                    false,
                    constraints
            );

            while ( results.hasMore() )
            {
                final LDAPEntry loopEntry = results.next();
                final String loopDN = loopEntry.getDN();
                final Map<String, List<String>> loopAttributes = new LinkedHashMap<>();
                final LDAPAttributeSet attrSet = loopEntry.getAttributeSet();
                for ( final Object anAttrSet : attrSet )
                {
                    final LDAPAttribute loopAttr = ( LDAPAttribute ) anAttrSet;
                    if ( onlyFirstValue )
                    {
                        loopAttributes.put( loopAttr.getName(), Collections.singletonList( loopAttr.getStringValue() ) );
                    }
                    else
                    {
                        loopAttributes.put( loopAttr.getName(), Arrays.asList( loopAttr.getStringValueArray() ) );
                    }
                }
                returnMap.put( loopDN, Collections.unmodifiableMap( loopAttributes ) );
            }
        }
        catch ( LDAPException e )
        {
            // check to see if there any results. If there are results, then
            // return them.  If no results, then throw the exception.  Most likely
            // cause of results+exception is search size/time exceeded.

            if ( !returnMap.isEmpty() )
            {
                return Collections.unmodifiableMap( returnMap );
            }

            throw ChaiOperationException.forErrorMessage( e.getLDAPErrorMessage(), e );
        }
        return Collections.unmodifiableMap( returnMap );
    }

    @Override
    public boolean isConnected()
    {
        return ldapConnection != null && ldapConnection.isConnected();
    }

    protected static LDAPControl[] convertControls( final ChaiRequestControl[] controls )
    {
        if ( controls == null )
        {
            return null;
        }

        final LDAPControl[] newControls = new LDAPControl[controls.length];
        for ( int i = 0; i < controls.length; i++ )
        {
            newControls[i] = new LDAPControl(
                    controls[i].getId(),
                    controls[i].isCritical(),
                    controls[i].getValue()
            );
        }
        return newControls;
    }
}
