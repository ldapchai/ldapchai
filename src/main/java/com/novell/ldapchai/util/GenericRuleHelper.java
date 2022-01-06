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

package com.novell.ldapchai.util;

import com.novell.ldapchai.ChaiPasswordPolicy;
import com.novell.ldapchai.ChaiPasswordRule;
import com.novell.ldapchai.util.internal.StringHelper;

import java.util.List;

/**
 * A generic rule helper implementation.
 *
 * @author Jason D. Rivard
 */
public class GenericRuleHelper implements PasswordRuleHelper
{
    private final ChaiPasswordPolicy wrappedPolicy;

    public GenericRuleHelper( final ChaiPasswordPolicy policy )
    {
        this.wrappedPolicy = policy;
    }

    @Override
    public boolean isAllowNumeric()
    {
        return StringHelper.convertStrToBoolean( readSetting( ChaiPasswordRule.AllowNumeric ) );
    }

    @Override
    public boolean isAllowFirstCharNumeric()
    {
        return readBooleanSetting( ChaiPasswordRule.AllowFirstCharNumeric );
    }

    @Override
    public boolean isAllowLastCharNumeric()
    {
        return readBooleanSetting( ChaiPasswordRule.AllowLastCharNumeric );
    }

    @Override
    public boolean isAllowSpecial()
    {
        return readBooleanSetting( ChaiPasswordRule.AllowSpecial );
    }

    @Override
    public boolean isAllowFirstCharSpecial()
    {
        return readBooleanSetting( ChaiPasswordRule.AllowFirstCharSpecial );
    }

    @Override
    public boolean isAllowLastCharSpecial()
    {
        return readBooleanSetting( ChaiPasswordRule.AllowLastCharSpecial );
    }

    @Override
    public int getMaximumSequentialRepeat()
    {
        return readNumericSetting( ChaiPasswordRule.MaximumSequentialRepeat );
    }

    @Override
    public int getMaximumRepeat()
    {
        return readNumericSetting( ChaiPasswordRule.MaximumRepeat );
    }

    @Override
    public int getMinimumLifetime()
    {
        return readNumericSetting( ChaiPasswordRule.MinimumLifetime );
    }

    @Override
    public final String getChangeMessage()
    {
        return readSetting( ChaiPasswordRule.ChangeMessage );
    }

    @Override
    public int getExpirationInterval()
    {
        return readNumericSetting( ChaiPasswordRule.ExpirationInterval );
    }

    @Override
    public boolean isCaseSensitive()
    {
        return readBooleanSetting( ChaiPasswordRule.CaseSensitive );
    }

    @Override
    public boolean isEnforceAtLogin()
    {
        return readBooleanSetting( ChaiPasswordRule.EnforceAtLogin );
    }

    @Override
    public boolean isUniqueRequired()
    {
        return readBooleanSetting( ChaiPasswordRule.UniqueRequired );
    }

    @Override
    public boolean isPolicyEnabled()
    {
        return readBooleanSetting( ChaiPasswordRule.PolicyEnabled );
    }

    private String readSetting( final ChaiPasswordRule attr )
    {
        return wrappedPolicy.getValue( attr );
    }

    private boolean readBooleanSetting( final ChaiPasswordRule attr )
    {
        return StringHelper.convertStrToBoolean( readSetting( attr ) );
    }

    private int readNumericSetting( final ChaiPasswordRule attr )
    {
        return StringHelper.convertStrToInt( readSetting( attr ), 0 );
    }

    @Override
    public List<String> getDisallowedValues()
    {
        return StringHelper.tokenizeString( readSetting( ChaiPasswordRule.DisallowedValues ), "\n" );
    }

    @Override
    public List<String> getDisallowedAttributes()
    {
        return StringHelper.tokenizeString( readSetting( ChaiPasswordRule.DisallowedAttributes ), "\n" );
    }
}
