package com.novell.ldapchai.impl.ad.entry;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.util.PasswordRuleHelper;
import com.novell.ldapchai.util.SearchHelper;

import java.math.BigInteger;
import java.util.*;

public class MsDSPasswordSettingsImpl extends TopImpl implements MsDSPasswordSettings {

    static final Collection<String> LDAP_PASSWORD_ATTRIBUTES;

    static {
        final ArrayList<String> ldapPasswordAttributes = new ArrayList<String>();
        for (final MsDSPasswordSettings.Attribute attribute : MsDSPasswordSettings.Attribute.values()) {
            ldapPasswordAttributes.add(attribute.getLdapAttribute());
        }
        LDAP_PASSWORD_ATTRIBUTES = Collections.unmodifiableCollection(ldapPasswordAttributes);
    }

    private final Map<String, String> ruleMap = new HashMap<String, String>();
    private final Map<String, List<String>> allEntryValues = new HashMap<String, List<String>>();

    MsDSPasswordSettingsImpl(final String entryDN, final ChaiProvider chaiProvider)
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

    private static Map<String,String> createRuleMapUsingAttributeValues(final Map<String,List<String>> entryValues) {
        final Map<String,String> returnMap = new HashMap<String,String>();

        // convert the standard attributes to chai rules
        for (final ChaiPasswordRule rule : ChaiPasswordRule.values()) {
            final MsDSPasswordSettings.Attribute attribute = MsDSPasswordSettings.Attribute.attributeForRule(rule);
            if (attribute != null) {
                //returnMap.put(rule.getKey(),attribute.getDefaultValue());
                if (attribute.getLdapAttribute() != null) {
                    final List<String> ruleValues = entryValues.get(attribute.getLdapAttribute());
                    if (ruleValues != null && !ruleValues.isEmpty()) {
                        if (attribute.getType() == Attribute.TYPE.DURATION) {
                            returnMap.put(rule.getKey(),timeSpanSyntaxToSeconds(ruleValues.get(0)));
                        } else {
                            returnMap.put(rule.getKey(),ruleValues.get(0));
                        }
                    }
                }
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

    public ChaiEntry getPolicyEntry() {
        return this;
    }

    public PasswordRuleHelper getRuleHelper() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private static String timeSpanSyntaxToSeconds(final String input) {
        if (input == null || input.length() < 1) {
            return "0";
        }

        final BigInteger numberValue;
        try {
            numberValue = new BigInteger(input).abs();
        } catch (NumberFormatException e) {
            return "0";
        }

        if (numberValue.compareTo(new BigInteger("9999999")) <= 0) {
            return "0";
        }

        return numberValue.divide(new BigInteger("10000000")).toString();
    }
}
