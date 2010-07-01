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

package com.novell.ldapchai.tests;

import com.novell.ldapchai.ChaiPasswordRule;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.HashSet;
import java.util.Set;

public class PasswordPolicyTester extends TestCase {
// ----------------------------- CONSTANTS ----------------------------


// ------------------------------ FIELDS ------------------------------


// -------------------------- STATIC METHODS --------------------------

// -------------------------- OTHER METHODS --------------------------

    protected void setUp()
            throws Exception
    {
        TestHelper.setUp();
    }

    /*
    public void testCreateRandoms()
            throws Exception
    {
        final int loopSize = 9999;
        final long startTime = System.currentTimeMillis();
        System.out.println("generating random passwords with no seed .....");
        for (int i = 0; i < loopSize; i++) {
            testPolicy.createRandomPassword();
        }
        System.out.println("generating random passwords with small seed .....");
        for (int i = 0; i < loopSize; i++) {
            testPolicy.createRandomPassword(Arrays.asList("seed1", "seedB"), 8, 5);
        }
        System.out.println("generated passwords in " + (System.currentTimeMillis() - startTime) + "ms");
    }
    */

    public void testUniquePasswordRules()
            throws Exception
    {
        final int ruleCount = ChaiPasswordRule.values().length;
        final Set<String> rulePropNames = new HashSet<String>();
        for (final ChaiPasswordRule rule : ChaiPasswordRule.values()) {
            rulePropNames.add(rule.getKey());
        }

        Assert.assertEquals(ruleCount, rulePropNames.size());
    }

}
