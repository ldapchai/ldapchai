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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ChallengeSetBean implements Serializable
{
    private final List<ChallengeBean> challenges;
    private final int minRandomRequired;
    private final Locale locale;
    private final String identifier;

    public ChallengeSetBean(
            final List<ChallengeBean> challenges,
            final int minRandomRequired,
            final Locale locale,
            final String identifier )
    {
        this.challenges = challenges == null
                ? Collections.emptyList()
                : Collections.unmodifiableList( new ArrayList<>( challenges ) );
        this.minRandomRequired = minRandomRequired;
        this.locale = locale;
        this.identifier = identifier;
    }

    public List<ChallengeBean> getChallenges()
    {
        return challenges;
    }

    public int getMinRandomRequired()
    {
        return minRandomRequired;
    }

    public Locale getLocale()
    {
        return locale;
    }

    public String getIdentifier()
    {
        return identifier;
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

        final ChallengeSetBean other = ( ChallengeSetBean ) o;
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
