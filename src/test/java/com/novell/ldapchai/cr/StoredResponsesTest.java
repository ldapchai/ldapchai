package com.novell.ldapchai.cr;

import com.novell.ldapchai.TestHelper;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.impl.edir.NmasResponseSet;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
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

        Assert.assertNotNull( responseSet );
        Assert.assertNotNull( responseSet.getChallengeSet() );
        Assert.assertNotNull( responseSet.getHelpdeskResponses() );

        Assert.assertEquals( 5, responseSet.getChallengeSet().getChallenges().size() );
        Assert.assertEquals( 3, responseSet.getChallengeSet().getRandomChallenges().size() );
        Assert.assertEquals( 2, responseSet.getChallengeSet().getRequiredChallenges().size() );

        Assert.assertEquals( 1, responseSet.getChallengeSet().getMinRandomRequired() );
        Assert.assertEquals( 3, responseSet.getChallengeSet().minimumResponses() );

        final Map<Challenge, String> challengeTestMap = new HashMap<>();
        for ( final Challenge challenge : responseSet.getChallengeSet().getChallenges() )
        {
            if ( testAnswers.containsKey( challenge.getChallengeText() ) )
            {
                challengeTestMap.put( challenge, testAnswers.get( challenge.getChallengeText() ) );
            }
        }

        Assert.assertTrue( responseSet.test( challengeTestMap ) );
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

        Assert.assertNotNull( responseSet );
        Assert.assertNotNull( responseSet.getChallengeSet() );
        Assert.assertNotNull( responseSet.getHelpdeskResponses() );

        Assert.assertEquals( 4, responseSet.getChallengeSet().getChallenges().size() );
        Assert.assertEquals( 4, responseSet.getChallengeSet().getRandomChallenges().size() );
        Assert.assertEquals( 0, responseSet.getChallengeSet().getRequiredChallenges().size() );

        Assert.assertEquals( 2, responseSet.getChallengeSet().getMinRandomRequired() );
        Assert.assertEquals( 2, responseSet.getChallengeSet().minimumResponses() );

        final Map<Challenge, String> challengeTestMap = new HashMap<>();
        for ( final Challenge challenge : responseSet.getChallengeSet().getChallenges() )
        {
            if ( testAnswers.containsKey( challenge.getChallengeText() ) )
            {
                challengeTestMap.put( challenge, testAnswers.get( challenge.getChallengeText() ) );
            }
        }

        Assert.assertTrue( responseSet.test( challengeTestMap ) );
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
