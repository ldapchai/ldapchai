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

package com.novell.ldapchai.cr.bean;

import java.io.Serializable;
import java.util.Objects;

public class ChallengeBean implements Serializable
{
    public final String challengeText;
    public final int minLength;
    public final int maxLength;
    public final boolean adminDefined;
    public final boolean required;
    public final int maxQuestionCharsInAnswer;
    public final boolean enforceWordlist;
    public final AnswerBean answer;


    @SuppressWarnings( "checkstyle:ParameterNumber" )
    public ChallengeBean(
            final String challengeText,
            final int minLength,
            final int maxLength,
            final boolean adminDefined,
            final boolean required,
            final int maxQuestionCharsInAnswer,
            final boolean enforceWordlist,
            final AnswerBean answer
    )
    {
        this.challengeText = challengeText;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.adminDefined = adminDefined;
        this.required = required;
        this.maxQuestionCharsInAnswer = maxQuestionCharsInAnswer;
        this.enforceWordlist = enforceWordlist;
        this.answer = answer;
    }

    public String getChallengeText()
    {
        return challengeText;
    }

    public int getMinLength()
    {
        return minLength;
    }

    public int getMaxLength()
    {
        return maxLength;
    }

    public boolean isAdminDefined()
    {
        return adminDefined;
    }

    public boolean isRequired()
    {
        return required;
    }

    public int getMaxQuestionCharsInAnswer()
    {
        return maxQuestionCharsInAnswer;
    }

    public boolean isEnforceWordlist()
    {
        return enforceWordlist;
    }

    public AnswerBean getAnswer()
    {
        return answer;
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

        final ChallengeBean other = ( ChallengeBean ) o;
        return Objects.equals( adminDefined, other.adminDefined )
                && Objects.equals( required, other.required )
                && Objects.equals( challengeText, other.challengeText )
                && Objects.equals( minLength, other.minLength )
                && Objects.equals( maxLength, other.maxLength )
                && Objects.equals( maxQuestionCharsInAnswer, other.maxQuestionCharsInAnswer )
                && Objects.equals( enforceWordlist, other.enforceWordlist )
                && Objects.equals( answer, other.answer );
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
                enforceWordlist,
                answer );
    }

}
