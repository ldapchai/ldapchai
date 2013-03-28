package com.novell.ldapchai.cr;

import com.novell.ldapchai.cr.bean.AnswerBean;
import com.novell.ldapchai.util.BCrypt;
import com.novell.ldapchai.util.ChaiLogger;
import org.jdom.Element;

public class BCryptAnswer implements Answer {
    private static ChaiLogger LOGGER = ChaiLogger.getLogger(Sha1SaltAnswer.class);

    private final String answerHash;
    private final boolean caseInsensitive;

    private BCryptAnswer(final String answerHash, final boolean caseInsensitive) {
        if (answerHash == null || answerHash.length() < 1) {
            throw new IllegalArgumentException("missing answer text");
        }

        this.answerHash = answerHash;
        this.caseInsensitive = caseInsensitive;
    }

    public static BCryptAnswer newAnswer(final String answer, final boolean caseInsensitive) {
        if (answer == null || answer.length() < 1) {
            throw new IllegalArgumentException("missing answerHash text");
        }


        final String salt = BCrypt.gensalt();
        final String casedAnswer = caseInsensitive ? answer.toLowerCase() : answer;
        final String hashedAnswer = BCrypt.hashpw(casedAnswer, salt);
        return new BCryptAnswer(hashedAnswer,caseInsensitive);
    }

    public static BCryptAnswer fromXml(final Element element, final boolean caseInsensitive) {
        final String answerValue = element.getText();
        return new BCryptAnswer(answerValue,caseInsensitive);
    }

    public Element toXml() {
        final Element answerElement = new Element(ChaiResponseSet.XML_NODE_ANSWER_VALUE);
        answerElement.setText(answerHash);
        answerElement.setAttribute(ChaiResponseSet.XML_ATTRIBUTE_CONTENT_FORMAT, Answer.FormatType.BCRYPT.toString());
        return answerElement;
    }


    public boolean testAnswer(final String testResponse) {
        if (testResponse == null) {
            return false;
        }

        final String casedAnswer = caseInsensitive ? testResponse.toLowerCase() : testResponse;
        return BCrypt.checkpw(casedAnswer, answerHash);
    }

    public AnswerBean asAnswerBean() {
        final AnswerBean answerBean = new AnswerBean();
        answerBean.setType(Answer.FormatType.BCRYPT);
        answerBean.setAnswerHash(answerHash);
        answerBean.setCaseInsensitive(caseInsensitive);
        answerBean.setHashCount(-1);
        return answerBean;
    }

}
