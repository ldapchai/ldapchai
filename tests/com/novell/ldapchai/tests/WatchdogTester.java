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
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderFactory;
import com.novell.ldapchai.provider.ChaiSetting;
import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

public class WatchdogTester extends TestCase {
// -------------------------- OTHER METHODS --------------------------

    protected void setUp()
            throws Exception
    {
        TestHelper.setUp();
    }

    public void testProviderLeak()
            throws Exception
    {
        final ChaiEntry testContainer = TestHelper.createTestContainer();
        final int testIterations = TestHelper.testBulkIterations > 150 ? TestHelper.testBulkIterations : 150;


        final int startThreads = Thread.activeCount();
        final Set<ChaiProvider> providerCollection = new HashSet<ChaiProvider>();
        System.out.println("startThreads = " + startThreads);

        final ChaiConfiguration chaiConfig = new ChaiConfiguration(TestHelper.bindURL, TestHelper.bindDN, TestHelper.bindPW);
        chaiConfig.setSetting(ChaiSetting.WATCHDOG_ENABLE, "true");
        chaiConfig.setSetting(ChaiSetting.PROMISCUOUS_SSL, "true");

        for (int i = 0; i < testIterations; i++) {
            final ChaiProvider provider = ChaiProviderFactory.createProvider(chaiConfig);

            provider.readStringAttribute(testContainer.getEntryDN(), "cn");
            providerCollection.add(provider);
            final int currentThreads = Thread.activeCount();
            System.out.println("currentThreads= " + currentThreads + ", iteration: " + i);
        }

        final long idleTime = 5000 + Integer.parseInt(chaiConfig.getSetting(ChaiSetting.WATCHDOG_IDLE_TIMEOUT));
        System.out.printf("sleeping for " + idleTime);
        TestHelper.pause(idleTime);

        int stopThreads = Thread.activeCount();
        System.out.println("startThreads = " + startThreads);
        System.out.println("stopThreads = " + stopThreads);

        System.out.println("re-using connections");

        int i = 0;
        for (final ChaiProvider provider : providerCollection) {
            provider.getChaiConfiguration();
            provider.readStringAttribute(testContainer.getEntryDN(), "cn");
            final int currentThreads = Thread.activeCount();
            System.out.println("currentThreads= " + currentThreads + ", iteration: " + i++);
        }

        System.out.printf("sleeping for " + idleTime);
        TestHelper.pause(idleTime);

        stopThreads = Thread.activeCount();
        System.out.println("startThreads = " + startThreads);
        System.out.println("stopThreads = " + stopThreads);

        System.out.println("re-using connections");

        if (stopThreads > startThreads) {
            System.out.println("\nthread dump:");
            final Thread[] tarray = new Thread[Thread.activeCount()];
            Thread.enumerate(tarray);
            for (final Thread t : tarray) {
                System.out.println(t.toString() + t.getStackTrace()[0]);
            }
            throw new Exception("possible thread leak startThreads=" + startThreads + " stopThreads=" + stopThreads);
        }
    }

    public void testWatchdogBasic()
            throws Exception
    {
        final ChaiEntry testContainer = TestHelper.createTestContainer();
        final long idleTime = 5 * 1000;


        final ChaiConfiguration chaiConfig = new ChaiConfiguration(TestHelper.bindURL, TestHelper.bindDN, TestHelper.bindPW);
        chaiConfig.setSetting(ChaiSetting.WATCHDOG_ENABLE, "true");
        chaiConfig.setSetting(ChaiSetting.PROMISCUOUS_SSL, "true");
        chaiConfig.setSetting(ChaiSetting.WATCHDOG_IDLE_TIMEOUT, String.valueOf(idleTime));
        final ChaiProvider provider = ChaiProviderFactory.createProvider(chaiConfig);

        //do initial read
        String cnValue = provider.readStringAttribute(testContainer.getEntryDN(), "ou");
        System.out.println("cnValue = " + cnValue);


        TestHelper.pause((long) (idleTime * 1.5));

        cnValue = provider.readStringAttribute(testContainer.getEntryDN(), "ou");
        System.out.println("cnValue = " + cnValue);
    }
}
