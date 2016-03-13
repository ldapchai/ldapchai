package com.novell.ldapchai.impl.edir.entry;


import org.hamcrest.core.IsNull;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EdirEntriesTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldReturnExceptionWhenConvertZuluToDateValueIsToShort() throws Exception {

        try {
            EdirEntries.convertZuluToDate("01234567890123");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("zulu date too short"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldReturnExceptionWhenConvertZuluToDateValueDoesNotHaveZAtChar14() throws Exception {
        try {
            EdirEntries.convertZuluToDate("012345678901234");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("zulu date must end in 'Z'"));
            throw e;
        }
    }

    @Test
    public void shouldReturnExceptionWhenConvertZuluToDateValueDoesHaveZAtChar14ButIsLonger() throws Exception {
        Date d = EdirEntries.convertZuluToDate("20150101000000Z9");
        assertThat(d.getTime(), is(1420070400000L));
    }

    @Test(expected = NullPointerException.class)
    public void shouldReturnExceptionWhenConvertZuluToDateValueIsNull() throws Exception {
        try {
            EdirEntries.convertZuluToDate(null);
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), is(IsNull.nullValue()));
            throw e;
        }
    }

    @Test
    public void shouldReturnDateWhenConvertZuluToDateValueIsCorrect() throws Exception {
        Date d = EdirEntries.convertZuluToDate("20150402010745Z");
        assertThat(d.getTime(), is(1427936865000L));
    }
}
