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


public enum CrSetting {
    /**
     * Case insensitive flag.  If true, the case of the responses will be ignored when tested.  Default is true.
     */
    CASE_INSENSITIVE("chai.crsetting.caseInsensitive", "true", Validator.BOOLEAN_VALIDATOR),


    /**
     * Allow duplicate response values to be used.  Default is false.
     */
    ALLOW_DUPLICATE_RESPONSES("chai.crsetting.allowDuplicateResponses", "false", Validator.BOOLEAN_VALIDATOR),

    /**
     * Setting key to control the ldap attribute name used when reading/writing Chai Challenge/Response formats.
     * <p/>
     * <i>Default: </i><b>carLicense</b>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     * @see com.novell.ldapchai.util.ConfigObjectRecord
     */
    CHAI_ATTRIBUTE_NAME("chai.cr.chai.attributeName", "carLicense", null),

    /**
     * Setting key to control if chai response sets are stored in "case insensitive" format.
     * <p/>
     * <i>Default: </i><b>true</b>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     */
    CHAI_CASE_INSENSITIVE("chai.cr.chai.caseInsensitive", "true", null),

    /**
     * Setting key to control the {@link com.novell.ldapchai.util.ConfigObjectRecord COR}
     * RecordType used when reading/writing Chai Challenge/Response formats.
     * <p/>
     * <i>Default: </i><b>0002</b>
     *
     * @see com.novell.ldapchai.cr.ChaiResponseSet
     * @see com.novell.ldapchai.util.ConfigObjectRecord
     */
    CHAI_RECORD_ID("chai.cr.chai.recordId", "0002", null),

    /**
     * Default mode for reading ChallengeResponse settings.
     * <p/>
     * <i>Default: </i><b>{@link com.novell.ldapchai.cr.CrMode#CHAI_SHA1_SALT}</b>
     *
     * @see com.novell.ldapchai.cr.CrMode
     */
    DEFAULT_READ_MODE("chai.cr.default.readMode", CrMode.CHAI_SHA1_SALT.toString(), new Validator() {
        public void validate(final String value)
        {
            if (CrMode.forString(value) == null) {
                throw new IllegalArgumentException("unknown mode");
            }
        }
    }),

    /**
     * Default mode for writing ChallengeResponse settings.
     * <p/>
     * <i>Default: </i><b>{@link com.novell.ldapchai.cr.CrMode#CHAI_SHA1_SALT}</b>
     *
     * @see com.novell.ldapchai.cr.CrMode
     */
    DEFAULT_WRITE_MODE("chai.cr.writeMode", CrMode.CHAI_SHA1_SALT.toString(), new Validator() {
        public void validate(final String value)
        {
            if (CrMode.forString(value) == null) {
                throw new IllegalArgumentException("unknown mode");
            }
        }
    });

// ------------------------------ FIELDS ------------------------------

    private final String key;
    private final String defaultValue;
    private final Validator validator;

// -------------------------- STATIC METHODS --------------------------

    /**
     *
     * For a given key, find the associated setting.  If no setting matches
     * the supplied key, null is returned.
     * @param key string value of the setting's key.
     * @return the setting associated witht the <i>key</i>, or <i>null</i>.
     * @see #getKey()
     */
    public static CrSetting forKey(final String key)
    {
        for (final CrSetting setting : CrSetting.values()) {
            if (setting.getKey().equals(key)) {
                return setting;
            }
        }
        return null;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    CrSetting(final String key, final String defaultValue, final Validator validator)
    {
        this.key = key;
        this.defaultValue = defaultValue;
        this.validator = validator;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * Returns the default value for this setting.  If no other value is configured, then the
     * default value is used
     *
     * @return the default value
     */
    public String getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Get the key name, suitable for use in a {@link java.util.Properties} instance
     *
     * @return key name
     */
    public String getKey()
    {
        return key;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Validates the syntactical structure of the value.  Useful for pre-testing a value to see
     * if it meets requirements.
     *
     * @param value the value to test
     * @throws IllegalArgumentException if the value is not syntactically correct
     */
    public void validateValue(final String value)
    {
        if (this.validator == null) {
            return;
        }
        this.validator.validate(value);
    }

// -------------------------- INNER CLASSES --------------------------

    private interface Validator extends Serializable {
        void validate(String value);

        static final Validator INTEGER_VALIDATOR = new Validator() {
            public void validate(final String value)
            {
                try {
                    Integer.parseInt(value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        };

        static final Validator BOOLEAN_VALIDATOR = new Validator() {
            public void validate(final String value)
            {
                try {
                    Boolean.parseBoolean(value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        };
    }

}
