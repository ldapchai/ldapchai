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

package com.novell.ldapchai.impl.edir.entry.ext.ber;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

/**
 * This class encodes a buffer into the classic: "Hexadecimal Dump" format of
 * the past. It is useful for analyzing the contents of binary buffers.
 * The format produced is as follows:
 * <pre>
 * xxxx: 00 11 22 33 44 55 66 77   88 99 aa bb cc dd ee ff ................
 * </pre>
 * Where xxxx is the offset into the buffer in 16 byte chunks, followed
 * by ascii coded hexadecimal bytes followed by the ASCII representation of
 * the bytes or '.' if they are not valid bytes.
 *
 * @author Chuck McManis
 */
public class HexDumpEncoder
{
    private int offset;
    private int thisLineLength;
    private int currentByte;
    private final byte[] thisLine = new byte[16];

    /**
     * Stream that understands "printing".
     */
    protected PrintStream printStream;

    static void hexDigit( final PrintStream p, final byte x )
    {
        char theChar;

        theChar = ( char ) ( ( x >> 4 ) & 0xf );
        if ( theChar > 9 )
        {
            theChar = ( char ) ( ( theChar - 10 ) + 'A' );
        }
        else
        {
            theChar = ( char ) ( theChar + '0' );
        }
        p.write( theChar );
        theChar = ( char ) ( x & 0xf );
        if ( theChar > 9 )
        {
            theChar = ( char ) ( ( theChar - 10 ) + 'A' );
        }
        else
        {
            theChar = ( char ) ( theChar + '0' );
        }
        p.write( theChar );
    }

    protected int bytesPerAtom()
    {
        return ( 1 );
    }

    protected int bytesPerLine()
    {
        return ( 16 );
    }

    protected void encodeBufferPrefix( final OutputStream o )
            throws IOException
    {
        offset = 0;
        printStream = new PrintStream( o );
    }

    protected void encodeLinePrefix( final OutputStream o, final int len )
            throws IOException
    {
        hexDigit( printStream, ( byte ) ( ( offset >>> 8 ) & 0xff ) );
        hexDigit( printStream, ( byte ) ( offset & 0xff ) );
        printStream.print( ": " );
        currentByte = 0;
        thisLineLength = len;
    }

    protected void encodeAtom( final OutputStream o, final byte[] buf, final int off, final int len )
            throws IOException
    {
        thisLine[currentByte] = buf[off];
        hexDigit( printStream, buf[off] );
        printStream.print( " " );
        currentByte++;
        if ( currentByte == 8 )
        {
            printStream.print( "  " );
        }
    }

    protected void encodeLineSuffix( final OutputStream o )
            throws IOException
    {
        if ( thisLineLength < 16 )
        {
            for ( int i = thisLineLength; i < 16; i++ )
            {
                printStream.print( "   " );
                if ( i == 7 )
                {
                    printStream.print( "  " );
                }
            }
        }
        printStream.print( " " );
        for ( int i = 0; i < thisLineLength; i++ )
        {
            if ( ( thisLine[i] < ' ' ) || ( thisLine[i] > 'z' ) )
            {
                printStream.print( "." );
            }
            else
            {
                printStream.write( thisLine[i] );
            }
        }
        printStream.println();
        offset += thisLineLength;
    }



    /**
     * This method works around the bizarre semantics of BufferedInputStream's
     * read method.
     */
    protected int readFully( final InputStream in, final byte[] buffer )
            throws java.io.IOException
    {
        for ( int i = 0; i < buffer.length; i++ )
        {
            final int q = in.read();
            if ( q == -1 )
            {
                return i;
            }
            buffer[i] = ( byte ) q;
        }
        return buffer.length;
    }

    /**
     * Encode bytes from the input stream, and write them as text characters
     * to the output stream. This method will run until it exhausts the
     * input stream, but does not print the line suffix for a final
     * line that is shorter than bytesPerLine().
     */
    public void encode( final InputStream inStream, final OutputStream outStream )
            throws IOException
    {
        int loop;
        int numBytes;
        final byte[] tmpbuffer = new byte[bytesPerLine()];

        encodeBufferPrefix( outStream );

        while ( true )
        {
            numBytes = readFully( inStream, tmpbuffer );
            if ( numBytes == 0 )
            {
                break;
            }
            encodeLinePrefix( outStream, numBytes );
            for ( loop = 0; loop < numBytes; loop += bytesPerAtom() )
            {

                if ( ( loop + bytesPerAtom() ) <= numBytes )
                {
                    encodeAtom( outStream, tmpbuffer, loop, bytesPerAtom() );
                }
                else
                {
                    encodeAtom( outStream, tmpbuffer, loop, ( numBytes ) - loop );
                }
            }
            if ( numBytes < bytesPerLine() )
            {
                break;
            }
            else
            {
                encodeLineSuffix( outStream );
            }
        }
    }

    /**
     * A 'streamless' version of encode that simply takes a buffer of
     * bytes and returns a string containing the encoded buffer.
     */
    public String encode( final byte[] aBuffer )
    {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final ByteArrayInputStream inStream = new ByteArrayInputStream( aBuffer );
        String retVal = null;
        try
        {
            encode( inStream, outStream );
            // explicit ascii->unicode conversion
            retVal = outStream.toString( "ISO-8859-1" );
        }
        catch ( final Exception ioException )
        {
            // This should never happen.
            throw new Error( "CharacterEncoder.encode internal error" );
        }
        return ( retVal );
    }

    /**
     * <p>Return a byte array from the remaining bytes in this ByteBuffer.</p>
     *
     * <p>The ByteBuffer's position will be advanced to ByteBuffer's limit.</p>
     *
     * <p>To avoid an extra copy, the implementation will attempt to return the
     * byte array backing the ByteBuffer.  If this is not possible, a
     * new byte array will be created.</p>
     */
    private byte[] getBytes( final ByteBuffer bb )
    {
        /*
         * This should never return a BufferOverflowException, as we're
         * careful to allocate just the right amount.
         */
        byte[] buf = null;

        /*
         * If it has a usable backing byte buffer, use it.  Use only
         * if the array exactly represents the current ByteBuffer.
         */
        if ( bb.hasArray() )
        {
            final byte[] tmp = bb.array();
            if ( ( tmp.length == bb.capacity() )
                    && ( tmp.length == bb.remaining() ) )
            {
                buf = tmp;
                bb.position( bb.limit() );
            }
        }

        if ( buf == null )
        {
            /*
             * This class doesn't have a concept of encode(buf, len, off),
             * so if we have a partial buffer, we must reallocate
             * space.
             */
            buf = new byte[bb.remaining()];

            /*
             * position() automatically updated
             */
            bb.get( buf );
        }

        return buf;
    }

    /**
     * <p>A 'streamless' version of encode that simply takes a ByteBuffer
     * and returns a string containing the encoded buffer.</p>
     *
     * <p>The ByteBuffer's position will be advanced to ByteBuffer's limit.</p>
     */
    public String encode( final ByteBuffer aBuffer )
    {
        final byte[] buf = getBytes( aBuffer );
        return encode( buf );
    }

    /**
     * Encode bytes from the input stream, and write them as text characters
     * to the output stream. This method will run until it exhausts the
     * input stream. It differs from encode in that it will add the
     * line at the end of a final line that is shorter than bytesPerLine().
     */
    public void encodeBuffer( final InputStream inStream, final OutputStream outStream )
            throws IOException
    {
        int numBytes;
        final byte[] tmpbuffer = new byte[bytesPerLine()];

        encodeBufferPrefix( outStream );

        while ( true )
        {
            numBytes = readFully( inStream, tmpbuffer );
            if ( numBytes == 0 )
            {
                break;
            }
            encodeLinePrefix( outStream, numBytes );
            for ( int loop = 0; loop < numBytes; loop += bytesPerAtom() )
            {
                if ( ( loop + bytesPerAtom() ) <= numBytes )
                {
                    encodeAtom( outStream, tmpbuffer, loop, bytesPerAtom() );
                }
                else
                {
                    encodeAtom( outStream, tmpbuffer, loop, ( numBytes ) - loop );
                }
            }
            encodeLineSuffix( outStream );
            if ( numBytes < bytesPerLine() )
            {
                break;
            }
        }
    }

    /**
     * Encode the buffer in <i>aBuffer</i> and write the encoded
     * result to the OutputStream <i>aStream</i>.
     */
    public void encodeBuffer( final byte[] aBuffer, final OutputStream aStream )
            throws IOException
    {
        final ByteArrayInputStream inStream = new ByteArrayInputStream( aBuffer );
        encodeBuffer( inStream, aStream );
    }

    /**
     * A 'streamless' version of encode that simply takes a buffer of
     * bytes and returns a string containing the encoded buffer.
     */
    public String encodeBuffer( final byte[] aBuffer )
    {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final ByteArrayInputStream inStream = new ByteArrayInputStream( aBuffer );
        try
        {
            encodeBuffer( inStream, outStream );
        }
        catch ( final Exception ioException )
        {
            // This should never happen.
            throw new Error( "CharacterEncoder.encodeBuffer internal error" );
        }
        return ( outStream.toString() );
    }

    /**
     * <p>Encode the <i>aBuffer</i> ByteBuffer and write the encoded
     * result to the OutputStream <i>aStream</i>.</p>
     *
     * <p>The ByteBuffer's position will be advanced to ByteBuffer's limit.</p>
     */
    public void encodeBuffer( final ByteBuffer aBuffer, final OutputStream aStream )
            throws IOException
    {
        final byte[] buf = getBytes( aBuffer );
        encodeBuffer( buf, aStream );
    }

}

