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


import com.novell.ldapchai.impl.edir.entry.ext.ber.BerDecoder;

import javax.naming.NamingException;
import java.io.IOException;

public class NMASLdapExtBerDecoder
{
    private final byte[] respBer;
    private final int respOffset;
    private final int respLength;
    private int nmasVersion;
    private int nmasRetCode;
    private int nmasRetDataLen = 0;
    private byte[] nmasRetData = null;
    private String pwdStr = null;
    private String pwdPolicyDNStr = null;
    private String x500DNStr = null;
    private NMASPwdStatus nmasPwdStatus = null;

    public NMASLdapExtBerDecoder( final byte[] var1, final int var2, final int var3 )
    {
        this.respBer = var1;
        this.respOffset = var2;
        this.respLength = var3;
    }

    public void decodePutLoginData()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            final int[] var2 = new int[1];
            var1.parseSeq( var2 );
            this.nmasVersion = var1.parseInt();
            this.nmasRetCode = var1.parseInt();
            this.nmasRetCode = this.fixRetCode( this.nmasRetCode );
        }
        catch ( final IOException var4 )
        {
            throw new NamingException( "Error BER decoding PutLoginResponse: " + var4.toString() );
        }
    }

    public void decodeDeleteLoginData()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            final int[] var2 = new int[1];
            var1.parseSeq( var2 );
            this.nmasVersion = var1.parseInt();
            this.nmasRetCode = var1.parseInt();
            this.nmasRetCode = this.fixRetCode( this.nmasRetCode );
        }
        catch ( final IOException var4 )
        {
            throw new NamingException( "Error BER decoding DeleteLoginResponse: " + var4.toString() );
        }
    }

    void decodeGetLoginData()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            final int[] var2 = new int[1];
            var1.parseSeq( var2 );
            this.nmasVersion = var1.parseInt();
            this.nmasRetCode = var1.parseInt();
            this.nmasRetCode = this.fixRetCode( this.nmasRetCode );
            if ( this.nmasRetCode == 0 )
            {
                this.nmasRetData = var1.parseOctetString( 4, var2 );
                this.nmasRetDataLen = this.nmasRetData.length;
            }

        }
        catch ( final IOException var4 )
        {
            throw new NamingException( "Error BER decoding GetLoginDataResponse: " + var4.toString() );
        }
    }

    public void decodeSetPwdData()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            final int[] var2 = new int[1];
            var1.parseSeq( var2 );
            this.nmasVersion = var1.parseInt();
            this.nmasRetCode = var1.parseInt();
            this.nmasRetCode = this.fixRetCode( this.nmasRetCode );
        }
        catch ( final IOException var4 )
        {
            throw new NamingException( "Error BER decoding Set Pwd Response: " + var4.toString() );
        }
    }

    public void decodeDeletePwdData()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            final int[] var2 = new int[1];
            var1.parseSeq( var2 );
            this.nmasVersion = var1.parseInt();
            this.nmasRetCode = var1.parseInt();
            this.nmasRetCode = this.fixRetCode( this.nmasRetCode );
        }
        catch ( final IOException var4 )
        {
            throw new NamingException( "Error BER decoding Delete Pwd Response: " + var4.toString() );
        }
    }

    void decodeGetPwdData()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            final int[] var2 = new int[1];
            var1.parseSeq( var2 );
            this.nmasVersion = var1.parseInt();
            this.nmasRetCode = var1.parseInt();
            this.nmasRetCode = this.fixRetCode( this.nmasRetCode );
            if ( this.nmasRetCode == 0 )
            {
                this.pwdStr = var1.parseString( true );
            }

        }
        catch ( final IOException var4 )
        {
            throw new NamingException( "Error BER decoding Get Pwd Response: " + var4.toString() );
        }
    }

    public void decodeChangePwdData()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            final int[] var2 = new int[1];
            var1.parseSeq( var2 );
            this.nmasVersion = var1.parseInt();
            this.nmasRetCode = var1.parseInt();
            this.nmasRetCode = this.fixRetCode( this.nmasRetCode );
        }
        catch ( final IOException var4 )
        {
            throw new NamingException( "Error BER decoding Change Pwd Response: " + var4.toString() );
        }
    }

    public void decodePwdPolicyCheckData()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            final int[] var2 = new int[1];
            var1.parseSeq( var2 );
            this.nmasVersion = var1.parseInt();
            this.nmasRetCode = var1.parseInt();
            this.nmasRetCode = this.fixRetCode( this.nmasRetCode );
        }
        catch ( final IOException var4 )
        {
            throw new NamingException( "Error BER decoding Pwd Policy Check Response: " + var4.toString() );
        }
    }

    public void decodePwdStatusCheckData()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            final int[] var2 = new int[1];
            var1.parseSeq( var2 );
            this.nmasVersion = var1.parseInt();
            this.nmasRetCode = var1.parseInt();
            this.nmasRetCode = this.fixRetCode( this.nmasRetCode );
            this.nmasPwdStatus = new NMASPwdStatus();
            this.nmasPwdStatus.setUniversalPwdStatus( var1.parseInt() );
            this.nmasPwdStatus.setSimplePwdStatus( var1.parseInt() );
        }
        catch ( final IOException var4 )
        {
            throw new NamingException( "Error BER decoding Get Password Status Response: " + var4.toString() );
        }
    }

    void decodeGetPwdPolicyInfoData()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            final int[] var2 = new int[1];
            var1.parseSeq( var2 );
            this.nmasVersion = var1.parseInt();
            this.nmasRetCode = var1.parseInt();
            this.nmasRetCode = this.fixRetCode( this.nmasRetCode );
            if ( this.nmasRetCode == 0 )
            {
                this.pwdPolicyDNStr = var1.parseString( true );
            }

        }
        catch ( final IOException var4 )
        {
            throw new NamingException( "Error BER decoding Get Pwd Policy Info Response: " + var4.toString() );
        }
    }

    void decodeDnsToX500DN()
            throws NamingException
    {
        final BerDecoder var1 = new BerDecoder( this.respBer, this.respOffset, this.respLength );

        try
        {
            this.x500DNStr = var1.parseString( true );
        }
        catch ( final IOException var3 )
        {
            throw new NamingException( "Error BER decoding X500 DN String: " + var3.toString() );
        }
    }

    public int getNmasVersion()
    {
        return this.nmasVersion;
    }

    public int getNmasRetCode()
    {
        return this.nmasRetCode;
    }

    public byte[] getNmasRetData()
    {
        return this.nmasRetData;
    }

    public String getPwdStr()
    {
        return this.pwdStr;
    }

    public String getPwdPolicyDNStr()
    {
        return this.pwdPolicyDNStr;
    }

    public String getX500DNStr()
    {
        return this.x500DNStr;
    }

    public NMASPwdStatus getNmasPwdStatus()
    {
        return this.nmasPwdStatus;
    }

    private int fixRetCode( final int var1 )
    {
        if ( var1 >= -16054 )
        {
            return var1;
        }
        else
        {
            final int var2 = var1 & 32767;
            return -1 * var2;
        }
    }
}

