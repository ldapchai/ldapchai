package com.novell.ldapchai.impl.edir.value;

import com.novell.ldapchai.TestHelper;
import org.junit.Assert;
import org.junit.Test;

public class NspmComplexityRulesTest
{
    @Test
    public void testResponseSet1()
            throws Exception
    {
        final String inputXml = TestHelper.readResourceFile( NspmComplexityRulesTest.class, "NspmComplexityRulesTest1.xml" );
        final NspmComplexityRules nspmComplexityRules = new NspmComplexityRules( inputXml );
        Assert.assertFalse( nspmComplexityRules.isMsComplexityPolicy() );
    }
}