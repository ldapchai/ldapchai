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

package com.novell.ldapchai.impl.edir;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.cr.AbstractResponseSet;
import com.novell.ldapchai.cr.ChaiChallengeSet;
import com.novell.ldapchai.cr.Challenge;
import com.novell.ldapchai.cr.ChallengeSet;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.impl.edir.entry.NspmPasswordPolicy;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.StringHelper;
import com.novell.security.nmas.jndi.ldap.ext.DeleteLoginConfigRequest;
import com.novell.security.nmas.jndi.ldap.ext.DeleteLoginConfigResponse;
import com.novell.security.nmas.mgmt.NMASChallengeResponse;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.util.*;

public class NmasCrFactory {

    final static private ChaiLogger LOGGER = ChaiLogger.getLogger(NmasCrFactory.class);


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

        return new ChaiChallengeSet(challenges, minRandQuestions, locale, identifer);
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

    public static boolean writeResponseSet(final NmasResponseSet responseSet)
            throws ChaiUnavailableException, ChaiOperationException {
        return responseSet.write();
    }

    public static void clearResponseSet(final ChaiUser theUser)
            throws ChaiUnavailableException, ChaiOperationException {
        final ChaiProvider provider = theUser.getChaiProvider();

        final DeleteLoginConfigRequest request = new DeleteLoginConfigRequest();
        request.setObjectDN(theUser.getEntryDN());
        request.setTag("ChallengeResponseQuestions");
        request.setMethodID(NMASChallengeResponse.METHOD_ID);
        request.setMethodIDLen(NMASChallengeResponse.METHOD_ID.length * 4);

        final DeleteLoginConfigResponse response = (DeleteLoginConfigResponse)provider.extendedOperation(request);
        if (response != null && response.getNmasRetCode() != 0) {
            final String errorMsg = "nmas error clearing loginResponseConfig: " + response.getNmasRetCode();
            LOGGER.debug(errorMsg);
            throw new ChaiOperationException(errorMsg, ChaiError.UNKNOWN);
        }
    }


    public static NmasResponseSet readNmasResponseSet(final ChaiUser user)
            throws ChaiUnavailableException, ChaiValidationException
    {
        return NmasResponseSet.readNmasUserResponseSet(user);
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
}
