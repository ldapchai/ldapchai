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


import java.io.UnsupportedEncodingException;

/**
 * A BER encoder.
 *
 * @author Jagane Sundar
 * @author Scott Seligman
 * @author Vincent Ryan
 */
public final class BerEncoder extends Ber
{
    private int curSeqIndex;
    private int[] seqOffset;
    private static final int INITIAL_SEQUENCES = 16;
    private static final int DEFAULT_BUFSIZE = 1024;

    // When buf is full, expand its size by the following factor.
    private static final int BUF_GROWTH_FACTOR = 8;

    /**
     * Creates a BER buffer for encoding.
     */
    public BerEncoder()
    {
        this( DEFAULT_BUFSIZE );
    }

    /**
     * Creates a BER buffer of a specified size for encoding.
     * Specify the initial bufsize.  Buffer will be expanded as needed.
     *
     * @param bufsize The number of bytes for the buffer.
     */
    public BerEncoder( final int bufsize )
    {
        buffer = new byte[bufsize];
        this.bufsize = bufsize;
        offset = 0;

        seqOffset = new int[INITIAL_SEQUENCES];
        curSeqIndex = 0;
    }

    /**
     * Resets encoder to state when newly constructed.  Zeros out
     * internal data structures.
     */
    public void reset()
    {
        while ( offset > 0 )
        {
            buffer[--offset] = 0;
        }
        while ( curSeqIndex > 0 )
        {
            seqOffset[--curSeqIndex] = 0;
        }
    }

// ------------------ Accessor methods ------------

    /**
     * Gets the number of encoded bytes in this BER buffer.
     */
    public int getDataLen()
    {
        return offset;
    }

    /**
     * Gets the buffer that contains the BER encoding. Throws an
     * exception if unmatched beginSeq() and endSeq() pairs were
     * encountered. Not entire buffer contains encoded bytes.
     * Use getDataLen() to determine number of encoded bytes.
     * Use getBuffer(true) to get rid of excess bytes in array.
     *
     * @throws IllegalStateException If buffer contains unbalanced sequence.
     */
    public byte[] getBuf()
    {
        if ( curSeqIndex != 0 )
        {
            throw new IllegalStateException( "BER encode error: Unbalanced SEQUENCEs." );
        }

        // shared buffer, be careful to use this method.
        return buffer;
    }

    /**
     * Gets the buffer that contains the BER encoding, trimming unused bytes.
     *
     * @throws IllegalStateException If buffer contains unbalanced sequence.
     */
    public byte[] getTrimmedBuf()
    {
        final int len = getDataLen();
        final byte[] trimBuf = new byte[len];

        System.arraycopy( getBuf(), 0, trimBuf, 0, len );
        return trimBuf;
    }

// -------------- encoding methods -------------

    /**
     * Begin encoding a sequence with a tag.
     */
    public void beginSeq( final int tag )
    {

        // Double the size of the SEQUENCE array if it overflows
        if ( curSeqIndex >= seqOffset.length )
        {
            final int[] seqOffsetTmp = new int[seqOffset.length * 2];

            for ( int i = 0; i < seqOffset.length; i++ )
            {
                seqOffsetTmp[i] = seqOffset[i];
            }
            seqOffset = seqOffsetTmp;
        }

        encodeByte( tag );
        seqOffset[curSeqIndex] = offset;

        // Save space for sequence length.
        // %%% Currently we save enough space for sequences up to 64k.
        //     For larger sequences we'll need to shift the data to the right
        //     in endSeq().  If we could instead pad the length field with
        //     zeros, it would be a big win.
        ensureFreeBytes( 3 );
        offset += 3;

        curSeqIndex++;
    }

    /**
     * Terminate a BER sequence.
     */
    public void endSeq()
            throws EncodeException
    {
        curSeqIndex--;
        if ( curSeqIndex < 0 )
        {
            throw new IllegalStateException( "BER encode error: Unbalanced SEQUENCEs." );
        }

        // index beyond length field
        final int start = seqOffset[curSeqIndex] + 3;
        final int len = offset - start;

        if ( len <= 0x7f )
        {
            shiftSeqData( start, len, -2 );
            buffer[seqOffset[curSeqIndex]] = ( byte ) len;
        }
        else if ( len <= 0xff )
        {
            shiftSeqData( start, len, -1 );
            buffer[seqOffset[curSeqIndex]] = ( byte ) 0x81;
            buffer[seqOffset[curSeqIndex] + 1] = ( byte ) len;
        }
        else if ( len <= 0xffff )
        {
            buffer[seqOffset[curSeqIndex]] = ( byte ) 0x82;
            buffer[seqOffset[curSeqIndex] + 1] = ( byte ) ( len >> 8 );
            buffer[seqOffset[curSeqIndex] + 2] = ( byte ) len;
        }
        else if ( len <= 0xffffff )
        {
            shiftSeqData( start, len, 1 );
            buffer[seqOffset[curSeqIndex]] = ( byte ) 0x83;
            buffer[seqOffset[curSeqIndex] + 1] = ( byte ) ( len >> 16 );
            buffer[seqOffset[curSeqIndex] + 2] = ( byte ) ( len >> 8 );
            buffer[seqOffset[curSeqIndex] + 3] = ( byte ) len;
        }
        else
        {
            throw new EncodeException( "SEQUENCE too long" );
        }
    }

    /**
     * Shifts contents of buf in the range [start,start+len) a specified amount.
     * Positive shift value means shift to the right.
     */
    private void shiftSeqData( final int start, final int len, final int shift )
    {
        if ( shift > 0 )
        {
            ensureFreeBytes( shift );
        }
        System.arraycopy( buffer, start, buffer, start + shift, len );
        offset += shift;
    }

    /**
     * Encode a single byte.
     */
    public void encodeByte( final int b )
    {
        ensureFreeBytes( 1 );
        buffer[offset++] = ( byte ) b;
    }

/*
    private void deleteByte() {
        offset--;
    }
*/


    /*
     * Encodes an int.
     *<blockquote><pre>
     * BER integer ::= 0x02 berlength byte {byte}*
     *</pre></blockquote>
     */
    public void encodeInt( final int i )
    {
        encodeInt( i, 0x02 );
    }

    /**
     * Encodes an int and a tag.
     * <blockquote><pre>
     * BER integer w tag ::= tag berlength byte {byte}*
     * </pre></blockquote>
     */
    public void encodeInt( final int i, final int tag )
    {
        final int mask = 0xff800000;
        int intsize = 4;
        int returnI = i;

        while ( ( ( ( i & mask ) == 0 ) || ( ( i & mask ) == mask ) ) && ( intsize > 1 ) )
        {
            intsize--;
            returnI <<= 8;
        }

        encodeInt( returnI, tag, intsize );
    }

    //
    // encodes an int using numbytes for the actual encoding.
    //
    private void encodeInt( final int i, final int tag, final int intsize )
    {

        //
        // integer ::= 0x02 asnlength byte {byte}*
        //
        int returnI = i;

        if ( intsize > 4 )
        {
            throw new IllegalArgumentException( "BER encode error: INTEGER too long." );
        }

        ensureFreeBytes( 2 + intsize );

        buffer[offset++] = ( byte ) tag;
        buffer[offset++] = ( byte ) intsize;

        final int mask = 0xff000000;

        int loopIntsize = intsize;
        while ( loopIntsize-- > 0 )
        {
            buffer[offset++] = ( byte ) ( ( returnI & mask ) >> 24 );
            returnI <<= 8;
        }
    }

    /**
     * Encodes a boolean.
     * <blockquote><pre>
     * BER boolean ::= 0x01 0x01 {0xff|0x00}
     * </pre></blockquote>
     */
    public void encodeBoolean( final boolean b )
    {
        encodeBoolean( b, ASN_BOOLEAN );
    }


    /**
     * Encodes a boolean and a tag.
     * <blockquote><pre>
     * BER boolean w TAG ::= tag 0x01 {0xff|0x00}
     * </pre></blockquote>
     */
    public void encodeBoolean( final boolean b, final int tag )
    {
        ensureFreeBytes( 3 );

        buffer[offset++] = ( byte ) tag;
        buffer[offset++] = 0x01;
        buffer[offset++] = b ? ( byte ) 0xff : ( byte ) 0x00;
    }

    /**
     * Encodes a string.
     * <blockquote><pre>
     * BER string ::= 0x04 strlen byte1 byte2...
     * </pre></blockquote>
     * The string is converted into bytes using UTF-8 or ISO-Latin-1.
     */
    public void encodeString( final String str, final boolean encodeUTF8 )
            throws EncodeException
    {
        encodeString( str, ASN_OCTET_STR, encodeUTF8 );
    }

    /**
     * Encodes a string and a tag.
     * <blockquote><pre>
     * BER string w TAG ::= tag strlen byte1 byte2...
     * </pre></blockquote>
     */
    public void encodeString( final String str, final int tag, final boolean encodeUTF8 )
            throws EncodeException
    {

        encodeByte( tag );

        final int count;
        byte[] bytes = null;

        if ( str == null )
        {
            count = 0;
        }
        else if ( encodeUTF8 )
        {
            try
            {
                bytes = str.getBytes( "UTF8" );
                count = bytes.length;
            }
            catch ( final UnsupportedEncodingException e )
            {
                throw new EncodeException( "UTF8 not available on platform" );
            }
        }
        else
        {
            try
            {
                bytes = str.getBytes( "8859_1" );
                count = bytes.length;
            }
            catch ( final UnsupportedEncodingException e )
            {
                throw new EncodeException( "8859_1 not available on platform" );
            }
        }

        encodeLength( count );

        ensureFreeBytes( count );

        int loop = 0;
        while ( loop < count )
        {
            buffer[offset++] = bytes[loop++];
        }
    }

    /**
     * Encodes a portion of an octet string and a tag.
     */
    public void encodeOctetString( final byte[] tb, final int tag, final int tboffset, final int length )
            throws EncodeException
    {

        encodeByte( tag );
        encodeLength( length );

        if ( length > 0 )
        {
            ensureFreeBytes( length );
            System.arraycopy( tb, tboffset, buffer, offset, length );
            offset += length;
        }
    }

    /**
     * Encodes an octet string and a tag.
     */
    public void encodeOctetString( final byte[] tb, final int tag )
            throws EncodeException
    {
        encodeOctetString( tb, tag, 0, tb.length );
    }

    private void encodeLength( final int len )
            throws EncodeException
    {
        // worst case
        ensureFreeBytes( 4 );

        if ( len < 128 )
        {
            buffer[offset++] = ( byte ) len;
        }
        else if ( len <= 0xff )
        {
            buffer[offset++] = ( byte ) 0x81;
            buffer[offset++] = ( byte ) len;
        }
        else if ( len <= 0xffff )
        {
            buffer[offset++] = ( byte ) 0x82;
            buffer[offset++] = ( byte ) ( len >> 8 );
            buffer[offset++] = ( byte ) ( len & 0xff );
        }
        else if ( len <= 0xffffff )
        {
            buffer[offset++] = ( byte ) 0x83;
            buffer[offset++] = ( byte ) ( len >> 16 );
            buffer[offset++] = ( byte ) ( len >> 8 );
            buffer[offset++] = ( byte ) ( len & 0xff );
        }
        else
        {
            throw new EncodeException( "string too long" );
        }
    }

    /**
     * Encodes an array of strings.
     */
    public void encodeStringArray( final String[] strs, final boolean encodeUTF8 )
            throws EncodeException
    {
        if ( strs == null )
        {
            return;
        }
        for ( int i = 0; i < strs.length; i++ )
        {
            encodeString( strs[i], encodeUTF8 );
        }
    }
/*
    private void encodeNull() {

        //
        // NULL ::= 0x05 0x00
        //
        encodeByte(0x05);
        encodeByte(0x00);
    }
*/


    /**
     * Ensures that there are at least "len" unused bytes in "buf".
     * When more space is needed "buf" is expanded by a factor of
     * BUF_GROWTH_FACTOR, then "len" bytes are added if "buf" still
     * isn't large enough.
     */
    private void ensureFreeBytes( final int len )
    {
        if ( bufsize - offset < len )
        {
            int newsize = bufsize * BUF_GROWTH_FACTOR;
            if ( newsize - offset < len )
            {
                newsize += len;
            }
            final byte[] newbuf = new byte[newsize];
            // Only copy bytes in the range [0, offset)
            System.arraycopy( buffer, 0, newbuf, 0, offset );
        }
    }
}

