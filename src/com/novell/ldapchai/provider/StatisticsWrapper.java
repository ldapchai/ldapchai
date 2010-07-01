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

import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.ChaiLogger;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link com.novell.ldapchai.provider.ProviderStatistics}.
 *
 * @author Jason D. Rivard
 * @see ChaiSetting#STATISTICS_ENABLE
 */
class StatisticsWrapper implements InvocationHandler {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger(StatisticsWrapper.class.getName());

    private static final StatsBean GLOBAL_STATS = new StatsBean();


    /**
     * The standard wrapper manages updating statistics and handling the wire trace functionality.
     */
    private ChaiProviderImplementor realProvider;
    private StatsBean statisticsProvider;

// -------------------------- STATIC METHODS --------------------------

    static ChaiProviderImplementor forProvider(final ChaiProviderImplementor chaiProvider)
    {
        if (Proxy.isProxyClass(chaiProvider.getClass()) && chaiProvider instanceof StatisticsWrapper) {
            LOGGER.warn("attempt to obtain StatisticsWrapper wrapper for already wrapped Provider.");
            return chaiProvider;
        }

        return (ChaiProviderImplementor) Proxy.newProxyInstance(
                chaiProvider.getClass().getClassLoader(),
                chaiProvider.getClass().getInterfaces(),
                new StatisticsWrapper(chaiProvider));
    }

    public static ProviderStatistics getGlobalStatistics()
    {
        return GLOBAL_STATS;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private StatisticsWrapper(final ChaiProviderImplementor realProvider)
    {
        this.realProvider = realProvider;
        statisticsProvider = new StatsBean();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface InvocationHandler ---------------------

    public Object invoke(final Object proxy, final Method method, final Object[] args)
            throws Throwable
    {
        final boolean isLdap = method.getAnnotation(ChaiProviderImplementor.LdapOperation.class) != null;
        final boolean isModify = method.getAnnotation(ChaiProviderImplementor.ModifyOperation.class) != null;
        final boolean isSearch = method.getAnnotation(ChaiProviderImplementor.SearchOperation.class) != null;

        if (method.getName().equals("getProviderStatistics")) {
            return statisticsProvider;
        }

        if (isLdap) {
            statisticsProvider.operationCounter++;
            GLOBAL_STATS.operationCounter++;

            if (isModify) {
                statisticsProvider.modifyCount++;
                GLOBAL_STATS.modifyCount++;
            } else if (isSearch) {
                statisticsProvider.searchCount++;
                GLOBAL_STATS.searchCount++;
            } else {
                statisticsProvider.readCount++;
                GLOBAL_STATS.readCount++;
            }

            statisticsProvider.lastOperationBegin = System.currentTimeMillis();
            GLOBAL_STATS.lastOperationBegin = System.currentTimeMillis();
        }

        try {
            return method.invoke(realProvider, args);
        } catch (InvocationTargetException e) {
            final Throwable exceptionCause = e.getCause();

            if (exceptionCause instanceof ChaiUnavailableException) {
                statisticsProvider.lastUnavailableException = System.currentTimeMillis();
                GLOBAL_STATS.lastUnavailableException = System.currentTimeMillis();

                statisticsProvider.unavailableCounter++;
                GLOBAL_STATS.unavailableCounter++;
            }

            throw exceptionCause;
        } finally {
            statisticsProvider.lastOperationFinish = System.currentTimeMillis();
            GLOBAL_STATS.lastOperationFinish = System.currentTimeMillis();
        }
    }

// -------------------------- INNER CLASSES --------------------------

    static class StatsBean implements ProviderStatistics, Serializable {
        private long readCount;
        private long modifyCount;
        private long searchCount;
        private long operationCounter;
        private long unavailableCounter;
        private long lastOperationBegin;
        private long lastOperationFinish;
        private long lastUnavailableException;


        public String getStatistic(final Statistic statistic)
        {
            switch (statistic) {
                case LAST_OPERATION_BEGIN:
                    return String.valueOf(lastOperationBegin);
                case LAST_OPERATION_FINISH:
                    return String.valueOf(lastOperationFinish);
                case READ_COUNT:
                    return String.valueOf(readCount);
                case MODIFY_COUNT:
                    return String.valueOf(modifyCount);
                case SEARCH_COUNT:
                    return String.valueOf(searchCount);
                case OPERATION_COUNT:
                    return String.valueOf(operationCounter);
                case LAST_UNAVAILABLE_EXCEPTION:
                    return String.valueOf(lastUnavailableException);
                case UNAVAILABLE_COUNT:
                    return String.valueOf(unavailableCounter);
                default:
                    return "";
            }
        }

        public Map<Statistic, String> getStatistics()
        {
            final Map<Statistic, String> returnMap = new HashMap<Statistic, String>();
            for (final Statistic statistic : Statistic.values()) {
                returnMap.put(statistic, getStatistic(statistic));
            }
            return Collections.unmodifiableMap(returnMap);
        }
    }
}
