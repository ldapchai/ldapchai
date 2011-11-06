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

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

/**
 * An instance of a challenge set to be used for challenge/response.   Instances of {@code ChallengeSet} can be
 * created or read using {@link ChaiCrFactory}.
 *
 * @author Jason D. Rivard
 */
public interface ChallengeSet extends Serializable {
// -------------------------- OTHER METHODS --------------------------

    /**
     * Helper method to return a filtered list of admin defined challenges.
     *
     * @return An unmodifiable collection of challenges
     */
    List<Challenge> getAdminDefinedChallenges();

    /**
     * Return a list of all the defined challenge texts.
     *
     * @return A list containing all defined challenge texts.
     */
    List<String> getChallengeTexts();

    /**
     * Get a collection of all the challenges associated with this set.
     *
     * @return all challenges associated with this set.
     */
    List<Challenge> getChallenges();

    /**
     * Get the Locale associated with this {@code ChallengeSet}.
     *
     * @return the locale used by the challenge set.
     */
    Locale getLocale();

    /**
     * Get the minimum number of challenges required to use this {@code ChallengeSet}.
     *
     * @return An int indicating the mimuimum number of challenges required
     */
    int getMinRandomRequired();

    /**
     * Helper method to return a filtered list of random challenges.
     *
     * @return An unmodifiable collection of challenges
     */
    List<Challenge> getRandomChallenges();

    /**
     * Helper method to return a filtered list of required challenges.
     *
     * @return An unmodifiable collection of challenges
     */
    List<Challenge> getRequiredChallenges();

    /**
     * Helper method to return a filtered list of user defined challenges.
     *
     * @return An unmodifiable collection of challenges
     */
    List<Challenge> getUserDefinedChallenges();

    /**
     * Computes the minimum number of responses that would satisfy this policy.
     *
     * Generally, this would be {@link #getMinRandomRequired()} + {@link #getRequiredChallenges()}{@code .size()};
     *
     * @return minimum number of responses that would satisfy this policy.
     */
    int minimumResponses();

    /**
     * Check to see if any of the underlying challenges are not locked.  If any challenge is not locked, then the
     * entire {@code ChallengeSet} is considered to be not locked.
     *
     * @return true if all contained {@code Challenge}s are locked.
     */
    boolean isLocked();

    /**
     * Lock this {@code ChallengeSet}, and any {@code Challenge} objects contained within.
     */
    void lock();

    /**
     * Get the identifier for this challenge set.  The value or existance of the identifier is defined by the
     * immplenetation
     * @return the value of the implementor
     */
    String getIdentifier();
}
