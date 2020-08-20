package com.novell.ldapchai.impl.edir.entry;


import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;


public class EdirEntriesTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldReturnExceptionWhenConvertZuluToDateValueIsToShort()
    {
        EdirEntries.convertZuluToInstant("01234567890123");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldReturnExceptionWhenConvertZuluToDateValueDoesNotHaveZAtChar14() throws Exception {
        EdirEntries.convertZuluToInstant("012345678901234");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldReturnExceptionWhenConvertZuluToDateValueDoesHaveZAtChar14ButIsLonger()
            throws Exception
    {
        EdirEntries.convertZuluToInstant( "20150101000000Z9" );
    }

    @Test(expected = NullPointerException.class)
    public void shouldReturnExceptionWhenConvertZuluToDateValueIsNull() throws Exception {
        EdirEntries.convertZuluToInstant(null);
    }

    @Test
    public void shouldReturnInstantWhenConvertZuluToInstantValueIsCorrect() throws Exception {
        Instant d = EdirEntries.convertZuluToInstant("20150402010745Z");
        Assert.assertEquals( d.toEpochMilli(), 1427936865000L );
    }

    @Test
    public void shouldReturnStringWhenConvertInstantToStringIsCorrect() throws Exception {
        final Instant input = Instant.ofEpochMilli( 1427936865000L );
        final String zuluTimestamp = EdirEntries.convertInstantToZulu( input );
        Assert.assertEquals(zuluTimestamp, "20150402010745Z");
    }
}
