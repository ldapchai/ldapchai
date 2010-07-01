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

package com.novell.ldapchai.util;

import java.util.List;

/**
 * A simple  helper for clients that wish to consume the rules via traditional getters instead
 * of reading rules and interpreting String values from a {@link com.novell.ldapchai.ChaiPasswordPolicy}.  Use
 * of a rule helper is not required, they are only available to make reading the most common password rules
 * easy for Java clients.
 *
 * @see com.novell.ldapchai.ChaiPasswordPolicy#getRuleHelper()
 *
 * @author Jason D. Rivard
 */
public interface PasswordRuleHelper {
    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowNumeric
     */
    public boolean isAllowNumeric();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowFirstCharNumeric
     */
    public boolean isAllowFirstCharNumeric();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowFirstCharNumeric
     */
    public boolean isAllowLastCharNumeric();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowSpecial
     */
    public boolean isAllowSpecial();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowFirstCharSpecial
     */
    public boolean isAllowFirstCharSpecial();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowLastCharSpecial
     */
    public boolean isAllowLastCharSpecial();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#MaximumSequentialRepeat
     */
    public int getMaximumSequentialRepeat();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#MaximumRepeat
     */
    public int getMaximumRepeat();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#MinimumLifetime
     */
    public int getMinimumLifetime();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#ChangeMessage
     */
    public String getChangeMessage();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#ExpirationInterval
     */
    public int getExpirationInterval();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#CaseSensitive
     */
    public boolean isCaseSensitive();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#EnforceAtLogin
     */
    public boolean isEnforceAtLogin();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowNumeric
     */
    public boolean isUniqueRequired();

    /**
     * @see com.novell.ldapchai.ChaiPasswordRule#PolicyEnabled
     */
    public boolean isPolicyEnabled();

    public List<String> getDisallowedValues();

    public List<String> getDisallowedAttributes();

}
