package com.novell.ldapchai.cr;

import com.novell.ldapchai.exception.ChaiOperationException;
import org.jdom.Element;

public interface Answer {
    public boolean testAnswer(final String answer);

    public Element toXml() throws ChaiOperationException;
}
