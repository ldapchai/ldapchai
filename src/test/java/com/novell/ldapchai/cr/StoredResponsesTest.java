package com.novell.ldapchai.cr;

import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StoredResponsesTest
{
    @Test
    public void testStoredResponseSet1()
            throws IOException, ChaiValidationException, ChaiOperationException, ChaiUnavailableException
    {
        final String responseAsString = readResourceFile( "ResponseSet1.xml" );
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
    public void testStoredResponseSet2()
            throws IOException, ChaiValidationException, ChaiOperationException, ChaiUnavailableException
    {
        final String responseAsString = readResourceFile( "ResponseSet2.xml" );
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

    private static String readResourceFile( final String name )
            throws IOException
    {
        final InputStream inputStream = ChaiCrFactoryTest.class.getResourceAsStream( name );
        final StringBuilder textBuilder = new StringBuilder();
        try ( Reader reader = new BufferedReader( new InputStreamReader( inputStream, Charset.forName( StandardCharsets.UTF_8.name() ) ) ) )
        {
            int c;
            while ( ( c = reader.read() ) != -1)
            {
                textBuilder.append( (char) c );
            }
        }
        return textBuilder.toString();
    }
}
