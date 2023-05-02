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

import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.cr.bean.ChallengeBean;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.util.internal.ChaiLogger;
import com.novell.ldapchai.util.internal.StringHelper;

import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Factory for generating {@code Challenge}s, {@code ChallengeSet}s and {@code ResponseSet}s.
 *
 * @author Jason D. Rivard
 */
public final class ChaiCrFactory
{
    /**
     * Constant used to indicate user supplied question.
     */
    public static final String USER_SUPPLIED_QUESTION = "%user%";

    private static final int MAX_HASH_THREAD_COUNT = 10;
    private static final Semaphore CONCURRENT_HASH_OPERATIONS = new Semaphore( 10 );

    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( ChaiCrFactory.class );

    private ChaiCrFactory()
    {
    }

    /**
     * Create a new ResponseSet.  The generated ResponseSet will be suitable for writing to the directory.
     *
     * @param challengeResponseMap  A map containing Challenges as the key, and string responses for values
     * @param locale                The locale the response set is stored in
     * @param minimumRandomRequired Minimum random responses required
     * @param chaiConfiguration     Appropriate configuration to use during this operation
     * @param csIdentifier          Identifier to store on generated ChaiResponseSet
     * @return A ResponseSet suitable for writing.
     * @throws com.novell.ldapchai.exception.ChaiValidationException when there is a logical problem with the response set data, such as more randoms required then exist
     */
    public static ChaiResponseSet newChaiResponseSet(
            final Map<Challenge, String> challengeResponseMap,
            final Locale locale,
            final int minimumRandomRequired,
            final ChaiConfiguration chaiConfiguration,
            final String csIdentifier
    )
            throws ChaiValidationException
    {
        return newChaiResponseSet( challengeResponseMap, Collections.emptyMap(), locale, minimumRandomRequired, chaiConfiguration, csIdentifier );
    }

    public static ChaiResponseSet newChaiResponseSet(
            final Map<Challenge, String> challengeResponseMap,
            final Map<Challenge, String> helpdeskChallengeResponseMap,
            final Locale locale,
            final int minimumRandomRequired,
            final ChaiConfiguration chaiConfiguration,
            final String csIdentifier
    )
            throws ChaiValidationException
    {
        final boolean caseInsensitive = chaiConfiguration.getBooleanSetting( ChaiSetting.CR_CASE_INSENSITIVE );
        validateAnswers( challengeResponseMap, chaiConfiguration );
        final Map<Challenge, Answer> answerMap = makeAnswerMap( challengeResponseMap, chaiConfiguration );
        final Map<Challenge, HelpdeskAnswer> helpdeskAnswerMap = makeHelpdeskAnswerMap( helpdeskChallengeResponseMap, chaiConfiguration );
        return new ChaiResponseSet( answerMap, helpdeskAnswerMap, locale, minimumRandomRequired, AbstractResponseSet.STATE.NEW, caseInsensitive, csIdentifier, Instant.now() );
    }

    public static ChaiResponseSet newChaiResponseSet(
            final Collection<ChallengeBean> challengeResponses,
            final Collection<ChallengeBean> helpdeskChallengeResponses,
            final Locale locale,
            final int minimumRandomRequired,
            final String csIdentifier
    )
            throws ChaiValidationException
    {
        final boolean caseInsensitive = challengeResponses != null
                && challengeResponses.size() > 1
                && challengeResponses.iterator().next().getAnswer().isCaseInsensitive();

        final ChaiConfiguration chaiConfiguration = ChaiConfiguration.builder()
                .setSetting( ChaiSetting.CR_CASE_INSENSITIVE, String.valueOf( caseInsensitive ) )
                .build();

        final Map<Challenge, String> crMap = challengeResponses == null
                ? Collections.emptyMap()
                : challengeResponses.stream().collect( Collectors.toMap(
                        ChaiChallenge::fromChallengeBean,
                        bean -> bean.getAnswer().getAnswerText() ) );

        final Map<Challenge, String> helpdeskCrMap = helpdeskChallengeResponses == null
                ? Collections.emptyMap()
                : helpdeskChallengeResponses.stream().collect( Collectors.toMap(
                        ChaiChallenge::fromChallengeBean,
                        bean -> bean.getAnswer().getAnswerText() ) );

        final Map<Challenge, Answer> tempCrMap = makeAnswerMap( crMap, chaiConfiguration );

        final Map<Challenge, HelpdeskAnswer> tempHelpdeskCrMap = makeHelpdeskAnswerMap( helpdeskCrMap, chaiConfiguration );

        return new ChaiResponseSet(
                tempCrMap,
                tempHelpdeskCrMap,
                locale,
                minimumRandomRequired,
                AbstractResponseSet.STATE.NEW,
                caseInsensitive,
                csIdentifier, Instant.now() );
    }

    public static void writeChaiResponseSet(
            final ChaiResponseSet chaiResponseSet,
            final Writer writer
    )
            throws ChaiOperationException
    {
        chaiResponseSet.write( writer );
    }

    public static boolean writeChaiResponseSet(
            final ChaiResponseSet chaiResponseSet,
            final ChaiUser chaiUser
    )
            throws ChaiUnavailableException, ChaiOperationException
    {
        return chaiResponseSet.write( chaiUser );
    }

    private static Map<Challenge, HelpdeskAnswer> makeHelpdeskAnswerMap(
            final Map<Challenge, String> crMap,
            final ChaiConfiguration chaiConfiguration
    )
    {
        final Map<Challenge, Answer> tempMap = makeAnswerMap( crMap, Answer.FormatType.HELPDESK, chaiConfiguration );
        final Map<Challenge, HelpdeskAnswer> returnMap = new LinkedHashMap<>();
        for ( final Map.Entry<Challenge, Answer> entry : tempMap.entrySet() )
        {
            returnMap.put( entry.getKey(), ( HelpdeskAnswer ) entry.getValue() );
        }
        return returnMap;
    }


    private static Map<Challenge, Answer> makeAnswerMap(
            final Map<Challenge, String> crMap,
            final ChaiConfiguration chaiConfiguration
    )
    {
        final Answer.FormatType formatType = Answer.FormatType.valueOf( chaiConfiguration.getSetting( ChaiSetting.CR_DEFAULT_FORMAT_TYPE ) );
        return makeAnswerMap( crMap, formatType, chaiConfiguration );
    }

    private static int figureThreadCount(
            final Map<Challenge, String> crMap,
            final ChaiConfiguration chaiConfiguration )
    {
        final int configuredThreadCount = Integer.parseInt( chaiConfiguration.getSetting( ChaiSetting.CR_CHAI_HASH_THREAD_COUNT ) );
        final int maxThreadCount = configuredThreadCount < 1 ? Runtime.getRuntime().availableProcessors() : configuredThreadCount;
        final int maxAppropriateToInput = Math.min( maxThreadCount, crMap.size() );
        final int effective = Math.min( MAX_HASH_THREAD_COUNT, maxAppropriateToInput );
        return effective > 0 ? effective : 1;
    }

    private static Map<Challenge, Answer> makeAnswerMap(
            final Map<Challenge, String> crMap,
            final Answer.FormatType formatType,
            final ChaiConfiguration chaiConfiguration
    )
    {
        if ( crMap == null || crMap.isEmpty() )
        {
            return Collections.emptyMap();
        }

        final Instant startTime = Instant.now();
        final int threadCount = figureThreadCount( crMap, chaiConfiguration );
        final List<Callable<Map<Challenge, Answer>>> callables = makeAnswerCallables( crMap, formatType, chaiConfiguration );

        Map<Challenge, Answer> answerMap = null;
        int effectiveThreadCount = 0;

        if ( threadCount > 1 && formatType.isHashThreadingEligible() )
        {
            if ( CONCURRENT_HASH_OPERATIONS.tryAcquire() )
            {
                try
                {
                    answerMap = concurrentAnswerMaker( threadCount, callables );
                    effectiveThreadCount = threadCount;
                }
                finally
                {
                    CONCURRENT_HASH_OPERATIONS.release();
                }
            }
            else
            {
                LOGGER.trace( () -> "max concurrent hash operations reached, skipping concurrent hash operation" );
            }
        }

        if ( answerMap == null )
        {
            answerMap = singleThreadAnswerMaker( callables );
            effectiveThreadCount = 1;
        }

        final int finalAnswerSize = answerMap.size();
        final int finalThreadCount = effectiveThreadCount;
        LOGGER.trace( () -> "generated " + formatType + " answer map with " + finalAnswerSize
                + " entries in " + Duration.between( Instant.now(), startTime ).toString()
                + " using " + finalThreadCount + " threads" );

        return answerMap;
    }

    private static Map<Challenge, Answer> singleThreadAnswerMaker(
            final List<Callable<Map<Challenge, Answer>>> callables
    )
    {
        final Map<Challenge, Answer> answerMap = new LinkedHashMap<>();
        for ( final Callable<Map<Challenge, Answer>> callable : callables )
        {
            try
            {
                answerMap.putAll( callable.call() );
            }
            catch ( Exception e )
            {
                final String errorMsg = "unexpected execution error during response set generation: " + e.getMessage();
                LOGGER.error( () -> errorMsg );
                throw new RuntimeException( errorMsg, e );
            }
        }
        return answerMap;
    }

    private static Map<Challenge, Answer> concurrentAnswerMaker(
            final int threadCount,
            final List<Callable<Map<Challenge, Answer>>> callables
    )
    {
        final Map<Challenge, Answer> answerMap = new LinkedHashMap<>();
        final ExecutorService threadPool = Executors.newFixedThreadPool( threadCount );
        try
        {
            final List<Future<Map<Challenge, Answer>>> futures = callables.stream()
                    .map( threadPool::submit )
                    .collect( Collectors.toList() );

            for ( final Future<Map<Challenge, Answer>> future : futures )
            {
                answerMap.putAll( future.get() );
            }
        }
        catch ( ExecutionException | InterruptedException e )
        {
            final String errorMsg = "unexpected execution error during response set generation: " + e.getMessage();
            LOGGER.error( () -> errorMsg );
            throw new RuntimeException( errorMsg, e );
        }
        finally
        {
            threadPool.shutdown();
        }

        return answerMap;
    }

    private static List<Callable<Map<Challenge, Answer>>> makeAnswerCallables(
            final Map<Challenge, String> crMap,
            final Answer.FormatType formatType,
            final ChaiConfiguration chaiConfiguration
    )
    {
        if ( crMap == null )
        {
            return Collections.emptyList();
        }

        final List<Callable<Map<Challenge, Answer>>> callableList = new ArrayList<>();

        for ( final Map.Entry<Challenge, String> entry : crMap.entrySet() )
        {
            final Challenge challenge = entry.getKey();
            final String answerText = entry.getValue();

            final int iterations;
            {
                final int configuredIterations = Integer.parseInt( chaiConfiguration.getSetting( ChaiSetting.CR_CHAI_ITERATIONS ) );
                iterations = configuredIterations > 0
                        ? configuredIterations
                        : formatType.getDefaultIterations();
            }

            final int saltCharCount;
            {
                final int configuredIterations = Integer.parseInt( chaiConfiguration.getSetting( ChaiSetting.CR_CHAI_SALT_CHAR_COUNT ) );
                saltCharCount = configuredIterations > 0
                        ? configuredIterations
                        : formatType.getSaltLength();
            }

            final AnswerConfiguration answerConfiguration = AnswerConfiguration.builder()
                    .caseInsensitive( chaiConfiguration.getBooleanSetting( ChaiSetting.CR_CASE_INSENSITIVE ) )
                    .iterations( iterations )
                    .saltCharCount( saltCharCount )
                    .formatType( formatType )
                    .challengeText( challenge.getChallengeText() )
                    .build();

            callableList.add( () -> Collections.singletonMap( challenge, AnswerFactory.newAnswer( answerConfiguration, answerText ) ) );
        }

        return callableList;
    }

    private static void validateAnswers( final Map<Challenge, String> crMap, final ChaiConfiguration chaiConfiguration )
            throws ChaiValidationException
    {
        final boolean allowDuplicates = chaiConfiguration.getBooleanSetting( ChaiSetting.CR_ALLOW_DUPLICATE_RESPONSES );
        for ( final Map.Entry<Challenge, String> entry : crMap.entrySet() )
        {
            final Challenge loopChallenge = entry.getKey();
            final String answerText = entry.getValue();

            if ( loopChallenge.getChallengeText() == null || loopChallenge.getChallengeText().length() < 1 )
            {
                throw new ChaiValidationException( "challenge text missing for challenge", ChaiError.CR_MISSING_REQUIRED_CHALLENGE_TEXT );
            }

            if ( answerText == null || answerText.length() < 1 )
            {
                final String errorString = "response text missing for challenge '" + loopChallenge.getChallengeText() + "'";
                throw new ChaiValidationException( errorString, ChaiError.CR_MISSING_REQUIRED_RESPONSE_TEXT, loopChallenge.getChallengeText() );
            }

            if ( answerText.length() < loopChallenge.getMinLength() )
            {
                final String errorString = "response text is too short for challenge '" + loopChallenge.getChallengeText() + "'";
                throw new ChaiValidationException( errorString, ChaiError.CR_RESPONSE_TOO_SHORT, loopChallenge.getChallengeText() );
            }

            if ( answerText.length() > loopChallenge.getMaxLength() )
            {
                final String errorString = "response text is too long for challenge '" + loopChallenge.getChallengeText() + "'";
                throw new ChaiValidationException( errorString, ChaiError.CR_RESPONSE_TOO_LONG, loopChallenge.getChallengeText() );
            }

            if ( loopChallenge.getMaxQuestionCharsInAnswer() > 0 )
            {
                checkIfAnswerTextContainsChallengeText( loopChallenge, answerText );

            }
        }

        if ( !allowDuplicates )
        {
            checkForDuplicateAnswerValues( crMap );
        }
    }

    private static void checkForDuplicateAnswerValues( final Map<Challenge, String> crMap )
            throws ChaiValidationException
    {
        final Set<String> seenResponses = new HashSet<>();

        for ( final Map.Entry<Challenge, String> entry : crMap.entrySet() )
        {
            final Challenge loopChallenge  = entry.getKey();
            final String responseText = entry.getValue();
            if ( !StringHelper.isEmpty( responseText ) )
            {
                final String lowercaseResponseText = responseText.toLowerCase();
                if ( seenResponses.contains( lowercaseResponseText ) )
                {
                    throw new ChaiValidationException(
                            "multiple responses have the same value",
                            ChaiError.CR_DUPLICATE_RESPONSES,
                            loopChallenge.getChallengeText() );
                }
                seenResponses.add( lowercaseResponseText );
            }
        }
    }

    private static void checkIfAnswerTextContainsChallengeText(
            final Challenge loopChallenge,
            final String answerText
    )
            throws ChaiValidationException
    {
        if ( loopChallenge == null || StringHelper.isEmpty( answerText ) )
        {
            return;
        }

        final String rawChallengeText = loopChallenge.getChallengeText();
        if ( rawChallengeText != null )
        {
            final String[] challengeWords = rawChallengeText.toLowerCase().split( "\\s" );
            final String responseTextLower = answerText.toLowerCase();
            final int maxChallengeLengthInResponse = loopChallenge.getMaxQuestionCharsInAnswer();

            for ( final String challengeWord : challengeWords )
            {
                if ( challengeWord.length() > maxChallengeLengthInResponse )
                {
                    for ( int i = 0; i <= challengeWord.length() - ( maxChallengeLengthInResponse + 1 ); i++ )
                    {
                        final String wordPart = challengeWord.substring( i, i + ( maxChallengeLengthInResponse + 1 ) );
                        if ( responseTextLower.contains( wordPart ) )
                        {
                            final String errorString = "response text contains too many challenge characters for challenge '"
                                    + loopChallenge.getChallengeText() + "'";
                            throw new ChaiValidationException(
                                    errorString,
                                    ChaiError.CR_TOO_MANY_QUESTION_CHARS,
                                    loopChallenge.getChallengeText() );
                        }
                    }
                }
            }
        }
    }

    /**
     * Read the user's configured ResponseSet from the directory.
     * A caller would typically use the returned {@code ResponseSet} for testing responses by calling
     * {@link ResponseSet#test(Map)}.
     *
     * @param user ChaiUser to read responses for
     * @return A valid ResponseSet if found, otherwise null.
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws ChaiOperationException   If an error is encountered during the operation
     * @throws ChaiValidationException when there is a logical problem with the response set data, such as more randoms required then exist
     */
    public static ChaiResponseSet readChaiResponseSet( final ChaiUser user )
            throws ChaiUnavailableException, ChaiValidationException, ChaiOperationException
    {
        return ChaiResponseSet.readUserResponseSet( user );
    }

    public static ResponseSet parseChaiResponseSetXML( final String inputXmlString )
            throws ChaiValidationException, ChaiOperationException
    {
        return ChaiResponseSet.ChaiResponseXmlParser.parseChaiResponseSetXML( inputXmlString );
    }
}
