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

import javax.naming.NamingException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import java.io.IOException;

public final class SetPwdRequest implements ExtendedRequest
{
    private static final String OID = "2.16.840.1.113719.1.39.42.100.11";

    private String treeName;
    private String objectDN;
    private String pwdStr;
    private byte[] encodedValue;

    public SetPwdRequest()
    {
    }

    public SetPwdRequest( final String var1, final String var2, final String var3 )
    {
        this.treeName = var1;
        this.objectDN = var2;
        this.pwdStr = var3;
    }

    public ExtendedResponse createExtendedResponse( final String var1, final byte[] var2, final int var3, final int var4 )
            throws NamingException
    {
        return new SetPwdResponse( var1, var2, var3, var4 );
    }

    public String getID()
    {
        return this.OID;
    }

    public byte[] getEncodedValue()
    {
        try
        {
            this.encodedValue = encodeSetPwdData( this.treeName, this.objectDN, this.pwdStr );
        }
        catch ( final IOException var2 )
        {
            throw new IllegalStateException( "Error BER Encoding SetPwdRequest data: " + var2.toString() );
        }

        return this.encodedValue;
    }

    public void setTreeName( final String var1 )
    {
        this.treeName = var1;
    }

    public void setObjectDN( final String var1 )
    {
        this.objectDN = var1;
    }

    public void setData( final String var1 )
    {
        this.pwdStr = var1;
    }

    public String getTreeName()
    {
        return this.treeName;
    }

    public String getObjectDN()
    {
        return this.objectDN;
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


}
