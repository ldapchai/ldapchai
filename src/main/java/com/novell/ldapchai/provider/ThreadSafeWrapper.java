/*
 * LDAP Chai API
 * Copyright (c) 2006-2017 Novell, Inc.
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

import com.novell.ldapchai.util.internal.ChaiLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.locks.ReentrantLock;

class ThreadSafeWrapper extends AbstractWrapper
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( ThreadSafeWrapper.class );

    private final ReentrantLock lock = new ReentrantLock();

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
        final boolean threadSafeEnabled = Boolean.parseBoolean( chaiProvider.getChaiConfiguration().getSetting( ChaiSetting.THREAD_SAFE_ENABLE ) );
        if ( !threadSafeEnabled )
        {
            final String errorStr = "attempt to obtain ThreadSafeWrapper wrapper when thread safe is not enabled in chai config";
            LOGGER.warn( () -> errorStr );
            throw new IllegalStateException( errorStr );
        }

        if ( Proxy.isProxyClass( chaiProvider.getClass() ) && chaiProvider instanceof ThreadSafeWrapper )
        {
            LOGGER.warn( () -> "attempt to obtain ThreadSafeWrapper wrapper for already wrapped Provider." );
            return chaiProvider;
        }

        return ( ChaiProviderImplementor ) Proxy.newProxyInstance(
                chaiProvider.getClass().getClassLoader(),
                chaiProvider.getClass().getInterfaces(),
                new ThreadSafeWrapper( chaiProvider ) );
    }

    ThreadSafeWrapper(
            final ChaiProviderImplementor realProvider
    )
    {
        this.realProvider = realProvider;
    }

    @Override
    public Object invoke(
            final Object proxy,
            final Method method,
            final Object[] args )
            throws Throwable
    {
        final boolean isLdap = method.getAnnotation( ChaiProviderImplementor.LdapOperation.class ) != null;

        try
        {
            if ( isLdap )
            {
                lock.lock();
                try
                {
                    return method.invoke( realProvider, args );
                }
                finally
                {
                    lock.unlock();
                }
            }
            else
            {
                return method.invoke( realProvider, args );
            }
        }
        catch ( InvocationTargetException e )
        {
            throw e.getCause();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "unexpected invocation exception: " + e.getMessage(), e );
        }
    }
}
