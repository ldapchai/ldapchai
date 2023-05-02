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

import java.io.Serializable;

class AnswerConfiguration implements Serializable
{
    public final boolean caseInsensitive;
    public final int iterations;
    public final int saltCharCount;
    public final Answer.FormatType formatType;
    public final String challengeText;

    private AnswerConfiguration(
            final boolean caseInsensitive,
            final int iterations,
            final int saltCharCount,
            final Answer.FormatType formatType,
            final String challengeText )
    {
        this.formatType = formatType;
        this.caseInsensitive = caseInsensitive;
        this.challengeText = challengeText;
        this.iterations = iterations < 1
                ? formatType.getDefaultIterations()
                : iterations;
        this.saltCharCount = saltCharCount < 1
                ? formatType.getSaltLength()
                : saltCharCount;

    }

    public boolean isCaseInsensitive()
    {
        return caseInsensitive;
    }

    public int getIterations()
    {
        return iterations;
    }

    public Answer.FormatType getFormatType()
    {
        return formatType;
    }

    public String getChallengeText()
    {
        return challengeText;
    }

    public int getSaltCharCount()
    {
        return saltCharCount;
    }

    public static AnswerConfigurationBuilder builder()
    {
        return new AnswerConfigurationBuilder();
    }

    public static class AnswerConfigurationBuilder
    {
        public boolean caseInsensitive;
        public int iterations;
        public int saltCharCount;
        public Answer.FormatType formatType;
        public String challengeText;

        public AnswerConfigurationBuilder caseInsensitive( final boolean caseInsensitive )
        {
            this.caseInsensitive = caseInsensitive;
            return this;
        }

        public AnswerConfigurationBuilder iterations( final int iterations )
        {
            this.iterations = iterations;
            return this;
        }

        public AnswerConfigurationBuilder formatType( final Answer.FormatType formatType )
        {
            this.formatType = formatType;
            return this;
        }

        public AnswerConfigurationBuilder challengeText( final String challengeText )
        {
            this.challengeText = challengeText;
            return this;
        }

        public AnswerConfigurationBuilder saltCharCount( final int saltCharCount )
        {
            this.saltCharCount = saltCharCount;
            return this;
        }

        public AnswerConfiguration build()
        {
            return new AnswerConfiguration( caseInsensitive, iterations, saltCharCount, formatType, challengeText );
        }
    }
}
