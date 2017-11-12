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

package com.novell.ldapchai.impl.openldap.entry;

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.edir.entry.EdirEntries;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.util.ChaiLogger;

import java.util.Date;

public class OpenLDAPEntries
{

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( OpenLDAPEntries.class );

    public static Date convertZuluToDate( final String dateString )
    {
        return EdirEntries.convertZuluToDate( dateString );
    }

    public static String convertDateToZulu( final Date date )
    {
        return EdirEntries.convertDateToZulu( date );
    }

    static OpenLDAPPasswordPolicy readPasswordPolicy( final OpenLDAPUser person )
            throws ChaiUnavailableException, ChaiOperationException
    {
        ChaiEntry searchEntry = new OpenLDAPEntry( person.getEntryDN(), person.getChaiProvider() );
        OpenLDAPEntry discoveredPolicy = null;
        int safetyCounter = 0;

        while ( safetyCounter < 50 && searchEntry != null && discoveredPolicy == null )
        {
            safetyCounter++;
            if ( searchEntry.isValid() )
            {
                final String pwdPolicySubentryValue = searchEntry.readStringAttribute(
                        ChaiConstant.ATTR_OPENLDAP_PASSWORD_SUB_ENTRY );
                LOGGER.trace( "pwdPolicySubentryValue = " + pwdPolicySubentryValue );
                if ( pwdPolicySubentryValue != null && !pwdPolicySubentryValue.isEmpty() )
                {
                    final OpenLDAPEntry policyEntry = new OpenLDAPEntry( pwdPolicySubentryValue,
                            person.getChaiProvider() );
                    if ( policyEntry.isValid() )
                    {
                        discoveredPolicy = policyEntry;
                    }
                }
            }
            searchEntry = searchEntry.getParentEntry();
        }

        if ( discoveredPolicy != null )
        {
            return new OpenLDAPPasswordPolicy( discoveredPolicy.getEntryDN(), person.getChaiProvider() );
        }

        final String passwordPolicyDn = person.getChaiProvider().getChaiConfiguration().getSetting( ChaiSetting.PASSWORD_POLICY_DN );
        LOGGER.debug( "passwordPolicyDn = " + passwordPolicyDn );
        if ( passwordPolicyDn != null && passwordPolicyDn.trim().length() > 0 )
        {
            final OpenLDAPPasswordPolicy defaultPolicy = new OpenLDAPPasswordPolicy( passwordPolicyDn, person.getChaiProvider() );
            if ( defaultPolicy.isValid() )
            {
                return defaultPolicy;
            }
        }

        return null;
    }
}
