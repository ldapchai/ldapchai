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

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.ChaiRequestControl;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiErrors;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.provider.SearchScope;
import com.novell.ldapchai.util.DefaultChaiPasswordPolicy;
import com.novell.ldapchai.util.SearchHelper;
import com.novell.ldapchai.util.internal.StringHelper;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class UserImpl extends AbstractChaiUser implements User, Top, ChaiUser
{

    // @todo: replace with @UserAccountControl
    private static final int COMPUTED_ACCOUNT_CONTROL_ACCOUNT_ACTIVE = 0x0002;
    private static final int COMPUTED_ACCOUNT_CONTROL_UC_LOCKOUT = 0x0010;
    private static final int COMPUTED_ACCOUNT_CONTROL_UC_PASSWORD_EXPIRED = 0x800000;
    private static final int ADS_UF_DONT_EXPIRE_PASSWD = 0x10000;
    private static final String LDAP_SERVER_POLICY_HINTS_OID = "1.2.840.113556.1.4.2066";

    UserImpl( final String userDN, final ChaiProvider chaiProvider )
    {
        super( userDN, chaiProvider );
    }

    @Override
    public Set<ChaiGroup> getGroups()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<ChaiGroup> returnGroups = new HashSet<>();
        final Set<String> groups = this.readMultiStringAttribute( ChaiConstant.ATTR_LDAP_MEMBER_OF );
        for ( final String group : groups )
        {
            returnGroups.add( getChaiProvider().getEntryFactory().newChaiGroup( group ) );
        }
        return Collections.unmodifiableSet( returnGroups );
    }

    @Override
    public void addGroupMembership( final ChaiGroup theGroup )
            throws ChaiOperationException, ChaiUnavailableException
    {
        theGroup.addAttribute( "member", this.getEntryDN() );
    }

    @Override
    public ChaiPasswordPolicy getPasswordPolicy()
            throws ChaiUnavailableException, ChaiOperationException
    {

        final Map<String, String> policyMap = new LinkedHashMap<>();

        // defaults for ad policy
        policyMap.put( ChaiPasswordRule.AllowNumeric.getKey(), String.valueOf( true ) );
        policyMap.put( ChaiPasswordRule.AllowSpecial.getKey(), String.valueOf( true ) );
        policyMap.put( ChaiPasswordRule.CaseSensitive.getKey(), String.valueOf( true ) );

        //read minimum password length from domain
        final Matcher domainMatcher = Pattern.compile( "(dc=[a-z0-9-]+[,]*)+", Pattern.CASE_INSENSITIVE ).matcher( this.getEntryDN() );
        if ( domainMatcher.find() )
        {
            final String domainDN = domainMatcher.group();
            final String minPwdLength = this.getChaiProvider().readStringAttribute( domainDN, "minPwdLength" );
            if ( minPwdLength != null && minPwdLength.length() > 0 )
            {
                policyMap.put( ChaiPasswordRule.MinimumLength.getKey(), minPwdLength );
            }
        }

        // Read PSO policy object.
        final String psoObject = this.readStringAttribute( ChaiConstant.ATTR_AD_PASSWORD_POLICY_RESULTANT_PSO );
        if ( psoObject != null && psoObject.length() > 0 )
        {
            final MsDSPasswordSettingsImpl msDSPasswordSetting = new MsDSPasswordSettingsImpl( psoObject, this.getChaiProvider() );
            for ( final String loopKey : msDSPasswordSetting.getKeys() )
            {
                policyMap.put( loopKey, msDSPasswordSetting.getValue( loopKey ) );
            }
        }

        return DefaultChaiPasswordPolicy.createDefaultChaiPasswordPolicy( policyMap );
    }

    @Override
    public String readPassword()
            throws ChaiUnavailableException, ChaiOperationException
    {
        throw new UnsupportedOperationException( "ChaiUser#readPassword not implemented in ad-impl ldapChai API" );
    }

    @Override
    public void removeGroupMembership( final ChaiGroup theGroup )
            throws ChaiOperationException, ChaiUnavailableException
    {
        theGroup.deleteAttribute( "member", this.getEntryDN() );
    }

    @Override
    public boolean testPassword( final String passwordValue )
            throws ChaiUnavailableException, ChaiPasswordPolicyException
    {
        throw new UnsupportedOperationException( "ChaiUser#testPassword not implemented in ad-impl ldapChai API" );
    }

    @Override
    public boolean testPasswordPolicy( final String testPassword )
            throws ChaiUnavailableException, ChaiPasswordPolicyException
    {
        return false;
    }

    @Override
    public void unlockPassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.writeStringAttribute( "lockoutTime", "0" );
    }

    @Override
    public void setPassword( final String newPassword, final boolean enforcePasswordPolicy )
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        final String quotedPwd = '"' + newPassword + '"';
        final byte[] littleEndianEncodedPwd;
        try
        {
            littleEndianEncodedPwd = quotedPwd.getBytes( "UTF-16LE" );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new IllegalStateException( "unexpected error, missing 'UTF-16LE' character encoder", e );
        }
        final byte[][] multiBA = new byte[][] {littleEndianEncodedPwd};

        try
        {
            if ( enforcePasswordPolicy && this.getChaiProvider().getChaiConfiguration().getBooleanSetting( ChaiSetting.AD_SET_POLICY_HINTS_ON_PW_SET ) )
            {
                //0x1 berEncoded
                final byte[] value = {48, ( byte ) 132, 0, 0, 0, 3, 2, 1, 1 };
                final ChaiRequestControl[] controls = new ChaiRequestControl[] {new ChaiRequestControl( LDAP_SERVER_POLICY_HINTS_OID, true, value )};
                chaiProvider.writeBinaryAttribute( this.getEntryDN(), "unicodePwd", multiBA, true, controls );
            }
            else
            {
                writeBinaryAttribute( "unicodePwd", multiBA );
            }
        }
        catch ( ChaiOperationException e )
        {
            if ( e.getErrorCode() == ChaiError.UNKNOWN )
            {
                throw new ChaiOperationException( e.getMessage(), ChaiError.PASSWORD_BADPASSWORD, e );
            }
            else
            {
                throw e;
            }
        }
    }

    @Override
    public boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException
    {
        // modern versions of ad have a (somewhat) sane way of checking account lockout; heaven forbid a boolean attribute.
        final String computedBit = readStringAttribute( "msDS-User-Account-Control-Computed" );
        if ( computedBit != null && computedBit.length() > 0 )
        {
            final int intValue = Integer.parseInt( computedBit );
            return ( ( intValue & COMPUTED_ACCOUNT_CONTROL_UC_PASSWORD_EXPIRED ) == COMPUTED_ACCOUNT_CONTROL_UC_PASSWORD_EXPIRED );
        }

        return false;

    }


    @Override
    public final String readGivenName()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute( ATTR_GIVEN_NAME );
    }

    @Override
    public final Instant readLastLoginTime()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<String> readAttributes = new HashSet<>( Arrays.asList( ATTR_LAST_LOGIN, ATTR_LAST_LOGIN_TIMESTAMP ) );
        final Map<String, String> readResults = this.readStringAttributes( readAttributes );
        final Instant lastLoginDate = readResults.containsKey( ATTR_LAST_LOGIN )
                ? ADEntries.convertWinEpochToDate( readResults.get( ATTR_LAST_LOGIN ) )
                : null;
        final Instant lastLoginDateTimestamp = readResults.containsKey( ATTR_LAST_LOGIN_TIMESTAMP )
                ? ADEntries.convertWinEpochToDate( readResults.get( ATTR_LAST_LOGIN_TIMESTAMP ) )
                : null;

        if ( lastLoginDate == null || lastLoginDateTimestamp == null )
        {
            return lastLoginDate == null ? lastLoginDateTimestamp : lastLoginDate;
        }

        return lastLoginDate.isAfter( lastLoginDateTimestamp )
                ? lastLoginDate
                : lastLoginDateTimestamp;
    }

    @Override
    public final void changePassword( final String oldPassword, final String newPassword )
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        final String quotedOldPwd = '"' + oldPassword + '"';
        final String quotedNewPwd = '"' + newPassword + '"';
        final byte[] littleEndianEncodedOldPwd;
        final byte[] littleEndianEncodedNewPwd;
        try
        {
            littleEndianEncodedOldPwd = quotedOldPwd.getBytes( "UTF-16LE" );
            littleEndianEncodedNewPwd = quotedNewPwd.getBytes( "UTF-16LE" );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new IllegalStateException( "unexpected error, missing 'UTF-16LE' character encoder", e );
        }

        try
        {
            replaceBinaryAttribute( "unicodePwd", littleEndianEncodedOldPwd, littleEndianEncodedNewPwd );
        }
        catch ( ChaiOperationException e )
        {
            if ( e.getErrorCode() == ChaiError.UNKNOWN )
            {
                throw new ChaiPasswordPolicyException( e.getMessage(), ChaiError.PASSWORD_BADPASSWORD );
            }
            else
            {
                throw new ChaiPasswordPolicyException( e.getMessage(), ChaiErrors.getErrorForMessage( e.getMessage() ) );
            }
        }

    }

    @Override
    public void expirePassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.writeStringAttribute( "pwdLastSet", "0" );
    }

    @Override
    public boolean isPasswordLocked()
            throws ChaiOperationException, ChaiUnavailableException
    {
        // modern versions of ad have a (somewhat) sane way of checking account lockout; heaven forbid a boolean attribute.
        final String computedBit = readStringAttribute( "msDS-User-Account-Control-Computed" );
        if ( computedBit != null && computedBit.length() > 0 )
        {
            final int intValue = Integer.parseInt( computedBit );
            return ( ( intValue & COMPUTED_ACCOUNT_CONTROL_UC_LOCKOUT ) == COMPUTED_ACCOUNT_CONTROL_UC_LOCKOUT );
        }

        // older ad versions have an insane way of checking account lockout.  what could possibly go wrong?

        // read lockout time of user.
        final Instant lockoutTime = this.readDateAttribute( "lockoutTime" );
        if ( lockoutTime != null )
        {
            ChaiEntry parentEntry = this.getParentEntry();
            long lockoutDurationMs = 0;

            // should never need this, but provided for sanity
            int recursionCount = 0;

            while ( lockoutDurationMs == 0 && parentEntry != null && recursionCount < 50 )
            {
                if ( parentEntry.compareStringAttribute( "objectClass", "domainDNS" ) )
                {
                    // find the domain dns parent entry of the user

                    // read the duration of lockouts from the domainDNS entry
                    lockoutDurationMs = Long.parseLong( parentEntry.readStringAttribute( "lockoutDuration" ) );

                    // why is it stored as a negative value?  who knows.
                    lockoutDurationMs = Math.abs( lockoutDurationMs );

                    // convert from 100 nanosecond intervals to milliseconds.  It's important that intruders don't sneak
                    // into the default 30 minute window a few nanoseconds early.  Thanks again MS.
                    lockoutDurationMs = lockoutDurationMs / 10000;
                }
                parentEntry = parentEntry.getParentEntry();
                recursionCount++;
            }

            final Instant futureUnlockTime = Instant.ofEpochMilli( lockoutTime.toEpochMilli() + lockoutDurationMs );
            return System.currentTimeMillis() <= futureUnlockTime.toEpochMilli();
        }
        return false;
    }

    @Override
    public Instant readPasswordModificationDate()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readDateAttribute( "pwdLastSet" );
    }

    @Override
    public Instant readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final String[] attrsToRead = new String[] {
                "pwdLastSet",
                "userAccountControl",
                "msDS-UserPasswordExpiryTimeComputed",
        };
        final Map<String, String> readAttrs = readStringAttributes( new HashSet<>( Arrays.asList( attrsToRead ) ) );

        final String computedValue = readAttrs.get( "msDS-UserPasswordExpiryTimeComputed" );
        if ( computedValue != null && computedValue.length() > 0 )
        {
            return ADEntries.convertWinEpochToDate( computedValue );
        }

        final String uacStrValue = readAttrs.get( "userAccountControl" );

        if ( uacStrValue != null && uacStrValue.length() > 0 )
        {
            final int intValue = StringHelper.convertStrToInt( uacStrValue, 0 );
            if ( ( ADS_UF_DONT_EXPIRE_PASSWD & intValue ) == ADS_UF_DONT_EXPIRE_PASSWD )
            {
                //user password does not expire
                return null;
            }
        }

        // now read domain object
        long maxPwdAgeMs = 0;
        {
            final String maxPwdAgeString = readDomainValue( "maxPwdAge" );
            if ( maxPwdAgeString != null && maxPwdAgeString.length() > 0 )
            {
                long value = Long.parseLong( maxPwdAgeString );

                // why is it stored as a negative value?  who knows.
                value = Math.abs( value );

                // convert from 100 nanosecond intervals to milliseconds.  It's important that intruders don't sneak
                // into the default 30 minute window a few nanoseconds early.  Thanks again MS.
                value = value / 10000;

                maxPwdAgeMs = value;
            }
        }

        if ( maxPwdAgeMs == 0 )
        {
            // passwords never expire according to the domain policy.
            return null;
        }

        final String maxPwdAgeString = readAttrs.get( "pwdLastSet" );
        if ( maxPwdAgeString != null && maxPwdAgeString.length() > 0 )
        {
            final Instant pwdLastSet = ADEntries.convertWinEpochToDate( maxPwdAgeString );
            if ( pwdLastSet != null )
            {
                final long pwExpireTimeMs = pwdLastSet.toEpochMilli() + maxPwdAgeMs;
                return Instant.ofEpochMilli( pwExpireTimeMs );
            }
        }

        return null;
    }

    private String readDomainValue( final String attribute )
            throws ChaiUnavailableException, ChaiOperationException
    {
        ChaiEntry parentEntry = this.getParentEntry();

        // should never need this, but provided for sanity
        int recursionCount = 0;
        while ( parentEntry != null && recursionCount < 50 )
        {
            if ( parentEntry.compareStringAttribute( "objectClass", "domainDNS" ) )
            {
                // find the domain dns parent entry of the user

                // read the desired attribute.
                return parentEntry.readStringAttribute( attribute );
            }
            parentEntry = parentEntry.getParentEntry();
            recursionCount++;
        }
        return null;
    }

    @Override
    public String readGUID()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return ADEntries.readGUID( this );
    }

    @Override
    public boolean isAccountEnabled()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String disabledUserSearchFilter = "(useraccountcontrol:1.2.840.113556.1.4.803:=2)";
        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter( disabledUserSearchFilter );
        searchHelper.setSearchScope( SearchScope.BASE );
        final Map<String, Map<String, String>> results = this.getChaiProvider().search( this.getEntryDN(), searchHelper );
        for ( final String resultDN : results.keySet() )
        {
            if ( resultDN != null && resultDN.equals( this.getEntryDN() ) )
            {
                return false;
            }
        }
        return true;
    }


    @Override
    public Instant readAccountExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return this.readDateAttribute( "accountExpires" );
    }

    @Override
    public String readCanonicalDN()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return readStringAttribute( "distinguishedName" );
    }
}
