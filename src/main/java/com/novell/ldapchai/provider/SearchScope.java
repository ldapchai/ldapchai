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

/**
 * LDAP search scope of BASE, ONE or SUBTREE.
 */
public enum SearchScope
{
    /**
     * Search the container below the specified context, but not any children below the specified context.
     */
    ONE( javax.naming.directory.SearchControls.ONELEVEL_SCOPE ),
    /**
     * Search the specified object, but not any descendants.
     */
    BASE( javax.naming.directory.SearchControls.OBJECT_SCOPE ),

    /**
     * Search the descendants below the specified context, and all lower descendants.
     */
    SUBTREE( javax.naming.directory.SearchControls.SUBTREE_SCOPE ),;

    private final int jndiScopeInt;

    SearchScope( final int jndiScopeInt )
    {
        this.jndiScopeInt = jndiScopeInt;
    }

    /**
     * Get the JNDI equivalent constant.
     *
     * @return the equivalent JNDI {@link javax.naming.directory.SearchControls} scope constant.
     */
    public int getJndiScopeInt()
    {
        return jndiScopeInt;
    }
}
