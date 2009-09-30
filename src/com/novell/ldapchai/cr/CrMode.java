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

package com.novell.ldapchai.cr;

/**
 * Mode for challenge/response operations.
 *
 * @see com.novell.ldapchai.cr discussion of challenge/response operations and modes
 */
public enum CrMode {
    /**
     * Novell eDirectory NMAS calls to handle challenge/response operations
     */
    NMAS(Type.NMAS),
    /**
     * Chai cleartext mode to handle challenge/response operations
     */
    CHAI_TEXT(Type.CHAI),
    /**
     * Chai SHA1 mode to handle challenge/response operations
     */
    CHAI_SHA1(Type.CHAI),
    /**
     * Chai salted SHA1 mode to handle challenge/response operations
     */
    CHAI_SHA1_SALT(Type.CHAI);

// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    enum Type {
        NMAS, CHAI
    }

// ------------------------------ FIELDS ------------------------------

    private Type type;

// -------------------------- STATIC METHODS --------------------------

    public static CrMode forString(final String value)
    {
        for (final CrMode mode : values()) {
            if (mode.toString().equals(value)) {
                return mode;
            }
        }
        return null;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    CrMode(final Type type)
    {
        this.type = type;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    Type getType()
    {
        return type;
    }
}

