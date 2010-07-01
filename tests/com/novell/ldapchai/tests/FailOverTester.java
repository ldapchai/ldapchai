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
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.provider.ChaiConfiguration;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderFactory;
import com.novell.ldapchai.provider.ChaiSetting;
import com.novell.ldapchai.tests.util.TcpProxy;
import junit.framework.TestCase;
import org.junit.Assert;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 */
public class FailOverTester extends TestCase {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final int basePort = 5000;

// -------------------------- OTHER METHODS --------------------------

    public void testMultiServerFailover()
            throws Exception
    {
        TestHelper.configureLogging();
        final InetSocketAddress destinationAddress = figureDestSocketAddress();
        final TcpProxy proxy1 = new TcpProxy(basePort + 1, destinationAddress);
        final TcpProxy proxy2 = new TcpProxy(basePort + 2, destinationAddress);
        final TcpProxy proxy3 = new TcpProxy(basePort + 3, destinationAddress);

        proxy2.start();

        final ChaiConfiguration testConfig = makeChaiConfig(figureUrlForProxy(proxy1, proxy2, proxy3));
        final ChaiProvider testProvider = ChaiProviderFactory.createProvider(testConfig);
        final ChaiEntry testContainer = TestHelper.createTestContainer(testProvider);
        final ChaiUser testUser = TestHelper.createNewTestUser(testContainer);

        TestHelper.doBasicNonDestructiveUserTest(testUser);

        proxy2.stop();
        TestHelper.pause(1000);

        {
            // test to make sure we get unavailable error
            boolean gotError = false;
            try {
                TestHelper.doBasicNonDestructiveUserTest(testUser);
            } catch (ChaiUnavailableException e) {
                System.out.println("got expected unavailable error: " + e.getMessage());
                gotError = true;
            }
            Assert.assertTrue(gotError);
        }

        proxy1.start();
        TestHelper.pause(1000);
        TestHelper.doBasicNonDestructiveUserTest(testUser);

        proxy1.stop();
        proxy3.start();
        TestHelper.pause(1000);
        TestHelper.doBasicNonDestructiveUserTest(testUser);
    }

    public void testSingleServerRestart()
            throws Exception
    {
        TestHelper.configureLogging();
        final InetSocketAddress destinationAddress = figureDestSocketAddress();
        final TcpProxy proxy1 = new TcpProxy(basePort + 1, destinationAddress);

        proxy1.start();

        final ChaiConfiguration testConfig = makeChaiConfig(figureUrlForProxy(proxy1));
        final ChaiProvider testProvider = ChaiProviderFactory.createProvider(testConfig);
        final ChaiEntry testContainer = TestHelper.createTestContainer(testProvider);
        final ChaiUser testUser = TestHelper.createNewTestUser(testContainer);

        TestHelper.doBasicNonDestructiveUserTest(testUser);

        proxy1.stop();
        TestHelper.pause(1000);
        // test to make sure we get errors

        boolean gotError = false;
        try {
            TestHelper.doBasicNonDestructiveUserTest(testUser);
        } catch (ChaiUnavailableException e) {
            System.out.println("got expected unavailable error: " + e.getMessage());
            gotError = true;
        }

        Assert.assertTrue(gotError);

        proxy1.start();
        TestHelper.pause(1000);

        TestHelper.doBasicNonDestructiveUserTest(testUser);
    }

    private static InetSocketAddress figureDestSocketAddress()
            throws Exception
    {
        final URI bindURL = new URI(TestHelper.bindURL);
        final String bindHost = bindURL.getHost();
        final int bindPort = bindURL.getPort();
        return new InetSocketAddress(bindHost, bindPort);
    }

    private static ChaiConfiguration makeChaiConfig(final String url)
    {
        final ChaiConfiguration config = new ChaiConfiguration();
        config.setSetting(ChaiSetting.BIND_URLS, url);
        config.setSetting(ChaiSetting.BIND_DN, TestHelper.bindDN);
        config.setSetting(ChaiSetting.BIND_PASSWORD, TestHelper.bindPW);
        config.setSetting(ChaiSetting.FAILOVER_ENABLE, "true");
        config.setSetting(ChaiSetting.STATISTICS_ENABLE, "false");
        config.setSetting(ChaiSetting.WATCHDOG_ENABLE, "false");
        config.setSetting(ChaiSetting.PROMISCUOUS_SSL, "true");
        return config;
    }

    private static String figureUrlForProxy(final TcpProxy... proxies)
            throws Exception
    {
        final URI bindURL = new URI(TestHelper.bindURL);
        final StringBuilder sb = new StringBuilder();

        for (final TcpProxy loopProxy : proxies) {
            sb.append(bindURL.getScheme());
            sb.append("://");
            sb.append(loopProxy.getListenInfo().getHostName());
            sb.append(":");
            sb.append(loopProxy.getListenInfo().getPort());

            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }
}
