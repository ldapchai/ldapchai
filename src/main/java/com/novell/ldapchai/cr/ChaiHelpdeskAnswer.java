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

package com.novell.ldapchai.cr;

import com.novell.ldapchai.cr.bean.AnswerBean;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.util.internal.StringHelper;
import org.jrivard.xmlchai.XmlElement;
import org.jrivard.xmlchai.XmlFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class ChaiHelpdeskAnswer implements HelpdeskAnswer
{
    private final String challengeText;
    private final String answer;
    private final boolean caseInsensitive;


    ChaiHelpdeskAnswer( final String answer, final String challengeText, final boolean caseInsensitive )
    {
        if ( StringHelper.isEmpty( answer ) )
        {
            throw new IllegalArgumentException( "missing answer text" );
        }

        this.answer = answer;
        this.challengeText = challengeText;
        this.caseInsensitive = caseInsensitive;

    }

    @Override
    public String answerText()
    {
        return answer;
    }

    @Override
    public XmlElement toXml()
            throws ChaiOperationException
    {
        final XmlElement answerElement = XmlFactory.getFactory().newElement( ChaiResponseSet.XML_NODE_ANSWER_VALUE );
        answerElement.setText( encryptValue( answer, challengeText ) );
        answerElement.setAttribute( ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT, FormatType.HELPDESK.toString() );
        return answerElement;
    }

    @Override
    public boolean testAnswer( final String testResponse )
    {
        if ( testResponse == null )
        {
            return false;
        }

        return caseInsensitive
                ? testResponse.equalsIgnoreCase( answer )
                : testResponse.equals( answer );
    }

    private static String encryptValue( final String value, final String key )
            throws ChaiOperationException
    {
        try
        {
            if ( value == null || value.length() < 1 )
            {
                return "";
            }

            final SecretKey secretKey = makeKey( key );
            final Cipher cipher = Cipher.getInstance( "AES" );
            cipher.init( Cipher.ENCRYPT_MODE, secretKey, cipher.getParameters() );
            final byte[] encrypted = cipher.doFinal( value.getBytes( ChaiCrFactory.DEFAULT_CHARSET ) );
            return StringHelper.base64Encode( encrypted, StringHelper.Base64Options.URL_SAFE, StringHelper.Base64Options.GZIP );
        }
        catch ( Exception e )
        {
            final String errorMsg = "unexpected error performing helpdesk answer crypt operation: " + e.getMessage();
            throw new ChaiOperationException( errorMsg, ChaiError.CHAI_INTERNAL_ERROR, e );
        }
    }

    private static String decryptValue( final String value, final String key )
    {
        try
        {
            if ( value == null || value.length() < 1 )
            {
                return "";
            }

            final SecretKey secretKey = makeKey( key );
            final byte[] decoded = StringHelper.base64Decode( value, StringHelper.Base64Options.URL_SAFE, StringHelper.Base64Options.GZIP );
            final Cipher cipher = Cipher.getInstance( "AES" );
            cipher.init( Cipher.DECRYPT_MODE, secretKey );
            final byte[] decrypted = cipher.doFinal( decoded );
            return new String( decrypted, ChaiCrFactory.DEFAULT_CHARSET );
        }
        catch ( Exception e )
        {
            final String errorMsg = "unexpected error performing helpdesk answer decrypt operation: " + e.getMessage();
            throw new IllegalArgumentException( errorMsg );
        }
    }

    private static SecretKey makeKey( final String text )
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        final MessageDigest md = MessageDigest.getInstance( "SHA1" );
        md.update( text.getBytes( "iso-8859-1" ), 0, text.length() );
        final byte[] key = new byte[16];
        System.arraycopy( md.digest(), 0, key, 0, 16 );
        return new SecretKeySpec( key, "AES" );
    }

    @Override
    public AnswerBean asAnswerBean()
    {
        return new AnswerBean(
                FormatType.HELPDESK,
                answer,
                null,
                null,
                0,
                false );
    }

    static class ChaiHelpdeskAnswerFactory implements ImplementationFactory
    {
        @Override
        public Answer newAnswer( final AnswerConfiguration answerConfiguration, final String answerText )
        {
            return new ChaiHelpdeskAnswer( answerText, answerConfiguration.getChallengeText(), answerConfiguration.isCaseInsensitive() );
        }

        @Override
        public Answer fromAnswerBean( final AnswerBean input, final String challengeText )
        {
            return new ChaiHelpdeskAnswer( input.getAnswerText(), challengeText, input.isCaseInsensitive() );
        }

        @Override
        public ChaiHelpdeskAnswer fromXml( final XmlElement element, final boolean caseInsensitive, final String challengeText )
        {
            final String hashedAnswer = element.getText().orElseThrow( () -> new IllegalArgumentException( "missing answer hash" ) );
            final String answerValue = decryptValue( hashedAnswer, challengeText );
            return new ChaiHelpdeskAnswer( answerValue, challengeText, caseInsensitive );
        }

    }
}
