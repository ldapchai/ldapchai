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


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Settings used by {@link ChaiProviderFactory} instances.
 */
public enum ChaiProviderFactorySetting
{
    /**
     * <p>The frequency that watchdog timeouts are checked (in ms).  The watchdog implementation is only guaranteed
     * to check timeouts at this frequency.  This will have a direct impact on the enforcement of timeouts.  For
     * example, if the idle timeout is 30 seconds, and the frequency is 30 seconds, then connections may actually
     * be able to remain idle between 30 and 60 seconds.</p>
     *
     * <table><caption>Setting Information</caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.provider.connection.watchdog.frequencyMs</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>1000</td></tr>
     * </table>
     */
    WATCHDOG_CHECK_FREQUENCY( "chai.providerFactory.connection.watchdog.frequencyMs", "1000", SettingValidator.INTEGER_VALIDATOR ),

    /**
     * <p>Maximum time duration to cache a vendor identification for a given LDAP URL.</p>
     *
     * <table><caption>Setting Information</caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.provider.vendor.cache.maxAgeMs</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>5000</td></tr>
     * </table>
     */
    VENDOR_CACHE_MAX_AGE_MS( "chai.providerFactory.vendorCache.maxAgeMs", "60000", SettingValidator.INTEGER_VALIDATOR ),;

    private final String key;
    private final String defaultValue;
    private final SettingValidator.Validator validator;

    private static final Map<ChaiProviderFactorySetting, String> DEFAULT_SETTINGS;

    static
    {
        final Map<ChaiProviderFactorySetting, String> settings = new LinkedHashMap<>();
        for ( final ChaiProviderFactorySetting s : ChaiProviderFactorySetting.values() )
        {
            settings.put( s, s.getDefaultValue() );
        }
        DEFAULT_SETTINGS = Collections.unmodifiableMap( settings );
    }

    ChaiProviderFactorySetting( final String key, final String defaultValue, final SettingValidator.Validator validator )
    {
        this.key = key;
        this.defaultValue = defaultValue;
        this.validator = validator;
    }

    public String getKey()
    {
        return key;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    @SuppressFBWarnings( value = "MS_EXPOSE_REP", justification = "static map is unmodifiable, no need to copy/wrap it" )
    public static Map<ChaiProviderFactorySetting, String> getDefaultSettings()
    {
        return DEFAULT_SETTINGS;
    }
}
