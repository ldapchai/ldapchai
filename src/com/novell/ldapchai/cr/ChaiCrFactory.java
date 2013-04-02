/*
 * LDAP Chai API
 * Copyright (c) 2006-2010 Novell, Inc.
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
import com.novell.ldapchai.cr.bean.AnswerBean;
import com.novell.ldapchai.cr.bean.ChallengeBean;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.util.ChaiLogger;

import java.util.*;

/**
 * Factory for generating {@code Challenge}s, {@code ChallengeSet}s and {@code ResponseSet}s.
 *
 * @author Jason D. Rivard
 */
public final class ChaiCrFactory {
    // ----------------------------- CONSTANTS ----------------------------

    /**
     * Constant used to indicate user supplied question
     */
    public static final String USER_SUPPLIED_QUESTION = "%user%";

// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(ChaiCrFactory.class);

// -------------------------- STATIC METHODS --------------------------

    /**
     * Create a new ResponseSet.  The generated ResponseSet will be suitable for writing to the directory.
     *
     * @param challengeResponseMap  A map containing Challenges as the key, and string responses for values
     * @param locale                The locale the response set is stored in
     * @param minimumRandomRequired Minimum random responses required
     * @return A ResponseSet suitable for writing.
     * @throws com.novell.ldapchai.exception.ChaiValidationException
     *          when there is a logical problem with the response set data, such as more randoms required then exist
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
        return newChaiResponseSet(challengeResponseMap, Collections.<Challenge,String>emptyMap(), locale, minimumRandomRequired, chaiConfiguration, csIdentifier);
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
        final boolean caseInsensitive = chaiConfiguration.getBooleanSetting(ChaiSetting.CR_CASE_INSENSITIVE);
        validateAnswers(challengeResponseMap,chaiConfiguration);
        final Map<Challenge,Answer> answerMap = makeAnswerMap(challengeResponseMap, chaiConfiguration);
        final Map<Challenge,HelpdeskAnswer> helpdeskAnswerMap = makeHelpdeskAnswerMap(helpdeskChallengeResponseMap, chaiConfiguration);
        return new ChaiResponseSet(answerMap, helpdeskAnswerMap, locale, minimumRandomRequired, AbstractResponseSet.STATE.NEW, caseInsensitive, csIdentifier, new Date());
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
        final Map<Challenge, Answer> tempCrMap = new LinkedHashMap<Challenge, Answer>();
        final boolean caseInsensitive = challengeResponses.isEmpty() ? false : challengeResponses.iterator().next().getAnswer().isCaseInsensitive();
        for (final ChallengeBean challengeBean : challengeResponses) {
            final AnswerBean answerBean = challengeBean.getAnswer();
            final Challenge challenge = ChaiChallenge.fromChallengeBean(challengeBean);
            tempCrMap.put(challenge,answerBeanToAnswer(answerBean,challenge));
            if (answerBean.isCaseInsensitive() != caseInsensitive) {
                throw new IllegalArgumentException("all answers must have the same caseInsensitive value");
            }
        }
        final Map<Challenge, HelpdeskAnswer> tempHelpdeskCrMap = new LinkedHashMap<Challenge, HelpdeskAnswer>();
        for (final ChallengeBean challengeBean : helpdeskChallengeResponses) {
            final AnswerBean answerBean = challengeBean.getAnswer();
            final Challenge challenge = ChaiChallenge.fromChallengeBean(challengeBean);
            tempHelpdeskCrMap.put(challenge,(HelpdeskAnswer)answerBeanToAnswer(answerBean,challenge));
        }

        return new ChaiResponseSet(tempCrMap, tempHelpdeskCrMap, locale, minimumRandomRequired, AbstractResponseSet.STATE.NEW, caseInsensitive, csIdentifier, new Date());
    }

    public static boolean writeChaiResponseSet(
            final ChaiResponseSet chaiResponseSet,
            final ChaiUser chaiUser
    )
            throws ChaiUnavailableException, ChaiOperationException
    {
        return chaiResponseSet.write(chaiUser);
    }

    private static Map<Challenge,HelpdeskAnswer> makeHelpdeskAnswerMap(
            final Map<Challenge, String> crMap,
            final ChaiConfiguration chaiConfiguration
    )
    {
        final Map<Challenge,Answer> tempMap = makeAnswerMap(crMap, Answer.FormatType.HELPDESK, chaiConfiguration);
        final Map<Challenge,HelpdeskAnswer> returnMap = new LinkedHashMap<Challenge, HelpdeskAnswer>();
        for (final Challenge challenge : tempMap.keySet()) {
            returnMap.put(challenge, (HelpdeskAnswer)tempMap.get(challenge));
        }
        return returnMap;
    }


    private static Map<Challenge,Answer> makeAnswerMap(
            final Map<Challenge, String> crMap,
            final ChaiConfiguration chaiConfiguration
    )
    {
        final Answer.FormatType formatType = Answer.FormatType.valueOf(chaiConfiguration.getSetting(ChaiSetting.CR_DEFAULT_FORMAT_TYPE));
        return makeAnswerMap(crMap, formatType, chaiConfiguration);
    }

    private static Map<Challenge,Answer> makeAnswerMap(
            final Map<Challenge, String> crMap,
            final Answer.FormatType formatType,
            final ChaiConfiguration chaiConfiguration
    )
    {
        final boolean caseInsensitive = chaiConfiguration.getBooleanSetting(ChaiSetting.CR_CASE_INSENSITIVE);
        final Map<Challenge,Answer> answerMap = new LinkedHashMap<Challenge, Answer>();
        for (final Challenge challenge : crMap.keySet()) {
            final String answerText = crMap.get(challenge);
            final Answer answer;
            switch (formatType) {
                case SHA1_SALT:
                case SHA1:
                    final int saltCount = Integer.parseInt(chaiConfiguration.getSetting(ChaiSetting.CR_CHAI_SALT_COUNT));
                    answer = Sha1SaltAnswer.newAnswer(answerText, saltCount, caseInsensitive);
                    break;
                case HELPDESK:
                    answer = ChaiHelpdeskAnswer.newAnswer(answerText, challenge.getChallengeText());
                    break;
                case BCRYPT:
                    answer = BCryptAnswer.newAnswer(answerText, caseInsensitive);
                    break;
                default:
                    answer = TextAnswer.newAnswer(answerText, caseInsensitive);
            }
            answerMap.put(challenge,answer);
        }
        return answerMap;
    }

    private static void validateAnswers(final Map<Challenge, String> crMap, final ChaiConfiguration chaiConfiguration)
            throws ChaiValidationException
    {
        final boolean allowDuplicates = chaiConfiguration.getBooleanSetting(ChaiSetting.CR_ALLOW_DUPLICATE_RESPONSES);
        for (final Challenge loopChallenge : crMap.keySet()) {
            final String answerText = crMap.get(loopChallenge);

            if (loopChallenge.getChallengeText() == null || loopChallenge.getChallengeText().length() < 1) {
                throw new ChaiValidationException("challenge text missing for challenge", ChaiError.CR_MISSING_REQUIRED_CHALLENGE_TEXT);
            }

            if (answerText == null || answerText.length() < 1) {
                final String errorString = "response text missing for challenge '" + loopChallenge.getChallengeText() + "'";
                throw new ChaiValidationException(errorString, ChaiError.CR_MISSING_REQUIRED_RESPONSE_TEXT,loopChallenge.getChallengeText());
            }

            if (answerText.length() < loopChallenge.getMinLength()) {
                final String errorString = "response text is too short for challenge '" + loopChallenge.getChallengeText() + "'";
                throw new ChaiValidationException(errorString, ChaiError.CR_RESPONSE_TOO_SHORT,loopChallenge.getChallengeText());
            }

            if (answerText.length() > loopChallenge.getMaxLength()) {
                final String errorString = "response text is too long for challenge '" + loopChallenge.getChallengeText() + "'";
                throw new ChaiValidationException(errorString, ChaiError.CR_RESPONSE_TOO_LONG,loopChallenge.getChallengeText());
            }
        }

        if (!allowDuplicates) {
            final Set<String> seenResponses = new HashSet<String>();
            for (final Challenge loopChallenge : crMap.keySet()) {
                final String responseText = crMap.get(loopChallenge);
                if (responseText != null && responseText.length() > 1) {
                    if (seenResponses.contains(responseText.toLowerCase())) {
                        throw new ChaiValidationException("multiple responses have the same value", ChaiError.CR_DUPLICATE_RESPONSES, loopChallenge.getChallengeText());
                    }
                    seenResponses.add(responseText);
                }
            }
        }
    }




// --------------------------- CONSTRUCTORS ---------------------------

    private ChaiCrFactory()
    {
    }

    /**
     * Read the user's configured ResponseSet from the directory.
     * <p/>
     * A caller would typically use the returned {@code ResponseSet} for testing responses by calling the ResponseSet's
     * {@link ResponseSet#test(java.util.Map<com.novell.ldapchai.cr.Challenge,java.lang.String>)} method.
     *
     * @param user         ChaiUser to read responses for
     * @return A valid ResponseSet if found, otherwise null.
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws com.novell.ldapchai.exception.ChaiValidationException
     *                                  when there is a logical problem with the response set data, such as more randoms required then exist
     */
    public static ChaiResponseSet readChaiResponseSet(final ChaiUser user)
            throws ChaiUnavailableException, ChaiValidationException, ChaiOperationException
    {
        return ChaiResponseSet.readUserResponseSet(user);
    }

    public static ResponseSet parseChaiResponseSetXML(final String inputXmlString)
            throws ChaiValidationException, ChaiOperationException
    {
        return ChaiResponseSet.ChaiResponseXmlParser.parseChaiResponseSetXML(inputXmlString);
    }

    public static Answer answerBeanToAnswer(final AnswerBean input, final Challenge challenge) {
        if (input == null) {
            throw new IllegalStateException("input cannot be null");
        }
        if (input.type == null) {
            throw new IllegalStateException("format type cannot be null");
        }
        switch (input.type) {
            case BCRYPT:
                return new BCryptAnswer(input.getAnswerHash(),input.isCaseInsensitive());

            case SHA1:
            case SHA1_SALT:
                return new Sha1SaltAnswer(input.getAnswerHash(),input.getSalt(),input.getHashCount(),input.isCaseInsensitive());

            case TEXT:
                return new TextAnswer(input.getAnswerText(), input.isCaseInsensitive());

            case HELPDESK:
                if (challenge == null) {
                    throw new IllegalArgumentException("challenge cannot be null to convert helpdesk answers");
                }
                return new ChaiHelpdeskAnswer(input.getAnswerText(), challenge.getChallengeText());

            default:
                throw new UnsupportedOperationException("unknown type format for answer");
        }
    }
}