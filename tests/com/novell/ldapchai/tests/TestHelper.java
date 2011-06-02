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
import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiException;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderFactory;
import com.novell.ldapchai.provider.ChaiSetting;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;

import java.util.*;

public class TestHelper {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    static int testContainerCounter = 0;

    static String bindURL = ResourceBundle.getBundle("com.novell.ldapchai.tests.TestParameters").getString("ldapURL");
    static String bindDN = ResourceBundle.getBundle("com.novell.ldapchai.tests.TestParameters").getString("ldapBindDN");
    static String bindPW = ResourceBundle.getBundle("com.novell.ldapchai.tests.TestParameters").getString("ldapBindPW");

    static String testContainer = ResourceBundle.getBundle("com.novell.ldapchai.tests.TestParameters").getString("testContainer");
    static String testProviderImpl = ResourceBundle.getBundle("com.novell.ldapchai.tests.TestParameters").getString("testProviderImpl");
    static int testBulkIterations = Integer.valueOf(ResourceBundle.getBundle("com.novell.ldapchai.tests.TestParameters").getString("testBulkIterations"));

    private static ChaiProvider cachedProvider;

// -------------------------- STATIC METHODS --------------------------

    public static void cleanUp(final ChaiEntry object)
            throws ChaiUnavailableException
    {
        System.out.print("cleaning up " + object.getEntryDN() + "...");

        try {
            implCleanUp(object);
        } catch (ChaiOperationException e) {
            System.out.print("Error during cleanup: " + e.getMessage());
        }
        System.out.println("...done");
    }

    private static void implCleanUp(final ChaiEntry object)
            throws ChaiUnavailableException, ChaiOperationException
    {
        try {
            final Set<ChaiEntry> results = object.getChildObjects();
            for (final ChaiEntry loopEntry : results) {
                if (!loopEntry.equals(object)) {
                    implCleanUp(loopEntry);
                }
            }

            if (!object.getEntryDN().equals("")) {
                System.out.print(".");
                object.getChaiProvider().deleteEntry(object.getEntryDN());
            }
        } catch (ChaiOperationException e) {
            if (e.getErrorCode() != ChaiError.NO_SUCH_ENTRY) {
                throw e;
            }
        }
    }

    public static ChaiUser createNewTestUser(final ChaiEntry testContainer)
            throws Exception
    {
        final String createDN = "cn=testUser," + testContainer.getEntryDN();
        final String createClass = "inetOrgPerson";
        final Map<String,String> createAttributes = new HashMap<String, String>();

        createAttributes.put("givenName", "Test");
        createAttributes.put("sn", "User");
        createAttributes.put("title", "TestUser");
        createAttributes.put("mail", "test@test.test");

        // perform the create operation in eDirectory
        try {
            testContainer.getChaiProvider().createEntry(createDN, createClass, createAttributes);
        } catch (ChaiException e) {
            throw new Exception("error creating test user", e);
        }

        return ChaiFactory.createChaiUser(createDN, testContainer.getChaiProvider());
    }

    static synchronized ChaiEntry createTestContainer()
            throws Exception
    {
        final String ouName = "ou=test" + testContainerCounter++ + "," + testContainer;

        final String createClass = "OrganizationalUnit";

        //clear out any existing data
        cleanUp(ouName, getProvider());

        //create container;
        getProvider().createEntry(ouName, createClass, null);

        //return container
        return ChaiFactory.createChaiEntry(ouName, getProvider());
    }

    public static void cleanUp(
            final String dn,
            final ChaiProvider provider
    )
            throws ChaiUnavailableException, ChaiOperationException
    {
        cleanUp(ChaiFactory.createChaiEntry(dn, provider));
    }

    public static ChaiProvider getProvider()
            throws Exception
    {
        if (cachedProvider == null) {
            final ChaiProvider newProvider;
            try {
                final ChaiConfiguration chaiConfig = new ChaiConfiguration(TestHelper.bindURL, TestHelper.bindDN, TestHelper.bindPW);
                chaiConfig.setSetting(ChaiSetting.PROVIDER_IMPLEMENTATION, testProviderImpl);
                chaiConfig.setSetting(ChaiSetting.PROMISCUOUS_SSL, "true");

                //chaiConfig.setSetting(ChaiSetting.WATCHDOG_ENABLE, "false");
                //chaiConfig.setSetting(ChaiSetting.FAILOVER_ENABLE, "false");

                newProvider = ChaiProviderFactory.createProvider(chaiConfig);
            } catch (ChaiException e) {
                throw new Exception("Cannot connect to test ldap directory", e);
            }

            // test for test container
            try {
                newProvider.readStringAttribute(TestHelper.testContainer, "objectClass");
            } catch (Exception e) {
                throw new Exception("Cannot connect to test ldap directory, missing test container", e);
            }

            cachedProvider = newProvider;
        }

        return cachedProvider;
    }

    static synchronized ChaiEntry createTestContainer(final ChaiProvider provider)
            throws Exception
    {
        final String ouName = "ou=test" + testContainerCounter++ + "," + testContainer;

        final String createClass = "OrganizationalUnit";

        //clear out any existing data
        cleanUp(ouName, provider);

        //create container;
        provider.createEntry(ouName, createClass, null);

        //return container
        return ChaiFactory.createChaiEntry(ouName, provider);
    }

    public static void doBasicNonDestructiveUserTest(final ChaiUser user)
            throws Exception
    {
        {
            final String value = "value1";
            user.writeStringAttribute("givenName", value);
            Assert.assertEquals(user.readStringAttribute("givenName"), value);
        }

        {
            final Set<String> values = new HashSet<String>();
            values.add("value1");
            values.add("value2");
            values.add("value3");
            user.writeStringAttribute("givenName", values);
            Assert.assertEquals(user.readMultiStringAttribute("givenName"), values);
        }


        System.out.println("user " + user.toString() + " passed non-desctructive testing");
    }

    /**
     * Causes the executing thread to pause for a period of time.
     *
     * @param time in ms
     */
    public static void pause(final long time)
    {
        final long startTime = System.currentTimeMillis();
        do {
            try {
                final long sleepTime = time - (System.currentTimeMillis() - startTime);
                Thread.sleep(sleepTime > 0 ? sleepTime : 10);
            } catch (InterruptedException e) {
                //don't care
            }
        } while ((System.currentTimeMillis() - startTime) < time);
    }

    public static void setUp()
    {
        configureLogging();
    }

    public static void configureLogging()
    {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.TRACE);
    }
}
