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

package com.novell.ldapchai.impl.edir;

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.security.nmas.NMASConstants;

public class EdirErrorMap implements ErrorMap {

    public ChaiProvider.DIRECTORY_VENDOR forDirectoryVendor() {
        return ChaiProvider.DIRECTORY_VENDOR.NOVELL_EDIRECTORY;
    }

    public ChaiError errorForMessage(final String message)
    {
        return forMessage(message).chaiErrorCode;
    }

    public boolean isPermanent(final String message) {
        return forMessage(message).isPermenant();
    }

    public boolean isAuthenticationRelated(final String message) {
        return forMessage(message).isAuthentication();
    }

    private static EdirError forMessage(final String message) {
        if (message == null || message.length() < 1) {
            return EdirError.UNKNOWN;
        }

        for (final EdirError error : EdirError.values()) {
            if (message.contains(String.valueOf(error.getEdirErrorCode()))) {
                return error;
            }

            for (final String errorString : error.getErrorStrings()) {
                if (message.contains(String.valueOf(errorString))) {
                    return error;
                }
            }
        }

        return EdirError.UNKNOWN;
    }

    enum EdirError {
        // nmas errors
        PASSWORD_TOO_LONG             (NMASConstants.NMAS_E_PASSWORD_TOO_LONG,             ChaiError.PASSWORD_TOO_LONG,              true, false),
        PASSWORD_NOT_ENOUGH_NUM       (NMASConstants.NMAS_E_PASSWORD_NUMERIC_MIN,          ChaiError.PASSWORD_NOT_ENOUGH_NUM,        true, false),
        PASSWORD_NOT_ENOUGH_SPECIAL   (NMASConstants.NMAS_E_PASSWORD_SPECIAL_MIN,          ChaiError.PASSWORD_NOT_ENOUGH_SPECIAL,    true, false),
        PASSWORD_NOT_ENOUGH_LOWER     (NMASConstants.NMAS_E_PASSWORD_LOWER_MIN,            ChaiError.PASSWORD_NOT_ENOUGH_LOWER,      true, false),
        PASSWORD_NOT_ENOUGH_UPPER     (NMASConstants.NMAS_E_PASSWORD_UPPER_MIN,            ChaiError.PASSWORD_NOT_ENOUGH_UPPER,      true, false),
        PASSWORD_NOT_ENOUGH_UNIQUE    (NMASConstants.NMAS_E_PASSWORD_UNIQUE_MIN,           ChaiError.PASSWORD_NOT_ENOUGH_UNIQUE,     true, false),
        PASSWORD_TOO_MANY_REPEAT      (NMASConstants.NMAS_E_PASSWORD_REPEAT_CHAR_MAX,      ChaiError.PASSWORD_TOO_MANY_REPEAT,       true, false),
        PASSWORD_TOO_MANY_NUMERIC     (NMASConstants.NMAS_E_PASSWORD_NUMERIC_MAX,          ChaiError.PASSWORD_TOO_MANY_NUMERIC,      true, false),
        PASSWORD_TOO_MANY_LOWER       (NMASConstants.NMAS_E_PASSWORD_LOWER_MAX,            ChaiError.PASSWORD_TOO_MANY_LOWER,        true, false),
        PASSWORD_TOO_MANY_UPPER       (NMASConstants.NMAS_E_PASSWORD_UPPER_MAX,            ChaiError.PASSWORD_TOO_MANY_UPPER,        true, false),
        PASSWORD_FIRST_IS_NUMERIC     (NMASConstants.NMAS_E_PASSWORD_NUMERIC_FIRST,        ChaiError.PASSWORD_FIRST_IS_NUMERIC,      true, false),
        PASSWORD_LAST_IS_NUMERIC      (NMASConstants.NMAS_E_PASSWORD_NUMERIC_LAST,         ChaiError.PASSWORD_LAST_IS_NUMERIC,       true, false),
        PASSWORD_FIRST_IS_SPECIAL     (NMASConstants.NMAS_E_PASSWORD_SPECIAL_FIRST,        ChaiError.PASSWORD_FIRST_IS_SPECIAL,      true, false),
        PASSWORD_LAST_IS_SPECIAL      (NMASConstants.NMAS_E_PASSWORD_SPECIAL_LAST,         ChaiError.PASSWORD_LAST_IS_SPECIAL,       true, false),
        PASSWORD_TOO_MANY_SPECIAL     (NMASConstants.NMAS_E_PASSWORD_SPECIAL_MAX,          ChaiError.PASSWORD_TOO_MANY_SPECIAL,      true, false),
        PASSWORD_INVALID_CHAR         (NMASConstants.NMAS_E_PASSWORD_EXTENDED_DISALLOWED,  ChaiError.PASSWORD_INVALID_CHAR,          true, false),
        PASSWORD_INWORDLIST           (NMASConstants.NMAS_E_PASSWORD_EXCLUDE,              ChaiError.PASSWORD_INWORDLIST,            true, false),
        PASSWORD_SAMEASATTR           (NMASConstants.NMAS_E_PASSWORD_ATTR_VALUE,           ChaiError.PASSWORD_SAMEASATTR,            true, false),
        PASSWORD_HISTORY_FULL         (NMASConstants.NMAS_E_PASSWORD_HISTORY_FULL,         ChaiError.PASSWORD_HISTORY_FULL,          true, false),
        PASSWORD_NUMERIC_DISALLOWED   (NMASConstants.NMAS_E_PASSWORD_NUMERIC_DISALLOWED,   ChaiError.PASSWORD_NUMERIC_DISALLOWED,    true, false),
        PASSWORD_SPECIAL_DISALLOWED   (NMASConstants.NMAS_E_PASSWORD_SPECIAL_DISALLOWED,   ChaiError.PASSWORD_SPECIAL_DISALLOWED,    true, false),
        PASSWORD_TOO_SOON             (NMASConstants.NMAS_E_PASSWORD_LIFE_MIN,             ChaiError.PASSWORD_TOO_SOON,              true, false),

        BUFFER_TOO_SMALL                               (-119, ChaiError.UNKNOWN,                       true, false),
        VOLUME_FLAG_NOT_SET                            (-120, ChaiError.UNKNOWN,                       true, false),
        NO_ITEMS_FOUND                                 (-121, ChaiError.UNKNOWN,                       true, false),
        CONN_ALREADY_TEMPORARY                         (-122, ChaiError.UNKNOWN,                       true, false),
        CONN_ALREADY_LOGGED_IN                         (-123, ChaiError.UNKNOWN,                       true, false),
        CONN_NOT_AUTHENTICATED                         (-124, ChaiError.UNKNOWN,                       true, false),
        CONN_NOT_LOGGED_IN                             (-125, ChaiError.UNKNOWN,                       true, false),
        NCP_BOUNDARY_CHECK_FAILED                      (-126, ChaiError.UNKNOWN,                       true, false),
        LOCK_WAITING                                   (-127, ChaiError.UNKNOWN,                       true, false),
        LOCK_FAIL                                      (-128, ChaiError.UNKNOWN,                       true, false),
        OUT_OF_HANDLES                                 (-129, ChaiError.UNKNOWN,                       true, false),
        NO_OPEN_PRIVILEGE                              (-130, ChaiError.UNKNOWN,                       true, false),
        HARD_IO_ERROR                                  (-131, ChaiError.UNKNOWN,                       true, false),
        NO_CREATE_PRIVILEGE                            (-132, ChaiError.UNKNOWN,                       true, false),
        NO_CREATE_DELETE_PRIV                          (-133, ChaiError.UNKNOWN,                       true, false),
        R_O_CREATE_FILE                                (-134, ChaiError.UNKNOWN,                       true, false),
        CREATE_FILE_INVALID_NAME                       (-135, ChaiError.UNKNOWN,                       true, false),
        INVALID_FILE_HANDLE                            (-136, ChaiError.UNKNOWN,                       true, false),
        NO_SEARCH_PRIVILEGE                            (-137, ChaiError.UNKNOWN,                       true, false),
        NO_DELETE_PRIVILEGE                            (-138, ChaiError.UNKNOWN,                       true, false),
        NO_RENAME_PRIVILEGE                            (-139, ChaiError.UNKNOWN,                       true, false),
        NO_SET_PRIVILEGE                               (-140, ChaiError.UNKNOWN,                       true, false),
        SOME_FILES_IN_USE                              (-141, ChaiError.UNKNOWN,                       true, false),
        ALL_FILES_IN_USE                               (-142, ChaiError.UNKNOWN,                       true, false),
        SOME_READ_ONLY                                 (-143, ChaiError.UNKNOWN,                       true, false),
        ALL_READ_ONLY                                  (-144, ChaiError.UNKNOWN,                       true, false),
        SOME_NAMES_EXIST                               (-145, ChaiError.UNKNOWN,                       true, false),
        ALL_NAMES_EXIST                                (-146, ChaiError.UNKNOWN,                       true, false),
        NO_READ_PRIVILEGE                              (-147, ChaiError.UNKNOWN,                       true, false),
        NO_WRITE_PRIVILEGE                             (-148, ChaiError.UNKNOWN,                       true, false),
        FILE_DETACHED                                  (-149, ChaiError.UNKNOWN,                       true, false),
        INSUFFICIENT_MEMORY                            (-150, ChaiError.UNKNOWN,                       true, false),
        NO_ALLOC_SPACE                                 (-150, ChaiError.UNKNOWN,                       true, false),
        TARGET_NOT_A_SUBDIR                            (-150, ChaiError.UNKNOWN,                       true, false),
        NO_SPOOL_SPACE                                 (-151, ChaiError.UNKNOWN,                       true, false),
        INVALID_VOLUME                                 (-152, ChaiError.UNKNOWN,                       true, false),
        DIRECTORY_FULL                                 (-153, ChaiError.UNKNOWN,                       true, false),
        RENAME_ACROSS_VOLUME                           (-154, ChaiError.UNKNOWN,                       true, false),
        BAD_DIR_HANDLE                                 (-155, ChaiError.UNKNOWN,                       true, false),
        INVALID_PATH                                   (-156, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_EXTENSION                              (-156, ChaiError.UNKNOWN,                       true, false),
        NO_DIR_HANDLES                                 (-157, ChaiError.UNKNOWN,                       true, false),
        BAD_FILE_NAME                                  (-158, ChaiError.UNKNOWN,                       true, false),
        DIRECTORY_ACTIVE                               (-159, ChaiError.UNKNOWN,                       true, false),
        DIRECTORY_NOT_EMPTY                            (-160, ChaiError.UNKNOWN,                       true, false),
        DIRECTORY_IO_ERROR                             (-161, ChaiError.UNKNOWN,                       true, false),
        IO_LOCKED                                      (-162, ChaiError.UNKNOWN,                       true, false),
        TRANSACTION_RESTARTED                          (-163, ChaiError.UNKNOWN,                       true, false),
        RENAME_DIR_INVALID                             (-164, ChaiError.UNKNOWN,                       true, false),
        INVALID_OPENCREATE_MODE                        (-165, ChaiError.UNKNOWN,                       true, false),
        ALREADY_IN_USE                                 (-166, ChaiError.UNKNOWN,                       true, false),
        INVALID_RESOURCE_TAG                           (-167, ChaiError.UNKNOWN,                       true, false),
        ACCESS_DENIED                                  (-168, ChaiError.UNKNOWN,                       true, false),
        DSERR_LOGIN_SIGNING_REQUIRED                   (-188, ChaiError.UNKNOWN,                       true, false),
        DSERR_LOGIN_ENCRYPT_REQUIRED                   (-189, ChaiError.UNKNOWN,                       true, false),
        INVALID_DATA_STREAM                            (-190, ChaiError.UNKNOWN,                       true, false),
        INVALID_NAME_SPACE                             (-191, ChaiError.UNKNOWN,                       true, false),
        NO_ACCOUNTING_PRIVILEGES                       (-192, ChaiError.UNKNOWN,                       true, false),
        NO_ACCOUNT_BALANCE                             (-193, ChaiError.UNKNOWN,                       true, false),
        CREDIT_LIMIT_EXCEEDED                          (-194, ChaiError.UNKNOWN,                       true, false),
        TOO_MANY_HOLDS                                 (-195, ChaiError.UNKNOWN,                       true, false),
        ACCOUNTING_DISABLED                            (-196, ChaiError.UNKNOWN,                       true, false),
        LOGIN_LOCKOUT                                  (-197, ChaiError.INTRUDER_LOCKOUT,               true, true),
        NO_CONSOLE_RIGHTS                              (-198, ChaiError.UNKNOWN,                       true, false),
        Q_IO_FAILURE                                   (-208, ChaiError.UNKNOWN,                       true, false),
        NO_QUEUE                                       (-209, ChaiError.UNKNOWN,                       true, false),
        NO_Q_SERVER                                    (-210, ChaiError.UNKNOWN,                       true, false),
        NO_Q_RIGHTS                                    (-211, ChaiError.UNKNOWN,                       true, false),
        Q_FULL                                         (-212, ChaiError.UNKNOWN,                       true, false),
        NO_Q_JOB                                       (-213, ChaiError.UNKNOWN,                       true, false),
        NO_Q_JOB_RIGHTS                                (-214, ChaiError.UNKNOWN,                       true, false),
        UNENCRYPTED_NOT_ALLOWED                        (-214, ChaiError.UNKNOWN,                       true, false),
        DUPLICATE_PASSWORD                             (-215, ChaiError.PASSWORD_PREVIOUSLYUSED,       true, false),
        Q_IN_SERVICE                                   (-215, ChaiError.UNKNOWN,                       true, false),
        Q_NOT_ACTIVE                                   (-216, ChaiError.UNKNOWN,                       true, false),
        PASSWORD_TOO_SHORT                             (-216, ChaiError.PASSWORD_TOO_SHORT,            true, false),
        MAXIMUM_LOGINS_EXCEEDED                        (-217, ChaiError.UNKNOWN,                       true, false),
        Q_STN_NOT_SERVER                               (-217, ChaiError.UNKNOWN,                       true, false),
        BAD_LOGIN_TIME                                 (-218, ChaiError.UNKNOWN,                       true, false),
        Q_HALTED                                       (-218, ChaiError.UNKNOWN,                       true, false),
        NODE_ADDRESS_VIOLATION                         (-219, ChaiError.UNKNOWN,                       true, false),
        Q_MAX_SERVERS                                  (-219, ChaiError.UNKNOWN,                       true, false),
        LOG_ACCOUNT_EXPIRED                            (-220, ChaiError.ACCOUNT_EXPIRED,               true, true),
        BAD_PASSWORD                                   (-222, ChaiError.PASSWORD_BADPASSWORD,          true, true),
        PASSWORD_EXPIRED                               (-223, ChaiError.PASSWORD_EXPIRED,              true, true),
        NO_LOGIN_CONN_AVAILABLE                        (-224, ChaiError.UNKNOWN,                       true, false),
        WRITE_TO_GROUP_PROPERTY                        (-232, ChaiError.UNKNOWN,                       true, false),
        MEMBER_ALREADY_EXISTS                          (-233, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_MEMBER                                 (-234, ChaiError.UNKNOWN,                       true, false),
        PROPERTY_NOT_GROUP                             (-235, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_VALUE_SET                              (-236, ChaiError.UNKNOWN,                       true, false),
        PROPERTY_ALREADY_EXISTS                        (-237, ChaiError.UNKNOWN,                       true, false),
        OBJECT_ALREADY_EXISTS                          (-238, ChaiError.UNKNOWN,                       true, false),
        ILLEGAL_NAME                                   (-239, ChaiError.UNKNOWN,                       true, false),
        ILLEGAL_WILDCARD                               (-240, ChaiError.UNKNOWN,                       true, false),
        BINDERY_SECURITY                               (-241, ChaiError.UNKNOWN,                       true, false),
        NO_OBJECT_READ_RIGHTS                          (-242, ChaiError.UNKNOWN,                       true, false),
        NO_OBJECT_RENAME_RIGHTS                        (-243, ChaiError.UNKNOWN,                       true, false),
        NO_OBJECT_DELETE_RIGHTS                        (-244, ChaiError.UNKNOWN,                       true, false),
        NO_OBJECT_CREATE_RIGHTS                        (-245, ChaiError.UNKNOWN,                       true, false),
        NO_PROPERTY_DELETE_RIGHTS                      (-246, ChaiError.UNKNOWN,                       true, false),
        NO_PROPERTY_CREATE_RIGHTS                      (-247, ChaiError.UNKNOWN,                       true, false),
        NO_PROPERTY_WRITE_RIGHTS                       (-248, ChaiError.UNKNOWN,                       true, false),
        NO_PROPERTY_READ_RIGHTS                        (-249, ChaiError.UNKNOWN,                       true, false),
        TEMP_REMAP                                     (-250, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_PROPERTY                               (-251, ChaiError.UNKNOWN,                       true, false),
        UNKNOWN_REQUEST                                (-251, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_OBJECT                                 (-252, ChaiError.NO_SUCH_ENTRY,                 true, false),
        MESSAGE_QUEUE_FULL                             (-252, ChaiError.UNKNOWN,                       true, false),
        TARGET_ALREADY_HAS_MSG                         (-252, ChaiError.UNKNOWN,                       true, false),
        BAD_STATION_NUMBER                             (-253, ChaiError.UNKNOWN,                       true, false),
        BINDERY_LOCKED                                 (-254, ChaiError.UNKNOWN,                       true, false),
        DIR_LOCKED                                     (-254, ChaiError.UNKNOWN,                       true, false),
        DSERR_TIMEOUT                                  (-254, ChaiError.UNKNOWN,                       true, false),
        LOGIN_DISABLED                                 (-254, ChaiError.ACCOUNT_DISABLED,              true, true),
        SPOOL_DELETE                                   (-254, ChaiError.UNKNOWN,                       true, false),
        TRUSTEE_NOT_FOUND                              (-254, ChaiError.UNKNOWN,                       true, false),
        BAD_PARAMETER                                  (-255, ChaiError.UNKNOWN,                       true, false),
        BAD_SPOOL_PRINTER                              (-255, ChaiError.UNKNOWN,                       true, false),
        CLOSE_FCB                                      (-255, ChaiError.UNKNOWN,                       true, false),
        ERR_OF_SOME_SORT                               (-255, ChaiError.UNKNOWN,                       true, false),
        FILE_EXISTS                                    (-255, ChaiError.UNKNOWN,                       true, false),
        FILE_NAME                                      (-255, ChaiError.UNKNOWN,                       true, false),
        HARD_FAILURE                                   (-255, ChaiError.UNKNOWN,                       true, false),
        IO_BOUND                                       (-255, ChaiError.UNKNOWN,                       true, false),
        MUST_FORCE_DOWN                                (-255, ChaiError.UNKNOWN,                       true, false),
        NO_FILES_FOUND                                 (-255, ChaiError.UNKNOWN,                       true, false),
        NO_SPOOL_FILE                                  (-255, ChaiError.UNKNOWN,                       true, false),
        NO_TRUSTEE_CHANGE_PRIV                         (-255, ChaiError.UNKNOWN,                       true, false),
        TARGET_NOT_ACCEPTING_MSGS                      (-255, ChaiError.UNKNOWN,                       true, false),
        TARGET_NOT_LOGGED_IN                           (-255, ChaiError.UNKNOWN,                       true, false),
        PREEMPT_COMM                                   (-286, ChaiError.UNKNOWN,                       true, false),
        CLOSE_COMM                                     (-287, ChaiError.UNKNOWN,                       true, false),
        OPEN_COMM                                      (-288, ChaiError.UNKNOWN,                       true, false),
        ALREADY_EXISTS                                 (-289, ChaiError.UNKNOWN,                       true, false),
        INVALID_COUNT                                  (-290, ChaiError.UNKNOWN,                       true, false),
        TIMEOUT                                        (-291, ChaiError.UNKNOWN,                       true, false),
        FATAL                                          (-292, ChaiError.UNKNOWN,                       true, false),
        MEMORY_ERR                                     (-293, ChaiError.UNKNOWN,                       true, false),
        VRDRIVER_INTERFACE_MISMATCH                    (-294, ChaiError.UNKNOWN,                       true, false),
        VRDRIVER_CREATE_FAIL                           (-295, ChaiError.UNKNOWN,                       true, false),
        VRDRIVER_MISSING                               (-296, ChaiError.UNKNOWN,                       true, false),
        JVM_CREATE_FAIL                                (-297, ChaiError.UNKNOWN,                       true, false),
        JVM_INIT_FAIL                                  (-298, ChaiError.UNKNOWN,                       true, false),
        JRE_LOAD_FAIL                                  (-299, ChaiError.UNKNOWN,                       true, false),
        NO_JRE                                         (-300, ChaiError.UNKNOWN,                       true, false),
        NOT_ENOUGH_MEMORY                              (-301, ChaiError.UNKNOWN,                       true, false),
        BAD_KEY                                        (-302, ChaiError.UNKNOWN,                       true, false),
        BAD_CONTEXT                                    (-303, ChaiError.UNKNOWN,                       true, false),
        BUFFER_FULL                                    (-304, ChaiError.UNKNOWN,                       true, false),
        LIST_EMPTY                                     (-305, ChaiError.UNKNOWN,                       true, false),
        BAD_SYNTAX                                     (-306, ChaiError.UNKNOWN,                       true, false),
        BUFFER_EMPTY                                   (-307, ChaiError.UNKNOWN,                       true, false),
        BAD_VERB                                       (-308, ChaiError.UNKNOWN,                       true, false),
        EXPECTED_IDENTIFIER                            (-309, ChaiError.UNKNOWN,                       true, false),
        EXPECTED_EQUALS                                (-310, ChaiError.UNKNOWN,                       true, false),
        ATTR_TYPE_EXPECTED                             (-311, ChaiError.UNKNOWN,                       true, false),
        ATTR_TYPE_NOT_EXPECTED                         (-312, ChaiError.UNKNOWN,                       true, false),
        FILTER_TREE_EMPTY                              (-313, ChaiError.UNKNOWN,                       true, false),
        INVALID_OBJECT_NAME                            (-314, ChaiError.UNKNOWN,                       true, false),
        EXPECTED_RDN_DELIMITER                         (-315, ChaiError.UNKNOWN,                       true, false),
        TOO_MANY_TOKENS                                (-316, ChaiError.UNKNOWN,                       true, false),
        INCONSISTENT_MULTIAVA                          (-317, ChaiError.UNKNOWN,                       true, false),
        COUNTRY_NAME_TOO_LONG                          (-318, ChaiError.UNKNOWN,                       true, false),
        SYSTEM_ERROR                                   (-319, ChaiError.UNKNOWN,                       true, false),
        CANT_ADD_ROOT                                  (-320, ChaiError.UNKNOWN,                       true, false),
        UNABLE_TO_ATTACH                               (-321, ChaiError.UNKNOWN,                       true, false),
        INVALID_HANDLE                                 (-322, ChaiError.UNKNOWN,                       true, false),
        BUFFER_ZERO_LENGTH                             (-323, ChaiError.UNKNOWN,                       true, false),
        INVALID_REPLICA_TYPE                           (-324, ChaiError.UNKNOWN,                       true, false),
        INVALID_ATTR_SYNTAX                            (-325, ChaiError.UNKNOWN,                       true, false),
        INVALID_FILTER_SYNTAX                          (-326, ChaiError.UNKNOWN,                       true, false),
        CONTEXT_CREATION                               (-328, ChaiError.UNKNOWN,                       true, false),
        INVALID_UNION_TAG                              (-329, ChaiError.UNKNOWN,                       true, false),
        INVALID_SERVER_RESPONSE                        (-330, ChaiError.UNKNOWN,                       true, false),
        NULL_POINTER                                   (-331, ChaiError.UNKNOWN,                       true, false),
        NO_SERVER_FOUND                                (-332, ChaiError.UNKNOWN,                       true, false),
        NO_CONNECTION                                  (-333, ChaiError.UNKNOWN,                       true, false),
        RDN_TOO_LONG                                   (-334, ChaiError.UNKNOWN,                       true, false),
        DUPLICATE_TYPE                                 (-335, ChaiError.UNKNOWN,                       true, false),
        DATA_STORE_FAILURE                             (-336, ChaiError.UNKNOWN,                       true, false),
        NOT_LOGGED_IN                                  (-337, ChaiError.UNKNOWN,                       true, false),
        INVALID_PASSWORD_CHARS                         (-338, ChaiError.UNKNOWN,                       true, false),
        FAILED_SERVER_AUTHENT                          (-339, ChaiError.UNKNOWN,                       true, false),
        TRANSPORT                                      (-340, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_SYNTAX                                 (-341, ChaiError.UNKNOWN,                       true, false),
        INVALID_DS_NAME                                (-342, ChaiError.UNKNOWN,                       true, false),
        ATTR_NAME_TOO_LONG                             (-343, ChaiError.UNKNOWN,                       true, false),
        INVALID_TDS                                    (-344, ChaiError.UNKNOWN,                       true, false),
        INVALID_DS_VERSION                             (-345, ChaiError.UNKNOWN,                       true, false),
        UNICODE_TRANSLATION                            (-346, ChaiError.UNKNOWN,                       true, false),
        SCHEMA_NAME_TOO_LONG                           (-347, ChaiError.UNKNOWN,                       true, false),
        UNICODE_FILE_NOT_FOUND                         (-348, ChaiError.UNKNOWN,                       true, false),
        UNICODE_ALREADY_LOADED                         (-349, ChaiError.UNKNOWN,                       true, false),
        NOT_CONTEXT_OWNER                              (-350, ChaiError.UNKNOWN,                       true, false),
        ATTEMPT_TO_AUTHENTICATE_0                      (-351, ChaiError.UNKNOWN,                       true, false),
        NO_WRITABLE_REPLICAS                           (-352, ChaiError.UNKNOWN,                       true, false),
        DN_TOO_LONG                                    (-353, ChaiError.UNKNOWN,                       true, false),
        RENAME_NOT_ALLOWED                             (-354, ChaiError.UNKNOWN,                       true, false),
        ERR_NOT_NDS_FOR_NT                             (-355, ChaiError.UNKNOWN,                       true, false),
        ERR_NDS_FOR_NT_NO_DOMAIN                       (-356, ChaiError.UNKNOWN,                       true, false),
        ERR_NDS_FOR_NT_SYNC_DISABLED                   (-357, ChaiError.UNKNOWN,                       true, false),
        ERR_ITR_INVALID_HANDLE                         (-358, ChaiError.UNKNOWN,                       true, false),
        ERR_ITR_INVALID_POSITION                       (-359, ChaiError.UNKNOWN,                       true, false),
        ERR_ITR_INVALID_SEARCH_DATA                    (-360, ChaiError.UNKNOWN,                       true, false),
        ERR_ITR_INVALID_SCOPE                          (-361, ChaiError.UNKNOWN,                       true, false),
        ERR_ITR_MAX_COUNT                              (-362, ChaiError.UNKNOWN,                       true, false),
        UNI_ALREADY_LOADED                             (-489, ChaiError.UNKNOWN,                       true, false),
        UNI_FUTURE_OPCODE                              (-490, ChaiError.UNKNOWN,                       true, false),
        UNI_NO_SUCH_FILE                               (-491, ChaiError.UNKNOWN,                       true, false),
        UNI_TOO_MANY_FILES                             (-492, ChaiError.UNKNOWN,                       true, false),
        UNI_NO_PERMISSION                              (-493, ChaiError.UNKNOWN,                       true, false),
        UNI_NO_MEMORY                                  (-494, ChaiError.UNKNOWN,                       true, false),
        UNI_LOAD_FAILED                                (-495, ChaiError.UNKNOWN,                       true, false),
        UNI_HANDLE_BAD                                 (-496, ChaiError.UNKNOWN,                       true, false),
        UNI_HANDLE_MISMATCH                            (-497, ChaiError.UNKNOWN,                       true, false),
        UNI_RULES_CORRUPT                              (-498, ChaiError.UNKNOWN,                       true, false),
        UNI_NO_DEFAULT                                 (-499, ChaiError.UNKNOWN,                       true, false),
        UNI_INSUFFICIENT_BUFFER                        (-500, ChaiError.UNKNOWN,                       true, false),
        UNI_OPEN_FAILED                                (-501, ChaiError.UNKNOWN,                       true, false),
        UNI_NO_LOAD_DIR                                (-502, ChaiError.UNKNOWN,                       true, false),
        UNI_BAD_FILE_HANDLE                            (-503, ChaiError.UNKNOWN,                       true, false),
        UNI_READ_FAILED                                (-504, ChaiError.UNKNOWN,                       true, false),
        UNI_TRANS_CORRUPT                              (-505, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_ENTRY                                  (-601, ChaiError.NO_SUCH_ENTRY,                 true, false),
        NO_SUCH_VALUE                                  (-602, ChaiError.NO_SUCH_VALUE,                 true, false),
        NO_SUCH_ATTRIBUTE                              (-603, ChaiError.NO_SUCH_ATTRIBUTE,             true, false),
        NO_SUCH_CLASS                                  (-604, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_PARTITION                              (-605, ChaiError.UNKNOWN,                       true, false),
        ENTRY_ALREADY_EXISTS                           (-606, ChaiError.UNKNOWN,                       true, false),
        NOT_EFFECTIVE_CLASS                            (-607, ChaiError.UNKNOWN,                       true, false),
        ILLEGAL_ATTRIBUTE                              (-608, ChaiError.UNKNOWN,                       true, false),
        MISSING_MANDATORY                              (-609, ChaiError.UNKNOWN,                       true, false),
        ILLEGAL_DS_NAME                                (-610, ChaiError.UNKNOWN,                       true, false),
        ILLEGAL_CONTAINMENT                            (-611, ChaiError.UNKNOWN,                       true, false),
        CANT_HAVE_MULTIPLE_VALUES                      (-612, ChaiError.UNKNOWN,                       true, false),
        SYNTAX_VIOLATION                               (-613, ChaiError.UNKNOWN,                       true, false),
        DUPLICATE_VALUE                                (-614, ChaiError.UNKNOWN,                       true, false),
        ATTRIBUTE_ALREADY_EXISTS                       (-615, ChaiError.UNKNOWN,                       true, false),
        MAXIMUM_ENTRIES_EXIST                          (-616, ChaiError.UNKNOWN,                       true, false),
        DATABASE_FORMAT                                (-617, ChaiError.UNKNOWN,                       true, false),
        INCONSISTENT_DATABASE                          (-618, ChaiError.UNKNOWN,                       true, false),
        INVALID_COMPARISON                             (-619, ChaiError.UNKNOWN,                       true, false),
        COMPARISON_FAILED                              (-620, ChaiError.UNKNOWN,                       true, false),
        TRANSACTIONS_DISABLED                          (-621, ChaiError.UNKNOWN,                       true, false),
        INVALID_TRANSPORT                              (-622, ChaiError.UNKNOWN,                       true, false),
        SYNTAX_INVALID_IN_NAME                         (-623, ChaiError.UNKNOWN,                       true, false),
        REPLICA_ALREADY_EXISTS                         (-624, ChaiError.UNKNOWN,                       true, false),
        TRANSPORT_FAILURE                              (-625, ChaiError.UNKNOWN,                       true, false),
        ALL_REFERRALS_FAILED                           (-626, ChaiError.UNKNOWN,                       true, false),
        CANT_REMOVE_NAMING_VALUE                       (-627, ChaiError.UNKNOWN,                       true, false),
        OBJECT_CLASS_VIOLATION                         (-628, ChaiError.UNKNOWN,                       true, false),
        ENTRY_IS_NOT_LEAF                              (-629, ChaiError.UNKNOWN,                       true, false),
        DIFFERENT_TREE                                 (-630, ChaiError.UNKNOWN,                       true, false),
        ILLEGAL_REPLICA_TYPE                           (-631, ChaiError.UNKNOWN,                       true, false),
        SYSTEM_FAILURE                                 (-632, ChaiError.UNKNOWN,                       true, false),
        INVALID_ENTRY_FOR_ROOT                         (-633, ChaiError.UNKNOWN,                       true, false),
        NO_REFERRALS                                   (-634, ChaiError.UNKNOWN,                       true, false),
        REMOTE_FAILURE                                 (-635, ChaiError.UNKNOWN,                       true, false),
        UNREACHABLE_SERVER                             (-636, ChaiError.UNKNOWN,                       true, false),
        PREVIOUS_MOVE_IN_PROGRESS                      (-637, ChaiError.UNKNOWN,                       true, false),
        NO_CHARACTER_MAPPING                           (-638, ChaiError.UNKNOWN,                       true, false),
        INCOMPLETE_AUTHENTICATION                      (-639, ChaiError.UNKNOWN,                       true, false),
        INVALID_CERTIFICATE                            (-640, ChaiError.UNKNOWN,                       true, false),
        INVALID_REQUEST                                (-641, ChaiError.UNKNOWN,                       true, false),
        INVALID_ITERATION                              (-642, ChaiError.UNKNOWN,                       true, false),
        SCHEMA_IS_NONREMOVABLE                         (-643, ChaiError.UNKNOWN,                       true, false),
        SCHEMA_IS_IN_USE                               (-644, ChaiError.UNKNOWN,                       true, false),
        CLASS_ALREADY_EXISTS                           (-645, ChaiError.UNKNOWN,                       true, false),
        BAD_NAMING_ATTRIBUTES                          (-646, ChaiError.UNKNOWN,                       true, false),
        NOT_ROOT_PARTITION                             (-647, ChaiError.UNKNOWN,                       true, false),
        INSUFFICIENT_STACK                             (-648, ChaiError.UNKNOWN,                       true, false),
        INSUFFICIENT_BUFFER                            (-649, ChaiError.UNKNOWN,                       true, false),
        AMBIGUOUS_CONTAINMENT                          (-650, ChaiError.UNKNOWN,                       true, false),
        AMBIGUOUS_NAMING                               (-651, ChaiError.UNKNOWN,                       true, false),
        DUPLICATE_MANDATORY                            (-652, ChaiError.UNKNOWN,                       true, false),
        DUPLICATE_OPTIONAL                             (-653, ChaiError.UNKNOWN,                       true, false),
        PARTITION_BUSY                                 (-654, ChaiError.UNKNOWN,                       true, false),
        MULTIPLE_REPLICAS                              (-655, ChaiError.UNKNOWN,                       true, false),
        CRUCIAL_REPLICA                                (-656, ChaiError.UNKNOWN,                       true, false),
        SCHEMA_SYNC_IN_PROGRESS                        (-657, ChaiError.UNKNOWN,                       true, false),
        SKULK_IN_PROGRESS                              (-658, ChaiError.UNKNOWN,                       true, false),
        TIME_NOT_SYNCHRONIZED                          (-659, ChaiError.UNKNOWN,                       true, false),
        RECORD_IN_USE                                  (-660, ChaiError.UNKNOWN,                       true, false),
        DS_VOLUME_NOT_MOUNTED                          (-661, ChaiError.UNKNOWN,                       true, false),
        DS_VOLUME_IO_FAILURE                           (-662, ChaiError.UNKNOWN,                       true, false),
        DS_LOCKED                                      (-663, ChaiError.UNKNOWN,                       false, false),
        OLD_EPOCH                                      (-664, ChaiError.UNKNOWN,                       true, false),
        NEW_EPOCH                                      (-665, ChaiError.UNKNOWN,                       true, false),
        INCOMPATIBLE_DS_VERSION                        (-666, ChaiError.UNKNOWN,                       true, false),
        PARTITION_ROOT                                 (-667, ChaiError.UNKNOWN,                       true, false),
        ENTRY_NOT_CONTAINER                            (-668, ChaiError.UNKNOWN,                       true, false),
        FAILED_AUTHENTICATION                          (-669, ChaiError.FAILED_AUTHENTICATION,         true, true),
        INVALID_CONTEXT                                (-670, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_PARENT                                 (-671, ChaiError.UNKNOWN,                       true, false),
        NO_ACCESS                                      (-672, ChaiError.NO_ACCESS,                     true, false),
        REPLICA_NOT_ON                                 (-673, ChaiError.UNKNOWN,                       true, false),
        INVALID_NAME_SERVICE                           (-674, ChaiError.UNKNOWN,                       true, false),
        INVALID_TASK                                   (-675, ChaiError.UNKNOWN,                       true, false),
        INVALID_CONN_HANDLE                            (-676, ChaiError.UNKNOWN,                       true, false),
        INVALID_IDENTITY                               (-677, ChaiError.UNKNOWN,                       true, false),
        DUPLICATE_ACL                                  (-678, ChaiError.UNKNOWN,                       true, false),
        PARTITION_ALREADY_EXISTS                       (-679, ChaiError.UNKNOWN,                       true, false),
        TRANSPORT_MODIFIED                             (-680, ChaiError.UNKNOWN,                       true, false),
        ALIAS_OF_AN_ALIAS                              (-681, ChaiError.UNKNOWN,                       true, false),
        AUDITING_FAILED                                (-682, ChaiError.UNKNOWN,                       true, false),
        INVALID_API_VERSION                            (-683, ChaiError.UNKNOWN,                       true, false),
        SECURE_NCP_VIOLATION                           (-684, ChaiError.UNKNOWN,                       true, false),
        MOVE_IN_PROGRESS                               (-685, ChaiError.UNKNOWN,                       true, false),
        NOT_LEAF_PARTITION                             (-686, ChaiError.UNKNOWN,                       true, false),
        CANNOT_ABORT                                   (-687, ChaiError.UNKNOWN,                       true, false),
        CACHE_OVERFLOW                                 (-688, ChaiError.UNKNOWN,                       true, false),
        INVALID_SUBORDINATE_COUNT                      (-689, ChaiError.UNKNOWN,                       true, false),
        INVALID_RDN                                    (-690, ChaiError.UNKNOWN,                       true, false),
        MOD_TIME_NOT_CURRENT                           (-691, ChaiError.UNKNOWN,                       true, false),
        INCORRECT_BASE_CLASS                           (-692, ChaiError.UNKNOWN,                       true, false),
        MISSING_REFERENCE                              (-693, ChaiError.UNKNOWN,                       true, false),
        LOST_ENTRY                                     (-694, ChaiError.UNKNOWN,                       true, false),
        AGENT_ALREADY_REGISTERED                       (-695, ChaiError.UNKNOWN,                       true, false),
        DS_LOADER_BUSY                                 (-696, ChaiError.UNKNOWN,                       true, false),
        DS_CANNOT_RELOAD                               (-697, ChaiError.UNKNOWN,                       true, false),
        REPLICA_IN_SKULK                               (-698, ChaiError.UNKNOWN,                       true, false),
        FATAL_ERR                                      (-699, ChaiError.UNKNOWN,                       true, false),
        OBSOLETE_API                                   (-700, ChaiError.UNKNOWN,                       true, false),
        SYNCHRONIZATION_DISABLED                       (-701, ChaiError.UNKNOWN,                       true, false),
        INVALID_PARAMETER                              (-702, ChaiError.UNKNOWN,                       true, false),
        DUPLICATE_TEMPLATE                             (-703, ChaiError.UNKNOWN,                       true, false),
        NO_MASTER_REPLICA                              (-704, ChaiError.UNKNOWN,                       true, false),
        DUPLICATE_CONTAINMENT                          (-705, ChaiError.UNKNOWN,                       true, false),
        NOT_SIBLING                                    (-706, ChaiError.UNKNOWN,                       true, false),
        INVALID_SIGNATURE                              (-707, ChaiError.UNKNOWN,                       true, false),
        INVALID_RESPONSE                               (-708, ChaiError.UNKNOWN,                       true, false),
        INSUFFICIENT_SOCKETS                           (-709, ChaiError.UNKNOWN,                       true, false),
        DATABASE_READ_FAIL                             (-710, ChaiError.UNKNOWN,                       true, false),
        INVALID_CODE_PAGE                              (-711, ChaiError.UNKNOWN,                       true, false),
        INVALID_ESCAPE_CHAR                            (-712, ChaiError.UNKNOWN,                       true, false),
        INVALID_DELIMITERS                             (-713, ChaiError.UNKNOWN,                       true, false),
        NOT_IMPLEMENTED                                (-714, ChaiError.UNKNOWN,                       true, false),
        CHECKSUM_FAILURE                               (-715, ChaiError.UNKNOWN,                       true, false),
        CHECKSUMMING_NOT_SUPPORTED                     (-716, ChaiError.UNKNOWN,                       true, false),
        CRC_FAILURE                                    (-717, ChaiError.UNKNOWN,                       true, false),
        INVALID_ENTRY_HANDLE                           (-718, ChaiError.UNKNOWN,                       true, false),
        INVALID_VALUE_HANDLE                           (-719, ChaiError.UNKNOWN,                       true, false),
        CONNECTION_DENIED                              (-720, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_FEDERATION_LINK                        (-721, ChaiError.UNKNOWN,                       true, false),
        OP_SCHEMA_MISMATCH                             (-722, ChaiError.UNKNOWN,                       true, false),
        STREAM_NOT_FOUND                               (-723, ChaiError.UNKNOWN,                       true, false),
        DCLIENT_UNAVAILABLE                            (-724, ChaiError.UNKNOWN,                       true, false),
        MASV_NO_ACCESS                                 (-725, ChaiError.UNKNOWN,                       true, false),
        MASV_INVALID_REQUEST                           (-726, ChaiError.UNKNOWN,                       true, false),
        MASV_FAILURE                                   (-727, ChaiError.UNKNOWN,                       true, false),
        MASV_ALREADY_EXISTS                            (-728, ChaiError.UNKNOWN,                       true, false),
        MASV_NOT_FOUND                                 (-729, ChaiError.UNKNOWN,                       true, false),
        MASV_BAD_RANGE                                 (-730, ChaiError.UNKNOWN,                       true, false),
        VALUE_DATA                                     (-731, ChaiError.UNKNOWN,                       true, false),
        DATABASE_LOCKED                                (-732, ChaiError.UNKNOWN,                       true, false),
        DATABASE_ALREADY_EXISTS                        (-733, ChaiError.UNKNOWN,                       true, false),
        DATABASE_NOT_FOUND                             (-734, ChaiError.UNKNOWN,                       true, false),
        NOTHING_TO_ABORT                               (-735, ChaiError.UNKNOWN,                       true, false),
        END_OF_STREAM                                  (-736, ChaiError.UNKNOWN,                       true, false),
        NO_SUCH_TEMPLATE                               (-737, ChaiError.UNKNOWN,                       true, false),
        SAS_LOCKED                                     (-738, ChaiError.UNKNOWN,                       true, false),
        INVALID_SAS_VERSION                            (-739, ChaiError.UNKNOWN,                       true, false),
        SAS_ALREADY_REGISTERED                         (-740, ChaiError.UNKNOWN,                       true, false),
        NAME_TYPE_NOT_SUPPORTED                        (-741, ChaiError.UNKNOWN,                       true, false),
        WRONG_DS_VERSION                               (-742, ChaiError.UNKNOWN,                       true, false),
        INVALID_CONTROL_FUNCTION                       (-743, ChaiError.UNKNOWN,                       true, false),
        INVALID_CONTROL_STATE                          (-744, ChaiError.UNKNOWN,                       true, false),
        ERR_CACHE_IN_USE                               (-745, ChaiError.UNKNOWN,                       true, false),
        ERR_ZERO_CREATION_TIME                         (-746, ChaiError.UNKNOWN,                       true, false),
        ERR_WOULD_BLOCK                                (-747, ChaiError.UNKNOWN,                       true, false),
        ERR_CONN_TIMEOUT                               (-748, ChaiError.UNKNOWN,                       true, false),
        ERR_TOO_MANY_REFERRALS                         (-749, ChaiError.UNKNOWN,                       true, false),
        ERR_OPERATION_CANCELLED                        (-750, ChaiError.UNKNOWN,                       true, false),
        ERR_UNKNOWN_TARGET                             (-751, ChaiError.UNKNOWN,                       true, false),
        ERR_GUID_FAILURE                               (-752, ChaiError.UNKNOWN,                       true, false),
        ERR_INCOMPATIBLE_OS                            (-753, ChaiError.UNKNOWN,                       true, false),
        ERR_CALLBACK_CANCEL                            (-754, ChaiError.UNKNOWN,                       true, false),
        ERR_INVALID_SYNC_DATA                          (-755, ChaiError.UNKNOWN,                       true, false),
        ERR_STREAM_EXISTS                              (-756, ChaiError.UNKNOWN,                       true, false),
        ERR_AUXILIARY_HAS_CONTAINMENT                  (-757, ChaiError.UNKNOWN,                       true, false),
        ERR_AUXILIARY_NOT_CONTAINER                    (-758, ChaiError.UNKNOWN,                       true, false),
        ERR_AUXILIARY_NOT_EFFECTIVE                    (-759, ChaiError.UNKNOWN,                       true, false),
        ERR_AUXILIARY_ON_ALIAS                         (-760, ChaiError.UNKNOWN,                       true, false),
        ERR_HAVE_SEEN_STATE                            (-761, ChaiError.UNKNOWN,                       true, false),
        ERR_VERB_LOCKED                                (-762, ChaiError.UNKNOWN,                       true, false),
        ERR_VERB_EXCEEDS_TABLE_LENGTH                  (-763, ChaiError.UNKNOWN,                       true, false),
        ERR_BOF_HIT                                    (-764, ChaiError.UNKNOWN,                       true, false),
        ERR_EOF_HIT                                    (-765, ChaiError.UNKNOWN,                       true, false),
        ERR_INCOMPATIBLE_REPLICA_VER                   (-766, ChaiError.UNKNOWN,                       true, false),
        ERR_QUERY_TIMEOUT                              (-767, ChaiError.UNKNOWN,                       true, false),
        ERR_QUERY_MAX_COUNT                            (-768, ChaiError.UNKNOWN,                       true, false),
        ERR_DUPLICATE_NAMING                           (-769, ChaiError.UNKNOWN,                       true, false),
        ERR_NO_TRANS_ACTIVE                            (-770, ChaiError.UNKNOWN,                       true, false),
        ERR_TRANS_ACTIVE                               (-771, ChaiError.UNKNOWN,                       true, false),
        ERR_ILLEGAL_TRANS_OP                           (-772, ChaiError.UNKNOWN,                       true, false),
        ERR_ITERATOR_SYNTAX                            (-773, ChaiError.UNKNOWN,                       true, false),
        ERR_REPAIRING_DIB                              (-774, ChaiError.UNKNOWN,                       true, false),
        ERR_INVALID_OID_FORMAT                         (-775, ChaiError.UNKNOWN,                       true, false),
        ERR_DS_AGENT_CLOSING                           (-776, ChaiError.UNKNOWN,                       true, false),
        ERR_SPARSE_FILTER_VIOLATION                    (-777, ChaiError.UNKNOWN,                       true, false),
        ERR_VPVECTOR_CORRELATION_ERR                   (-778, ChaiError.UNKNOWN,                       true, false),
        ERR_CANNOT_GO_REMOTE                           (-779, ChaiError.UNKNOWN,                       true, false),
        ERR_REQUEST_NOT_SUPPORTED                      (-780, ChaiError.UNKNOWN,                       true, false),
        ERR_ENTRY_NOT_LOCAL                            (-781, ChaiError.UNKNOWN,                       true, false),
        ERR_ROOT_UNREACHABLE                           (-782, ChaiError.UNKNOWN,                       true, false),
        ERR_VRDIM_NOT_INITIALIZED                      (-783, ChaiError.UNKNOWN,                       true, false),
        ERR_WAIT_TIMEOUT                               (-784, ChaiError.UNKNOWN,                       true, false),
        ERR_DIB_ERROR                                  (-785, ChaiError.UNKNOWN,                       true, false),
        ERR_DIB_IO_FAILURE                             (-786, ChaiError.UNKNOWN,                       true, false),
        ERR_ILLEGAL_SCHEMA_ATTRIBUTE                   (-787, ChaiError.UNKNOWN,                       true, false),
        ERR_SCHEMA_PARTITION                           (-788, ChaiError.UNKNOWN,                       true, false),
        ERR_INVALID_TEMPLATE                           (-789, ChaiError.UNKNOWN,                       true, false),
        ERR_OPENING_FILE                               (-790, ChaiError.UNKNOWN,                       true, false),
        ERR_DIRECT_OPENING_FILE                        (-791, ChaiError.UNKNOWN,                       true, false),
        ERR_CREATING_FILE                              (-792, ChaiError.UNKNOWN,                       true, false),
        ERR_DIRECT_CREATING_FILE                       (-793, ChaiError.UNKNOWN,                       true, false),
        ERR_READING_FILE                               (-794, ChaiError.UNKNOWN,                       true, false),
        ERR_DIRECT_READING_FILE                        (-795, ChaiError.UNKNOWN,                       true, false),
        ERR_WRITING_FILE                               (-796, ChaiError.UNKNOWN,                       true, false),
        ERR_DIRECT_WRITING_FILE                        (-797, ChaiError.UNKNOWN,                       true, false),
        ERR_POSITIONING_IN_FILE                        (-798, ChaiError.UNKNOWN,                       true, false),
        ERR_GETTING_FILE_SIZE                          (-799, ChaiError.UNKNOWN,                       true, false),

        UNSUPPORTED_OPERATION                          (Integer.MAX_VALUE,   ChaiError.UNSUPPORTED_OPERATION, true, false, "Unrecognized extended operation"),
        UNKNOWN                                        (-1,   ChaiError.UNKNOWN,                       true, false),


        ;

        private int edirErrorCode;
        private ChaiError chaiErrorCode;
        private boolean permenant;
        private boolean authentication;
        private String[] errorStrings;

        EdirError(
                final int errorCodeNumber,
                final ChaiError chaiErrorCode,
                final boolean permenant,
                final boolean authentication,
                final String... errorStrings
        )
        {
            this.edirErrorCode = errorCodeNumber;
            this.chaiErrorCode = chaiErrorCode;
            this.permenant = permenant;
            this.authentication = authentication;
            this.errorStrings = errorStrings;
        }

        public int getEdirErrorCode()
        {
            return edirErrorCode;
        }

        public boolean isPermenant()
        {
            return permenant;
        }

        public boolean isAuthentication()
        {
            return authentication;
        }

        public ChaiError getChaiErrorCode()
        {
            return chaiErrorCode;
        }

        public String[] getErrorStrings()
        {
            return errorStrings;
        }
    }
}
