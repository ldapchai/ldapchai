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

import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.util.ChaiLogger;

import java.util.*;

/**
 * Factory for generating {@code Challenge}s, {@code ChallengeSet}s and {@code ResponseSet}s.
 *
 * @author Jason D. Rivard
 */
public final class ChaiCrFactory {
    // ----------------------------- CONSTANTS ----------------------------

    /**
     * Constant used to indicate user supplied question
     */
    public static final String USER_SUPPLIED_QUESTION = "%user%";

// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(ChaiCrFactory.class);

// -------------------------- STATIC METHODS --------------------------

    /**
     * Create a new ResponseSet.  The generated ResponseSet will be suitable for writing to the directory
     * by calling the ResponseSet's {@link ResponseSet#write(ChaiResponseStorageMode)} method.
     *
     * @param user                  User to associate the response set with
     * @param challengeResponseMap  A map containing Challenges as the key, and string responses for values
     * @param locale                The locale the response set is stored in
     * @param minimumRandomRequired Minimum random responses required
     * @return A ResponseSet suitable for writing.
     * @throws com.novell.ldapchai.exception.ChaiValidationException
     *          when there is a logical problem with the response set data, such as more randoms required then exist
     */
    public static ChaiResponseSet newChaiResponseSet(
            final Map<Challenge, String> challengeResponseMap,
            final Locale locale,
            final int minimumRandomRequired,
            final ChaiConfiguration chaiConfiguration,
            final String csIdentifier
    )
            throws ChaiValidationException
    {
        final boolean caseInsensitive = chaiConfiguration.getBooleanSetting(ChaiSetting.CR_CASE_INSENSITIVE);
        final ChaiResponseSet.FormatType formatType = ChaiResponseSet.FormatType.valueOf(chaiConfiguration.getSetting(ChaiSetting.CR_DEFAULT_FORMAT_TYPE));
        return new ChaiResponseSet(challengeResponseMap, locale, minimumRandomRequired, AbstractResponseSet.STATE.NEW, formatType, caseInsensitive, csIdentifier, new Date());
    }

    public static boolean writeChaiResponseSet(
            final ChaiResponseSet chaiResponseSet,
            final ChaiUser chaiUser
    )
            throws ChaiUnavailableException, ChaiOperationException
    {
        return chaiResponseSet.write(chaiUser);
    }








// --------------------------- CONSTRUCTORS ---------------------------

    private ChaiCrFactory()
    {
    }

    /**
     * Read the user's configured ResponseSet from the directory.
     * <p/>
     * A caller would typically use the returned {@code ResponseSet} for testing responses by calling the ResponseSet's
     * {@link ResponseSet#test(java.util.Map<com.novell.ldapchai.cr.Challenge,java.lang.String>)} method.
     *
     * @param user         ChaiUser to read responses for
     * @return A valid ResponseSet if found, otherwise null.
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws com.novell.ldapchai.exception.ChaiValidationException
     *                                  when there is a logical problem with the response set data, such as more randoms required then exist
     */
    public static ChaiResponseSet readChaiResponseSet(final ChaiUser user)
            throws ChaiUnavailableException, ChaiValidationException, ChaiOperationException
    {
        return ChaiResponseSet.readUserResponseSet(user);
    }


}