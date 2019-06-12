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
import java.util.List;
import java.util.Locale;

public class ChallengeSetBean implements Serializable
{
    private List<ChallengeBean> challenges;
    private int minRandomRequired;
    private Locale locale;
    private String identifier;

    public List<ChallengeBean> getChallenges()
    {
        return challenges;
    }

    public void setChallenges( final List<ChallengeBean> challenges )
    {
        this.challenges = challenges;
    }

    public int getMinRandomRequired()
    {
        return minRandomRequired;
    }

    public void setMinRandomRequired( final int minRandomRequired )
    {
        this.minRandomRequired = minRandomRequired;
    }

    public Locale getLocale()
    {
        return locale;
    }

    public void setLocale( final Locale locale )
    {
        this.locale = locale;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier( final String identifier )
    {
        this.identifier = identifier;
    }
}
