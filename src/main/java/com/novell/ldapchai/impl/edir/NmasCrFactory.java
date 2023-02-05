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

package com.novell.ldapchai.impl.edir;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.cr.AbstractResponseSet;
import com.novell.ldapchai.cr.ChaiChallenge;
import com.novell.ldapchai.cr.ChaiChallengeSet;
import com.novell.ldapchai.cr.Challenge;
import com.novell.ldapchai.cr.ChallengeSet;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicy;
import com.novell.ldapchai.impl.edir.entry.ext.DeleteLoginConfigRequest;
import com.novell.ldapchai.impl.edir.entry.ext.DeleteLoginConfigResponse;
import com.novell.ldapchai.impl.edir.entry.ext.NMASChallengeResponse;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.util.internal.ChaiLogger;
import com.novell.ldapchai.util.internal.StringHelper;
import org.jrivard.xmlchai.AccessMode;
import org.jrivard.xmlchai.XmlDocument;
import org.jrivard.xmlchai.XmlElement;
import org.jrivard.xmlchai.XmlFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;


/**
 * Utility class for reading and writing challenge response sets too and from eDirectory.  The functionality in this class
 * is enabled regardless of the configuration setting {@link com.novell.ldapchai.provider.ChaiSetting#EDIRECTORY_ENABLE_NMAS}.
 */
public class NmasCrFactory
{

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( NmasCrFactory.class );


    private static ChallengeSet readNmasAssignedChallengeSetPolicy(
            final ChaiProvider provider,
            final String challengeSetDN,
            final Locale locale,
            final String identifer
    )
            throws ChaiUnavailableException, ChaiOperationException, ChaiValidationException
    {
        if ( challengeSetDN == null || challengeSetDN.length() < 1 )
        {
            LOGGER.trace( () -> "challengeSetDN is null, return null for readNmasAssignedChallengeSetPolicy()" );
            return null;
        }

        final List<Challenge> challenges = new ArrayList<>();
        final ChaiEntry csSetEntry = provider.getEntryFactory().newChaiEntry( challengeSetDN );

        final Map<String, String> allValues = csSetEntry.readStringAttributes( Collections.emptySet() );

        final String requiredQuestions = allValues.get( "nsimRequiredQuestions" );
        final String randomQuestions = allValues.get( "nsimRandomQuestions" );

        try
        {
            if ( requiredQuestions != null && requiredQuestions.length() > 0 )
            {
                challenges.addAll( parseNmasChallengePolicyXML( requiredQuestions, locale ) );
            }
            if ( randomQuestions != null && randomQuestions.length() > 0 )
            {
                challenges.addAll( parseNmasChallengePolicyXML( randomQuestions, locale ) );
            }
        }
        catch ( IOException e )
        {
            LOGGER.debug( () -> "error reading NMAS challenge set policy: " + e );
        }

        final int minRandQuestions = StringHelper.convertStrToInt( allValues.get( "nsimNumberRandomQuestions" ), 0 );

        return new ChaiChallengeSet( challenges, minRandQuestions, locale, identifer );
    }

    /**
     * <p>Read the theUser's configured ChallengeSet from the directory.  Operations are performed according
     * to the ChaiConfiguration found by looking at the ChaiProvider underlying the ChaiEntry.  For example,
     * the setting {@link com.novell.ldapchai.provider.ChaiSetting#EDIRECTORY_ENABLE_NMAS} determines if NMAS calls
     * are used to discover a universal password c/r policy.</p>
     *
     * <p>This method is preferred over {@link #readAssignedChallengeSet(com.novell.ldapchai.ChaiUser)}, as it does not
     * require a (possibly unnecessary) call to read the user's assigned password policy.</p>
     *
     * @param provider       provider used for ldap communication
     * @param passwordPolicy the policy to examine to find a challenge set.
     * @param locale         desired retreival locale.  If the stored ChallengeSet is internationalized,
     *                       the appropriate localized strings will be returned.
     * @return A valid ChallengeSet if found, otherwise null.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws ChaiValidationException  when there is a logical problem with the challenge set data, such as more randoms required then exist
     */
    public static ChallengeSet readAssignedChallengeSet(
            final ChaiProvider provider,
            final ChaiPasswordPolicy passwordPolicy,
            final Locale locale
    )
            throws ChaiUnavailableException, ChaiOperationException, ChaiValidationException
    {
        final String challengeSetDN;

        try
        {
            challengeSetDN = ( ( NspmPasswordPolicy ) passwordPolicy ).getChallengeSetDN();
        }
        catch ( ClassCastException e )
        {
            LOGGER.trace( () -> "password policy is not an nmas password policy, unable to read challenge set policy" );
            return null;
        }

        if ( challengeSetDN == null )
        {
            LOGGER.trace( () -> "password policy does not have a challengeSetDN, return null for readAssignedChallengeSet()" );
            return null;
        }

        final String identifier = ( ( NspmPasswordPolicy ) passwordPolicy ).readStringAttribute( "nsimChallengeSetGUID" );
        return readNmasAssignedChallengeSetPolicy( provider, challengeSetDN, locale, identifier );
    }

    /**
     * This method will first read the user's assigned password challenge set policy.
     *
     * @param theUser ChaiUser to read policy for
     * @param locale  Desired locale
     * @return A valid ChallengeSet if found, otherwise null.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws ChaiValidationException  when there is a logical problem with the challenge set data, such as more randoms required then exist
     */
    public static ChallengeSet readAssignedChallengeSet( final ChaiUser theUser, final Locale locale )
            throws ChaiUnavailableException, ChaiOperationException, ChaiValidationException
    {
        final ChaiPasswordPolicy passwordPolicy = theUser.getPasswordPolicy();

        if ( passwordPolicy == null )
        {
            LOGGER.trace( () -> "user does not have an assigned password policy, return null for readAssignedChallengeSet()" );
            return null;
        }

        return readAssignedChallengeSet( theUser.getChaiProvider(), passwordPolicy, locale );
    }


    /**
     * This method will first read the user's assigned password challenge set policy.
     *
     * @param theUser ChaiUser to read policy for
     * @return A valid ChallengeSet if found, otherwise null.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws ChaiValidationException  when there is a logical problem with the challenge set data, such as more randoms required then exist
     */
    public static ChallengeSet readAssignedChallengeSet( final ChaiUser theUser )
            throws ChaiUnavailableException, ChaiOperationException, ChaiValidationException
    {
        return readAssignedChallengeSet( theUser, Locale.getDefault() );
    }

    public static boolean writeResponseSet( final NmasResponseSet responseSet )
            throws ChaiUnavailableException, ChaiOperationException
    {
        return responseSet.write();
    }

    public static void clearResponseSet( final ChaiUser theUser )
            throws ChaiUnavailableException, ChaiOperationException
    {
        final ChaiProvider provider = theUser.getChaiProvider();

        final DeleteLoginConfigRequest request = new DeleteLoginConfigRequest();
        request.setObjectDN( theUser.getEntryDN() );
        request.setTag( NMASChallengeResponse.CHALLENGE_RESPONSE_QUESTIONS_TAG );
        request.setMethodID( NMASChallengeResponse.getMethodId() );
        request.setMethodIDLen( NMASChallengeResponse.getMethodId().length * 4 );

        final DeleteLoginConfigResponse response = ( DeleteLoginConfigResponse ) provider.extendedOperation( request );
        if ( response != null && response.getNmasRetCode() != 0 )
        {
            final String errorMsg = "nmas error clearing loginResponseConfig: " + response.getNmasRetCode();
            LOGGER.debug( () -> errorMsg );
            throw new ChaiOperationException( errorMsg, ChaiError.UNKNOWN );
        }
    }


    public static NmasResponseSet readNmasResponseSet( final ChaiUser user )
            throws ChaiUnavailableException, ChaiValidationException
    {
        return NmasResponseSet.readNmasUserResponseSet( user );
    }

    public static NmasResponseSet newNmasResponseSet(
            final Map<Challenge, String> crMap,
            final Locale locale,
            final int minRandom,
            final ChaiUser theUser,
            final String csIdentifier
    )
            throws ChaiValidationException
    {
        return new NmasResponseSet(
                crMap,
                locale,
                minRandom,
                AbstractResponseSet.STATE.NEW,
                theUser,
                csIdentifier
        );
    }

    static List<Challenge> parseNmasChallengePolicyXML( final String str, final Locale locale )
            throws IOException
    {
        final List<Challenge> returnList = new ArrayList<>();

        final XmlDocument doc = XmlFactory.getFactory().parseString( str, AccessMode.IMMUTABLE );
        final boolean required = doc.getRootElement().getName().equals( "RequiredQuestions" );


        for ( final XmlElement loopQ : doc.evaluateXpathToElements( "//Question" ) )
        {
            final int maxLength = StringHelper.convertStrToInt( loopQ.getAttribute( "MaxLength" ).orElse( "" ), 255 );
            final int minLength = StringHelper.convertStrToInt( loopQ.getAttribute( "MinLength" ).orElse( "" ), 1 );

            final String challengeText = readDisplayString( loopQ, locale );

            final Challenge challenge = new ChaiChallenge( required, challengeText, minLength, maxLength, true, 0, false );
            returnList.add( challenge );
        }

        for ( final XmlElement loopQ : doc.evaluateXpathToElements( "//UserDefined" ) )
        {
            final int maxLength = StringHelper.convertStrToInt( loopQ.getAttribute( "MaxLength" ).orElse( "" ), 255 );
            final int minLength = StringHelper.convertStrToInt( loopQ.getAttribute( "MinLength" ).orElse( "" ), 1 );

            final Challenge challenge = new ChaiChallenge( required, null, minLength, maxLength, false, 0, false );
            returnList.add( challenge );
        }

        return returnList;
    }

    private static String readDisplayString( final XmlElement questionElement, final Locale locale )
    {
        // see if the node has any localized displays.
        final List<XmlElement> displayChildren = questionElement.getChildren( "display" );

        // if no locale specified, or if no localized text is available, just use the default.
        if ( locale == null || displayChildren == null || displayChildren.size() < 1 )
        {
            return questionElement.getText().orElseThrow( () -> new IllegalArgumentException( "missing display text" ) );
        }

        // convert the xml 'display' elements to a map of locales/strings
        final Map<Locale, String> localizedStringMap = new HashMap<>();
        for ( final XmlElement loopDisplayChild : displayChildren )
        {
            final Optional<String> localeAttr = loopDisplayChild.getAttribute( "lang" );
            if ( localeAttr.isPresent() )
            {
                /*
                final String localeStr = localeAttr.getValue();
                final String displayStr = loopDisplay.getText();
                final Locale localeKey = parseLocaleString( localeStr );
                localizedStringMap.put( localeKey, displayStr );

                 */
            }
        }

        final Locale matchedLocale = localeResolver( locale, localizedStringMap.keySet() );

        if ( matchedLocale != null )
        {
            return localizedStringMap.get( matchedLocale );
        }

        // none found, so just return the default string.
        return questionElement.getText().orElseThrow( () -> new IllegalArgumentException( "missing display text" ) );
    }

    private static Locale localeResolver( final Locale desiredLocale, final Collection<Locale> localePool )
    {
        if ( desiredLocale == null || localePool == null || localePool.isEmpty() )
        {
            return null;
        }

        for ( final Locale loopLocale : localePool )
        {
            if ( loopLocale.getLanguage().equalsIgnoreCase( desiredLocale.getLanguage() ) )
            {
                if ( loopLocale.getCountry().equalsIgnoreCase( desiredLocale.getCountry() ) )
                {
                    if ( loopLocale.getVariant().equalsIgnoreCase( desiredLocale.getVariant() ) )
                    {
                        return loopLocale;
                    }
                }
            }
        }

        for ( final Locale loopLocale : localePool )
        {
            if ( loopLocale.getLanguage().equalsIgnoreCase( desiredLocale.getLanguage() ) )
            {
                if ( loopLocale.getCountry().equalsIgnoreCase( desiredLocale.getCountry() ) )
                {
                    return loopLocale;
                }
            }
        }

        for ( final Locale loopLocale : localePool )
        {
            if ( loopLocale.getLanguage().equalsIgnoreCase( desiredLocale.getLanguage() ) )
            {
                return loopLocale;
            }
        }

        final Locale defaultLocale = parseLocaleString( "" );
        if ( localePool.contains( defaultLocale ) )
        {
            return defaultLocale;
        }

        return null;
    }


    private static Locale parseLocaleString( final String localeString )
    {
        if ( localeString == null )
        {
            return new Locale( "" );
        }

        final StringTokenizer st = new StringTokenizer( localeString, "_" );

        if ( !st.hasMoreTokens() )
        {
            return new Locale( "" );
        }

        final String language = st.nextToken();
        if ( !st.hasMoreTokens() )
        {
            return new Locale( language );
        }

        final String country = st.nextToken();
        if ( !st.hasMoreTokens() )
        {
            return new Locale( language, country );
        }

        final String variant = st.nextToken( "" );
        return new Locale( language, country, variant );
    }

}
