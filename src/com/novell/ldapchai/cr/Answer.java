package com.novell.ldapchai.cr;

import com.novell.ldapchai.cr.bean.AnswerBean;
import com.novell.ldapchai.exception.ChaiOperationException;
import org.jdom2.Element;

public interface Answer {
    public boolean testAnswer(final String answer);

    public Element toXml() throws ChaiOperationException;

    public AnswerBean asAnswerBean();

    public enum FormatType {
        TEXT(new TextAnswer.TextAnswerFactory()),
        MD5(new HashSaltAnswer.HashSaltAnswerFactory()),
        SHA1(new HashSaltAnswer.HashSaltAnswerFactory()),
        SHA1_SALT(new HashSaltAnswer.HashSaltAnswerFactory()),
        SHA256_SALT(new HashSaltAnswer.HashSaltAnswerFactory()),
        SHA512_SALT(new HashSaltAnswer.HashSaltAnswerFactory()),
        BCRYPT(new PasswordCryptAnswer.PasswordCryptAnswerFactory()),
        SCRYPT(new PasswordCryptAnswer.PasswordCryptAnswerFactory()),
        PBKDF2(new PKDBF2Answer.PKDBF2AnswerFactory()),
        HELPDESK(new ChaiHelpdeskAnswer.ChaiHelpdeskAnswerFactory()),
        ;

        private ImplementationFactory factory;


        private FormatType(final ImplementationFactory implementationClass) {
            this.factory = implementationClass;
        }

        public ImplementationFactory getFactory() {
            return factory;
        }
    }

    interface ImplementationFactory {
        Answer newAnswer(AnswerFactory.AnswerConfiguration answerConfiguration, String answerText);

        Answer fromAnswerBean(AnswerBean input, String challengeText);

        Answer fromXml(org.jdom2.Element element, boolean caseInsensitive, String challengeText);
    }


}
