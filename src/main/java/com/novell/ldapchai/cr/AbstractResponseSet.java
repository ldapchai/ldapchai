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

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.util.internal.ChaiLogger;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractResponseSet implements ResponseSet
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( AbstractResponseSet.class );

    public enum STATE
    {
        NEW( true ),
        WRITTEN( true ),
        READ( false );

        private final boolean readableResponses;

        STATE( final boolean readableResponses )
        {
            this.readableResponses = readableResponses;
        }

        public boolean isReadableResponses()
        {
            return readableResponses;
        }
    }

    protected final Map<Challenge, Answer> crMap;
    protected final ChallengeSet allChallengeSet;
    protected final ChallengeSet presentableChallengeSet;
    protected final Locale locale;
    protected final int minimumRandomRequired;
    protected final Instant timestamp;
    protected final String csIdentifier;
    protected final Map<Challenge, HelpdeskAnswer> helpdeskCrMap;

    protected STATE state;

    protected AbstractResponseSet(
            final Map<Challenge, Answer> crMap,
            final Map<Challenge, HelpdeskAnswer> helpdeskCrMap,
            final Locale locale,
            final int minimumRandomRequired,
            final STATE state,
            final String csIdentifier,
            final Instant timestamp
    )
            throws ChaiValidationException
    {
        this.state = state;
        this.locale = locale;
        this.minimumRandomRequired = minimumRandomRequired;
        this.crMap = crMap == null ? Collections.emptyMap() : Collections.unmodifiableMap( new HashMap<>( crMap ) );
        this.helpdeskCrMap = helpdeskCrMap == null ? Collections.emptyMap() : Collections.unmodifiableMap( new HashMap<>( helpdeskCrMap ) );
        this.csIdentifier = csIdentifier;

        this.timestamp = timestamp;

        allChallengeSet = new ChaiChallengeSet( this.crMap.keySet(), minimumRandomRequired, locale, csIdentifier );
        presentableChallengeSet = reduceCsToMinRandoms( allChallengeSet );
    }


    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( this.getClass().getSimpleName() );
        sb.append( ": state(" ).append( state ).append( ") " );
        sb.append( "ChallengeSet: (" );
        try
        {
            sb.append( this.getChallengeSet().toString() );
        }
        catch ( Exception e )
        {
            sb.append( "[error]" );
        }
        sb.append( ")" );
        return sb.toString();
    }


    @Override
    public final ChallengeSet getChallengeSet()
            throws ChaiValidationException
    {
        return allChallengeSet;
    }

    @Override
    public boolean meetsChallengeSetRequirements( final ChallengeSet challengeSet )
            throws ChaiValidationException
    {
        for ( final Challenge loopChallenge : challengeSet.getChallenges() )
        {
            if ( loopChallenge.isRequired() && loopChallenge.isAdminDefined() )
            {
                final String loopChallengeText = loopChallenge.getChallengeText();
                final List<String> challengeTexts = this.getChallengeSet().getChallengeTexts();

                if ( !challengeTexts.contains( loopChallengeText ) )
                {
                    throw new ChaiValidationException( "multiple challenges have the same value", ChaiError.CR_MISSING_REQUIRED_RESPONSE_TEXT, loopChallengeText );
                }
            }
        }

        if ( this.getChallengeSet().getRequiredChallenges().size() < challengeSet.getRequiredChallenges().size() )
        {
            throw new ChaiValidationException( "too few challenges are required", ChaiError.CR_TOO_FEW_CHALLENGES );
        }

        if ( this.getChallengeSet().getRandomChallenges().size() < challengeSet.getMinRandomRequired() )
        {
            final StringBuilder errorMsg = new StringBuilder();
            errorMsg.append( "minimum number of random responses in response set (" ).append( this.getChallengeSet().getRandomChallenges().size() ).append( ")" );
            errorMsg.append( " is less than minimum number of random responses required in challenge set (" ).append( challengeSet.getMinRandomRequired() ).append( ")" );
            throw new ChaiValidationException( errorMsg.toString(), ChaiError.CR_TOO_FEW_RANDOM_RESPONSES );
        }

        return true;
    }

    @Override
    public Locale getLocale()
    {
        return locale;
    }

    @Override
    public Instant getTimestamp()
    {
        return timestamp;
    }

    @Override
    public ChallengeSet getPresentableChallengeSet()
            throws ChaiValidationException
    {
        return presentableChallengeSet;
    }

    @Override
    public Map<Challenge, String> getHelpdeskResponses()
    {
        final Map<Challenge, String> returnMap = new LinkedHashMap<>();
        if ( this.helpdeskCrMap != null )
        {
            for ( final Map.Entry<Challenge, HelpdeskAnswer> entry : helpdeskCrMap.entrySet() )
            {
                final Challenge challenge = entry.getKey();
                final HelpdeskAnswer answer = entry.getValue();
                final String answerText = answer.answerText();
                returnMap.put( challenge, answerText );
            }
        }
        return returnMap;
    }

    private static ChallengeSet reduceCsToMinRandoms( final ChallengeSet allChallengeSet )
            throws ChaiValidationException
    {
        if ( allChallengeSet.getRandomChallenges().size() <= allChallengeSet.getMinRandomRequired() )
        {
            return allChallengeSet;
        }

        final SecureRandom random = new SecureRandom();
        final List<Challenge> newChallenges = new ArrayList<>();
        final List<Challenge> allRandoms = new ArrayList<>( allChallengeSet.getRandomChallenges() );
        while ( newChallenges.size() < allChallengeSet.getMinRandomRequired() )
        {
            newChallenges.add( allRandoms.remove( random.nextInt( allRandoms.size() ) ) );
        }
        newChallenges.addAll( allChallengeSet.getRequiredChallenges() );
        return new ChaiChallengeSet(
                newChallenges,
                allChallengeSet.getMinRandomRequired(),
                allChallengeSet.getLocale(),
                allChallengeSet.getIdentifier()
        );
    }

    public Map<Challenge, Answer> getChallengeAnswers()
    {

        return crMap == null ? Collections.emptyMap() : Collections.unmodifiableMap( crMap );
    }

    public Map<Challenge, HelpdeskAnswer> getHelpdeskAnswers()
    {
        return helpdeskCrMap == null ? Collections.emptyMap() : Collections.unmodifiableMap( helpdeskCrMap );
    }
}
