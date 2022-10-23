package com.novell.ldapchai.impl.edir.entry;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

public class EDirectoryVendorFactoryTest
{
    @Test
    public void stringToInstantTest()
    {
        final EDirectoryVendorFactory factory = new EDirectoryVendorFactory();
        Assert.assertEquals( Instant.parse( "2022-10-19T19:57:31Z" ), factory.stringToInstant( "20221019195731Z" ) );
    }


    @Test
    public void instantToStringTest()
    {
        final EDirectoryVendorFactory factory = new EDirectoryVendorFactory();
        Assert.assertEquals( "20221020214612Z", factory.instantToString( Instant.parse( "2022-10-20T21:46:12Z" ) ) );
    }
}