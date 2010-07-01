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

import java.util.*;

public class StringHelper {

    private StringHelper() {
    }

    /**
     * Convert a string value to a boolean.  If the value is a common positive string
     * value such as "1", "true", "y" or "yes" then TRUE is returned.  For any other
     * value or null, FALSE is returned.
     *
     * @param string value to test
     * @return true if the string resolves to a positive value.
     */
    public static boolean convertStrToBoolean(final String string)
    {
        return !(string == null || string.length() < 1) && (string.equalsIgnoreCase("true") ||
                string.equalsIgnoreCase("1") ||
                string.equalsIgnoreCase("yes") ||
                string.equalsIgnoreCase("y"));
    }

    /**
     * Convert a string to an int value.  If an error occurs during the conversion,
     * the default value is returned instead.  Unlike the {@link Integer#parseInt(String)}
     * method, this method will not throw an exception.
     *
     * @param string       value to test
     * @param defaultValue value to return in case of difficulting converting.
     * @return the int value contained in the string, otherwise the default value.
     */
    public static int convertStrToInt(final String string, final int defaultValue)
    {
        if (string == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Convert a string to a long value.  If an error occurs during the conversion,
     * the default value is returned instead.  Unlike the {@link Integer#parseInt(String)}
     * method, this method will not throw an exception.
     *
     * @param string       value to test
     * @param defaultValue value to return in case of difficulting converting.
     * @return the int value contained in the string, otherwise the default value.
     */
    public static long convertStrToLong(final String string, final long defaultValue)
    {
        if (string == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(string);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Break apart a string using a tokenizer into a {@code List} of {@code String}s.
     *
     * @param inputString a string containing zero or or more segments
     * @param seperator   seperator to use for the split, or null for the default
     * @return a {@code List} of {@code String}s.  An emtpy list is returned if <i>inputString</i> is null.
     */
    public static List<String> tokenizeString(
            final String inputString,
            final String seperator
    )
    {
        if (inputString == null || inputString.length() < 1) {
            return Collections.emptyList();
        }

        final List<String> values = new ArrayList<String>();
        values.addAll(Arrays.asList(inputString.split(seperator)));
        return Collections.unmodifiableList(values);
    }

    public static Map<String, String> tokenizeString(
            final String inputString,
            final String seperator,
            final String subSeperator
    )
    {
        if (inputString == null || inputString.length() < 1) {
            return new HashMap<String, String>();
        }

        final Map<String, String> returnProps = new LinkedHashMap<String, String>();

        final List<String> values = tokenizeString(inputString, seperator);
        for (final String loopValue : values) {
            if (loopValue != null && loopValue.length() > 0) {
                final int subSeperatorPosition = loopValue.indexOf(subSeperator);
                if (subSeperatorPosition != -1) {
                    final String key = loopValue.substring(0,subSeperatorPosition);
                    final String value = loopValue.substring(subSeperatorPosition + 1);
                    returnProps.put(key, value);
                } else {
                    returnProps.put(loopValue,"");
                }
            }
        }
        return returnProps;
    }

    public static String stringCollectionToString(final Collection<String> c, String seperator) {
        if (c == null || c.isEmpty()) {
            return "";
        }

        if (seperator == null) {
            seperator = ", ";
        }

        final StringBuilder sb = new StringBuilder();
        for (final String value : c) {
            sb.append(value);
            sb.append(seperator);
        }
        sb.delete(sb.length() - seperator.length(), sb.length());
        return sb.toString();
    }

    public static String stringMapToString(final Map<String,String> map, final String seperator) {
        if (map == null) {
            return "";
        }

        final List<String> tempList = new ArrayList<String>();
        for (final String key : map.keySet()) {
            tempList.add(key + "=" + map.get(key));
        }

        return stringCollectionToString(tempList, seperator);
    }
}
