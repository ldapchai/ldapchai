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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class SCryptUtil
{
    // dkLen
    private static final int SCRYPT_LENGTH = 64;
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public static boolean check( final String password, final String hashed )
    {
        try
        {
            final String[] parts = hashed.split( "\\$" );

            if ( parts.length != 5 || !parts[1].equals( "s0" ) )
            {
                throw new IllegalArgumentException( "Invalid hashed value" );
            }

            final long params = Long.parseLong( parts[2], 16 );
            final byte[] salt = StringHelper.base64Decode( parts[3] );
            final byte[] derived0 = StringHelper.base64Decode( parts[4] );

            final int valueN = ( int ) Math.pow( 2, params >> 16 & 0xffff );
            final int valueR = ( int ) params >> 8 & 0xff;
            final int valueP = ( int ) params & 0xff;

            final byte[] derived1 = org.bouncycastle.crypto.generators.SCrypt.generate( password.getBytes( CHARSET ), salt, valueN, valueR, valueP, SCRYPT_LENGTH );

            if ( derived0.length != derived1.length )
            {
                return false;
            }

            int result = 0;
            for ( int i = 0; i < derived0.length; i++ )
            {
                result |= derived0[i] ^ derived1[i];
            }
            return result == 0;
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new IllegalStateException( "JVM doesn't support UTF-8?" );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Issue decoding base64 hash: " + e.getMessage() );
        }
    }

    public static String scrypt( final String password )
    {
        return scrypt( password, 16, 16 );
    }

    public static String scrypt( final String password, final int saltLength, final int cost )
    {
        try
        {
            // N
            final int effectiveCost = nextPowerOf2( cost );

            // r
            final int blockSize = 16;

            // P
            final int parallelization = 16;

            final byte[] salt = new byte[saltLength];
            SecureRandom.getInstanceStrong().nextBytes( salt );

            final byte[] pwdBytes = password.getBytes( CHARSET );
            final byte[] derived = org.bouncycastle.crypto.generators.SCrypt.generate( pwdBytes, salt, effectiveCost, blockSize, parallelization, SCRYPT_LENGTH );

            final String params = Long.toString( log2( effectiveCost ) << 16L | blockSize << 8 | parallelization, 16 );

            return "$s0$" + params + '$'
                    + StringHelper.base64Encode(  salt ) + '$'
                    + StringHelper.base64Encode( derived );
        }
        catch ( GeneralSecurityException e )
        {
            throw new IllegalStateException( "JVM doesn't support SHA1PRNG or HMAC_SHA256?" );
        }
    }

    private static int log2( final int input )
    {
        int log = 0;
        int valueN = input;

        if ( ( valueN & 0xffff0000 ) != 0 )
        {
            valueN >>>= 16;
            log = 16;
        }

        if ( valueN >= 256 )
        {
            valueN >>>= 8;
            log += 8;
        }

        if ( valueN >= 16 )
        {
            valueN >>>= 4;
            log += 4;
        }

        if ( valueN >= 4 )
        {
            valueN >>>= 2;
            log += 2;
        }

        return log + ( valueN >>> 1 );
    }

    private static int nextPowerOf2( final int input )
    {
        int value = input;
        value--;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        value++;
        return Math.max( 16, value );
    }
}
