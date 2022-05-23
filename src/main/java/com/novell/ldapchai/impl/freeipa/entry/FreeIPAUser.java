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

package com.novell.ldapchai.impl.freeipa.entry;

import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;

import java.time.Instant;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.temporal.ChronoUnit;

class FreeIPAUser extends AbstractChaiUser implements ChaiUser
{
    private static final String ATTR_PRINCIPAL_NAME = "krbPrincipalName";

    private static final String ATTR_ACCOUNT_LOCK = "nsAccountLock";
    private static final String ATTR_PASSWORD_EXPIRATION_TIME = "krbPasswordExpiration";
    private static final String ATTR_LAST_PASSWORD_CHANGE_TIME = "krbLastPwdChange";
    private static final String ATTR_LAST_FAILED_AUTH_TIME = "krbLastFailedAuth";
    private static final String ATTR_LAST_ADMIN_UNLOCK_TIME = "krbLastAdminUnlock";
    private static final String ATTR_LOGIN_FAILED_COUNT = "krbLoginFailedCount";
    private static final String ATTR_PWD_POLICY_REFERENCE = "krbPwdPolicyReference";

    private static final String ATTR_IPA_PWD_POLICY_MAX_FAILURE = "krbPwdMaxFailure";
    private static final String ATTR_IPA_PWD_POLICY_LOCKOUT_DURATION = "krbPwdLockoutDuration";
    private static final String ATTR_IPA_PWD_POLICY_FAIL_COUNT_INTERVAL = "krbPwdFailureCountInterval";

    FreeIPAUser( final String userDN, final ChaiProvider chaiProvider )
    {
        super( userDN, chaiProvider );
    }

    public String getRealm()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String principalName = readStringAttribute( ATTR_PRINCIPAL_NAME );
        if ( principalName != null )
        {
            final Pattern pattern = Pattern.compile( "@([^@]+)$" );
            final Matcher matcher = pattern.matcher( principalName );
            if ( matcher.find() )
            {
                return matcher.group( 1 );
            }
        }

        return null;
    }

    public String getNamingContext()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final String entryDN = getEntryDN();
        if ( entryDN != null )
        {
            final Pattern pattern = Pattern.compile( ",(dc=.*)$" );
            final Matcher matcher = pattern.matcher( entryDN );
            if ( matcher.find() )
            {
                return matcher.group( 1 );
            }
        }

        return null;
    }

    @Override
    public Set<ChaiGroup> getGroups()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Set<ChaiGroup> returnGroups = new LinkedHashSet<>();
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
        this.addAttribute( ChaiConstant.ATTR_LDAP_MEMBER_OF, theGroup.getEntryDN() );
        theGroup.addAttribute( ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN() );
    }

    @Override
    public void removeGroupMembership( final ChaiGroup theGroup )
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.deleteAttribute( ChaiConstant.ATTR_LDAP_MEMBER_OF, theGroup.getEntryDN() );
        theGroup.deleteAttribute( ChaiConstant.ATTR_LDAP_MEMBER, this.getEntryDN() );
    }

    @Override
    public Instant readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        return readDateAttribute( ATTR_PASSWORD_EXPIRATION_TIME );
    }

    @Override
    public ChaiPasswordPolicy getPasswordPolicy()
            throws ChaiUnavailableException, ChaiOperationException
    {
        String pwdPolicyEntryDn = readStringAttribute( ATTR_PWD_POLICY_REFERENCE );
        if ( pwdPolicyEntryDn == null || pwdPolicyEntryDn.isEmpty() )
        {
            pwdPolicyEntryDn = "cn=global_policy,cn=" + getRealm() + ",cn=kerberos," + getNamingContext();
        }

        final FreeIPAEntry pwdPolicyEntry = new FreeIPAEntry( pwdPolicyEntryDn, getChaiProvider() );
        if ( pwdPolicyEntry.exists() )
        {
            return new FreeIPAPasswordPolicy( pwdPolicyEntryDn, getChaiProvider() );
        }

        return null;
    }

    @Override
    public boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final Instant expireDate = readPasswordExpirationDate();

        return expireDate == null ? false : expireDate.isBefore( Instant.now() );

    }

    @Override
    public Instant readPasswordModificationDate()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return readDateAttribute( ATTR_LAST_PASSWORD_CHANGE_TIME );
    }

    @Override
    public boolean isAccountEnabled()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return !readBooleanAttribute( ATTR_ACCOUNT_LOCK );
    }

    @Override
    public boolean isPasswordLocked()
            throws ChaiOperationException, ChaiUnavailableException
    {
        final ChaiPasswordPolicy pwdPolicy = getPasswordPolicy();
        if ( pwdPolicy == null )
        {
            return false;
        }

        try
        {
            final int loginFailedCount = readIntAttribute( ATTR_LOGIN_FAILED_COUNT );
            final int maxFailures = Integer.parseInt( pwdPolicy.getValue( ATTR_IPA_PWD_POLICY_MAX_FAILURE ) );

            if ( loginFailedCount < maxFailures )
            {
                return false;
            }

            final Instant lastFailedAuth = readDateAttribute( ATTR_LAST_FAILED_AUTH_TIME );
            final int lockoutDuration = Integer.parseInt( pwdPolicy.getValue( ATTR_IPA_PWD_POLICY_LOCKOUT_DURATION ) );

            if ( lastFailedAuth.plus( lockoutDuration, ChronoUnit.SECONDS ).isBefore( Instant.now() ) )
            {
                return false;
            }
        }
        catch ( NumberFormatException e )
        {
            return false;
        }

        return true;
    }

    @Override
    public boolean isLocked()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return isPasswordExpired();
    }

    @Override
    public String readGUID()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return FreeIPAEntry.readGUIDImpl( this.getChaiProvider(), this.getEntryDN() );
    }
}
