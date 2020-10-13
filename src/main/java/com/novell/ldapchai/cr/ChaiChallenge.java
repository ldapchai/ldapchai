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

import com.google.gson.Gson;
import com.novell.ldapchai.cr.bean.ChallengeBean;

import java.io.Serializable;
import java.util.Objects;

/**
 * An immutable implementation of {@link Challenge}.
 */
public class ChaiChallenge implements Challenge, Serializable
{
    private final boolean adminDefined;
    private final boolean required;
    private final String challengeText;

    private final int minLength;
    private final int maxLength;

    private final int maxQuestionCharsInAnswer;
    private final boolean enforceWordlist;

    public ChaiChallenge(
            final boolean required,
            final String challengeText,
            final int minLength,
            final int maxLength,
            final boolean adminDefined,
            final int maxQuestionCharsInAnswer,
            final boolean enforceWordlist
    )
    {
        this.adminDefined = adminDefined;
        this.required = required;
        this.challengeText = challengeText == null ? null : challengeText.trim();
        this.minLength = minLength < 0 ? 2 : minLength;
        this.maxLength = maxLength < 0 ? 255 : maxLength;
        this.maxQuestionCharsInAnswer = Math.max( maxQuestionCharsInAnswer, 0 );
        this.enforceWordlist = enforceWordlist;
    }

    @Override
    public final String getChallengeText()
    {
        return challengeText;
    }

    @Override
    public final int getMaxLength()
    {
        return maxLength;
    }

    @Override
    public final int getMinLength()
    {
        return minLength;
    }

    @Override
    public final boolean isAdminDefined()
    {
        return adminDefined;
    }

    @Override
    public final boolean isRequired()
    {
        return required;
    }

    @Override
    public int getMaxQuestionCharsInAnswer()
    {
        return maxQuestionCharsInAnswer;
    }

    @Override
    public boolean isEnforceWordlist()
    {
        return enforceWordlist;
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

        final ChaiChallenge other = ( ChaiChallenge ) o;
        return Objects.equals( adminDefined, other.adminDefined )
                && Objects.equals( required, other.required )
                && Objects.equals( challengeText, other.challengeText )
                && Objects.equals( minLength, other.minLength )
                && Objects.equals( maxLength, other.maxLength )
                && Objects.equals( maxQuestionCharsInAnswer, other.maxQuestionCharsInAnswer )
                && Objects.equals( enforceWordlist, other.enforceWordlist );
    }

    public int hashCode()
    {
        return Objects.hash(
                adminDefined,
                required,
                challengeText,
                minLength,
                maxLength,
                maxQuestionCharsInAnswer,
                enforceWordlist );
    }

    public String toString()
    {
        return "Challenge: " + new Gson().toJson( asChallengeBean() );
    }

    @Override
    public ChallengeBean asChallengeBean()
    {
        return new ChallengeBean(
                challengeText,
                minLength,
                maxLength,
                adminDefined,
                required,
                maxQuestionCharsInAnswer,
                enforceWordlist,
                null );
    }

    public static Challenge fromChallengeBean( final ChallengeBean challengeBean )
    {
        return new ChaiChallenge(
                challengeBean.isRequired(),
                challengeBean.getChallengeText(),
                challengeBean.getMinLength(),
                challengeBean.getMaxLength(),
                challengeBean.isAdminDefined(),
                challengeBean.getMaxQuestionCharsInAnswer(),
                challengeBean.isEnforceWordlist()

        );
    }
}


