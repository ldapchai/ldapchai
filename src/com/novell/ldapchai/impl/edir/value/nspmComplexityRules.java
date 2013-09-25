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

package com.novell.ldapchai.impl.edir.value;

import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.util.ChaiLogger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class nspmComplexityRules {
// ------------------------------ FIELDS ------------------------------
    
    public static final nspmComplexityRules MS_COMPLEXITY_POLICY;
    
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(ChaiFactory.class);

    private List<Policy> policies = Collections.emptyList();

// -------------------------- STATIC METHODS --------------------------

    static {
        final List<RuleSet> ruleSets = new ArrayList<RuleSet>();
        {
            final Map<Rule,String> rules = new HashMap<Rule,String>();
            rules.put(Rule.MinPwdLen,"6");
            rules.put(Rule.MaxPwdLen,"128");
            final RuleSet ruleSet = new RuleSet(0, rules);
            ruleSets.add(ruleSet);
        }
        {
            final Map<Rule,String> rules = new HashMap<Rule,String>();
            rules.put(Rule.MinUppercase,"1");
            rules.put(Rule.MinLowercase,"1");
            rules.put(Rule.MinSpecial,"1");
            rules.put(Rule.MinNumeric,"1");
            final RuleSet ruleSet = new RuleSet(1, rules);
            ruleSets.add(ruleSet);
        }
        final List<Policy> policyList = new ArrayList<Policy>();
        final Policy policy = new Policy(ruleSets);
        policyList.add(policy);
        MS_COMPLEXITY_POLICY = new nspmComplexityRules(policyList);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public nspmComplexityRules(final List<Policy> policies) {
        if (policies == null) {
            throw new NullPointerException("policies may not be null");
        }
        this.policies = Collections.unmodifiableList(policies);
    }

    public nspmComplexityRules(final String input) {
        this.policies = readComplexityPoliciesFromXML(input);
    }

     private static List<Policy> readComplexityPoliciesFromXML(final String input) {
         final List<Policy> returnList = new ArrayList<Policy>();
         try {
             final SAXBuilder builder = new SAXBuilder();
             final Document doc = builder.build(new StringReader(input));
             final Element rootElement = doc.getRootElement();

             final List policyElements = rootElement.getChildren("Policy");
             for (final Object policyNode : policyElements) {
                 final Element policyElement = (Element)policyNode;
                 final List<RuleSet> returnRuleSets = new ArrayList<RuleSet>();
                 for (final Object ruleSetObjects : policyElement.getChildren("RuleSet")) {
                     final Element loopRuleSet = (Element)ruleSetObjects;
                     final Map<Rule,String> returnRules = new HashMap<Rule,String>();
                     int violationsAllowed = 0;

                     final org.jdom2.Attribute violationsAttribute = loopRuleSet.getAttribute("ViolationsAllowed");
                     if (violationsAttribute != null && violationsAttribute.getValue().length() > 0) {
                         violationsAllowed = Integer.parseInt(violationsAttribute.getValue());
                     }

                     for (final Object ruleObject : loopRuleSet.getChildren("Rule")) {
                         final Element loopRuleElement = (Element)ruleObject;

                         final List ruleAttributes = loopRuleElement.getAttributes();
                         for (final Object attributeObject : ruleAttributes) {
                             final org.jdom2.Attribute loopAttribute = (org.jdom2.Attribute)attributeObject;

                             final Rule rule = Rule.valueOf(loopAttribute.getName());
                             final String value = loopAttribute.getValue();
                             returnRules.put(rule, value);
                         }
                     }
                     returnRuleSets.add(new RuleSet(violationsAllowed,returnRules));
                 }
                 returnList.add(new Policy(returnRuleSets));
             }
         } catch (JDOMException e) {
             LOGGER.debug("error parsing stored response record: " + e.getMessage());
         } catch (IOException e) {
             LOGGER.debug("error parsing stored response record: " + e.getMessage());
         } catch (NullPointerException e) {
             LOGGER.debug("error parsing stored response record: " + e.getMessage());
         } catch (IllegalArgumentException e) {
             LOGGER.debug("error parsing stored response record: " + e.getMessage());
         }
         return returnList;
     }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public int hashCode() {
        return policies.hashCode();
    }

// -------------------------- OTHER METHODS --------------------------

    public List<Policy> getComplexityPolicies() {
        return policies;
    }

    public boolean isMsComplexityPolicy() {
        return MS_COMPLEXITY_POLICY.equals(this);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        nspmComplexityRules that = (nspmComplexityRules) o;

        if (!policies.equals(that.policies)) return false;

        return true;
    }

// -------------------------- ENUMERATIONS --------------------------

     public enum Rule {
         MinPwdLen,
         MaxPwdLen,
         MinUppercase,
         MaxUppercase,
         MinLowercase,
         MaxLowercase,
         MinNumeric,
         MaxNumeric,
         MinSpecial,
         MaxSpecial,
         MaxRepeated,
         MaxConsecutive,
         MinUnique,
         UppercaseFirstCharDisallowed,
         UppercaseLastCharDisallowed,
         LowercaseFirstCharDisallowed,
         LowercaseLastCharDisallowed,
         FirstCharNumericDisallowed,
         LastCharNumericDisallowed,
         FirstCharSpecialDisallowed,
         LastCharSpecialDisallowed,
         ExtendedCharDisallowed
     }

// -------------------------- INNER CLASSES --------------------------

    public static class Policy {
         List<RuleSet> ruleSets;

         public Policy(final List<RuleSet> ruleSets) {
             if (ruleSets == null) {
                 throw new NullPointerException("ruleSets may not be null");
             }
             this.ruleSets = Collections.unmodifiableList(ruleSets);
         }

         public List<RuleSet> getComplexityRuleSets() {
             return ruleSets;
         }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Policy policy = (Policy) o;

            if (!ruleSets.equals(policy.ruleSets)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return ruleSets.hashCode();
        }
    }

     public static class RuleSet {
         int violationsAllowed;
         Map<Rule,String> complexityRules;

         public RuleSet(final int violationsAllowed, final Map<Rule, String> complexityRules) {
             if (complexityRules == null) {
                 throw new NullPointerException("complexityRules may not be null");
             }
             this.violationsAllowed = violationsAllowed;
             this.complexityRules = Collections.unmodifiableMap(complexityRules);
         }

         public int getViolationsAllowed() {
             return violationsAllowed;
         }

         public Map<Rule, String> getComplexityRules() {
             return complexityRules;
         }

         @Override
         public boolean equals(Object o) {
             if (this == o) return true;
             if (o == null || getClass() != o.getClass()) return false;

             RuleSet ruleSet = (RuleSet) o;

             if (violationsAllowed != ruleSet.violationsAllowed) return false;
             if (complexityRules != null ? !complexityRules.equals(ruleSet.complexityRules) : ruleSet.complexityRules != null)
                 return false;

             return true;
         }

         @Override
         public int hashCode() {
             int result = violationsAllowed;
             result = 31 * result + (complexityRules != null ? complexityRules.hashCode() : 0);
             return result;
         }
     }
}
