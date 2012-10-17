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

import com.novell.ldapchai.provider.ChaiProvider;

import java.io.Serializable;
import java.util.*;

/**
 * {@code SearchHelper} is a mutable helper class for managing LDAP search queries.
 *
 * @author Jason D. Rivard
 */
public class SearchHelper implements Serializable, Cloneable {
// ----------------------------- CONSTANTS ----------------------------

    public static final String DEFAULT_FILTER = "(objectClass=*)";
    public static final ChaiProvider.SEARCH_SCOPE DEFAULT_SCOPE = ChaiProvider.SEARCH_SCOPE.SUBTREE;
    public static final int DEFAULT_TIMEOUT = 0;
    public static final int DEFAULT_MAX_RESULTS = 0;

// ------------------------------ FIELDS ------------------------------

    private String filter = DEFAULT_FILTER;
    private ChaiProvider.SEARCH_SCOPE searchScope = DEFAULT_SCOPE;
    private Set<String> attributes = null;
    private int maxResults = DEFAULT_MAX_RESULTS;
    private int timeLimit = DEFAULT_TIMEOUT;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Construct a default {@code SearchHelper} with default values for all parameters.
     */
    public SearchHelper()
    {
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     */
    public SearchHelper(final String filter)
    {
        this.setFilter(filter);
    }

    /**
     * Set the filter to a valid ldap search filter string.
     *
     * @param filter A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     */
    public void setFilter(final String filter)
    {
        this.filter = filter == null ? DEFAULT_FILTER : filter;
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     */
    public SearchHelper(final ChaiProvider.SEARCH_SCOPE searchScope)
    {
        this.setSearchScope(searchScope);
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param attributes A list of attribute names.
     */
    public SearchHelper(final String[] attributes)
    {
        this.setAttributes(attributes);
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param attributes A list of attribute names.
     */
    public SearchHelper(final Set<String> attributes)
    {
        this.setAttributes(attributes);
    }

    /**
     * Set the filter to a list of attributes.
     *
     * @param attributes a collection of attribute names
     */
    public void setAttributes(final Collection<String> attributes)
    {
        this.attributes = attributes == null ? null : Collections.unmodifiableSet(new HashSet<String>(attributes));
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter      A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     */
    public SearchHelper(final String filter, final ChaiProvider.SEARCH_SCOPE searchScope)
    {
        this.setFilter(filter);
        this.setSearchScope(searchScope);
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter     A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param attributes A list of attribute names.
     */
    public SearchHelper(final String filter, final String[] attributes)
    {
        this.setFilter(filter);
        this.setAttributes(attributes);
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter     A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param attributes A list of attribute names.
     */
    public SearchHelper(final String filter, final Set<String> attributes)
    {
        this.setFilter(filter);
        this.setAttributes(attributes);
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter     A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param attributes A list of comma or space seperated attributes
     */
    public SearchHelper(final String filter, final String attributes)
    {
        this.setFilter(filter);
        this.setAttributes(attributes);
    }

    /**
     * Set the filter to a list of attributes.
     *
     * @param attributes A list of comma or space seperated attributes
     */
    public void setAttributes(final String attributes)
    {
        setAttributes(attributes.split(",| "));
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     * @param attributes  A list of attribute names.
     */
    public SearchHelper(final String[] attributes, final ChaiProvider.SEARCH_SCOPE searchScope)
    {
        this.setSearchScope(searchScope);
        this.setAttributes(attributes);
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     * @param attributes  A list of attribute names.
     */
    public SearchHelper(final Set<String> attributes, final ChaiProvider.SEARCH_SCOPE searchScope)
    {
        this.setSearchScope(searchScope);
        this.setAttributes(attributes);
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter      A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     * @param attributes  A list of attribute names.
     */
    public SearchHelper(final String filter, final ChaiProvider.SEARCH_SCOPE searchScope, final String[] attributes)
    {
        this.setFilter(filter);
        this.setSearchScope(searchScope);
        this.setAttributes(attributes);
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter      A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     * @param attributes  A list of attribute names.
     */
    public SearchHelper(final String filter, final ChaiProvider.SEARCH_SCOPE searchScope, final Set<String> attributes)
    {
        this.setFilter(filter);
        this.setSearchScope(searchScope);
        this.setAttributes(attributes);
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter      A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     * @param attributes  A list of comma or space seperated attributes
     */
    public SearchHelper(final String filter, final ChaiProvider.SEARCH_SCOPE searchScope, final String attributes)
    {
        this.setFilter(filter);
        this.setSearchScope(searchScope);
        this.setAttributes(attributes);
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * Get the list of attributes that will be returned during a search.  Note that some
     * places that {@code SearchHelper} is used don't return attribute values at all, so
     * this setting is discounted.
     *
     * @return A list of valid attribute names
     */
    public Set<String> getAttributes()
    {
        return attributes == null ? null : Collections.unmodifiableSet(attributes);
    }

    /**
     * Set the filter to a list of attributes.
     *
     * @param attributes A list of attribute names.
     */
    public void setAttributes(final String... attributes)
    {
        this.attributes = attributes == null ? null : new HashSet<String>(Arrays.asList(attributes));
    }

    /**
     * Current LDAP search filter
     *
     * @return String representation of filter in RFC2254 format.
     */
    public String getFilter()
    {
        return filter;
    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(final int maxResults)
    {
        this.maxResults = maxResults;
    }

    /**
     * Current scope of the search operation
     *
     * @return A valid SEARCH_SCOPE of Base, One or Subtree
     */
    public ChaiProvider.SEARCH_SCOPE getSearchScope()
    {
        return searchScope;
    }

    /**
     * Set the scipe of the search operation
     *
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     */
    public void setSearchScope(final ChaiProvider.SEARCH_SCOPE searchScope)
    {
        this.searchScope = searchScope;
    }

    public int getTimeLimit()
    {
        return timeLimit;
    }

    public void setTimeLimit(final int timeLimit)
    {
        this.timeLimit = timeLimit;
    }

// ------------------------ CANONICAL METHODS ------------------------

    public Object clone()
            throws CloneNotSupportedException
    {
        return super.clone();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final SearchHelper that = (SearchHelper) o;

        if (maxResults != that.maxResults) return false;
        if (timeLimit != that.timeLimit) return false;
        if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) return false;
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;
        if (searchScope != that.searchScope) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = filter != null ? filter.hashCode() : 0;
        result = 31 * result + (searchScope != null ? searchScope.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + maxResults;
        result = 31 * result + timeLimit;
        return result;
    }

    /**
     * Converts the {@code SearchHelper} to a string suitable for debugging.
     *
     * @return a debug string
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append("SearchHelper: ");
        sb.append("filter: ").append(filter).append(", ");
        sb.append("scope: ").append(this.getSearchScope()).append(", ");
        if (attributes != null) {
            sb.append("attributes: ").append(Arrays.toString(attributes.toArray(new String[attributes.size()])));
        }

        return sb.toString();
    }

// -------------------------- OTHER METHODS --------------------------

    public void returnNoAttributes()
    {
        attributes = Collections.emptySet();
    }

    public void returnAllAttributes()
    {
        attributes = null;
    }

    /**
     * Set the filter to {@link #DEFAULT_FILTER}
     */
    public void clearFilter()
    {
        filter = DEFAULT_FILTER;
    }

    /**
     * Set up an AND filter for each map key and value.  Consider the following example.
     * <h4>Example Values</h4>
     * <table border="1">
     * <tr><td><b>Attribute</b></td><td><b>Value</b></td></tr>
     * <tr><td>givenName</td><td>John</td></tr>
     * <tr><td>sn</td><td>Smith</td></tr>
     * </table>
     * <h4>Result</h4>
     * <pre>(&(givenName=John)(sn=Smith))</pre>
     *
     * @param nameValuePairs A valid list of attribute to name pairs
     */
    public void setFilterAnd(final Map<String, String> nameValuePairs)
    {
        if (nameValuePairs == null) {
            throw new NullPointerException();
        }

        if (nameValuePairs.size() < 1) {
            throw new IllegalArgumentException("requires at least one key");
        }

        final List<FilterSequence> filters = new ArrayList<FilterSequence>();
        for (final String name : nameValuePairs.keySet()) {
            filters.add(new FilterSequence(name, nameValuePairs.get(name), FilterSequence.MatchingRuleEnum.EQUALS));
        }
        setFilterBind(filters, "&");
    }

    private void setFilterBind(final List<FilterSequence> filterSequences, final String operator)
    {
        if (filterSequences == null || filterSequences.size() < 1) {
            throw new IllegalArgumentException("requires at least one key");
        }

        final StringBuilder sb = new StringBuilder();

        if (filterSequences.size() > 1) {
            sb.append("(");
            sb.append(operator);
            for (final FilterSequence sequence : filterSequences) {
                sb.append(sequence.toString());
            }
            sb.append(")");
        } else {
            sb.append(filterSequences.get(0).toString());
        }

        filter = sb.toString();
    }

    /**
     * Convienence wrapper for {@link #setFilterAnd(java.util.Map)}
     *
     * @param nameValuePairs A valid map of name=value pairs.
     * @see #setFilterAnd(java.util.Map)
     */
    public void setFilterAnd(final Properties nameValuePairs)
    {
        if (nameValuePairs == null) {
            throw new NullPointerException();
        }

        final Map<String, String> newMap = new HashMap<String, String>();
        for (Enumeration enumer = nameValuePairs.propertyNames(); enumer.hasMoreElements();) {
            final String name = (String) enumer.nextElement();
            newMap.put(name, nameValuePairs.getProperty(name));
        }
        setFilterAnd(newMap);
    }

    /**
     * Set up an exists filter for attribute name.  Consider the following example.
     * <h4>Example Values</h4>
     * <table border="1">
     * <tr><td><b>Value</b></td></tr>
     * <tr><td>givenName</td></tr>
     * </table>
     * <h4>Result</h4>
     * <pre>(givenName=*)</pre>
     *
     * @param attributeName A valid attribute name
     */
    public void setFilterExists(final String attributeName)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(new FilterSequence(attributeName, "*", FilterSequence.MatchingRuleEnum.EQUALS));
        sb.append(")");
        this.filter = sb.toString();
    }

    /**
     * Set up an exists filter for attribute name.  Consider the following example.
     * <h4>Example Values</h4>
     * <table border="1">
     * <tr><td><b>Value</b></td></tr>
     * <tr><td>givenName</td></tr>
     * <tr><td>sn</td></tr>
     * </table>
     * <h4>Result</h4>
     * <pre>(&(givenName=*)(sn=*))</pre>
     *
     * @param attributeNames A valid set of attribute names
     */
    public void setFilterExists(final Set<String> attributeNames)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("(&");

        for (final String name : attributeNames) {
            sb.append("(");
            sb.append(new FilterSequence(name, "*", FilterSequence.MatchingRuleEnum.EQUALS));
            sb.append(")");
        }

        sb.append(")");

        filter = sb.toString();
    }

    /**
     * Set up a not exists filter for an attribute name and value pair.
     * <h4>Example Values</h4>
     * <table border="1">
     * <tr><td><b>Attribute</b></td><td><b>Value</b></td></tr>
     * <tr><td>givenName</td><td>John</td></tr>
     * </table>
     * <h4>Result</h4>
     * <pre>(!(givenName=John))</pre>
     *
     * @param attributeName A valid attribute name
     * @param value         A value that, if it exists, will cause the object to be excluded from the result set.
     */
    public void setFilterNot(final String attributeName, final String value)
    {
        this.setFilter(attributeName, value);
        filter = "(!" + filter + ")";
    }

    /**
     * Set up a standard filter attribute name and value pair.
     * <h4>Example Values</h4>
     * <table border="1">
     * <tr><td><b>Attribute</b></td><td><b>Value</b></td></tr>
     * <tr><td>givenName</td><td>John</td></tr>
     * </table>
     * <h4>Result</h4>
     * <pre>(givenName=John)</pre>
     *
     * @param attributeName A valid attribute name
     * @param value         A value that, if it exists, will cause the object to be included in result set.
     */
    public void setFilter(final String attributeName, final String value)
    {
        filter = new FilterSequence(attributeName, value).toString();
    }

    /**
     * Set up an OR filter for each map key and value.  Consider the following example.
     * <h4>Example Values</h4>
     * <table border="1">
     * <tr><td><b>Attribute</b></td><td><b>Value</b></td></tr>
     * <tr><td>givenName</td><td>John</td></tr>
     * <tr><td>sn</td><td>Smith</td></tr>
     * </table>
     * <h4>Result</h4>
     * <pre>(|(givenName=John)(sn=Smith))</pre>
     *
     * @param nameValuePairs A valid list of attribute to name pairs
     */
    public void setFilterOr(final Map<String, String> nameValuePairs)
    {
        if (nameValuePairs == null) {
            throw new NullPointerException();
        }

        if (nameValuePairs.size() < 1) {
            throw new IllegalArgumentException("requires at least one key");
        }

        final List<FilterSequence> filters = new ArrayList<FilterSequence>();
        for (final String name : nameValuePairs.keySet()) {
            filters.add(new FilterSequence(name, nameValuePairs.get(name), FilterSequence.MatchingRuleEnum.EQUALS));
        }
        setFilterBind(filters, "|");
    }

    /**
     * Convienence wrapper for {@link #setFilterOr(java.util.Map)}
     *
     * @param nameValuePairs A valid map of name=value pairs.
     * @see #setFilterOr(java.util.Map)
     */
    public void setFilterOr(final Properties nameValuePairs)
    {
        if (nameValuePairs == null) {
            throw new NullPointerException();
        }

        final Map<String, String> newMap = new HashMap<String, String>();
        for (Enumeration enumer = nameValuePairs.propertyNames(); enumer.hasMoreElements();) {
            final String name = (String) enumer.nextElement();
            newMap.put(name, nameValuePairs.getProperty(name));
        }
        setFilterOr(newMap);
    }

// -------------------------- INNER CLASSES --------------------------

    static class FilterSequence {
        public enum MatchingRuleEnum {
            EQUALS("="),
            APPROX_EQUALS("~="),
            GREATER(">="),
            LESS("<=");

            private String matchCode;

            private MatchingRuleEnum(final String matchCode)
            {
                this.matchCode = matchCode;
            }

            public String getMatchCode()
            {
                return matchCode;
            }
        }

        private String attr;
        private String value;
        private MatchingRuleEnum matchingRule;


        public FilterSequence(final String attr, final String value, final MatchingRuleEnum matchingRule)
        {
            if (attr == null) {
                throw new NullPointerException("attr is required");
            }

            this.attr = attr;
            this.value = value;
            this.matchingRule = matchingRule;
        }

        public FilterSequence(final String attr, final String value)
        {
            this(attr, value, MatchingRuleEnum.EQUALS);
        }

        public String getAttr()
        {
            return attr;
        }

        public String getValue()
        {
            return value;
        }

        public MatchingRuleEnum getMatchingRule()
        {
            return matchingRule;
        }

        public String toString()
        {
            return "(" + attr + matchingRule.getMatchCode() + value + ")";
        }
    }
}
