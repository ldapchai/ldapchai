package com.novell.ldapchai;

import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.provider.ChaiProviderFactory;

@Deprecated
public class ChaiFactory
{

    private ChaiFactory()
    {
    }

    /**
     * Convenience method for {@link com.novell.ldapchai.provider.ChaiProviderFactory#quickProvider(String, String, String)}.
     * <p>
     * Get a ChaiUser using a standard JNDI ChaiProvider with default settings.
     *
     * @param bindDN   ldap bind DN, in ldap fully qualified syntax.  Also used as the DN of the returned ChaiUser.
     * @param password password for the bind DN.
     * @param ldapURL  ldap server and port in url format, example: <i>ldap://127.0.0.1:389</i>
     * @return A ChaiUser instance of the bindDN with an underlying ChaiProvider connected using the supplied parameters.
     * @throws ChaiUnavailableException If the directory server(s) are not reachable.
     * @see com.novell.ldapchai.provider.ChaiProviderFactory#quickProvider (String, String, String)
     */
    @Deprecated
    public static ChaiUser quickProvider( final String ldapURL, final String bindDN, final String password )
            throws ChaiUnavailableException
    {
        return ChaiProviderFactory.newProviderFactory().quickProvider( ldapURL, bindDN, password );
    }

    @Deprecated
    public static ChaiEntry createChaiEntry( final String entryDN, final ChaiProvider provider )
            throws ChaiUnavailableException
    {

        return provider.getEntryFactory().createChaiEntry( entryDN );
    }

    /**
     * Returns a {@code ChaiGroup} instance representing the supplied <i>groupDN</i>.
     *
     * @param groupDN  A valid ldap entry DN (Distinguished Name) of an entry
     * @param provider A valid and functioning {@code ChaiProvider}.  The {@code ChaiProvider}'s ldap
     *                 connection will be used by the {@code ChaiGroup}.
     * @return A valid {@code ChaiGroup}
     */
    @Deprecated
    public static ChaiGroup createChaiGroup( final String groupDN, final ChaiProvider provider )
            throws ChaiUnavailableException
    {
        return provider.getEntryFactory().createChaiGroup( groupDN );
    }

    /**
     * Returns a {@code ChaiUser} instance representing the supplied <i>userDN</i>.
     *
     * @param userDN   A valid ldap entry DN (Distinguished Name) of an entry
     * @param provider A valid and functioning {@code ChaiProvider}.  The {@code ChaiProvider}'s ldap
     *                 connection will be used by the {@code ChaiGroup}.
     * @return A valid {@code ChaiUser}
     */
    @Deprecated
    public static ChaiUser createChaiUser( final String userDN, final ChaiProvider provider )
            throws ChaiUnavailableException
    {
        return provider.getEntryFactory().createChaiUser( userDN );
    }

    /**
     * Returns a {@code ChaiUser} instance representing the supplied <i>userDN</i>.
     *
     * @param provider A valid and functioning {@code ChaiProvider}.  The {@code ChaiProvider}'s ldap
     *                 connection will be used by the {@code ChaiGroup}.
     * @return A valid {@code ChaiUser}
     */
    @Deprecated
    public static ErrorMap getErrorMap( final ChaiProvider provider )
            throws ChaiUnavailableException
    {
        return provider.getEntryFactory().getErrorMap();
    }

    /**
     * Returns a {@code ChaiUser} instance representing the supplied <i>userDN</i>.
     *
     * @param vendor A valid and functioning {@code ChaiProvider}.  The {@code ChaiProvider}'s ldap
     *               connection will be used by the {@code ChaiGroup}.
     * @return A valid {@code ChaiUser}
     */
    @Deprecated
    public static ErrorMap getErrorMap( final ChaiProvider.DIRECTORY_VENDOR vendor )
    {
        return ChaiEntryFactory.getErrorMap( vendor );
    }
}
