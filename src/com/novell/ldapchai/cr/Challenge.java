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

import java.io.Serializable;

/**
 * Challenge data object, containing all the properties defined by a challenge.
 * <p/>
 * Instances of {@code Challenge} can be generated using {@link ChaiCrFactory}.
 * <p/>
 * {@code Challenge}s are mutable until they are locked.  Once locked, setters will throw an illegal state exception.
 *
 * @author Jason D. Rivard
 */
public interface Challenge extends Serializable {
// -------------------------- OTHER METHODS --------------------------

    /**
     * Get the text of the {@code Challenge} question.  Depending on the origin and type of the
     * challenge, the value may be null.
     *
     * @return A string containing the challenge text (question), or null if not available
     */
    public String getChallengeText();

    /**
     * Get the maximum length allowed for the response to this {@code Challege}.
     *
     * @return Maximum character length
     */
    public int getMaxLength();

    /**
     * Get the minimum length allowed for the response to this {@code Challege}.
     *
     * @return Minimum character length
     */
    public int getMinLength();

    /**
     * Get a boolean indicating if the question is defined by the administrator or
     * by the user.
     *
     * @return true if the question is defined by the administrator, false if defined
     *         by the user.
     */
    public boolean isAdminDefined();

    /**
     * Check if the {@code Challege} is locked.  Once locked, a {@code Challenge} is immutable.
     *
     * @return true if locked.
     */
    boolean isLocked();

    /**
     * Indicates if this {@code Challenge} is one which is required, or if it is one of the
     * random questions.
     *
     * @return true if the question is required, false if random.
     */
    public boolean isRequired();

    /**
     * Lock this {@code Challenge}.  Once locked, it is immutable.
     */
    void lock();

    /**
     * Set the challenge text.  Challenge text is only modifiable if the challenge is <i>not</i> locked, and if is not
     * admin defiled ({@link #isAdminDefined()}).
     *
     * @param challengeText A valid challenge text string.
     * @throws IllegalArgumentException if the challenge is admin defined
     * @throws IllegalStateException    if the challenge is locked
     */
    public void setChallengeText(String challengeText);

    public ChallengeBean asChallengeBean();
}
