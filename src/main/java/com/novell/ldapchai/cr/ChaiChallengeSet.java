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
import java.util.Objects;
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
            final String identifier
    )
            throws ChaiValidationException
    {
        this.challenges = Collections.unmodifiableList( new LinkedList<>( challenges ) );
        this.minRandomRequired = Math.min( minRandomRequired, getRandomChallenges().size() );
        this.locale = locale == null ? Locale.getDefault() : locale;
        this.identifier = identifier;
        this.isValid();
    }

    private void isValid()
            throws ChaiValidationException
    {
        if ( this.minRandomRequired > this.getRandomChallenges().size() )
        {
            throw new ChaiValidationException( "number of required responses greater then count of supplied random challenges", ChaiError.CR_NOT_ENOUGH_RANDOM_RESPONSES );
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
        int minimumResponses = 0;

        minimumResponses += getRequiredChallenges().size();
        minimumResponses += getMinRandomRequired();

        return minimumResponses;
    }

    @Override
    public String getIdentifier()
    {
        return identifier;
    }

    @Override
    public ChallengeSetBean asChallengeSetBean()
    {
        final List<ChallengeBean> challengeBeans = this.getChallenges().stream()
                .map( Challenge::asChallengeBean )
                .collect( Collectors.toList() );

        return new ChallengeSetBean(
                challengeBeans,
                minRandomRequired,
                locale,
                identifier );
    }

    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        final ChaiChallengeSet other = ( ChaiChallengeSet ) o;
        return Objects.equals( challenges, other.challenges )
                && Objects.equals( minRandomRequired, other.minRandomRequired )
                && Objects.equals( locale, other.locale )
                && Objects.equals( identifier, other.identifier );
    }

    public int hashCode()
    {
        return Objects.hash(
                challenges,
                minRandomRequired,
                locale,
                identifier );
    }

}
