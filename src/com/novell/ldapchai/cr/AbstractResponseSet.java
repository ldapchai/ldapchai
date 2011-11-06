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

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.util.ChaiLogger;

import java.security.SecureRandom;
import java.util.*;

public abstract class AbstractResponseSet implements ResponseSet {

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(ResponseSet.class.getName());
// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    public enum STATE {
        NEW(true),
        WRITTEN(true),
        READ(false);

        private boolean readableResponses;

        STATE(final boolean readableResponses)
        {
            this.readableResponses = readableResponses;
        }

        public boolean isReadableResponses()
        {
            return readableResponses;
        }
    }

// ------------------------------ FIELDS ------------------------------

    protected Map<Challenge, String> crMap = Collections.emptyMap();
    protected ChallengeSet allChallengeSet;
    protected ChallengeSet presentableChallengeSet;
    protected Locale locale;
    protected int minimumRandomRequired;
    protected Date timestamp;
    protected String csIdentifier;

    protected STATE state;

// --------------------------- CONSTRUCTORS ---------------------------

    protected AbstractResponseSet(
            final Map<Challenge, String> crMap,
            final Locale locale,
            final int minimumRandomRequired,
            final STATE state,
            final String csIdentifier
    )
            throws ChaiValidationException
    {
        this.state = state;
        this.locale = locale;
        this.minimumRandomRequired = minimumRandomRequired;
        this.crMap = crMap;
        this.csIdentifier = csIdentifier;

        this.timestamp = new Date();

        this.isValid();

        allChallengeSet = new ChaiChallengeSet(crMap.keySet(), minimumRandomRequired, locale, csIdentifier);
        presentableChallengeSet = reduceCsToMinRandoms(allChallengeSet);

        if (state == STATE.READ) {
            this.allChallengeSet.lock();
            this.presentableChallengeSet.lock();
        }

    }

    protected void isValid()
            throws ChaiValidationException
    {
        isValid(false);
    }

    protected void isValid(boolean allowDuplicates)
            throws ChaiValidationException
    {
        for (final Challenge loopChallenge : crMap.keySet()) {
            final String responseText = crMap.get(loopChallenge);

            if (loopChallenge.getChallengeText() == null || loopChallenge.getChallengeText().length() < 1) {
                throw new ChaiValidationException("challenge text missing for challenge", ChaiError.CR_MISSING_REQUIRED_CHALLENGE_TEXT);
            }

            if (state.isReadableResponses()) {
                if (responseText == null || responseText.length() < 1) {
                    final String errorString = "response text missing for challenge '" + loopChallenge.getChallengeText() + "'";
                    throw new ChaiValidationException(errorString, ChaiError.CR_MISSING_REQUIRED_RESPONSE_TEXT,loopChallenge.getChallengeText());
                }

                if (responseText.length() < loopChallenge.getMinLength()) {
                    final String errorString = "response text is too short for challenge '" + loopChallenge.getChallengeText() + "'";
                    throw new ChaiValidationException(errorString, ChaiError.CR_RESPONSE_TOO_SHORT,loopChallenge.getChallengeText());
                }

                if (responseText.length() > loopChallenge.getMaxLength()) {
                    final String errorString = "response text is too long for challenge '" + loopChallenge.getChallengeText() + "'";
                    throw new ChaiValidationException(errorString, ChaiError.CR_RESPONSE_TOO_LONG,loopChallenge.getChallengeText());
                }
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

// ------------------------ CANONICAL METHODS ------------------------

    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append(": state(").append(state).append(") ");
        sb.append("ChallengeSet: (");
        try {
            sb.append(this.getChallengeSet().toString());
        } catch (Exception e) {
            sb.append("[error]");
        }
        sb.append(")");
        return sb.toString();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ResponseSet ---------------------

    public final ChallengeSet getChallengeSet() throws ChaiValidationException
    {
        return allChallengeSet;
    }

    public boolean meetsChallengeSetRequirements(final ChallengeSet challengeSet)
            throws ChaiValidationException
    {
        for (final Challenge loopChallenge : challengeSet.getChallenges()) {
            if (loopChallenge.isRequired() && loopChallenge.isAdminDefined()) {
                final String loopChallengeText = loopChallenge.getChallengeText();
                final List<String> challengeTexts = this.getChallengeSet().getChallengeTexts();

                if (!challengeTexts.contains(loopChallengeText)) {
                    throw new ChaiValidationException("multiple responses have the same value", ChaiError.CR_MISSING_REQUIRED_RESPONSE_TEXT, loopChallengeText);
                }
            }
        }

        if (this.getChallengeSet().getRequiredChallenges().size() < challengeSet.getRequiredChallenges().size()) {
            throw new ChaiValidationException("too few challenges are required", ChaiError.CR_TOO_FEW_CHALLENGES);
        }

        if (this.getChallengeSet().getRandomChallenges().size() < challengeSet.getMinRandomRequired()) {
            final StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("minimum number of ramdom responses in response set (").append(this.getChallengeSet().getRandomChallenges().size()).append(")");
            errorMsg.append(" is less than minimum number of random responses required in challenge set (").append(challengeSet.getMinRandomRequired()).append(")");
            throw new ChaiValidationException(errorMsg.toString(), ChaiError.CR_TOO_FEW_RANDOM_RESPONSES);
        }

        return true;
    }

    public Locale getLocale()
    {
        return locale;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public ChallengeSet getPresentableChallengeSet() throws ChaiValidationException {
        return presentableChallengeSet;
    }

    // -------------------------- OTHER METHODS --------------------------

    private static ChallengeSet reduceCsToMinRandoms(final ChallengeSet allChallengeSet)
            throws ChaiValidationException
    {
        if (allChallengeSet.getRandomChallenges().size() <= allChallengeSet.getMinRandomRequired()) {
            return allChallengeSet;
        }

        final SecureRandom random = new SecureRandom();
        final List<Challenge> newChallenges = new ArrayList<Challenge>();
        final List<Challenge> allRandoms = new ArrayList<Challenge>(allChallengeSet.getRandomChallenges());
        while (newChallenges.size() < allChallengeSet.getMinRandomRequired()) {
            newChallenges.add(allRandoms.remove(random.nextInt(allRandoms.size())));
        }
        newChallenges.addAll(allChallengeSet.getRequiredChallenges());
        return new ChaiChallengeSet(
                newChallenges,
                allChallengeSet.getMinRandomRequired(),
                allChallengeSet.getLocale(),
                allChallengeSet.getIdentifier()
                );
    }
}
