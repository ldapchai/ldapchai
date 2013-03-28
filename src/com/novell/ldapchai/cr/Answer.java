package com.novell.ldapchai.cr;

import com.novell.ldapchai.cr.bean.AnswerBean;
import com.novell.ldapchai.exception.ChaiOperationException;
import org.jdom.Element;

public interface Answer {
    public boolean testAnswer(final String answer);

    public Element toXml() throws ChaiOperationException;

    public AnswerBean asAnswerBean();

    enum FormatType {
        TEXT,
        SHA1,
        SHA1_SALT,
        HELPDESK,
        BCRYPT

        ;
    }
}
