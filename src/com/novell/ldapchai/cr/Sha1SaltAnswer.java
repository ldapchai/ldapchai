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

import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.internal.Base64Util;
import org.jdom.Element;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

class Sha1SaltAnswer implements Answer {
    private static ChaiLogger LOGGER = ChaiLogger.getLogger(Sha1SaltAnswer.class);

    private final String answerHash;
    private final String salt;
    private final int hashCount;
    private final boolean caseInsensitive;

    private Sha1SaltAnswer(final String answer, final String salt, final int saltCount, final boolean caseInsensitive) {
        if (answer == null || answer.length() < 1) {
            throw new IllegalArgumentException("missing answer text");
        }

        this.answerHash = answer;
        this.salt = salt;
        this.hashCount = saltCount;
        this.caseInsensitive = caseInsensitive;
    }

    public static Sha1SaltAnswer newResponse(final String answer, final int saltCount, final boolean caseInsensitive) {
        if (answer == null || answer.length() < 1) {
            throw new IllegalArgumentException("missing answerHash text");
        }

        final String salt = generateSalt(32);
        final String casedAnswer = caseInsensitive ? answer.toLowerCase() : answer;
        final String saltedAnswer = salt + casedAnswer;
        final String hashedAnswer = hashValue(saltedAnswer,saltCount);
        return new Sha1SaltAnswer(hashedAnswer,salt,saltCount,caseInsensitive);
    }

    public static Sha1SaltAnswer fromXml(final Element element, final boolean caseInsensitive) {
        final String answerValue = element.getText();
        final String salt = element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_SALT) == null ? "" : element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_SALT).getValue();
        final String saltCountStr = element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_HASH_COUNT) == null ? "1" : element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_HASH_COUNT).getValue();
        int saltCount = 1;
        try { saltCount = Integer.parseInt(saltCountStr); } catch (NumberFormatException e) { /* noop */ }
        return new Sha1SaltAnswer(answerValue,salt,saltCount,caseInsensitive);
    }

    public Element toXml() {
        final Element answerElement = new Element(ChaiResponseSet.XML_NODE_ANSWER_VALUE);
        answerElement.setText(answerHash);
        answerElement.setAttribute(ChaiResponseSet.XML_ATTRIBUTE_SALT,salt);
        answerElement.setAttribute(ChaiResponseSet.XNL_ATTRIBUTE_CONTENT_FORMAT, ChaiResponseSet.FormatType.SHA1_SALT.toString());
        if (hashCount >= 1) {
            answerElement.setAttribute(ChaiResponseSet.XML_ATTRIBUTE_HASH_COUNT,String.valueOf(hashCount));
        }
        return answerElement;
    }


    public boolean testAnswer(final String testResponse) {
        if (testResponse == null) {
            return false;
        }

        final String casedResponse = caseInsensitive ? testResponse.toLowerCase() : testResponse;
        final String saltedTest = salt + casedResponse;
        final String hashedTest = hashValue(saltedTest, hashCount);
        return answerHash.equalsIgnoreCase(hashedTest);
    }

    private static String hashValue(final String rawResponse, final int saltCount)
            throws IllegalStateException
    {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("unable to load SHA1 hash algorithm: " + e.getMessage());
        }
        byte[] hashedAnswer = rawResponse.getBytes();
        for (int i = 0; i < saltCount; i++) {
            hashedAnswer = md.digest(rawResponse.getBytes());
        }
        return Base64Util.encodeBytes(hashedAnswer);
    }

    private static String generateSalt(final int length)
    {
        final SecureRandom random = new SecureRandom();
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ChaiResponseSet.SALT_CHARS.charAt(random.nextInt(ChaiResponseSet.SALT_CHARS.length())));
        }
        return sb.toString();
    }

}
