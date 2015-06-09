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

package com.novell.ldapchai.impl.ad.value;

import com.novell.ldapchai.util.StringHelper;

public class UserAccountControl {
    public enum UAC_BIT {
        ACCOUNT_ACTIVE(0x2),
        ACCOUNT_LOCKOUT(0x10),
        PASSWORD_EXPIRED(0x800000),
        PASSWORD_CANT_CHANGE(0x40),
        DONT_EXPIRE_PASSWORD(0x10000),
        ;

        UAC_BIT(final int bitValue)
        {
            this.bitValue = bitValue;
        }

        private final int bitValue;

        public int bitValue()
        {
            return bitValue;
        }
    }

    private final int uacValue;

    public UserAccountControl(final String uacValue) {
        this.uacValue = StringHelper.convertStrToInt(uacValue, 0);
    }

    public UserAccountControl(final int uacValue) {
        this.uacValue = uacValue;
    }

    public boolean isAccountLockout() {
        return isBit(UAC_BIT.ACCOUNT_LOCKOUT);
    }

    public boolean isAccountActive() {
        return isBit(UAC_BIT.ACCOUNT_ACTIVE);
    }

    public boolean isPasswordCantChange() {
        return isBit(UAC_BIT.PASSWORD_CANT_CHANGE);
    }

    public boolean isPasswordExpired() {
        return isBit(UAC_BIT.PASSWORD_EXPIRED);
    }

    public boolean isPasswordNeverExpires() {
        return isBit(UAC_BIT.PASSWORD_CANT_CHANGE);
    }

    public boolean isBit(final UAC_BIT uacBit) {
        return (uacValue & uacBit.bitValue()) == uacBit.bitValue();
    }
}
