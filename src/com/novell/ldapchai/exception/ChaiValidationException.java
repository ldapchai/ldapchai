/*
 * LDAP Chai API
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009 Jason D. Rivard
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

package com.novell.ldapchai.exception;

/**
 * Validation exceptions.  Thrown when certain methods perform client side validations
 * for some reason.
 *
 * @author Jason D. Rivard
 */
public class ChaiValidationException extends ChaiException {
// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    /**
     * Enumeration of validation errors
     */
    public enum VALIDATION_ERROR {
        NOT_ENOUGH_RANDOM_RESPONSES("NotEnoughRandom", "number of required responses greater then count of supplied random challenges"),
        TOO_FEW_RANDOM_RESPONSES("TooFewRandom", "number of responses does not meet minimum random requirement"),
        MISSING_REQUIRED_CHALLENGE_TEXT("MissingRequiredChallenge", "challenge text missing for challenge"),
        MISSING_REQUIRED_RESPONSE_TEXT("MissingRequiredResponse", "response text missing for challenge"),
        RESPONSE_TOO_SHORT("ResponseTooShort", "response text is too short for challenge"),
        RESPONSE_TOO_LONG("ResponseTooLong", "response text is too long for challenge"),
        TOO_FEW_CHALLENGES("TooFewChallenges", "too few challenges are required"),
        DUPLICATE_RESPONSES("DuplicateResponses", "multiple responses have the same value"),
        ;

        private String errorKey;
        private String debugDescription;

        private VALIDATION_ERROR(final String errorKey, final String debugDescription)
        {
            this.errorKey = errorKey;
            this.debugDescription = debugDescription;
        }

        public String getDebugDescription()
        {
            return debugDescription;
        }

        public String getErrorKey()
        {
            return errorKey;
        }
    }

// ------------------------------ FIELDS ------------------------------

    private ChaiValidationException.VALIDATION_ERROR validationError;
    private String validationField;

// --------------------------- CONSTRUCTORS ---------------------------

    public ChaiValidationException(final ChaiValidationException.VALIDATION_ERROR validationError, final String validationField)
    {
        super(validationError.getErrorKey(), ChaiErrorCode.UNKNOWN);
        this.validationError = validationError;
        this.validationField = validationField;
    }

    public ChaiValidationException(final ChaiValidationException.VALIDATION_ERROR validationError)
    {
        this(validationError, null);

    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public ChaiValidationException.VALIDATION_ERROR getValidationError()
    {
        return validationError;
    }

    public String getValidationField()
    {
        return validationField;
    }
}
