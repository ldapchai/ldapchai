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

package com.novell.ldapchai.cr;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicy;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.StringHelper;
import org.jdom.JDOMException;

import java.io.IOException;
import java.util.*;

/**
 * Factory for generating {@code Challenge}s, {@code ChallengeSet}s and {@code ResponseSet}s.
 *
 * @author Jason D. Rivard
 */
public final class CrFactory {
// ----------------------------- CONSTANTS ----------------------------

    /**
     * Constant used to indicate user supplied question
     */
    public static final String USER_SUPPLIED_QUESTION = "%user%";

// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(CrFactory.class);

// -------------------------- STATIC METHODS --------------------------

    /**
     * Create a new challenge data object.
     *
     * @param required        if the challenge is a required or random.
     * @param challengeString The text of the ChallengeString.  May be null if not admin defined.
     * @param minLength       The minimum length of the challenge string
     * @param maxLength       The maximum length of the challenge string
     * @param adminDefined    If the challengeString is defined by admin or created by user.
     * @return A Challenge object suitable for constructing a new ChallengeSet.
     */
    public static Challenge newChallenge(
            final boolean required,
            final String challengeString,
            final int minLength,
            final int maxLength,
            final boolean adminDefined
    )
    {
        return new ChallengeImpl(
                required,
                challengeString,
                minLength,
                maxLength,
                adminDefined
        );
    }

    /**
     * Create a new Challenge Set.  No method is provided for writing this challenge set to the
     * directory, rather this is provided as a conveneice to use the Chai APIs in cases where
     * the Challenge Set is read by some other means (such as a configuration file).
     *
     * @param challenges   A collection of Challenge objects
     * @param locale       A local associated with the challenge set (may be null).
     * @param minimumRands Minimum random values required
     * @param identifer    A string identifier of the challenge set.
     * @return A functional ChallengeSet.
     * @throws com.novell.ldapchai.exception.ChaiValidationException
     *          when there is a logical problem with the challenge set data, such as more randoms required then exist
     */
    public static ChallengeSet newChallengeSet(
            final Collection<Challenge> challenges,
            final Locale locale,
            final int minimumRands,
            final String identifer
    )
            throws ChaiValidationException
    {
        return new ChallengeSetImpl(challenges, minimumRands, locale, identifer);
    }

    /**
     * Create a new ResponseSet.  The generated ResponseSet will be suitable for writing to the directory
     * by calling the ResponseSet's {@link ResponseSet#write(CrMode)} method.
     *
     * @param user                  User to associate the response set with
     * @param challengeResponseMap  A map containing Challenges as the key, and string responses for values
     * @param locale                The locale the response set is stored in
     * @param minimumRandomRequired Minimum random responses required
     * @param csIdentifier          Identifier from ChallengeSet (see {@link ChallengeSet#getIdentifier()}
     * @return A ResponseSet suitable for writing.
     * @throws com.novell.ldapchai.exception.ChaiValidationException
     *          when there is a logical problem with the response set data, such as more randoms required then exist
     */
    public static ResponseSet newResponseSet(
            final Map<Challenge, String> challengeResponseMap,
            final Locale locale,
            final int minimumRandomRequired,
            final ChaiUser user,
            final String csIdentifier
    )
            throws ChaiValidationException
    {
        final Properties properties = user.getChaiProvider().getChaiConfiguration().getCrSettings();
        final boolean caseInsensitive = StringHelper.convertStrToBoolean(properties.getProperty(CrSetting.CASE_INSENSITIVE.getKey()));
        final ChaiResponseSet.FormatType formatType = ChaiResponseSet.FormatType.forCrMode(CrMode.CHAI_SHA1_SALT);
        return new ChaiResponseSet(challengeResponseMap, locale, minimumRandomRequired, AbstractResponseSet.STATE.NEW, user, formatType, caseInsensitive, csIdentifier, new Date());
    }


    /**
     * A convenience wrapper for {@link #readAssignedChallengeSet(com.novell.ldapchai.provider.ChaiProvider, com.novell.ldapchai.ChaiPasswordPolicy)}.  This
     * method will first read the user's password policy using {@link com.novell.ldapchai.ChaiUser#getPasswordPolicy()},
     *
     * @param theUser ChaiUser to read policy for
     * @param locale  Desired locale
     * @return A valid ChallengeSet if found, otherwise null.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws ChaiValidationException  when there is a logical problem with the challenge set data, such as more randoms required then exist
     */
    public static ChallengeSet readAssignedChallengeSet(final ChaiUser theUser, final Locale locale)
            throws ChaiUnavailableException, ChaiOperationException, ChaiValidationException
    {
        final ChaiPasswordPolicy passwordPolicy = theUser.getPasswordPolicy();

        if (passwordPolicy == null) {
            LOGGER.trace("user does not have an assigned password policy, return null for readAssignedChallengeSet()");
            return null;
        }

        return readAssignedChallengeSet(theUser.getChaiProvider(), passwordPolicy, locale);
    }


    /**
     * A convenience wrapper for {@link #readAssignedChallengeSet(com.novell.ldapchai.provider.ChaiProvider, com.novell.ldapchai.ChaiPasswordPolicy)}.  This
     * method will first read the user's password policy using {@link com.novell.ldapchai.ChaiUser#getPasswordPolicy()},
     * and use the default locale.
     *
     * @param theUser ChaiUser to read policy for
     * @return A valid ChallengeSet if found, otherwise null.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws ChaiValidationException  when there is a logical problem with the challenge set data, such as more randoms required then exist
     */
    public static ChallengeSet readAssignedChallengeSet(final ChaiUser theUser)
            throws ChaiUnavailableException, ChaiOperationException, ChaiValidationException
    {
        return readAssignedChallengeSet(theUser, Locale.getDefault());
    }

    /**
     * A convience wrapper for {@link #readAssignedChallengeSet(com.novell.ldapchai.provider.ChaiProvider, com.novell.ldapchai.ChaiPasswordPolicy, Locale)}.  This
     * use the default locale.
     *
     * @param provider       provider used for ldap communication
     * @param passwordPolicy the policy to examine to find a challenge set.
     * @return A valid ChallengeSet if found, otherwise null.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws ChaiValidationException  when there is a logical problem with the challenge set data, such as more randoms required then exist
     */
    public static ChallengeSet readAssignedChallengeSet(final ChaiProvider provider, final ChaiPasswordPolicy passwordPolicy)
            throws ChaiUnavailableException, ChaiOperationException, ChaiValidationException
    {
        return readAssignedChallengeSet(provider, passwordPolicy, Locale.getDefault());
    }

    /**
     * Read the theUser's configured ChallengeSet from the directory.  Operations are performed according
     * to the ChaiConfiguration found by looking at the ChaiProvider underlying the ChaiEntry.  For example,
     * the setting {@link com.novell.ldapchai.provider.ChaiSetting#EDIRECTORY_ENABLE_NMAS} determines if NMAS calls
     * are used to discover a universal password c/r policy.
     * <p/>
     * This method is preferred over {@link #readAssignedChallengeSet(com.novell.ldapchai.ChaiUser)}, as it does not
     * require a (possibly unneccessary) call to read the user's assigned password policy.
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

        try {
            challengeSetDN = ((NspmPasswordPolicy)passwordPolicy).getChallengeSetDN();
        } catch (ClassCastException e) {
            LOGGER.trace("password policy is not an nmas password policy, unable to read challenge set policy");
            return null;
        }

        if (challengeSetDN == null) {
            LOGGER.trace("password policy does not have a challengeSetDN, return null for readAssignedChallengeSet()");
            return null;
        }

        final String identifier = ((NspmPasswordPolicy)passwordPolicy).readStringAttribute("nsimChallengeSetGUID");
        return readNmasAssignedChallengeSetPolicy(provider, challengeSetDN, locale, identifier);
    }

    private static ChallengeSet readNmasAssignedChallengeSetPolicy(
            final ChaiProvider provider,
            final String challengeSetDN,
            final Locale locale,
            final String identifer
    )
            throws ChaiUnavailableException, ChaiOperationException, ChaiValidationException
    {
        if (challengeSetDN == null || challengeSetDN.length() < 1) {
            LOGGER.trace("challengeSetDN is null, return null for readNmasAssignedChallengeSetPolicy()");
            return null;
        }

        final List<Challenge> challenges = new ArrayList<Challenge>();
        final ChaiEntry csSetEntry = ChaiFactory.createChaiEntry(challengeSetDN, provider);

        final Map<String,String> allValues = csSetEntry.readStringAttributes(Collections.<String>emptySet());

        final String requiredQuestions = allValues.get("nsimRequiredQuestions");
        final String randomQuestions = allValues.get("nsimRandomQuestions");

        try {
            if (requiredQuestions != null && requiredQuestions.length() > 0) {
                challenges.addAll(NmasResponseSet.parseNmasPolicyXML(requiredQuestions, locale));
            }
            if (randomQuestions != null && randomQuestions.length() > 0) {
                challenges.addAll(NmasResponseSet.parseNmasPolicyXML(randomQuestions, locale));
            }
        } catch (JDOMException e) {
            LOGGER.debug(e);
        } catch (IOException e) {
            LOGGER.debug(e);
        }

        final int minRandQuestions = StringHelper.convertStrToInt(allValues.get("nsimNumberRandomQuestions"), 0);

        return new ChallengeSetImpl(challenges, minRandQuestions, locale, identifer);
    }

    /**
     * {@link #readResponseSet(com.novell.ldapchai.ChaiUser, CrMode)} where
     * the provider's setting default mode is used.
     *
     * @param user ChaiUser to read responses for
     * @return A valid ResponseSet if found, otherwise null.
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws com.novell.ldapchai.exception.ChaiValidationException
     *                                  when there is a logical problem with the response set data, such as more randoms required then exist
     */
    public static ResponseSet readResponseSet(final ChaiUser user)
            throws ChaiUnavailableException, ChaiValidationException
    {
        final String strDefault = user.getChaiProvider().getChaiConfiguration().getCrSetting(CrSetting.DEFAULT_READ_MODE);
        final CrMode defaultMode = CrMode.forString(strDefault);

        return readResponseSet(user, defaultMode);
    }

    /**
     * Read the user's configured ResponseSet from the directory.
     * <p/>
     * A caller would typically use the returned {@code ResponseSet} for testing responses by calling the ResponseSet's
     * {@link ResponseSet#test(java.util.Map<com.novell.ldapchai.cr.Challenge,java.lang.String>)} method.
     *
     * @param user         ChaiUser to read responses for
     * @param responseMode Which type of response to read - note that any of the Chai types use the same exact method to read
     * @return A valid ResponseSet if found, otherwise null.
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws com.novell.ldapchai.exception.ChaiValidationException
     *                                  when there is a logical problem with the response set data, such as more randoms required then exist
     */
    public static ResponseSet readResponseSet(final ChaiUser user, final CrMode responseMode)
            throws ChaiUnavailableException, ChaiValidationException
    {
        ResponseSet returnSet = null;

        switch (responseMode.getType()) {
            case NMAS:
                returnSet = NmasResponseSet.readNmasUserResponseSet(user);
                break;

            case CHAI:
                returnSet = ChaiResponseSet.readUserResponseSet(user);
                break;
        }
        return returnSet;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private CrFactory()
    {
    }
}