package com.novell.ldapchai.util;

import net.iharder.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class SCryptUtil
{
    // dkLen
    static final int SCRYPT_LENGTH = 32;

    public static boolean check( final String passwd, final String hashed )
    {
        try
        {
            final String[] parts = hashed.split( "\\$" );

            if ( parts.length != 5 || !parts[1].equals( "s0" ) )
            {
                throw new IllegalArgumentException( "Invalid hashed value" );
            }

            final long params = Long.parseLong( parts[2], 16 );
            final byte[] salt = Base64.decode( parts[3] );
            final byte[] derived0 = Base64.decode( parts[4] );

            final int valueN = ( int ) Math.pow( 2, params >> 16 & 0xffff );
            final int valueR = ( int ) params >> 8 & 0xff;
            final int valueP = ( int ) params & 0xff;

            final byte[] derived1 = org.bouncycastle.crypto.generators.SCrypt.generate( passwd.getBytes( "UTF-8" ), salt, valueN, valueR, valueP, SCRYPT_LENGTH );

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


    public static String scrypt( final String passwd )
    {
        try
        {
            final int saltLength = 16;

            // N
            final int cost = 16;

            // r
            final int blockSize = 16;

            // P
            final int parallelization = 16;

            final byte[] salt = new byte[saltLength];
            SecureRandom.getInstance( "SHA1PRNG" ).nextBytes( salt );

            final byte[] pwdBytes = passwd.getBytes( "UTF-8" );
            final byte[] derived = org.bouncycastle.crypto.generators.SCrypt.generate( pwdBytes, salt, cost, blockSize, parallelization, SCRYPT_LENGTH );

            final String params = Long.toString( log2( cost ) << 16L | blockSize << 8 | parallelization, 16 );

            final StringBuilder sb = new StringBuilder( ( salt.length + derived.length ) * 2 );
            sb.append( "$s0$" ).append( params ).append( '$' );
            sb.append( Base64.encodeBytes( salt ) ).append( '$' );
            sb.append( Base64.encodeBytes( derived ) );
            return sb.toString();
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new IllegalStateException( "JVM doesn't support UTF-8?" );
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
}
