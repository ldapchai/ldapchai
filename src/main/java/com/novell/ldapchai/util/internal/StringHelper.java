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

package com.novell.ldapchai.util.internal;

import com.novell.ldapchai.exception.ChaiRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class StringHelper
{
    private StringHelper()
    {
    }

    /**
     * Convert a string value to a boolean.  If the value is a common positive string
     * value such as "1", "true", "y" or "yes" then TRUE is returned.  For any other
     * value or null, FALSE is returned.
     *
     * @param string value to test
     * @return true if the string resolves to a positive value.
     */
    public static boolean convertStrToBoolean( final String string )
    {
        return !StringHelper.isEmpty( string ) && ( "true".equalsIgnoreCase( string )
                || "1".equalsIgnoreCase( string )
                || "yes".equalsIgnoreCase( string )
                || "y".equalsIgnoreCase( string )
        );
    }

    /**
     * Convert a string to an int value.  If an error occurs during the conversion,
     * the default value is returned instead.  Unlike the {@link Integer#parseInt(String)}
     * method, this method will not throw an exception.
     *
     * @param string       value to test
     * @param defaultValue value to return in case of difficulting converting.
     * @return the int value contained in the string, otherwise the default value.
     */
    public static int convertStrToInt( final String string, final int defaultValue )
    {
        if ( string == null )
        {
            return defaultValue;
        }

        try
        {
            return Integer.parseInt( string );
        }
        catch ( Exception e )
        {
            return defaultValue;
        }
    }

    /**
     * Convert a string to a long value.  If an error occurs during the conversion,
     * the default value is returned instead.  Unlike the {@link Integer#parseInt(String)}
     * method, this method will not throw an exception.
     *
     * @param string       value to test
     * @param defaultValue value to return in case of difficulties converting.
     * @return the int value contained in the string, otherwise the default value.
     */
    public static long convertStrToLong( final String string, final long defaultValue )
    {
        if ( string == null )
        {
            return defaultValue;
        }

        try
        {
            return Long.parseLong( string );
        }
        catch ( Exception e )
        {
            return defaultValue;
        }
    }

    /**
     * Break apart a string using a tokenizer into a {@code List} of {@code String}s.
     *
     * @param inputString a string containing zero or more segments
     * @param separator   separator to use for the split, or null for the default
     * @return a {@code List} of {@code String}s.  An emtpy list is returned if <i>inputString</i> is null.
     */
    public static List<String> tokenizeString(
            final String inputString,
            final String separator
    )
    {
        if ( inputString == null || inputString.length() < 1 )
        {
            return Collections.emptyList();
        }

        final List<String> values = new ArrayList<>( Arrays.asList( inputString.split( separator ) ) );
        return Collections.unmodifiableList( values );
    }

    public static Map<String, String> tokenizeString(
            final String inputString,
            final String separator,
            final String subSeparator
    )
    {
        if ( inputString == null || inputString.length() < 1 )
        {
            return new HashMap<>();
        }

        final Map<String, String> returnProps = new LinkedHashMap<>();

        final List<String> values = tokenizeString( inputString, separator );
        for ( final String loopValue : values )
        {
            if ( loopValue != null && loopValue.length() > 0 )
            {
                final int subSeperatorPosition = loopValue.indexOf( subSeparator );
                if ( subSeperatorPosition != -1 )
                {
                    final String key = loopValue.substring( 0, subSeperatorPosition );
                    final String value = loopValue.substring( subSeperatorPosition + 1 );
                    returnProps.put( key, value );
                }
                else
                {
                    returnProps.put( loopValue, "" );
                }
            }
        }
        return returnProps;
    }

    public static String stringCollectionToString(
            final Collection<String> c,
            final String separator
    )
    {
        if ( c == null || c.isEmpty() )
        {
            return "";
        }

        final String effectiveSeparator = separator == null
                ? ", "
                : separator;


        final StringBuilder sb = new StringBuilder();
        for ( final String value : c )
        {
            sb.append( value );
            sb.append( effectiveSeparator );
        }
        sb.delete( sb.length() - effectiveSeparator.length(), sb.length() );
        return sb.toString();
    }

    public static String stringMapToString( final Map<String, String> map, final String separator )
    {
        if ( map == null )
        {
            return "";
        }

        final List<String> tempList = new ArrayList<>( map.size() );
        for ( final Map.Entry<String, String> entry : map.entrySet() )
        {
            tempList.add( entry.getKey() + "=" + entry.getValue() );
        }

        return stringCollectionToString( tempList, separator );
    }

    public static boolean isEmpty( final CharSequence value )
    {
        return value == null || value.length() == 0;
    }

    public static <E extends Enum<E>> boolean enumArrayContainsValue(
            final E[] enumArray,
            final E enumValue
    )
    {
        if ( enumArray == null || enumArray.length == 0 )
        {
            return false;
        }

        for ( final E loopValue : enumArray )
        {
            if ( loopValue == enumValue )
            {
                return true;
            }
        }

        return false;
    }

    public enum Base64Options
    {
        GZIP,
        URL_SAFE,;
    }

    public static byte[] base64Decode( final CharSequence input, final Base64Options... options )
            throws IOException
    {
        if ( isEmpty( input ) )
        {
            return new byte[0];
        }

        final byte[] decodedBytes;
        if ( enumArrayContainsValue( options, Base64Options.URL_SAFE ) )
        {
            decodedBytes = java.util.Base64.getUrlDecoder().decode( input.toString() );
        }
        else
        {
            decodedBytes = java.util.Base64.getMimeDecoder().decode( input.toString() );
        }

        if ( enumArrayContainsValue( options, Base64Options.GZIP ) )
        {
            return gunzip( decodedBytes );
        }
        else
        {
            return decodedBytes;
        }
    }

    public static String base64Encode( final byte[] input, final Base64Options... options )
    {
        final byte[] compressedBytes;
        if ( enumArrayContainsValue( options, Base64Options.GZIP ) )
        {
            try
            {
                compressedBytes = gzip( input );
            }
            catch ( final IOException e )
            {
                throw new ChaiRuntimeException( "unexpected error during base64 decoding: " + e, e );
            }
        }
        else
        {
            compressedBytes = input;
        }

        if ( enumArrayContainsValue( options, Base64Options.URL_SAFE ) )
        {
            return java.util.Base64.getUrlEncoder().encodeToString( compressedBytes );
        }
        else
        {
            return java.util.Base64.getMimeEncoder( 0, new byte[ 0 ] ).encodeToString( compressedBytes );
        }
    }

    public static byte[] gunzip( final byte[] bytes )
            throws IOException
    {
        try ( ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                GZIPInputStream inputGzipStream = new GZIPInputStream( new ByteArrayInputStream( bytes ) )
        )
        {
            final byte[] buffer = new byte[128];

            int len;
            while ( ( len = inputGzipStream.read( buffer ) ) > 0 )
            {
                byteArrayOutputStream.write( buffer, 0, len );
            }

            return byteArrayOutputStream.toByteArray();
        }
    }

    public static byte[] gzip( final byte[] bytes )
            throws IOException
    {
        try ( ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
              GZIPOutputStream gzipOutputStream = new GZIPOutputStream( byteArrayOutputStream ) )
        {
            gzipOutputStream.write( bytes );
            gzipOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static <K> boolean isEmpty( final Collection<K> collection )
    {
        return collection == null || collection.isEmpty();
    }
}
