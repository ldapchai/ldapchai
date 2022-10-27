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

package com.novell.ldapchai.impl.edir.value;

import com.novell.ldapchai.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NspmComplexityRulesTest
{
    @Test
    public void testResponseSet1()
            throws Exception
    {
        final String inputXml = TestHelper.readResourceFile( NspmComplexityRulesTest.class, "NspmComplexityRulesTest1.xml" );
        final NspmComplexityRules nspmComplexityRules = new NspmComplexityRules( inputXml );
        Assertions.assertFalse( nspmComplexityRules.isMsComplexityPolicy() );
    }
}
