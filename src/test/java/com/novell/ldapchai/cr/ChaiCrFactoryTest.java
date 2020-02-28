package com.novell.ldapchai.cr;

import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiSetting;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChaiCrFactoryTest
{
    @Test
    public void testResponseSetAnswers()
            throws Exception
    {
        for ( final Answer.FormatType formatType : Answer.FormatType.implementedValues() )
        {
            final Instant startTime = Instant.now();
            final Map<Challenge, String> challengeAnswerMap = new HashMap<>();
            challengeAnswerMap.put(
                    new ChaiChallenge( true, "challenge1", 0, 255, true, 0, false ),
                    "response1" );
            challengeAnswerMap.put(
                    new ChaiChallenge( true, "challenge2", 0, 255, true, 0, false ),
                    "response2" );
            challengeAnswerMap.put(
                    new ChaiChallenge( true, "challenge3", 0, 255, true, 0, false ),
                    "response3" );

            final ChaiConfiguration chaiConfiguration = ChaiConfiguration.builder( "ldap://1", "bindDN", "bindPW" )
                    .setSetting( ChaiSetting.CR_DEFAULT_FORMAT_TYPE, formatType.name() )
                    .build();

            final ResponseSet responseSet;
            {
                final ResponseSet responseSetTemp = ChaiCrFactory.newChaiResponseSet(
                        challengeAnswerMap, Locale.US, 0, chaiConfiguration, "test-response-set" );
                final String stringValue = responseSetTemp.stringValue();
                responseSet = ChaiCrFactory.parseChaiResponseSetXML( stringValue );
            }
            final boolean tested = responseSet.test( challengeAnswerMap );
            Assert.assertTrue("responseSet.test() fail for format type " + formatType,  tested );
            //System.out.println( responseSet.stringValue() );
            final Duration duration = Duration.between( startTime, Instant.now() );
            System.out.println( " format " + formatType.name() + " time: " + duration.toString() );
        }
    }
}
