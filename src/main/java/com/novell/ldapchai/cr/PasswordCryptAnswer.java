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
import com.novell.ldapchai.util.internal.SCryptUtil;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import org.jrivard.xmlchai.XmlElement;
import org.jrivard.xmlchai.XmlFactory;

import java.security.SecureRandom;
import java.util.function.Supplier;

class PasswordCryptAnswer implements Answer
{
    private static final Supplier<SecureRandom> SECURE_RANDOM_SUPPLIER = SecureRandom::new;

    private final String answerHash;
    private final boolean caseInsensitive;
    private final FormatType formatType;

    private PasswordCryptAnswer( final String answerHash, final boolean caseInsensitive, final FormatType formatType )
    {
        if ( answerHash == null || answerHash.length() < 1 )
        {
            throw new IllegalArgumentException( "missing answer text" );
        }

        this.answerHash = answerHash;
        this.caseInsensitive = caseInsensitive;
        this.formatType = formatType;
    }

    private PasswordCryptAnswer( final AnswerConfiguration answerConfiguration, final String answer )
    {
        if ( answer == null || answer.length() < 1 )
        {
            throw new IllegalArgumentException( "missing answerHash text" );
        }

        this.caseInsensitive = answerConfiguration.isCaseInsensitive();
        this.formatType = answerConfiguration.formatType;
        final String casedAnswer = caseInsensitive ? answer.toLowerCase() : answer;

        final int iterations = Math.max( 10, answerConfiguration.getIterations() );
        final int saltChars = Math.max( 16, answerConfiguration.getSaltCharCount() );

        switch ( formatType )
        {
            case BCRYPT:
                final byte[] salt = new byte[saltChars];
                SECURE_RANDOM_SUPPLIER.get().nextBytes( salt );
                answerHash = OpenBSDBCrypt.generate( casedAnswer.toCharArray(), salt, iterations );
                break;

            case SCRYPT:
                answerHash = SCryptUtil.scrypt( casedAnswer, saltChars, iterations );
                break;

            default:
                throw new IllegalArgumentException( "can't test answer for unknown format " + formatType.toString() );
        }
    }

    @Override
    public XmlElement toXml()
    {
        final XmlElement answerElement = XmlFactory.getFactory().newElement( ChaiResponseSet.XML_NODE_ANSWER_VALUE );
        answerElement.setText( answerHash );
        answerElement.setAttribute( ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT, formatType.toString() );
        return answerElement;
    }

    @Override
    public boolean testAnswer( final String testResponse )
    {
        if ( testResponse == null )
        {
            return false;
        }

        final String casedAnswer = caseInsensitive ? testResponse.toLowerCase() : testResponse;
        switch ( formatType )
        {
            case BCRYPT:
                return OpenBSDBCrypt.checkPassword( answerHash, casedAnswer.toCharArray() );

            case SCRYPT:
                return SCryptUtil.check( casedAnswer, answerHash );

            default:
                throw new IllegalArgumentException( "can't test answer for unknown format " + formatType.toString() );
        }
    }

    @Override
    public AnswerBean asAnswerBean()
    {
        return new AnswerBean(
                formatType,
                null,
                answerHash,
                null,
                -1,
                caseInsensitive );
    }

    static class PasswordCryptAnswerFactory implements ImplementationFactory
    {
        @Override
        public PasswordCryptAnswer newAnswer( final AnswerConfiguration answerConfiguration, final String answer )
        {
            return new PasswordCryptAnswer( answerConfiguration, answer );
        }

        @Override
        public PasswordCryptAnswer fromAnswerBean( final AnswerBean answerBean, final String challengeText )
        {
            return new PasswordCryptAnswer( answerBean.getAnswerHash(), answerBean.isCaseInsensitive(), answerBean.getType() );
        }

        @Override
        public PasswordCryptAnswer fromXml( final XmlElement element, final boolean caseInsensitive, final String challengeText )
        {
            final String answerValue = element.getText()
                    .orElseThrow( () -> new IllegalArgumentException( "missing answer text" ) );
            final String formatStr = element.getAttribute( ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT )
                    .orElseThrow( () -> new IllegalArgumentException( "missing format type" ) );

            final FormatType formatType;
            try
            {
                formatType = FormatType.valueOf( formatStr );
            }
            catch ( IllegalArgumentException e )
            {
                throw new IllegalArgumentException( "unknown content format specified in xml format value: '" + formatStr + "'" );
            }
            return new PasswordCryptAnswer( answerValue, caseInsensitive, formatType );
        }
    }
}
