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


import com.novell.ldapchai.cr.Answer;
import com.novell.ldapchai.cr.AnswerFactory;

import java.io.Serializable;
import java.util.Objects;

public class AnswerBean implements Serializable
{
    private final Answer.FormatType type;
    private final String answerText;
    private final String answerHash;
    private final String salt;
    private final int hashCount;
    private final boolean caseInsensitive;

    public AnswerBean(
            final Answer.FormatType type,
            final String answerText,
            final String answerHash,
            final String salt,
            final int hashCount,
            final boolean caseInsensitive )
    {
        this.type = type;
        this.answerText = answerText;
        this.answerHash = answerHash;
        this.salt = salt;
        this.hashCount = hashCount;
        this.caseInsensitive = caseInsensitive;
    }

    public Answer.FormatType getType()
    {
        return type;
    }

    public String getAnswerText()
    {
        return answerText;
    }

    public String getAnswerHash()
    {
        return answerHash;
    }

    public String getSalt()
    {
        return salt;
    }

    public int getHashCount()
    {
        return hashCount;
    }

    public boolean isCaseInsensitive()
    {
        return caseInsensitive;
    }

    public Answer asAnswer( final ChallengeBean associatedChallengeBean )
    {
        return AnswerFactory.fromAnswerBean( this, associatedChallengeBean.getChallengeText() );
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

        final AnswerBean other = ( AnswerBean ) o;
        return Objects.equals( type, other.type )
                && Objects.equals( answerText, other.answerText )
                && Objects.equals( answerHash, other.answerHash )
                && Objects.equals( salt, other.salt )
                && Objects.equals( hashCount, other.hashCount )
                && Objects.equals( caseInsensitive, other.caseInsensitive );
    }

    public int hashCode()
    {
        return Objects.hash(
                type,
                answerText,
                answerHash,
                salt,
                hashCount,
                caseInsensitive );
    }
}
