package com.novell.ldapchai.cr;

import org.jdom.Element;

public interface Answer {
    public boolean testAnswer(final String answer);

    public Element toXml();
}
