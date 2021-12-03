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

package com.novell.ldapchai.impl.edir.entry.ext;


import com.novell.ldapchai.impl.edir.entry.ext.ber.BerEncoder;

import java.io.IOException;

public class NMASLdapExtBerEncoder
{
    public static final int CHECK_CURRENT_PASSWORD = 1;
    public static final int CHECK_PASSWORD_STATUS = 2;

    public NMASLdapExtBerEncoder()
    {
    }

    public static byte[] encodePutLoginData( final String var0, final String var1, final int var2, final int[] var3, final String var4, final int var5, final byte[] var6 )
            throws IOException
    {
        final BerEncoder var7 = new BerEncoder();

        try
        {
            var7.beginSeq( 16 );
            var7.encodeInt( 1 );
            var7.encodeString( var1, true );
            var7.beginSeq( 16 );
            var7.encodeInt( var2 );
            var7.beginSeq( 16 );

            for ( int var8 = 0; var8 < var2 / 4; ++var8 )
            {
                var7.encodeInt( var3[var8] );
            }

            var7.endSeq();
            var7.endSeq();
            var7.encodeString( var4, true );
            var7.encodeInt( var5 );
            var7.encodeOctetString( var6, 4 );
            var7.endSeq();
            return var7.getTrimmedBuf();
        }
        catch ( final IOException var9 )
        {
            throw new IOException( "BerEncoder error: " + var9.toString() );
        }
    }

    public static byte[] encodeGetLoginData( final String var0, final String var1, final int var2, final int[] var3, final String var4 )
            throws IOException
    {
        final BerEncoder var5 = new BerEncoder();

        try
        {
            var5.beginSeq( 16 );
            var5.encodeInt( 1 );
            var5.encodeString( var1, true );
            var5.beginSeq( 16 );
            var5.encodeInt( var2 );
            var5.beginSeq( 16 );

            for ( int var6 = 0; var6 < var2 / 4; ++var6 )
            {
                var5.encodeInt( var3[var6] );
            }

            var5.endSeq();
            var5.endSeq();
            var5.encodeString( var4, true );
            var5.endSeq();
            return var5.getTrimmedBuf();
        }
        catch ( final IOException var7 )
        {
            throw new IOException( "BerEncoder error: " + var7.toString() );
        }
    }

    public static byte[] encodeDeleteLoginData( final String var0, final String var1, final int var2, final int[] var3, final String var4 )
            throws IOException
    {
        final BerEncoder var5 = new BerEncoder();

        try
        {
            var5.beginSeq( 16 );
            var5.encodeInt( 1 );
            var5.encodeString( var1, true );
            var5.beginSeq( 16 );
            var5.encodeInt( var2 );
            var5.beginSeq( 16 );

            for ( int var6 = 0; var6 < var2 / 4; ++var6 )
            {
                var5.encodeInt( var3[var6] );
            }

            var5.endSeq();
            var5.endSeq();
            var5.encodeString( var4, true );
            var5.endSeq();
            return var5.getTrimmedBuf();
        }
        catch ( final IOException var7 )
        {
            throw new IOException( "BerEncoder error: " + var7.toString() );
        }
    }

    public static byte[] encodeSetPwdData( final String var0, final String var1, final String var2 )
            throws IOException
    {
        final BerEncoder var3 = new BerEncoder();

        try
        {
            var3.beginSeq( 16 );
            var3.encodeInt( 1 );
            var3.encodeString( var1, true );
            var3.encodeString( var2, true );
            var3.endSeq();
        }
        catch ( final IOException var5 )
        {
            throw new IOException( "BerEncoder error: " + var5.toString() );
        }

        return var3.getTrimmedBuf();
    }

    public static byte[] encodeGetPwdData( final String var0, final String var1 )
            throws IOException
    {
        final BerEncoder var2 = new BerEncoder();

        try
        {
            var2.beginSeq( 16 );
            var2.encodeInt( 1 );
            var2.encodeString( var1, true );
            var2.endSeq();
        }
        catch ( final IOException var4 )
        {
            throw new IOException( "BerEncoder error: " + var4.toString() );
        }

        return var2.getTrimmedBuf();
    }

    public static byte[] encodeDeletePwdData( final String var0, final String var1 )
            throws IOException
    {
        final BerEncoder var2 = new BerEncoder();

        try
        {
            var2.beginSeq( 16 );
            var2.encodeInt( 1 );
            var2.encodeString( var1, true );
            var2.endSeq();
        }
        catch ( final IOException var4 )
        {
            throw new IOException( "BerEncoder error: " + var4.toString() );
        }

        return var2.getTrimmedBuf();
    }

    public static byte[] encodeChangePwdData( final String var0, final String var1, final String var2, final String var3 )
            throws IOException
    {
        final BerEncoder var4 = new BerEncoder();

        try
        {
            var4.beginSeq( 16 );
            var4.encodeInt( 1 );
            var4.encodeString( var1, true );
            var4.encodeString( var2, true );
            var4.encodeString( var3, true );
            var4.endSeq();
        }
        catch ( final IOException var6 )
        {
            throw new IOException( "BerEncoder error: " + var6.toString() );
        }

        return var4.getTrimmedBuf();
    }

    public static byte[] encodePwdPolicyCheckData( final String var0, final String var1, final String var2 )
            throws IOException
    {
        final BerEncoder var3 = new BerEncoder();

        try
        {
            var3.beginSeq( 16 );
            var3.encodeInt( 1 );
            var3.encodeString( var1, true );
            var3.beginSeq( 16 );
            if ( var2 != null )
            {
                var3.encodeInt( 0 );
                var3.beginSeq( 16 );
                var3.encodeString( var2, true );
                var3.endSeq();
            }
            else
            {
                var3.encodeInt( 1 );
            }

            var3.endSeq();
            var3.endSeq();
        }
        catch ( final IOException var5 )
        {
            throw new IOException( "BerEncoder error: " + var5.toString() );
        }

        return var3.getTrimmedBuf();
    }

    public static byte[] encodeGetPwdStatusData( final String var0, final String var1 )
            throws IOException
    {
        final BerEncoder var2 = new BerEncoder();

        try
        {
            var2.beginSeq( 16 );
            var2.encodeInt( 1 );
            var2.encodeString( var1, true );
            var2.beginSeq( 16 );
            var2.encodeInt( 3 );
            var2.endSeq();
            var2.endSeq();
        }
        catch ( final IOException var4 )
        {
            throw new IOException( "BerEncoder error: " + var4.toString() );
        }

        return var2.getTrimmedBuf();
    }

    public static byte[] encodeGetPwdPolicyInfoData( final String var0, final String var1 )
            throws IOException
    {
        final BerEncoder var2 = new BerEncoder();

        try
        {
            var2.beginSeq( 16 );
            var2.encodeInt( 1 );
            var2.encodeString( var1, true );
            var2.endSeq();
        }
        catch ( final IOException var4 )
        {
            throw new IOException( "BerEncoder error: " + var4.toString() );
        }

        return var2.getTrimmedBuf();
    }

    public static byte[] encodeDnsToX500DN( final String var0 )
            throws IOException
    {
        final BerEncoder var1 = new BerEncoder();

        try
        {
            var1.encodeString( var0, true );
        }
        catch ( final IOException var3 )
        {
            throw new IOException( "BerEncoder error: " + var3.toString() );
        }

        return var1.getTrimmedBuf();
    }
}

