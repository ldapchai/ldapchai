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
import org.jdom2.Element;

class TextAnswer implements Answer {
    private String answer;
    private boolean caseInsensitive;

    TextAnswer(final String answer, final boolean caseInsensitive) {
        if (answer == null || answer.length() < 1) {
            throw new IllegalArgumentException("missing answer text");
        }

        this.answer = answer;
        this.caseInsensitive = caseInsensitive;
    }

    public Element toXml() {
        final Element answerElement = new Element(ChaiResponseSet.XML_NODE_ANSWER_VALUE);
        answerElement.setText(answer);
        answerElement.setAttribute(ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT, FormatType.TEXT.toString());
        return answerElement;
    }

    public boolean testAnswer(final String testResponse) {
        if (testResponse == null) {
            return false;
        }

        final String casedResponse = caseInsensitive ? testResponse.toLowerCase() : testResponse;
        return answer.equalsIgnoreCase(casedResponse);
    }

    public AnswerBean asAnswerBean() {
        final AnswerBean answerBean = new AnswerBean();
        answerBean.setType(FormatType.TEXT);
        answerBean.setAnswerText(answer);
        answerBean.setCaseInsensitive(caseInsensitive);
        return answerBean;
    }

    static class TextAnswerFactory implements ImplementationFactory {
        public TextAnswer newAnswer(final AnswerFactory.AnswerConfiguration answerConfiguration, final String answer) {
            final boolean caseInsensitive = answerConfiguration.caseInsensitive;
            return new TextAnswer(answer,caseInsensitive);
        }

        public Answer fromAnswerBean(AnswerBean input, String challengeText) {
            return new TextAnswer(input.getAnswerText(), input.isCaseInsensitive());
        }

        public TextAnswer fromXml(final Element element, final boolean caseInsensitive, final String challengeText) {
            final String answerValue = element.getText();
            return new TextAnswer(answerValue,caseInsensitive);
        }
    }
}
