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

package com.novell.ldapchai.impl.edir;

import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.cr.AbstractResponseSet;
import com.novell.ldapchai.cr.Answer;
import com.novell.ldapchai.cr.ChaiChallenge;
import com.novell.ldapchai.cr.ChaiChallengeSet;
import com.novell.ldapchai.cr.Challenge;
import com.novell.ldapchai.cr.ChallengeSet;
import com.novell.ldapchai.cr.HelpdeskAnswer;
import com.novell.ldapchai.cr.bean.AnswerBean;
import com.novell.ldapchai.cr.bean.ChallengeBean;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.StringHelper;
import com.novell.security.nmas.jndi.ldap.ext.GetLoginConfigRequest;
import com.novell.security.nmas.jndi.ldap.ext.PutLoginConfigRequest;
import com.novell.security.nmas.jndi.ldap.ext.PutLoginConfigResponse;
import com.novell.security.nmas.jndi.ldap.ext.PutLoginSecretRequest;
import com.novell.security.nmas.jndi.ldap.ext.PutLoginSecretResponse;
import com.novell.security.nmas.mgmt.NMASChallengeResponse;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import javax.naming.ldap.ExtendedResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;


public class NmasResponseSet extends AbstractResponseSet {

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(NmasResponseSet.class.getName());

    private ChaiUser user;

    static List<Challenge> parseNmasPolicyXML(final String str, final Locale locale)
            throws IOException, JDOMException
    {
        final List<Challenge> returnList = new ArrayList<Challenge>();

        final Reader xmlreader = new StringReader(str);
        final SAXBuilder builder = new SAXBuilder();
        final Document doc = builder.build(xmlreader);
        final boolean required = doc.getRootElement().getName().equals("RequiredQuestions");

        for (final Iterator questionIterator = doc.getDescendants(new ElementFilter("Question")); questionIterator.hasNext(); ) {
            final Element loopQ = (Element) questionIterator.next();
            final int maxLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MaxLength"), 255);
            final int minLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MinLength"), 1);

            final String challengeText = readDisplayString(loopQ, locale);

            final Challenge challenge = new ChaiChallenge(required, challengeText, minLength, maxLength, true, 0, false);
            returnList.add(challenge);
        }

        for (Iterator iter = doc.getDescendants(new ElementFilter("UserDefined")); iter.hasNext(); ) {
            final Element loopQ = (Element) iter.next();
            final int maxLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MaxLength"), 255);
            final int minLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MinLength"), 1);
            final Challenge challenge = new ChaiChallenge(required, null, minLength, maxLength, false, 0, false);
            returnList.add(challenge);
        }

        return returnList;
    }

    private static String readDisplayString(final Element questionElement, final Locale locale) {

        final Namespace XML_NAMESPACE = Namespace.getNamespace("xml","http://www.w3.org/XML/1998/namespace");

        // someday ResoureBundle won't suck and this will be a 5 line method.

        // see if the node has any localized displays.
        final List displayChildren = questionElement.getChildren("display");

        // if no locale specified, or if no localized text is available, just use the default.
        if (locale == null || displayChildren == null || displayChildren.size() < 1) {
            return questionElement.getText();
        }

        // convert the xml 'display' elements to a map of locales/strings
        final Map<Locale, String> localizedStringMap = new HashMap<Locale, String>();
        for (final Object loopDisplayChild : displayChildren) {
            final Element loopDisplay = (Element) loopDisplayChild;
            final Attribute localeAttr = loopDisplay.getAttribute("lang",XML_NAMESPACE);
            if (localeAttr != null) {
                final String localeStr = localeAttr.getValue();
                final String displayStr = loopDisplay.getText();
                final Locale localeKey = parseLocaleString(localeStr);
                localizedStringMap.put(localeKey, displayStr);
            }
        }

        final Locale matchedLocale = localeResolver(locale, localizedStringMap.keySet());

        if (matchedLocale != null) {
            return localizedStringMap.get(matchedLocale);
        }

        // none found, so just return the default string.
        return questionElement.getText();
    }

    static NmasResponseSet readNmasUserResponseSet(
            final ChaiUser theUser
    )
            throws ChaiUnavailableException, ChaiValidationException
    {
        final GetLoginConfigRequest request = new GetLoginConfigRequest();
        request.setObjectDN(theUser.getEntryDN());
        request.setTag("ChallengeResponseQuestions");
        request.setMethodID(NMASChallengeResponse.METHOD_ID);
        request.setMethodIDLen(NMASChallengeResponse.METHOD_ID.length * 4);
        try {
            final ExtendedResponse response = theUser.getChaiProvider().extendedOperation(request);
            final byte[] responseValue = response.getEncodedValue();

            if (responseValue == null) {
                return null;
            }

            final String xmlString = new String(responseValue,"UTF8");
            LOGGER.trace("[parse v3]: read ChallengeResponseQuestions from server: " + xmlString);

            ChallengeSet cs = null;
            int parseAttempts = 0;
            final StringBuilder parsingErrorMsg = new StringBuilder();

            {
                final int beginIndex = xmlString.indexOf("<");
                if (beginIndex > 0) {
                    try {
                        parseAttempts++;
                        final String xmlSubstring = xmlString.substring(beginIndex, xmlString.length());
                        LOGGER.trace("attempting parse of index stripped value: " + xmlSubstring);
                        cs = parseNmasUserResponseXML(xmlSubstring);
                        LOGGER.trace("successfully parsed nmas ChallengeResponseQuestions response after index " + beginIndex);
                    } catch (JDOMException e) {
                        if (parsingErrorMsg.length() > 0) {
                            parsingErrorMsg.append(", ");
                        }
                        parsingErrorMsg.append("error parsing index stripped value: ").append(e.getMessage());
                        LOGGER.trace("unable to parse index stripped ChallengeResponseQuestions nmas response; error: " + e.getMessage());
                    }
                }
            }

            if (cs == null) {
                if (xmlString.startsWith("<?xml")) {
                    try {
                        parseAttempts++;
                        cs = parseNmasUserResponseXML(xmlString);
                    } catch (JDOMException e) {
                        parsingErrorMsg.append("error parsing raw value: ").append(e.getMessage());
                        LOGGER.trace("unable to parse raw ChallengeResponseQuestions nmas response; will retry after stripping header; error: " + e.getMessage());
                    }
                    LOGGER.trace("successfully parsed full nmas ChallengeResponseQuestions response");
                }
            }

            if (cs == null) {
                if (xmlString.length() > 16) {
                    final String strippedXml = xmlString.substring(16); // first 16 bytes are non-xml header.
                    try {
                        parseAttempts++;
                        cs = parseNmasUserResponseXML(strippedXml);
                        LOGGER.trace("successfully parsed full nmas ChallengeResponseQuestions response");
                    } catch (JDOMException e) {
                        if (parsingErrorMsg.length() > 0) {
                            parsingErrorMsg.append(", ");
                        }
                        parsingErrorMsg.append("error parsing header stripped value: ").append(e.getMessage());
                        LOGGER.trace("unable to parse stripped ChallengeResponseQuestions nmas response; error: " + e.getMessage());
                    }
                }
            }


            if (cs == null) {
                final String logMsg = "unable to parse nmas ChallengeResponseQuestions: " + parsingErrorMsg;
                if (parseAttempts > 0 && xmlString.length() > 16) {
                    LOGGER.error(logMsg);
                } else {
                    LOGGER.trace(logMsg);

                }
                return null;
            }

            final Map<Challenge, String> crMap = new HashMap<Challenge, String>();
            for (final Challenge loopChallenge : cs.getChallenges()) {
                crMap.put(loopChallenge, null);
            }

            return new NmasResponseSet(crMap, cs.getLocale(), cs.getMinRandomRequired(), AbstractResponseSet.STATE.READ, theUser, cs.getIdentifier());
        } catch (ChaiOperationException e) {
            LOGGER.error("error reading nmas user response for " + theUser.getEntryDN() + ", error: " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error("error reading nmas user response for " + theUser.getEntryDN() + ", error: " + e.getMessage());
        }
        return null;
    }

    static ChallengeSet parseNmasUserResponseXML(final String str)
            throws IOException, JDOMException, ChaiValidationException
    {
        final List<Challenge> returnList = new ArrayList<Challenge>();

        final Reader xmlreader = new StringReader(str);
        final SAXBuilder builder = new SAXBuilder();
        final Document doc = builder.build(xmlreader);

        final Element rootElement = doc.getRootElement();
        final int minRandom = StringHelper.convertStrToInt(rootElement.getAttributeValue("RandomQuestions"), 0);

        final String guidValue;
        {
            final Attribute guidAttribute = rootElement.getAttribute("GUID");
            guidValue = guidAttribute == null ? null : guidAttribute.getValue();
        }

        for (Iterator iter = doc.getDescendants(new ElementFilter("Challenge")); iter.hasNext(); ) {
            final Element loopQ = (Element) iter.next();
            final int maxLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MaxLength"), 255);
            final int minLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MinLength"), 2);
            final String defineStrValue = loopQ.getAttributeValue("Define");
            final boolean adminDefined = "Admin".equalsIgnoreCase(defineStrValue);
            final String typeStrValue = loopQ.getAttributeValue("Type");
            final boolean required = "Required".equalsIgnoreCase(typeStrValue);
            final String challengeText = loopQ.getText();

            final Challenge challenge = new ChaiChallenge(required, challengeText, minLength, maxLength, adminDefined, 0, false);
            returnList.add(challenge);
        }

        return new ChaiChallengeSet(returnList, minRandom, null, guidValue);
    }

    NmasResponseSet(
            final Map<Challenge, String> crMap,
            final Locale locale,
            final int minimumRandomRequired,
            final STATE state,
            final ChaiUser user,
            final String csIdentifier
    )
            throws ChaiValidationException
    {
        super(convertAnswerTextMap(crMap), Collections.<Challenge,HelpdeskAnswer>emptyMap(), locale, minimumRandomRequired, state, csIdentifier);
        this.user = user;
    }

    public String stringValue()
            throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("stringValue() is not supported by NMAS response sets");
    }

    public boolean test(final Map<Challenge, String> responseTest)
    {
        //@todo TODO
        throw new UnsupportedOperationException("NMAS Response testing not yet implemented");
    }

    boolean write()
            throws ChaiUnavailableException, ChaiOperationException
    {
        if (this.state != STATE.NEW) {
            throw new IllegalStateException("RepsonseSet not suitable for writing (not in NEW state)");
        }

        //write challenge set questions to Nmas Login Config
        try {
            final PutLoginConfigRequest request = new PutLoginConfigRequest();
            request.setObjectDN(user.getEntryDN());
            final byte[] data = csToNmasXML(getChallengeSet(), this.csIdentifier).getBytes("UTF8");
            request.setData(data);
            request.setDataLen(data.length);
            request.setTag("ChallengeResponseQuestions");
            request.setMethodID(NMASChallengeResponse.METHOD_ID);
            request.setMethodIDLen(NMASChallengeResponse.METHOD_ID.length * 4);

            final ExtendedResponse response = user.getChaiProvider().extendedOperation(request);
            if (response != null && ((PutLoginConfigResponse) response).getNmasRetCode() != 0) {
                LOGGER.debug("nmas error writing question: " + ((PutLoginConfigResponse) response).getNmasRetCode());
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("error while writing nmas questions: " + e.getMessage());
            return false;
        } catch (ChaiOperationException e) {
            LOGGER.error("error while writing nmas questions: " + e.getMessage());
            throw e;
        } catch (ChaiValidationException e) {
            LOGGER.error("error while writing nmas questions: " + e.getMessage());
            throw ChaiOperationException.forErrorMessage(e.getMessage());
        }

        boolean success = true;

        //write responses
        for (final Map.Entry<Challenge, Answer> entry : crMap.entrySet()) {
            final Challenge loopChallenge = entry.getKey();
            try {
                final byte[] data = ((NmasAnswer)entry.getValue()).getAnswerText().getBytes("UTF8");
                final PutLoginSecretRequest request = new PutLoginSecretRequest();
                request.setObjectDN(user.getEntryDN());
                request.setData(data);
                request.setDataLen(data.length);
                request.setTag(loopChallenge.getChallengeText());
                request.setMethodID(NMASChallengeResponse.METHOD_ID);
                request.setMethodIDLen(NMASChallengeResponse.METHOD_ID.length * 4);

                final ExtendedResponse response = user.getChaiProvider().extendedOperation(request);
                if (response != null && ((PutLoginSecretResponse) response).getNmasRetCode() != 0) {
                    LOGGER.debug("nmas error writing answer: " + ((PutLoginSecretResponse) response).getNmasRetCode());
                    success = false;
                }
            } catch (Exception e) {
                LOGGER.error("error while writing nmas answer: " + e.getMessage());
            }
        }

        if (success) {
            LOGGER.info("successfully wrote NMAS challenge/response set for user " + user.getEntryDN());
            this.state = STATE.WRITTEN;
        }

        return success;
    }

    public static Locale localeResolver(final Locale desiredLocale, final Collection<Locale> localePool) {
        if (desiredLocale == null || localePool == null || localePool.isEmpty()) {
            return null;
        }

        for (final Locale loopLocale : localePool) {
            if (loopLocale.getLanguage().equalsIgnoreCase(desiredLocale.getLanguage())) {
                if (loopLocale.getCountry().equalsIgnoreCase(desiredLocale.getCountry())) {
                    if (loopLocale.getVariant().equalsIgnoreCase(desiredLocale.getVariant())) {
                        return loopLocale;
                    }
                }
            }
        }

        for (final Locale loopLocale : localePool) {
            if (loopLocale.getLanguage().equalsIgnoreCase(desiredLocale.getLanguage())) {
                if (loopLocale.getCountry().equalsIgnoreCase(desiredLocale.getCountry())) {
                    return loopLocale;
                }
            }
        }

        for (final Locale loopLocale : localePool) {
            if (loopLocale.getLanguage().equalsIgnoreCase(desiredLocale.getLanguage())) {
                return loopLocale;
            }
        }

        final Locale defaultLocale = parseLocaleString("");
        if (localePool.contains(defaultLocale)) {
            return defaultLocale;
        }

        return null;
    }

    public static Locale parseLocaleString(final String localeString) {
        if (localeString == null) {
            return new Locale("");
        }

        final StringTokenizer st = new StringTokenizer(localeString, "_");

        if (!st.hasMoreTokens()) {
            return new Locale("");
        }

        final String language = st.nextToken();
        if (!st.hasMoreTokens()) {
            return new Locale(language);
        }

        final String country = st.nextToken();
        if (!st.hasMoreTokens()) {
            return new Locale(language, country);
        }

        final String variant = st.nextToken("");
        return new Locale(language, country, variant);
    }

    private static final String NMAS_XML_ROOTNODE = "Challenges";
    private static final String NMAS_XML_ATTR_RANDOM_COUNT = "RandomQuestions";
    private static final String NMAS_XML_NODE_CHALLENGE = "Challenge";
    private static final String NMAS_XML_ATTR_TYPE = "Type";
    private static final String NMAS_XML_ATTR_DEFINE = "Define";
    private static final String NMAS_XML_ATTR_MIN_LENGTH = "MinLength";
    private static final String NMAS_XML_ATTR_MAX_LENGTH = "MaxLength";

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
        final Format format = Format.getRawFormat();
        format.setTextMode(Format.TextMode.PRESERVE);
        format.setLineSeparator("");
        outputter.setFormat(format);
        return outputter.outputString(rootElement);
    }

    private static Map<Challenge,Answer> convertAnswerTextMap(final Map<Challenge,String> crMap) {
        final Map<Challenge,Answer> returnMap = new LinkedHashMap<>();
        for (final Map.Entry<Challenge, String> entry : crMap.entrySet()) {
            final Challenge challenge = entry.getKey();
            final String answerText = entry.getValue();
            returnMap.put(challenge,new NmasAnswer(answerText));
        }
        return returnMap;
    }

    private static class NmasAnswer implements Answer {
        private String answerText;

        private NmasAnswer(final String answerText) {
            this.answerText = answerText;
        }

        public String getAnswerText() {
            return answerText;
        }

        public boolean testAnswer(final String answer) {
            //@todo TODO
            throw new UnsupportedOperationException("NMAS Response testing not yet implemented");
        }

        public Element toXml() {
            return null;
        }

        public AnswerBean asAnswerBean() {
            throw new UnsupportedOperationException("NMAS stored responses do not support retrieval of answers");
        }
    }

    public List<ChallengeBean> asChallengeBeans(final boolean includeAnswers) {
        if (includeAnswers) {
            throw new UnsupportedOperationException("NMAS stored responses do not support retrieval of answers");
        }

        if (crMap == null) {
            return Collections.emptyList();
        }

        final List<ChallengeBean> returnList = new ArrayList<ChallengeBean>();
        for (final Challenge challenge : this.crMap.keySet()) {
            returnList.add(challenge.asChallengeBean());
        }
        return returnList;
    }

    public List<ChallengeBean> asHelpdeskChallengeBeans(final boolean includeAnswers) {
        //@todo TODO
        throw new UnsupportedOperationException("NMAS stored responses do not support Helpdesk Challenges");
    }
}

