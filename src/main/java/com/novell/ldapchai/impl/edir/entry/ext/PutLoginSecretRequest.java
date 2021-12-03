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

import javax.naming.NamingException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import java.io.IOException;

public final class PutLoginSecretRequest implements ExtendedRequest
{
    private static final String OID = "2.16.840.1.113719.1.39.42.100.7";
    private String treeName;
    private String objectDN;
    private int methodIDLen;
    private int[] methodID;
    private String tag;
    private int dataLen;
    private byte[] data;
    private byte[] encodedValue;

    public PutLoginSecretRequest()
    {
    }

    public PutLoginSecretRequest( final String var1, final String var2, final int var3, final int[] var4, final String var5, final int var6, final byte[] var7 )
    {
        this.treeName = var1;
        this.objectDN = var2;
        this.methodIDLen = var3;
        this.methodID = var4;
        this.tag = var5;
        this.dataLen = var6;
        this.data = var7;
    }

    public ExtendedResponse createExtendedResponse( final String var1, final byte[] var2, final int var3, final int var4 )
            throws NamingException
    {
        return new PutLoginSecretResponse( var1, var2, var3, var4 );
    }

    public String getID()
    {
        return this.OID;
    }

    public byte[] getEncodedValue()
    {
        try
        {
            this.encodedValue = NMASLdapExtBerEncoder.encodePutLoginData( this.treeName, this.objectDN, this.methodIDLen, this.methodID, this.tag, this.dataLen, this.data );
        }
        catch ( final IOException var2 )
        {
            throw new IllegalStateException( "Error BER Encoding PutLoginSecretRequest data: " + var2.toString() );
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

    public void setMethodIDLen( final int var1 )
    {
        this.methodIDLen = var1;
    }

    public void setMethodID( final int[] var1 )
    {
        this.methodID = var1;
    }

    public void setTag( final String var1 )
    {
        this.tag = var1;
    }

    public void setDataLen( final int var1 )
    {
        this.dataLen = var1;
    }

    public void setData( final byte[] var1 )
    {
        this.data = var1;
    }

    public String getTreeName()
    {
        return this.treeName;
    }

    public String getObjectDN()
    {
        return this.objectDN;
    }

    public int getMethodIDLen()
    {
        return this.methodIDLen;
    }

    public int[] getMethodID()
    {
        return this.methodID;
    }

    public String getTag()
    {
        return this.tag;
    }

    public int getDataLen()
    {
        return this.dataLen;
    }

    public byte[] getData()
    {
        return this.data;
    }
}
