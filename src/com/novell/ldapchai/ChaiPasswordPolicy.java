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

package com.novell.ldapchai;

import com.novell.ldapchai.util.PasswordRuleHelper;

import java.util.Set;

/**
 * Represents a password policy ldap entry.  The actual implementation for this
 * class may or may not represent a one to one relationship with an ldap entry, such
 * functionality is directory implementation specific.
 * <p/>
 * Generally, a {@code ChaiPasswordPolicy} will behave as an
 * immutable object.  Specifically, there are no set operations to modify a
 * {@code ChaiPasswordPolicy} once instantiated.
 * However, an instance of {@code ChaiPasswordPolicy}
 * may be backed directly by interaction with the underlying directory; therefore
 * it is possible that returned values change over time.
 * <p/>
 * {@code ChaiPasswordPolicy} instances can generally be
 * thought of as simple wrappers for a Map of String/String where the keys are
 * keys defined by {@link ChaiPasswordRule#getKey()} and values
 * are string representations of the policy values. 
 *
 * @author Jason D. Rivard
 */
public interface ChaiPasswordPolicy {
// -------------------------- OTHER METHODS --------------------------

    /**
     * Returns the value for the requested key.  If the {@code ChaiPasswordPolicy} does
     * not have a value for the requested key, {@code null} will be returned.  The requested key may or may not be
     * defined by a {@link ChaiPasswordRule} constant.
     *
     * @param key requested key value.
     * @return String representation of the value, or {@code null} if no value.
     * @see ChaiPasswordRule#getKey() 
     */
    String getValue(String key);

    /**
     * Returns the value for the requested key.  If the {@code com.novell.ldapchai.ChaiPasswordPolicy} does
     * not have a value for the requested key, {@code null} will be returned.
     *
     * @param key requested key value.
     * @return String representation of the value, or null if no value.
     */
    String getValue(ChaiPasswordRule key);

    /**
     * Get a list of all keys available from the instance.
     *
     * @return Set of all keys available from the instance.
     */
    Set<String> getKeys();

    /**
     * Get the backing {@code ChaiEntry} instance for
     * the policy.  Some  instances (such as those of {@link com.novell.ldapchai.util.DefaultChaiPasswordPolicy})
     * do not have a backing entry and therefore will return {@code null}.
     *
     * @return the backing {@code ChaiEntry} or {@code null} if no backing entry.
     */
    ChaiEntry getPolicyEntry();

// -------------------------- ENUMERATIONS --------------------------

    /**
     * Return a {@link com.novell.ldapchai.util.PasswordRuleHelper} object for this
     * {@code com.novell.ldapchai.ChaiPasswordPolicy}.
     *
     * @return a {@link com.novell.ldapchai.util.PasswordRuleHelper} object for this {@code com.novell.ldapchai.ChaiPasswordPolicy}.
     */
    public PasswordRuleHelper getRuleHelper();


}
