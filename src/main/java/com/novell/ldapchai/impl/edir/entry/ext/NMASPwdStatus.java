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

package com.novell.ldapchai.impl.edir.entry.ext;

public final class NMASPwdStatus
{
    public static final byte SPM_UPWD_ENABLED = 1;
    public static final byte SPM_UPWD_SET = 2;
    public static final byte SPM_UPWD_HISTORY_FULL = 4;
    public static final byte SPM_UPWD_MATCHES_NDS = 16;
    public static final byte SPM_UPWD_OLDER_THAN_NDS = 32;
    public static final byte SPM_UPWD_MATCHES_SPWD = 64;
    public static final byte SPM_SPWD_SET = 1;
    public static final byte SPM_SPWD_IS_CLEARTEXT = 2;
    public static final byte SPM_SPWD_MATCHES_NDS = 16;
    private int universalPwdStatus = 0;
    private int simplePwdStatus = 0;

    public NMASPwdStatus()
    {
    }

    public void setUniversalPwdStatus( final int var1 )
    {
        this.universalPwdStatus = var1;
    }

    public int getUniversalPwdStatus()
    {
        return this.universalPwdStatus;
    }

    public boolean isSpmUpwdEnabled()
    {
        return ( 1 & this.universalPwdStatus ) > 0;
    }

    public boolean isSpmUpwdSet()
    {
        return ( 2 & this.universalPwdStatus ) > 0;
    }

    public boolean isSpmUpwdHistoryFull()
    {
        return ( 4 & this.universalPwdStatus ) > 0;
    }

    public boolean isSpmUpwdMatchesNDS()
    {
        return ( 16 & this.universalPwdStatus ) > 0;
    }

    public boolean isSpmUpwdOlderThanNDS()
    {
        return ( 32 & this.universalPwdStatus ) > 0;
    }

    public boolean isSpmUpwdMatchesSPWD()
    {
        return ( 64 & this.universalPwdStatus ) > 0;
    }

    public void setSimplePwdStatus( final int var1 )
    {
        this.simplePwdStatus = var1;
    }

    public int getSimplePwdStatus()
    {
        return this.simplePwdStatus;
    }

    public boolean isSpmSpwdSet()
    {
        return ( 1 & this.simplePwdStatus ) > 0;
    }

    public boolean isSpmSpwdClearText()
    {
        return ( 2 & this.simplePwdStatus ) > 0;
    }

    public boolean isSpmSpwdMatchesNDS()
    {
        return ( 16 & this.simplePwdStatus ) > 0;
    }
}
