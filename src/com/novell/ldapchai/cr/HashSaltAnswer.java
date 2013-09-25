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

import com.novell.ldapchai.cr.bean.AnswerBean;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.internal.Base64Util;
import org.jdom2.Element;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class HashSaltAnswer implements Answer {
    private static final Map<FormatType,String> supportedFormats;

    private final String answerHash;
    private final String salt;
    private final int hashCount;
    private final boolean caseInsensitive;
    private final FormatType formatType;

    static {
        final Map<FormatType,String> map = new HashMap<FormatType,String>();
        map.put(FormatType.MD5,"MD5");
        map.put(FormatType.SHA1,"SHA1");
        map.put(FormatType.SHA1_SALT,"SHA1");
        map.put(FormatType.SHA256_SALT,"SHA-256");
        map.put(FormatType.SHA512_SALT,"SHA-512");
        supportedFormats = Collections.unmodifiableMap(map);
    }

    HashSaltAnswer(final String answerHash, final String salt, final int saltCount, final boolean caseInsensitive, final FormatType formatType) {
        if (answerHash == null || answerHash.length() < 1) {
            throw new IllegalArgumentException("missing answer text");
        }

        if (formatType == null || !supportedFormats.containsKey(formatType)) {
            throw new IllegalArgumentException("unsupported format type '" + formatType == null ? "null" : formatType.toString() + "'");
        }

        this.formatType = formatType;
        this.answerHash = answerHash;
        this.salt = salt;
        this.hashCount = saltCount;
        this.caseInsensitive = caseInsensitive;
    }

    public Element toXml() {
        final Element answerElement = new Element(ChaiResponseSet.XML_NODE_ANSWER_VALUE);
        answerElement.setText(answerHash);
        if (salt != null && salt.length() > 0) {
            answerElement.setAttribute(ChaiResponseSet.XML_ATTRIBUTE_SALT,salt);
        }
        answerElement.setAttribute(ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT, formatType.toString());
        if (hashCount > 1) {
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
        final String hashedTest = hashValue(saltedTest, hashCount, formatType);
        return answerHash.equalsIgnoreCase(hashedTest);
    }

    private static String hashValue(final String rawResponse, final int saltCount, final FormatType formatType)
            throws IllegalStateException
    {
        final String algorithm = supportedFormats.get(formatType);
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("unable to load " + algorithm + " message digest algorithm: " + e.getMessage());
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

    public AnswerBean asAnswerBean() {
        final AnswerBean answerBean = new AnswerBean();
        answerBean.setType(Answer.FormatType.SHA1_SALT);
        answerBean.setAnswerHash(answerHash);
        answerBean.setCaseInsensitive(caseInsensitive);
        answerBean.setHashCount(hashCount);
        answerBean.setSalt(salt);
        return answerBean;
    }

    static class HashSaltAnswerFactory implements ImplementationFactory {
        public HashSaltAnswer newAnswer(final AnswerFactory.AnswerConfiguration answerConfiguration, final String answer) {
            final int hashCount = answerConfiguration.hashCount;
            final boolean caseInsensitive = answerConfiguration.caseInsensitive;
            final FormatType formatType = answerConfiguration.formatType;

            if (answer == null || answer.length() < 1) {
                throw new IllegalArgumentException("missing answerHash text");
            }

            if (formatType == null || !supportedFormats.containsKey(formatType)) {
                throw new IllegalArgumentException("unsupported format type '" + formatType == null ? "null" : formatType.toString() + "'");
            }

            final boolean includeSalt = formatType.toString().contains("SALT");
            final String casedAnswer = caseInsensitive ? answer.toLowerCase() : answer;
            final String salt = includeSalt ? generateSalt(32) : "";
            final String saltedAnswer = includeSalt ? salt + casedAnswer : casedAnswer;
            final String hashedAnswer = hashValue(saltedAnswer,hashCount,formatType);
            return new HashSaltAnswer(hashedAnswer,salt,hashCount,caseInsensitive,formatType);
        }

        public Answer fromAnswerBean(AnswerBean input, String challengeText) {
            return new HashSaltAnswer(
                    input.getAnswerHash(),
                    input.getSalt(),
                    input.getHashCount(),
                    input.isCaseInsensitive(),
                    input.getType()
            );
        }

        public HashSaltAnswer fromXml(final Element element, final boolean caseInsensitive, final String challengeText) {
            final String answerValue = element.getText();
            final String salt = element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_SALT) == null ? "" : element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_SALT).getValue();
            final String hashCount = element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_HASH_COUNT) == null ? "1" : element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_HASH_COUNT).getValue();
            int saltCount = 1;
            try { saltCount = Integer.parseInt(hashCount); } catch (NumberFormatException e) { /* noop */ }
            final String formatStr = element.getAttributeValue(ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT) == null ? "" : element.getAttributeValue(ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT);
            final FormatType formatType;
            try {
                formatType = FormatType.valueOf(formatStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown xml content format specified: '" + formatStr + "'");
            }
            return new HashSaltAnswer(answerValue,salt,saltCount,caseInsensitive,formatType);
        }


    }
}
