/*
 * LDAP Chai API
 * Copyright (c) 2006-2017 Novell, Inc.
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
import com.novell.ldapchai.cr.bean.AnswerBean;
import com.novell.ldapchai.cr.bean.ChallengeBean;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.impl.edir.entry.ext.GetLoginConfigRequest;
import com.novell.ldapchai.impl.edir.entry.ext.NMASChallengeResponse;
import com.novell.ldapchai.impl.edir.entry.ext.PutLoginConfigRequest;
import com.novell.ldapchai.impl.edir.entry.ext.PutLoginConfigResponse;
import com.novell.ldapchai.impl.edir.entry.ext.PutLoginSecretRequest;
import com.novell.ldapchai.impl.edir.entry.ext.PutLoginSecretResponse;
import com.novell.ldapchai.util.internal.ChaiLogger;
import com.novell.ldapchai.util.internal.StringHelper;
import org.jrivard.xmlchai.AccessMode;
import org.jrivard.xmlchai.XmlDocument;
import org.jrivard.xmlchai.XmlElement;
import org.jrivard.xmlchai.XmlFactory;

import javax.naming.ldap.ExtendedResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class NmasResponseSet extends AbstractResponseSet
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( NmasResponseSet.class );

    private final ChaiUser user;

    static NmasResponseSet readNmasUserResponseSet(
            final ChaiUser theUser
    )
            throws ChaiUnavailableException
    {
        final GetLoginConfigRequest request = new GetLoginConfigRequest();
        request.setObjectDN( theUser.getEntryDN() );
        request.setTag( NMASChallengeResponse.CHALLENGE_RESPONSE_QUESTIONS_TAG );
        request.setMethodID( NMASChallengeResponse.getMethodId() );
        request.setMethodIDLen( NMASChallengeResponse.getMethodId().length * 4 );

        try
        {
            final ExtendedResponse response = theUser.getChaiProvider().extendedOperation( request );
            final byte[] responseValue = response.getEncodedValue();

            if ( responseValue == null )
            {
                return null;
            }

            final String xmlString = new String( responseValue, "UTF8" );
            LOGGER.trace( () -> "[parse v3]: read ChallengeResponseQuestions from server: " + xmlString );
            return readNmasUserResponseSet( theUser,  xmlString );
        }
        catch ( IOException | ChaiValidationException | ChaiOperationException e )
        {
            LOGGER.error( () -> "error reading nmas user response for " + theUser.getEntryDN() + ", error: " + e.getMessage() );
        }

        return null;
    }


    static NmasResponseSet readNmasUserResponseSet(
            final ChaiUser theUser,
            final String xmlString
    )
            throws ChaiValidationException
    {
        ChallengeSet cs = null;
        int parseAttempts = 0;
        final StringBuilder parsingErrorMsg = new StringBuilder();

        {
            final int beginIndex = xmlString.indexOf( "<" );
            if ( beginIndex > 0 )
            {
                try
                {
                    parseAttempts++;
                    final String xmlSubstring = xmlString.substring( beginIndex );
                    LOGGER.trace( () -> "attempting parse of index stripped value: " + xmlSubstring );
                    cs = parseNmasUserResponseXML( xmlSubstring );
                    LOGGER.trace( () -> "successfully parsed nmas ChallengeResponseQuestions response after index " + beginIndex );
                }
                catch ( IOException e )
                {
                    if ( parsingErrorMsg.length() > 0 )
                    {
                        parsingErrorMsg.append( ", " );
                    }
                    parsingErrorMsg.append( "error parsing index stripped value: " ).append( e.getMessage() );
                    LOGGER.trace( () -> "unable to parse index stripped ChallengeResponseQuestions nmas response; error: " + e.getMessage() );
                }
            }
        }

        if ( cs == null )
        {
            if ( xmlString.startsWith( "<?xml" ) )
            {
                try
                {
                    parseAttempts++;
                    cs = parseNmasUserResponseXML( xmlString );
                }
                catch ( IOException e )
                {
                    parsingErrorMsg.append( "error parsing raw value: " ).append( e.getMessage() );
                    LOGGER.trace( () -> "unable to parse raw ChallengeResponseQuestions nmas response; will retry after stripping header; error: " + e.getMessage() );
                }
                LOGGER.trace( () -> "successfully parsed full nmas ChallengeResponseQuestions response" );
            }
        }

        if ( cs == null )
        {
            if ( xmlString.length() > 16 )
            {
                // first 16 bytes are non-xml header.
                final String strippedXml = xmlString.substring( 16 );
                try
                {
                    parseAttempts++;
                    cs = parseNmasUserResponseXML( strippedXml );
                    LOGGER.trace( () -> "successfully parsed full nmas ChallengeResponseQuestions response" );
                }
                catch ( IOException e )
                {
                    if ( parsingErrorMsg.length() > 0 )
                    {
                        parsingErrorMsg.append( ", " );
                    }
                    parsingErrorMsg.append( "error parsing header stripped value: " ).append( e.getMessage() );
                    LOGGER.trace( () -> "unable to parse stripped ChallengeResponseQuestions nmas response; error: " + e.getMessage() );
                }
            }
        }


        if ( cs == null )
        {
            final String logMsg = "unable to parse nmas ChallengeResponseQuestions: " + parsingErrorMsg;
            if ( parseAttempts > 0 && xmlString.length() > 16 )
            {
                LOGGER.error( () -> logMsg );
            }
            else
            {
                LOGGER.trace( () -> logMsg );

            }
            return null;
        }

        final Map<Challenge, String> crMap = new HashMap<>();
        for ( final Challenge loopChallenge : cs.getChallenges() )
        {
            crMap.put( loopChallenge, null );
        }

        return new NmasResponseSet( crMap, cs.getLocale(), cs.getMinRandomRequired(), AbstractResponseSet.STATE.READ, theUser, cs.getIdentifier() );
    }

    static ChallengeSet parseNmasUserResponseXML( final String str )
            throws IOException, ChaiValidationException
    {
        final List<Challenge> returnList = new ArrayList<>();

        final XmlDocument doc = XmlFactory.getFactory().parseString( str, AccessMode.IMMUTABLE );
        final XmlElement rootElement = doc.getRootElement();

        final int minRandom = StringHelper.convertStrToInt( rootElement.getAttribute( "RandomQuestions" ).orElse( "0" ), 0 );
        final String guidValue = rootElement.getAttribute( "GUID" ).orElse( null );

        for ( final XmlElement loopQ : doc.evaluateXpathToElements( "//Challenge" ) )
        {

            final int maxLength = StringHelper.convertStrToInt( loopQ.getAttribute( "MaxLength" ).orElse( "" ), 255 );
            final int minLength = StringHelper.convertStrToInt( loopQ.getAttribute( "MinLength" ).orElse( "" ), 2 );
            final boolean adminDefined = "Admin".equalsIgnoreCase( loopQ.getAttribute( "Define" ).orElse( null ) );
            final boolean required = "Required".equalsIgnoreCase( loopQ.getAttribute( "Type" ).orElse( null ) );
            final String challengeText = loopQ.getText().orElseThrow( () -> new IllegalStateException( "missing challenge text" ) );

            final Challenge challenge = new ChaiChallenge( required, challengeText, minLength, maxLength, adminDefined, 0, false );
            returnList.add( challenge );
        }

        return new ChaiChallengeSet( returnList, minRandom, null, guidValue );
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
        super( convertAnswerTextMap( crMap ), Collections.emptyMap(), locale, minimumRandomRequired, state, csIdentifier, null );
        this.user = user;
    }

    @Override
    public String stringValue()
            throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException( "stringValue() is not supported by NMAS response sets" );
    }

    @Override
    public boolean test( final Map<Challenge, String> responseTest )
    {
        //@todo TODO
        throw new UnsupportedOperationException( "NMAS Response testing not yet implemented" );
    }

    boolean write()
            throws ChaiUnavailableException, ChaiOperationException
    {
        if ( this.state != STATE.NEW )
        {
            throw new IllegalStateException( "RepsonseSet not suitable for writing (not in NEW state)" );
        }

        //write challenge set questions to Nmas Login Config
        try
        {
            final PutLoginConfigRequest request = new PutLoginConfigRequest();
            request.setObjectDN( user.getEntryDN() );
            final byte[] data = csToNmasXML( getChallengeSet(), this.csIdentifier ).getBytes( "UTF8" );
            request.setData( data );
            request.setDataLen( data.length );
            request.setTag( NMASChallengeResponse.CHALLENGE_RESPONSE_QUESTIONS_TAG );
            request.setMethodID( NMASChallengeResponse.getMethodId() );
            request.setMethodIDLen( NMASChallengeResponse.getMethodId().length * 4 );

            final ExtendedResponse response = user.getChaiProvider().extendedOperation( request );
            if ( response != null && ( ( PutLoginConfigResponse ) response ).getNmasRetCode() != 0 )
            {
                LOGGER.debug( () -> "nmas error writing question: " + ( ( PutLoginConfigResponse ) response ).getNmasRetCode() );
                return false;
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            LOGGER.error( () -> "error while writing nmas questions: " + e.getMessage() );
            return false;
        }
        catch ( ChaiOperationException e )
        {
            LOGGER.error( () -> "error while writing nmas questions: " + e.getMessage() );
            throw e;
        }
        catch ( ChaiValidationException e )
        {
            LOGGER.error( () -> "error while writing nmas questions: " + e.getMessage() );
            throw ChaiOperationException.forErrorMessage( e.getMessage(), e );
        }

        boolean success = true;

        //write responses
        for ( final Map.Entry<Challenge, Answer> entry : crMap.entrySet() )
        {
            final Challenge loopChallenge = entry.getKey();
            try
            {
                final byte[] data = ( ( NmasAnswer ) entry.getValue() ).getAnswerText().getBytes( "UTF8" );
                final PutLoginSecretRequest request = new PutLoginSecretRequest();
                request.setObjectDN( user.getEntryDN() );
                request.setData( data );
                request.setDataLen( data.length );
                request.setTag( loopChallenge.getChallengeText() );
                request.setMethodID( NMASChallengeResponse.getMethodId() );
                request.setMethodIDLen( NMASChallengeResponse.getMethodId().length * 4 );

                final ExtendedResponse response = user.getChaiProvider().extendedOperation( request );
                if ( response != null && ( ( PutLoginSecretResponse ) response ).getNmasRetCode() != 0 )
                {
                    LOGGER.debug( () -> "nmas error writing answer: " + ( ( PutLoginSecretResponse ) response ).getNmasRetCode() );
                    success = false;
                }
            }
            catch ( Exception e )
            {
                LOGGER.error( () -> "error while writing nmas answer: " + e.getMessage() );
            }
        }

        if ( success )
        {
            LOGGER.info( () -> "successfully wrote NMAS challenge/response set for user " + user.getEntryDN() );
            this.state = STATE.WRITTEN;
        }

        return success;
    }


    private static final String NMAS_XML_ROOTNODE = "Challenges";
    private static final String NMAS_XML_ATTR_RANDOM_COUNT = "RandomQuestions";
    private static final String NMAS_XML_NODE_CHALLENGE = "Challenge";
    private static final String NMAS_XML_ATTR_TYPE = "Type";
    private static final String NMAS_XML_ATTR_DEFINE = "Define";
    private static final String NMAS_XML_ATTR_MIN_LENGTH = "MinLength";
    private static final String NMAS_XML_ATTR_MAX_LENGTH = "MaxLength";

    static String csToNmasXML( final ChallengeSet cs, final String guidValue )
    {
        final XmlFactory xmlFactory = XmlFactory.getFactory();
        final XmlDocument xmlDocument = xmlFactory.newDocument( NMAS_XML_ROOTNODE );
        final XmlElement rootElement = xmlDocument.getRootElement();

        rootElement.setAttribute( NMAS_XML_ATTR_RANDOM_COUNT, String.valueOf( cs.getMinRandomRequired() ) );
        if ( guidValue != null )
        {
            rootElement.setAttribute( "GUID", guidValue );
        }
        else
        {
            rootElement.setAttribute( "GUID", "0" );
        }

        for ( final Challenge challenge : cs.getChallenges() )
        {
            final XmlElement loopElement = xmlFactory.newElement( NMAS_XML_NODE_CHALLENGE );
            if ( challenge.getChallengeText() != null )
            {
                loopElement.setText( challenge.getChallengeText() );
            }

            if ( challenge.isAdminDefined() )
            {
                loopElement.setAttribute( NMAS_XML_ATTR_DEFINE, "Admin" );
            }
            else
            {
                loopElement.setAttribute( NMAS_XML_ATTR_DEFINE, "User" );
            }

            if ( challenge.isRequired() )
            {
                loopElement.setAttribute( NMAS_XML_ATTR_TYPE, "Required" );
            }
            else
            {
                loopElement.setAttribute( NMAS_XML_ATTR_TYPE, "Random" );
            }

            loopElement.setAttribute( NMAS_XML_ATTR_MIN_LENGTH, String.valueOf( challenge.getMinLength() ) );
            loopElement.setAttribute( NMAS_XML_ATTR_MAX_LENGTH, String.valueOf( challenge.getMaxLength() ) );

            rootElement.attachElement( loopElement );
        }

        try
        {
            return xmlFactory.outputString( xmlDocument );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "unexpected error serializing xml document for NMAS response set: " + e.getMessage() );
        }
    }

    private static Map<Challenge, Answer> convertAnswerTextMap( final Map<Challenge, String> crMap )
    {
        final Map<Challenge, Answer> returnMap = new LinkedHashMap<>();
        for ( final Map.Entry<Challenge, String> entry : crMap.entrySet() )
        {
            final Challenge challenge = entry.getKey();
            final String answerText = entry.getValue();
            returnMap.put( challenge, new NmasAnswer( answerText ) );
        }
        return returnMap;
    }

    private static class NmasAnswer implements Answer
    {
        private final String answerText;

        private NmasAnswer( final String answerText )
        {
            this.answerText = answerText;
        }

        public String getAnswerText()
        {
            return answerText;
        }

        @Override
        public boolean testAnswer( final String answer )
        {
            //@todo TODO
            throw new UnsupportedOperationException( "NMAS Response testing not yet implemented" );
        }

        @Override
        public XmlElement toXml()
        {
            return null;
        }

        @Override
        public AnswerBean asAnswerBean()
        {
            throw new UnsupportedOperationException( "NMAS stored responses do not support retrieval of answers" );
        }
    }

    @Override
    public List<ChallengeBean> asChallengeBeans( final boolean includeAnswers )
    {
        if ( includeAnswers )
        {
            throw new UnsupportedOperationException( "NMAS stored responses do not support retrieval of answers" );
        }

        if ( crMap == null )
        {
            return Collections.emptyList();
        }

        final List<ChallengeBean> returnList = new ArrayList<>();
        for ( final Challenge challenge : this.crMap.keySet() )
        {
            returnList.add( challenge.asChallengeBean() );
        }
        return returnList;
    }

    @Override
    public List<ChallengeBean> asHelpdeskChallengeBeans( final boolean includeAnswers )
    {
        //@todo TODO
        throw new UnsupportedOperationException( "NMAS stored responses do not support Helpdesk Challenges" );
    }
}

