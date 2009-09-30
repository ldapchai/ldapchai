/*
 * LDAP Chai API
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009 Jason D. Rivard
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
import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.cr.*;
import com.novell.ldapchai.exception.ChaiValidationException;
import com.novell.ldapchai.impl.edir.entry.EdirEntries;
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderFactory;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.util.ChaiUtility;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.*;


public class ChaiTester extends TestCase {
// -------------------------- OTHER METHODS --------------------------

    protected void setUp()
            throws Exception
    {
        TestHelper.setUp();
    }

    public void testChaiResponseSet()
            throws Exception
    {
        final ChaiEntry testContainer = TestHelper.createTestContainer();
        final ChaiUser testUser;
        {    // create provider and test user.
            final ChaiConfiguration chaiConfig = new ChaiConfiguration("ldaps://ldaphost:636", "cn=admin,ou=ou,o=o", "novell");
            chaiConfig.setSetting(ChaiSetting.PROMISCUOUS_SSL, "true");
            final ChaiProvider provider = ChaiProviderFactory.createProvider(chaiConfig);

            testUser = ChaiUtility.createUser("cn=responseTestUser," + testContainer.getEntryDN(), "sn", provider);
        }

        // create challenges/responses
        final Map<Challenge, String> crMap;
        {
            final Map<Challenge, String> tempMap = new HashMap<Challenge, String>();
            tempMap.put(CrFactory.newChallenge(true, "c1", 5, 200, true), "response1");
            tempMap.put(CrFactory.newChallenge(true, "c2", 5, 200, true), "response2");
            tempMap.put(CrFactory.newChallenge(false, "c3", 5, 200, true), "response3");
            tempMap.put(CrFactory.newChallenge(false, "c4", 5, 200, true), "response4");
            crMap = Collections.unmodifiableMap(tempMap);
        }

        // write responses to user entry
        CrFactory.newResponseSet(crMap, null, 0, testUser, null).write(null);

        // read responses from user entry
        final ResponseSet retreivedSet = testUser.readResponseSet();

        Assert.assertTrue("error testing chai responses", retreivedSet.test(crMap));

        {
            final Map<Challenge, String> testMap = new HashMap<Challenge, String>(crMap);
            testMap.put(CrFactory.newChallenge(true, "c2", 5, 200, true), "response3");
            Assert.assertFalse("error testing chai responses, false positive", retreivedSet.test(testMap));
        }

        {
            final Map<Challenge, String> testMap = new HashMap<Challenge, String>(crMap);
            testMap.put(CrFactory.newChallenge(true, "c2", 50, 200, true), "response2");
            try {
                CrFactory.newResponseSet(testMap, null, 0, testUser, null).write(null);
                Assert.fail("did not throw expected IllegalArgumentException due to response length being to short");
            } catch (ChaiValidationException e) { /* test should throw exception */ }
        }

        {
            final ResponseSet testRs = CrFactory.newResponseSet(crMap, null, 1, testUser, null);
            final ChallengeSet testCs = CrFactory.newChallengeSet(crMap.keySet(), null, 1, null);
            Assert.assertTrue("meetsChallengeSetRequirements failed positive test", testRs.meetsChallengeSetRequirements(testCs));
        }

        {
            final Map<Challenge, String> testMap = new HashMap<Challenge, String>();
            testMap.put(CrFactory.newChallenge(true, "c1", 5, 200, true), "response1");
            testMap.put(CrFactory.newChallenge(true, "c2", 5, 200, true), "response2");
            final ResponseSet testRs = CrFactory.newResponseSet(testMap, null, 1, testUser, null);
            final ChallengeSet testCs = CrFactory.newChallengeSet(crMap.keySet(), null, 2, null);

            try {
                testRs.meetsChallengeSetRequirements(testCs);
                Assert.fail("meetsChallengeSetRequirements failed positive test");
            } catch (ChaiValidationException e) { /* test should throw exception */ }
        }
    }

    public void testClosedProvider()
            throws Exception
    {
        final ChaiConfiguration testConfig = new ChaiConfiguration(TestHelper.bindURL, TestHelper.bindDN, TestHelper.bindPW);
        testConfig.setSetting(ChaiSetting.PROMISCUOUS_SSL, "true");
        testConfig.setSetting(ChaiSetting.WATCHDOG_ENABLE, "true");
        testConfig.setSetting(ChaiSetting.STATISTICS_ENABLE, "true");
        testConfig.setSetting(ChaiSetting.FAILOVER_ENABLE, "true");

        final ChaiProvider testProvider = ChaiProviderFactory.createProvider(testConfig);
        final ChaiEntry testContainer = TestHelper.createTestContainer(testProvider);
        final ChaiUser testUser = TestHelper.createNewTestUser(testContainer);


        TestHelper.doBasicNonDestructiveUserTest(testUser);

        testProvider.close();

        {
            boolean gotError = false;
            try {
                TestHelper.doBasicNonDestructiveUserTest(testUser);
            } catch (IllegalStateException e) {
                gotError = true;
            }
            Assert.assertTrue(gotError);
        }

        // all should be able to be called on a closed provider.
        testProvider.close();
        testProvider.getProviderStatistics();
    }

    public void testCreateBulk()
            throws Exception
    {
        final ChaiEntry testContainer = TestHelper.createTestContainer();

        final String userClass = "inetOrgPerson";
        for (int i = 0; i < TestHelper.testBulkIterations; i++) {
            final String dn = "cn=user" + i + "," + testContainer.getEntryDN();
            final Properties props = new Properties();
            props.setProperty("givenName", "first" + i);
            props.setProperty("sn", "last" + i);
            TestHelper.getProvider().createEntry(dn, userClass, props);
            System.out.println("Created " + dn);
        }
    }

    public void testCreateNmasResponses()
            throws Exception
    {
        final ChaiEntry testContainer = TestHelper.createTestContainer();

        final String createDN = "cn=sadams," + testContainer.getEntryDN();
        final String createClass = "inetOrgPerson";
        final Properties createAttributes = new Properties();

        createAttributes.setProperty("givenName", "Sam");
        createAttributes.setProperty("sn", "Adams");
        createAttributes.setProperty("title", "Revolutionary");
        createAttributes.setProperty("mail", "mc@teaparty.org");

        // perform the create operation in eDirectory
        TestHelper.getProvider().createEntry(createDN, createClass, createAttributes);
        final ChaiUser theUser = ChaiFactory.createChaiUser(createDN, TestHelper.getProvider());

        final Map<Challenge, String> crMap = new HashMap<Challenge, String>();
        crMap.put(CrFactory.newChallenge(true, "Got Milk?", 2, 255, true), "yep");
        crMap.put(CrFactory.newChallenge(true, "Zoinks?", 2, 255, true), "Zoinks!");

        final ResponseSet rs = CrFactory.newResponseSet(crMap, null, 2, theUser, null);
        Assert.assertTrue("NMAS Response Writing Test failed", rs.write(CrMode.NMAS));
    }

    public void testCreateUser()
            throws Exception
    {
        final ChaiEntry testContainer = TestHelper.createTestContainer();


        final String createDN = "cn=chaiCreateTestUser," + testContainer.getEntryDN();
        final String createClass = "inetOrgPerson";
        final Properties createAttributes = new Properties();

        createAttributes.setProperty("givenName", "GivenNameValue");
        createAttributes.setProperty("sn", "SurnameValue");
        createAttributes.setProperty("title", "test.Tester");
        createAttributes.setProperty("mail", "test@test.test");

        // perform the create operation in eDirectory
        TestHelper.getProvider().createEntry(createDN, createClass, createAttributes);
    }

    public void testIsPasswordExpired()
            throws Exception
    {
        final ChaiEntry testContainer = TestHelper.createTestContainer();


        final String createDN = "cn=chaiPasswordExpiredTestUser," + testContainer.getEntryDN();
        final String createClass = "inetOrgPerson";
        final Properties createAttributes = new Properties();

        createAttributes.setProperty("givenName", "GivenNameValue");
        createAttributes.setProperty("sn", "SurnameValue");
        createAttributes.setProperty("title", "test.Tester");
        createAttributes.setProperty("mail", "est@test.test");

        // perform the create operation in eDirectory
        TestHelper.getProvider().createEntry(createDN, createClass, createAttributes);
        final ChaiUser theUser = ChaiFactory.createChaiUser(createDN, TestHelper.getProvider());

        if (theUser.isPasswordExpired()) {
            throw new Exception("password is expired, but shouldn't be");
        }

        theUser.setPassword("newPAssW04d!");
        theUser.writeStringAttribute(ChaiUser.ATTR_PASSWORD_EXPIRE_TIME, EdirEntries.convertDateToZulu(new Date()));

        if (!theUser.isPasswordExpired()) {
            Assert.fail("password should not be expired, but is");
        }
    }

    public void testReplicationChecker()
            throws Exception
    {
        final ChaiEntry testContainer = TestHelper.createTestContainer();

        testContainer.writeStringAttribute("description", "testValue" + (new Random()).nextInt());

        final int maximumWaitTime = 120 * 1000;
        final int pauseTime = 3 * 1000;

        final long startTime = System.currentTimeMillis();
        boolean replicated = false;
        while (System.currentTimeMillis() - startTime < (maximumWaitTime)) {
            try {
                Thread.sleep(pauseTime);
            } catch (InterruptedException e) {
            }
            replicated = ChaiUtility.testAttributeReplication(testContainer, "description", null);
            if (replicated) {
                break;
            }
        }
        System.out.println("Attribute replication successful: " + replicated);


        Assert.assertTrue("attributes never synchronized", replicated);
    }
}
