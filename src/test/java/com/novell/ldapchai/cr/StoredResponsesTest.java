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

import com.novell.ldapchai.TestHelper;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StoredResponsesTest
{
    @Test
    public void testChaiStoredResponseSet1()
            throws IOException, ChaiValidationException, ChaiOperationException, ChaiUnavailableException
    {
        final String responseAsString = TestHelper.readResourceFile( StoredResponsesTest.class, "ChaiResponseSet1.xml" );
        final ResponseSet responseSet = ChaiResponseSet.ChaiResponseXmlParser.parseChaiResponseSetXML( responseAsString );

        final Map<String, String> testAnswers = new HashMap<>();
        {
            testAnswers.put( "How do you like your burger?", "burger" );
            testAnswers.put( "What is your mother's maiden name?", "name" );
            testAnswers.put( "What is your PIN?", "123456" );
        }

        Assertions.assertNotNull( responseSet );
        Assertions.assertNotNull( responseSet.getChallengeSet() );
        Assertions.assertNotNull( responseSet.getHelpdeskResponses() );

        Assertions.assertEquals( 5, responseSet.getChallengeSet().getChallenges().size() );
        Assertions.assertEquals( 3, responseSet.getChallengeSet().getRandomChallenges().size() );
        Assertions.assertEquals( 2, responseSet.getChallengeSet().getRequiredChallenges().size() );

        Assertions.assertEquals( 1, responseSet.getChallengeSet().getMinRandomRequired() );
        Assertions.assertEquals( 3, responseSet.getChallengeSet().minimumResponses() );

        final Map<Challenge, String> challengeTestMap = new HashMap<>();
        for ( final Challenge challenge : responseSet.getChallengeSet().getChallenges() )
        {
            if ( testAnswers.containsKey( challenge.getChallengeText() ) )
            {
                challengeTestMap.put( challenge, testAnswers.get( challenge.getChallengeText() ) );
            }
        }

        Assertions.assertTrue( responseSet.test( challengeTestMap ) );
    }

    @Test
    public void testChaiStoredResponseSet2()
            throws IOException, ChaiValidationException, ChaiOperationException, ChaiUnavailableException
    {
        final String responseAsString = TestHelper.readResourceFile( StoredResponsesTest.class, "ChaiResponseSet2.xml" );
        final ResponseSet responseSet = ChaiResponseSet.ChaiResponseXmlParser.parseChaiResponseSetXML( responseAsString );

        final Map<String, String> testAnswers = new HashMap<>();
        {
            testAnswers.put( "What is the name of your favoraaite teacher?", "teacher" );
            testAnswers.put( "Who is your favorite author?", "author" );
        }

        Assertions.assertNotNull( responseSet );
        Assertions.assertNotNull( responseSet.getChallengeSet() );
        Assertions.assertNotNull( responseSet.getHelpdeskResponses() );

        Assertions.assertEquals( 4, responseSet.getChallengeSet().getChallenges().size() );
        Assertions.assertEquals( 4, responseSet.getChallengeSet().getRandomChallenges().size() );
        Assertions.assertEquals( 0, responseSet.getChallengeSet().getRequiredChallenges().size() );

        Assertions.assertEquals( 2, responseSet.getChallengeSet().getMinRandomRequired() );
        Assertions.assertEquals( 2, responseSet.getChallengeSet().minimumResponses() );

        final Map<Challenge, String> challengeTestMap = new HashMap<>();
        for ( final Challenge challenge : responseSet.getChallengeSet().getChallenges() )
        {
            if ( testAnswers.containsKey( challenge.getChallengeText() ) )
            {
                challengeTestMap.put( challenge, testAnswers.get( challenge.getChallengeText() ) );
            }
        }

        Assertions.assertTrue( responseSet.test( challengeTestMap ) );
    }


    @Test
    public void testNmasStoredResponseSet1()
            throws IOException, ChaiValidationException, ChaiOperationException, ChaiUnavailableException
    {
    /*
        final String responseAsString = TestHelper.readResourceFile( StoredResponsesTest.class, "NmasResponseSet1.xml" );
        final ChallengeSet challengeSet = NmasResponseSet.parseNmasUserResponseXML( responseAsString );
        Assert.assertEquals( 0, challengeSet.getChallenges().size() );
     */
    }
}
