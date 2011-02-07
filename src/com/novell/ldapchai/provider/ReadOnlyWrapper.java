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
import com.novell.ldapchai.util.ChaiLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Read only wrapper
 *
 * @author Jason D. Rivard
 * @see com.novell.ldapchai.provider.ChaiSetting#READONLY
 */
class ReadOnlyWrapper implements InvocationHandler {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(ReadOnlyWrapper.class.getName());

    /**
     * The standard wrapper manages updating statistics and handling the wire trace functionality.
     */
    private ChaiProviderImplementor realProvider;

// -------------------------- STATIC METHODS --------------------------

    static ChaiProviderImplementor forProvider(final ChaiProviderImplementor chaiProvider)
    {
        if (Proxy.isProxyClass(chaiProvider.getClass()) && chaiProvider instanceof ReadOnlyWrapper) {
            LOGGER.warn("attempt to obtain ReadOnlyWrapper wrapper for already wrapped Provider.");
            return chaiProvider;
        }

        return (ChaiProviderImplementor) Proxy.newProxyInstance(
                chaiProvider.getClass().getClassLoader(),
                chaiProvider.getClass().getInterfaces(),
                new ReadOnlyWrapper(chaiProvider));
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private ReadOnlyWrapper(final ChaiProviderImplementor realProvider)
    {
        this.realProvider = realProvider;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface InvocationHandler ---------------------

    public Object invoke(final Object proxy, final Method method, final Object[] args)
            throws Throwable
    {
        final boolean isModify = method.getAnnotation(ChaiProviderImplementor.ModifyOperation.class) != null;

        if (isModify) {
            throw new ChaiOperationException("attempt to make ldap modifaction, but Chai is configured for read-only",
                    ChaiError.READ_ONLY_VIOLATION,
                    true,
                    false);
        }

        try {
            return method.invoke(realProvider, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}