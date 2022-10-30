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

package com.novell.ldapchai.util;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.generic.entry.GenericEntryFactory;
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.provider.DirectoryVendor;
import com.novell.ldapchai.provider.SearchScope;
import com.novell.ldapchai.util.internal.ChaiLogger;

import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A collection of static helper methods used by the LDAP Chai API.
 *
 * @author Jason D. Rivard
 */
public final class ChaiUtility
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( ChaiUtility.class );

    private static final Supplier<SecureRandom> SECURE_RANDOM_SUPPLIER = SecureRandom::new;

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
    public static ChaiGroup createGroup( final String parentDN, final String name, final ChaiProvider provider )
            throws ChaiOperationException, ChaiUnavailableException
    {
        //Get a good CN for it
        final String objectCN = findUniqueName( name, parentDN, provider );

        //Concantonate the entryDN
        final StringBuilder entryDN = new StringBuilder();
        entryDN.append( "cn=" );
        entryDN.append( objectCN );
        entryDN.append( ',' );
        entryDN.append( parentDN );

        //First create the base group.
        provider.createEntry( entryDN.toString(), ChaiConstant.OBJECTCLASS_BASE_LDAP_GROUP, Collections.emptyMap() );

        //Now build an ldapentry object to add attributes to it
        final ChaiEntry theObject = provider.getEntryFactory().newChaiEntry( entryDN.toString() );

        //Add the description
        theObject.writeStringAttribute( ChaiConstant.ATTR_LDAP_DESCRIPTION, name );

        //Return the newly created group.
        return provider.getEntryFactory().newChaiGroup( entryDN.toString() );
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
    public static String findUniqueName( final String baseName, final String containerDN, final ChaiProvider provider )
            throws ChaiOperationException, ChaiUnavailableException
    {
        char ch;
        final StringBuilder cnStripped = new StringBuilder();

        final String effectiveBasename = ( baseName == null )
                ? ""
                : baseName;

        // First boil down the root name. Preserve only the alpha-numerics.
        for ( int i = 0; i < effectiveBasename.length(); i++ )
        {
            ch = effectiveBasename.charAt( i );
            if ( Character.isLetterOrDigit( ch ) )
            {
                cnStripped.append( ch );
            }
        }

        if ( cnStripped.length() == 0 )
        {
            // Generate a random seed to runServer with, how about the current date
            cnStripped.append( System.currentTimeMillis() );
        }

        // Now we have a base name, let's runServer testing it...
        String uniqueCN;
        StringBuilder filter;

        String stringCounter = null;

        // Start with a random 3 digit number
        int counter = SECURE_RANDOM_SUPPLIER.get().nextInt( 1000 );
        while ( true )
        {
            // Initialize the String Buffer and Unique DN.
            filter = new StringBuilder( 64 );

            if ( stringCounter != null )
            {
                uniqueCN = cnStripped.append( stringCounter ).toString();
            }
            else
            {
                uniqueCN = cnStripped.toString();
            }
            filter.append( "(" ).append( ChaiConstant.ATTR_LDAP_COMMON_NAME ).append( "=" ).append( uniqueCN ).append( ")" );

            final Map<String, Map<String, String>> results = provider.search( containerDN, filter.toString(), null, SearchScope.ONE );
            if ( results.size() == 0 )
            {
                // No object found!
                break;
            }
            else
            {
                // Increment it every time
                stringCounter = Integer.toString( counter++ );
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
    public static ChaiUser createUser( final String userDN, final String sn, final ChaiProvider provider )
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Map<String, String> createAttributes = new HashMap<>();

        createAttributes.put( ChaiConstant.ATTR_LDAP_SURNAME, sn );

        provider.createEntry( userDN, ChaiConstant.OBJECTCLASS_BASE_LDAP_USER, createAttributes );

        //lets create a user object
        return provider.getEntryFactory().newChaiUser( userDN );
    }

    /**
     * Convert to an LDIF format.  Useful for debugging or other purposes
     *
     * @param theEntry A valid {@code ChaiEntry}
     * @return A string containing a properly formatted LDIF view of the entry.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    public static String entryToLDIF( final ChaiEntry theEntry )
            throws ChaiUnavailableException, ChaiOperationException
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "dn: " ).append( theEntry.getEntryDN() ).append( "\n" );

        final Map<String, Map<String, List<String>>> results = theEntry.getChaiProvider().searchMultiValues(
                theEntry.getEntryDN(),
                "(objectClass=*)",
                null,
                SearchScope.BASE
        );
        final Map<String, List<String>> props = results.get( theEntry.getEntryDN() );

        for ( final Map.Entry<String, List<String>> entry : props.entrySet() )
        {
            final String attrName = entry.getKey();
            final List<String> values = entry.getValue();
            for ( final String value : values )
            {
                sb.append( attrName ).append( ": " ).append( value ).append( '\n' );
            }
        }

        return sb.toString();
    }

    /**
     * <p>Test the replication of an attribute.  It is left to the implementation to determine the means and criteria for
     * this operation.  Typically this method would be used just after a write operation in some type of time delayed loop.
     * This method does not write any data to the directory.</p>
     *
     * <p>Typical implementations will do the following:</p>
     * <ul>
     * <li>issue {@link com.novell.ldapchai.ChaiEntry#readStringAttribute(String)} to read a value</li>
     * <li>establish an LDAP connection to all known replicas</li>
     * <li>issue {@link com.novell.ldapchai.ChaiEntry#compareStringAttribute(String, String)} to to each server directly</li>
     * <li>return true if each server contacted has the same value, false if not</li>
     * </ul>
     *
     * <p>Target servers that are unreachable or return errors are ignored, and do not influence the results. It is entirely
     * possible that no matter how many times this method is called, false will always be returned, so the caller should
     * take care not to repeat a test indefinitely.</p>
     *
     * <p>This operation is potentially expensive, as it may establish new LDAP level connections to each target server each
     * time it is invoked.</p>
     *
     * <p>The following sample shows how this method might be used.  There are a few important attributes of the sample:</p>
     * <ul>
     * <li>Multiple ldap servers are specified</li>
     * <li>There is a pause time between each replication check (the test can be expensive)</li>
     * <li>There is a timeout period (the test may never successfully complete)</li>
     * </ul>
     * <p><b>Example Usage:</b></p>
     * <pre>
     * // write a timestamp value to an attribute
     * theUser.writeStringAttributes("description","testValue" + Instant.now().toString());
     *
     * // maximum time to wait for replication
     * final int maximumWaitTime = 120 * 1000;
     *
     *  // time between iterations
     * final int pauseTime = 3 * 1000;
     *
     * // timestamp of beginning of wait
     * final long startTime = System.currentTimeMillis();
     *
     * boolean replicated = false;
     *
     * // loop until
     * while (System.currentTimeMillis() - startTime &lt; maximumWaitTime) {
     *
     *    // sleep between iterations
     *    try { Thread.sleep(pauseTime); } catch (InterruptedException e)  {}
     *
     *    // check if data replicated yet
     *    replicated = ChaiUtility.testAttributeReplication(theUser,"description",null);
     *
     *    // break if data has replicated
     *    if (replicated) {
     *        break;
     *    }
     * }
     *
     * // report success
     * System.out.println("Attribute replication successful: " + replicated);
     * </pre>
     *
     * @param chaiEntry A valid entry
     * @param attribute A valid attribute on the entry
     * @param value     The value to test for.  If {@code null}, a value is read from the active server
     * @return true if the attribute is the same on all servers
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    If the underlying connection is not in an available state
     */
    public static boolean testAttributeReplication( final ChaiEntry chaiEntry, final String attribute, final String value )
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String effectiveValue = ( value == null || value.length() < 1 )
                ? chaiEntry.readStringAttribute( attribute )
                : value;


        if ( effectiveValue == null )
        {
            throw ChaiOperationException.forErrorMessage( "unreadable to read test attribute from primary ChaiProvider" );
        }

        final ChaiConfiguration chaiConfiguration = chaiEntry.getChaiProvider().getChaiConfiguration();

        final List<String> ldapURLs = chaiConfiguration.bindURLsAsList();

        LOGGER.trace( () -> "testAttributeReplication, will test the following ldap urls: " + ldapURLs );

        int testCount = 0;
        int successCount = 0;

        final Collection<ChaiConfiguration> perReplicaProviders = splitConfigurationPerReplica(
                chaiEntry.getChaiProvider().getChaiConfiguration(),
                Collections.singletonMap( ChaiSetting.FAILOVER_CONNECT_RETRIES, "1" )
        );

        for ( final ChaiConfiguration loopConfiguration : perReplicaProviders )
        {
            ChaiProvider loopProvider = null;
            try
            {
                loopProvider = chaiEntry.getChaiProvider().getProviderFactory().newProvider( loopConfiguration );
                if ( loopProvider.compareStringAttribute( chaiEntry.getEntryDN(), attribute, effectiveValue ) )
                {
                    successCount++;
                }

                testCount++;
            }
            catch ( ChaiUnavailableException e )
            {
                //disregard
            }
            catch ( ChaiOperationException e )
            {
                //disregard
            }
            finally
            {
                try
                {
                    if ( loopProvider != null )
                    {
                        loopProvider.close();
                    }
                }
                catch ( Exception e )
                {
                    //already closed, whatever.
                }
            }
        }

        final int finalTestCount = testCount;
        final int finalSuccessCount = successCount;

        LOGGER.debug( () -> "testAttributeReplication for " + chaiEntry + ":" + attribute
                + " " + finalTestCount + " up," + " " + ( ldapURLs.size() - finalTestCount ) + " down,"
                + " " + finalSuccessCount + " in sync" );

        return testCount > 0 && testCount == successCount;
    }

    public static Collection<ChaiConfiguration> splitConfigurationPerReplica(
            final ChaiConfiguration chaiConfiguration,
            final Map<ChaiSetting, String> additionalSettings
    )
    {
        final Collection<ChaiConfiguration> returnProviders = new ArrayList<>();

        final List<String> ldapURLs = chaiConfiguration.bindURLsAsList();

        for ( final String loopURL : ldapURLs )
        {
            final ChaiConfiguration.ChaiConfigurationBuilder builder = ChaiConfiguration.builder( chaiConfiguration );
            builder.setSetting( ChaiSetting.BIND_URLS, loopURL );
            if ( additionalSettings != null )
            {
                for ( final Map.Entry<ChaiSetting, String> entry : additionalSettings.entrySet() )
                {
                    final String value = entry.getValue();
                    builder.setSetting( entry.getKey(), value );
                }
            }
            returnProviders.add( builder.build() );
        }

        return returnProviders;
    }


    private ChaiUtility()
    {
    }

    public static String passwordPolicyToString( final ChaiPasswordPolicy policy )
    {
        if ( policy == null )
        {
            throw new NullPointerException( "null ChaiPasswordPolicy can not be converted to string" );
        }

        final StringBuilder sb = new StringBuilder();

        sb.append( "ChaiPasswordPolicy: " );

        if ( !policy.getKeys().isEmpty() )
        {
            sb.append( "{" );
            for ( final String key : policy.getKeys() )
            {
                final ChaiPasswordRule rule = ChaiPasswordRule.forKey( key );
                sb.append( rule == null ? key : rule );
                sb.append( "=" );
                sb.append( policy.getValue( key ) );
                sb.append( ", " );
            }
            sb.delete( sb.length() - 2, sb.length() );
            sb.append( "}" );
        }
        else
        {
            sb.append( "[empty]" );
        }

        return sb.toString();
    }

    /**
     * Determines the vendor of a the ldap directory by reading RootDSE attributes.
     *
     * @param rootDSE A valid entry
     * @return the proper directory vendor, or {@link DirectoryVendor#GENERIC} if the vendor can not be determined.
     * @throws ChaiUnavailableException If the directory is unreachable
     * @throws ChaiOperationException   If there is an error reading values from the Root DSE entry
     */
    public static DirectoryVendor determineDirectoryVendor( final ChaiEntry rootDSE )
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Set<String> interestedAttributes = new HashSet<>();
        for ( final DirectoryVendor directoryVendor : DirectoryVendor.values() )
        {
            interestedAttributes.addAll( directoryVendor.getVendorFactory().interestedDseAttributes() );
        }

        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setAttributes( interestedAttributes.toArray( new String[0] ) );
        searchHelper.setFilter( SearchHelper.DEFAULT_FILTER );
        searchHelper.setMaxResults( 1 );
        searchHelper.setSearchScope( SearchScope.BASE );

        final Map<String, Map<String, List<String>>> results = rootDSE.getChaiProvider().searchMultiValues( "", searchHelper );
        if ( results != null && !results.isEmpty() )
        {
            final Map<String, List<String>> rootDseSearchResults = results.values().iterator().next();
            for ( final DirectoryVendor directoryVendor : DirectoryVendor.values() )
            {
                if ( directoryVendor.getVendorFactory().detectVendorFromRootDSEData( rootDseSearchResults ) )
                {
                    return directoryVendor;
                }
            }
        }

        return DirectoryVendor.GENERIC;
    }

    public static ChaiEntry getRootDSE( final ChaiProvider provider )
            throws ChaiUnavailableException
    {
        final List<String> splitUrls = provider.getChaiConfiguration().bindURLsAsList();

        final StringBuilder newUrlConfig = new StringBuilder();

        boolean currentURLsHavePath = false;
        for ( final String splitUrl : splitUrls )
        {
            final URI uri = URI.create( splitUrl );
            final String newURI = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
            newUrlConfig.append( newURI );
            if ( uri.getPath() != null && uri.getPath().length() > 0 )
            {
                currentURLsHavePath = true;
            }

            newUrlConfig.append( "," );
        }

        final ChaiConfiguration rootDSEChaiConfig = ChaiConfiguration.builder( provider.getChaiConfiguration() )
                .setSetting( ChaiSetting.BIND_URLS, newUrlConfig.toString() )
                .build();

        final ChaiProvider rootDseProvider = currentURLsHavePath
                ? provider.getProviderFactory().newProvider( rootDSEChaiConfig )
                : provider;

        // can not call the VendorFactory here, because VendorFactory in turn calls this method to get the
        // directory vendor.  Instead, we will go directly to the Generic VendorFactory
        final GenericEntryFactory genericEntryFactory = new GenericEntryFactory();
        return genericEntryFactory.newChaiEntry( "", rootDseProvider );
    }
}
