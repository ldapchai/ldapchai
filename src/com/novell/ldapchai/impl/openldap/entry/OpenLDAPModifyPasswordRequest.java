/*
 * LDAP Chai API
 * Copyright (c) 2006-2010 Novell, Inc.
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

import javax.naming.SizeLimitExceededException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import java.io.Serializable;

/**
 * A class conforming to the ExtendedRequest that implements the
 * Modify Password extended operation.
 *
 * @author ipac3870
 * @see javax.naming.ldap.ExtendedRequest
 */
public class OpenLDAPModifyPasswordRequest
        implements ExtendedRequest, Serializable
{
    /**
     * Creates a new <code>OpenLDAPUser</code> instance.
     *
     * @param dn the dn whose password is to change
     * @param password the new password
     * @exception NullPointerException if dn or password is null
     * @exception javax.naming.SizeLimitExceededException when the dn or password
     *            is too long
     */
    public OpenLDAPModifyPasswordRequest(String dn, String password)
            throws NullPointerException, SizeLimitExceededException
    {
        if (dn == null)
        {
            throw new NullPointerException("dn cannot be null");
        }

        if (password == null)
        {
            throw new NullPointerException("password cannot be null");
        }

        int dnlen = dn.length();
        int passlen = password.length();
        int totallen = 4 + dnlen + passlen;

        if (dnlen <= 0)
        {
            throw new SizeLimitExceededException("dn cannot be 0 length");
        }

        if (dnlen > 0xFF)
        {
            throw new SizeLimitExceededException(
                    "dn cannot be larger then 255 characters");
        }

        if (passlen <= 0)
        {
            throw new SizeLimitExceededException(
                    "password cannot be 0 length");
        }

        if (passlen > 0xFF)
        {
            throw new SizeLimitExceededException(
                    "password cannot be larger then 255 characters");
        }

        if (totallen > 0xFF)
        {
            throw new SizeLimitExceededException(
                    "the lengh of the dn + the lengh of the password cannot" +
                            " exceed 251 characters");
        }

        mDn = dn;
        mPassword = password;
    }

    /**
     * Returns the ID of this extended operation.
     *
     * @return a String with the OID of this operation
     */
    public String getID()
    {
        return LDAP_EXOP_X_MODIFY_PASSWD;
    }

    /**
     * Get the BER encoded value for this operation.
     *
     * @return a bytearray containing the BER sequence.
     */
    public byte[] getEncodedValue()
    {
        byte[] password = mPassword.getBytes();
        byte[] dn = mDn.getBytes();

        // Sequence tag (1) + sequence length (1) + dn tag (1) +
        // dn length (1) + dn (variable) + password tag (1) +
        // password length (1) + password (variable)
        int encodedLength = 6 + dn.length + password.length;

        byte[] encoded = new byte[encodedLength];

        int i = 0;
        encoded[i++] = (byte) 0x30; // sequence start
        // length of body
        encoded[i++] = (byte) (4 + dn.length + password.length);


        encoded[i++] = LDAP_TAG_EXOP_X_MODIFY_PASSWD_ID;
        encoded[i++] = (byte) dn.length;

        System.arraycopy(dn, 0, encoded, i, dn.length);
        i += dn.length;

        encoded[i++] = LDAP_TAG_EXOP_X_MODIFY_PASSWD_NEW;
        encoded[i++] = (byte) password.length;

        System.arraycopy(password, 0, encoded, i, password.length);
        i += password.length;

        return encoded;
    }

    /**
     * Creates the extended response.  With OpenLDAP, the extended
     * operation for Password modification doesn't create a
     * response so we just return null here.
     *
     * @param id the OID of the response
     * @param berValue the BER encoded value of the response
     * @param offset the offset
     * @param length the length of the response
     * @return returns null as the modify password operation doesn't
     *         generate a response.
     */
    public ExtendedResponse createExtendedResponse(String id,
                                                   byte[] berValue,
                                                   int offset, int length)
    {
        return null;
    }

    /** The OID of the modify password extended operation */
    public static final String LDAP_EXOP_X_MODIFY_PASSWD =
            "1.3.6.1.4.1.4203.1.11.1";
    /** The BER tag for the modify password dn entry */
    private static final byte LDAP_TAG_EXOP_X_MODIFY_PASSWD_ID =
            (byte) 0x80;
    /** The BER tag for the modify password new password entry */
    private static final byte LDAP_TAG_EXOP_X_MODIFY_PASSWD_NEW =
            (byte) 0x82;
    /** The dn we want to change */
    private String mDn;
    /** The password to change to */
    private String mPassword;
}
