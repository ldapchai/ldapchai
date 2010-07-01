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

package com.novell.ldapchai.util;

/**
 * Internal Chai API logging wrapper.  Users of Chai should ignore this class.
 *
 * @author Jason D. Rivard
 */
public class ChaiLogger {
// ------------------------------ FIELDS ------------------------------

    private final String name;
    private final org.apache.log4j.Logger logger;

// -------------------------- STATIC METHODS --------------------------

    public static ChaiLogger getLogger(final Class className)
    {
        return new ChaiLogger(className.getName());
    }

    public static ChaiLogger getLogger(final String name)
    {
        return new ChaiLogger(name);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public ChaiLogger(final String name)
    {
        this.name = name;
        logger = org.apache.log4j.Logger.getLogger(name);
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getName()
    {
        return name;
    }

// -------------------------- OTHER METHODS --------------------------

    public void debug(final Object message)
    {
        logger.debug(message);
    }

    public void debug(final Object message, final Exception exception)
    {
        logger.trace(message, exception);
    }

    public void error(final Object message)
    {
        logger.error(message);
    }

    public void error(final Object message, final Exception exception)
    {
        logger.error(message, exception);
    }

    public void fatal(final Object message)
    {
        logger.fatal(message);
    }

    public void fatal(final Object message, final Exception exception)
    {
        logger.fatal(message, exception);
    }

    public void info(final Object message)
    {
        logger.info(message);
    }

    public void info(final Object message, final Exception exception)
    {
        logger.info(message, exception);
    }

    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    public boolean isTraceEnabled()
    {
        return logger.isTraceEnabled();
    }

    public void trace(final Object message)
    {
        logger.trace(message);
    }

    public void trace(final Object message, final Exception exception)
    {
        logger.debug(message, exception);
    }

    public void warn(final Object message)
    {
        logger.warn(message);
    }

    public void warn(final Object message, final Exception exception)
    {
        logger.warn(message, exception);
    }
}
