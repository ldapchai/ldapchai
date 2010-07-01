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

import java.io.Serializable;
import java.util.Map;

/**
 * Tracks the statistics for a {@link ChaiProvider}.  For a list of available statistics, see {@link com.novell.ldapchai.provider.ProviderStatistics.Statistic}.
 *
 * @author Jason D. Rivard
 * @see com.novell.ldapchai.provider.ChaiSetting#STATISTICS_ENABLE
 */
public interface ProviderStatistics extends Serializable {
// ----------------------------- CONSTANTS ----------------------------


// -------------------------- ENUMERATIONS --------------------------

    /**
     * Enumeration of available statistics.
     */
    enum Statistic {
        /**
         * Number of read operations
         */
        READ_COUNT,
        /**
         * Number of modify operations
         */
        MODIFY_COUNT,

        /**
         * Number of search operations
         */
        SEARCH_COUNT,

        /**
         * Total number of ldap operations operations
         */
        OPERATION_COUNT,

        /**
         * Total number of ldap unavailabe exceptions thrown
         */
        UNAVAILABLE_COUNT,

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

// -------------------------- OTHER METHODS --------------------------

    /**
     * Get an individual statistic.
     *
     * @param statistic requested statistc
     * @return the string value of the statistic, typically a number or timestamp.
     */
    public String getStatistic(final Statistic statistic);

    /**
     * Get all of the available statistics in a convenient map.
     *
     * @return An unmodifiable map of statistic values.
     */
    public Map<Statistic, String> getStatistics();
}
