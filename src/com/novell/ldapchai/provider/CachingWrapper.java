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

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Implenents a 'naive' caching wrapper around a provider.
 *
 * @author Jason D. Rivard
 */
class CachingWrapper extends AbstractWrapper {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(CachingWrapper.class.getName());

    private ChaiProvider realProvider;
    private ChaiProvider memorizedProvider;

// -------------------------- STATIC METHODS --------------------------

    static ChaiProviderImplementor forProvider(final ChaiProviderImplementor chaiProvider)
    {
        if (Proxy.isProxyClass(chaiProvider.getClass()) && chaiProvider instanceof CachingWrapper) {
            LOGGER.warn("Attempt to obtain CachingWrapper wrapper for already wrapped Provider.");
            return chaiProvider;
        }

        return (ChaiProviderImplementor) Proxy.newProxyInstance(
                chaiProvider.getClass().getClassLoader(),
                chaiProvider.getClass().getInterfaces(),
                new CachingWrapper(chaiProvider));
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private CachingWrapper(final ChaiProvider providerImpl)
    {
        if (Proxy.isProxyClass(providerImpl.getClass()) && providerImpl instanceof CachingWrapper) {
            LOGGER.warn("attempt to obtain CachingWrapper wrapper for already wrapped provider");
            throw new IllegalStateException("chaiProvider is already wrapped for caching");
        }

        final int setting_maxSize = Integer.parseInt(providerImpl.getChaiConfiguration().getSetting(ChaiSetting.CACHE_MAXIMUM_SIZE));
        final int setting_maxTime = Integer.parseInt(providerImpl.getChaiConfiguration().getSetting(ChaiSetting.CACHE_MAXIMUM_AGE));

        this.realProvider = providerImpl;
        this.memorizedProvider = (ChaiProviderImplementor) Memorizer.forObject(realProvider, setting_maxSize, setting_maxTime);
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface InvocationHandler ---------------------

    public Object invoke(
            final Object proxy,
            final Method m,
            final Object[] args)
            throws Throwable
    {
        if (m.getAnnotation(ChaiProviderImplementor.ModifyOperation.class) != null) {
            clearCache();
        }

        try {
            if (m.getAnnotation(ChaiProviderImplementor.LdapOperation.class) != null) {
                return m.invoke(memorizedProvider, args);
            } else {
                return m.invoke(realProvider, args);
            }
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public void clearCache()
    {
        ((Memorizer) memorizedProvider).clearCache();
    }

// -------------------------- INNER CLASSES --------------------------

    private static class Memorizer implements InvocationHandler {
        private static final ChaiLogger LOGGER = ChaiLogger.getLogger(Memorizer.class);

        private Object memorizedObject;

        private Map<Method, Map<List<Object>, ValueWrapper>> hardCache = new HashMap<Method, Map<List<Object>, ValueWrapper>>();
        private Map<Method, Map<List<Object>, WeakReference<ValueWrapper>>> weakCache = new HashMap<Method, Map<List<Object>, WeakReference<ValueWrapper>>>();
        private LinkedList<ValueWrapper> valueStack = new LinkedList<ValueWrapper>();

        private final Object lock = new Object();

        // timeout values stored as primitives for performance.
        private int maxSize = Integer.parseInt(ChaiSetting.CACHE_MAXIMUM_SIZE.getDefaultValue());
        private int maxAge = Integer.parseInt(ChaiSetting.CACHE_MAXIMUM_AGE.getDefaultValue());

        public static Object forObject(final Object memorizedObject, final int maxAge, final int maxSize)
        {
            if (memorizedObject instanceof Memorizer) {
                LOGGER.warn("Attempt to re-memorized already wrapped memorized object");
                return memorizedObject;
            }

            final Object p = Proxy.newProxyInstance(
                    memorizedObject.getClass().getClassLoader(),
                    memorizedObject.getClass().getInterfaces(),
                    new Memorizer(memorizedObject));

            ((Memorizer) p).maxAge = maxAge >= 0 ? maxAge : 0;
            ((Memorizer) p).maxSize = maxSize >= 0 ? maxSize : 0;

            return p;
        }

        private Memorizer(final Object memorizedObject)
        {
            this.memorizedObject = memorizedObject;
            clearCache();
        }

        public void clearCache()
        {
            hardCache.clear();
        }

        public Object invoke(final Object object, final Method method, final Object[] args)
                throws Throwable
        {
            if (method.getReturnType().equals(Void.TYPE)) {
                // Don't cache void methods
                return invoke(memorizedObject, method, args);
            } else {
                final List<Object> key = Arrays.asList(args);

                Object value = this.getCachedValue(method, key);

                // value is not in cache, so invoke method normaly
                if (value == null) {
                    try {
                        value = invoke(memorizedObject, method, args);
                        addCachedValue(new ValueWrapper(method, key, value));
                    } catch (Exception e) {
                        throw e.getCause();
                    }
                }

                return value;
            }
        }

        private Map<List<Object>, ValueWrapper> getHardCachedInvocations(final Method method)
        {
            Map<List<Object>, ValueWrapper> cache = this.hardCache.get(method);
            if (cache == null) {
                cache = new HashMap<List<Object>, ValueWrapper>();
                this.hardCache.put(method, cache);
            }
            return cache;
        }

        private Map<List<Object>, WeakReference<ValueWrapper>> getWeakCachedInvocations(final Method method)
        {
            Map<List<Object>, WeakReference<ValueWrapper>> cache = this.weakCache.get(method);
            if (cache == null) {
                cache = new WeakHashMap<List<Object>, WeakReference<ValueWrapper>>();
                this.weakCache.put(method, cache);
            }
            return cache;
        }

        private Object getCachedValue(final Method method, final List<Object> key)
        {
            // retreive the value from cache
            ValueWrapper vw = getHardCachedInvocations(method).get(key);

            // if got nothing, check the weak map
            if (vw == null) {
                final WeakReference<ValueWrapper> wr = this.getWeakCachedInvocations(method).get(key);
                if (wr != null) {
                    vw = wr.get();
                }
            }

            // if still got nothing then to bad.
            if (vw == null) {
                return null;
            }

            // if we did get something, then check if its to old
            final long age = System.currentTimeMillis() - vw.getTimestamp();
            if (age > maxAge) {
                removeCachedValue(vw, false);
                return null;
            }

            return vw.getValue();
        }

        private void removeCachedValue(final ValueWrapper vw, final boolean addToWeakCache)
        {
            synchronized (lock) {
                final Map<List<Object>, ValueWrapper> cachedInvocations = this.getHardCachedInvocations(vw.getMethod());
                cachedInvocations.remove(vw.getKey());
                valueStack.remove(vw);

                if (addToWeakCache) {
                    getWeakCachedInvocations(vw.getMethod()).put(vw.getKey(), new WeakReference<ValueWrapper>(vw));
                }
            }
        }

        private void addCachedValue(final ValueWrapper vw)
        {
            synchronized (lock) {
                final Map<List<Object>, ValueWrapper> cachedInvocations = this.getHardCachedInvocations(vw.getMethod());
                cachedInvocations.put(vw.getKey(), vw);
                valueStack.remove(vw);
                valueStack.addFirst(vw);
            }

            while (valueStack.size() > maxSize) {
                final ValueWrapper loopVw = valueStack.getLast();
                this.removeCachedValue(loopVw, true);
            }
        }

        class ValueWrapper {
            private long timestamp;

            private Method method;
            private List<Object> key;
            private Object value;

            public ValueWrapper(final Method method, final List<Object> key, final Object value)
            {
                this.timestamp = System.currentTimeMillis();

                this.method = method;
                this.key = key;
                this.value = value;
            }

            public Method getMethod()
            {
                return method;
            }

            public long getTimestamp()
            {
                return timestamp;
            }

            public List<Object> getKey()
            {
                return key;
            }

            public Object getValue()
            {
                return value;
            }
        }
    }
}
