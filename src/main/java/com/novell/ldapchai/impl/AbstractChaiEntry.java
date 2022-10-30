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

package com.novell.ldapchai.impl;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.provider.SearchScope;
import com.novell.ldapchai.util.internal.ChaiLogger;
import com.novell.ldapchai.util.SearchHelper;
import com.novell.ldapchai.util.internal.StringHelper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * A complete implementation of {@code ChaiEntry} interface.
 * Clients looking to obtain a {@code ChaiEntry} instance should look to {@link com.novell.ldapchai.ChaiEntryFactory}.
 *
 * @author Jason D. Rivard
 */
public abstract class AbstractChaiEntry implements ChaiEntry
{

    private enum NetworkAddressType
    {
        IPv4( 9 );

        private final int typeIdentifier;

        NetworkAddressType( final int typeIdentifier )
        {
            this.typeIdentifier = typeIdentifier;
        }

        public int getTypeIdentifier()
        {
            return typeIdentifier;
        }

        public static NetworkAddressType forIdentifier( final int typeIdentifier )
        {
            for ( final NetworkAddressType type : NetworkAddressType.values() )
            {
                if ( type.getTypeIdentifier() == typeIdentifier )
                {
                    return type;
                }
            }
            return null;
        }
    }


    protected static final ChaiLogger LOGGER = ChaiLogger.getLogger( AbstractChaiEntry.class );
    /**
     * Stores the original dn, used in the constructor.
     */
    protected String entryDN;

    /**
     * Attribute to store the LDAP Provider.
     */
    protected ChaiProvider chaiProvider;

    /**
     * Standard constructor.
     *
     * @param entryDN      ldap DN in String format
     * @param chaiProvider an active {@code ChaiProvider} instance
     */
    public AbstractChaiEntry( final String entryDN, final ChaiProvider chaiProvider )
    {
        this.chaiProvider = chaiProvider;
        this.entryDN = entryDN == null ? "" : entryDN;
    }

    @Override
    public final ChaiProvider getChaiProvider()
    {
        return this.chaiProvider;
    }

    @Override
    public final String getEntryDN()
    {
        return this.entryDN;
    }

    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof ChaiEntry ) )
        {
            return false;
        }

        final AbstractChaiEntry chaiEntry = ( AbstractChaiEntry ) o;

        return !( chaiProvider != null
                ? !chaiProvider.equals( chaiEntry.chaiProvider )
                : chaiEntry.chaiProvider != null ) && !( entryDN != null
                ? !entryDN.equals( chaiEntry.entryDN )
                : chaiEntry.entryDN != null
        );
    }

    public int hashCode()
    {
        int result;
        result = ( entryDN != null ? entryDN.hashCode() : 0 );
        result = 29 * result + ( chaiProvider != null ? chaiProvider.hashCode() : 0 );
        return result;
    }

    /**
     * This function would display the object and its attributes.
     *
     * @return String
     */
    public String toString()
    {
        // Allocate the String Buffer
        final StringBuilder sb = new StringBuilder();

        sb.append( "EntryDN: " ).append( this.entryDN );

        // Return the String.
        return sb.toString();
    }

    @Override
    public final void addAttribute( final String attributeName, final String attributeValue )
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.writeStringAttribute( entryDN, attributeName, Collections.singleton( attributeValue ), false );
    }

    @Override
    public final void addAttribute( final String attributeName, final Set<String> attributeValues )
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.writeStringAttribute( entryDN, attributeName, attributeValues, false );
    }

    @Override
    public final void addAttribute( final String attributeName, final String... attributeValues )
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.writeStringAttribute( entryDN, attributeName, new LinkedHashSet<>( Arrays.asList( attributeValues ) ), false );
    }

    @Override
    public final boolean compareStringAttribute( final String attributeName, final String attributeValue )
            throws ChaiUnavailableException, ChaiOperationException
    {
        return chaiProvider.compareStringAttribute( this.getEntryDN(), attributeName, attributeValue );
    }

    @Override
    public final void deleteAttribute( final String attributeName, final String attributeValue )
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.deleteStringAttributeValue( this.entryDN, attributeName, attributeValue );
    }

    @Override
    public final Set<ChaiEntry> getChildObjects()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Set<ChaiEntry> returnSet = new LinkedHashSet<>();
        final String filter = "(" + ChaiConstant.ATTR_LDAP_OBJECTCLASS + "=*)";
        final Map<String, Map<String, String>> results = this.getChaiProvider().search(
                this.getEntryDN(),
                filter,
                Collections.emptySet(),
                SearchScope.ONE
        );

        for ( final String dn : results.keySet() )
        {
            returnSet.add( getChaiProvider().getEntryFactory().newChaiEntry( dn ) );
        }
        return returnSet;
    }

    @Override
    public final ChaiEntry getParentEntry()
            throws ChaiUnavailableException
    {
        final String parentDNString = getParentDNString( this.getEntryDN() );
        if ( parentDNString == null )
        {
            return null;
        }
        return getChaiProvider().getEntryFactory().newChaiEntry( parentDNString );
    }

    private static String getParentDNString( final String inputDN )
    {
        if ( inputDN == null || inputDN.length() < 0 )
        {
            return null;
        }
        final String dnSeparatorRegex = "(?<!\\\\),";
        final String[] dnSegments = inputDN.split( dnSeparatorRegex );
        if ( dnSegments.length < 2 )
        {
            return null;
        }
        final StringBuilder parentDN = new StringBuilder();
        for ( int i = 1; i < ( dnSegments.length ); i++ )
        {
            parentDN.append( dnSegments[i] );
            if ( i < ( dnSegments.length - 1 ) )
            {
                parentDN.append( "," );
            }
        }
        return parentDN.toString();
    }

    @Override
    public final boolean exists()
    {
        try
        {
            final Set<String> results = this.readMultiStringAttribute( ChaiConstant.ATTR_LDAP_OBJECTCLASS );
            if ( results != null && results.size() >= 1 )
            {
                return true;
            }
        }
        catch ( Exception e )
        {
            LOGGER.trace( () -> "error during exists check of '" + this.getEntryDN() + "', error: " + e.getMessage() );
        }

        return false;
    }

    @Override
    public final boolean readBooleanAttribute( final String attributeName )
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String value = this.readStringAttribute( attributeName );
        return value != null && "TRUE".equalsIgnoreCase( value );
    }

    @Override
    public String readCanonicalDN()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.returnNoAttributes();
        searchHelper.setSearchScope( SearchScope.BASE );
        searchHelper.setFilter( SearchHelper.DEFAULT_FILTER );

        final Map<String, Map<String, String>> results = this.getChaiProvider().search( this.getEntryDN(), searchHelper );
        if ( results.size() == 1 )
        {
            return results.keySet().iterator().next();
        }

        if ( results.isEmpty() )
        {
            throw new ChaiOperationException( "search for canonical DN resulted in no results", ChaiError.UNKNOWN );
        }

        throw new ChaiOperationException( "search for canonical DN resulted in multiple results", ChaiError.UNKNOWN );
    }

    @Override
    public final int readIntAttribute( final String attributeName )
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String value = this.readStringAttribute( attributeName );
        try
        {
            return Integer.parseInt( value );
        }
        catch ( NumberFormatException e )
        {
            return 0;
        }
    }

    @Override
    public final byte[][] readMultiByteAttribute( final String attributeName )
            throws ChaiOperationException, ChaiUnavailableException
    {
        return chaiProvider.readMultiByteAttribute( this.getEntryDN(), attributeName );
    }

    @Override
    public final Set<String> readMultiStringAttribute( final String attributeName )
            throws ChaiOperationException, ChaiUnavailableException
    {
        return chaiProvider.readMultiStringAttribute( entryDN, attributeName );
    }

    @Override
    public List<InetAddress> readNetAddressAttribute( final String attributeName )
            throws ChaiUnavailableException, ChaiOperationException
    {
        final byte[][] values = this.readMultiByteAttribute( attributeName );
        final List<InetAddress> returnValues = new ArrayList<>();
        final String characterEncoding = chaiProvider.getChaiConfiguration().getSetting( ChaiSetting.LDAP_CHARACTER_ENCODING );

        for ( final byte[] value : values )
        {
            final String strValue = new String( value, Charset.forName( characterEncoding ) );
            final int sepPos = strValue.indexOf( '#' );
            final int typeInt = Integer.parseInt( strValue.substring( 0, sepPos ) );
            final NetworkAddressType type = NetworkAddressType.forIdentifier( typeInt );
            switch ( type )
            {
                case IPv4:
                    final StringBuilder sb = new StringBuilder();
                    try
                    {
                        sb.append( 256 + value[sepPos + 3] % 256 ).append( "." );
                        sb.append( 256 + value[sepPos + 4] % 256 ).append( "." );
                        sb.append( 256 + value[sepPos + 5] % 256 ).append( "." );
                        sb.append( 256 + value[sepPos + 6] % 256 );
                        returnValues.add( InetAddress.getByName( sb.toString() ) );
                    }
                    catch ( UnknownHostException | ArrayIndexOutOfBoundsException e )
                    {
                        LOGGER.error( () -> "error while parsing network address '" + strValue + "' " + e.getMessage() );
                    }
                    break;

                default:
                    throw new IllegalStateException( "unable to parse non-ipv4 address" );
            }
        }
        return returnValues;
    }

    @Override
    public final Set<String> readObjectClass()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readMultiStringAttribute( ChaiConstant.ATTR_LDAP_OBJECTCLASS );
    }

    @Override
    public final String readStringAttribute( final String attributeName )
            throws ChaiOperationException, ChaiUnavailableException
    {
        // Using the LDAP Helper, get the attribute for the selected attribute name.
        return chaiProvider.readStringAttribute( entryDN, attributeName );
    }

    @Override
    public final Map<String, String> readStringAttributes( final Set<String> attributes )
            throws ChaiOperationException, ChaiUnavailableException
    {
        return chaiProvider.readStringAttributes( this.entryDN, attributes );
    }

    @Override
    public final void replaceAttribute( final String attributeName, final String oldValue, final String newValue )
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.replaceStringAttribute( this.entryDN, attributeName, oldValue, newValue );
    }

    @Override
    public final Set<ChaiEntry> search( final String filter )
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.search( new SearchHelper( filter ) );
    }

    @Override
    public Set<ChaiEntry> search( final SearchHelper searchHelper )
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<ChaiEntry> resultSet = new LinkedHashSet<>();
        final Map<String, Map<String, String>> results = chaiProvider.search(
                this.getEntryDN(),
                searchHelper.getFilter(),
                searchHelper.getAttributes(),
                searchHelper.getSearchScope()
        );

        for ( final String dn : results.keySet() )
        {
            resultSet.add( getChaiProvider().getEntryFactory().newChaiEntry( dn ) );
        }
        return resultSet;
    }

    @Override
    public final Set<ChaiEntry> search( final String filter, final SearchScope searchScope )
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.search( new SearchHelper( filter, searchScope ) );
    }

    @Override
    public final void writeStringAttribute( final String attributeName, final String attributeValue )
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.writeStringAttribute( this.entryDN, attributeName, attributeValue == null
                ? null
                : Collections.singleton( attributeValue ), true );
    }

    @Override
    public final void writeStringAttributes( final Map<String, String> attributeValueProps )
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.writeStringAttributes( this.entryDN, attributeValueProps, true );
    }

    @Override
    public final void writeStringAttribute( final String attributeName, final Set<String> attributeValues )
            throws ChaiOperationException, ChaiUnavailableException
    {
        // Using the LDAP Helper, set the attributes.
        chaiProvider.writeStringAttribute( this.entryDN, attributeName, attributeValues, true );
    }

    @Override
    public void writeStringAttribute( final String attributeName, final String... attributeValues )
            throws ChaiOperationException, ChaiUnavailableException
    {
        // Using the LDAP Helper, set the attributes.
        chaiProvider.writeStringAttribute( this.entryDN, attributeName, attributeValues == null
                ? null
                : new LinkedHashSet<>( Arrays.asList( attributeValues ) ), true );
    }

    @Override
    public void writeBinaryAttribute( final String attributeName, final byte[]... attributeValues )
            throws ChaiOperationException, ChaiUnavailableException
    {
        // Using the LDAP Helper, set the attributes.
        chaiProvider.writeBinaryAttribute( this.entryDN, attributeName, attributeValues, true );
    }

    @Override
    public void replaceBinaryAttribute( final String attributeName, final byte[] oldValue, final byte[] newValue )
            throws ChaiOperationException, ChaiUnavailableException
    {
        chaiProvider.replaceBinaryAttribute( this.entryDN, attributeName, oldValue, newValue );
    }

    @Override
    public Instant readDateAttribute( final String attributeName )
            throws ChaiUnavailableException, ChaiOperationException
    {
        final String value = this.readStringAttribute( attributeName );

        if ( value == null || value.isEmpty() )
        {
            return null;
        }

        return getChaiProvider().getDirectoryVendor().getVendorFactory().stringToInstant( value );
    }

    @Override
    public void writeDateAttribute( final String attributeName, final Instant instant )
            throws ChaiUnavailableException, ChaiOperationException
    {
        Objects.requireNonNull( instant );

        final String strValue = getChaiProvider().getDirectoryVendor().getVendorFactory().instantToString( instant );
        writeStringAttribute( attributeName, strValue );
    }

    @Override
    public String readGUID()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final byte[][] guidValues = this.readMultiByteAttribute( "guid" );
        if ( guidValues == null || guidValues.length < 1 )
        {
            return null;
        }
        final byte[] guidValue = guidValues[0];
        return StringHelper.base64Encode( guidValue );
    }

    @Override
    public boolean hasChildren()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter( SearchHelper.DEFAULT_FILTER );
        searchHelper.setMaxResults( 1 );
        searchHelper.setAttributes( Collections.emptyList() );
        searchHelper.setSearchScope( SearchScope.ONE );

        final Map<String, Map<String, String>> subSearchResults = chaiProvider.search( getEntryDN(), searchHelper );
        return !subSearchResults.isEmpty();
    }
}
