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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AnswerFactoryTest
{
    @Test
    public void testAnswerTesting()
    {
        for ( final Answer.FormatType formatType : Answer.FormatType.implementedValues() )
        {
            final AnswerConfiguration answerConfiguration = makeAnswerConfiguration( formatType );
            final Answer answer = AnswerFactory.newAnswer( answerConfiguration, "response-value" );
            Assertions.assertTrue( answer.testAnswer( "response-value" ), formatType + " format answer test error" );
        }
    }

    private AnswerConfiguration makeAnswerConfiguration( final Answer.FormatType formatType )
    {
        return AnswerConfiguration.builder()
                .caseInsensitive( false )
                .challengeText( "challenge" )
                .formatType( formatType )
                .iterations( formatType.getDefaultIterations() )
                .build();
    }
}
