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

package com.novell.ldapchai.util;

import java.util.List;

/**
 * A simple  helper for clients that wish to consume the rules via traditional getters instead
 * of reading rules and interpreting String values from a {@link com.novell.ldapchai.ChaiPasswordPolicy}.  Use
 * of a rule helper is not required, they are only available to make reading the most common password rules
 * easy for Java clients.
 *
 * @author Jason D. Rivard
 * @see com.novell.ldapchai.ChaiPasswordPolicy#getRuleHelper()
 */
public interface PasswordRuleHelper
{
    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowNumeric
     */
    boolean isAllowNumeric();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowFirstCharNumeric
     */
    boolean isAllowFirstCharNumeric();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowFirstCharNumeric
     */
    boolean isAllowLastCharNumeric();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowSpecial
     */
    boolean isAllowSpecial();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowFirstCharSpecial
     */
    boolean isAllowFirstCharSpecial();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowLastCharSpecial
     */
    boolean isAllowLastCharSpecial();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#MaximumSequentialRepeat
     */
    int getMaximumSequentialRepeat();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#MaximumRepeat
     */
    int getMaximumRepeat();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#MinimumLifetime
     */
    int getMinimumLifetime();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#ChangeMessage
     */
    String getChangeMessage();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#ExpirationInterval
     */
    int getExpirationInterval();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#CaseSensitive
     */
    boolean isCaseSensitive();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#EnforceAtLogin
     */
    boolean isEnforceAtLogin();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#AllowNumeric
     */
    boolean isUniqueRequired();

    /**
     * @return value of the rule
     * @see com.novell.ldapchai.ChaiPasswordRule#PolicyEnabled
     */
    boolean isPolicyEnabled();

    List<String> getDisallowedValues();

    List<String> getDisallowedAttributes();

}
