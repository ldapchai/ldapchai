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
    private static final String VERSION_SEPARATOR = ":";
    private static final VERSION DEFAULT_VERSION = VERSION.B;

    protected final String answerHash;
    protected final String salt;
    protected final int hashCount;
    protected final boolean caseInsensitive;
    protected final FormatType formatType;
    protected final VERSION version;

    enum VERSION {
        A, // original version had bug where only one iteration was ever actually performed regardless of hashCount value
        B, // nominal working version
    }

    static {
        final Map<FormatType,String> map = new HashMap<FormatType,String>();
        map.put(FormatType.MD5,"MD5");
        map.put(FormatType.SHA1,"SHA1");
        map.put(FormatType.SHA1_SALT,"SHA1");
        map.put(FormatType.SHA256_SALT,"SHA-256");
        map.put(FormatType.SHA512_SALT,"SHA-512");
        supportedFormats = Collections.unmodifiableMap(map);
    }

    HashSaltAnswer(
            final String answerHash,
            final String salt,
            final int hashCount,
            final boolean caseInsensitive,
            final FormatType formatType,
            final VERSION version
    ) {
        if (answerHash == null || answerHash.length() < 1) {
            throw new IllegalArgumentException("missing answerHash");
        }

        if (formatType == null || !supportedFormats.containsKey(formatType)) {
            throw new IllegalArgumentException("unsupported format type '" + (formatType == null ? "null" : formatType.toString() + "'"));
        }

        this.answerHash = answerHash;
        this.version = version;
        this.formatType = formatType;
        this.salt = salt;
        this.hashCount = hashCount;
        this.caseInsensitive = caseInsensitive;
    }

    HashSaltAnswer(final AnswerFactory.AnswerConfiguration answerConfiguration, final String answer) {
        this.hashCount = answerConfiguration.hashCount;
        this.caseInsensitive = answerConfiguration.caseInsensitive;
        this.formatType = answerConfiguration.formatType;
        this.version = DEFAULT_VERSION;

        if (answer == null || answer.length() < 1) {
            throw new IllegalArgumentException("missing answerHash text");
        }

        if (formatType == null || !supportedFormats.containsKey(formatType)) {
            throw new IllegalArgumentException("unsupported format type '" + (formatType == null ? "null" : formatType.toString() + "'"));
        }

        { // make hash
            final boolean includeSalt = formatType.toString().contains("SALT");
            final String casedAnswer = caseInsensitive ? answer.toLowerCase() : answer;
            this.salt = includeSalt ? generateSalt(32) : "";
            final String saltedAnswer = includeSalt ? salt + casedAnswer : casedAnswer;
            this.answerHash = hashValue(saltedAnswer);
        }

    }

    public Element toXml() {
        final Element answerElement = new Element(ChaiResponseSet.XML_NODE_ANSWER_VALUE);
        answerElement.setText(version.toString() + VERSION_SEPARATOR + answerHash);
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
        final String hashedTest = hashValue(saltedTest);
        return answerHash.equalsIgnoreCase(hashedTest);
    }

    protected String hashValue(final String input) {
        return doHash(input, hashCount, formatType, version);
    }

    static String doHash(
            final String input,
            final int hashCount,
            final FormatType formatType,
            final VERSION version
    )
            throws IllegalStateException
    {
        final String algorithm = supportedFormats.get(formatType);
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("unable to load " + algorithm + " message digest algorithm: " + e.getMessage());
        }


        byte[] hashedBytes = input.getBytes();
        switch (version) {
            case A:
                hashedBytes = md.digest(hashedBytes);
                return Base64Util.encodeBytes(hashedBytes);

            case B:
                for (int i = 0; i < hashCount; i++) {
                    hashedBytes = md.digest(hashedBytes);
                }
                return Base64Util.encodeBytes(hashedBytes);

            default:
                throw new IllegalStateException("unexpected version enum in hash method");
        }
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
        answerBean.setType(formatType);
        answerBean.setAnswerHash(version.toString() + VERSION_SEPARATOR + answerHash);
        answerBean.setCaseInsensitive(caseInsensitive);
        answerBean.setHashCount(hashCount);
        answerBean.setSalt(salt);
        return answerBean;
    }

    static class HashSaltAnswerFactory implements ImplementationFactory {
        public HashSaltAnswer newAnswer(
                final AnswerFactory.AnswerConfiguration answerConfiguration,
                final String answer
        ) {
            return new HashSaltAnswer(answerConfiguration, answer);
        }

        public Answer fromAnswerBean(final AnswerBean input, final String challengeText) {

            final String answerValue = input.getAnswerHash();

            if (answerValue == null || answerValue.length() < 1) {
                throw new IllegalArgumentException("missing answer value");
            }

            final String hashString;
            final VERSION version;
            if (answerValue.contains(VERSION_SEPARATOR)) {
                final String[] s = answerValue.split(VERSION_SEPARATOR);
                try {
                    version = VERSION.valueOf(s[0]);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("unsupported version type " + s[0]);
                }
                hashString = s[1];
            } else {
                version = VERSION.A;
                hashString = answerValue;
            }

            return new HashSaltAnswer(
                    hashString,
                    input.getSalt(),
                    input.getHashCount(),
                    input.isCaseInsensitive(),
                    input.getType(),
                    version
            );
        }

        public HashSaltAnswer fromXml(final Element element, final boolean caseInsensitive, final String challengeText) {
            final String answerValue = element.getText();

            if (answerValue == null || answerValue.length() < 1) {
                throw new IllegalArgumentException("missing answer value");
            }

            final String hashString;
            final VERSION version;
            if (answerValue.contains(VERSION_SEPARATOR)) {
                final String[] s = answerValue.split(VERSION_SEPARATOR);
                try {
                    version = VERSION.valueOf(s[0]);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("unsupported version type " + s[0]);
                }
                hashString = s[1];
            } else {
                version = VERSION.A;
                hashString = answerValue;
            }

            final String salt = element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_SALT) == null ? "" : element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_SALT).getValue();
            final String hashCount = element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_HASH_COUNT) == null ? "1" : element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_HASH_COUNT).getValue();
            int saltCount = 1;
            try { saltCount = Integer.parseInt(hashCount); } catch (NumberFormatException e) { /* noop */ }
            final String formatStr = element.getAttributeValue(ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT) == null ? "" : element.getAttributeValue(ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT);
            final FormatType formatType;
            try {
                formatType = FormatType.valueOf(formatStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown content format specified in xml format value: '" + formatStr + "'");
            }
            return new HashSaltAnswer(hashString,salt,saltCount,caseInsensitive,formatType,version);
        }
    }
}
