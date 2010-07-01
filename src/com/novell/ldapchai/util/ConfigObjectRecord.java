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

import com.novell.ldapchai.ChaiEntry;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiUnavailableException;

import java.util.*;

/**
 * Represents a config object record stored in eDirectory used for holding complex data types.
 * Config object records are comprised of guids and a payload.  The payload is typically an XML document
 * but can be any data type representable as a string (and potentially even binary data).
 * <p/>
 * Config object records, or CORs are useful for storing blobs, xml documents, and even access control lists.
 * <p/>
 * CORs should be stored on a multi-valued case-ignore-string attribute.  Mutiple CORs can be written to a single attribute value.  The
 * {@code ConfigObjectRecord} class is capable of distinguishing the appropriate COR based on the supplied guids.
 * <p/>
 * The guid values are used to tie a COR on one entry to a remote entry.  For some use cases, it is useful to think
 * of a COR as a sort of advanced DN attribute.  It indicates a relationship to another entry, however it is also possible
 * to add aditional information about the remote object reference.  This information can be used to describe the conditions
 * of the relationship.
 * <p/>
 * <h4>Format</h4><pre>
 * +------+-------+-------+---------------------------+
 * | TYPE | GUID1 | GUID2 | PAYLOAD                   |
 * +------+-------+-------+---------------------------+
 * </pre><h4>Example</h4>
 * <pre>
 * 0002#{3F2504E0-4F89-11D3-9A0C-0305E82C3301}#.#&lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;config"&gt;...&lt;/config&gt;
 * </pre>
 * <p/>
 * <table border="1">
 * <tr><td>{@link #getRecordType() TYPE}</td><td>Indicates the TYPE of data resting in the payload.  By convention, a four character string integer such as "0001" is used.</tr>
 * <tr><td>{@link #getGuid1() GUID1}</td><td> Optional value for the GUID of a related object</tr>
 * <tr><td>{@link #getGuid2() GUID2}</td><td> Optional value for the GUID of a second related object</tr>
 * <tr><td>{@link #getPayload() PAYLOAD}</td><td> Payload value.  Typically in XML format, but any String is permitted.</tr>
 * </table>
 *
 * @author Jason D. Rivard
 */
public class ConfigObjectRecord {
// ----------------------------- CONSTANTS ----------------------------

    // ------------------------- PUBLIC CONSTANTS -------------------------
    public static final String EMPTY_RECORD_VALUE = ".";
    public static final String RECORD_SEPERATOR = "#";

// ------------------------------ FIELDS ------------------------------

    private String recordType;
    private String guid1;
    private String guid2;
    private String payload;

    private String attr;
    private ChaiEntry objectEntry;

// -------------------------- STATIC METHODS --------------------------

    /**
     * Create a new config object record.  This will only create a java object representing the config
     * object record.  It is up to the caller to call the updatePayload method which will actually
     * commit the record to the directory.
     *
     * @param entry      The {@code ChaiEntry} object where the config object record will be stored.
     * @param attr       The ldap attribute name to use for storage.
     * @param recordType Record type for the record.
     * @param guid1      The first associated guid value
     * @param guid2      The second associated guid value
     * @return A ConfigObjectRecordInstance
     */
    public static ConfigObjectRecord createNew(final ChaiEntry entry,
                                               final String attr,
                                               String recordType,
                                               final String guid1,
                                               final String guid2)
    {
        //Ensure the entry is not null
        if (entry == null) {
            throw new NullPointerException("entry can not be null");
        }

        //Ensure the record type is not null
        if (recordType == null) {
            throw new NullPointerException("recordType can not be null");
        }

        //Make sure the attr is not null
        if (attr == null) {
            throw new NullPointerException("attr can not be null");
        }

        // truncate recod type to 4 chars.
        if (recordType.length() > 4) {
            recordType = recordType.substring(0, 4);
        }

        final ConfigObjectRecord cor = new ConfigObjectRecord();
        cor.objectEntry = entry;
        cor.attr = attr;
        cor.recordType = recordType;

        cor.guid1 = (guid1 == null || guid1.length() < 1) ? EMPTY_RECORD_VALUE : guid1;
        cor.guid2 = (guid2 == null || guid2.length() < 1) ? EMPTY_RECORD_VALUE : guid2;

        return cor;
    }

    /**
     * Retreive matching config object records from the directory.
     *
     * @param ldapEntry  The ldapEntry object to examine.  Must be a valid entry.
     * @param attr       The attribute used to store COR's
     * @param recordType the record recordType to use, can not be null.
     * @param guid1      GUID1 value to match on, may be null if no guid1 match is desired.
     * @param guid2      GUID2 value to match on, may be null if no guid2 match is desired.
     * @return A List containing matching ConfigObjectRecords
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    public static List<ConfigObjectRecord> readRecordFromLDAP(final ChaiEntry ldapEntry, final String attr, final String recordType, final Set guid1, final Set guid2)
            throws ChaiOperationException, ChaiUnavailableException
    {
        if (ldapEntry == null) {
            throw new NullPointerException("ldapEntry can not be null");
        }

        if (attr == null) {
            throw new NullPointerException("attr can not be null");
        }

        if (recordType == null) {
            throw new NullPointerException("recordType can not be null");
        }

        //Read the attribute
        final Set<String> values = ldapEntry.readMultiStringAttribute(attr);

        final List<ConfigObjectRecord> cors = new ArrayList<ConfigObjectRecord>();

        for (final String value : values) {
            final ConfigObjectRecord loopRec = parseString(value);
            loopRec.objectEntry = ldapEntry;
            loopRec.attr = attr;
            cors.add(loopRec);

            //If it doesnt match any of the tests, then remove the record.
            if (!loopRec.getRecordType().equalsIgnoreCase(recordType)) {
                cors.remove(loopRec);
            } else if (guid1 != null && !guid1.contains(loopRec.getGuid1())) {
                cors.remove(loopRec);
            } else if (guid2 != null && !guid2.contains(loopRec.getGuid2())) {
                cors.remove(loopRec);
            }
        }

        return cors;
    }

    /**
     * Read a string value and convert to a {@code ConfigObjectRecord}.
     *
     * @param input a complete config object record string including type, guids and payload.
     * @return A COR parsed from the <i>inputString</i>
     */
    public static ConfigObjectRecord parseString(final String input)
    {
        final ConfigObjectRecord cor = new ConfigObjectRecord();
        try {
            cor.parseObjectRecord(input);
        } catch (Exception e) {
            throw new IllegalArgumentException("Data value is mailformed, invalid ConfigObjectRecord '" + input + "'");
        }
        return cor;
    }

    private void parseObjectRecord(final String inputString)
    {
        final StringTokenizer st = new StringTokenizer(inputString, ConfigObjectRecord.RECORD_SEPERATOR);

        try {
            recordType = st.nextToken();
            guid1 = st.nextToken();
            guid2 = st.nextToken();
            payload = st.nextToken("");
            if (payload.startsWith(RECORD_SEPERATOR)) {
                payload = payload.substring(RECORD_SEPERATOR.length(), payload.length());
            }
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("incomplete COR string. missing components");
        }
    }

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * No argument private constructor
     */
    private ConfigObjectRecord()
    {
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * Return the GUID1 value for this {@code ConfigObjectRecord}
     *
     * @return string value of GUID1
     */
    public final String getGuid1()
    {
        return guid1;
    }

    /**
     * Return the GUID2 value for this {@code ConfigObjectRecord}
     *
     * @return string value of GUID2
     */
    public final String getGuid2()
    {
        return guid2;
    }

    /**
     * Return the payload for this {@code ConfigObjectRecord}
     *
     * @return string value of the payload
     */
    public final String getPayload()
    {
        return payload;
    }

    /**
     * Return the record type identifier for this {@code ConfigObjectRecord}
     *
     * @return string value of the record type
     */
    public final String getRecordType()
    {
        return recordType;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Updates the payload both in the directory and in the object instance.  When updating the directory
     * this method will determine if the entry already exists (same record type and guid values) and replace it
     * or it will add a new entry if it doesnt already exist.
     *
     * @param payload A string containing the payload to store.
     * @throws ChaiOperationException   If there is an error during the operation
     * @throws ChaiUnavailableException If the directory server(s) are unavailable
     */
    public final void updatePayload(final String payload)
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.payload = payload;

        Set<String> currentValues = null;
        try {
            currentValues = objectEntry.readMultiStringAttribute(attr);
        } catch (ChaiOperationException e) {
            //no current value
        }

        //If there is already a value there, just replace it
        if (currentValues != null) {
            for (final String currentValue : currentValues) {
                final ConfigObjectRecord record = ConfigObjectRecord.parseString(currentValue);
                if (record.getRecordType().equals(recordType) && record.getGuid1().equals(guid1) && record.getGuid2().equals(guid2)) {
                    final String existingValue = record.toString();
                    final String newValue = this.toString();
                    objectEntry.replaceAttribute(attr, existingValue, newValue);
                    return;
                }
            }
        }

        //If there isnt a value, append it to the end
        objectEntry.addAttribute(attr, this.toString());
    }

    /**
     * Convert COR object to literal string value.
     *
     * @return A string representation of the COR, same as stored in ldap.
     */
    public final String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(recordType);
        sb.append(ConfigObjectRecord.RECORD_SEPERATOR);
        sb.append(guid1 != null ? guid1 : ConfigObjectRecord.EMPTY_RECORD_VALUE);
        sb.append(ConfigObjectRecord.RECORD_SEPERATOR);
        sb.append(guid2 != null ? guid2 : ConfigObjectRecord.EMPTY_RECORD_VALUE);
        sb.append(ConfigObjectRecord.RECORD_SEPERATOR);
        sb.append(payload != null ? payload : ConfigObjectRecord.EMPTY_RECORD_VALUE);

        return sb.toString();
    }
}

