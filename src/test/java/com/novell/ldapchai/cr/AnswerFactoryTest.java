package com.novell.ldapchai.cr;

import org.junit.Assert;
import org.junit.Test;

public class AnswerFactoryTest
{
    @Test
    public void testAnswerTesting()
    {
        for ( final Answer.FormatType formatType : Answer.FormatType.implementedValues() )
        {
            System.out.println( formatType );
            final AnswerConfiguration answerConfiguration = makeAnswerConfiguration( formatType );
            final Answer answer = AnswerFactory.newAnswer( answerConfiguration, "response-value" );
            Assert.assertTrue( formatType + " format answer test error", answer.testAnswer( "response-value" ) );
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
