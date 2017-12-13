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
import com.novell.ldapchai.exception.ChaiOperationException;
import org.jdom2.Element;

public interface Answer
{
    boolean testAnswer( String answer );

    Element toXml()
            throws ChaiOperationException;

    AnswerBean asAnswerBean();

    enum FormatType
    {
        TEXT( new TextAnswer.TextAnswerFactory() ),
        MD5( new HashSaltAnswer.HashSaltAnswerFactory() ),
        SHA1( new HashSaltAnswer.HashSaltAnswerFactory() ),
        SHA1_SALT( new HashSaltAnswer.HashSaltAnswerFactory() ),
        SHA256_SALT( new HashSaltAnswer.HashSaltAnswerFactory() ),
        SHA512_SALT( new HashSaltAnswer.HashSaltAnswerFactory() ),
        BCRYPT( new PasswordCryptAnswer.PasswordCryptAnswerFactory() ),
        SCRYPT( new PasswordCryptAnswer.PasswordCryptAnswerFactory() ),
        PBKDF2( new PKDBF2Answer.PKDBF2AnswerFactory() ),
        PBKDF2_SHA256( new PKDBF2Answer.PKDBF2AnswerFactory() ),
        PBKDF2_SHA512( new PKDBF2Answer.PKDBF2AnswerFactory() ),
        HELPDESK( new ChaiHelpdeskAnswer.ChaiHelpdeskAnswerFactory() ),
        NMAS( null ),;

        private ImplementationFactory factory;


        FormatType( final ImplementationFactory implementationClass )
        {
            this.factory = implementationClass;
        }

        public ImplementationFactory getFactory()
        {
            return factory;
        }
    }

    interface ImplementationFactory
    {
        Answer newAnswer( AnswerFactory.AnswerConfiguration answerConfiguration, String answerText );

        Answer fromAnswerBean( AnswerBean input, String challengeText );

        Answer fromXml( org.jdom2.Element element, boolean caseInsensitive, String challengeText );
    }


}
