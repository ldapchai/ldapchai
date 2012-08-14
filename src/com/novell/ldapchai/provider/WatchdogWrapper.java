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

import com.novell.ldapchai.ChaiFactory;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiException;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.ChaiLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * A {@link ChaiProvider} implementation wrapper that handles automatic idle disconnects.
 *
 * @author Jason D. Rivard
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_ENABLE
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_IDLE_TIMEOUT
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_OPERATION_TIMEOUT
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_CHECK_FREQUENCY
 */
class WatchdogWrapper implements InvocationHandler {
// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    private static enum STATUS {
        ACTIVE, IDLE, CLOSED
    }

// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(WatchdogWrapper.class);

    private static final WatchdogManager watchdogManager = new WatchdogManager();

    private static long setting_watchdogFrequency = Integer.parseInt(ChaiSetting.WATCHDOG_CHECK_FREQUENCY.getDefaultValue());

    // timeout values stored as primitives for performance.
    private int setting_operationTimeout = Integer.parseInt(ChaiSetting.WATCHDOG_OPERATION_TIMEOUT.getDefaultValue());
    private int setting_idleTimeout = Integer.parseInt(ChaiSetting.WATCHDOG_IDLE_TIMEOUT.getDefaultValue());

    // the real provider and it's associated configuration
    private volatile ChaiProvider realProvider;
    private final ChaiProvider originalProvider;
    private final ChaiConfiguration originalProviderConfig;


    /**
     * number of outsanding ldap operations.  If the value is non-zero, then the provider is considered "in-use"
     */
    private volatile int outstandingOperations = 0;

    /**
     * last time an ldap operation was initiated
     */
    private volatile long lastBeginTimestamp = System.currentTimeMillis();

    /**
     * last time an ldap operation was completed
     */
    private volatile long lastFinishTimestamp = System.currentTimeMillis();

    /**
     * current wdStatus of this WatchdogWrapper
     */
    private volatile STATUS wdStatus = STATUS.ACTIVE;

// -------------------------- STATIC METHODS --------------------------

    /**
     * Wrap a pre-existing ChaiProvider with a WatchdogWrapper instance.
     *
     * @param chaiProvider a pre-existing {@code ChaiProvider}
     * @return a wrapped {@code ChaiProvider} instance.
     */
    static ChaiProviderImplementor forProvider(
            final ChaiProviderImplementor chaiProvider
    )
    {
        //check to make sure watchdog ise enabled;
        final boolean watchDogEnabled = Boolean.parseBoolean(chaiProvider.getChaiConfiguration().getSetting(ChaiSetting.WATCHDOG_ENABLE));
        if (!watchDogEnabled) {
            final String errorStr = "attempt to obtain WatchdogWrapper wrapper when watchdog is not enabled in chai config";
            LOGGER.warn(errorStr);
            throw new IllegalStateException(errorStr);
        }

        if (Proxy.isProxyClass(chaiProvider.getClass()) && chaiProvider instanceof WatchdogWrapper) {
            LOGGER.warn("attempt to obtain WatchdogWrapper wrapper for already wrapped Provider.");
            return chaiProvider;
        }

        return (ChaiProviderImplementor) Proxy.newProxyInstance(
                chaiProvider.getClass().getClassLoader(),
                chaiProvider.getClass().getInterfaces(),
                new WatchdogWrapper(chaiProvider));
    }

    // --------------------------- CONSTRUCTORS ---------------------------

    private WatchdogWrapper(
            final ChaiProviderImplementor realProvider
    )
    {
        this.originalProvider = realProvider;
        this.realProvider = realProvider;
        this.originalProviderConfig = realProvider.getChaiConfiguration();

        setting_watchdogFrequency = Integer.parseInt(originalProviderConfig.getSetting(ChaiSetting.WATCHDOG_CHECK_FREQUENCY));

        setting_operationTimeout = Integer.parseInt(originalProviderConfig.getSetting(ChaiSetting.WATCHDOG_OPERATION_TIMEOUT));
        setting_idleTimeout = Integer.parseInt(originalProviderConfig.getSetting(ChaiSetting.WATCHDOG_IDLE_TIMEOUT));

        watchdogManager.registerInstance(this);

        checkForPwExpiration();
    }

    private void checkForPwExpiration()

    {
        final boolean doPwExpCheck = realProvider.getChaiConfiguration().getBooleanSetting(ChaiSetting.WATCHDOG_DISABLE_IF_PW_EXPIRED);
        if (!doPwExpCheck) {
            return;
        }

        LOGGER.trace("checking for user password expiration to adjust watchdog timeout");

        final boolean userPwExpired;
        try {
            final String bindUserDN = realProvider.getChaiConfiguration().getSetting(ChaiSetting.BIND_DN);
            final ChaiUser bindUser = ChaiFactory.createChaiUser(bindUserDN, realProvider);
            userPwExpired = bindUser.isPasswordExpired();
        } catch (ChaiException e) {
            LOGGER.error("unexpected error attempting to read user password expiration value during watchdog initialization: " + e.getMessage(),e);
            return;
        }

        if (userPwExpired) {
            LOGGER.info("connection user account password is currently expired.  Disabling watchdog timeout.");
            setting_idleTimeout = Integer.MAX_VALUE;
        } else {
            setting_idleTimeout = realProvider.getChaiConfiguration().getIntSetting(ChaiSetting.WATCHDOG_IDLE_TIMEOUT);
        }
    }

// ------------------------ CANONICAL METHODS ------------------------

    protected void finalize()
            throws Throwable
    {
        super.finalize();
        watchdogManager.deRegisterInstance(this);  //safegaurd, this should be done in #handleClient
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface InvocationHandler ---------------------

    public Object invoke(
            final Object proxy,
            final Method method,
            final Object[] args)
            throws Throwable
    {
        // before performing any operation, check to see what the current watchdog wdStatus is.
        if (method.getName().equals("close")) {
            handleClientCloseRequest();
            return Void.TYPE;
        }

        if (method.getName().equals("isConnected")) {
            if (wdStatus != STATUS.ACTIVE) {
                return false;
            }
        }


        final boolean isLdap = method.getAnnotation(ChaiProviderImplementor.LdapOperation.class) != null;

        if (wdStatus == STATUS.CLOSED || !isLdap) {
            return AbstractWrapper.invoker(originalProvider, method, args);
        }

        final Object returnObject;
        try {
            outstandingOperations++;
            lastBeginTimestamp = System.currentTimeMillis();

            if (wdStatus == STATUS.IDLE) {
                reopenRealProvider();
                lastBeginTimestamp = System.currentTimeMillis();
            }

            returnObject = AbstractWrapper.invoker(realProvider, method, args);
        } catch (Exception e) {
            if (e instanceof ChaiOperationException) {
                throw (ChaiOperationException) e;
            } else if (e instanceof ChaiUnavailableException) {
                throw (ChaiUnavailableException)e;
            } else {
                LOGGER.warn("unexpected chai api error",e);
                throw new IllegalStateException(e.getMessage(),e);
            }
        } finally {
            outstandingOperations--;
            lastFinishTimestamp = System.currentTimeMillis();
        }


        if ("setPassword".equals(method.getName()) || "setPassword".equals(method.getName())) {
            checkForPwExpiration();
        }

        return returnObject;
    }

// -------------------------- OTHER METHODS --------------------------

    private synchronized void checkStatus()
    {
        if (wdStatus == STATUS.ACTIVE) {  // if current wdStatus us normal, then check to see if timed out
            if (outstandingOperations > 0) { // check for operation timeout if we have outstanding
                final long operationDuration = System.currentTimeMillis() - lastBeginTimestamp;
                if (operationDuration > setting_operationTimeout) {
                    handleOperationTimeout();
                }
            } else {
                final long idleDuration = System.currentTimeMillis() - lastFinishTimestamp;
                if (idleDuration > setting_idleTimeout) {
                    handleIdleTimeout();
                }
            }
        }
    }

    private synchronized void handleOperationTimeout()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ldap operation timeout detected, discarding questionable connection");
        if (realProvider != null) {
            sb.append(" for ");
            sb.append(realProvider.toString());
        }
        LOGGER.warn(sb.toString());
        synchronized (this) {
            if (realProvider != null) {
                this.realProvider.close();
            }
            wdStatus = STATUS.IDLE;
            watchdogManager.deRegisterInstance(this);
        }
    }

    private synchronized void handleIdleTimeout()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ldap idle timeout detected, closing ldap connection");
        if (realProvider != null) {
            sb.append(" for ");
            sb.append(realProvider.toString());
        }

        LOGGER.debug(sb.toString());
        synchronized (this) {
            if (realProvider != null) {
                this.realProvider.close();
            }
            wdStatus = STATUS.IDLE;
            watchdogManager.deRegisterInstance(this);
        }
    }

    private synchronized void handleClientCloseRequest()
    {
        wdStatus = STATUS.CLOSED;
        if (realProvider != null) {
            realProvider.close();
        }
        watchdogManager.deRegisterInstance(this);
    }

    private synchronized void reopenRealProvider()
            throws Exception
    {
        if (wdStatus != STATUS.IDLE) {
            return;
        }

        {
            final StringBuilder sb = new StringBuilder();
            sb.append("reopening ldap connection for ");
            sb.append(originalProviderConfig.getSetting(ChaiSetting.BIND_DN));
            LOGGER.debug(sb.toString());
        }

        // if old provider exists, try to close it first.
        if (realProvider != null) {
            try {
                realProvider.close();
            } catch (Exception e) {
                final StringBuilder sb = new StringBuilder();
                sb.append("error during pre-close connection for ");
                sb.append(originalProviderConfig.getSetting(ChaiSetting.BIND_DN));
                LOGGER.debug(sb.toString());
            } finally {
                realProvider = null;
            }
        }

        try {
            realProvider = ChaiProviderFactory.createProvider(originalProviderConfig);
        } catch (Exception e) {
            final StringBuilder sb = new StringBuilder();
            sb.append("error reopening ldap connection for ");
            sb.append(originalProviderConfig.getSetting(ChaiSetting.BIND_DN));
            sb.append(" ");
            sb.append(e.toString());
            LOGGER.debug(sb.toString());
            throw e;
        }

        lastBeginTimestamp = System.currentTimeMillis();
        wdStatus = STATUS.ACTIVE;
        watchdogManager.registerInstance(this);
    }

// -------------------------- INNER CLASSES --------------------------

    private static class WatchdogManager {
        private final static String THREAD_NAME = "LDAP Chai WatchdogWrapper timer thread";

        private final Collection<WatchdogWrapper> activeWrappers = Collections.synchronizedSet(new HashSet<WatchdogWrapper>());

        /**
         * timer instance used to watch all the outstanding providers
         */
        private volatile Timer watchDogTimer = null;

        private synchronized void registerInstance(final WatchdogWrapper wdWrapper)
        {
            activeWrappers.add(wdWrapper);
            checkTimer();
        }

        private synchronized void deRegisterInstance(final WatchdogWrapper wdWrapper)
        {
            activeWrappers.remove(wdWrapper);
            checkTimer();
        }

        /**
         * Regulate the timer.  This is important because the timer task creates its own thread,
         * and if the task isn't cleaned up, there could be a thread leak.
         */
        public synchronized void checkTimer()
        {
            if (watchDogTimer == null) {  // if there is NOT an active timer
                if (!activeWrappers.isEmpty()) { // if there are active providers.
                    LOGGER.debug("starting up " + THREAD_NAME + ", " + setting_watchdogFrequency + "ms check frequency");
                    watchDogTimer = new Timer(THREAD_NAME, true);  // create a new timer
                    watchDogTimer.schedule(new WatchdogTask(), setting_watchdogFrequency, setting_watchdogFrequency);
                }
            } else { // if there IS an active timer
                if (activeWrappers.isEmpty()) { // if there are no active providers
                    LOGGER.debug("exiting " + THREAD_NAME + ", no connections requiring monitoring are in use");
                    watchDogTimer.cancel(); // kill the timer.
                    watchDogTimer = null;
                }
            }
        }

        private static void checkProvider(final WatchdogWrapper wdWrapper)
        {
            try {
                if (wdWrapper != null) {
                    wdWrapper.checkStatus();
                }
            } catch (Exception e) {
                final StringBuilder sb = new StringBuilder();
                sb.append("error during watchdog provider idle check: ");
                sb.append(e.getMessage());
                if (wdWrapper.realProvider != null) {
                    sb.append(" for ");
                    sb.append(wdWrapper.toString());
                }
                LOGGER.warn(sb);
            }
        }

        private class WatchdogTask extends TimerTask implements Runnable {
            public void run()
            {
                final Collection<WatchdogWrapper> c = new HashSet<WatchdogWrapper>(activeWrappers);
                for (final WatchdogWrapper wdWrapper : c) {
                    try {
                        checkProvider(wdWrapper);
                    } catch (Exception e) {
                        try {
                            LOGGER.error("error during watchdog timer check: " + e.getMessage(),e);
                        } catch (Exception e2) {
                        }
                    }
                }
            }
        }
    }

    public boolean isIdle() {
        return STATUS.IDLE.equals(wdStatus);
    }
}