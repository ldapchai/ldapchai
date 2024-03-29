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

import com.novell.ldapchai.exception.ChaiUnavailableException;

/**
 * Service Provider Interface (SPI) required for {@link ChaiProvider} implementations.  Any implementation of {@code ChaiProvider}
 * must also implement this interface as well.  Chai clients can safely ignore this interface.
 *
 * @author Jason D. Rivard
 */
public interface ChaiProviderImplementor extends ChaiProvider
{
    /**
     * Current status of the {@code ChaiProvider} instance.
     */
    enum ConnectionState
    {
        NEW, OPEN, CLOSED
    }

    /**
     * <p>Return the underlying connection object.  The implementation class must decide
     * which type of object to return, if any.</p>
     *
     * <p>Extreme care must be taken when using this menu.  The chai provider implementation
     * typically does things with the underling connection that your code will not
     * expect, as well as the reverse.  For example, Chai may invalidate the connection
     * object in cases of load balancing or fail over.</p>
     *
     * <p>For this reason, use of this method is strongly discouraged.</p>
     *
     * @return a connection-level object used by the {@code ChaiProvider} implementation.
     * @throws Exception any type of error at all, generated by the implementation
     */
    Object getConnectionObject()
            throws Exception;

    /**
     * Return the current state of the provider.
     *
     * @return current state
     */
    ConnectionState getConnectionState();

    String getCurrentConnectionURL();

    boolean errorIsRetryable( Exception e );

    /**
     * Initialize a newly created {@code ChaiProvider} instance.  The init method can be called once and only once
     * during the life of a provider.  Calling any other method before init must result in an {@code IllegalStateException},
     * and calling the init method after it has already been called will also result in an {@code IllegalStateException}.
     *
     * @param providerFactory the provider factory used to create the instance.
     * @param chaiConfig A configuration object to be used by the instance
     * @throws ChaiUnavailableException If no directory servers are reachable
     * @throws IllegalStateException    if the instance has already been initialized.
     */
    void init( ChaiConfiguration chaiConfig, ChaiProviderFactory providerFactory )
            throws ChaiUnavailableException, IllegalStateException;

    String getIdentifier();
}
