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
import com.novell.ldapchai.util.internal.StringHelper;
import org.jrivard.xmlchai.XmlElement;
import org.jrivard.xmlchai.XmlFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;

class PBKDF2Answer implements Answer
{
    private final String hashedAnswer;
    private final String salt;
    private final int hashCount;
    private final boolean caseInsensitive;

    private final FormatType formatType;

    private PBKDF2Answer(
            final FormatType formatType,
            final String answerHash,
            final String salt,
            final int hashCount,
            final boolean caseInsensitive
    )
    {
        if ( formatType == null )
        {
            throw new IllegalArgumentException( "missing formatType" );
        }

        switch ( formatType )
        {
            case PBKDF2:
            case PBKDF2_SHA256:
            case PBKDF2_SHA512:
                break;

            default:
                throw new IllegalArgumentException( "unsupported formatType: " + formatType );
        }


        if ( answerHash == null || answerHash.length() < 1 )
        {
            throw new IllegalArgumentException( "missing answerHash" );
        }

        this.formatType = formatType;
        this.hashedAnswer = answerHash;
        this.salt = salt;
        this.hashCount = hashCount;
        this.caseInsensitive = caseInsensitive;
    }

    private PBKDF2Answer( final AnswerConfiguration answerConfiguration, final String answer )
    {
        this.hashCount = answerConfiguration.iterations;
        this.caseInsensitive = answerConfiguration.caseInsensitive;
        this.salt = generateSalt( 32 );
        this.formatType = answerConfiguration.getFormatType();

        if ( answer == null || answer.length() < 1 )
        {
            throw new IllegalArgumentException( "missing answerHash text" );
        }

        final String casedAnswer = this.caseInsensitive ? answer.toLowerCase() : answer;
        this.hashedAnswer = hashValue( casedAnswer );
    }


    @Override
    public XmlElement toXml()
    {
        final XmlElement answerElement = XmlFactory.getFactory().newElement( ChaiResponseSet.XML_NODE_ANSWER_VALUE );
        answerElement.setText( this.hashedAnswer );
        if ( salt != null && salt.length() > 0 )
        {
            answerElement.setAttribute( ChaiResponseSet.XML_ATTRIBUTE_SALT, salt );
        }
        answerElement.setAttribute( ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT, formatType.toString() );
        if ( hashCount > 1 )
        {
            answerElement.setAttribute( ChaiResponseSet.XML_ATTRIBUTE_HASH_COUNT, String.valueOf( hashCount ) );
        }
        return answerElement;
    }


    @Override
    public boolean testAnswer( final String testResponse )
    {
        if ( testResponse == null )
        {
            return false;
        }

        final String casedResponse = caseInsensitive ? testResponse.toLowerCase() : testResponse;
        final String hashedTest = hashValue( casedResponse );

        if ( hashedTest != null && hashedAnswer != null )
        {
            return hashedAnswer.equalsIgnoreCase( hashedTest );
        }

        return false;
    }

    private String hashValue( final String input )
    {
        try
        {
            final PBEKeySpec spec;
            final SecretKeyFactory skf;
            {
                final String methodName;
                final int keyLength;
                switch ( formatType )
                {
                    case PBKDF2:
                        methodName = "PBKDF2WithHmacSHA1";
                        keyLength = 64 * 8;
                        break;

                    case PBKDF2_SHA256:
                        methodName = "PBKDF2WithHmacSHA256";
                        keyLength = 128 * 8;
                        break;

                    case PBKDF2_SHA512:
                        methodName = "PBKDF2WithHmacSHA512";
                        keyLength = 192 * 8;
                        break;

                    default:
                        throw new IllegalStateException( "formatType not supported: " + formatType.toString() );

                }

                final char[] chars = input.toCharArray();
                final byte[] saltBytes = salt.getBytes( ChaiCrFactory.DEFAULT_CHARSET );

                spec = new PBEKeySpec( chars, saltBytes, hashCount, keyLength );
                skf = SecretKeyFactory.getInstance( methodName );
            }
            final byte[] hash = skf.generateSecret( spec ).getEncoded();
            return StringHelper.base64Encode( hash );
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( "unable to perform PBKDF2 hashing operation: " + e.getMessage() );
        }
    }

    private static String generateSalt( final int length )
    {
        final SecureRandom random = new SecureRandom();
        final StringBuilder sb = new StringBuilder( length );
        for ( int i = 0; i < length; i++ )
        {
            sb.append( ChaiResponseSet.SALT_CHARS.charAt( random.nextInt( ChaiResponseSet.SALT_CHARS.length() ) ) );
        }
        return sb.toString();
    }

    @Override
    public AnswerBean asAnswerBean()
    {
        return new AnswerBean(
                formatType,
                null,
                hashedAnswer,
                salt,
                hashCount,
                caseInsensitive );
    }

    static class PKDBF2AnswerFactory implements ImplementationFactory
    {
        @Override
        public PBKDF2Answer newAnswer(
                final AnswerConfiguration answerConfiguration,
                final String answer
        )
        {
            return new PBKDF2Answer( answerConfiguration, answer );
        }

        @Override
        public Answer fromAnswerBean( final AnswerBean input, final String challengeText )
        {

            final String answerValue = input.getAnswerHash();

            if ( answerValue == null || answerValue.length() < 1 )
            {
                throw new IllegalArgumentException( "missing answer value" );
            }

            return new PBKDF2Answer(
                    input.getType(),
                    input.getAnswerHash(),
                    input.getSalt(),
                    input.getHashCount(),
                    input.isCaseInsensitive()
            );
        }

        @Override
        public PBKDF2Answer fromXml( final XmlElement element, final boolean caseInsensitive, final String challengeText )
        {
            final String answerValue = element.getText().orElseThrow( () -> new IllegalArgumentException( "missing answer value" ) );

            final String salt = element.getAttribute( ChaiResponseSet.XML_ATTRIBUTE_SALT ).orElse( "" );
            final String hashCount = element.getAttribute( ChaiResponseSet.XML_ATTRIBUTE_HASH_COUNT ).orElse( "1" );
            final FormatType formatTypeEnum = element.getAttribute( ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT )
                    .map( FormatType::valueOf )
                    .orElseThrow( () -> new IllegalArgumentException( "unknown format type" ) );
            int saltCount = 1;
            try
            {
                saltCount = Integer.parseInt( hashCount );
            }
            catch ( NumberFormatException e )
            {
                /* noop */
            }
            return new PBKDF2Answer( formatTypeEnum, answerValue, salt, saltCount, caseInsensitive );
        }
    }
}
