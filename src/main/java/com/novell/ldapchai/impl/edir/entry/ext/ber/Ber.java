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
import java.io.IOException;
import java.io.OutputStream;

/**
 * Base class that defines common fields, constants, and debug method.
 *
 * @author Jagane Sundar
 */
public abstract class Ber
{

    protected byte[] buffer;
    protected int offset;
    protected int bufsize;

    protected Ber()
    {
    }

    public static void dumpBER( final OutputStream outStream, final String tag, final byte[] bytes,
                                final int from, final int to )
    {

        try
        {
            outStream.write( '\n' );
            outStream.write( tag.getBytes( "UTF8" ) );

            new HexDumpEncoder().encodeBuffer(
                    new ByteArrayInputStream( bytes, from, to ),
                    outStream );

            outStream.write( '\n' );
        }
        catch ( final IOException e )
        {
            try
            {
                outStream.write(
                        "Ber.dumpBER(): error encountered\n".getBytes( "UTF8" ) );
            }
            catch ( final IOException e2 )
            {
                // ignore
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // some ASN defines
    //
    ////////////////////////////////////////////////////////////////////////////

    public static final int ASN_BOOLEAN = 0x01;
    public static final int ASN_INTEGER = 0x02;
    public static final int ASN_BIT_STRING = 0x03;
    public static final int ASN_SIMPLE_STRING = 0x04;
    public static final int ASN_OCTET_STR = 0x04;
    public static final int ASN_NULL = 0x05;
    public static final int ASN_OBJECT_ID = 0x06;
    public static final int ASN_SEQUENCE = 0x10;
    public static final int ASN_SET = 0x11;


    public static final int ASN_PRIMITIVE = 0x00;
    public static final int ASN_UNIVERSAL = 0x00;
    public static final int ASN_CONSTRUCTOR = 0x20;
    public static final int ASN_APPLICATION = 0x40;
    public static final int ASN_CONTEXT = 0x80;
    public static final int ASN_PRIVATE = 0xC0;

    public static final int ASN_ENUMERATED = 0x0a;

    static class EncodeException extends IOException
    {
        private static final long serialVersionUID = -5247359637775781768L;

        EncodeException( final String msg )
        {
            super( msg );
        }
    }

    static class DecodeException extends IOException
    {
        private static final long serialVersionUID = 8735036969244425583L;

        DecodeException( final String msg )
        {
            super( msg );
        }
    }
}
