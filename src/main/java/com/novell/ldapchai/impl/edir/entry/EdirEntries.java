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

package com.novell.ldapchai.impl.edir.entry;


import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiEntryFactory;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.edir.entry.ext.GetPwdPolicyInfoRequest;
import com.novell.ldapchai.impl.edir.entry.ext.GetPwdPolicyInfoResponse;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.provider.SearchScope;
import com.novell.ldapchai.util.internal.ChaiLogger;
import com.novell.ldapchai.util.DefaultChaiPasswordPolicy;
import com.novell.ldapchai.util.SearchHelper;
import com.novell.ldapchai.util.internal.StringHelper;

import javax.naming.ldap.ExtendedResponse;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A collection of static helper methods used by the LDAP Chai API.
 * Generally, consumers of the LDAP Chai API should avoid calling these methods directly.  Where possible,
 * use the {@link com.novell.ldapchai.ChaiEntry} wrappers instead.
 *
 * @author Jason D. Rivard
 */
public class EdirEntries
{
    private static final String EDIR_TIMESTAMP_PATTERN = "yyyyMMddHHmmss'Z'";

    private static final DateTimeFormatter EDIR_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern( EDIR_TIMESTAMP_PATTERN )
            .toFormatter();

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( EdirEntries.class );

    static final String EDIR_ATTR_SUBORDINATE_COUNT = "subordinateCount";

    /**
     * Convert a Instant to the Zulu String format.
     * See the <a href="http://developer.novell.com/documentation/ndslib/schm_enu/data/sdk5701.html">eDirectory Time attribute syntax definition</a> for more details.
     *
     * @param instant The Date to be converted
     * @return A string formatted such as "199412161032Z".
     */
    public static String convertInstantToZulu( final Instant instant )
    {
        if ( instant == null )
        {
            throw new NullPointerException();
        }

        try
        {
            return EDIR_TIMESTAMP_FORMATTER.format( instant.atZone( ZoneOffset.UTC ) );
        }
        catch ( DateTimeParseException e )
        {
            throw new IllegalArgumentException( "unable to format zulu time-string: " + e.getMessage() );
        }
    }

    /**
     * Convert the commonly used eDirectory zulu time string to java Date object.
     * See the <a href="http://developer.novell.com/documentation/ndslib/schm_enu/data/sdk5701.html">eDirectory Time attribute syntax definition</a> for more details.
     *
     * @param input a date string in the format of "yyyyMMddHHmmss'Z'", for example "19941216103200Z"
     * @return A Date object representing the string date
     * @throws IllegalArgumentException if dateString is incorrectly formatted
     */
    public static Instant convertZuluToInstant( final String input )
    {
        if ( input == null )
        {
            throw new NullPointerException();
        }

        try
        {
            final LocalDateTime localDateTime = LocalDateTime.parse( input, EDIR_TIMESTAMP_FORMATTER );
            final ZonedDateTime zonedDateTime = localDateTime.atZone( ZoneOffset.UTC );
            return Instant.from( zonedDateTime );
        }
        catch ( DateTimeParseException e )
        {
            throw new IllegalArgumentException( "unable to parse zulu time-string: " + e.getMessage() );
        }
    }

    static boolean convertStrToBoolean( final String string )
    {
        return StringHelper.convertStrToBoolean( string );
    }

    static int convertStrToInt( final String string, final int defaultValue )
    {
        if ( string == null )
        {
            return defaultValue;
        }

        try
        {
            return Integer.parseInt( string );
        }
        catch ( Exception e )
        {
            return defaultValue;
        }
    }

    /**
     * Creates a new user entry in the ldap directory.  A new "inetOrgPerson" object is created in the
     * ldap directory.  Generally, calls to this method will typically be followed by a call to the returned
     * {@link com.novell.ldapchai.ChaiUser}'s write methods to add additional data to the ldap user entry.
     *
     * @param userDN   the new userDN.
     * @param sn       the last name of
     * @param provider a ldap provider be used to create the group.
     * @return an instance of the ChaiUser entry
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the directory server(s) are unavailable
     */
    public static ChaiUser createUser( final String userDN, final String sn, final ChaiProvider provider )
            throws ChaiOperationException, ChaiUnavailableException
    {
        final Map<String, String> createAttributes = new HashMap<>();

        createAttributes.put( ChaiConstant.ATTR_LDAP_SURNAME, sn );

        provider.createEntry( userDN, ChaiConstant.OBJECTCLASS_BASE_LDAP_USER, createAttributes );

        //lets create a user object
        return provider.getEntryFactory().newChaiUser( userDN );
    }

    private static ChaiEntry findPartitionRoot( final ChaiEntry theEntry )
            throws ChaiUnavailableException, ChaiOperationException
    {
        ChaiEntry loopEntry = theEntry;

        while ( loopEntry != null )
        {
            final Set<String> objClasses = loopEntry.readMultiStringAttribute( ChaiConstant.ATTR_LDAP_OBJECTCLASS );
            if ( objClasses.contains( ChaiConstant.OBJECTCLASS_BASE_LDAP_PARTITION ) )
            {
                return loopEntry;
            }
            loopEntry = loopEntry.getParentEntry();
        }
        return null;
    }

    /**
     * Find the appropriate password policy for the given user.  The ChaiProvider
     * must have appropriate rights to read the policy and policy assignment attributes.
     *
     * @param theUser The user to find the the password policy for
     * @return A valid PasswordPolicy for the user
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the ldap server(s) are not available
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     * @see com.novell.ldapchai.ChaiUser#getPasswordPolicy()
     */
    public static ChaiPasswordPolicy readPasswordPolicy( final ChaiUser theUser )
            throws ChaiUnavailableException, ChaiOperationException
    {
        return UserPasswordPolicyReader.readPasswordPolicy( theUser );
    }

    /**
     * Remove a group membership for the supplied user and group.  This implementation
     * takes care of all four attributes used in eDirectory static group associations:
     * <ul>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_GROUP_MEMBERSHIP}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_MEMBER}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_SECURITY_EQUALS}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_EQUIVALENT_TO_ME}</li>
     * </ul>
     *
     * @param user  A valid {@code ChaiUser}
     * @param group A valid {@code ChaiGroup}
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the ldap server(s) are not available
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     */
    public static void removeGroupMembership( final ChaiUser user, final ChaiGroup group )
            throws ChaiOperationException, ChaiUnavailableException
    {
        if ( user == null )
        {
            throw new NullPointerException( "user cannot be null" );
        }

        if ( group == null )
        {
            throw new NullPointerException( "group cannot be null" );
        }

        //Delete the attribs off of the user
        user.deleteAttribute( ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, group.getEntryDN() );
        user.deleteAttribute( ChaiConstant.ATTR_LDAP_SECURITY_EQUALS, group.getEntryDN() );

        //Delete the attribs off of the group
        group.deleteAttribute( ChaiConstant.ATTR_LDAP_MEMBER, user.getEntryDN() );
        group.deleteAttribute( ChaiConstant.ATTR_LDAP_EQUIVALENT_TO_ME, user.getEntryDN() );
    }


    /**
     * Add a group membership for the supplied user and group.  This implementation
     * takes care of all four attributes used in eDirectory static group associations:
     * <ul>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_GROUP_MEMBERSHIP}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_MEMBER}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_SECURITY_EQUALS}</li>
     * <li>{@link com.novell.ldapchai.ChaiConstant#ATTR_LDAP_EQUIVALENT_TO_ME}</li>
     * </ul>
     *
     * @param user  A valid {@code ChaiUser}
     * @param group A valid {@code ChaiGroup}
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the ldap server(s) are not available
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     */
    public static void writeGroupMembership( final ChaiUser user, final ChaiGroup group )
            throws ChaiOperationException, ChaiUnavailableException
    {
        if ( user == null )
        {
            throw new NullPointerException( "user cannot be null" );
        }

        if ( group == null )
        {
            throw new NullPointerException( "group cannot be null" );
        }

        user.addAttribute( ChaiConstant.ATTR_LDAP_GROUP_MEMBERSHIP, group.getEntryDN() );
        user.addAttribute( ChaiConstant.ATTR_LDAP_SECURITY_EQUALS, group.getEntryDN() );

        group.addAttribute( ChaiConstant.ATTR_LDAP_MEMBER, user.getEntryDN() );
        group.addAttribute( ChaiConstant.ATTR_LDAP_EQUIVALENT_TO_ME, user.getEntryDN() );
    }

    private EdirEntries()
    {
    }

    private static final class UserPasswordPolicyReader
    {
        private static final Set<String> TRADITIONAL_PASSWORD_ATTRIBUTES;
        private static final SearchHelper NSPM_ENTRY_SEARCH_HELPER = new SearchHelper();

        static
        {
            {
                final Set<String> tempSet = new HashSet<>();
                tempSet.add( ChaiConstant.ATTR_LDAP_PASSWORD_MINIMUM_LENGTH );
                tempSet.add( ChaiConstant.ATTR_LDAP_PASSWORD_EXPIRE_INTERVAL );
                tempSet.add( ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_PASSWORD_UNIQUE_REQUIRED );
                TRADITIONAL_PASSWORD_ATTRIBUTES = Collections.unmodifiableSet( tempSet );
            }

            final Set<String> nspmPasswordAttributes;
            {
                final Set<String> tempSet = new HashSet<>();
                for ( final NspmPasswordPolicy.Attribute attr : NspmPasswordPolicy.Attribute.values() )
                {
                    tempSet.add( attr.getLdapAttribute() );
                }
                nspmPasswordAttributes = Collections.unmodifiableSet( tempSet );
            }

            {
                NSPM_ENTRY_SEARCH_HELPER.setSearchScope( SearchScope.BASE );
                NSPM_ENTRY_SEARCH_HELPER.setAttributes( nspmPasswordAttributes );
            }
        }


        static ChaiPasswordPolicy readPasswordPolicy( final ChaiUser theUser )
                throws ChaiUnavailableException, ChaiOperationException
        {
            ChaiPasswordPolicy pwordPolicy = DefaultChaiPasswordPolicy.createDefaultChaiPasswordPolicy();

            boolean usedUniversalPolicy = false;

            try
            {
                // fetch the user's associated password policy
                final ChaiEntry policyEntry = findNspmPolicyForUser( theUser );
                if ( policyEntry != null )
                {
                    pwordPolicy = new NspmPasswordPolicyImpl( policyEntry.getEntryDN(), policyEntry.getChaiProvider() );

                    // check to see if the advanced rules on the password policy are "enabled"
                    if ( pwordPolicy.getRuleHelper().isPolicyEnabled() )
                    {
                        // we've got a policy, and rules are enabled, now read the policy.
                        LOGGER.trace( () -> "using active universal password policy for user " + theUser.getEntryDN() + " at " + policyEntry.getEntryDN() );
                        usedUniversalPolicy = true;
                    }
                    else
                    {
                        LOGGER.debug( () -> "ignoring unenabled nspm password policy for user " + theUser.getEntryDN() + " at " + policyEntry.getEntryDN() );
                    }
                }
            }
            catch ( ChaiOperationException e )
            {
                LOGGER.error( () -> "ldap error reading universal password policy: " + e.getMessage() );
                throw e;
            }

            // if there is no universal password policy then fall back to reading user object attrs
            if ( !usedUniversalPolicy )
            {
                try
                {
                    pwordPolicy = readTraditionalPasswordRules( theUser );
                    LOGGER.trace( () -> "read traditional (non-nmas) password attributes from user entry " + theUser.getEntryDN() );
                }
                catch ( ChaiOperationException e )
                {
                    LOGGER.error( () -> "ldap error reading traditional password policy: " + e.getMessage() );
                }
            }

            return pwordPolicy;
        }

        private static ChaiEntry findNspmPolicyForUser( final ChaiUser theUser )
                throws ChaiUnavailableException, ChaiOperationException
        {
            final boolean useNmasSetting = theUser.getChaiProvider().getChaiConfiguration().getBooleanSetting( ChaiSetting.EDIRECTORY_ENABLE_NMAS );
            final ChaiEntryFactory chaiEntryFactory = theUser.getChaiProvider().getEntryFactory();

            if ( useNmasSetting )
            {
                final GetPwdPolicyInfoRequest request = new GetPwdPolicyInfoRequest();
                request.setObjectDN( theUser.getEntryDN() );
                final ExtendedResponse response = theUser.getChaiProvider().extendedOperation( request );
                if ( response != null )
                {
                    final GetPwdPolicyInfoResponse polcyInfoResponse = ( GetPwdPolicyInfoResponse ) response;
                    final String policyDN = polcyInfoResponse.getPwdPolicyDNStr();
                    if ( policyDN != null )
                    {
                        return chaiEntryFactory.newChaiEntry( policyDN );
                    }
                }
                return null;
            }
            else
            {
                // look at user object first
                {
                    final String policyDN = theUser.readStringAttribute( ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_DN );
                    if ( policyDN != null && policyDN.length() > 0 )
                    {
                        return chaiEntryFactory.newChaiEntry( policyDN );
                    }
                }

                final ChaiEntry parentObject = theUser.getParentEntry();

                // look at parent next
                {
                    if ( parentObject != null )
                    {
                        final String policyDN = parentObject.readStringAttribute( ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_DN );
                        if ( policyDN != null && policyDN.length() > 0 )
                        {
                            return chaiEntryFactory.newChaiEntry( policyDN );
                        }
                    }
                }

                // look at partition root
                {
                    if ( parentObject != null )
                    {
                        final ChaiEntry partitonRoot = findPartitionRoot( parentObject );
                        if ( partitonRoot != null )
                        {
                            final String policyDN = partitonRoot.readStringAttribute( ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_DN  );
                            if ( policyDN != null && policyDN.length() > 0 )
                            {
                                return chaiEntryFactory.newChaiEntry( policyDN );
                            }
                        }
                    }
                }

                // look at policy object
                {
                    final ChaiEntry securityContainer = chaiEntryFactory.newChaiEntry( "cn=Security" );
                    final String loginPolicyDN = securityContainer.readStringAttribute( "sASLoginPolicyDN" );
                    if ( loginPolicyDN != null && loginPolicyDN.length() > 0 )
                    {
                        final ChaiEntry loginPolicy = chaiEntryFactory.newChaiEntry( loginPolicyDN );
                        final String policyDN = loginPolicy.readStringAttribute( ChaiConstant.ATTR_EDIR_PASSWORD_POLICY_DN  );
                        if ( policyDN != null && policyDN.length() > 0 )
                        {
                            return chaiEntryFactory.newChaiEntry( policyDN );
                        }
                    }
                }
            }
            return null;
        }

        /**
         * <p>Reads and applies the user's traditional (non-UP) password rules to the policy.</p>
         *
         * <p>If the user does not have an associated universal password policy, this
         * method can be used to read the old style password rules.</p>
         *
         * @param theUser a valid chaiUser instance
         * @return a PasswordPolicy instance representing the users policy
         * @throws ChaiUnavailableException If the ldap server(s) are not available
         * @throws ChaiOperationException   If there is an error during the operation
         */
        private static ChaiPasswordPolicy readTraditionalPasswordRules( final ChaiUser theUser )
                throws ChaiUnavailableException, ChaiOperationException
        {
            final Map<String, String> values = theUser.readStringAttributes( TRADITIONAL_PASSWORD_ATTRIBUTES );

            final int minLength = convertStrToInt( values.get( "passwordMinimumLength" ), 0 );
            final int expireInterval = convertStrToInt( values.get( "passwordExpirationInterval" ), 0 );
            final boolean uniqueRequired = convertStrToBoolean( values.get( "passwordUniqueRequired" ) );

            final Map<ChaiPasswordRule, String> policyMap = new LinkedHashMap<>();

            // default for legacy passwords;
            policyMap.put( ChaiPasswordRule.MaximumLength, String.valueOf( 16 ) );

            policyMap.put( ChaiPasswordRule.MinimumLength, String.valueOf( minLength ) );
            policyMap.put( ChaiPasswordRule.ExpirationInterval, String.valueOf( expireInterval ) );
            policyMap.put( ChaiPasswordRule.UniqueRequired, String.valueOf( uniqueRequired ) );

            //other defaults for non up-policy.
            policyMap.put( ChaiPasswordRule.AllowNumeric, String.valueOf( true ) );
            policyMap.put( ChaiPasswordRule.AllowSpecial, String.valueOf( true ) );
            policyMap.put( ChaiPasswordRule.CaseSensitive, String.valueOf( false ) );

            return DefaultChaiPasswordPolicy.createDefaultChaiPasswordPolicyByRule( policyMap );
        }
    }

    static String readGuid( final ChaiEntry entry )
            throws ChaiUnavailableException, ChaiOperationException
    {
        final byte[] st = entry.getChaiProvider().readMultiByteAttribute( entry.getEntryDN(), "guid" )[0];
        final BigInteger bigInt = new BigInteger( 1, st );
        return bigInt.toString( 16 );
    }
}
