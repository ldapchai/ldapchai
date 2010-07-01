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

package com.novell.ldapchai;

import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;

import java.util.Set;

/**
 * Represents an ldap group entry.
 * <p/>
 * Instances of ChaiGroup can be obtained by using {@link com.novell.ldapchai.ChaiFactory}.
 *
 * @author Jason D. Rivard
 */
public interface ChaiGroup extends ChaiEntry {
// ----------------------------- CONSTANTS ----------------------------

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_GROUP_NAME = ChaiConstant.ATTR_LDAP_DESCRIPTION;

    /**
     * Convenience LDAP attribute definition
     */
    public static final String ATTR_AUXCLASS_DYNAMIC = ChaiConstant.OBJECTCLASS_AUX_LDAP_DYNAMIC_GROUP;

// -------------------------- OTHER METHODS --------------------------

    /**
     * Make the passed in user a member of this group.  This method takes care of all four attribute assignments
     * used in eDirectory static groups.
     *
     * @param theUser The user to assign to this group's membership list.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see ChaiUser#addGroupMembership(ChaiGroup)
     */
    void addMember(ChaiUser theUser)
            throws ChaiUnavailableException, ChaiOperationException;

    /**
     * Get a Set of ChaiUser instances that are members of this group.
     *
     * @return Set of ChaiUser objects.  If there are no members, then an empty Set will be returned.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    Set<ChaiUser> getMembers()
            throws ChaiOperationException, ChaiUnavailableException;


    /**
     * Convienence method to read this ChaiUser instance's {@link #ATTR_GROUP_NAME} attribute.
     *
     * @return The value of the attribute, or null if no value
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    String readGroupName()
            throws ChaiOperationException, ChaiUnavailableException;

    /**
     * Remove the passed in user from being a member of this group.  This method takes care of all four attribute assignments
     * used in eDirectory static groups.
     *
     * @param theUser The user to remove from this group's membership list.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     * @see ChaiUser#removeGroupMembership(ChaiGroup)
     */
    void removeMember(ChaiUser theUser)
            throws ChaiUnavailableException, ChaiOperationException;
}

