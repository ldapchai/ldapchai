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

import com.novell.ldapchai.cr.bean.ChallengeBean;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A representation of the responses part of challenge/response set.  {@code RespnseSet}s include an embedded {@link com.novell.ldapchai.cr.ChallengeSet}.
 * This means that a full copy of the challenges are embedded at the response set, and a seperate challenge set instance
 * is not required to test responses.
 * <p/>
 * Response sets are associated to a user.
 * <p/>
 * The underlying implementation may pull all response information into memory at time of
 * creation, or there may be directory communication required for operation of methods on this
 * interface.
 * <p/>
 * Instances of {@code ResponseSet} can be created or read using {@link ChaiCrFactory}.
 *
 * @author Jason D. Rivard
 */
public interface ResponseSet {
// ------------------------ CANONICAL METHODS ------------------------

    /**
     * Get a debug-friendly representation of this {@code ResponseSet}.
     *
     * @return A string suitable for debug logging
     */
    String toString();

// -------------------------- OTHER METHODS --------------------------

    /**
     * Get a {@link com.novell.ldapchai.cr.ChallengeSet} that governs the responses
     * in this ChallengeSet
     *
     * @return Get the ChallengeSet embedded in this ResponseSet.
     */
    ChallengeSet getChallengeSet()
            throws ChaiValidationException;

    ChallengeSet getPresentableChallengeSet()
            throws ChaiValidationException;

    /**
     * Tests the {@code ResponseSet} to see if it meets the requirements of the supplied {@link com.novell.ldapchai.cr.ChallengeSet}.
     * This method does not test any response values.
     * <p/>
     * A typical use case for this method is to validate if an existing, stored {@code com.novell.ldapchai.cr.ResponseSet} of
     * a user satisfies a current {@code com.novell.ldapchai.cr.ChallengeSet} policy.
     *
     * @param challengeSet A valid {@code ChallengeSet}
     * @return true if this {@code ResponseSet} meets the requirements of the challenge set.
     * @throws ChaiValidationException if this response set does not meet the requirements of the challenge set
     */
    boolean meetsChallengeSetRequirements(ChallengeSet challengeSet)
            throws ChaiValidationException;

    /**
     * Get the literal string value of this {@code ResponseSet}.  Not all types of ResponseSet's support
     * this operation, in which case a {@link UnsupportedOperationException} will be thrown if the implementation
     * doesn't support a string representation.
     *
     * @return The string representation of this {@code ResponseSet}
     * @throws UnsupportedOperationException if the implementation does not support a string view of the response set.
     */
    String stringValue()
            throws UnsupportedOperationException, ChaiOperationException;

    /**
     * Test the returned response set.
     * <p/>
     * <b>Note:</b> There is no implementation (yet) for testing NMAS response sets.  Attempting
     * to test an NMAS ResponseSet will throw an {@link UnsupportedOperationException}.
     *
     * @param responseTest a map containing {@code Challenge}s as keys and String response values to use for testing.
     * @return true if the responses pass the test
     * @throws ChaiUnavailableException      If the directory server(s) are unavailable
     * @throws UnsupportedOperationException if the implementation does not support a test operation.
     */
    boolean test(Map<Challenge, String> responseTest)
            throws ChaiUnavailableException;

    /**
     * Get the locale of the response set.  A response set is always stored with a single
     * locale.
     * @return the Locale used to save the response set.
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws ChaiOperationException   If there is an error during the write operation
     * @throws IllegalStateException    if this response set is not suitable for writing, for example, if it has already been written, or was obtained by reading from ldap
     */
    Locale getLocale()
            throws ChaiUnavailableException, IllegalStateException, ChaiOperationException;

    /**
     * Get the timestamp of the response.  Generally indicates when the responseset was created.
     * @return the Locale used to save the response set.
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @throws ChaiOperationException   If there is an error during the write operation
     * @throws IllegalStateException    if this response set is not suitable for writing, for example, if it has already been written, or was obtained by reading from ldap
     */
    Date getTimestamp()
            throws ChaiUnavailableException, IllegalStateException, ChaiOperationException;

    /**
     * Return the helpdesk challenge responses.  The helpdesk challenge response answers are stored in reversable format
     * to be used by helpdesk administrators.  The answers can be used to help verify the identity of users when authenticating
     * over the phone to a helpdesk or other 3rd party.
     * @return
     */

    Map<Challenge, String> getHelpdeskResponses();

    List<ChallengeBean> asChallengeBeans(boolean includeAnswers);

    List<ChallengeBean> asHelpdeskChallengeBeans(boolean includeAnswers);
}
