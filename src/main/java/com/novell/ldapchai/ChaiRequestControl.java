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

package com.novell.ldapchai;

import java.io.Serializable;
import java.util.Arrays;

/**
 * An immutable representation of an LDAP extended request control.
 */
public class ChaiRequestControl implements Serializable
{
    private final String id;
    private final boolean critical;
    private final byte[] value;

    public ChaiRequestControl(
            final String id,
            final boolean critical,
            final byte[] value
    )
    {
        this.id = id;
        this.critical = critical;
        this.value = value == null ? null : Arrays.copyOf( value, value.length );
    }

    public String getId()
    {
        return id;
    }

    public boolean isCritical()
    {
        return critical;
    }

    public byte[] getValue()
    {
        return value == null ? null : Arrays.copyOf( value, value.length );
    }
}
