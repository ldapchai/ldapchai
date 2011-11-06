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

import java.io.Serializable;
import java.util.*;

public class ChaiChallengeSet implements ChallengeSet, Serializable {
// ------------------------------ FIELDS ------------------------------

    private List<Challenge> challenges;
    private int minRandomRequired;
    private Locale locale;
    private String identifier;

// --------------------------- CONSTRUCTORS ---------------------------

    public ChaiChallengeSet(
            final Collection<Challenge> challenges,
            final int minRandomRequired,
            final Locale locale,
            final String identifer
    )
            throws ChaiValidationException
    {
        this.challenges = Collections.unmodifiableList(new LinkedList<Challenge>(challenges));
        this.minRandomRequired = minRandomRequired > getRandomChallenges().size() ? getRandomChallenges().size() : minRandomRequired;
        this.locale = locale == null ? Locale.getDefault() : locale;
        this.identifier = identifer;
        this.isValid();
    }

    private void isValid()
            throws ChaiValidationException
    {
        if (this.minRandomRequired > this.getRandomChallenges().size()) {
            throw new ChaiValidationException("number of required responses greater then count of supplied random challenges", ChaiError.CR_NOT_ENOUGH_RANDOM_RESPONSES);
        }

        if (this.minRandomRequired + this.getRequiredChallenges().size() < 1) {
            throw new ChaiValidationException("too few challenges are required", ChaiError.CR_TOO_FEW_CHALLENGES);
        }
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public final List<Challenge> getChallenges()
    {
        return challenges;
    }

    public final Locale getLocale()
    {
        return locale;
    }

    public final int getMinRandomRequired()
    {
        return minRandomRequired;
    }

// ------------------------ CANONICAL METHODS ------------------------

    public final String toString()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append("ChallengeSet ");
        sb.append("identifier: ").append(this.identifier == null ? "[null]" : this.identifier);
        sb.append(", minRandom: ").append(this.minRandomRequired);
        sb.append(", locale: ").append(this.locale);
        sb.append(", ");

        for (final Challenge loopC : this.getChallenges()) {
            sb.append("(");
            sb.append(loopC.toString());
            sb.append(") ");
        }

        return sb.toString();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ChallengeSet ---------------------

    public final List<Challenge> getAdminDefinedChallenges()
    {
        final List<Challenge> tempList = new ArrayList<Challenge>();
        for (final Challenge loopChallenge : challenges)
            if (loopChallenge.isAdminDefined())
                tempList.add(loopChallenge);
        return Collections.unmodifiableList(tempList);
    }

    public List<String> getChallengeTexts()
    {
        final List<String> tempList = new ArrayList<String>();
        for (final Challenge loopChallenge : challenges)
            if (loopChallenge.getChallengeText() != null && loopChallenge.getChallengeText().length() > 0)
                tempList.add(loopChallenge.getChallengeText());
        return Collections.unmodifiableList(tempList);
    }

    public final List<Challenge> getRandomChallenges()
    {
        final List<Challenge> tempList = new ArrayList<Challenge>();
        for (final Challenge loopChallenge : challenges)
            if (!loopChallenge.isRequired())
                tempList.add(loopChallenge);
        return Collections.unmodifiableList(tempList);
    }

    public final List<Challenge> getRequiredChallenges()
    {
        final List<Challenge> tempList = new ArrayList<Challenge>();
        for (final Challenge loopChallenge : challenges)
            if (loopChallenge.isRequired())
                tempList.add(loopChallenge);
        return Collections.unmodifiableList(tempList);
    }

    public final List<Challenge> getUserDefinedChallenges()
    {
        final List<Challenge> tempList = new ArrayList<Challenge>();
        for (final Challenge loopChallenge : challenges)
            if (!loopChallenge.isAdminDefined())
                tempList.add(loopChallenge);
        return Collections.unmodifiableList(tempList);
    }

    public int minimumResponses()
    {
        int mininimumResponses = 0;

        mininimumResponses += getRequiredChallenges().size();
        mininimumResponses += getMinRandomRequired();

        return mininimumResponses;
    }

    public boolean isLocked()
    {
        for (final Challenge loopChallenge : challenges)
            if (!loopChallenge.isLocked())
                return false;

        return true;
    }

    public void lock()
    {
        for (final Challenge loopChallenge : challenges)
            loopChallenge.lock();
    }

    public String getIdentifier() {
        return identifier;
    }
}
