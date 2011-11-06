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

/**
 * Contains classes for managing challenge/response values for use with forgotten passwords.  This package provides an
 * abstraction to prevent having to deal directly with the underlying directory's semantics.
 * <p/>
 * Challenge/Response data objects and factories are used to read, write and test user responses
 * for forgotten password functionality. Functionality for reading and working with challenges and
 * reading challenge set policies from the directory is also provided. LDAP Chai wraps other Novell
 * APIs to reduce the complexity of working with these data structures as much as possible.
 * <p/>
 * LDAP Chai API supports two different formats for handling user challenge/response data. Many
 * modern Novell applications and utilities that support challenge/response do so using the
 * <a href="http://www.novell.com/products/nmas/">NMAS</a> services embedded in Novell eDirectory.
 * An alternative method for managing challenge/response sets is to use a standard ldap attribute to
 * store user responses. The Chai Format does just that, using an XML blob and optionally hashing
 * response values for security. The Chai Format is also the same format used by the popular
 * <a href="http://code.google.com/p/pwm/">PWM</a> utility.
 * <p/>
 * The classes in this package perform operations based on a {@code ChaiConfiguration} setting
 * embedded in the {@code ChaiProvider} supplied during construction. Two configuration operations
 * are used: {@link com.novell.ldapchai.provider.ChaiConfiguration#getCrReadMode()} and {@link
 * com.novell.ldapchai.provider.ChaiConfiguration#getCrWriteMode()}.
 * These settings control which format will be used for reading and writing (they may be different). Multiple
 * methods (which are defined in {@link ChaiResponseStorageMode}) can be set for writing, in which case all of
 * the specified formats will be written to during a call to {@link com.novell.ldapchai.cr.ResponseSet#write()}.
 *
 * <h4>Chai Format Detail</h4>
 * The Chai Format is built using an extensible XML record.   When used by <a href="http://code.google.com/p/ldapchai/">PWM</a>,
 * the record is stored in ldap using {@link com.novell.ldapchai.util.ConfigObjectRecord} with a type of "0002" and no
 * guids. By default,
 * LDAP Chai will use the <i>carLicense</i> attribute to read and write the values, however this can be
 * overwridden by setting the {@link com.novell.ldapchai.provider.ChaiConfiguration#SETTING_CHAI_RESPONSESET_ATTR_NAME}
 * value
 * when constructing your {@code ChaiProvider}.
 * <h5>Chai Format features</h5><pre>
 * <ul>
 *   <li>XML record format allows for easy parsing and records  
 * <p/>
 * 
 *
 * <h5>Chai Format (SHA1) XML example:</h5><pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;ResponseSet minRandomRequired="0" locale="en_US"&gt;
 * 	&lt;response adminDefined="true" required="true" minLength="5" maxLength="200"&gt;
 * 		&lt;challenge&gt;&lt;![CDATA[c2]]&gt;&lt;/challenge&gt;
 * 		&lt;answer format="SHA1"&gt;&lt;![CDATA[fsEw6G4j3sbdBWt6yKmIn2oz63I=]]&gt;&lt;/answer&gt;
 * 	&lt;/response&gt;
 * 	&lt;response adminDefined="true" required="true" minLength="5" maxLength="200"&gt;
 * 		&lt;challenge&gt;&lt;![CDATA[c1]]&gt;&lt;/challenge&gt;
 * 		&lt;answer format="SHA1"&gt;&lt;![CDATA[2fm396fcv62PZUlWwQWN6pvLOhQ=]]&gt;&lt;/answer&gt;
 * 	&lt;/response&gt;
 * &lt;/ResponseSet&gt;
 * </pre>
 *
 * @author Jason D. Rivard
 */
package com.novell.ldapchai.cr;