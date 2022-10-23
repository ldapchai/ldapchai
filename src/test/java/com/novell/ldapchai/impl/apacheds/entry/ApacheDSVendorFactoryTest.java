package com.novell.ldapchai.impl.apacheds.entry;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

public class ApacheDSVendorFactoryTest
{
    @Test
    public void stringToInstantTest()
    {
        final ApacheDSVendorFactory factory = new ApacheDSVendorFactory();
        Assert.assertEquals( Instant.parse( "2022-10-20T21:46:12.316Z" ), factory.stringToInstant( "20221020214612.316Z" ) );
    }


    @Test
    public void instantToStringTest()
    {
        final ApacheDSVendorFactory factory = new ApacheDSVendorFactory();
        Assert.assertEquals( "20221020214612.316Z", factory.instantToString( Instant.parse( "2022-10-20T21:46:12.316Z" ) ) );
    }

}