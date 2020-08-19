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

import com.novell.ldapchai.cr.bean.ChallengeBean;
import com.novell.ldapchai.cr.bean.ChallengeSetBean;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiValidationException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


public class ChaiChallengeSet implements ChallengeSet, Serializable
{
    private final List<Challenge> challenges;
    private final int minRandomRequired;
    private final Locale locale;
    private final String identifier;

    public ChaiChallengeSet(
            final Collection<Challenge> challenges,
            final int minRandomRequired,
            final Locale locale,
            final String identifer
    )
            throws ChaiValidationException
    {
        this.challenges = Collections.unmodifiableList( new LinkedList<>( challenges ) );
        this.minRandomRequired = minRandomRequired > getRandomChallenges().size() ? getRandomChallenges().size() : minRandomRequired;
        this.locale = locale == null ? Locale.getDefault() : locale;
        this.identifier = identifer;
        this.isValid();
    }

    private void isValid()
            throws ChaiValidationException
    {
        if ( this.minRandomRequired > this.getRandomChallenges().size() )
        {
            throw new ChaiValidationException( "number of required responses greater then count of supplied random challenges", ChaiError.CR_NOT_ENOUGH_RANDOM_RESPONSES );
        }

        if ( this.minRandomRequired + this.getRequiredChallenges().size() < 1 )
        {
            throw new ChaiValidationException( "too few challenges are required", ChaiError.CR_TOO_FEW_CHALLENGES );
        }
    }

    @Override
    public final List<Challenge> getChallenges()
    {
        return challenges;
    }

    @Override
    public final Locale getLocale()
    {
        return locale;
    }

    @Override
    public final int getMinRandomRequired()
    {
        return minRandomRequired;
    }

    public final String toString()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append( "ChallengeSet " );
        sb.append( "identifier: " ).append( this.identifier == null ? "[null]" : this.identifier );
        sb.append( ", minRandom: " ).append( this.minRandomRequired );
        sb.append( ", locale: " ).append( this.locale );
        sb.append( ", " );

        for ( final Challenge loopC : this.getChallenges() )
        {
            sb.append( "(" );
            sb.append( loopC.toString() );
            sb.append( ") " );
        }

        return sb.toString();
    }

    @Override
    public final List<Challenge> getAdminDefinedChallenges()
    {
        final List<Challenge> tempList = new ArrayList<>();
        for ( final Challenge loopChallenge : challenges )
        {
            if ( loopChallenge.isAdminDefined() )
            {
                tempList.add( loopChallenge );
            }
        }
        return Collections.unmodifiableList( tempList );
    }

    @Override
    public List<String> getChallengeTexts()
    {
        final List<String> tempList = new ArrayList<>();
        for ( final Challenge loopChallenge : challenges )
        {
            if ( loopChallenge.getChallengeText() != null && loopChallenge.getChallengeText().length() > 0 )
            {
                tempList.add( loopChallenge.getChallengeText() );
            }
        }
        return Collections.unmodifiableList( tempList );
    }

    @Override
    public final List<Challenge> getRandomChallenges()
    {
        final List<Challenge> tempList = new ArrayList<>();
        for ( final Challenge loopChallenge : challenges )
        {
            if ( !loopChallenge.isRequired() )
            {
                tempList.add( loopChallenge );
            }
        }
        return Collections.unmodifiableList( tempList );
    }

    @Override
    public final List<Challenge> getRequiredChallenges()
    {
        final List<Challenge> tempList = new ArrayList<>();
        for ( final Challenge loopChallenge : challenges )
        {
            if ( loopChallenge.isRequired() )
            {
                tempList.add( loopChallenge );
            }
        }
        return Collections.unmodifiableList( tempList );
    }

    @Override
    public final List<Challenge> getUserDefinedChallenges()
    {
        final List<Challenge> tempList = new ArrayList<>();
        for ( final Challenge loopChallenge : challenges )
        {
            if ( !loopChallenge.isAdminDefined() )
            {
                tempList.add( loopChallenge );
            }
        }
        return Collections.unmodifiableList( tempList );
    }

    @Override
    public int minimumResponses()
    {
        int mininimumResponses = 0;

        mininimumResponses += getRequiredChallenges().size();
        mininimumResponses += getMinRandomRequired();

        return mininimumResponses;
    }

    @Override
    public boolean isLocked()
    {
        for ( final Challenge loopChallenge : challenges )
        {
            if ( !loopChallenge.isLocked() )
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public void lock()
    {
        for ( final Challenge loopChallenge : challenges )
        {
            loopChallenge.lock();
        }
    }

    @Override
    public String getIdentifier()
    {
        return identifier;
    }

    @Override
    public ChallengeSetBean asChallengeSetBean()
    {
        final ChallengeSetBean challengeSetBean = new ChallengeSetBean();
        challengeSetBean.setIdentifier( this.getIdentifier() );
        challengeSetBean.setLocale(  this.getLocale() );
        challengeSetBean.setMinRandomRequired( this.getMinRandomRequired() );

        final List<ChallengeBean> challengeBeans = this.getChallenges().stream()
                .map( Challenge::asChallengeBean )
                .collect( Collectors.toList() );
        challengeSetBean.setChallenges( challengeBeans );
        return challengeSetBean;
    }
}
