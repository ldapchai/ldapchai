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

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.ConfigObjectRecord;
import com.novell.ldapchai.util.internal.Base64Util;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChaiResponseSet extends AbstractResponseSet {
// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    public static enum FormatType {
        TEXT("TEXT"),
        SHA1("SHA1"),
        SHA1_SALT("SHA1_SALT");

        private final String format;

        private FormatType(final String format)
        {
            this.format = format;
        }

        public String toString()
        {
            return format;
        }
    }

// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(ChaiResponseSet.class.getName());

    private static final String SALT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String SALT_SEPERATOR = "___";

    private final static String XML_NODE_ROOT = "ResponseSet";
    private final static String XML_ATTRIBUTE_MIN_RANDOM_REQUIRED = "minRandomRequired";
    private final static String XML_ATTRIBUTE_LOCALE = "locale";

    private final static String XML_NODE_RESPONSE = "response";
    private final static String XML_NODE_CHALLENGE = "challenge";
    private final static String XML_NODE_ANSWER_VALUE = "answer";

    private final static String XML_ATTRIBUTE_VERSION = "version";
    private final static String XML_ATTRIBUTE_CHAI_VERSION = "chaiVersion";
    private final static String XML_ATTRIBUTE_ADMIN_DEFINED = "adminDefined";
    private final static String XML_ATTRIBUTE_REQUIRED = "required";
    private final static String XNL_ATTRIBUTE_CONTENT_FORMAT = "format";
    private final static String XML_ATTRIBUTE_SALT = "salt";
    private final static String XNL_ATTRIBUTE_MIN_LENGTH = "minLength";
    private final static String XNL_ATTRIBUTE_MAX_LENGTH = "maxLength";
    private final static String XML_ATTRIBUTE_CASE_INSENSITIVE = "caseInsensitive";
    private final static String XML_ATTRIBUTE_CHALLENGE_SET_IDENTIFER = "challengeSetID"; // identifier from challenge set.
    private final static String XML_ATTRIBUTE_TIMESTAMP = "time";

    private final static String VALUE_VERSION = "2";

    private final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    private final FormatType formatType;
    private final boolean caseInsensitive;

    static {
        DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("Zulu"));
    }

// -------------------------- STATIC METHODS --------------------------

    static ChaiResponseSet readUserResponseSet(final ChaiUser theUser)
            throws ChaiUnavailableException, ChaiValidationException, ChaiOperationException
    {
        final String corRecordIdentifer = theUser.getChaiProvider().getChaiConfiguration().getSetting(ChaiSetting.CR_CHAI_STORAGE_RECORD_ID);
        final String corAttribute = theUser.getChaiProvider().getChaiConfiguration().getSetting(ChaiSetting.CR_CHAI_STORAGE_ATTRIBUTE);

        final ChaiResponseSet returnVal;
        final List<ConfigObjectRecord> corList = ConfigObjectRecord.readRecordFromLDAP(theUser, corAttribute, corRecordIdentifer, null, null);
        String payload = "";
        if (!corList.isEmpty()) {
            final ConfigObjectRecord theCor = corList.get(0);
            payload = theCor.getPayload();
        }
        returnVal = parseChaiResponseSetXML(payload, theUser);

        if (returnVal == null) {
            return null;
        }

        // strip out any randoms beyond the minimum required.
        if (returnVal.getChallengeSet().getMinRandomRequired() > 0) {
            while (returnVal.getChallengeSet().getRandomChallenges().size() > returnVal.getChallengeSet().getMinRandomRequired()) {
                final List<Challenge> randChallenges = returnVal.getChallengeSet().getRandomChallenges();
                returnVal.crMap.remove(randChallenges.get(new SecureRandom().nextInt(randChallenges.size())));
            }
        }

        returnVal.isValid();

        return returnVal;
    }

    public static ChaiResponseSet parseChaiResponseSetXML(final String input, final ChaiUser user)
            throws ChaiValidationException
    {
        if (input == null || input.length() < 1) {
            return null;
        }

        final Map<Challenge, String> crMap = new LinkedHashMap<Challenge, String>();
        int minRandRequired = 0;
        Attribute localeAttr = null;
        ChaiResponseSet.FormatType respFormat = ChaiResponseSet.FormatType.TEXT;
        boolean caseInsensitive = false;
        String csIdentifier = null;
        Date timestamp = null;

        try {
            final SAXBuilder builder = new SAXBuilder();
            final Document doc = builder.build(new StringReader(input));
            final Element rootElement = doc.getRootElement();
            minRandRequired = rootElement.getAttribute(XML_ATTRIBUTE_MIN_RANDOM_REQUIRED).getIntValue();
            localeAttr = rootElement.getAttribute(XML_ATTRIBUTE_LOCALE);

            {
                final Attribute caseAttr = rootElement.getAttribute(XML_ATTRIBUTE_CASE_INSENSITIVE);
                if (caseAttr != null && caseAttr.getBooleanValue()) {
                    caseInsensitive = true;
                }
            }

            {
                final Attribute csIdentiferAttr = rootElement.getAttribute(XML_ATTRIBUTE_CHALLENGE_SET_IDENTIFER);
                if (csIdentiferAttr != null) {
                    csIdentifier = csIdentiferAttr.getValue();
                }
            }

            {
                final Attribute timeAttr = rootElement.getAttribute(XML_ATTRIBUTE_TIMESTAMP);
                if (timeAttr != null) {
                    final String timeStr = timeAttr.getValue();
                    try {
                        timestamp = DATE_FORMATTER.parse(timeStr);
                    } catch (ParseException e) {
                        LOGGER.error("unexpected error attempting to parse timestamp: " + e.getMessage());
                    }
                }
            }

            for (final Object o : rootElement.getChildren()) {
                final Element loopElement = (Element) o;

                final boolean required = loopElement.getAttribute(XML_ATTRIBUTE_REQUIRED).getBooleanValue();
                final boolean adminDefined = loopElement.getAttribute(XML_ATTRIBUTE_ADMIN_DEFINED).getBooleanValue();

                final String challengeText = loopElement.getChild(XML_NODE_CHALLENGE).getText();
                final int minLength = loopElement.getAttribute(XNL_ATTRIBUTE_MIN_LENGTH).getIntValue();
                final int maxLength = loopElement.getAttribute(XNL_ATTRIBUTE_MAX_LENGTH).getIntValue();


                final String format = loopElement.getChild(XML_NODE_ANSWER_VALUE).getAttribute(XNL_ATTRIBUTE_CONTENT_FORMAT).getValue();
                respFormat = ChaiResponseSet.FormatType.valueOf(format);

                String answer = loopElement.getChild(XML_NODE_ANSWER_VALUE).getText();
                if (respFormat == FormatType.SHA1_SALT) {
                    final String salt = loopElement.getChild(XML_NODE_ANSWER_VALUE).getAttribute(XML_ATTRIBUTE_SALT).getValue();
                    answer = salt + SALT_SEPERATOR + answer;
                }

                final Challenge newChallenge = new ChaiChallenge(required, challengeText, minLength, maxLength, adminDefined);
                crMap.put(newChallenge, answer);
            }
        } catch (JDOMException e) {
            LOGGER.debug("error parsing stored response record: " + e.getMessage());
        } catch (IOException e) {
            LOGGER.debug("error parsing stored response record: " + e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.debug("error parsing stored response record: " + e.getMessage());
        }

        Locale challengeLocale = Locale.getDefault();
        if (localeAttr != null) {
            challengeLocale = new Locale(localeAttr.getValue());
        }

        return new ChaiResponseSet(
                crMap,
                challengeLocale,
                minRandRequired,
                STATE.READ,
                respFormat,
                caseInsensitive,
                csIdentifier,
                timestamp);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    ChaiResponseSet(
            final Map<Challenge, String> crMap,
            final Locale locale,
            final int minimumRandomRequired,
            final STATE state,
            final FormatType formatType,
            final boolean caseInsensitive,
            final String csIdentifer,
            final Date timestamp
    )
            throws ChaiValidationException
    {
        super(crMap, locale, minimumRandomRequired, state, csIdentifer);
        this.formatType = formatType == null ? FormatType.SHA1_SALT : formatType;
        this.caseInsensitive = caseInsensitive;
        this.timestamp = timestamp;
    }

// ------------------------ CANONICAL METHODS ------------------------

    public String toString()
    {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append(", format(");
        sb.append(this.formatType);
        sb.append(")");
        return sb.toString();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ResponseSet ---------------------

    public String stringValue()
            throws UnsupportedOperationException
    {
        try {
            String stringResult = rsToChaiXML(this);
            stringResult = stringResult.replace("\r", "");
            stringResult = stringResult.replace("\n", "");
            return stringResult;
        } catch (ChaiValidationException e) {
            LOGGER.warn("error writing XML response set",e);
            throw new UnsupportedOperationException(e);
        }
    }

    public boolean test(final Map<Challenge, String> testResponses)
    {
        if (testResponses == null) {
            throw new IllegalArgumentException("responses required");
        }

        try {
            if (this.getChallengeSet().getRequiredChallenges().isEmpty() && this.minimumRandomRequired == 0) {
                throw new IllegalArgumentException("challenge set does not require any responses");
            }
        } catch (ChaiValidationException e) {
            LOGGER.warn("error",e);
            return false;
        }



        int correctRandoms = 0;
        for (final Challenge loopChallenge : this.crMap.keySet()) {
            final String proposedResponse = testResponses.get(loopChallenge);

            final boolean correct = testRepsonse(crMap.get(loopChallenge), proposedResponse);

            if (correct && !loopChallenge.isRequired()) {
                correctRandoms++;
            }

            if (!correct && loopChallenge.isRequired()) {
                return false;
            }
        }

        return correctRandoms >= minimumRandomRequired;
    }

// -------------------------- OTHER METHODS --------------------------

    private boolean testRepsonse(String actualResponse, String testResponse)
    {
        if (testResponse == null) {
            return false;
        }

        if (caseInsensitive) {
            testResponse = testResponse.toLowerCase();
        }

        switch (formatType) {
            case SHA1_SALT:
                // move the salt from the actual password (put there temporarily when read from XML)
                // to the test response

                if (actualResponse.contains(SALT_SEPERATOR)) {
                    final String salt = actualResponse.split(SALT_SEPERATOR)[0];
                    actualResponse = actualResponse.split(SALT_SEPERATOR)[1];
                    testResponse = salt + testResponse;
                }

                // continue on with sha1 method

            case SHA1:
                try {
                    final String encryptedTestResponse = hashValue(testResponse);
                    if (actualResponse.equals(encryptedTestResponse)) {
                        return true;
                    }
                } catch (NoSuchAlgorithmException e) {
                    LOGGER.warn("error encoding hash for answer: \"" + e.getMessage() + "\"");
                }
                break;

            case TEXT:
                if (actualResponse.equals(testResponse)) {
                    return true;
                }
                break;
        }
        return false;
    }

    private static String hashValue(final String rawResponse)
            throws NoSuchAlgorithmException
    {
        final MessageDigest md = MessageDigest.getInstance("SHA1");
        final byte[] hashedAnswer = md.digest(rawResponse.getBytes());
        return Base64Util.encodeBytes(hashedAnswer);
    }

    boolean write(final ChaiUser user)
            throws ChaiUnavailableException, ChaiOperationException
    {
        if (this.state != STATE.NEW) {
            throw new IllegalStateException("ResponseSet not suitable for writing (not in NEW state)");
        }


        final String corAttribute = user.getChaiProvider().getChaiConfiguration().getSetting(ChaiSetting.CR_CHAI_STORAGE_ATTRIBUTE);
        final String corRecordIdentifier = user.getChaiProvider().getChaiConfiguration().getSetting(ChaiSetting.CR_CHAI_STORAGE_RECORD_ID);

        try {
            final ConfigObjectRecord theCor;
            final List<ConfigObjectRecord> corList = ConfigObjectRecord.readRecordFromLDAP(user, corAttribute, corRecordIdentifier, null, null);
            if (!corList.isEmpty()) {
                theCor = corList.get(0);
            } else {
                theCor = ConfigObjectRecord.createNew(user, corAttribute, corRecordIdentifier, null, null);
            }

            final String attributePaylod = rsToChaiXML(this);

            theCor.updatePayload(attributePaylod);
        } catch (ChaiOperationException e) {
            LOGGER.warn("ldap error writing response set: " + e.getMessage());
            throw e;
        } catch (ChaiValidationException e) {
            LOGGER.warn("validation error",e);
            throw new ChaiOperationException(e.getMessage(), ChaiError.UNKNOWN);
        }

        LOGGER.info("successfully wrote Chai challenge/response set for user " + user.getEntryDN());
        this.state = STATE.WRITTEN;

        return true;
    }

    static String rsToChaiXML(final ChaiResponseSet rs) throws ChaiValidationException {
        final Element rootElement = new Element(XML_NODE_ROOT);
        rootElement.setAttribute(XML_ATTRIBUTE_MIN_RANDOM_REQUIRED, String.valueOf(rs.getChallengeSet().getMinRandomRequired()));
        rootElement.setAttribute(XML_ATTRIBUTE_LOCALE, rs.getChallengeSet().getLocale().toString());
        rootElement.setAttribute(XML_ATTRIBUTE_VERSION, VALUE_VERSION);
        rootElement.setAttribute(XML_ATTRIBUTE_CHAI_VERSION, ChaiConstant.CHAI_API_VERSION);

        if (rs.caseInsensitive) {
            rootElement.setAttribute(XML_ATTRIBUTE_CASE_INSENSITIVE, "true");
        }

        if (rs.csIdentifier != null) {
            rootElement.setAttribute(XML_ATTRIBUTE_CHALLENGE_SET_IDENTIFER, rs.csIdentifier);
        }

        if (rs.timestamp != null) {
            rootElement.setAttribute(XML_ATTRIBUTE_TIMESTAMP, DATE_FORMATTER.format(rs.timestamp));
        }

        for (final Challenge loopChallenge : rs.crMap.keySet()) {
            String loopResponseText = rs.caseInsensitive ? rs.crMap.get(loopChallenge).toLowerCase() : rs.crMap.get(loopChallenge);

            final Element loopElement = new Element(XML_NODE_RESPONSE);
            loopElement.addContent(new Element(XML_NODE_CHALLENGE).addContent(new CDATA(loopChallenge.getChallengeText())));

            {
                final Element contentElement = new Element(XML_NODE_ANSWER_VALUE);
                contentElement.setAttribute(XNL_ATTRIBUTE_CONTENT_FORMAT, rs.formatType.toString());
                switch (rs.formatType) {
                    case TEXT:
                        contentElement.addContent(new CDATA(loopResponseText));
                        break;

                    case SHA1_SALT:
                        // generate salt, prepend the salt to the answer, and store the salt as an attr in the xml
                        final String salt = generateSalt(32);
                        loopResponseText = salt + loopResponseText;
                        contentElement.setAttribute(XML_ATTRIBUTE_SALT, salt);
                        // continue on to SHA1 storage.

                    case SHA1:
                        try {
                            final MessageDigest md = MessageDigest.getInstance("SHA1");
                            final byte[] hashedAnswer = md.digest((loopResponseText).getBytes());
                            final String encodedAnswer = Base64Util.encodeBytes(hashedAnswer);
                            contentElement.addContent(new CDATA(encodedAnswer));
                        } catch (NoSuchAlgorithmException e) {
                            LOGGER.warn("error while hashing Chai SHA1 response: " + e.getMessage());
                        }
                }

                loopElement.addContent(contentElement);
            }

            loopElement.setAttribute(XML_ATTRIBUTE_ADMIN_DEFINED, String.valueOf(loopChallenge.isAdminDefined()));
            loopElement.setAttribute(XML_ATTRIBUTE_REQUIRED, String.valueOf(loopChallenge.isRequired()));
            loopElement.setAttribute(XNL_ATTRIBUTE_MIN_LENGTH, String.valueOf(loopChallenge.getMinLength()));
            loopElement.setAttribute(XNL_ATTRIBUTE_MAX_LENGTH, String.valueOf(loopChallenge.getMaxLength()));

            rootElement.addContent(loopElement);
        }

        final Document doc = new Document(rootElement);
        final XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getCompactFormat().setTextMode(Format.TextMode.NORMALIZE));

        return outputter.outputString(doc);
    }

    private static String generateSalt(final int length)
    {
        final SecureRandom random = new SecureRandom();
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SALT_CHARS.charAt(random.nextInt(SALT_CHARS.length())));
        }
        return sb.toString();
    }
}
