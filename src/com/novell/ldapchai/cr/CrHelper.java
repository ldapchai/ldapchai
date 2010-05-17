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

package com.novell.ldapchai.cr;

import com.novell.ldapchai.util.ChaiLogger;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

class CrHelper {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger logger = ChaiLogger.getLogger(CrHelper.class.getName());


    private static final String NMAS_XML_ROOTNODE = "Challenges";
    private static final String NMAS_XML_ATTR_RANDOM_COUNT = "RandomQuestions";
    private static final String NMAS_XML_NODE_CHALLENGE = "Challenge";
    private static final String NMAS_XML_ATTR_TYPE = "Type";
    private static final String NMAS_XML_ATTR_DEFINE = "Define";
    private static final String NMAS_XML_ATTR_MIN_LENGTH = "MinLength";
    private static final String NMAS_XML_ATTR_MAX_LENGTH = "MaxLength";

// -------------------------- STATIC METHODS --------------------------

    static String csToNmasXML(final ChallengeSet cs, final String guidValue)
    {
        final Element rootElement = new Element(NMAS_XML_ROOTNODE);
        rootElement.setAttribute(NMAS_XML_ATTR_RANDOM_COUNT, String.valueOf(cs.getMinRandomRequired()));
        if (guidValue != null) {
            rootElement.setAttribute("GUID", guidValue);
        } else {
            rootElement.setAttribute("GUID", "0");
        }

        for (final Challenge challenge : cs.getChallenges()) {
            final Element loopElement = new Element(NMAS_XML_NODE_CHALLENGE);
            if (challenge.getChallengeText() != null) {
                loopElement.setText(challenge.getChallengeText());
            }

            if (challenge.isAdminDefined()) {
                loopElement.setAttribute(NMAS_XML_ATTR_DEFINE, "Admin");
            } else {
                loopElement.setAttribute(NMAS_XML_ATTR_DEFINE, "User");
            }

            if (challenge.isRequired()) {
                loopElement.setAttribute(NMAS_XML_ATTR_TYPE, "Required");
            } else {
                loopElement.setAttribute(NMAS_XML_ATTR_TYPE, "Random");
            }

            loopElement.setAttribute(NMAS_XML_ATTR_MIN_LENGTH, String.valueOf(challenge.getMinLength()));
            loopElement.setAttribute(NMAS_XML_ATTR_MAX_LENGTH, String.valueOf(challenge.getMaxLength()));

            rootElement.addContent(loopElement);
        }

        final XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getCompactFormat());

        return outputter.outputString(rootElement);
    }
}
