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
import com.novell.ldapchai.util.internal.ChaiLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Default implementation of {@link com.novell.ldapchai.provider.ProviderStatistics}.
 *
 * @author Jason D. Rivard
 * @see ChaiSetting#STATISTICS_ENABLE
 */
class StatisticsWrapper implements InvocationHandler
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( StatisticsWrapper.class );

    /**
     * The standard wrapper manages updating statistics and handling the wire trace functionality.
     */
    private final ChaiProviderImplementor realProvider;

    private final StatsBean statisticsProvider = new StatsBean();

    static ChaiProviderImplementor forProvider( final ChaiProviderImplementor chaiProvider )
    {
        if ( Proxy.isProxyClass( chaiProvider.getClass() ) && chaiProvider instanceof StatisticsWrapper )
        {
            LOGGER.warn( () -> "attempt to obtain StatisticsWrapper wrapper for already wrapped Provider." );
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


    @Override
    public Object invoke( final Object proxy, final Method method, final Object[] args )
            throws Throwable
    {
        final boolean isLdap = method.getAnnotation( ChaiProviderImplementor.LdapOperation.class ) != null;

        if ( method.getName().equals( "getProviderStatistics" ) )
        {
            return statisticsProvider;
        }

        if ( isLdap )
        {
            incrementStat( ProviderStatistics.IncrementerStatistic.OPERATION_COUNT );

            final boolean isModify = method.getAnnotation( ChaiProviderImplementor.ModifyOperation.class ) != null;
            final boolean isSearch = method.getAnnotation( ChaiProviderImplementor.SearchOperation.class ) != null;

            if ( isModify )
            {
                incrementStat( ProviderStatistics.IncrementerStatistic.MODIFY_COUNT );
            }
            else if ( isSearch )
            {
                incrementStat( ProviderStatistics.IncrementerStatistic.SEARCH_COUNT );
            }
            else
            {
                incrementStat( ProviderStatistics.IncrementerStatistic.READ_COUNT );
            }

            markTimestampStatistic( ProviderStatistics.TimestampStatistic.LAST_OPERATION_BEGIN );
        }

        try
        {
            return method.invoke( realProvider, args );
        }
        catch ( InvocationTargetException e )
        {
            final Throwable exceptionCause = e.getCause();

            if ( exceptionCause instanceof ChaiUnavailableException )
            {
                markTimestampStatistic( ProviderStatistics.TimestampStatistic.LAST_UNAVAILABLE_EXCEPTION );

                incrementStat( ProviderStatistics.IncrementerStatistic.UNAVAILABLE_COUNT );
            }

            throw exceptionCause;
        }
        finally
        {
            markTimestampStatistic( ProviderStatistics.TimestampStatistic.LAST_OPERATION_FINISH );
        }
    }

    private void incrementStat( final ProviderStatistics.IncrementerStatistic incrementerStatistic )
    {
        statisticsProvider.incrementStatistic( incrementerStatistic );
        getGlobalStatsBean().incrementStatistic( incrementerStatistic );
    }

    private void markTimestampStatistic( final ProviderStatistics.TimestampStatistic timestampStatistic )
    {
        statisticsProvider.markTimestampStatistic( timestampStatistic );
        getGlobalStatsBean().markTimestampStatistic( timestampStatistic );
    }

    static class StatsBean implements ProviderStatistics
    {

        private final Map<IncrementerStatistic, LongAdder> incrementerMap = new HashMap<>();
        private final Map<TimestampStatistic, Instant> timestampMap = new ConcurrentHashMap<>();

        StatsBean()
        {
            for ( final IncrementerStatistic statistic : IncrementerStatistic.values() )
            {
                incrementerMap.put( statistic, new LongAdder() );
            }
            for ( final TimestampStatistic statistic : TimestampStatistic.values() )
            {
                timestampMap.put( statistic, Instant.now() );
            }
        }

        @Override
        public long getIncrementorStatistic( final IncrementerStatistic statistic )
        {
            return incrementerMap.get( statistic ).sum();
        }

        @Override
        public Instant getTimestampStatistic( final TimestampStatistic timestampStatistic )
        {
            return timestampMap.get( timestampStatistic );
        }

        void incrementStatistic( final IncrementerStatistic incrementerStatistic )
        {
            incrementerMap.get( incrementerStatistic ).increment();
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
