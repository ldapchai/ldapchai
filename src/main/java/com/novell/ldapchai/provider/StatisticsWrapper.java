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

import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.ChaiLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link com.novell.ldapchai.provider.ProviderStatistics}.
 *
 * @author Jason D. Rivard
 * @see ChaiSetting#STATISTICS_ENABLE
 */
class StatisticsWrapper implements InvocationHandler
{

    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( StatisticsWrapper.class.getName() );

    /**
     * The standard wrapper manages updating statistics and handling the wire trace functionality.
     */
    private ChaiProviderImplementor realProvider;
    private final StatsBean statisticsProvider = new StatsBean();

    static ChaiProviderImplementor forProvider( final ChaiProviderImplementor chaiProvider )
    {
        if ( Proxy.isProxyClass( chaiProvider.getClass() ) && chaiProvider instanceof StatisticsWrapper )
        {
            LOGGER.warn( "attempt to obtain StatisticsWrapper wrapper for already wrapped Provider." );
            return chaiProvider;
        }

        return ( ChaiProviderImplementor ) Proxy.newProxyInstance(
                chaiProvider.getClass().getClassLoader(),
                chaiProvider.getClass().getInterfaces(),
                new StatisticsWrapper( chaiProvider ) );
    }

    public ProviderStatistics getGlobalStatistics()
    {
        return getGlobalStatsBean();
    }

    private StatsBean getGlobalStatsBean()
    {
        return realProvider.getProviderFactory().getCentralService().getStatsBean();
    }

    private StatisticsWrapper( final ChaiProviderImplementor realProvider )
    {
        this.realProvider = realProvider;
    }


    public Object invoke( final Object proxy, final Method method, final Object[] args )
            throws Throwable
    {
        final boolean isLdap = method.getAnnotation( ChaiProviderImplementor.LdapOperation.class ) != null;
        final boolean isModify = method.getAnnotation( ChaiProviderImplementor.ModifyOperation.class ) != null;
        final boolean isSearch = method.getAnnotation( ChaiProviderImplementor.SearchOperation.class ) != null;

        if ( method.getName().equals( "getProviderStatistics" ) )
        {
            return statisticsProvider;
        }

        if ( isLdap )
        {
            statisticsProvider.incrementStatistic( ProviderStatistics.IncrementerStatistic.OPERATION_COUNT );
            getGlobalStatsBean().incrementStatistic( ProviderStatistics.IncrementerStatistic.OPERATION_COUNT );

            if ( isModify )
            {
                statisticsProvider.incrementStatistic( ProviderStatistics.IncrementerStatistic.MODIFY_COUNT );
                getGlobalStatsBean().incrementStatistic( ProviderStatistics.IncrementerStatistic.MODIFY_COUNT );
            }
            else if ( isSearch )
            {
                statisticsProvider.incrementStatistic( ProviderStatistics.IncrementerStatistic.SEARCH_COUNT );
                getGlobalStatsBean().incrementStatistic( ProviderStatistics.IncrementerStatistic.SEARCH_COUNT );
            }
            else
            {
                statisticsProvider.incrementStatistic( ProviderStatistics.IncrementerStatistic.READ_COUNT );
                getGlobalStatsBean().incrementStatistic( ProviderStatistics.IncrementerStatistic.READ_COUNT );
            }

            statisticsProvider.markTimestampStatistic( ProviderStatistics.TimestampStatistic.LAST_OPERATION_BEGIN );
            getGlobalStatsBean().markTimestampStatistic( ProviderStatistics.TimestampStatistic.LAST_OPERATION_BEGIN );
        }

        try
        {
            final Object returnObj = method.invoke( realProvider, args );

            if ( method.getName().equals( "init" ) )
            {
                statisticsProvider.incrementStatistic( ProviderStatistics.IncrementerStatistic.BIND_COUNT );
            }

            return returnObj;
        }
        catch ( InvocationTargetException e )
        {
            final Throwable exceptionCause = e.getCause();

            if ( exceptionCause instanceof ChaiUnavailableException )
            {
                statisticsProvider.markTimestampStatistic( ProviderStatistics.TimestampStatistic.LAST_UNAVAILABLE_EXCEPTION );
                getGlobalStatsBean().markTimestampStatistic( ProviderStatistics.TimestampStatistic.LAST_UNAVAILABLE_EXCEPTION );

                statisticsProvider.incrementStatistic( ProviderStatistics.IncrementerStatistic.UNAVAILABLE_COUNT );
                getGlobalStatsBean().incrementStatistic( ProviderStatistics.IncrementerStatistic.UNAVAILABLE_COUNT );
            }

            throw exceptionCause;
        }
        finally
        {
            statisticsProvider.markTimestampStatistic( ProviderStatistics.TimestampStatistic.LAST_OPERATION_FINISH );
            getGlobalStatsBean().markTimestampStatistic( ProviderStatistics.TimestampStatistic.LAST_OPERATION_FINISH );
        }
    }

    static class StatsBean implements ProviderStatistics
    {

        private final Map<IncrementerStatistic, AtomicInteger> incrementerMap = new ConcurrentHashMap<>();
        private final Map<TimestampStatistic, Instant> timestampMap = new ConcurrentHashMap<>();

        StatsBean()
        {
            for ( final IncrementerStatistic statistic : IncrementerStatistic.values() )
            {
                incrementerMap.put( statistic, new AtomicInteger( 0 ) );
            }
            for ( final TimestampStatistic statistic : TimestampStatistic.values() )
            {
                timestampMap.put( statistic, Instant.now() );
            }
        }

        public long getIncrementorStatistic( final IncrementerStatistic statistic )
        {
            return incrementerMap.get( statistic ).get();
        }

        public Instant getTimestampStatistic( final TimestampStatistic timestampStatistic )
        {
            return timestampMap.get( timestampStatistic );
        }

        void incrementStatistic( final IncrementerStatistic incrementerStatistic )
        {
            incrementerMap.get( incrementerStatistic ).incrementAndGet();
        }

        void markTimestampStatistic( final TimestampStatistic timestampStatistic )
        {
            timestampMap.put( timestampStatistic, Instant.now() );
        }

        @Override
        public Map<String, String> allStatistics()
        {
            final Map<String, String> outputMap = new LinkedHashMap<>(  );

            for ( final IncrementerStatistic stat : IncrementerStatistic.values() )
            {
                outputMap.put( stat.name(), String.valueOf( incrementerMap.get( stat ) ) );
            }

            for ( final TimestampStatistic stat : TimestampStatistic.values() )
            {
                outputMap.put( stat.name(), String.valueOf( timestampMap.get( stat ) ) );
            }

            return Collections.unmodifiableMap( outputMap );
        }
    }
}
