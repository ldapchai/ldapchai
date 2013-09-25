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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.novell.ldapchai.util.ChaiLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Simple wire trace provider wrapper.  Adds lots of debugging info to the log4j trace level.
 *
 * @author Jason D. Rivard
 * @see ChaiSetting#WIRETRACE_ENABLE
 */
class WireTraceWrapper extends AbstractWrapper {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(WireTraceWrapper.class);

    private volatile long operationCounter;

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
        final boolean watchDogEnabled = Boolean.parseBoolean(chaiProvider.getChaiConfiguration().getSetting(ChaiSetting.WIRETRACE_ENABLE));
        if (!watchDogEnabled) {
            final String errorStr = "attempt to obtain WireTrace wrapper when watchdog is not enabled in chai config";
            LOGGER.warn(errorStr);
            throw new IllegalStateException(errorStr);
        }

        if (Proxy.isProxyClass(chaiProvider.getClass()) && chaiProvider instanceof WireTraceWrapper) {
            LOGGER.warn("attempt to obtain WireTraceWrapper wrapper for already wrapped Provider.");
            return chaiProvider;
        }

        return (ChaiProviderImplementor) Proxy.newProxyInstance(
                chaiProvider.getClass().getClassLoader(),
                chaiProvider.getClass().getInterfaces(),
                new WireTraceWrapper(chaiProvider));
    }

// --------------------------- CONSTRUCTORS ---------------------------

    WireTraceWrapper(
            final ChaiProviderImplementor realProvider
    )
    {
        this.realProvider = realProvider;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface InvocationHandler ---------------------

    public Object invoke(
            final Object proxy,
            final Method method,
            final Object[] args)
            throws Throwable
    {
        final boolean isLdap = method.getAnnotation(ChaiProviderImplementor.LdapOperation.class) != null;

        try {
            if (isLdap) {
                return traceInvokation(method, args);
            } else {
                return method.invoke(realProvider, args);
            }
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            throw new RuntimeException("unexpected invocation exception: " + e.getMessage(),e);
        }
    }

// -------------------------- OTHER METHODS --------------------------

    private Object traceInvokation(
            final Method method,
            final Object[] args)
            throws Throwable
    {
        final long opNumber = getNextCounter();
        final String messageLabel = "id=" + realProvider.getIdentifier() + ",op#" + opNumber;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("begin " + messageLabel + " method " + AbstractProvider.methodToDebugStr(method, args));
        }

        final long startTime = System.currentTimeMillis();
        final Object result = method.invoke(realProvider, args);
        final long totalTime = System.currentTimeMillis() - startTime;

        String debugResult = null;
        if (result != null) {
            try {
                debugResult = (new GsonBuilder().disableHtmlEscaping().create()).toJson(result);
            } catch (Exception e) {
                debugResult = toString();
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("finish " + messageLabel + " result: " + (debugResult== null ? "null" : debugResult) + " (" + totalTime + "ms)");
        }

        return result;
    }

    private synchronized long getNextCounter()
    {
        operationCounter++;
        return operationCounter;
    }
}