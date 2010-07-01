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

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiPasswordRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Makes a basic password policy using default password rules.
 *
 * @author Jason D. Rivard
 */
public class DefaultChaiPasswordPolicy implements ChaiPasswordPolicy {

    private final Map<String, String> rules = makeDefaultRuleMap();

    private static Map<String, String> makeDefaultRuleMap() {
        final Map<String, String> rules = new HashMap<String, String>();

        for (final ChaiPasswordRule rule : ChaiPasswordRule.values() ) {
            rules.put(rule.getKey(), rule.getDefaultValue());
        }

        return rules;
    }

    public DefaultChaiPasswordPolicy() {
    }

    public String getValue(final String key) {
        return rules.get(key);
    }

    public String getValue(final ChaiPasswordRule rule) {
        return rules.get(rule.getKey());
    }

    public Set<String> getKeys() {
        return Collections.unmodifiableSet(rules.keySet());
    }

    public PasswordRuleHelper getRuleHelper() {
        return new GenericRuleHelper(this);
    }

    public static com.novell.ldapchai.util.DefaultChaiPasswordPolicy createDefaultChaiPasswordPolicy(final Map<String, String> rules) {
        final com.novell.ldapchai.util.DefaultChaiPasswordPolicy newPolicy = new com.novell.ldapchai.util.DefaultChaiPasswordPolicy();
        if (rules != null) {
            newPolicy.rules.putAll(rules);
        }
        return newPolicy;
    }

    public static com.novell.ldapchai.util.DefaultChaiPasswordPolicy createDefaultChaiPasswordPolicyByRule(final Map<ChaiPasswordRule, String> rules) {
        final com.novell.ldapchai.util.DefaultChaiPasswordPolicy newPolicy = new com.novell.ldapchai.util.DefaultChaiPasswordPolicy();
        if (rules != null) {
            for (final ChaiPasswordRule rule : rules.keySet()) {
                newPolicy.rules.put(rule.getKey(),rules.get(rule));
            }
        }
        return newPolicy;
    }

    public static com.novell.ldapchai.util.DefaultChaiPasswordPolicy createDefaultChaiPasswordPolicy() {
        return new com.novell.ldapchai.util.DefaultChaiPasswordPolicy();
    }

    public String toString() {
        return ChaiUtility.passwordPolicyToString(this);
    }

    public ChaiEntry getPolicyEntry() {
        return null;
    }
}
