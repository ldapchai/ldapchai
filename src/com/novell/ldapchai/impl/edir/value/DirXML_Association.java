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

package com.novell.ldapchai.impl.edir.value;

import java.io.Serializable;
import java.util.*;

/**
 * An immutable representation of an eDirectory DirXML-Attribute value.
 * <p/>
 * <b>Example value in ldap representation:</b>
 * <pre>
 * cn=driver1,cn=DriverSet,ou=orgUnit,o=org#1#USER_ID=1,table=USERS,schema=IDM
 *                 |                        |                |
 *              driverDN                  state            value
 * </pre>
 *
 * @author Jason D. Rivard
 */
public class DirXML_Association implements Serializable, Comparable {
// ----------------------------- CONSTANTS ----------------------------

    /**
     * ASN value of the <i>DirXML-Association</i> attribute.
     */
    public static final String ASN_VALUE = "2.16.840.1.113719.1.14.4.1.4";

// -------------------------- ENUMERATIONS --------------------------

    /**
     * State of the association, stored as an integer.
     */
    public enum State {
        /**
         * 0
         */
        DISABLED(0),
        /**
         * 1
         */
        PROCESSED(1),

        /**
         * 2
         */
        PENDING(2),

        /**
         * 3
         */
        MANUAL(3),

        /**
         * 4
         */
        MIGRATE(4),;

        private int numValue;

        private State(final int numValue)
        {
            this.numValue = numValue;
        }

        /**
         * Returns the numerical value of the state
         *
         * @return numerical value of the state
         */
        public int getNumValue()
        {
            return numValue;
        }

        public static State forIntValue(final int numValue)
        {
            for (final State loopEnum : values()) {
                if (loopEnum.getNumValue() == numValue) {
                    return loopEnum;
                }
            }
            throw new IllegalArgumentException("unknown state for " + DirXML_Association.class.getSimpleName() + " (" + numValue + ")");
        }
    }

// ------------------------------ FIELDS ------------------------------

    private static final String SEPERATOR = "#";

    private String driverDN;
    private State state;
    private String value;

// -------------------------- STATIC METHODS --------------------------

    public static DirXML_Association forStoredValue(final String storedValue)
    {
        return new DirXML_Association(storedValue);
    }

    public static Set<DirXML_Association> forStoredValues(final Collection<String> values)
    {
        return forStoredValues(values.toArray(new String[values.size()]));
    }

    public static Set<DirXML_Association> forStoredValues(final String... values)
    {
        final Set<DirXML_Association> returnSet = new HashSet<DirXML_Association>();
        for (final String value : values) {
            final DirXML_Association assocValue = new DirXML_Association(value);
            returnSet.add(assocValue);
        }
        return returnSet;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private DirXML_Association(final String storedValue)
    {
        if (storedValue == null || storedValue.length() < 1) {
            throw new NullPointerException("missing value");
        }

        final StringTokenizer st = new StringTokenizer(storedValue, SEPERATOR);

        try {
            this.driverDN = st.nextToken();

            {
                final String stateString = st.nextToken();
                final int stateNumber = Integer.valueOf(stateString);
                this.state = State.forIntValue(stateNumber);
            }

            if (st.hasMoreTokens()) {
                this.value = st.nextToken();
            } else {
                this.value = "";
            }
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("malformed value");
        }
    }

    public DirXML_Association(final String driverDN, final State state, final String value)
    {
        this.driverDN = driverDN;
        this.state = state;
        if (value == null) {
            this.value = "";
        } else {
            this.value = value;
        }
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * DN of the driver the assoication is referring to
     *
     * @return String in ldap DN format
     */
    public String getDriverDN()
    {
        return driverDN;
    }

    /**
     * State of the association
     *
     * @return state of the association
     */
    public State getState()
    {
        return state;
    }

    /**
     * Value of the association.  Syntax, format, and the actual value is driver specific.
     *
     * @return string value of the association
     */
    public String getValue()
    {
        return value;
    }

// ------------------------ CANONICAL METHODS ------------------------

    /**
     * Output the value of the association, in ldap format.  The results
     * are suitable for writing to ldap.
     *
     * @return ldap format of the association value.
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getDriverDN());
        sb.append('#');
        sb.append(this.getState().getNumValue());
        sb.append('#');
        sb.append(this.getValue());
        return sb.toString();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Comparable ---------------------

    /**
     * Sorts {@code DirXML_Association} values based on the string ordering of the
     * DN of the driver.
     *
     * @param o another {@code DirXML_Association} instance
     * @return -1 if less, 0 if equal, +1 if greater.
     */
    public int compareTo(final Object o)
    {
        if (o instanceof DirXML_Association) {
            return this.getDriverDN().compareTo(((DirXML_Association) o).getDriverDN());
        }
        throw new IllegalArgumentException("object must be of type " + this.getClass().getName());
    }
}
