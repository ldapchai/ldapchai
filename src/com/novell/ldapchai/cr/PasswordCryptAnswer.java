package com.novell.ldapchai.cr;

import com.novell.ldapchai.cr.bean.AnswerBean;
import com.novell.ldapchai.util.BCrypt;
import com.novell.ldapchai.util.SCrypt;
import org.jdom2.Element;

class PasswordCryptAnswer implements Answer {
    private final String answerHash;
    private final boolean caseInsensitive;
    private final FormatType formatType;

    private PasswordCryptAnswer(final String answerHash, final boolean caseInsensitive, final FormatType formatType) {
        if (answerHash == null || answerHash.length() < 1) {
            throw new IllegalArgumentException("missing answer text");
        }

        this.answerHash = answerHash;
        this.caseInsensitive = caseInsensitive;
        this.formatType = formatType;
    }

    private PasswordCryptAnswer(final AnswerFactory.AnswerConfiguration answerConfiguration, final String answer) {
        if (answer == null || answer.length() < 1) {
            throw new IllegalArgumentException("missing answerHash text");
        }

        this.caseInsensitive = answerConfiguration.isCaseInsensitive();
        this.formatType = answerConfiguration.formatType;
        final String casedAnswer = caseInsensitive ? answer.toLowerCase() : answer;
        switch (formatType) {
            case BCRYPT:
                answerHash = BCrypt.hashpw(casedAnswer, BCrypt.gensalt());
                break;

            case SCRYPT:
                answerHash = SCrypt.scrypt(casedAnswer);
                break;

            default:
                throw new IllegalArgumentException("can't test answer for unknown format " + formatType.toString());
        }
    }

    public Element toXml() {
        final Element answerElement = new Element(ChaiResponseSet.XML_NODE_ANSWER_VALUE);
        answerElement.setText(answerHash);
        answerElement.setAttribute(ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT, formatType.toString());
        return answerElement;
    }

    public boolean testAnswer(final String testResponse) {
        if (testResponse == null) {
            return false;
        }

        final String casedAnswer = caseInsensitive ? testResponse.toLowerCase() : testResponse;
        switch (formatType) {
            case BCRYPT:
                return BCrypt.checkpw(casedAnswer, answerHash);

            case SCRYPT:
                return SCrypt.check(casedAnswer, answerHash);
        }
        throw new IllegalArgumentException("can't test answer for unknown format " + formatType.toString());
    }

    public AnswerBean asAnswerBean() {
        final AnswerBean answerBean = new AnswerBean();
        answerBean.setType(formatType);
        answerBean.setAnswerHash(answerHash);
        answerBean.setCaseInsensitive(caseInsensitive);
        answerBean.setHashCount(-1);
        return answerBean;
    }

    static class PasswordCryptAnswerFactory implements ImplementationFactory{
        public PasswordCryptAnswer newAnswer(final AnswerFactory.AnswerConfiguration answerConfiguration, final String answer) {
            return new PasswordCryptAnswer(answerConfiguration,answer);
        }

        public PasswordCryptAnswer fromAnswerBean(final AnswerBean answerBean, final String challengeText) {
            return new PasswordCryptAnswer(answerBean.getAnswerHash(), answerBean.isCaseInsensitive(), answerBean.getType());
        }

        public PasswordCryptAnswer fromXml(final Element element, final boolean caseInsensitive, final String challengeText) {
            final String answerValue = element.getText();
            final String formatStr = element.getAttributeValue(ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT);
            final FormatType formatType;
            try {
                formatType = FormatType.valueOf(formatStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown content format specified in xml format value: '" + formatStr + "'");
            }
            return new PasswordCryptAnswer(answerValue,caseInsensitive,formatType);
        }
    }
}
