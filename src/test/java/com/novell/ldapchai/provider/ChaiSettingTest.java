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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class ChaiSettingTest
{
    @Test
    public void testUniqueSettingKeys()
            throws Exception
    {
        final int settingCount = ChaiSetting.values().length;
        final Set<String> settingPropNames = new HashSet<>();
        for ( final ChaiSetting setting : ChaiSetting.values() )
        {
            settingPropNames.add( setting.getKey() );
        }

        Assertions.assertEquals( settingCount, settingPropNames.size() );
    }

    @Test
    public void testValidatedDefaults()
            throws Exception
    {
        for ( final ChaiSetting setting : ChaiSetting.values() )
        {
            setting.validateValue( setting.getDefaultValue() );
        }
    }
}
