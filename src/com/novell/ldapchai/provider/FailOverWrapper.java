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

package com.novell.ldapchai.provider;

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.ChaiLogger;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Failover provider.
 *
 * @author Jason D. Rivard
 * @see ChaiSetting#FAILOVER_ENABLE
 */
class FailOverWrapper implements InvocationHandler {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(FailOverWrapper.class);

    private volatile RotationMachine rotationMachine;
    private final ChaiProvider originalProvider;

    private final FailOverSettings settings;
    private volatile boolean closed = false;

// -------------------------- STATIC METHODS --------------------------

    static ChaiProviderImplementor forConfiguration(final ChaiConfiguration chaiConfig)
            throws ChaiUnavailableException
    {
        final Class[] interfaces = new Class[]{ChaiProviderImplementor.class};

        final Object newProxy = Proxy.newProxyInstance(
                chaiConfig.getClass().getClassLoader(),
                interfaces,
                new FailOverWrapper(chaiConfig));

        return (ChaiProviderImplementor) newProxy;
    }

    /**
     * Causes the executing thread to pause for a period of time.
     *
     * @param time in ms
     */
    private static void pause(final long time)
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

// --------------------------- CONSTRUCTORS ---------------------------

    private FailOverWrapper(final ChaiConfiguration chaiConfig)
            throws ChaiUnavailableException
    {
        final int settingMaxRetries = Integer.parseInt(chaiConfig.getSetting(ChaiSetting.FAILOVER_CONNECT_RETRIES));
        final int settingMinFailbackTime = Integer.parseInt(chaiConfig.getSetting(ChaiSetting.FAILOVER_MINIMUM_FAILBACK_TIME));

        final ChaiProviderImplementor failOverHelper;
        try {
            failOverHelper = ChaiProviderFactory.createConcreateProvider(chaiConfig, false);
        } catch (Exception e) {
            throw new ChaiUnavailableException(
                    "unable to create a required concrete provider for the failover wrapper",
                    ChaiError.CHAI_INTERNAL_ERROR);
        }

        this.settings = new FailOverSettings(failOverHelper);
        this.settings.maxRetries = settingMaxRetries;
        this.settings.minFailBackTime = settingMinFailbackTime;

        rotationMachine = new RotationMachine(chaiConfig, settings);

        // call get current provider.  must be able to connect, else should not return a new instance.
        originalProvider = rotationMachine.getCurrentProvider();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface InvocationHandler ---------------------

    public Object invoke(final Object proxy, final Method m, final Object[] args)
            throws Throwable
    {
        if (m.getName().equals("close")) {
            closeThis();
            return Void.TYPE;
        }

        final boolean isLdap = m.getAnnotation(ChaiProviderImplementor.LdapOperation.class) != null;

        if (closed || !isLdap) {
            return AbstractWrapper.invoker(originalProvider, m, args);
        } else {
            return failOverInvoke(m, args);
        }
    }

// -------------------------- OTHER METHODS --------------------------

    private void closeThis() {
        closed = true;
        if (rotationMachine != null) {
            rotationMachine.destoryAllConnections();
            rotationMachine = null;
        }

        originalProvider.close();
    }

    private Object failOverInvoke(final Method m, final Object[] args)
            throws ChaiUnavailableException, ChaiOperationException
    {
        int attempts = 0;
        final int maxAttempts = settings.getMaxRetries();
        while (attempts < maxAttempts) {
            // Check to make sure we haven't been closed while looping.
            if (closed || rotationMachine == null) {
                LOGGER.debug("close detected while inside retry loop, throwing ChaiUnavailableException");
                throw new ChaiUnavailableException("FailOverWrapper closed while retrying connection", ChaiError.COMMUNICATION);
            }

            // fetch the current active provider from the machine.  If unable to reach
            // any ldap servers, this will throw ChaiUnavailable right here.
            final ChaiProvider currentProvider;
            try {
                currentProvider = rotationMachine.getCurrentProvider();
            } catch (NullPointerException e) {
                LOGGER.debug("RotationMachine unavailable");
                throw new ChaiUnavailableException("RotationMachine unavailable while retrying connection", ChaiError.COMMUNICATION);
            }

            try {
                return AbstractWrapper.invoker(currentProvider, m, args);
            } catch (Exception e) {
                if (settings.errorIsRetryable(e) && !closed) {
                    rotationMachine.reportBrokenProvider(currentProvider);
                } else {
                    if (e instanceof ChaiOperationException) {
                        throw (ChaiOperationException) e;
                    } else if (e instanceof ChaiUnavailableException) {
                        throw (ChaiUnavailableException)e;
                    } else {
                        LOGGER.warn("unexpected chai api error",e);
                        throw new IllegalStateException(e.getMessage(),e);             
                    }
                }
            }
            attempts++;
        }

        throw new ChaiUnavailableException("unable to reach any configured server, maximum retries exceeded", ChaiError.COMMUNICATION);
    }


// -------------------------- INNER CLASSES --------------------------

    /**
     * The rotatation machine manages which URL is currently active.  It does this by
     * setting up a "Slot" for each of the avilable URLs.  Visually it might look something
     * like this:
     * <pre>
     *     Slot#   ProviderSlot instance       state
     *   +-------+--------------------------+----------+
     *   |   0   | URL #1, config, provider |  closed  |
     *   +-------+--------------------------+----------+
     *   |   1   | URL #2, config, provider |  active  |
     *   +-------+--------------------------+----------+
     *   |   2   | URL #3, config, provider |  null    |
     *   +-------+--------------------------+----------+
     * </pre>
     * The machine keeps track of a which slot is "active".  If the machie is informed that the
     * current provider is broken, the machine will advance the active marker to the next slot
     * and attempt to make it's provider active.  All other slots will have their provider's inactived.
     * <p/>
     * If a slot other than #0 is active for a duration longer than the
     * {@link com.novell.ldapchai.provider.FailOverWrapper.FailOverSettings#getMinFailBackTime()}, the
     * machine will rotate back to slot #0 then next time it is accessed.
     * <p/>
     * For any given unique urlList used in the settings, a global (static) "last known good" cache is maintained
     * with the last known good slot.  In this way, if a new provider is created, it will start with a good
     * slot instead of the dead "0"th slot.  This makes initial connection times much faster in cases
     * where the 0th slot is a consistently dead server.
     * <p/>
     * Despite the last known good cache, every rotation machine maintains an unrelated state.  The cache
     * is only used for setting the initial slot used when a new rotation machine is created.
     */
    private static class RotationMachine implements Serializable {
        private enum FAILSTATE {
            NEW, OKAY, SEEKING, FAILED
        }

        private long lastFailureTime = System.currentTimeMillis();

        private final List<ProviderSlot> proividerSlots = new ArrayList<ProviderSlot>();
        private volatile int activeSlot = 0;
        private final FailOverSettings settings;

        private final int urlListHashCode;

        private static final Map<Integer,Integer> LAST_KNOWN_GOOD_CACHE = new LinkedHashMap<Integer,Integer>();
        private static final int MAX_SIZE_LNG_CACHE = 50;
        private static long lngLastPopulateTime = System.currentTimeMillis();

        private Exception lastConnectionException;

        private volatile FAILSTATE failState = FAILSTATE.NEW;

        RotationMachine(final ChaiConfiguration chaiConfig, final FailOverSettings settings)
                throws ChaiUnavailableException
        {
            urlListHashCode = chaiConfig.bindURLsAsList().hashCode();
            this.settings = settings;
            configureInitialState(chaiConfig);
        }

        private void setActiveSlot(final int activeSlot) {
            this.activeSlot = activeSlot;

            if (activeSlot != 0) {
                LAST_KNOWN_GOOD_CACHE.put(urlListHashCode, activeSlot);
                lngLastPopulateTime = System.currentTimeMillis();

                while (LAST_KNOWN_GOOD_CACHE.size() > MAX_SIZE_LNG_CACHE) {
                    LAST_KNOWN_GOOD_CACHE.keySet().iterator().remove();
                    LOGGER.warn("RotationMachine maximum Last Known Good cache size (" + MAX_SIZE_LNG_CACHE + ") exceeded, reducing cached entries " );
                }
            }
        }

        private void configureInitialState(final ChaiConfiguration chaiConfig)
        {
            try {
                for (final String loopUrl : chaiConfig.bindURLsAsList()) {
                    final ChaiConfiguration loopConfig = (ChaiConfiguration) chaiConfig.clone();
                    loopConfig.setSetting(ChaiSetting.BIND_URLS, loopUrl);
                    proividerSlots.add(new ProviderSlot(loopConfig, loopUrl));
                }
            } catch (CloneNotSupportedException e) {
                final String errorMsg = "unexpected api error";
                LOGGER.warn(errorMsg, e);
                throw new IllegalStateException(errorMsg);
            }

            if (!LAST_KNOWN_GOOD_CACHE.isEmpty()) {
                if ((System.currentTimeMillis() - lngLastPopulateTime) > settings.getMinFailBackTime()) {
                    LAST_KNOWN_GOOD_CACHE.clear();
                }
            }

            if (LAST_KNOWN_GOOD_CACHE.containsKey(urlListHashCode)) {
                activeSlot = LAST_KNOWN_GOOD_CACHE.get(urlListHashCode);
                LOGGER.debug("using slot #" + activeSlot + " (" + proividerSlots.get(activeSlot).getUrl() + ") as initial bind URL due to Last Known Good cache");
            }
        }

        public synchronized ChaiProvider getCurrentProvider()
                throws ChaiUnavailableException
        {
            failbackCheck();

            if (failState == FAILSTATE.NEW) {
                try {
                    makeNewProvider(activeSlot);
                    failState = FAILSTATE.OKAY;
                } catch (ChaiUnavailableException e) {
                    if (settings.errorIsRetryable(e)) {
                        failState = FAILSTATE.FAILED;
                    } else {
                        throw e;
                    }
                }
            }

            if (failState == FAILSTATE.OKAY) {
                return proividerSlots.get(activeSlot).getProvider();
            }

            if (failState == FAILSTATE.FAILED) {
                currentServerIsBroken();

                if (failState == FAILSTATE.OKAY) {
                    return proividerSlots.get(activeSlot).getProvider();
                }
            }

            final StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("unable to connect to any configured ldap url");
            if (lastConnectionException != null) {
                errorMsg.append(", last error: ");
                errorMsg.append(lastConnectionException.getMessage());
            }
            throw new ChaiUnavailableException(errorMsg.toString(), ChaiError.COMMUNICATION);
        }

        public synchronized void reportBrokenProvider(final ChaiProvider provider)
        {
            //no point doing anything if state is already reported as broken.
            if (failState != FAILSTATE.OKAY) {
                return;
            }

            //make sure the reported provider is the one thats actually currently active, otherwise ignore the report
            final ChaiProvider presumedCurrentProvider = proividerSlots.get(activeSlot).getProvider();
            if (presumedCurrentProvider != null && presumedCurrentProvider.equals(provider)) {
                currentServerIsBroken();
            }
        }

        private synchronized void failbackCheck()
        {
            if (failState == FAILSTATE.OKAY && activeSlot != 0) {
                final long msSinceLastFailure = System.currentTimeMillis() - lastFailureTime;
                if (msSinceLastFailure > settings.getMinFailBackTime()) {
                    failState = FAILSTATE.NEW;
                    setActiveSlot(0);
                }
            }
        }

        private synchronized void currentServerIsBroken()
        {
            if (proividerSlots.size() > 1) {
                LOGGER.warn("current server " + proividerSlots.get(activeSlot).getUrl() + " has failed, failing over to next server in list");
            } else {
                LOGGER.warn("unable to reach ldap server " + proividerSlots.get(activeSlot).getUrl());
            }
            lastFailureTime = System.currentTimeMillis();
            boolean success = false;

            try {
                failState = FAILSTATE.SEEKING;

                final int maxRetries = proividerSlots.size();
                int retryCounter = 0;
                while (!success && retryCounter < maxRetries) {

                    if (activeSlot + 1 > proividerSlots.size() - 1) {
                        setActiveSlot(0);
                        pause(settings.getRotateDelay());
                    } else {
                        setActiveSlot(++activeSlot);
                    }


                    if (proividerSlots.size() > 1) {
                        LOGGER.info("failing over to " + proividerSlots.get(activeSlot).getUrl());
                    }

                    try {
                        makeNewProvider(activeSlot);
                        success = true;
                    } catch (ChaiUnavailableException e) {
                        lastConnectionException = e;
                        if (settings.failOverHelper.errorIsRetryable(e)) {
                            LOGGER.debug("error connecting to ldap server, will retry, " + e.getMessage());
                        } else {
                            LOGGER.debug("detected unretryable error while rotating servers: " + e.getMessage());
                            break;
                        }
                    }
                    retryCounter++;
                }
            } finally {
                failState = success ? FAILSTATE.OKAY : FAILSTATE.FAILED;
            }
        }

        private void makeNewProvider(final int forSlot)
                throws ChaiUnavailableException
        {
            destoryAllConnections();

            //create a new connection
            final ProviderSlot slot = proividerSlots.get(forSlot);
            try {
                final ChaiProviderImplementor newProvider = ChaiProviderFactory.createConcreateProvider(slot.getConfig(), true);
                slot.setProvider(newProvider);
            } catch (ChaiUnavailableException e) {
                throw e;
            } catch (Exception e) {
                final String errorMsg = "unexepected error creating new FailOver ChaiProvider: " + e.getMessage();
                LOGGER.error(errorMsg);
                throw new IllegalStateException(errorMsg, e);
            }
        }

        public void destoryAllConnections()
        {
            for (final ProviderSlot loopSlot : proividerSlots) {
                final ChaiProvider loopProvider = loopSlot.getProvider();
                if (loopProvider != null) {
                    loopProvider.close();
                }
            }
        }
    }

    private static class ProviderSlot {
        ChaiConfiguration config;
        String url;
        ChaiProviderImplementor provider;

        private ProviderSlot(final ChaiConfiguration config, final String url)
        {
            this.config = config;
            this.url = url;
        }

        public ChaiConfiguration getConfig()
        {
            return config;
        }

        public String getUrl()
        {
            return url;
        }

        public ChaiProviderImplementor getProvider()
        {
            return provider;
        }

        public void setProvider(final ChaiProviderImplementor provider)
        {
            this.provider = provider;
        }
    }

    private static class FailOverSettings {
        private final ChaiProviderImplementor failOverHelper;

        private int maxRetries = 3;
        private int minFailBackTime = 5 * 60 * 1000; // 5 minutes
        private final int rotateDelay = 1000;

        private FailOverSettings(
                final ChaiProviderImplementor failOverHelper
        )
        {
            this.failOverHelper = failOverHelper;
        }

        boolean errorIsRetryable(final Exception e)
        {
            return failOverHelper.errorIsRetryable(e);
        }

        public int getMaxRetries()
        {
            return maxRetries;
        }

        public int getMinFailBackTime()
        {
            return minFailBackTime;
        }

        public int getRotateDelay()
        {
            return rotateDelay;
        }
    }
}
