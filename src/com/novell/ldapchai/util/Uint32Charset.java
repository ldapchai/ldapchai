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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

class Uint32Charset extends Charset {
// --------------------------- CONSTRUCTORS ---------------------------

    public Uint32Charset()
    {
        super("unit32", null);
    }

// -------------------------- OTHER METHODS --------------------------

    public boolean contains(final Charset cs)
    {
        return cs.getClass() == this.getClass();
    }

    public CharsetDecoder newDecoder()
    {
        return new Uint32CharsetDecoder(this);
    }

    public CharsetEncoder newEncoder()
    {
        throw new UnsupportedOperationException();
    }

// -------------------------- INNER CLASSES --------------------------

    static class Uint32CharsetDecoder extends CharsetDecoder {
        public Uint32CharsetDecoder(final Charset cs)
        {
            super(cs, 0.5f, 1);
        }

        protected CoderResult decodeLoop(final ByteBuffer in, final CharBuffer out)
        {
            for (int i = 0; in.hasRemaining(); i++) {
                final byte b = in.get();
                if (i % 2 == 0) {
                    out.append((char) b);
                }
            }
            return null;
        }
    }
}
