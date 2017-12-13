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
