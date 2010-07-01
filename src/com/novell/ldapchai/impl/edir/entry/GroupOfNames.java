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

import com.novell.ldapchai.ChaiConstant;
import com.novell.ldapchai.ChaiGroup;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;

public interface GroupOfNames extends Top, ChaiGroup {
    public static final String OBJECT_CLASS_VALUE = ChaiConstant.OBJECTCLASS_BASE_LDAP_GROUP;

    

    /**
     * Identifies if the group is dynamic or not.
     * <p/>
     * <i>Implementation Note:<i/> This method is functionally equivalent to calling
     * {@link #compareStringAttribute(String ATTR_OBJECT_CLASS,String OBJECTCLASS_AUX_LDAP_DYNAMIC_GROUP)}
     *
     * @return true if the group is dynamic
     * @throws com.novell.ldapchai.exception.ChaiOperationException   If there is an error during the operation
     * @throws com.novell.ldapchai.exception.ChaiUnavailableException If the directory server(s) are unavailable
     */
    boolean isDynamic()
            throws ChaiUnavailableException, ChaiOperationException;

}
