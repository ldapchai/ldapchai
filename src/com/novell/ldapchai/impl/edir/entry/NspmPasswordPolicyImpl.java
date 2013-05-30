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

package com.novell.ldapchai.impl.edir.entry;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.edir.value.nspmComplexityRules;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.util.GenericRuleHelper;
import com.novell.ldapchai.util.PasswordRuleHelper;
import com.novell.ldapchai.util.SearchHelper;
import com.novell.ldapchai.util.StringHelper;

import java.io.Serializable;
import java.util.*;


/**
 * An immutable class describing a user password policy.  {@code nspmPasswordPolicyImpl} features directly
 * map to features and options that are part of the Universal Password policy object (ldap class <i>nspmPasswordPolicyImpl</i>).
 * <p/>
 * {@code nspmPasswordPolicyImpl} instances are backed by a {@link java.util.Properties} keymap.  The key
 * values are all defined as public fields of {@code PasswordPolicy}.  Each of these keys are
 * the attribute names found on a ldap <i>nspmPasswordPolicyImpl</i> entry.
 * <p/>
 * This class contains no mechanisms to contact an ldap directory.  Specifically,
 * it does not hold a {@link com.novell.ldapchai.provider.ChaiProvider} reference.  {@code nspmPasswordPolicyImpl} instances
 * do hold a <i>sourceDN</i> attribute, however this is for reference use only.  {@code nspmPasswordPolicyImpl} instances
 * do not use the <i>sourceDN</i> value themselves.
 * <p/>
 * <i>Notes for implementors:</i>
 * <p/>
 * This class is designed to be subclassed for feature enhancements.  Subclasses can add their own
 * settings to the properties environment.
 *
 * @author Jason D. Rivard
 */
class NspmPasswordPolicyImpl extends TopImpl implements NspmPasswordPolicy {

    static final Collection<String> LDAP_PASSWORD_ATTRIBUTES;

    static {
        final ArrayList<String> ldapPasswordAttributes = new ArrayList<String>();
        for (final Attribute attribute : Attribute.values()) {
            ldapPasswordAttributes.add(attribute.getLdapAttribute());
        }
        LDAP_PASSWORD_ATTRIBUTES = Collections.unmodifiableCollection(ldapPasswordAttributes);
    }


    private final Map<String, String> ruleMap = new HashMap<String, String>();
    private final Map<String, List<String>> allEntryValues = new HashMap<String, List<String>>();

    NspmPasswordPolicyImpl(final String entryDN, final ChaiProvider chaiProvider)
            throws ChaiUnavailableException, ChaiOperationException
    {
        super(entryDN, chaiProvider);

        //read all attribute values from entry.
        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter(SearchHelper.DEFAULT_FILTER);
        searchHelper.setSearchScope(ChaiProvider.SEARCH_SCOPE.BASE);
        searchHelper.setAttributes(LDAP_PASSWORD_ATTRIBUTES);

        final Map<String, Map<String, List<String>>> bigResults = this.getChaiProvider().searchMultiValues(getEntryDN(), searchHelper);
        final Map<String, List<String>> results = bigResults.get(this.getEntryDN());

        allEntryValues.putAll(results);
        ruleMap.putAll(createRuleMapUsingAttributeValues(results));
    }

    public String getLdapObjectClassName()
    {
        return "nspmPasswordPolicy";
    }

    public String getChallengeSetDN() {
        final List<String> dnValues = allEntryValues.get(Attribute.CHALLENGE_SET_DN.getLdapAttribute());
        return (dnValues != null && !dnValues.isEmpty()) ? dnValues.get(0) : "";
    }

    public String getSourceDN() {
        return this.getEntryDN();
    }

    public List<ChaiError> testPasswordForErrors(final String password) {
        //@todo implement this
        throw new UnsupportedOperationException("not implemented");
    }

    public PasswordRuleHelper getRuleHelper() {
        return new GenericRuleHelper(this);
    }

    private static Map<String,String> createRuleMapUsingComplexityRules(final String input) {
        final Map<String, String> returnMap = new HashMap<String,String>();

        final nspmComplexityRules complexityRules = new nspmComplexityRules(input);
        if (complexityRules.isMsComplexityPolicy()) {
            returnMap.put(ChaiPasswordRule.ADComplexity.getKey(),String.valueOf(true));
            return returnMap;
        }

        returnMap.put(ChaiPasswordRule.NovellComplexityRules.getKey(),input);
        return returnMap;
    }

    private static Map<String,String> createRuleMapUsingAttributeValues(final Map<String,List<String>> entryValues) {
        final Map<String,String> returnMap = new HashMap<String,String>();

        // check if Complexity XML value is populated.
        {
            final List<String> complexityValues = entryValues.get("nspmComplexityRules");
            if (complexityValues != null && !complexityValues.isEmpty()) {
                final String strValue = complexityValues.get(0);
                returnMap.putAll(createRuleMapUsingComplexityRules(strValue));
            }
        }


        // convert the standard attributes to chai rules
        for (final ChaiPasswordRule rule : ChaiPasswordRule.values()) {
            final Attribute attribute = Attribute.attributeForRule(rule);
            if (attribute != null) {
                returnMap.put(rule.getKey(),attribute.getDefaultValue());
                if (attribute.getLdapAttribute() != null) {
                    final List<String> ruleValues = entryValues.get(attribute.getLdapAttribute());
                    if (ruleValues != null && !ruleValues.isEmpty()) {
                        returnMap.put(rule.getKey(),ruleValues.get(0));
                    }
                }
            }
        }

        //special read for multivalued attributes:
        {
            final List<String> results = entryValues.get(Attribute.DISALLOWED_ATTRIBUTES.getLdapAttribute());
            if (results != null) {
                final List<String> cleanedResults = new ArrayList<String>();
                for (ListIterator<String> iterator = results.listIterator(); iterator.hasNext(); ) {
                    cleanedResults.add(iterator.next().replaceAll("[ :]", ""));
                }
                final String normalizedValue = StringHelper.stringCollectionToString(cleanedResults,"\n");
                returnMap.put(ChaiPasswordRule.DisallowedAttributes.getKey(),normalizedValue);
            }
        }

        // convert the options bitmask.
        {
            final List<String> optionsValues = entryValues.get(Attribute.PASSWORD_POLICY_OPTIONS.getLdapAttribute());
            if (optionsValues != null && !optionsValues.isEmpty()) {
                final String optionsValue = optionsValues.get(0);
                final int defaultOptionsValue = Integer.parseInt(Attribute.PASSWORD_POLICY_OPTIONS.getDefaultValue());
                final int options = StringHelper.convertStrToInt(optionsValue,defaultOptionsValue);
                final PolicyOptions policyOptions = new PolicyOptions(String.valueOf(options));
                returnMap.put(ChaiPasswordRule.PolicyEnabled.getKey(),Boolean.toString(policyOptions.isPolicyEnabled()));
            }
        }


        return returnMap;
    }

    public String getValue(final String key) {
        return ruleMap.get(key);
    }

    public String getValue(final ChaiPasswordRule rule) {
        return ruleMap.get(rule.getKey());
    }

    public Set<String> getKeys() {
        return Collections.unmodifiableSet(ruleMap.keySet());
    }

    /**
     * Wrapper class for the value of the {@link com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicy.Attribute#PASSWORD_POLICY_OPTIONS} attribute.
     *
     * @author Jason D. Rivard
     */
    static class PolicyOptions implements Serializable {
        private int bitMask = 0;

        /**
         * Individual option values
         */
        private enum Option {
            REMOVE_NDS_HASH(0x01),
            DO_NOT_SET_NDS(0x02),
            DO_NOT_SET_SIMPLE(0x04),
            ALLOW_SELF_RETRIEVAL(0x10),
            ALLOW_ADMIN_RETRIEVAL(0x20),
            ALLOW_AGENT_RETRIEVAL(0x40),
            PASSWORD_ENABLED(0x100),
            ADVANCED_POLICY_ENABLED(0x200);

            private final int position;

            Option(final int intValue)
            {
                this.position = intValue;
            }

            private int getPosition()
            {
                return position;
            }
        }

        /**
         * Create a {@code PolicyOptions} class for the given int value.  The supplied
         * int value is typically read from the {@link Attribute#PASSWORD_POLICY_OPTIONS}.
         *
         * @param intValue an int value of a bit mask.
         */
        public PolicyOptions(final String intValue)
        {
            bitMask = StringHelper.convertStrToInt(intValue, 0);
        }

        private boolean getOption(final Option option)
        {
            return ((bitMask & option.getPosition()) == option.getPosition());
        }

        /**
         * 0x01, On set password request the NDS password hash will be removed by SPM
         *
         * @return true if the bit is set
         */
        public boolean isRemoveNdsHash()
        {
            return getOption(Option.REMOVE_NDS_HASH);
        }

        /**
         * 0x02, On set password request the NDS password hash will not be set by SPM
         *
         * @return true if the bit is set
         */
        public boolean isDoNotSetNds()
        {
            return getOption(Option.DO_NOT_SET_NDS);
        }

        /**
         * 0x04, On set password request the SpanShapeRenderer.Simple password will not be set by SPM
         *
         * @return true if the bit is set
         */
        public boolean isDoNotSetSimple()
        {
            return getOption(Option.DO_NOT_SET_SIMPLE);
        }

        /**
         * 0x10, Allow password retrieval by self
         *
         * @return true if the bit is set
         */
        public boolean isAllowSelfRetrieval()
        {
            return getOption(Option.ALLOW_SELF_RETRIEVAL);
        }

        /**
         * 0x20, Allow password retrieval by admin
         *
         * @return true if the bit is set
         */
        public boolean isAllowAdminRetrieval()
        {
            return getOption(Option.ALLOW_ADMIN_RETRIEVAL);
        }

        /**
         * 0x40, Allow password retrieval by password agents
         *
         * @return true if the bit is set
         */
        public boolean isAllowAgentRetrieval()
        {
            return getOption(Option.ALLOW_AGENT_RETRIEVAL);
        }

        /**
         * 0x100, Password enabled
         *
         * @return true if the bit is set
         */
        public boolean isPasswordEnabled()
        {
            return getOption(Option.PASSWORD_ENABLED);
        }

        /**
         * 0x200, Advanced password policy enabled
         *
         * @return true if the bit is set
         */
        public boolean isPolicyEnabled()
        {
            return getOption(Option.ADVANCED_POLICY_ENABLED);
        }

        /**
         * String representation, suitable for debugging
         *
         * @return a String useful for debugging
         */
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();

            for (final Option o : Option.values()) {
                sb.append(o.toString());
                sb.append(": ");
                sb.append(this.getOption(o));
                sb.append(", ");
            }

            if (sb.length() > 2) {
                sb.delete(sb.length() - 2, sb.length());
            }

            return sb.toString();
        }
    }

    public ChaiEntry getPolicyEntry() {
        return this;
    }
}
