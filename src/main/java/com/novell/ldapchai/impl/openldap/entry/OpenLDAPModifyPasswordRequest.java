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

package com.novell.ldapchai.impl.openldap.entry;

import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiSetting;

import javax.naming.SizeLimitExceededException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import java.nio.charset.Charset;

/**
 * A class conforming to the ExtendedRequest that implements the
 * Modify Password extended operation.
 *
 * @author ipac3870
 * @see javax.naming.ldap.ExtendedRequest
 */
public class OpenLDAPModifyPasswordRequest
        implements ExtendedRequest
{
    private static final long serialVersionUID = 1L;

    /**
     * The OID of the modify password extended operation.
     */
    public static final String LDAP_EXOP_X_MODIFY_PASSWD =
            "1.3.6.1.4.1.4203.1.11.1";
    /**
     * The BER tag for the modify password dn entry.
     */
    private static final byte LDAP_TAG_EXOP_X_MODIFY_PASSWD_ID =
            ( byte ) 0x80;
    /**
     * The BER tag for the modify password new password entry.
     */
    private static final byte LDAP_TAG_EXOP_X_MODIFY_PASSWD_NEW =
            ( byte ) 0x82;
    /**
     * The dn we want to change.
     */
    private String modifyDn;
    /**
     * The password to change to.
     */
    private String modifyPassword;

    private final transient ChaiConfiguration chaiConfiguration;

    /**
     * Creates a new <code>OpenLDAPUser</code> instance.
     *
     * @param dn       the dn whose password is to change
     * @param password the new password
     * @param chaiConfiguration appropriate chaiConfiguration
     * @throws NullPointerException                    if dn or password is null
     * @throws javax.naming.SizeLimitExceededException when the dn or password
     *                                                 is too long
     */
    public OpenLDAPModifyPasswordRequest( final String dn, final String password, final ChaiConfiguration chaiConfiguration )
            throws NullPointerException, SizeLimitExceededException
    {
        this.chaiConfiguration = chaiConfiguration;

        if ( dn == null )
        {
            throw new NullPointerException( "dn cannot be null" );
        }

        if ( password == null )
        {
            throw new NullPointerException( "password cannot be null" );
        }

        final int dnlen = dn.length();
        final int passlen = password.length();
        final int totallen = 4 + dnlen + passlen;

        if ( dnlen <= 0 )
        {
            throw new SizeLimitExceededException( "dn cannot be 0 length" );
        }

        if ( dnlen > 0xFF )
        {
            throw new SizeLimitExceededException( "dn cannot be larger then 255 characters" );
        }

        if ( passlen <= 0 )
        {
            throw new SizeLimitExceededException( "password cannot be 0 length" );
        }

        if ( passlen > 0xFF )
        {
            throw new SizeLimitExceededException( "password cannot be larger then 255 characters" );
        }

        if ( totallen > 0xFF )
        {
            throw new SizeLimitExceededException( "the length of the dn + the lengh of the password cannot"
                            + " exceed 251 characters" );
        }

        modifyDn = dn;
        modifyPassword = password;
    }

    /**
     * Returns the ID of this extended operation.
     *
     * @return a String with the OID of this operation
     */
    @Override
    public String getID()
    {
        return LDAP_EXOP_X_MODIFY_PASSWD;
    }

    /**
     * Get the BER encoded value for this operation.
     *
     * @return a bytearray containing the BER sequence.
     */
    @Override
    public byte[] getEncodedValue()
    {
        final String characterEncoding = this.chaiConfiguration.getSetting( ChaiSetting.LDAP_CHARACTER_ENCODING );
        final byte[] password = modifyPassword.getBytes( Charset.forName( characterEncoding ) );
        final byte[] dn = modifyDn.getBytes( Charset.forName( characterEncoding ) );

        // Sequence tag (1) + sequence length (1) + dn tag (1) +
        // dn length (1) + dn (variable) + password tag (1) +
        // password length (1) + password (variable)
        final int encodedLength = 6 + dn.length + password.length;

        final byte[] encoded = new byte[encodedLength];

        int valueI = 0;

        // sequence start
        encoded[valueI++] = ( byte ) 0x30;

        // length of body
        encoded[valueI++] = ( byte ) ( 4 + dn.length + password.length );


        encoded[valueI++] = LDAP_TAG_EXOP_X_MODIFY_PASSWD_ID;
        encoded[valueI++] = ( byte ) dn.length;

        System.arraycopy( dn, 0, encoded, valueI, dn.length );
        valueI += dn.length;

        encoded[valueI++] = LDAP_TAG_EXOP_X_MODIFY_PASSWD_NEW;
        encoded[valueI++] = ( byte ) password.length;

        System.arraycopy( password, 0, encoded, valueI, password.length );
        valueI += password.length;

        return encoded;
    }

    /**
     * Creates the extended response.  With OpenLDAP, the extended
     * operation for Password modification doesn't create a
     * response so we just return null here.
     *
     * @param id       the OID of the response
     * @param berValue the BER encoded value of the response
     * @param offset   the offset
     * @param length   the length of the response
     * @return returns null as the modify password operation doesn't
     *     generate a response.
     */
    @Override
    public ExtendedResponse createExtendedResponse(
            final String id,
            final byte[] berValue,
            final int offset,
            final int length )
    {
        return null;
    }

}
