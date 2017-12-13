package com.novell.ldapchai.provider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum ChaiProviderFactorySetting
{
    /**
     * <p>The frequency that watchdog timeouts are checked (in ms).  The watchdog implementation is only guaranteed
     * to check timeouts at this frequency.  This will have a direct impact on the enforcement of timeouts.  For
     * example, if the idle timeout is 30 seconds, and the frequency is 30 seconds, then connections may actually
     * be able to remain idle between 30 and 60 seconds.</p>
     *
     * <table border="0"><caption>Setting Information</caption>
     * <tr><td style="text-align: right"><i>Key: </i></td><td>chai.connection.watchdog.frequency</td></tr>
     * <tr><td style="text-align: right"><i>Default: </i></td><td>5000</td></tr>
     * </table>
     */
    WATCHDOG_CHECK_FREQUENCY( "chai.connection.watchdog.frequency", "1000", ChaiSetting.Validator.INTEGER_VALIDATOR ),;

    private final String key;
    private final String defaultValue;
    private final ChaiSetting.Validator validator;

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

    ChaiProviderFactorySetting( final String key, final String defaultValue, final ChaiSetting.Validator validator )
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

    public static Map<ChaiProviderFactorySetting, String> getDefaultSettings()
    {
        return DEFAULT_SETTINGS;
    }
}
