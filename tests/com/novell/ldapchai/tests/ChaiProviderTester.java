/*
 * LDAP Chai API
 * Copyright (c) 2006-2010 Novell, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.novell.ldapchai.tests;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.provider.*;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChaiProviderTester extends TestCase {
// ------------------------------ FIELDS ------------------------------

    final private String testAttribute = "roomNumber";  //generally unused standard CaseIgnoreString attr

// -------------------------- OTHER METHODS --------------------------

    protected void setUp()
            throws Exception
    {
        TestHelper.setUp();
    }

    public void testDeleteAttribute()
            throws Exception
    {
        final ChaiProvider[] providers = this.getProviders();
        for (final ChaiProvider provider : providers) {
            final ChaiEntry testContainer = TestHelper.createTestContainer();
            final String testUserDN = TestHelper.createNewTestUser(testContainer).getEntryDN();
            System.out.println("Testing provider " + provider.toString());

            {   // test single value writes.
                provider.writeStringAttribute(testUserDN, testAttribute, new HashSet<String>(Arrays.asList("value1", "value2")), false);
                final Set<String> results = provider.readMultiStringAttribute(testUserDN, testAttribute);
                Assert.assertTrue("setup values failed", results.size() == 2 && results.contains("value1") && results.contains("value2"));
            }

            {   // delete existing value
                provider.deleteStringAttributeValue(testUserDN, testAttribute, "value2");
                final Set<String> results = provider.readMultiStringAttribute(testUserDN, testAttribute);
                Assert.assertTrue("delete existing value failed", results.size() == 1 && results.contains("value1"));
            }

            {   // delete non existing value
                try {
                    provider.deleteStringAttributeValue(testUserDN, testAttribute, "value2");
                    Assert.fail("missing exception during delete of non-existant value");
                } catch (ChaiOperationException e) {
                    Assert.assertEquals("missing NO_SUCH_VALUE error", e.getErrorCode(), ChaiError.NO_SUCH_VALUE);
                }
            }
        }
    }

    public void testWriteStringAttribute()
            throws Exception
    {
        final ChaiProvider[] providers = this.getProviders();
        for (final ChaiProvider provider : providers) {
            final ChaiEntry testContainer = TestHelper.createTestContainer();
            final String testUserDN = TestHelper.createNewTestUser(testContainer).getEntryDN();
            System.out.println("Testing provider " + provider.toString());

            {   // test single value writes.
                provider.writeStringAttribute(testUserDN, testAttribute, Collections.singleton("value1"), false);
                final Set<String> results = provider.readMultiStringAttribute(testUserDN, testAttribute);
                Assert.assertTrue("single write failed", results.size() == 1 && results.contains("value1"));
            }

            {   // append single value writes.
                provider.writeStringAttribute(testUserDN, testAttribute, Collections.singleton("value2"), false);
                final Set<String> results = provider.readMultiStringAttribute(testUserDN, testAttribute);
                Assert.assertTrue("single append failed", results.size() == 2 && results.contains("value1") && results.contains("value2"));
            }

            {   // overwrite single value writes.
                provider.writeStringAttribute(testUserDN, testAttribute, Collections.singleton("value3"), true);
                final Set<String> results = provider.readMultiStringAttribute(testUserDN, testAttribute);
                Assert.assertTrue("single overwrite failed", results.size() == 1 && results.contains("value3"));
            }

            {   // append multi value writes.
                provider.writeStringAttribute(testUserDN, testAttribute, new HashSet<String>(Arrays.asList("value4", "value5")), false);
                final Set<String> results = provider.readMultiStringAttribute(testUserDN, testAttribute);
                Assert.assertTrue("multi append failed", results.size() == 3 && results.contains("value3") && results.contains("value4") && results.contains("value5"));
            }

            {   // overwrite multi value writes.
                provider.writeStringAttribute(testUserDN, testAttribute, new HashSet<String>(Arrays.asList("value6", "value7")), true);
                final Set<String> results = provider.readMultiStringAttribute(testUserDN, testAttribute);
                Assert.assertTrue("multi overwrite failed", results.size() == 2 && results.contains("value6") && results.contains("value7"));
            }
        }
    }

    private ChaiProvider[] getProviders()
            throws Exception
    {
        final ChaiProvider jldapProvider;
        {
            final ChaiConfiguration chaiConfig = new ChaiConfiguration(TestHelper.bindURL, TestHelper.bindDN, TestHelper.bindPW);
            chaiConfig.setSetting(ChaiSetting.PROVIDER_IMPLEMENTATION, JLDAPProviderImpl.class.getName());
            chaiConfig.setSetting(ChaiSetting.PROMISCUOUS_SSL,"true");
            jldapProvider = ChaiProviderFactory.createProvider(chaiConfig);
        }

        return new ChaiProvider[]{TestHelper.getProvider(), jldapProvider};
    }
}
