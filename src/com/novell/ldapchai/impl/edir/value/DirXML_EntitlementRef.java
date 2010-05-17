/*
 * LDAP Chai API
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2010 The LDAP Chai Project
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

/**
 * An immutable wrapper for the value of an eDirectory DirXML-EntitlementRef value.
 * <p/>
 */
package com.novell.ldapchai.impl.edir.value;

import com.novell.ldapchai.provider.ChaiProvider;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.StringTokenizer;


/**
 * An immutable bean representation of an eDirectory DirXML-EntitlementRef value.
 *
 * @author Jason D. Rivard
 */
class DirXML_EntitlementRef implements Serializable {
// ----------------------------- CONSTANTS ----------------------------

    //@todo make this class public
    public static final String ASN_VALUE = "2.16.840.1.113719.1.14.4.1.2087";

// -------------------------- ENUMERATIONS --------------------------

    public enum State {
        REVOKED(0),  //@todo verify
        GRANTED(1);

        private int numValue;

        private State(final int intValue)
        {
            this.numValue = intValue;
        }

        public int getNumValue()
        {
            return numValue;
        }

        static State forNumValue(final int numValue)
        {
            for (final State s : values()) {
                if (numValue == s.numValue) {
                    return s;
                }
            }
            throw new IllegalArgumentException("unknown state for " + DirXML_EntitlementRef.class.getSimpleName() + " (" + numValue + ")");
        }
    }

// ------------------------------ FIELDS ------------------------------

    private String entitlementDN;
    private State state;
    private Document payload;

// --------------------------- CONSTRUCTORS ---------------------------

    public DirXML_EntitlementRef(final String value, final ChaiProvider provider)
    {
        if (value == null || value.length() < 1) {
            throw new NullPointerException("missing value");
        }

        final StringTokenizer st = new StringTokenizer(value, "#");
        this.entitlementDN = st.nextToken();
        final String stateString = st.nextToken();
        final int stateNumber = Integer.valueOf(stateString);
        this.state = State.forNumValue(stateNumber);
        if (st.hasMoreTokens()) {
            this.payload = convertStrToDoc(st.nextToken());
        } else {
            this.payload = null;
        }
    }

    private static Document convertStrToDoc(final String str)
    {
        final Reader xmlreader = new StringReader(str);
        final SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(xmlreader);
        } catch (JDOMException e) {
            e.printStackTrace();  //@todo
        } catch (IOException e) {
            e.printStackTrace();  //@todo
        }
        return doc;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getEntitlementDN()
    {
        return entitlementDN;
    }

    public State getState()
    {
        return state;
    }
}
