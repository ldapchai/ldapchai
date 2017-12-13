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

import java.time.Instant;
import java.util.Map;

/**
 * Tracks the statistics for a {@link ChaiProvider}.  For a list of available statistics, see
 * {@link com.novell.ldapchai.provider.ProviderStatistics.IncrementerStatistic} and
 * {@link com.novell.ldapchai.provider.ProviderStatistics.TimestampStatistic}.
 *
 * @author Jason D. Rivard
 * @see com.novell.ldapchai.provider.ChaiSetting#STATISTICS_ENABLE
 */
public interface ProviderStatistics
{

    /**
     * Enumeration of available statistics.
     */
    enum IncrementerStatistic
    {
        /**
         * Number of read operations.
         */
        READ_COUNT,

        /**
         * Number of modify operations.
         */
        MODIFY_COUNT,

        /**
         * Number of search operations.
         */
        SEARCH_COUNT,

        /**
         * Total number of ldap operations.
         */
        OPERATION_COUNT,

        /**
         * Total number of ldap bind operations.
         */
        BIND_COUNT,

        /**
         * Total number of ldap unavailable exceptions thrown.
         */
        UNAVAILABLE_COUNT,

    }

    enum TimestampStatistic
    {
        /**
         * The last time (in ms seconds epoch) a ldap operation was initiated.
         */
        LAST_OPERATION_BEGIN,

        /**
         * The last time (in ms seconds epoch) a ldap operation was completed.
         */
        LAST_OPERATION_FINISH,

        /**
         * The last time (in ms seconds epoch) a {@link com.novell.ldapchai.exception.ChaiUnavailableException} was returned.
         */
        LAST_UNAVAILABLE_EXCEPTION,
    }


    /**
     * Get an individual statistic.
     *
     * @param statistic requested statistic
     * @return the string value of the statistic, typically a number or timestamp.
     */
    long getIncrementorStatistic( IncrementerStatistic statistic );

    Instant getTimestampStatistic( TimestampStatistic statistic );

    /**
     * Generate a string key/value map with all statistics suitable for debug logging.
     * @return a string map suitable for debug logging.
     */
    Map<String, String> allStatistics();

}
