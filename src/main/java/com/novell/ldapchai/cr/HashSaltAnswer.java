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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

class HashSaltAnswer implements Answer
{
    private static final String VERSION_SEPARATOR = ":";
    private static final VERSION DEFAULT_VERSION = VERSION.B;

    protected final String answerHash;
    protected final String salt;
    protected final int hashCount;
    protected final boolean caseInsensitive;
    protected final FormatType formatType;
    protected final VERSION version;

    enum VERSION
    {
        // original version had bug where only one iteration was ever actually performed regardless of hashCount value
        A,

        // nominal working version
        B,
    }


    enum HashType
    {
        MD5( FormatType.MD5, "MD5", false ),
        SHA1( FormatType.SHA1, "SHA1", false ),
        SHA1_SALT( FormatType.SHA1_SALT, "SHA1", true ),
        SHA256_SALT( FormatType.SHA256_SALT, "SHA-256", true ),
        SHA512_SALT( FormatType.SHA512_SALT, "SHA-512", true ),;

        private final FormatType formatType;
        private final String hashAlgName;
        private final boolean saltEnabled;

        HashType( final FormatType formatType, final String hashAlgName, final boolean saltEnabled )
        {
            this.hashAlgName = hashAlgName;
            this.formatType = formatType;
            this.saltEnabled = saltEnabled;
        }

        public FormatType getFormatType()
        {
            return formatType;
        }

        public String getHashAlgName()
        {
            return hashAlgName;
        }

        public boolean isSaltEnabled()
        {
            return saltEnabled;
        }

        public static HashType forFormatType( final FormatType formatType )
        {
            return Arrays.stream( values() )
                    .filter( ( t ) -> t.getFormatType() == formatType )
                    .findFirst()
                    .orElseThrow( () ->  new IllegalArgumentException( "unsupported format type '" ) );
        }
    }


    HashSaltAnswer(
            final String answerHash,
            final String salt,
            final int hashCount,
            final boolean caseInsensitive,
            final FormatType formatType,
            final VERSION version
    )
    {
        if ( StringHelper.isEmpty( answerHash ) )
        {
            throw new IllegalArgumentException( "missing answerHash value" );
        }

        // throw exception for unknown format type;
        HashType.forFormatType( formatType );

        this.answerHash = answerHash;
        this.version = version;
        this.formatType = formatType;
        this.salt = salt;
        this.hashCount = hashCount;
        this.caseInsensitive = caseInsensitive;
    }

    HashSaltAnswer( final AnswerConfiguration answerConfiguration, final String answer )
    {
        this.hashCount = answerConfiguration.iterations;
        this.caseInsensitive = answerConfiguration.caseInsensitive;
        this.formatType = answerConfiguration.formatType;
        this.version = DEFAULT_VERSION;

        if ( StringHelper.isEmpty( answer ) )
        {
            throw new IllegalArgumentException( "missing answer value" );
        }

        final HashType hashType = HashType.forFormatType( formatType );

        // make hash
        final boolean includeSalt = hashType.isSaltEnabled();
        final String casedAnswer = caseInsensitive ? answer.toLowerCase() : answer;
        this.salt = includeSalt ? generateSalt( answerConfiguration.getSaltCharCount() ) : "";
        final String saltedAnswer = includeSalt ? salt + casedAnswer : casedAnswer;
        this.answerHash = hashValue( saltedAnswer );
    }

    @Override
    public XmlElement toXml()
    {
        final XmlElement answerElement = XmlFactory.getFactory().newElement( ChaiResponseSet.XML_NODE_ANSWER_VALUE );
        answerElement.setText( version.toString() + VERSION_SEPARATOR + answerHash );

        if ( !StringHelper.isEmpty( salt ) )
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
        final String saltedTest = salt + casedResponse;
        final String hashedTest = hashValue( saltedTest );
        return answerHash.equalsIgnoreCase( hashedTest );
    }

    protected String hashValue( final String input )
    {
        return doHash( input, hashCount, formatType, version );
    }

    static String doHash(
            final String input,
            final int hashCount,
            final FormatType formatType,
            final VERSION version
    )
            throws IllegalStateException
    {
        final HashType hashType = HashType.forFormatType( formatType );
        final MessageDigest md;
        try
        {
            md = MessageDigest.getInstance( hashType.getHashAlgName() );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new IllegalStateException( "unable to load " + hashType.getHashAlgName() + " message digest algorithm: " + e.getMessage() );
        }


        byte[] hashedBytes = input.getBytes( ChaiCrFactory.DEFAULT_CHARSET );
        switch ( version )
        {
            case A:
                hashedBytes = md.digest( hashedBytes );
                return StringHelper.base64Encode( hashedBytes );

            case B:
                for ( int i = 0; i < hashCount; i++ )
                {
                    hashedBytes = md.digest( hashedBytes );
                }
                return StringHelper.base64Encode( hashedBytes );

            default:
                throw new IllegalStateException( "unexpected version enum in hash method" );
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
                version.toString() + VERSION_SEPARATOR + answerHash,
                salt,
                hashCount,
                caseInsensitive );

    }

    static class HashSaltAnswerFactory implements ImplementationFactory
    {
        @Override
        public HashSaltAnswer newAnswer(
                final AnswerConfiguration answerConfiguration,
                final String answer
        )
        {
            return new HashSaltAnswer( answerConfiguration, answer );
        }

        @Override
        public Answer fromAnswerBean( final AnswerBean input, final String challengeText )
        {

            final String answerValue = input.getAnswerHash();

            if ( answerValue == null || answerValue.length() < 1 )
            {
                throw new IllegalArgumentException( "missing answer value" );
            }

            final String hashString;
            final VERSION version;
            if ( answerValue.contains( VERSION_SEPARATOR ) )
            {
                final String[] s = answerValue.split( VERSION_SEPARATOR );
                try
                {
                    version = VERSION.valueOf( s[0] );
                }
                catch ( IllegalArgumentException e )
                {
                    throw new IllegalArgumentException( "unsupported version type " + s[0] );
                }
                hashString = s[1];
            }
            else
            {
                version = VERSION.A;
                hashString = answerValue;
            }

            return new HashSaltAnswer(
                    hashString,
                    input.getSalt(),
                    input.getHashCount(),
                    input.isCaseInsensitive(),
                    input.getType(),
                    version
            );
        }

        @Override
        public HashSaltAnswer fromXml( final XmlElement element, final boolean caseInsensitive, final String challengeText )
        {
            final String answerValue = element.getText().orElseThrow( () -> new IllegalArgumentException( "missing answer value" ) );

            final String hashString;
            final VERSION version;
            if ( answerValue.contains( VERSION_SEPARATOR ) )
            {
                final String[] s = answerValue.split( VERSION_SEPARATOR );
                try
                {
                    version = VERSION.valueOf( s[0] );
                }
                catch ( IllegalArgumentException e )
                {
                    throw new IllegalArgumentException( "unsupported version type " + s[0] );
                }
                hashString = s[1];
            }
            else
            {
                version = VERSION.A;
                hashString = answerValue;
            }

            final String salt = element.getAttribute( ChaiResponseSet.XML_ATTRIBUTE_SALT ).orElse( "" );
            final String hashCount = element.getAttribute( ChaiResponseSet.XML_ATTRIBUTE_HASH_COUNT ).orElse( "1" );

            int saltCount = 1;
            try
            {
                saltCount = Integer.parseInt( hashCount );
            }
            catch ( NumberFormatException e )
            {
                /* noop */
            }
            final String formatStr = element.getAttribute( ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT ).orElse( "" );
            final FormatType formatType;
            try
            {
                formatType = FormatType.valueOf( formatStr );
            }
            catch ( IllegalArgumentException e )
            {
                throw new IllegalArgumentException( "unknown content format specified in xml format value: '" + formatStr + "'" );
            }
            return new HashSaltAnswer( hashString, salt, saltCount, caseInsensitive, formatType, version );
        }
    }
}
