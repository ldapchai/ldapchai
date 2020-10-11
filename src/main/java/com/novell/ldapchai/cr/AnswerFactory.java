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

/**
 * Factory for generating {@link Answer} instances.
 */
public class AnswerFactory
{
    private AnswerFactory()
    {
    }

    public static Answer newAnswer( final AnswerConfiguration answerConfiguration, final String answerText )
    {
        final Answer.ImplementationFactory implementationFactory = answerConfiguration.getFormatType().getFactory();
        return implementationFactory.newAnswer( answerConfiguration, answerText );
    }

    public static Answer fromAnswerBean( final AnswerBean input, final String challengeText )
    {
        final Answer.ImplementationFactory implementationFactory = input.getType().getFactory();
        return implementationFactory.fromAnswerBean( input, challengeText );
    }

    public static Answer fromXml( final org.jdom2.Element element, final boolean caseInsensitive, final String challengeText )
    {
        final String formatStr = element.getAttribute( ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT ).getValue();
        final Answer.FormatType respFormat;
        if ( formatStr != null && formatStr.length() > 0 )
        {
            respFormat = Answer.FormatType.valueOf( formatStr );
        }
        else
        {
            respFormat = Answer.FormatType.TEXT;
        }
        return respFormat.getFactory().fromXml( element, caseInsensitive, challengeText );
    }

}
