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

package com.novell.ldapchai.impl.ad.entry;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.SearchScope;
import com.novell.ldapchai.util.PasswordRuleHelper;
import com.novell.ldapchai.util.SearchHelper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MsDSPasswordSettingsImpl extends TopImpl implements MsDSPasswordSettings
{

    static final Collection<String> LDAP_PASSWORD_ATTRIBUTES;

    static
    {
        final ArrayList<String> ldapPasswordAttributes = new ArrayList<>();
        for ( final MsDSPasswordSettings.Attribute attribute : MsDSPasswordSettings.Attribute.values() )
        {
            ldapPasswordAttributes.add( attribute.getLdapAttribute() );
        }
        LDAP_PASSWORD_ATTRIBUTES = Collections.unmodifiableCollection( ldapPasswordAttributes );
    }

    private final Map<String, String> ruleMap = new HashMap<>();
    private final Map<String, List<String>> allEntryValues = new HashMap<>();

    MsDSPasswordSettingsImpl( final String entryDN, final ChaiProvider chaiProvider )
            throws ChaiUnavailableException, ChaiOperationException
    {
        super( entryDN, chaiProvider );

        //read all attribute values from entry.
        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter( SearchHelper.DEFAULT_FILTER );
        searchHelper.setSearchScope( SearchScope.BASE );
        searchHelper.setAttributes( LDAP_PASSWORD_ATTRIBUTES );

        final Map<String, Map<String, List<String>>> bigResults = this.getChaiProvider().searchMultiValues( getEntryDN(), searchHelper );
        final Map<String, List<String>> results = bigResults.get( this.getEntryDN() );

        allEntryValues.putAll( results );
        ruleMap.putAll( createRuleMapUsingAttributeValues( results ) );
    }

    private static Map<String, String> createRuleMapUsingAttributeValues( final Map<String, List<String>> entryValues )
    {
        final Map<String, String> returnMap = new HashMap<>();

        // convert the standard attributes to chai rules
        for ( final ChaiPasswordRule rule : ChaiPasswordRule.values() )
        {
            final MsDSPasswordSettings.Attribute attribute = MsDSPasswordSettings.Attribute.attributeForRule( rule );
            if ( attribute != null )
            {
                //returnMap.put(rule.getKey(),attribute.getDefaultValue());
                if ( attribute.getLdapAttribute() != null )
                {
                    final List<String> ruleValues = entryValues.get( attribute.getLdapAttribute() );
                    if ( ruleValues != null && !ruleValues.isEmpty() )
                    {
                        if ( attribute.getType() == Attribute.TYPE.DURATION )
                        {
                            returnMap.put( rule.getKey(), timeSpanSyntaxToSeconds( ruleValues.get( 0 ) ) );
                        }
                        else
                        {
                            returnMap.put( rule.getKey(), ruleValues.get( 0 ) );
                        }
                    }
                }
            }
        }

        return returnMap;
    }


    @Override
    public String getValue( final String key )
    {
        return ruleMap.get( key );
    }

    @Override
    public String getValue( final ChaiPasswordRule rule )
    {
        return ruleMap.get( rule.getKey() );
    }

    @Override
    public Set<String> getKeys()
    {
        return Collections.unmodifiableSet( ruleMap.keySet() );
    }

    @Override
    public ChaiEntry getPolicyEntry()
    {
        return this;
    }

    @Override
    public PasswordRuleHelper getRuleHelper()
    {
        return null;
    }

    private static String timeSpanSyntaxToSeconds( final String input )
    {
        if ( input == null || input.length() < 1 )
        {
            return "0";
        }

        final BigInteger numberValue;
        try
        {
            numberValue = new BigInteger( input ).abs();
        }
        catch ( NumberFormatException e )
        {
            return "0";
        }

        if ( numberValue.compareTo( new BigInteger( "9999999" ) ) <= 0 )
        {
            return "0";
        }

        return numberValue.divide( new BigInteger( "10000000" ) ).toString();
    }

    @Override
    public String readCanonicalDN()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return readStringAttribute( "distinguishedName" );
    }
}
