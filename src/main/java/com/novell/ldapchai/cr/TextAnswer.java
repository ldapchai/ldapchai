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
import org.jrivard.xmlchai.XmlElement;
import org.jrivard.xmlchai.XmlFactory;

class TextAnswer implements Answer
{
    private final String answer;
    private final boolean caseInsensitive;

    TextAnswer( final String answer, final boolean caseInsensitive )
    {
        if ( answer == null || answer.length() < 1 )
        {
            throw new IllegalArgumentException( "missing answer text" );
        }

        this.answer = answer;
        this.caseInsensitive = caseInsensitive;
    }

    @Override
    public XmlElement toXml()
    {
        final XmlElement answerElement = XmlFactory.getFactory().newElement( ChaiResponseSet.XML_NODE_ANSWER_VALUE );
        answerElement.setText( answer );
        answerElement.setAttribute( ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT, FormatType.TEXT.toString() );
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

    @Override
    public AnswerBean asAnswerBean()
    {
        return new AnswerBean(
                FormatType.TEXT,
                answer,
                null,
                null,
                0,
                caseInsensitive );
    }

    static class TextAnswerFactory implements ImplementationFactory
    {
        @Override
        public TextAnswer newAnswer( final AnswerConfiguration answerConfiguration, final String answer )
        {
            final boolean caseInsensitive = answerConfiguration.caseInsensitive;
            return new TextAnswer( answer, caseInsensitive );
        }

        @Override
        public Answer fromAnswerBean( final AnswerBean input, final String challengeText )
        {
            return new TextAnswer( input.getAnswerText(), input.isCaseInsensitive() );
        }

        @Override
        public TextAnswer fromXml( final XmlElement element, final boolean caseInsensitive, final String challengeText )
        {
            final String answerValue = element.getText().orElseThrow( () -> new IllegalArgumentException( "missing answer text" ) );
            return new TextAnswer( answerValue, caseInsensitive );
        }
    }
}
