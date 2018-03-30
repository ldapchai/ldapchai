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
import com.novell.ldapchai.util.ChaiLogger;
import com.novell.ldapchai.util.SearchHelper;

import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private static final AtomicInteger ID_COUNTER = new AtomicInteger( 0 );
    private final int counter = ID_COUNTER.getAndIncrement();

    @Override
    public Object getConnectionObject()
            throws Exception
    {
        return providerHolder.getProvider().getConnectionObject();
    }

    @Override
    public ConnectionState getConnectionState()
    {
        if ( providerHolder.getStatus() == HolderStatus.CLOSED )
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
        final StringBuilder id = new StringBuilder(  );
        id.append( "w" );
        id.append( counter );

        if ( providerHolder != null )
        {
            id.append( providerHolder.getIdentifier() );
        }

        return id.toString();
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
        return providerHolder.getProvider().compareStringAttribute( entryDN, attributeName, value );
    }

    @Override
    public void createEntry( final String entryDN, final String baseObjectClass, final Map<String, String> stringAttributes )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.getProvider().createEntry( entryDN, baseObjectClass, stringAttributes );
    }

    @Override
    public void createEntry( final String entryDN, final Set<String> baseObjectClasses, final Map<String, String> stringAttributes )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.getProvider().createEntry( entryDN, baseObjectClasses, stringAttributes );
    }

    @Override
    public void renameEntry(String entryDN, String newRDN, String newParentDN)
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException {
        providerHolder.getProvider().renameEntry( entryDN, newRDN, newParentDN );
    }

    @Override
    public void deleteEntry( final String entryDN )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.getProvider().deleteEntry( entryDN );
    }

    @Override
    public void deleteStringAttributeValue( final String entryDN, final String attributeName, final String value )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.getProvider().deleteStringAttributeValue( entryDN, attributeName, value );
    }

    @Override
    public ExtendedResponse extendedOperation( final ExtendedRequest request )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.getProvider().extendedOperation( request );
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
        return providerHolder.getProvider().readMultiByteAttribute( entryDN, attribute );
    }

    @Override
    public Set<String> readMultiStringAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.getProvider().readMultiStringAttribute( entryDN, attribute );
    }

    @Override
    public String readStringAttribute( final String entryDN, final String attribute )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.getProvider().readStringAttribute( entryDN, attribute );
    }

    @Override
    public Map<String, String> readStringAttributes( final String entryDN, final Set<String> attributes )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.getProvider().readStringAttributes( entryDN, attributes );
    }

    @Override
    public void replaceStringAttribute( final String entryDN, final String attributeName, final String oldValue, final String newValue )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.getProvider().replaceStringAttribute( entryDN, attributeName, oldValue, newValue );
    }

    @Override
    public Map<String, Map<String, String>> search( final String baseDN, final SearchHelper searchHelper )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.getProvider().search( baseDN, searchHelper );
    }

    @Override
    public Map<String, Map<String, String>> search( final String baseDN, final String filter, final Set<String> attributes, final SearchScope searchScope )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.getProvider().search( baseDN, filter, attributes, searchScope );
    }

    @Override
    public Map<String, Map<String, List<String>>> searchMultiValues( final String baseDN, final SearchHelper searchHelper )
            throws ChaiUnavailableException, ChaiOperationException
    {
        return providerHolder.getProvider().searchMultiValues( baseDN, searchHelper );
    }

    @Override
    public Map<String, Map<String, List<String>>> searchMultiValues( final String baseDN, final String filter, final Set<String> attributes, final SearchScope searchScope )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        return providerHolder.getProvider().searchMultiValues( baseDN, filter, attributes, searchScope );
    }

    @Override
    public void writeBinaryAttribute( final String entryDN, final String attributeName, final byte[][] values, final boolean overwrite )
            throws ChaiUnavailableException, ChaiOperationException
    {
        providerHolder.getProvider().writeBinaryAttribute( entryDN, attributeName, values, overwrite );
    }

    @Override
    public void writeBinaryAttribute( final String entryDN, final String attributeName, final byte[][] values, final boolean overwrite, final ChaiRequestControl[] controls )
            throws ChaiUnavailableException, ChaiOperationException
    {
        providerHolder.getProvider().writeBinaryAttribute( entryDN, attributeName, values, overwrite, controls );
    }

    @Override
    public void writeStringAttribute( final String entryDN, final String attributeName, final Set<String> values, final boolean overwrite )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.getProvider().writeStringAttribute( entryDN, attributeName, values, overwrite );
    }

    @Override
    public void writeStringAttributes( final String entryDN, final Map<String, String> attributeValueProps, final boolean overwrite )
            throws ChaiOperationException, ChaiUnavailableException, IllegalStateException
    {
        providerHolder.getProvider().writeStringAttributes( entryDN, attributeValueProps, overwrite );
    }

    @Override
    public DirectoryVendor getDirectoryVendor()
            throws ChaiUnavailableException
    {
        return providerHolder.getProvider().getDirectoryVendor();
    }

    @Override
    public void replaceBinaryAttribute( final String entryDN, final String attributeName, final byte[] oldValue, final byte[] newValue )
            throws ChaiUnavailableException, ChaiOperationException
    {
        providerHolder.getProvider().replaceBinaryAttribute( entryDN, attributeName, oldValue, newValue );
    }

    @Override
    public boolean isConnected()
    {
        return providerHolder.getStatus() == HolderStatus.ACTIVE;
    }

    @Override
    public ChaiProviderFactory getProviderFactory()
    {
        return chaiProviderFactory;
    }

    @Override
    public ChaiEntryFactory getEntryFactory()
    {
        return ChaiEntryFactory.newChaiFactory( this );
    }

    private final ProviderHolder providerHolder;
    private final ChaiConfiguration chaiConfiguration;
    private final ChaiProviderFactory chaiProviderFactory;
    private final Settings settings;

    public void checkStatus()
    {
        providerHolder.checkStatus();
    }

    static class Settings
    {
        private final int operationTimeout;
        private final int idleTimeout;

        private Settings( final int operationTimeout, final int idleTimeout )
        {
            this.operationTimeout = operationTimeout;
            this.idleTimeout = idleTimeout;
        }

        private int getOperationTimeoutMS()
        {
            return operationTimeout;
        }

        private int getIdleTimeoutMS()
        {
            return idleTimeout;
        }

        private static Settings fromConfig( final ChaiConfiguration chaiConfiguration )
        {
            final int operationTimeout = Integer.parseInt( chaiConfiguration.getSetting( ChaiSetting.WATCHDOG_OPERATION_TIMEOUT ) );
            final int idleTimeout = Integer.parseInt( chaiConfiguration.getSetting( ChaiSetting.WATCHDOG_IDLE_TIMEOUT ) );
            return new Settings( operationTimeout, idleTimeout );
        }
    }


    private WatchdogWrapper(
            final ChaiProviderFactory chaiProviderFactory,
            final ChaiProviderImplementor chaiProviderImplementor
    )
    {
        this.chaiConfiguration = chaiProviderImplementor.getChaiConfiguration();
        this.chaiProviderFactory = chaiProviderFactory;
        this.settings = Settings.fromConfig( chaiConfiguration );
        this.providerHolder = new ProviderHolder( chaiProviderImplementor );
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

            LOGGER.warn( errorStr );
            throw new IllegalStateException( errorStr );
        }

        if ( chaiProvider instanceof WatchdogWrapper )
        {
            LOGGER.debug( "attempt to obtain WatchdogWrapper wrapper for already wrapped Provider id=" + chaiProvider.getIdentifier() );
            return chaiProvider;
        }

        return new WatchdogWrapper( chaiProviderFactory, chaiProvider );
    }

    private enum HolderStatus
    {
        ACTIVE,
        IDLE,
        CLOSED,
    }

    private class ProviderHolder
    {
        private volatile ChaiProviderImplementor realProvider;
        private volatile boolean allowDisconnect;
        private volatile HolderStatus wdStatus = HolderStatus.ACTIVE;
        private volatile Instant lastActivity = Instant.now();
        private final ReadWriteLock statusChangeLock = new ReentrantReadWriteLock(  );

        ProviderHolder( final ChaiProviderImplementor chaiProviderImplementor )
        {
            this.realProvider = chaiProviderImplementor;
            allowDisconnect = !checkForPwExpiration( realProvider );
            chaiProviderFactory.getCentralService().getWatchdogService().registerInstance( WatchdogWrapper.this );
        }

        HolderStatus getStatus()
        {
            return wdStatus;
        }

        public String getIdentifier()
        {
            try
            {
                statusChangeLock.readLock().lock();
                if ( wdStatus != HolderStatus.CLOSED && realProvider != null )
                {
                    return "-" + realProvider.getIdentifier();
                }
                else
                {
                    return "";
                }
            }
            finally
            {
                statusChangeLock.readLock().unlock();
            }
        }

        public void close()
        {
            try
            {
                statusChangeLock.writeLock().lock();
                wdStatus = HolderStatus.CLOSED;
                if ( realProvider != null )
                {
                    realProvider.close();
                }
                realProvider = null;
                lastActivity = Instant.now();
            }
            finally
            {
                statusChangeLock.writeLock().unlock();
            }
        }

        private ChaiProviderImplementor getProvider( )
                throws ChaiUnavailableException
        {
            try
            {
                statusChangeLock.writeLock().lock();
                lastActivity = Instant.now();

                switch ( wdStatus )
                {
                    case CLOSED:
                        throw new IllegalStateException( "ChaiProvider instance has been closed" );

                    case ACTIVE:
                        return realProvider;

                    case IDLE:
                        return restoreRealProvider();

                    default:
                        throw new IllegalStateException( "unexpected internal ProviderState encountered: " + wdStatus );
                }
            }
            finally
            {
                lastActivity = Instant.now();
                statusChangeLock.writeLock().unlock();
            }
        }

        private ChaiProviderImplementor restoreRealProvider()
                throws ChaiUnavailableException
        {
            {
                final Duration duration = Duration.between( Instant.now(), lastActivity );
                final String msg = "reopening ldap connection for method="
                        + ", id="
                        + getIdentifier() + ", after "
                        + duration.toString();
                LOGGER.debug( msg );
            }

            try
            {
                realProvider = chaiProviderFactory.newProviderImpl( chaiConfiguration, true );
                wdStatus = HolderStatus.ACTIVE;
                chaiProviderFactory.getCentralService().getWatchdogService().registerInstance( WatchdogWrapper.this );

                return realProvider;
            }
            catch ( ChaiUnavailableException e )
            {
                final String msg = "error reopening ldap connection for id="
                        + getIdentifier()
                        + ", error: "
                        + e.getMessage();
                LOGGER.debug( msg );
                throw e;
            }
        }



        void checkStatus()
        {
            if ( statusChangeLock.writeLock().tryLock() )
            {
                try
                {
                    if ( wdStatus == HolderStatus.ACTIVE )
                    {
                        final Duration idleDuration = Duration.between( lastActivity, Instant.now() );
                        if ( idleDuration.toMillis() > settings.getIdleTimeoutMS() )
                        {
                            final String msg = "ldap idle timeout detected ("
                                    + idleDuration.toString()
                                    + "), closing connection id="
                                    + WatchdogWrapper.this.getIdentifier();

                            disconnectRealProvider( msg );
                        }
                    }
                }
                finally
                {
                    statusChangeLock.writeLock().unlock();
                }
            }
            else
            {
                final Duration operationDuration = Duration.between( lastActivity, Instant.now() );
                if ( operationDuration.toMillis() > settings.getOperationTimeoutMS() )
                {
                    final String msg = "ldap operation timeout detected ("
                            + operationDuration.toString()
                            + "), closing questionable connection id="
                            + WatchdogWrapper.this.getIdentifier();
                    try
                    {
                        statusChangeLock.writeLock().lock();
                        disconnectRealProvider( msg );
                    }
                    finally
                    {
                        statusChangeLock.writeLock().unlock();
                    }
                }
            }
        }

        private void disconnectRealProvider(
                final String debugMsg
        )
        {
            if ( !allowDisconnect )
            {
                return;
            }

            wdStatus = HolderStatus.IDLE;

            LOGGER.debug( debugMsg );

            if ( realProvider != null )
            {
                this.realProvider.close();
            }

            chaiProviderFactory.getCentralService().getWatchdogService().deRegisterInstance( WatchdogWrapper.this );
        }

    }

    private boolean checkForPwExpiration(
            final ChaiProviderImplementor chaiProvider
    )

    {
        final boolean doPwExpCheck = chaiProvider.getChaiConfiguration().getBooleanSetting( ChaiSetting.WATCHDOG_DISABLE_IF_PW_EXPIRED );
        if ( !doPwExpCheck )
        {
            return false;
        }

        LOGGER.trace( "checking for user password expiration to adjust watchdog timeout id=" + getIdentifier() );

        boolean userPwExpired;
        try
        {
            final String bindUserDN = chaiProvider.getChaiConfiguration().getSetting( ChaiSetting.BIND_DN );
            final ChaiUser bindUser = chaiProvider.getEntryFactory().newChaiUser( bindUserDN );
            userPwExpired = bindUser.isPasswordExpired();
        }
        catch ( ChaiException e )
        {
            LOGGER.error( "unexpected error attempting to read user password expiration value during"
                    + " watchdog initialization, will assume expiration, id="
                    + this.getIdentifier()
                    + ", error: " + e.getMessage() );
            userPwExpired = true;
        }

        if ( userPwExpired )
        {
            LOGGER.info( "connection user account password is currently expired.  Disabling watchdog timeout. id=" + this.getIdentifier() );
            return true;
        }

        return false;
    }
}
