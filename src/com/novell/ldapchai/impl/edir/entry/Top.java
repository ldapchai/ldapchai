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

package com.novell.ldapchai.impl.edir.entry;

import com.novell.ldapchai.ChaiEntry;

/**
 * eDirectory "top" class.  All other eDirectory classes inherit from this class.
 * <p/>
 * See <a href="http://developer.novell.com/documentation/ndslib/schm_enu/data/sdk714.html#sdk714">eDirectory schema documentation</a> for
 * more information.
 *
 * @author Jason D. Rivard
 */
public interface Top extends ChaiEntry, EdirEntry {
// -------------------------- OTHER METHODS --------------------------

    public String getLdapObjectClassName();    
}
