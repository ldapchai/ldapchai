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
import com.novell.ldapchai.cr.bean.AnswerBean;
import com.novell.ldapchai.cr.bean.ChallengeBean;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.ConfigObjectRecord;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChaiResponseSet extends AbstractResponseSet implements Serializable {
// ----------------------------- CONSTANTS ----------------------------


    // ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(ChaiResponseSet.class.getName());

    static final String SALT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    final static String XML_NODE_ROOT = "ResponseSet";
    final static String XML_ATTRIBUTE_MIN_RANDOM_REQUIRED = "minRandomRequired";
    final static String XML_ATTRIBUTE_LOCALE = "locale";

    final static String XML_NODE_RESPONSE = "response";
    final static String XML_NODE_HELPDESK_RESPONSE = "helpdesk-response";
    final static String XML_NODE_CHALLENGE = "challenge";
    final static String XML_NODE_ANSWER_VALUE = "answer";

    final static String XML_ATTRIBUTE_VERSION = "version";
    final static String XML_ATTRIBUTE_CHAI_VERSION = "chaiVersion";
    final static String XML_ATTRIBUTE_ADMIN_DEFINED = "adminDefined";
    final static String XML_ATTRIBUTE_REQUIRED = "required";
    final static String XML_ATTRIBUTE_HASH_COUNT = "hashcount";
    final static String XML_ATTRIBUTE_CONTENT_FORMAT = "format";
    final static String XML_ATTRIBUTE_SALT = "salt";
    final static String XNL_ATTRIBUTE_MIN_LENGTH = "minLength";
    final static String XNL_ATTRIBUTE_MAX_LENGTH = "maxLength";
    final static String XML_ATTRIBUTE_CASE_INSENSITIVE = "caseInsensitive";
    final static String XML_ATTRIBUTE_CHALLENGE_SET_IDENTIFER = "challengeSetID"; // identifier from challenge set.
    final static String XML_ATTRIBUTE_TIMESTAMP = "time";

    final static String VALUE_VERSION = "2";

    final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

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
        returnVal = ChaiResponseXmlParser.parseChaiResponseSetXML(payload);

        if (returnVal == null) {
            return null;
        }

        return returnVal;
    }


// --------------------------- CONSTRUCTORS ---------------------------

    ChaiResponseSet(
            final Map<Challenge, Answer> crMap,
            final Map<Challenge, HelpdeskAnswer> helpdeskCrMap,
            final Locale locale,
            final int minimumRandomRequired,
            final STATE state,
            final boolean caseInsensitive,
            final String csIdentifer,
            final Date timestamp
    )
            throws ChaiValidationException
    {
        super(crMap, helpdeskCrMap, locale, minimumRandomRequired, state, csIdentifer);
        this.caseInsensitive = caseInsensitive;
        this.timestamp = timestamp;
    }

// ------------------------ CANONICAL METHODS ------------------------

    public String toString()
    {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append(", format(");
        sb.append(")");
        return sb.toString();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ResponseSet ---------------------

    public String stringValue()
            throws UnsupportedOperationException, ChaiOperationException {
        try {
            String stringResult = rsToChaiXML(this);
            stringResult = stringResult.replace("\r", "");
            stringResult = stringResult.replace("\n", "");
            return stringResult;
        } catch (ChaiValidationException e) {
            LOGGER.warn("error writing XML response set", e);
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

            final boolean correct = crMap.get(loopChallenge).testAnswer(proposedResponse);

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

    static String rsToChaiXML(final ChaiResponseSet rs)
            throws ChaiValidationException, ChaiOperationException
    {
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

        if (rs.crMap != null) {
            for (final Challenge loopChallenge : rs.crMap.keySet()) {
                final Answer answer = rs.crMap.get(loopChallenge);
                final Element responseElement = challengeToXml(loopChallenge, answer, XML_NODE_RESPONSE);
                rootElement.addContent(responseElement);
            }
        }

        if (rs.helpdeskCrMap != null) {
            for (final Challenge loopChallenge : rs.helpdeskCrMap.keySet()) {
                final Answer answer = rs.helpdeskCrMap.get(loopChallenge);
                final Element responseElement = challengeToXml(loopChallenge, answer, XML_NODE_HELPDESK_RESPONSE);
                rootElement.addContent(responseElement);
            }
        }


        final Document doc = new Document(rootElement);
        final XMLOutputter outputter = new XMLOutputter();
        final Format format = Format.getRawFormat();
        format.setTextMode(Format.TextMode.PRESERVE);
        format.setLineSeparator("");
        outputter.setFormat(format);
        return outputter.outputString(doc);
    }

    private static Element challengeToXml(final Challenge loopChallenge, final Answer answer, final String elementName)
            throws ChaiOperationException
    {
        final Element responseElement = new Element(elementName);
        responseElement.addContent(new Element(XML_NODE_CHALLENGE).addContent(new Text(loopChallenge.getChallengeText())));
        final Element answerElement = answer.toXml();
        responseElement.addContent(answerElement);
        responseElement.setAttribute(XML_ATTRIBUTE_ADMIN_DEFINED, String.valueOf(loopChallenge.isAdminDefined()));
        responseElement.setAttribute(XML_ATTRIBUTE_REQUIRED, String.valueOf(loopChallenge.isRequired()));
        responseElement.setAttribute(XNL_ATTRIBUTE_MIN_LENGTH, String.valueOf(loopChallenge.getMinLength()));
        responseElement.setAttribute(XNL_ATTRIBUTE_MAX_LENGTH, String.valueOf(loopChallenge.getMaxLength()));
        return responseElement;
    }

    static class ChaiResponseXmlParser {
        static ChaiResponseSet parseChaiResponseSetXML(final String input)
                throws ChaiValidationException, ChaiOperationException {
            if (input == null || input.length() < 1) {
                return null;
            }

            final Map<Challenge, Answer> crMap = new LinkedHashMap<Challenge, Answer>();
            final Map<Challenge, HelpdeskAnswer> helpdeskCrMap = new LinkedHashMap<Challenge,HelpdeskAnswer>();
            int minRandRequired = 0;
            Attribute localeAttr = null;
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

                for (final Object o : rootElement.getChildren(XML_NODE_RESPONSE)) {
                    final Element loopResponseElement = (Element) o;
                    final Challenge newChallenge = parseResponseElement(loopResponseElement);
                    final Answer answer = AnswerFactory.fromXml(loopResponseElement.getChild(XML_NODE_ANSWER_VALUE),caseInsensitive,newChallenge.getChallengeText());
                    crMap.put(newChallenge, answer);
                }
                for (final Object o : rootElement.getChildren(XML_NODE_HELPDESK_RESPONSE)) {
                    final Element loopResponseElement = (Element) o;
                    final Challenge newChallenge = parseResponseElement(loopResponseElement);
                    final HelpdeskAnswer answer = (HelpdeskAnswer)AnswerFactory.fromXml(loopResponseElement.getChild(XML_NODE_ANSWER_VALUE),caseInsensitive,newChallenge.getChallengeText());
                    helpdeskCrMap.put(newChallenge, answer);
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
                    helpdeskCrMap,
                    challengeLocale,
                    minRandRequired,
                    STATE.READ,
                    caseInsensitive,
                    csIdentifier,
                    timestamp);
        }

        private static Challenge parseResponseElement(final Element loopResponseElement)
                throws DataConversionException
        {
            final boolean required = loopResponseElement.getAttribute(XML_ATTRIBUTE_REQUIRED).getBooleanValue();
            final boolean adminDefined = loopResponseElement.getAttribute(XML_ATTRIBUTE_ADMIN_DEFINED).getBooleanValue();

            final String challengeText = loopResponseElement.getChild(XML_NODE_CHALLENGE).getText();
            final int minLength = loopResponseElement.getAttribute(XNL_ATTRIBUTE_MIN_LENGTH).getIntValue();
            final int maxLength = loopResponseElement.getAttribute(XNL_ATTRIBUTE_MAX_LENGTH).getIntValue();

            return new ChaiChallenge(required, challengeText, minLength, maxLength, adminDefined);
        }
    }

    /**
     *
     * @param inputXmlString
     * @param theUser
     * @deprecated {@link ChaiCrFactory#parseChaiResponseSetXML(String)}
     * @see  {@link ChaiCrFactory#parseChaiResponseSetXML(String)}
     * @return
     * @throws ChaiValidationException
     */
    @Deprecated
    public static ResponseSet parseChaiResponseSetXML(final String inputXmlString, final ChaiUser theUser)
            throws ChaiValidationException
    {
        try {
            return ChaiResponseXmlParser.parseChaiResponseSetXML(inputXmlString);
        } catch (ChaiOperationException e) {
            throw new ChaiValidationException(e.getMessage(),e.getErrorCode());
        }
    }

    public List<ChallengeBean> asChallengeBeans(boolean includeAnswers) {
        return asBeans(crMap,includeAnswers);
    }

    public List<ChallengeBean> asHelpdeskChallengeBeans(boolean includeAnswers) {
        return asBeans(helpdeskCrMap,includeAnswers);
    }

    public static List<ChallengeBean> asBeans(final Map inputMap, boolean includeAnswers) {
        if (inputMap == null) {
            return Collections.emptyList();
        }

        final List<ChallengeBean> returnList = new ArrayList<ChallengeBean>();
        for (final Object loopChallengeObj : inputMap.keySet()) {
            final Challenge loopChallenge = (Challenge)loopChallengeObj;
            final ChallengeBean challengeBean = loopChallenge.asChallengeBean();
            if (includeAnswers) {
                final Answer loopAnswer = (Answer)inputMap.get(loopChallenge);
                final AnswerBean answerBean = loopAnswer.asAnswerBean();
                challengeBean.setAnswer(answerBean);
            }
            returnList.add(challengeBean);
        }
        return returnList;
    }

}
