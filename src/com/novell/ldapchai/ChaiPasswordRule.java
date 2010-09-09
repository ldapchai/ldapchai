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

/**
 * A list of rules available for the password policy.   The underlying ldap service may or may not
 * provide values for each of these rules.  By convention, rules with a value of "0" are considered non-applicable.
 * <p/>
 * Most {@link ChaiPasswordPolicy} implementations will override the defaults listed here.
 *
 * @author Jason D. Rivard
 */
public enum ChaiPasswordRule {
    /** Marks if the password policy is active.  Some directories support policy definitons that may not be enabled.  Detault is true.  */
    PolicyEnabled("chai.pwrule.policyEnabled", RuleType.BOOLEAN, "true"),

    /** Minimum length of password.  Default is 0. */
    MinimumLength("chai.pwrule.length.min", RuleType.MIN, "0"),

    /** Maximum length of password.  Default is 0. */
    MaximumLength("chai.pwrule.length.max", RuleType.MAX, "0"),

    /** Maximum number of upper case characters.  Default is 0. */
    MaximumUpperCase("chai.pwrule.upper.max", RuleType.MAX, "0"),

    /** Minimum number of upper case characters.  Default is 0. */
    MinimumUpperCase("chai.pwrule.upper.min", RuleType.MIN, "0"),

    /** Maximum number of lower case characters.  Default is 0. */
    MaximumLowerCase("chai.pwrule.lower.max", RuleType.MAX, "0"),

    /** Minimum number of lower case characters.  Default is 0. */
    MinimumLowerCase("chai.pwrule.lower.min", RuleType.MIN, "0"),

    /** If false, then no numeric characters are permited.  Default is false. */
    AllowNumeric("chai.pwrule.numeric.allow", RuleType.BOOLEAN, "false"),

    MinimumNumeric("chai.pwrule.numeric.min", RuleType.MIN, "0"),
    MaximumNumeric("chai.pwrule.numeric.max", RuleType.MAX, "0"),
    MinimumUnique("chai.pwrule.unique.min", RuleType.MIN, "0"),
    MaximumUnique("chai.pwrule.unique.max", RuleType.MAX, "0"),
    AllowFirstCharNumeric("chai.pwrule.numeric.allowFirst", RuleType.BOOLEAN, "true"),
    AllowLastCharNumeric("chai.pwrule.numeric.allowLast", RuleType.BOOLEAN, "true"),
    AllowSpecial("chai.pwrule.special.allow", RuleType.BOOLEAN, "false"),
    MinimumSpecial("chai.pwrule.special.min", RuleType.MIN, "0"),
    MaximumSpecial("chai.pwrule.special.max", RuleType.MAX, "0"),
    AllowFirstCharSpecial("chai.pwrule.special.allowFirst", RuleType.BOOLEAN, "true"),
    AllowLastCharSpecial("chai.pwrule.special.allowLast", RuleType.BOOLEAN, "true"),
    MaximumRepeat("chai.pwrule.repeat.max", RuleType.MAX,"0"),
    MaximumSequentialRepeat("chai.pwrule.sequentialRepeat.max", RuleType.MAX,"0"),
    ChangeMessage("chai.pwrule.changeMessage", RuleType.TEXT, ""),
    ExpirationInterval("chai.pwrule.expirationInterval", RuleType.NUMERIC, "0"),
    MinimumLifetime("chai.pwrule.lifetime.minimimum", RuleType.NUMERIC, "0"),
    CaseSensitive("chai.pwrule.caseSensitive", RuleType.BOOLEAN, "true"),
    EnforceAtLogin("chai.pwrule.enforceAtLogin", RuleType.BOOLEAN, "false"),
    ChallengeResponseEnabled("chai.pwrule.challengeResponseEnabled", RuleType.BOOLEAN, "false"),
    UniqueRequired("chai.pwrule.uniqueRequired", RuleType.BOOLEAN, "false"),
    ADComplexity("chai.pwrule.ADComplexity", RuleType.BOOLEAN, "false"),
    NovellComplexityRules("chai.pwrule.novellComplexity", RuleType.TEXT, ""),

    /**
     * A list of disallowed values that may not appear in the password.
     * Weather or not these values are case sensitive or support any type
     * of wild-card or other matching patterns is left to the backing implementation.
     * <p/>
     * Values are stored as seperated by a newline character ({@code '\n'}).
     */
    DisallowedValues("chai.pwrule.disallowedValues", RuleType.OTHER, ""),

    /**
     * A list of disallowed attributes that may not appear in the password.
     * <p/>
     * Values are stored as seperated by a newline character ({@code '\n'}).
     */
    DisallowedAttributes("chai.pwrule.disallowedAttributes", RuleType.OTHER, ""),

    ;
    private final RuleType ruleType;
    private final String defaultValue;
    private final String key;

    ChaiPasswordRule(final String key, final RuleType ruleType, final String defaultValue) {
        this.key = key;
        this.ruleType = ruleType;
        this.defaultValue = defaultValue;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns a key for the enumeration, suitable for use in a map.  Keys
     * @return the key for the enumeration.
     */
    public String getKey() {
        return key;
    }

    public static enum RuleType {
        MIN, MAX, BOOLEAN, TEXT, OTHER, NUMERIC
    }

    public static ChaiPasswordRule forKey(final String key) {
        if (key == null) {
            return null;
        }

        for (final ChaiPasswordRule rule : ChaiPasswordRule.values()) {
            if (key.equals(rule.getKey())) {
                return rule;
            }
        }

        return null;
    }
}
