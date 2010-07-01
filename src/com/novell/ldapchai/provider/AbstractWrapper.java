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

import com.novell.ldapchai.util.ChaiLogger;

import java.lang.reflect.*;

abstract class AbstractWrapper implements InvocationHandler {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(AbstractWrapper.class);
    protected ChaiProviderImplementor realProvider;

// -------------------------- STATIC METHODS --------------------------

    /**
     * Wrap a pre-existing ChaiProvider with a WatchdogWrapper instance.
     *
     * @param chaiProvider a pre-existing {@code ChaiProvider}
     * @return a wrapped {@code ChaiProvider} instance.
     */
    protected static ChaiProviderImplementor factoryImpl(
            final Class wrapperClass,
            final ChaiSetting enableSetting,
            final ChaiProviderImplementor chaiProvider
    )
            throws Exception
    {
        //check to make sure watchdog ise enabled;
        final boolean isEnabled = Boolean.parseBoolean(chaiProvider.getChaiConfiguration().getSetting(enableSetting));
        if (!isEnabled) {
            final String errorStr = "attempt to obtain " + wrapperClass.getName() + " wrapper when not enabled in chaiConfiguration";
            LOGGER.warn(errorStr);
            throw new IllegalStateException(errorStr);
        }

        if (Proxy.isProxyClass(chaiProvider.getClass()) && wrapperClass.isInstance(chaiProvider)) {
            LOGGER.warn("attempt to obtain " + wrapperClass.getName() + " wrapper for already wrapped ChaiProvider.");
            return chaiProvider;
        }

        try {
            final Constructor constructor = wrapperClass.getConstructor(ChaiProviderImplementor.class.getClass());
            final InvocationHandler wrapper = (InvocationHandler) constructor.newInstance(chaiProvider);
            final Object wrappedProvider = Proxy.newProxyInstance(chaiProvider.getClass().getClassLoader(), chaiProvider.getClass().getInterfaces(), wrapper);
            return (ChaiProviderImplementor) wrappedProvider;
        } catch (Exception e) {
            LOGGER.fatal("Chai internal error, no appropriate constructor for " + wrapperClass.getCanonicalName(), e);
        }

        throw new Exception("Chai internal error, unable to create wrapper for " + wrapperClass.getCanonicalName());
    }

    static Object invoker(final ChaiProvider provider, final Method m, final Object[] args)
            throws Exception
    {
        try {
            // try to invoke the method.
            return m.invoke(provider, args);
        } catch (IllegalAccessException e) {
            final String errorMsg = "unexpected api error";
            LOGGER.warn(errorMsg, e);
            throw new IllegalStateException(errorMsg);
        } catch (InvocationTargetException e) {
            final Throwable realThrowable = e.getCause();
            if (realThrowable instanceof Exception) {
                throw (Exception) realThrowable;
            } else {
                final String errorMsg = "unexpected api error";
                LOGGER.warn(errorMsg, e);
                throw new IllegalStateException(errorMsg);
            }
        }
    }
}
