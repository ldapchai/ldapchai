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

import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.StringHelper;
import com.novell.security.nmas.jndi.ldap.ext.*;
import com.novell.security.nmas.mgmt.NMASChallengeResponse;
import org.jdom.*;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;

import javax.naming.ldap.ExtendedResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.*;

class NmasResponseSet extends AbstractResponseSet {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(NmasResponseSet.class.getName());

// -------------------------- STATIC METHODS --------------------------

    static List<Challenge> parseNmasPolicyXML(final String str, final Locale locale)
            throws IOException, JDOMException
    {
        final List<Challenge> returnList = new ArrayList<Challenge>();

        final Reader xmlreader = new StringReader(str);
        final SAXBuilder builder = new SAXBuilder();
        final Document doc = builder.build(xmlreader);
        final boolean required = doc.getRootElement().getName().equals("RequiredQuestions");

        for (Iterator qIter = doc.getDescendants(new ElementFilter("Question")); qIter.hasNext();) {
            final Element loopQ = (Element) qIter.next();
            final int maxLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MaxLength"), 255);
            final int minLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MinLength"), 1);

            final String challengeText = readDisplayString(loopQ, locale);

            final Challenge challenge = new ChallengeImpl(required, challengeText, minLength, maxLength, true);
            returnList.add(challenge);
        }

        for (Iterator iter = doc.getDescendants(new ElementFilter("UserDefined")); iter.hasNext();) {
            final Element loopQ = (Element) iter.next();
            final int maxLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MaxLength"), 255);
            final int minLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MinLength"), 1);
            final Challenge challenge = new ChallengeImpl(required, null, minLength, maxLength, false);
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
        for (final Object aDisplayChildren : displayChildren) {
            final Element loopDisplay = (Element) aDisplayChildren;
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

    public static ResponseSet readNmasUserResponseSet(
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
            String xmlString = new String(responseValue);

            xmlString = xmlString.substring(16); // first 16 bytes are non-xml header.
            final ChallengeSet cs = parseNmasUserResponseXML(xmlString);
            final Map<Challenge, String> crMap = new HashMap<Challenge, String>();
            for (final Challenge loopChallenge : cs.getChallenges()) {
                crMap.put(loopChallenge, null);
            }

            return new NmasResponseSet(crMap, cs.getLocale(), cs.getMinRandomRequired(), AbstractResponseSet.STATE.READ, theUser, cs.getIdentifier());
        } catch (ChaiOperationException e) {
            LOGGER.error("error reading nmas user response for " + theUser.getEntryDN() + ", error: " + e.getMessage());
        } catch (JDOMException e) {
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

        for (Iterator iter = doc.getDescendants(new ElementFilter("Challenge")); iter.hasNext();) {
            final Element loopQ = (Element) iter.next();
            final int maxLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MaxLength"), 255);
            final int minLength = StringHelper.convertStrToInt(loopQ.getAttributeValue("MinLength"), 2);
            final String defineStrValue = loopQ.getAttributeValue("Define");
            final boolean adminDefined = defineStrValue.equalsIgnoreCase("Admin");
            final String typeStrValue = loopQ.getAttributeValue("Type");
            final boolean required = typeStrValue.equalsIgnoreCase("Required");
            final String challengeText = loopQ.getText();

            final Challenge challenge = new ChallengeImpl(required, challengeText, minLength, maxLength, adminDefined);
            returnList.add(challenge);
        }

        return new ChallengeSetImpl(returnList, minRandom, null, guidValue);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public NmasResponseSet(
            final Map<Challenge, String> crMap,
            final Locale locale,
            final int minimumRandomRequired,
            final STATE state,
            final ChaiUser user,
            final String csIdentifier
    )
            throws ChaiValidationException
    {
        super(crMap, locale, minimumRandomRequired, state, user, csIdentifier);
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ResponseSet ---------------------

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

// -------------------------- OTHER METHODS --------------------------

    public boolean writeImplementation()
            throws ChaiUnavailableException, ChaiOperationException
    {
        if (this.state != STATE.NEW) {
            throw new IllegalStateException("RepsonseSet not suitable for writing (not in NEW state)");
        }

        //write challenge set questions to Nmas Login Config
        try {
            final PutLoginConfigRequest request = new PutLoginConfigRequest();
            request.setObjectDN(user.getEntryDN());
            final byte[] data = CrHelper.csToNmasXML(getChallengeSet(), this.csIdentifier).getBytes("UTF8");
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
        for (final Challenge loopChallenge : crMap.keySet()) {
            try {
                final byte[] data = crMap.get(loopChallenge).getBytes("UTF8");
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


}

