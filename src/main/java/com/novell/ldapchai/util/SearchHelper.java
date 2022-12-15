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

import com.novell.ldapchai.provider.SearchScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * {@code SearchHelper} is a mutable helper class for managing LDAP search queries.
 *
 * @author Jason D. Rivard
 */
public class SearchHelper implements Serializable
{
    public static final String DEFAULT_FILTER = "(objectClass=*)";
    public static final SearchScope DEFAULT_SCOPE = SearchScope.SUBTREE;
    public static final int DEFAULT_TIMEOUT = 0;
    public static final int DEFAULT_MAX_RESULTS = 0;


    private String filter = DEFAULT_FILTER;
    private SearchScope searchScope = DEFAULT_SCOPE;
    private Set<String> attributes = null;
    private int maxResults = DEFAULT_MAX_RESULTS;
    private int timeLimit = DEFAULT_TIMEOUT;

    /**
     * Construct a default {@code SearchHelper} with default values for all parameters.
     */
    public SearchHelper()
    {
    }

    public SearchHelper( final SearchHelper source )
    {
        this.filter = source.filter;
        this.searchScope = source.searchScope;
        this.attributes = source.attributes;
        this.maxResults = source.maxResults;
        this.timeLimit = source.timeLimit;
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     */
    public SearchHelper( final String filter )
    {
        this.setFilter( filter );
    }

    /**
     * Set the filter to a valid ldap search filter string.
     *
     * @param filter A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     */
    public void setFilter( final String filter )
    {
        this.filter = filter == null ? DEFAULT_FILTER : filter;
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     */
    public SearchHelper( final SearchScope searchScope )
    {
        this.setSearchScope( searchScope );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param attributes A list of attribute names.
     */
    public SearchHelper( final String[] attributes )
    {
        this.setAttributes( attributes );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param attributes A list of attribute names.
     */
    public SearchHelper( final Set<String> attributes )
    {
        this.setAttributes( attributes );
    }

    /**
     * Set the filter to a list of attributes.
     *
     * @param attributes a collection of attribute names
     */
    public void setAttributes( final Collection<String> attributes )
    {
        this.attributes = attributes == null ? null : Collections.unmodifiableSet( new HashSet<>( attributes ) );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter      A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     */
    public SearchHelper( final String filter, final SearchScope searchScope )
    {
        this.setFilter( filter );
        this.setSearchScope( searchScope );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter     A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param attributes A list of attribute names.
     */
    public SearchHelper( final String filter, final String[] attributes )
    {
        this.setFilter( filter );
        this.setAttributes( attributes );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter     A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param attributes A list of attribute names.
     */
    public SearchHelper( final String filter, final Set<String> attributes )
    {
        this.setFilter( filter );
        this.setAttributes( attributes );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter     A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param attributes A list of comma or space seperated attributes
     */
    public SearchHelper( final String filter, final String attributes )
    {
        this.setFilter( filter );
        this.setAttributes( attributes );
    }

    /**
     * Set the filter to a list of attributes.
     *
     * @param attributes A list of comma or space seperated attributes
     */
    public void setAttributes( final String attributes )
    {
        setAttributes( attributes.split( ",| " ) );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     * @param attributes  A list of attribute names.
     */
    public SearchHelper( final String[] attributes, final SearchScope searchScope )
    {
        this.setSearchScope( searchScope );
        this.setAttributes( attributes );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     * @param attributes  A list of attribute names.
     */
    public SearchHelper( final Set<String> attributes, final SearchScope searchScope )
    {
        this.setSearchScope( searchScope );
        this.setAttributes( attributes );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter      A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     * @param attributes  A list of attribute names.
     */
    public SearchHelper( final String filter, final SearchScope searchScope, final String[] attributes )
    {
        this.setFilter( filter );
        this.setSearchScope( searchScope );
        this.setAttributes( attributes );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter      A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     * @param attributes  A list of attribute names.
     */
    public SearchHelper( final String filter, final SearchScope searchScope, final Set<String> attributes )
    {
        this.setFilter( filter );
        this.setSearchScope( searchScope );
        this.setAttributes( attributes );
    }

    /**
     * Construct a {@code SearchHelper} with the supplied parameters.  Default values are used for
     * missing values.
     *
     * @param filter      A valid ldap search filter.  <i>null</i> will set the filter to {@link #DEFAULT_FILTER}.
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     * @param attributes  A list of comma or space seperated attributes
     */
    public SearchHelper( final String filter, final SearchScope searchScope, final String attributes )
    {
        this.setFilter( filter );
        this.setSearchScope( searchScope );
        this.setAttributes( attributes );
    }

    /**
     * Get the list of attributes that will be returned during a search.  Note that some
     * places that {@code SearchHelper} is used don't return attribute values at all, so
     * this setting is discounted.
     *
     * @return A list of valid attribute names
     */
    public Set<String> getAttributes()
    {
        return attributes == null ? null : Collections.unmodifiableSet( attributes );
    }

    /**
     * Set the filter to a list of attributes.
     *
     * @param attributes A list of attribute names.
     */
    public void setAttributes( final String... attributes )
    {
        this.attributes = attributes == null ? null : new HashSet<>( Arrays.asList( attributes ) );
    }

    /**
     * Current LDAP search filter.
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

    public void setMaxResults( final int maxResults )
    {
        this.maxResults = maxResults;
    }

    /**
     * Current scope of the search operation.
     *
     * @return A valid SEARCH_SCOPE of Base, One or Subtree
     */
    public SearchScope getSearchScope()
    {
        return searchScope;
    }

    /**
     * Set the scipe of the search operation.
     *
     * @param searchScope A valid SEARCH_SCOPE of Base, One or Subtree
     */
    public void setSearchScope( final SearchScope searchScope )
    {
        this.searchScope = searchScope;
    }

    public int getTimeLimit()
    {
        return timeLimit;
    }

    public void setTimeLimit( final int timeLimit )
    {
        this.timeLimit = timeLimit;
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        final SearchHelper that = ( SearchHelper ) o;
        return maxResults == that.maxResults
                && timeLimit == that.timeLimit
                && Objects.equals( filter, that.filter )
                && searchScope == that.searchScope
                && Objects.equals( attributes, that.attributes );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( filter, searchScope, attributes, maxResults, timeLimit );
    }

    /**
     * Converts the {@code SearchHelper} to a string suitable for debugging.
     *
     * @return a debug string
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append( "SearchHelper: " );
        sb.append( "filter: " ).append( filter ).append( ", " );
        sb.append( "scope: " ).append( this.getSearchScope() ).append( ", " );

        if ( attributes != null )
        {
            sb.append( "attributes: [" );
            sb.append( String.join( ",", attributes ) );
            sb.append( "]" );
        }

        if ( maxResults >= 0 )
        {
            sb.append( ", max=" ).append( maxResults );
        }

        if ( timeLimit >= 0 )
        {
            sb.append( ", timeLimit=" ).append( timeLimit );
        }

        return sb.toString();
    }

    public void returnNoAttributes()
    {
        attributes = Collections.emptySet();
    }

    public void returnAllAttributes()
    {
        attributes = null;
    }

    /**
     * Set the filter to {@link #DEFAULT_FILTER}.
     */
    public void clearFilter()
    {
        filter = DEFAULT_FILTER;
    }

    /**
     * Set up an AND filter for each map key and value.  Consider the following example.
     *
     * <table border="1"><caption>Example Values</caption>
     * <tr><td><b>Attribute</b></td><td><b>Value</b></td></tr>
     * <tr><td>givenName</td><td>John</td></tr>
     * <tr><td>sn</td><td>Smith</td></tr>
     * </table>
     * <p><i>Result</i></p>
     * <code>(&amp;(givenName=John)(sn=Smith))</code>
     *
     * @param nameValuePairs A valid list of attribute to name pairs
     */
    public void setFilterAnd( final Map<String, String> nameValuePairs )
    {
        if ( nameValuePairs == null )
        {
            throw new NullPointerException();
        }

        if ( nameValuePairs.size() < 1 )
        {
            throw new IllegalArgumentException( "requires at least one key" );
        }

        final List<FilterSequence> filters = new ArrayList<>();
        for ( final Map.Entry<String, String> entry : nameValuePairs.entrySet() )
        {
            filters.add( new FilterSequence( entry.getKey(), entry.getValue(), FilterSequence.MatchingRuleEnum.EQUALS ) );
        }
        setFilterBind( filters, "&" );
    }

    private void setFilterBind( final List<FilterSequence> filterSequences, final String operator )
    {
        if ( filterSequences == null || filterSequences.size() < 1 )
        {
            throw new IllegalArgumentException( "requires at least one key" );
        }

        final StringBuilder sb = new StringBuilder();

        if ( filterSequences.size() > 1 )
        {
            sb.append( "(" );
            sb.append( operator );
            for ( final FilterSequence sequence : filterSequences )
            {
                sb.append( sequence.toString() );
            }
            sb.append( ")" );
        }
        else
        {
            sb.append( filterSequences.get( 0 ).toString() );
        }

        filter = sb.toString();
    }

    /**
     * Convenience wrapper for {@link #setFilterAnd(java.util.Map)}.
     *
     * @param nameValuePairs A valid map of name=value pairs.
     * @see #setFilterAnd(java.util.Map)
     */
    public void setFilterAnd( final Properties nameValuePairs )
    {
        if ( nameValuePairs == null )
        {
            throw new NullPointerException();
        }

        final Map<String, String> newMap = new HashMap<>();
        for ( Enumeration enumer = nameValuePairs.propertyNames(); enumer.hasMoreElements(); )
        {
            final String name = ( String ) enumer.nextElement();
            newMap.put( name, nameValuePairs.getProperty( name ) );
        }
        setFilterAnd( newMap );
    }

    /**
     * Set up an exists filter for attribute name.  Consider the following example.
     *
     * <table border="1"><caption>Example Values</caption>
     * <tr><td><b>Value</b></td></tr>
     * <tr><td>givenName</td></tr>
     * </table>
     * <p><i>Result</i></p>
     * <code>(givenName=*)</code>
     *
     * @param attributeName A valid attribute name
     */
    public void setFilterExists( final String attributeName )
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "(" );
        sb.append( new FilterSequence( attributeName, "*", FilterSequence.MatchingRuleEnum.EQUALS ) );
        sb.append( ")" );
        this.filter = sb.toString();
    }

    /**
     * Set up an exists filter for attribute name.  Consider the following example.
     *
     * <table border="1"><caption>Example Values</caption>
     * <tr><td><b>Value</b></td></tr>
     * <tr><td>givenName</td></tr>
     * <tr><td>sn</td></tr>
     * </table>
     * <p><i>Result</i></p>
     * <code>(&amp;(givenName=*)(sn=*))</code>
     *
     * @param attributeNames A valid set of attribute names
     */
    public void setFilterExists( final Set<String> attributeNames )
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "(&" );

        for ( final String name : attributeNames )
        {
            sb.append( "(" );
            sb.append( new FilterSequence( name, "*", FilterSequence.MatchingRuleEnum.EQUALS ) );
            sb.append( ")" );
        }

        sb.append( ")" );

        filter = sb.toString();
    }

    /**
     * Set up a not exists filter for an attribute name and value pair.
     *
     * <table border="1"><caption>Example Values</caption>
     * <tr><td><b>Attribute</b></td><td><b>Value</b></td></tr>
     * <tr><td>givenName</td><td>John</td></tr>
     * </table>
     * <p><i>Result</i></p>
     * <code>(!(givenName=John))</code>
     *
     * @param attributeName A valid attribute name
     * @param value         A value that, if it exists, will cause the object to be excluded from the result set.
     */
    public void setFilterNot( final String attributeName, final String value )
    {
        this.setFilter( attributeName, value );
        filter = "(!" + filter + ")";
    }

    /**
     * Set up a standard filter attribute name and value pair.
     *
     * <table border="1"><caption>Example Values</caption>
     * <tr><td><b>Attribute</b></td><td><b>Value</b></td></tr>
     * <tr><td>givenName</td><td>John</td></tr>
     * </table>
     * <p><i>Result</i></p>
     * <code>(givenName=John)</code>
     *
     * @param attributeName A valid attribute name
     * @param value         A value that, if it exists, will cause the object to be included in result set.
     */
    public void setFilter( final String attributeName, final String value )
    {
        filter = new FilterSequence( attributeName, value ).toString();
    }

    /**
     * Set up an OR filter for each map key and value.  Consider the following example.
     *
     * <table border="1"><caption>Example Values</caption>
     * <tr><td><b>Attribute</b></td><td><b>Value</b></td></tr>
     * <tr><td>givenName</td><td>John</td></tr>
     * <tr><td>sn</td><td>Smith</td></tr>
     * </table>
     * <p><i>Result</i></p>
     * <code>(|(givenName=John)(sn=Smith))</code>
     *
     * @param nameValuePairs A valid list of attribute to name pairs
     */
    public void setFilterOr( final Map<String, String> nameValuePairs )
    {
        if ( nameValuePairs == null )
        {
            throw new NullPointerException();
        }

        if ( nameValuePairs.size() < 1 )
        {
            throw new IllegalArgumentException( "requires at least one key" );
        }

        final List<FilterSequence> filters = new ArrayList<>();
        for ( final Map.Entry<String, String> entry : nameValuePairs.entrySet() )
        {
            filters.add( new FilterSequence( entry.getKey(), entry.getValue(), FilterSequence.MatchingRuleEnum.EQUALS ) );
        }
        setFilterBind( filters, "|" );
    }

    /**
     * Convenience wrapper for {@link #setFilterOr(java.util.Map)}.
     *
     * @param nameValuePairs A valid map of name=value pairs.
     * @see #setFilterOr(java.util.Map)
     */
    public void setFilterOr( final Properties nameValuePairs )
    {
        if ( nameValuePairs == null )
        {
            throw new NullPointerException();
        }

        final Map<String, String> newMap = new HashMap<>();
        for ( Enumeration enumer = nameValuePairs.propertyNames(); enumer.hasMoreElements(); )
        {
            final String name = ( String ) enumer.nextElement();
            newMap.put( name, nameValuePairs.getProperty( name ) );
        }
        setFilterOr( newMap );
    }

    static class FilterSequence
    {
        public enum MatchingRuleEnum
        {
            EQUALS( "=" ),
            APPROX_EQUALS( "~=" ),
            GREATER( ">=" ),
            LESS( "<=" );

            private final String matchCode;

            MatchingRuleEnum( final String matchCode )
            {
                this.matchCode = matchCode;
            }

            public String getMatchCode()
            {
                return matchCode;
            }
        }

        private final String attr;
        private final String value;
        private final MatchingRuleEnum matchingRule;


        FilterSequence( final String attr, final String value, final MatchingRuleEnum matchingRule )
        {
            if ( attr == null )
            {
                throw new NullPointerException( "attr is required" );
            }

            this.attr = attr;
            this.value = value;
            this.matchingRule = matchingRule;
        }

        FilterSequence( final String attr, final String value )
        {
            this( attr, value, MatchingRuleEnum.EQUALS );
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
