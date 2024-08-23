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

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ChaiException;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.util.internal.ChaiLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Failover provider.
 *
 * @author Jason D. Rivard
 * @see ChaiSetting#FAILOVER_ENABLE
 */
class FailOverWrapper implements InvocationHandler
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( FailOverWrapper.class );

    private final FailOverRotationMachine rotationMachine;
    private final ChaiProvider originalProvider;
    private final ChaiConfiguration originalConfiguration;

    private final FailOverSettings settings;
    private volatile boolean closed = false;

    private final Lock failOverLock = new ReentrantLock();

    static ChaiProviderImplementor forConfiguration( final ChaiProviderFactory providerFactory, final ChaiConfiguration chaiConfig )
            throws ChaiUnavailableException
    {
        final Class<?>[] interfaces = new Class[] {ChaiProviderImplementor.class};

        final Object newProxy = Proxy.newProxyInstance(
                chaiConfig.getClass().getClassLoader(),
                interfaces,
                new FailOverWrapper( providerFactory, chaiConfig ) );

        return ( ChaiProviderImplementor ) newProxy;
    }


    private FailOverWrapper( final ChaiProviderFactory chaiProviderFactory, final ChaiConfiguration chaiConfig )
            throws ChaiUnavailableException
    {
        final int settingMaxRetries = Integer.parseInt( chaiConfig.getSetting( ChaiSetting.FAILOVER_CONNECT_RETRIES ) );
        final int settingMinFailbackTime = Integer.parseInt( chaiConfig.getSetting( ChaiSetting.FAILOVER_MINIMUM_FAILBACK_TIME ) );
        this.originalConfiguration = chaiConfig;

        final ChaiProviderImplementor failOverHelper;

        failOverLock.lock();
        try
        {
            failOverHelper = ChaiProviderFactory.createConcreteProvider( chaiProviderFactory, chaiConfig, false );
        }
        catch ( Exception e )
        {
            throw new ChaiUnavailableException(
                    "unable to create a required concrete provider for the failover wrapper",
                    ChaiError.CHAI_INTERNAL_ERROR, e );
        }
        finally
        {
            failOverLock.unlock();
        }

        this.settings = new FailOverSettings( failOverHelper, settingMaxRetries, settingMinFailbackTime );

        rotationMachine = new FailOverRotationMachine( chaiProviderFactory, chaiConfig, settings );

        // call get current provider.  must be able to connect, else should not return a new instance.
        originalProvider = rotationMachine.getCurrentProvider();
    }

    @Override
    public Object invoke( final Object proxy, final Method method, final Object[] args )
            throws Throwable
    {
        if ( "close".equals( method.getName() ) )
        {
            closeThis();
            return Void.TYPE;
        }


        if ( "getChaiConfiguration".equals( method.getName() ) )
        {
            return originalConfiguration;
        }

        if ( closed )
        {
            throw new IllegalStateException( "fail-over wrapper is closed" );
        }

        final boolean isLdap = method.getAnnotation( ChaiProviderImplementor.LdapOperation.class ) != null;

        if ( isLdap )
        {
            return failableInvoke( method, args );
        }
        else
        {
            return AbstractWrapper.invoker( originalProvider, method, args );
        }
    }

    private void closeThis()
    {
        if ( closed )
        {
            return;
        }

        if ( rotationMachine != null )
        {
            rotationMachine.destoryAllConnections();
        }

        originalProvider.close();
        closed = true;
    }

    private Object failableInvoke( final Method method, final Object[] args )
            throws ChaiException
    {
        final int maxAttempts = settings.getMaxRetries();

        int attempts = 0;
        while ( attempts < maxAttempts )
        {
            // Check to make sure we haven't been closed while looping.
            if ( closed || rotationMachine == null )
            {
                LOGGER.debug( () -> "close detected while inside retry loop, throwing ChaiUnavailableException" );
                throw new ChaiUnavailableException( "FailOverWrapper closed while retrying connection", ChaiError.COMMUNICATION );
            }

            final ChaiProvider currentProvider;

            failOverLock.lock();
            try
            {
                // fetch the current active provider from the machine.  If unable to reach
                // any ldap servers, this will throw ChaiUnavailable right here.
                currentProvider = rotationMachine.getCurrentProvider();
            }
            finally
            {
                failOverLock.unlock();
            }

            try
            {
                return AbstractWrapper.invoker( currentProvider, method, args );
            }
            catch ( Exception e )
            {
                if ( settings.errorIsRetryable( e ) && !closed )
                {
                    failOverLock.lock();
                    try
                    {
                        rotationMachine.reportBrokenProvider( currentProvider, e );
                    }
                    finally
                    {
                        failOverLock.unlock();
                    }
                }
                else
                {
                    if ( e instanceof ChaiOperationException )
                    {
                        throw ( ChaiOperationException ) e;
                    }
                    else if ( e instanceof ChaiUnavailableException )
                    {
                        throw ( ChaiUnavailableException ) e;
                    }
                    else
                    {
                        throw new IllegalStateException( "unexpected chai api error: " + e.getMessage(), e );
                    }
                }
            }
            attempts++;
        }

        if ( originalConfiguration.bindURLsAsList().size() > 1 )
        {
            throw new ChaiUnavailableException( "unable to reach any configured ldap server, maximum retries exceeded", ChaiError.COMMUNICATION );
        }

        throw new ChaiUnavailableException( "unable to reach ldap server", ChaiError.COMMUNICATION );
    }
}
