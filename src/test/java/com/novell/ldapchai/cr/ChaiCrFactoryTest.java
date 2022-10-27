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

import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.util.internal.ChaiLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChaiCrFactoryTest
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( ChaiCrFactoryTest.class );

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
            Assertions.assertTrue( tested, "responseSet.test() fail for format type " + formatType );
            //System.out.println( responseSet.stringValue() );
            final Duration duration = Duration.between( startTime, Instant.now() );

            LOGGER.trace( () ->  " format " + formatType.name() + " time: " + duration.toString() );
        }
    }
}
