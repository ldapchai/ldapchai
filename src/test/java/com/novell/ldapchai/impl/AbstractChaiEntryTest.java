package com.novell.ldapchai.impl;

import com.novell.ldapchai.provider.ChaiProvider;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


public class AbstractChaiEntryTest {

    private AbstractChaiEntryMakeReal classUnderTest;
    private ChaiProvider mockChiProvider;
    private static String ANY_DN = "testdn";
    private static String ANY_DATE_VALUE = "dateKey";

    @Before
    public void setup() {
        mockChiProvider = mock(ChaiProvider.class);
        classUnderTest =  new AbstractChaiEntryMakeReal("testdn", mockChiProvider);
    }

    @Test
    public void shouldReturnNullWhenReadDateAttributeRetrievesValueNull() throws Exception {
        when(mockChiProvider.readStringAttribute(ANY_DN, ANY_DATE_VALUE)).thenReturn(null);
        assertThat(classUnderTest.readDateAttribute(ANY_DATE_VALUE), is(IsNull.nullValue()));
    }

    @Test
    public void shouldReturnNullWhenReadDateAttributeRetrievesValue0() throws Exception {
        when(mockChiProvider.readStringAttribute(ANY_DN, ANY_DATE_VALUE)).thenReturn("0");
        assertThat(classUnderTest.readDateAttribute(ANY_DATE_VALUE), is(IsNull.nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldReturnExceptionWhenReadDateAttributeValueIsToShort() throws Exception {
        when(mockChiProvider.readStringAttribute(ANY_DN, ANY_DATE_VALUE)).thenReturn("01234567890123");
        try {
            classUnderTest.readDateAttribute(ANY_DATE_VALUE);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("zulu date too short"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldReturnExceptionWhenReadDateAttributeValueDoesNotHaveZAtChar14() throws Exception {
        when(mockChiProvider.readStringAttribute(ANY_DN, ANY_DATE_VALUE)).thenReturn("0123456789012345");
        try {
            classUnderTest.readDateAttribute(ANY_DATE_VALUE);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("zulu date must end in 'Z'"));
            throw e;
        }
    }

    public class AbstractChaiEntryMakeReal extends  AbstractChaiEntry {

        public AbstractChaiEntryMakeReal(final String entryDN, final ChaiProvider chaiProvider) {
            super(entryDN, chaiProvider);
        }
    }
}
