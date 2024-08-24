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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class AnswerTest
{
    @ParameterizedTest
    @ArgumentsSource( AnswerTestParameterProviders.FormatTypeTestArgumentProvider.class )
    public void testCaseSensitivesAnswerTesting( final Answer.FormatType formatType )
    {
        {
            final AnswerConfiguration answerConfiguration = AnswerConfiguration.builder()
                    .caseInsensitive( false )
                    .challengeText( "challenge" )
                    .formatType( formatType )
                    .iterations( formatType.getDefaultIterations() )
                    .build();
            final Answer answer = AnswerFactory.newAnswer( answerConfiguration, "response-value" );
            Assertions.assertTrue( answer.testAnswer( "response-value" ) );
            Assertions.assertFalse( answer.testAnswer( "response-VALUE" ) );
        }
    }

    @ParameterizedTest
    @ArgumentsSource( AnswerTestParameterProviders.FormatTypeTestArgumentProvider.class )
    public void testCaseInSensitiveLowerAnswerTesting( final Answer.FormatType formatType )
    {
        final AnswerConfiguration answerConfiguration = AnswerConfiguration.builder()
                .caseInsensitive( true )
                .challengeText( "challenge" )
                .formatType( formatType )
                .iterations( formatType.getDefaultIterations() )
                .build();
        final Answer answer = AnswerFactory.newAnswer( answerConfiguration, "response-value" );
        Assertions.assertTrue( answer.testAnswer( "response-value" ) );
        Assertions.assertTrue( answer.testAnswer( "response-VALUE" ) );
    }

    @ParameterizedTest
    @ArgumentsSource( AnswerTestParameterProviders.FormatTypeTestArgumentProvider.class )
    public void testCaseInSensitiveUpperAnswerTesting( final Answer.FormatType formatType )
    {
        final AnswerConfiguration answerConfiguration = AnswerConfiguration.builder()
                .caseInsensitive( true )
                .challengeText( "challege" )
                .formatType( formatType )
                .iterations( formatType.getDefaultIterations() )
                .build();
        final Answer answer = AnswerFactory.newAnswer( answerConfiguration, "response-VALUE" );
        Assertions.assertTrue( answer.testAnswer( "response-value" ) );
        Assertions.assertTrue( answer.testAnswer( "response-VALUE" ) );
    }
}
