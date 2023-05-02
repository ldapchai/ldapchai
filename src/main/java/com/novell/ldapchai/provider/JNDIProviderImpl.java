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
import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiRequestControl;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.ChaiUtility;
import com.novell.ldapchai.util.SearchHelper;
import com.novell.ldapchai.util.internal.ChaiLogger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.InterruptedNamingException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.BasicControl;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Default {@code ChaiProvider} implementation; wraps the standard JNDI ldap API.</p>
 *
 * <p>This implementation can be used by setting {@link ChaiSetting#PROVIDER_IMPLEMENTATION}
 * to {@code com.novell.ldapchai.provider.JNDIProverImpl}.</p>
 *
 * @author Jason D. Rivard
 */
public class JNDIProviderImpl extends AbstractProvider implements ChaiProviderImplementor
{

    /**
     * The default initial pool size to create when communicating with an individual server. *
     */
    private static final int DEFAULT_INITIAL_POOL_SIZE = 1;

    /**
     * The default preferred pool size to create when communicating with an individual server. *
     */
    private static final int DEFAULT_PREFERRED_POOL_SIZE = 10;

    /**
     * The default maximum pool size to create when communicating with an individual server. *
     */
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 50;


    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( JNDIProviderImpl.class );

    private Boolean cachedPagingEnableSupport = null;
    private LdapContext jndiConnection;
    private SocketFactory socketFactory;

    private static LdapContext generateNewJndiContext( final Hashtable<String, Object> environment )
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String url = String.valueOf( environment.get( Context.PROVIDER_URL ) );
        final String bindDN = String.valueOf( environment.get( Context.SECURITY_PRINCIPAL ) );

        try
        {
            final Instant startTime = Instant.now();
            final LdapContext newDirContext;
            newDirContext = new InitialLdapContext( environment, null );
            final Duration duration = Duration.between( startTime, Instant.now() );
            LOGGER.trace( () -> "bind successful as " + bindDN, duration );
            return newDirContext;
        }
        catch ( NamingException e )
        {
            final StringBuilder logMsg = new StringBuilder();
            logMsg.append( "unable to bind to " );
            logMsg.append( url );
            logMsg.append( " as " );
            logMsg.append( bindDN );
            logMsg.append( " reason: " );
            if ( e instanceof CommunicationException )
            {
                logMsg.append( "CommunicationException (" ).append( e.getMessage() );
                final Throwable rootCause = e.getRootCause();
                if ( rootCause != null )
                {
                    logMsg.append( "; " ).append( rootCause.getMessage() );
                }
                logMsg.append( ")" );
                throw new ChaiUnavailableException( logMsg.toString(), ChaiError.COMMUNICATION, false, true, e );
            }
            else
            {
                logMsg.append( e.getMessage() );

                //check for bad password or intruder detection
                throw ChaiUnavailableException.forErrorMessage( logMsg.toString(), e );
            }
        }
    }

    /**
     * <p>Converts an array of primitive bytes to objects.</p>
     *
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * <p>From Jakarta Commons project</p>
     *
     * @param array a <code>byte</code> array
     * @return a <code>Byte</code> array, <code>null</code> if null array input
     */
    private static Byte[] toObject( final byte[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return new Byte[0];
        }
        final Byte[] result = new Byte[array.length];
        int counter = 0;
        while ( counter < array.length )
        {
            result[counter] = array[counter];
            counter++;
        }
        return result;
    }

    /**
     * <p>Converts an array of object Bytes to primitives.</p>
     *
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * <p>From Jakarta Commons project</p>
     *
     * @param array a <code>Byte</code> array, may be <code>null</code>
     * @return a <code>byte</code> array, <code>null</code> if null array input
     * @throws NullPointerException if array content is <code>null</code>
     */
    private static byte[] toPrimitive( final Byte[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return new byte[0];
        }
        final byte[] result = new byte[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = array[i];
        }
        return result;
    }

    JNDIProviderImpl()
    {
        super();
    }

    @Override
    public void close()
    {
        super.close();
        if ( jndiConnection != null )
        {
            try
            {
                jndiConnection.close();
            }
            catch ( Exception e )
            {
                LOGGER.warn( () -> "unexpected error during jndi connection close " + e.getMessage(), e );
            }
            finally
            {
                jndiConnection = null;
            }
        }
    }

    @Override
    @LdapOperation
    public final boolean compareStringAttribute( final String entryDN, final String attributeName, final String value )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().compareStringAttribute( entryDN, attributeName, value );

        final byte[] ba;
        try
        {
            ba = value.getBytes( "UTF-8" );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new UnsupportedOperationException( e );
        }

        // Set up the search controls
        final SearchControls ctls = new SearchControls();

        // Return no attrs
        ctls.setReturningAttributes( new String[0] );

        // Search object only
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );

        final LdapContext ldapConnection = getLdapConnection();
        NamingEnumeration<SearchResult> answer = null;
        boolean result = false;
        try
        {
            answer = ldapConnection.search( addJndiEscape( entryDN ), "(" + attributeName + "={0})", new Object[] {ba}, ctls );
            result = answer.hasMore();
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
        finally
        {
            if ( answer != null )
            {
                try
                {
                    answer.close();
                }
                catch ( Exception e )
                {
                    /* action not required */
                }
            }
        }

        return result;
    }

    @Override
    @LdapOperation
    @ModifyOperation
    public final void createEntry( final String entryDN, final String baseObjectClass, final Map<String, String> stringAttributes )
            throws ChaiUnavailableException, ChaiOperationException
    {
        getInputValidator().createEntry( entryDN, baseObjectClass, stringAttributes );
        this.createEntry( entryDN, Collections.singleton( baseObjectClass ), stringAttributes );
    }

    @Override
    @LdapOperation
    @ModifyOperation
    public final void createEntry( final String entryDN, final Set<String> baseObjectClasses, final Map<String, String> stringAttributes )
            throws ChaiOperationException, ChaiUnavailableException
    {
        activityPreCheck();
        getInputValidator().createEntry( entryDN, baseObjectClasses, stringAttributes );

        final Attributes attrs = new BasicAttributes();

        //Put in the base object class an attribute
        final BasicAttribute objectClassAttr = new BasicAttribute( ChaiConstant.ATTR_LDAP_OBJECTCLASS );
        for ( final String loopClass : baseObjectClasses )
        {
            objectClassAttr.add( loopClass );
        }
        attrs.put( objectClassAttr );

        //Add each of the attributes required.
        for ( final Map.Entry<String, String> entry : stringAttributes.entrySet() )
        {
            attrs.put( entry.getKey(), entry.getValue() );
        }

        // Create the object.
        final DirContext ldapConnection = getLdapConnection();
        try
        {
            ldapConnection.createSubcontext( addJndiEscape( entryDN ), attrs );
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
    }

    @Override
    @ChaiProvider.LdapOperation
    @ChaiProvider.ModifyOperation
    public void renameEntry( final String entryDN, final String newRDN, final String newParentDN )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        activityPreCheck();
        getInputValidator().renameEntry( entryDN, newRDN, newParentDN );

        final LdapContext ldapConnection = getLdapConnection();
        try
        {
            final String newDN = newRDN + "," + newParentDN;
            ldapConnection.rename( entryDN, newDN );
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
    }

    @Override
    @LdapOperation
    @ModifyOperation
    public final void deleteEntry( final String entryDN )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().deleteEntry( entryDN );

        final LdapContext ldapConnection = getLdapConnection();
        try
        {
            ldapConnection.destroySubcontext( addJndiEscape( entryDN ) );
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
    }

    @Override
    @LdapOperation
    @ModifyOperation
    public final void deleteStringAttributeValue( final String entryDN, final String attributeName, final String attributeValue )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().deleteStringAttributeValue( entryDN, attributeName, attributeValue );

        // Create a BasicAttribute for the object.
        final BasicAttribute attributeToReplace = new BasicAttribute( attributeName, attributeValue );

        // Create the ModificationItem
        final ModificationItem[] modificationItem = new ModificationItem[1];

        // Populate the ModificationItem object with the flag & the attribute to replace.
        modificationItem[0] = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, attributeToReplace );

        // Modify the Attributes.
        final LdapContext ldapConnection = getLdapConnection();
        try
        {
            ldapConnection.modifyAttributes( addJndiEscape( entryDN ), modificationItem );
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
    }

    @Override
    @LdapOperation
    @ModifyOperation
    public final ExtendedResponse extendedOperation( final ExtendedRequest request )
            throws ChaiUnavailableException, ChaiOperationException

    {
        activityPreCheck();

        getInputValidator().extendedOperation( request );

        preCheckExtendedOperation( request );

        final LdapContext ldapConnection = getLdapConnection();

        try
        {
            return ldapConnection.extendedOperation( request );
        }
        catch ( NamingException e )
        {
            cacheExtendedOperationException( request, e );

            throw convertNamingException( e );
        }
        catch ( Throwable t )
        {
            throw new IllegalStateException( "unexpected chai api error: " + t.getMessage(), t );
        }
    }


    @Override
    public ChaiConfiguration getChaiConfiguration()
    {
        return chaiConfig;
    }

    @Override
    public ProviderStatistics getProviderStatistics()
    {
        return null;
    }

    @Override
    @LdapOperation
    // legacy code, suppress should be removed in future
    @SuppressFBWarnings( "DCN_NULLPOINTER_EXCEPTION" )
    public final byte[][] readMultiByteAttribute( final String entryDN, final String attributeName )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().readMultiByteAttribute( entryDN, attributeName );

        final List<Byte[]> returnValues = new ArrayList<>();
        final String jndiBinarySetting = "java.naming.ldap.attributes.binary";
        NamingEnumeration<?> namingEnum = null;

        // Get only the Attribute that is passed in.
        final String[] attributesArray = {attributeName};

        try
        {
            final LdapContext ldapConnection = ( LdapContext ) getLdapConnection().lookup( "" );

            // inform jndi the attribute is binary.
            ldapConnection.addToEnvironment( jndiBinarySetting, attributeName );

            // Get the Enumeration of attribute values.
            namingEnum = ldapConnection.getAttributes( addJndiEscape( entryDN ), attributesArray ).get( attributeName ).getAll();
            while ( namingEnum.hasMore() )
            {
                final Object value = namingEnum.next();

                if ( value instanceof byte[] )
                {
                    final Byte[] objectValue = toObject( ( byte[] ) value );
                    returnValues.add( objectValue );
                }
            }

            // Return the list as a set of primitives.
            final byte[][] returnArray = new byte[returnValues.size()][];
            for ( int i = 0; i < returnValues.size(); i++ )
            {
                returnArray[i] = toPrimitive( returnValues.get( i ) );
            }

            return returnArray;
        }
        catch ( NullPointerException e )
        {
            return new byte[0][0];
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
        finally
        {
            // close the enumeration
            try
            {
                if ( namingEnum != null )
                {
                    namingEnum.close();
                }
            }
            catch ( NamingException e )
            {
                LOGGER.trace( () -> "unexpected error closing naming exception: " + e.getMessage() );
            }
        }
    }

    @Override
    @LdapOperation
    // legacy code, new spotbugs detection, suppress should be removed in future
    @SuppressFBWarnings( "DCN_NULLPOINTER_EXCEPTION" )
    public final Set<String> readMultiStringAttribute( final String entryDN, final String attributeName )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().readMultiStringAttribute( entryDN, attributeName );

        final Set<String> attributeValues = new HashSet<>();
        NamingEnumeration<?> namingEnum = null;

        try
        {
            // Get only the Attribute that is passed in.
            final String[] attributesArray = {attributeName};

            // Get the Enumeration of attribute values.
            final LdapContext ldapConnection = getLdapConnection();

            namingEnum = ldapConnection.getAttributes( addJndiEscape( entryDN ), attributesArray ).get( attributeName ).getAll();
            while ( namingEnum.hasMore() )
            {
                attributeValues.add( namingEnum.next().toString() );
            }

            // Return the list as an array.
            return attributeValues;
        }
        catch ( NullPointerException e )
        {
            // to be consistent with nps impl.
            return Collections.emptySet();
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
        finally
        {
            try
            {
                if ( namingEnum != null )
                {
                    namingEnum.close();
                }
            }
            catch ( NamingException e )
            {
                LOGGER.trace( () -> "unexpected error closing naming exception: " + e.getMessage() );
            }
        }
    }

    @Override
    @LdapOperation
    public final String readStringAttribute( final String entryDN, final String attributeName )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().readStringAttribute( entryDN, attributeName );

        return readStringAttributes( entryDN, Collections.singleton( attributeName ) ).get( attributeName );
    }

    @Override
    @LdapOperation
    public final Map<String, String> readStringAttributes( final String entryDN, final Set<String> attributes )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().readStringAttributes( entryDN, attributes );

        // Allocate a return object
        final Map<String, String> returnObj = new LinkedHashMap<>();

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Get only the Attribute that is passed in.
        final Attributes returnedAttribs;

        NamingEnumeration attrEnumeration = null;

        try
        {
            if ( attributes == null || attributes.isEmpty() )
            {
                returnedAttribs = ldapConnection.getAttributes( addJndiEscape( entryDN ), null );
                attrEnumeration = returnedAttribs.getAll();
                while ( attrEnumeration.hasMoreElements() )
                {
                    final Attribute attribute = ( Attribute ) attrEnumeration.nextElement();

                    // Put an entry in the map, if there are no values insert null, otherwise, insert the first value
                    if ( attribute != null )
                    {
                        returnObj.put( attribute.getID(), attribute.get().toString() );
                    }
                }
            }
            else
            {
                // Loop through each requested attribute
                returnedAttribs = ldapConnection.getAttributes( addJndiEscape( entryDN ), attributes.toArray( new String[0] ) );
                for ( final String loopAttr : attributes )
                {
                    // Ask JNDI for the attribute (which actually includes all the values)
                    final Attribute attribute = returnedAttribs.get( loopAttr );

                    // Put an entry in the map, if there are no values insert null, otherwise, insert the first value
                    if ( attribute != null )
                    {
                        returnObj.put( loopAttr, attribute.get().toString() );
                    }
                }
            }
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
        finally
        {
            if ( attrEnumeration != null )
            {
                try
                {
                    attrEnumeration.close();
                }
                catch ( NamingException e )
                {
                    // nothing to do
                }
            }
        }
        return returnObj;
    }

    @Override
    @LdapOperation
    @ModifyOperation
    public final void replaceStringAttribute( final String entryDN, final String attributeName, final String oldValue, final String newValue )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().replaceStringAttribute( entryDN, attributeName, oldValue, newValue );

        // Create the ModificationItem
        final ModificationItem[] mods = new ModificationItem[2];

        // Mark the flag to remover the existing attribute.
        mods[0] = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, new BasicAttribute( attributeName, oldValue ) );

        // Mark the flag to add the new attribute
        mods[1] = new ModificationItem( DirContext.ADD_ATTRIBUTE, new BasicAttribute( attributeName, newValue ) );

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Modify the Attributes.
        try
        {
            ldapConnection.modifyAttributes( addJndiEscape( entryDN ), mods );
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
    }

    @Override
    @LdapOperation
    public final Map<String, Map<String, String>> search( final String baseDN, final SearchHelper searchHelper )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().search( baseDN, searchHelper );

        // perform search
        final SearchEngine searchEngine = new SearchEngine( chaiConfig, baseDN, searchHelper, false );
        final Map<String, Map<String, List<String>>> results = searchEngine.getResults();

        // convert to <String, Properties> return set.
        if ( results != null )
        {
            final Map<String, Map<String, String>> returnMap = new HashMap<>( results.size() );
            for ( final Map.Entry<String, Map<String, List<String>>> resultEntry : results.entrySet() )
            {
                final String entryDN = resultEntry.getKey();
                final Map<String, List<String>> attributeMap = resultEntry.getValue();
                final Map<String, String> newProps = new LinkedHashMap<>();
                for ( final Map.Entry<String, List<String>> attributeEntry : attributeMap.entrySet() )
                {
                    final String attrName = attributeEntry.getKey();
                    final List<String> values = attributeEntry.getValue();
                    newProps.put( attrName, values.get( 0 ) );
                }
                returnMap.put( entryDN, Collections.unmodifiableMap( newProps ) );
            }
            return Collections.unmodifiableMap( returnMap );
        }
        else
        {
            return Collections.emptyMap();
        }
    }

    @Override
    @LdapOperation
    public final Map<String, Map<String, String>> search( final String baseDN, final String filter, final Set<String> attributes, final SearchScope searchScope )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().search( baseDN, filter, attributes, searchScope );

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter( filter );
        searchHelper.setAttributes( attributes );
        searchHelper.setSearchScope( searchScope );

        return this.search( baseDN, searchHelper );
    }

    @Override
    public final Map<String, Map<String, List<String>>> searchMultiValues( final String baseDN, final SearchHelper searchHelper )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().searchMultiValues( baseDN, searchHelper );
        final SearchEngine searchEngine = new SearchEngine( chaiConfig, baseDN, searchHelper, true );
        return searchEngine.getResults();
    }

    @Override
    public final Map<String, Map<String, List<String>>> searchMultiValues( final String baseDN, final String filter, final Set<String> attributes, final SearchScope searchScope )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().searchMultiValues( baseDN, filter, attributes, searchScope );

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter( filter );
        searchHelper.setAttributes( attributes );
        searchHelper.setSearchScope( searchScope );

        final SearchEngine searchEngine = new SearchEngine( chaiConfig, baseDN, searchHelper, true );
        return searchEngine.getResults();
    }

    @Override
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
        writeBinaryAttribute( entryDN, attributeName, values, overwrite, null );
    }

    @Override
    public final void writeBinaryAttribute(
            final String entryDN,
            final String attributeName,
            final byte[][] values,
            final boolean overwrite,
            final ChaiRequestControl[] controls
    )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().writeBinaryAttribute( entryDN, attributeName, values, overwrite );

        final String jndiBinarySetting = "java.naming.ldap.attributes.binary";

        // Create the ModificationItem
        final ModificationItem[] modificationItem = new ModificationItem[values.length];
        for ( int i = 0; i < values.length; i++ )
        {
            // Create a BasicAttribute for the object.
            final BasicAttribute attributeToReplace = new BasicAttribute( attributeName, values[i] );

            // Determine the modification type, if replace, only replace on the first attribute, the rest just get added.
            final int modType = ( i == 0 && overwrite ) ? DirContext.REPLACE_ATTRIBUTE : DirContext.ADD_ATTRIBUTE;

            // Populate the ModificationItem object with the flag & the attribute to replace.
            modificationItem[i] = new ModificationItem( modType, attributeToReplace );
        }

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Modify the Attributes.
        try
        {
            if ( controls != null && controls.length > 0 )
            {
                ldapConnection.setRequestControls( convertControls( controls ) );
            }

            ldapConnection.modifyAttributes( addJndiEscape( entryDN ), modificationItem );
            // inform jndi the attribute is binary.
            ldapConnection.addToEnvironment( jndiBinarySetting, attributeName );
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
        finally
        {
            // clean up jndi environment
            try
            {
                ldapConnection.removeFromEnvironment( jndiBinarySetting );
            }
            catch ( Exception e )
            {
                //doesnt matter
            }
        }
    }

    @Override
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
        getInputValidator().replaceBinaryAttribute( entryDN, attributeName, oldValue, newValue );

        final String jndiBinarySetting = "java.naming.ldap.attributes.binary";

        // Create the ModificationItem
        final ModificationItem[] modificationItem = new ModificationItem[2];
        {
            // Create a BasicAttribute for the old value.
            final BasicAttribute oldValueOperation = new BasicAttribute( attributeName, oldValue );

            // Populate the ModificationItem array with the removal of the old value.
            modificationItem[0] = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, oldValueOperation );

            // Create a BasicAttribute for the new value.
            final BasicAttribute newValueOperation = new BasicAttribute( attributeName, newValue );

            // Populate the ModificationItem array with the removal of the old value.
            modificationItem[1] = new ModificationItem( DirContext.ADD_ATTRIBUTE, newValueOperation );
        }

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Modify the Attributes.
        try
        {
            ldapConnection.modifyAttributes( addJndiEscape( entryDN ), modificationItem );
            // inform jndi the attribute is binary.
            ldapConnection.addToEnvironment( jndiBinarySetting, attributeName );
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
        finally
        {
            // clean up jndi environment
            try
            {
                ldapConnection.removeFromEnvironment( jndiBinarySetting );
            }
            catch ( Exception e )
            {
                //doesnt matter
            }
        }
    }

    @Override
    @LdapOperation
    @ModifyOperation
    public final void writeStringAttribute( final String entryDN, final String attributeName, final Set<String> values, final boolean overwrite )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().writeStringAttribute( entryDN, attributeName, values, overwrite );


        // Create the ModificationItem
        final ModificationItem[] modificationItem = new ModificationItem[values.size()];

        int loopCounter = 0;
        for ( final String value : values )
        {
            // Create a BasicAttribute for the object.
            final BasicAttribute attributeToReplace = new BasicAttribute( attributeName, value );

            // Determine the modification type, if replace, only replace on the first attribute, the rest just get added.
            final int modType = ( loopCounter == 0 && overwrite ) ? DirContext.REPLACE_ATTRIBUTE : DirContext.ADD_ATTRIBUTE;

            // Populate the ModificationItem object with the flag & the attribute to replace.
            modificationItem[loopCounter] = new ModificationItem( modType, attributeToReplace );
            loopCounter++;
        }

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Modify the Attributes.
        try
        {
            ldapConnection.modifyAttributes( addJndiEscape( entryDN ), modificationItem );
        }
        catch ( NamingException e )
        {
            LOGGER.trace( () -> "error during write of attribute '" + attributeName + "', error: " + e.getMessage() );
            throw convertNamingException( e );
        }
    }

    @Override
    @LdapOperation
    @ModifyOperation
    public final void writeStringAttributes( final String entryDN, final Map<String, String> attributeValueProps, final boolean overwrite )
            throws ChaiUnavailableException, ChaiOperationException
    {
        writeStringAttributes( entryDN, attributeValueProps, overwrite, null );
    }

    public final void writeStringAttributes(
            final String entryDN,
            final Map<String, String> attributeValueProps,
            final boolean overwrite,
            final BasicControl[] controls
    )
            throws ChaiUnavailableException, ChaiOperationException
    {
        activityPreCheck();
        getInputValidator().writeStringAttributes( entryDN, attributeValueProps, overwrite );

        // Determine the modification type, if replace, only replace on the first attribute, the rest just get added.
        final int modType = overwrite ? DirContext.REPLACE_ATTRIBUTE : DirContext.ADD_ATTRIBUTE;

        // Create the ModificationItem
        final List<ModificationItem> modificationItems = new ArrayList<>();
        for ( final Map.Entry<String, String> entry : attributeValueProps.entrySet() )
        {
            // Create a BasicAttribute for the object.
            final BasicAttribute attributeToReplace = new BasicAttribute( entry.getKey(), entry.getValue() );

            // Populate the ModificationItem object with the flag & the attribute to replace.
            modificationItems.add( new ModificationItem( modType, attributeToReplace ) );
        }

        // convert to array
        final ModificationItem[] modificationItemArray = modificationItems.toArray( new ModificationItem[0] );

        // get ldap connection
        final LdapContext ldapConnection = getLdapConnection();

        // Modify the Attributes.
        try
        {
            ldapConnection.modifyAttributes( addJndiEscape( entryDN ), modificationItemArray );
        }
        catch ( NamingException e )
        {
            throw convertNamingException( e );
        }
    }

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
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException if no valid {@code LdapContext} is currently allocated.
     */
    @Override
    public Object getConnectionObject()
            throws Exception
    {
        return getLdapConnection();
    }

    @Override
    public String getCurrentConnectionURL()
    {
        return this.getChaiConfiguration().getDebugUrl();
    }

    @Override
    public void init( final ChaiConfiguration chaiConfig, final ChaiProviderFactory providerFactory )
            throws ChaiUnavailableException, IllegalStateException
    {
        this.chaiConfig = chaiConfig;
        final String connectionURL = chaiConfig.bindURLsAsList().get( 0 );
        final Hashtable<String, Object> env = generateJndiEnvironment( connectionURL );
        try
        {
            jndiConnection = generateNewJndiContext( env );
        }
        catch ( ChaiOperationException e )
        {
            throw new ChaiUnavailableException( "bind failed (" + e.getMessage() + ")", e.getErrorCode(), e );
        }

        super.init( chaiConfig, providerFactory );
    }

    private Hashtable<String, Object> generateJndiEnvironment( final String ldapURL )
    {
        // Populate the hashtable with the attributes to connect to eDirectory.
        final Hashtable<String, Object> env = new Hashtable<>();

        // add in basic connection info
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, ldapURL );
        env.put( Context.SECURITY_PRINCIPAL, addJndiEscape( chaiConfig.getSetting( ChaiSetting.BIND_DN ) ) );
        env.put( Context.SECURITY_CREDENTIALS, chaiConfig.getBindPassword() );

        // set the JNDI pooler up
        final boolean jndiConnectionPoolEnable = Boolean.valueOf( chaiConfig.getSetting( ChaiSetting.JNDI_ENABLE_POOL ) );
        if ( jndiConnectionPoolEnable )
        {
            env.put( "com.sun.jndi.ldap.connect.pool", "true" );
            env.put( "com.sun.jndi.ldap.connect.pool.initsize", String.valueOf( DEFAULT_INITIAL_POOL_SIZE ) );
            env.put( "com.sun.jndi.ldap.connect.pool.maxsize", String.valueOf( DEFAULT_MAXIMUM_POOL_SIZE ) );
            env.put( "com.sun.jndi.ldap.connect.pool.prefsize", String.valueOf( DEFAULT_PREFERRED_POOL_SIZE ) );
        }

        // connect using plaintext or plaintext/ssl
        env.put( "com.sun.jndi.ldap.connect.pool.protocol", "plain ssl" );

        // Set the ldap timeout time.
        env.put( "com.sun.jndi.ldap.connect.timeout", chaiConfig.getSetting( ChaiSetting.LDAP_CONNECT_TIMEOUT ) );

        // Set the ldap read timeout time.
        if ( chaiConfig.getIntSetting( ChaiSetting.LDAP_READ_TIMEOUT ) > 0 )
        {
            env.put( "com.sun.jndi.ldap.read.timeout", chaiConfig.getSetting( ChaiSetting.LDAP_READ_TIMEOUT ) );
        }

        //set alias de-referencing
        env.put( "java.naming.ldap.derefAliases", chaiConfig.getSetting( ChaiSetting.LDAP_DEREFENCE_ALIAS ) );

        //set referrals
        if ( chaiConfig.getBooleanSetting( ChaiSetting.LDAP_FOLLOW_REFERRALS ) )
        {
            env.put( Context.REFERRAL, "follow" );
        }

        final boolean isSecureLdapURL = ( URI.create( ldapURL ) ).getScheme().equalsIgnoreCase( "ldaps" );

        //setup blind SSL socket factory
        final boolean promiscuousMode = chaiConfig.getBooleanSetting( ChaiSetting.PROMISCUOUS_SSL );
        if ( isSecureLdapURL )
        {
            if ( promiscuousMode )
            {
                try
                {
                    final SSLContext sc = SSLContext.getInstance( "SSL" );
                    sc.init( null, new X509TrustManager[] {new PromiscuousTrustManager()}, new java.security.SecureRandom() );
                    socketFactory = sc.getSocketFactory();
                    ThreadLocalSocketFactory.set( socketFactory );
                    env.put( "java.naming.ldap.factory.socket", ThreadLocalSocketFactory.class.getName() );
                }
                catch ( NoSuchAlgorithmException | KeyManagementException e )
                {
                    LOGGER.error( () -> "error configuring promiscuous socket factory" );
                }

            }
            else if ( chaiConfig.getTrustManager() != null )
            {
                try
                {
                    final SSLContext sc = SSLContext.getInstance( "SSL" );
                    sc.init( null, chaiConfig.getTrustManager(), new java.security.SecureRandom() );
                    socketFactory = sc.getSocketFactory();
                    ThreadLocalSocketFactory.set( socketFactory );

                    env.put( "java.naming.ldap.factory.socket", ThreadLocalSocketFactory.class.getName() );
                }
                catch ( NoSuchAlgorithmException | KeyManagementException e )
                {
                    LOGGER.error( () -> "error configuring socket factory from configured trust manager" );
                }
            }
        }

        // mix in default environment settings
        if ( chaiConfig.getImplementationConfiguration() != null && chaiConfig.getImplementationConfiguration() instanceof Map )
        {
            final Map defaultEnvironment = ( Map ) chaiConfig.getImplementationConfiguration();

            for ( final Map.Entry entry : ( Set<Map.Entry> ) defaultEnvironment.entrySet() )
            {
                final Object key = entry.getKey();
                final Object value = entry.getValue();
                if ( key instanceof String && value instanceof String )
                {
                    env.put( ( String ) key, ( String ) value );
                }
            }
        }

        return env;
    }

    private class SearchEngine
    {
        private final String baseDN;
        private final SearchHelper searchHelper;
        private final boolean returnAllValues;
        private final ChaiConfiguration chaiConfiguration;

        private boolean used = false;
        private final Map<String, Map<String, List<String>>> results = new HashMap<>();

        SearchEngine(
                final ChaiConfiguration chaiConfiguration,
                final String baseDN,
                final SearchHelper searchHelper,
                final boolean returnAllValues
        )
                throws ChaiOperationException
        {
            this.baseDN = baseDN != null ? baseDN : "";

            // make a copy so if it changes somewhere else we won't be affected.
            this.searchHelper = new SearchHelper( searchHelper );
            this.returnAllValues = returnAllValues;

            this.chaiConfiguration = chaiConfiguration;
        }

        public Map<String, Map<String, List<String>>> getResults()
                throws ChaiUnavailableException, ChaiOperationException
        {
            if ( used )
            {
                throw new IllegalStateException( "SearchEngine instance can only be used once" );
            }
            used = true;

            activityPreCheck();

            // Define the Search Controls object.
            final SearchControls searchControls = makeSearchControls();


            final int maxPageSize = getChaiConfiguration().getIntSetting( ChaiSetting.LDAP_SEARCH_PAGING_SIZE );

            // enabling paging if search count is unlimited (0) or bigger than the max page size.
            final boolean pagingEnabled = ( searchControls.getCountLimit() == 0 || ( searchControls.getCountLimit() > maxPageSize )
                    && supportsSearchResultPaging() );

            final LdapContext ldapConnection = getLdapConnection();

            NamingEnumeration<SearchResult> answer = null;
            try
            {
                byte[] pageCookie = null;
                do
                {
                    // set the paging control if using paging
                    if ( pagingEnabled )
                    {
                        final Control pagedControl = pageCookie == null
                                ? new PagedResultsControl( maxPageSize, Control.NONCRITICAL )
                                : new PagedResultsControl( maxPageSize, pageCookie, Control.CRITICAL );
                        ldapConnection.setRequestControls( new Control[] {pagedControl} );
                    }

                    // execute the search
                    answer = ldapConnection.search( addJndiEscape( baseDN ), searchHelper.getFilter(), searchControls );

                    // read search results from ldap into the result map
                    final int previousResultSize = results.size();
                    parseSearchResults( answer );
                    if ( pageCookie != null && previousResultSize == results.size() )
                    {
                        LOGGER.warn( () -> "ldap paged search has returned an empty result page, current result size=" + results.size() );
                    }

                    // if paging enabled, read the cookie value.
                    pageCookie = pagingEnabled
                            ? readResultResponsePageCookie( ldapConnection.getResponseControls() )
                            : null;

                    // loop until no more paged results.
                } while ( pagingEnabled && pageCookie != null );
            }
            catch ( SizeLimitExceededException e )
            {
                // ignore
            }
            catch ( IOException e )
            {
                throw new ChaiOperationException( "io error during paged search result: " + e.getMessage(), ChaiError.COMMUNICATION );
            }
            catch ( NamingException e )
            {
                throw convertNamingException( e );
            }
            finally
            {
                if ( answer != null )
                {
                    try
                    {
                        answer.close();
                    }
                    catch ( NamingException e )
                    {
                        // nothing to do
                    }
                }
            }

            return Collections.unmodifiableMap( results );
        }

        private SearchControls makeSearchControls()
        {
            final SearchControls searchControls = new SearchControls();
            searchControls.setReturningObjFlag( false );
            searchControls.setReturningAttributes( new String[0] );
            searchControls.setSearchScope( searchHelper.getSearchScope().getJndiScopeInt() );
            final String[] returnAttributes = searchHelper.getAttributes() == null
                    ? null
                    : searchHelper.getAttributes().toArray( new String[0] );

            searchControls.setReturningAttributes( returnAttributes );
            searchControls.setTimeLimit( searchHelper.getTimeLimit() );
            searchControls.setCountLimit( searchHelper.getMaxResults() );
            return searchControls;
        }

        private byte[] readResultResponsePageCookie( final Control[] controls )
        {
            if ( controls != null )
            {
                for ( Control control : controls )
                {
                    if ( control instanceof PagedResultsResponseControl )
                    {
                        final PagedResultsResponseControl prrc = ( PagedResultsResponseControl ) control;
                        final byte[] cookie = prrc.getCookie();
                        if ( cookie != null )
                        {
                            return cookie;
                        }
                    }
                }
            }
            return null;
        }

        private void parseSearchResults(
                final NamingEnumeration<SearchResult> answer
        )
                throws NamingException
        {
            while ( answer.hasMore() )
            {
                final SearchResult searchResult = answer.next();

                String entryDN = null;
                if ( chaiConfiguration.getBooleanSetting( ChaiSetting.JNDI_RESOLVE_IN_NAMESPACE ) )
                {
                    try
                    {
                        entryDN = searchResult.getNameInNamespace();
                        entryDN = removeJndiEscapes( entryDN );
                    }
                    catch ( UnsupportedOperationException e )
                    {
                        LOGGER.debug( () -> "unable to use jndi NameInNamespace api: " + e.getMessage() );
                    }
                }

                if ( entryDN == null )
                {
                    final StringBuilder entryDNbuilder = new StringBuilder();
                    entryDNbuilder.append( removeJndiEscapes( searchResult.getName() ) );
                    if ( baseDN.length() > 0 )
                    {
                        if ( entryDNbuilder.length() > 0 )
                        {
                            entryDNbuilder.append( ',' );
                        }
                        entryDNbuilder.append( baseDN );
                    }

                    entryDN = entryDNbuilder.toString();
                }

                final Map<String, List<String>> attrValues = new HashMap<>();
                {
                    final NamingEnumeration attributeEnum = searchResult.getAttributes().getAll();
                    attrValues.putAll( parseAttributeValues( attributeEnum, returnAllValues ) );
                }

                final String finalEntryDN = entryDN;
                if ( results.containsKey( entryDN ) )
                {
                    LOGGER.warn( () -> "ignoring duplicate DN in search result from ldap server: " + finalEntryDN );
                }
                else
                {
                    results.put( entryDN, Collections.unmodifiableMap( attrValues ) );
                }
            }
        }
    }

    private Map<String, List<String>> parseAttributeValues(
            final NamingEnumeration attributeEnum,
            final boolean returnAllValues
    )
            throws NamingException
    {
        final Map<String, List<String>> attrValues = new HashMap<>();
        if ( attributeEnum != null && attributeEnum.hasMore() )
        {
            while ( attributeEnum.hasMore() )
            {
                final Attribute loopAttribute = ( Attribute ) attributeEnum.next();
                final String attrName = loopAttribute.getID();
                final List<String> valueList = new ArrayList<>();
                for ( NamingEnumeration attrValueEnum = loopAttribute.getAll(); attrValueEnum.hasMore(); )
                {
                    final Object value = attrValueEnum.next();
                    valueList.add( value.toString() );
                    if ( !returnAllValues )
                    {
                        attrValueEnum.close();
                        break;
                    }
                }
                attrValues.put( attrName, Collections.unmodifiableList( valueList ) );
            }
        }
        return Collections.unmodifiableMap( attrValues );
    }

    private boolean supportsSearchResultPaging()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final String enableSettingStr = this.getChaiConfiguration().getSetting( ChaiSetting.LDAP_SEARCH_PAGING_ENABLE );
        if ( "auto".equalsIgnoreCase( enableSettingStr ) )
        {
            if ( cachedPagingEnableSupport == null )
            {
                final ChaiEntry rootDse = ChaiUtility.getRootDSE( this );
                final Set<String> supportedControls = rootDse.readMultiStringAttribute( "supportedControl" );
                cachedPagingEnableSupport = supportedControls.contains( PagedResultsControl.OID );
            }
            return cachedPagingEnableSupport;
        }
        return Boolean.parseBoolean( enableSettingStr );
    }


    private LdapContext getLdapConnection()
            throws ChaiUnavailableException
    {
        try
        {
            if ( socketFactory != null )
            {
                ThreadLocalSocketFactory.set( socketFactory );
            }
            return jndiConnection.newInstance( null );
        }
        catch ( NamingException e )
        {
            final String errorMsg = "error creating new jndiConnection instance: " + e.getMessage();
            throw new ChaiUnavailableException( errorMsg, ChaiError.CHAI_INTERNAL_ERROR, e );
        }
    }

    private ChaiOperationException convertNamingException( final NamingException e )
            throws ChaiUnavailableException
    {
        // important safety tip: naming exceptions sometimes come with null messages....
        final StringBuilder errorMsg = new StringBuilder();
        errorMsg.append( e.getClass().getName() );
        if ( e.getMessage() != null )
        {
            errorMsg.append( ": " ).append( e.getMessage() );
        }

        Throwable cause = e.getCause();
        int safetyCounter = 0;
        while ( cause != null && safetyCounter < 10 )
        {
            safetyCounter++;
            errorMsg.append( ", cause:" ).append( cause.getClass().getName() );
            if ( cause.getMessage() != null )
            {
                errorMsg.append( ": " ).append( cause.getMessage() );
            }
            cause = cause.getCause();
        }

        if ( errorIsRetryable( e ) )
        {
            throw new ChaiUnavailableException( errorMsg.toString(), ChaiError.COMMUNICATION, false, false, e );
        }

        return ChaiOperationException.forErrorMessage( errorMsg.toString(), e );
    }


    @Override
    public boolean errorIsRetryable( final Exception e )
    {
        if ( e instanceof CommunicationException || e instanceof ServiceUnavailableException )
        {
            final String msgText = e.getMessage();
            if ( msgText != null && !msgText.toLowerCase().contains( "unrecognized extended operation" ) )
            {
                return true;
            }
        }

        if ( e instanceof InterruptedException || e instanceof InterruptedNamingException )
        {
            LOGGER.trace( () -> "operation resulted in InterruptedNamingException, will retry; error=" + e.getMessage() );
            return true;
        }

        return super.errorIsRetryable( e );
    }

    @Override
    public boolean isConnected()
    {
        return jndiConnection != null;
    }

    protected static String removeJndiEscapes( final String input )
    {
        if ( input == null )
        {
            return null;
        }

        // remove surrounding quotes if the internal value contains a / character
        final String slashEscapePattern = "^\".*/.*\"$";
        if ( input.matches( slashEscapePattern ) )
        {
            return input.replaceAll( "^\"|\"$", "" );
        }
        return input;
    }

    protected static String addJndiEscape( final String input )
    {
        if ( input == null )
        {
            return null;
        }
        return input.replaceAll( "/", "\\\\2f" );
    }

    protected static BasicControl[] convertControls( final ChaiRequestControl[] controls )
    {
        if ( controls == null )
        {
            return null;
        }

        final BasicControl[] newControls = new BasicControl[controls.length];
        for ( int i = 0; i < controls.length; i++ )
        {
            newControls[i] = new BasicControl(
                    controls[i].getId(),
                    controls[i].isCritical(),
                    controls[i].getValue()
            );
        }
        return newControls;
    }

    public abstract static class ThreadLocalSocketFactory
            extends SocketFactory
    {

        static ThreadLocal<SocketFactory> local = new ThreadLocal<>();

        public static SocketFactory getDefault()
        {
            final SocketFactory result = local.get();
            if ( result == null )
            {
                throw new IllegalStateException( "missing threadlocal socketfactory for ChaiProvider" );
            }
            return result;
        }

        public static void set( final SocketFactory factory )
        {
            local.set( factory );
        }

        public static void remove()
        {
            local.remove();
        }

    }
}
