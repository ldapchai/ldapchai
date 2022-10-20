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

import com.novell.ldapchai.ChaiEntryFactory;
import com.novell.ldapchai.ChaiRequestControl;
import com.novell.ldapchai.ChaiUser;
import com.novell.ldapchai.exception.ChaiException;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.internal.ChaiLogger;
import com.novell.ldapchai.util.SearchHelper;

import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link ChaiProvider} implementation wrapper that handles automatic idle disconnects.
 *
 * @author Jason D. Rivard
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_ENABLE
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_IDLE_TIMEOUT
 * @see com.novell.ldapchai.provider.ChaiSetting#WATCHDOG_OPERATION_TIMEOUT
 */
class WatchdogWrapper implements ChaiProviderImplementor
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( WatchdogWrapper.class );

    private final WatchdogProviderHolder providerHolder;
    private final ChaiConfiguration chaiConfiguration;
    private final ChaiProviderFactory chaiProviderFactory;
    private final Settings settings;

    private WatchdogWrapper(
            final ChaiProviderFactory chaiProviderFactory,
            final ChaiProviderImplementor chaiProviderImplementor
    )
    {
        this.chaiConfiguration = chaiProviderImplementor.getChaiConfiguration();
        this.chaiProviderFactory = chaiProviderFactory;
        this.settings = Settings.fromConfig( chaiConfiguration );
        this.providerHolder = new WatchdogProviderHolder( this, chaiProviderImplementor );
    }

    static ChaiProviderImplementor forProvider(
            final ChaiProviderFactory chaiProviderFactory,
            final ChaiProviderImplementor chaiProvider
    )
    {
        //check to make sure watchdog is enabled;
        final boolean watchDogEnabled = Boolean.parseBoolean( chaiProvider.getChaiConfiguration().getSetting( ChaiSetting.WATCHDOG_ENABLE ) );
        if ( !watchDogEnabled )
        {
            final String errorStr = "attempt to obtain WatchdogWrapper wrapper when watchdog is not enabled in chai config id="
                    + chaiProvider.getIdentifier();

            LOGGER.warn( () -> errorStr );
            throw new IllegalStateException( errorStr );
        }

        if ( chaiProvider instanceof WatchdogWrapper )
        {
            LOGGER.debug( () -> "attempt to obtain WatchdogWrapper wrapper for already wrapped Provider id=" + chaiProvider.getIdentifier() );
            return chaiProvider;
        }

        return new WatchdogWrapper( chaiProviderFactory, chaiProvider );
    }

    @Override
    public Object getConnectionObject()
            throws Exception
    {
        return providerHolder.getProvider().getConnectionObject();
    }

    @Override
    public ConnectionState getConnectionState()
    {
        if ( providerHolder.getStatus() == WatchdogProviderHolder.HolderStatus.CLOSED )
        {
            return ConnectionState.CLOSED;
        }

        return ConnectionState.OPEN;
    }

    @Override
    public String getCurrentConnectionURL()
    {
        try
        {
            return providerHolder.getProvider().getCurrentConnectionURL();
        }
        catch ( ChaiUnavailableException e )
        {
            throw new IllegalStateException( "unexpected error trying to load internal provider: " + e.getMessage(), e );
        }
    }

    @Override
    public Map<String, Object> getProviderProperties()
    {
        try
        {
            return providerHolder.getProvider().getProviderProperties();
        }
        catch ( ChaiUnavailableException e )
        {
            throw new IllegalStateException( "unexpected error trying to load internal provider: " + e.getMessage(), e );
        }
    }

    @Override
    public boolean errorIsRetryable( final Exception e )
    {
        try
        {
            return providerHolder.getProvider().errorIsRetryable( e );
        }
        catch ( ChaiUnavailableException e1 )
        {
            throw new IllegalStateException( "unexpected error trying to load internal provider: " + e.getMessage(), e );
        }
    }

    @Override
    public void init( final ChaiConfiguration chaiConfig, final ChaiProviderFactory providerFactory )
            throws ChaiUnavailableException, IllegalStateException
    {
    }

    @Override
    public String getIdentifier()
    {
        return providerHolder == null
                ? "[null provider holder]"
                : providerHolder.getIdentifier();
    }

    @Override
    public void close()
    {
        if ( providerHolder != null )
        {
            providerHolder.close();
        }
    }

    @Override
    public boolean compareStringAttribute( final String entryDN, final String attributeName, final String value )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.execute( chaiProvider -> chaiProvider.compareStringAttribute( entryDN, attributeName, value ) );
    }

    @Override
    public void createEntry( final String entryDN, final String baseObjectClass, final Map<String, String> stringAttributes )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.createEntry( entryDN, baseObjectClass, stringAttributes );
            return null;
        } );
    }

    @Override
    public void createEntry( final String entryDN, final Set<String> baseObjectClasses, final Map<String, String> stringAttributes )
            throws ChaiOperationException, ChaiUnavailableException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.createEntry( entryDN, baseObjectClasses, stringAttributes );
            return null;
        } );
    }

    @Override
    public void renameEntry( final String entryDN, final String newRDN, final String newParentDN )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.renameEntry( entryDN, newRDN, newParentDN );
            return null;
        } );
    }

    @Override
    public void deleteEntry( final String entryDN )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.deleteEntry( entryDN );
            return null;
        } );
    }

    @Override
    public void deleteStringAttributeValue( final String entryDN, final String attributeName, final String value )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.deleteStringAttributeValue( entryDN, attributeName, value );
            return null;
        } );
    }

    @Override
    public ExtendedResponse extendedOperation( final ExtendedRequest request )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.execute( chaiProvider -> chaiProvider.extendedOperation( request ) );
    }

    @Override
    public ChaiConfiguration getChaiConfiguration()
    {
        return chaiConfiguration;
    }

    @Override
    public ProviderStatistics getProviderStatistics()
    {
        return null;
    }

    @Override
    public byte[][] readMultiByteAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.execute( chaiProvider -> chaiProvider.readMultiByteAttribute( entryDN, attribute ) );
    }

    @Override
    public Set<String> readMultiStringAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.execute( chaiProvider -> chaiProvider.readMultiStringAttribute( entryDN, attribute ) );
    }

    @Override
    public String readStringAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.execute( chaiProvider -> chaiProvider.readStringAttribute( entryDN, attribute ) );
    }

    @Override
    public Map<String, String> readStringAttributes( final String entryDN, final Set<String> attributes )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.execute( chaiProvider -> chaiProvider.readStringAttributes( entryDN, attributes ) );
    }

    @Override
    public void replaceStringAttribute( final String entryDN, final String attributeName, final String oldValue, final String newValue )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.replaceStringAttribute( entryDN, attributeName, oldValue, newValue );
            return null;
        } );
    }

    @Override
    public Map<String, Map<String, String>> search( final String baseDN, final SearchHelper searchHelper )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.execute( chaiProvider -> chaiProvider.search( baseDN, searchHelper ) );
    }

    @Override
    public Map<String, Map<String, String>> search( final String baseDN, final String filter, final Set<String> attributes, final SearchScope searchScope )
            throws ChaiOperationException, ChaiUnavailableException
    {
        return providerHolder.execute( chaiProvider -> chaiProvider.search( baseDN, filter, attributes, searchScope ) );
    }

    @Override
    public Map<String, Map<String, List<String>>> searchMultiValues( final String baseDN, final SearchHelper searchHelper )
            throws ChaiUnavailableException, ChaiOperationException
    {
        return providerHolder.execute( chaiProvider -> chaiProvider.searchMultiValues( baseDN, searchHelper ) );
    }

    @Override
    public Map<String, Map<String, List<String>>> searchMultiValues( final String baseDN, final String filter, final Set<String> attributes, final SearchScope searchScope )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.execute( chaiProvider -> chaiProvider.searchMultiValues( baseDN, filter, attributes, searchScope ) );
    }

    @Override
    public void writeBinaryAttribute( final String entryDN, final String attributeName, final byte[][] values, final boolean overwrite )
            throws ChaiUnavailableException, ChaiOperationException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.writeBinaryAttribute( entryDN, attributeName, values, overwrite );
            return null;
        } );
    }

    @Override
    public void writeBinaryAttribute( final String entryDN, final String attributeName, final byte[][] values, final boolean overwrite, final ChaiRequestControl[] controls )
            throws ChaiUnavailableException, ChaiOperationException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.writeBinaryAttribute( entryDN, attributeName, values, overwrite, controls );
            return null;
        } );
    }

    @Override
    public void writeStringAttribute( final String entryDN, final String attributeName, final Set<String> values, final boolean overwrite )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.writeStringAttribute( entryDN, attributeName, values, overwrite );
            return null;
        } );
    }

    @Override
    public void writeStringAttributes( final String entryDN, final Map<String, String> attributeValueProps, final boolean overwrite )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.writeStringAttributes( entryDN, attributeValueProps, overwrite );
            return null;
        } );
    }

    @Override
    public DirectoryVendor getDirectoryVendor()
            throws ChaiUnavailableException
    {
        try
        {
            return providerHolder.execute( chaiProvider -> chaiProvider.getDirectoryVendor() );
        }
        catch ( ChaiOperationException e )
        {
            LOGGER.error( () -> "unexpected ChaiOperationException during getDirectoryVendor " + e.getMessage(), e );
        }
        return null;
    }

    @Override
    public void replaceBinaryAttribute( final String entryDN, final String attributeName, final byte[] oldValue, final byte[] newValue )
            throws ChaiUnavailableException, ChaiOperationException
    {
        providerHolder.execute( ( WatchdogProviderHolder.LdapFunction<Void> ) chaiProvider ->
        {
            chaiProvider.replaceBinaryAttribute( entryDN, attributeName, oldValue, newValue );
            return null;
        } );
    }

    @Override
    public boolean isConnected()
    {
        return providerHolder.getStatus() == WatchdogProviderHolder.HolderStatus.ACTIVE;
    }

    @Override
    public ChaiProviderFactory getProviderFactory()
    {
        return chaiProviderFactory;
    }

    public Settings getSettings()
    {
        return settings;
    }

    @Override
    public ChaiEntryFactory getEntryFactory()
    {
        return ChaiEntryFactory.newChaiFactory( this );
    }

    void checkStatus()
    {
        providerHolder.checkStatus();
    }

    static class Settings
    {
        private final int operationTimeout;
        private final int idleTimeout;
        private final Duration maxConnectionLifetime;

        private Settings( final int operationTimeout, final int idleTimeout, final Duration maxConnectionLifetime )
        {
            this.operationTimeout = operationTimeout;
            this.idleTimeout = idleTimeout;
            this.maxConnectionLifetime = maxConnectionLifetime;
        }

        public int getOperationTimeoutMS()
        {
            return operationTimeout;
        }

        public int getIdleTimeoutMS()
        {
            return idleTimeout;
        }

        public Duration getMaxConnectionLifetime()
        {
            return maxConnectionLifetime;
        }

        private static Settings fromConfig( final ChaiConfiguration chaiConfiguration )
        {
            final int operationTimeout = Integer.parseInt( chaiConfiguration.getSetting( ChaiSetting.WATCHDOG_OPERATION_TIMEOUT ) );
            final int idleTimeout = Integer.parseInt( chaiConfiguration.getSetting( ChaiSetting.WATCHDOG_IDLE_TIMEOUT ) );
            final Duration maxConnectionLifetime = Duration.of(
                    Integer.parseInt( chaiConfiguration.getSetting( ChaiSetting.WATCHDOG_MAX_CONNECTION_LIFETIME ) ),
                    ChronoUnit.MILLIS );
            return new Settings( operationTimeout, idleTimeout, maxConnectionLifetime );
        }
    }

    boolean checkForPwExpiration(
            final ChaiProviderImplementor chaiProvider
    )

    {
        final boolean doPwExpCheck = chaiProvider.getChaiConfiguration().getBooleanSetting( ChaiSetting.WATCHDOG_DISABLE_IF_PW_EXPIRED );
        if ( !doPwExpCheck )
        {
            return false;
        }

        boolean userPwExpired;
        try
        {
            final String bindUserDN = chaiProvider.getChaiConfiguration().getSetting( ChaiSetting.BIND_DN );
            final ChaiUser bindUser = chaiProvider.getEntryFactory().newChaiUser( bindUserDN );
            userPwExpired = bindUser.isPasswordExpired();
        }
        catch ( ChaiException e )
        {
            LOGGER.error( () -> "unexpected error attempting to read user password expiration value during"
                    + " watchdog initialization, will assume expiration, error: " + e.getMessage() );
            userPwExpired = true;
        }

        if ( userPwExpired )
        {
            LOGGER.info( () -> "connection user account password is currently expired.  Disabling watchdog timeout. id=" + this.getIdentifier() );
            return true;
        }

        return false;
    }

    @Override
    public String toString()
    {
        return "WatchdogWrapper[" + getIdentifier() + "]";
    }
}
