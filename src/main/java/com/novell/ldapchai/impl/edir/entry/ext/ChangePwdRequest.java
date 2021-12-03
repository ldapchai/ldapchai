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

public final class ChangePwdRequest implements ExtendedRequest
{
    private static final String OID = "2.16.840.1.113719.1.39.42.100.21";
    private String treeName;
    private String objectDN;
    private String oldPwdStr;
    private String newPwdStr;
    private byte[] encodedValue;

    public ChangePwdRequest()
    {
    }

    public ChangePwdRequest( final String var1, final String var2, final String var3, final String var4 )
    {
        this.treeName = var1;
        this.objectDN = var2;
        this.oldPwdStr = var3;
        this.newPwdStr = var4;
    }

    public ExtendedResponse createExtendedResponse( final String var1, final byte[] var2, final int var3, final int var4 )
            throws NamingException
    {
        return new ChangePwdResponse( var1, var2, var3, var4 );
    }

    public String getID()
    {
        return OID;
    }

    public byte[] getEncodedValue()
    {
        try
        {
            this.encodedValue = NMASLdapExtBerEncoder.encodeChangePwdData( this.treeName, this.objectDN, this.oldPwdStr, this.newPwdStr );
        }
        catch ( final IOException var2 )
        {
            throw new IllegalStateException( "Error BER Encoding ChangePwdRequest data: " + var2.toString() );
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

    public void setOldPwd( final String var1 )
    {
        this.oldPwdStr = var1;
    }

    public void setNewPwd( final String var1 )
    {
        this.newPwdStr = var1;
    }

    public String getTreeName()
    {
        return this.treeName;
    }

    public String getObjectDN()
    {
        return this.objectDN;
    }
}

