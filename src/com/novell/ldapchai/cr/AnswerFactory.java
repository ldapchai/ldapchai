package com.novell.ldapchai.cr;

import com.novell.ldapchai.cr.bean.AnswerBean;

import java.io.Serializable;

public class AnswerFactory {
    private AnswerFactory() {
    }

    public static Answer newAnswer(final AnswerConfiguration answerConfiguration, final String answerText) {
        final Answer.ImplementationFactory implementationFactory = answerConfiguration.getFormatType().getFactory();
        return implementationFactory.newAnswer(answerConfiguration, answerText);
    }

    public static Answer fromAnswerBean(final AnswerBean input, final String challengeText) {
        final Answer.ImplementationFactory implementationFactory = input.getType().getFactory();
        return implementationFactory.fromAnswerBean(input, challengeText);
    }

    public static Answer fromXml(final org.jdom2.Element element, final boolean caseInsensitive, final String challengeText) {
        final String formatStr = element.getAttribute(ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT).getValue();
        final Answer.FormatType respFormat;
        if (formatStr != null && formatStr.length() > 0) {
            respFormat = Answer.FormatType.valueOf(formatStr);
        } else {
            respFormat = Answer.FormatType.TEXT;
        }
        return respFormat.getFactory().fromXml(element, caseInsensitive, challengeText);
    }

    public static class AnswerConfiguration implements Serializable {
        public boolean caseInsensitive;
        public int hashCount;
        public Answer.FormatType formatType;
        public String challengeText;

        public boolean isCaseInsensitive() {
            return caseInsensitive;
        }

        public void setCaseInsensitive(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        public int getHashCount() {
            return hashCount;
        }

        public void setHashCount(int hashCount) {
            this.hashCount = hashCount;
        }

        public Answer.FormatType getFormatType() {
            return formatType;
        }

        public void setFormatType(Answer.FormatType formatType) {
            this.formatType = formatType;
        }

        public String getChallengeText() {
            return challengeText;
        }

        public void setChallengeText(String challengeText) {
            this.challengeText = challengeText;
        }
    }
}
