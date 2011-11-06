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

package com.novell.ldapchai.impl.ad.entry;

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiEntry;
import com.novell.ldapchai.provider.ChaiProvider;

import java.util.Date;

class TopImpl extends AbstractChaiEntry implements ChaiEntry {

    public TopImpl(final String entryDN, final ChaiProvider chaiProvider) {
        super(entryDN, chaiProvider);
    }

    @Override
    public Date readDateAttribute(final String attributeName) throws ChaiUnavailableException, ChaiOperationException {
        return ADEntries.readDateAttribute(this, attributeName);
    }

    @Override
    public void writeDateAttribute(final String attributeName, final Date date) throws ChaiUnavailableException, ChaiOperationException {
        ADEntries.writeDateAttribute(this, attributeName, date);
    }
}
