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
import javax.naming.ldap.ExtendedResponse;

public final class GetLoginConfigResponse implements ExtendedResponse
{
    private static final String OID = "2.16.840.1.113719.1.39.42.100.4";
    private final byte[] responseBer;
    private BerDecoder ber;
    private final int nmasVersion;
    private final int nmasRetCode;
    private int nmasRetDataLen = 0;
    private byte[] nmasRetData = null;

    GetLoginConfigResponse( final String var1, final byte[] var2, final int var3, final int var4 )
            throws NamingException
    {
        this.responseBer = var2;
        final NMASLdapExtBerDecoder var5 = new NMASLdapExtBerDecoder( var2, var3, var4 );
        var5.decodeGetLoginData();
        this.nmasVersion = var5.getNmasVersion();
        this.nmasRetCode = var5.getNmasRetCode();
        if ( this.nmasRetCode == 0 )
        {
            this.nmasRetData = var5.getNmasRetData();
            this.nmasRetDataLen = this.nmasRetData.length;
        }

    }

    public byte[] getEncodedValue()
    {
        return this.responseBer;
    }

    public String getID()
    {
        return this.OID;
    }

    public int getNmasRetCode()
    {
        return this.nmasRetCode;
    }

    public int getNmasRetDataLen()
    {
        return this.nmasRetDataLen;
    }

    public byte[] getNmasRetData()
    {
        return this.nmasRetData;
    }
}
