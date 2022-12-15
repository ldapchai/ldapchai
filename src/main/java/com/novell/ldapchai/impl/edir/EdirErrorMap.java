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

package com.novell.ldapchai.impl.edir;

import com.novell.ldapchai.exception.ChaiError;
import com.novell.ldapchai.exception.ErrorMap;
import com.novell.ldapchai.util.internal.StringHelper;

public class EdirErrorMap implements ErrorMap
{
    private static final EdirErrorMap SINGLETON = new EdirErrorMap();

    private EdirErrorMap()
    {
    }

    public static EdirErrorMap instance()
    {
        return SINGLETON;
    }

    @Override
    public ChaiError errorForMessage( final String message )
    {
        return forMessage( message ).getChaiErrorCode();
    }

    @Override
    public boolean isPermanent( final String message )
    {
        return forMessage( message ).isPermanent();
    }

    @Override
    public boolean isAuthenticationRelated( final String message )
    {
        return forMessage( message ).isAuthentication();
    }

    private static final String UNRECOGNIZED_EXTENDED_OPERATION_ERROR_TEXT = "Unrecognized extended operation";

    private static EdirError forMessage( final String message )
    {
        if ( message == null || message.length() < 1 )
        {
            return EdirError.UNKNOWN;
        }

        for ( final EdirError error : EdirError.values() )
        {
            if ( message.contains( String.valueOf( error.getEdirErrorCode() ) ) )
            {
                return error;
            }
        }

        if ( message.contains( UNRECOGNIZED_EXTENDED_OPERATION_ERROR_TEXT ) )
        {
            return EdirError.UNSUPPORTED_OPERATION;
        }


        return EdirError.UNKNOWN;
    }

    private enum EdirError
    {
        // nmas errors
        PASSWORD_TOO_LONG( -16000, ChaiError.PASSWORD_TOO_LONG ),
        PASSWORD_NOT_ENOUGH_NUM( -16008, ChaiError.PASSWORD_NOT_ENOUGH_NUM ),
        PASSWORD_NOT_ENOUGH_SPECIAL( -16013, ChaiError.PASSWORD_NOT_ENOUGH_SPECIAL ),
        PASSWORD_NOT_ENOUGH_LOWER( -16003, ChaiError.PASSWORD_NOT_ENOUGH_LOWER ),
        PASSWORD_NOT_ENOUGH_UPPER( -16001, ChaiError.PASSWORD_NOT_ENOUGH_UPPER ),
        PASSWORD_NOT_ENOUGH_UNIQUE( -16017, ChaiError.PASSWORD_NOT_ENOUGH_UNIQUE ),
        PASSWORD_TOO_MANY_REPEAT( -16015, ChaiError.PASSWORD_TOO_MANY_REPEAT ),
        PASSWORD_TOO_MANY_NUMERIC( -16009, ChaiError.PASSWORD_TOO_MANY_NUMERIC ),
        PASSWORD_TOO_MANY_LOWER( -16004, ChaiError.PASSWORD_TOO_MANY_LOWER ),
        PASSWORD_TOO_MANY_UPPER( -16002, ChaiError.PASSWORD_TOO_MANY_UPPER ),
        PASSWORD_FIRST_IS_NUMERIC( -16006, ChaiError.PASSWORD_FIRST_IS_NUMERIC ),
        PASSWORD_LAST_IS_NUMERIC( -16007, ChaiError.PASSWORD_LAST_IS_NUMERIC ),
        PASSWORD_FIRST_IS_SPECIAL( -16011, ChaiError.PASSWORD_FIRST_IS_SPECIAL ),
        PASSWORD_LAST_IS_SPECIAL( -16012, ChaiError.PASSWORD_LAST_IS_SPECIAL ),
        PASSWORD_TOO_MANY_SPECIAL( -16014, ChaiError.PASSWORD_TOO_MANY_SPECIAL ),
        PASSWORD_INVALID_CHAR( -16021, ChaiError.PASSWORD_INVALID_CHAR ),
        PASSWORD_INWORDLIST( -16019, ChaiError.PASSWORD_INWORDLIST ),
        PASSWORD_SAMEASATTR( -16020, ChaiError.PASSWORD_SAMEASATTR ),
        PASSWORD_HISTORY_FULL( -1696, ChaiError.PASSWORD_HISTORY_FULL ),
        PASSWORD_NUMERIC_DISALLOWED( -16005, ChaiError.PASSWORD_NUMERIC_DISALLOWED ),
        PASSWORD_SPECIAL_DISALLOWED( -16010, ChaiError.PASSWORD_SPECIAL_DISALLOWED ),
        PASSWORD_TOO_SOON( -16018, ChaiError.PASSWORD_TOO_SOON ),

        BUFFER_TOO_SMALL( -119, ChaiError.UNKNOWN ),
        VOLUME_FLAG_NOT_SET( -120, ChaiError.UNKNOWN ),
        NO_ITEMS_FOUND( -121, ChaiError.UNKNOWN ),
        CONN_ALREADY_TEMPORARY( -122, ChaiError.UNKNOWN ),
        CONN_ALREADY_LOGGED_IN( -123, ChaiError.UNKNOWN ),
        CONN_NOT_AUTHENTICATED( -124, ChaiError.UNKNOWN ),
        CONN_NOT_LOGGED_IN( -125, ChaiError.UNKNOWN ),
        NCP_BOUNDARY_CHECK_FAILED( -126, ChaiError.UNKNOWN ),
        LOCK_WAITING( -127, ChaiError.UNKNOWN ),
        LOCK_FAIL( -128, ChaiError.UNKNOWN ),
        OUT_OF_HANDLES( -129, ChaiError.UNKNOWN ),
        NO_OPEN_PRIVILEGE( -130, ChaiError.UNKNOWN ),
        HARD_IO_ERROR( -131, ChaiError.UNKNOWN ),
        NO_CREATE_PRIVILEGE( -132, ChaiError.UNKNOWN ),
        NO_CREATE_DELETE_PRIV( -133, ChaiError.UNKNOWN ),
        R_O_CREATE_FILE( -134, ChaiError.UNKNOWN ),
        CREATE_FILE_INVALID_NAME( -135, ChaiError.UNKNOWN ),
        INVALID_FILE_HANDLE( -136, ChaiError.UNKNOWN ),
        NO_SEARCH_PRIVILEGE( -137, ChaiError.UNKNOWN ),
        NO_DELETE_PRIVILEGE( -138, ChaiError.UNKNOWN ),
        NO_RENAME_PRIVILEGE( -139, ChaiError.UNKNOWN ),
        NO_SET_PRIVILEGE( -140, ChaiError.UNKNOWN ),
        SOME_FILES_IN_USE( -141, ChaiError.UNKNOWN ),
        ALL_FILES_IN_USE( -142, ChaiError.UNKNOWN ),
        SOME_READ_ONLY( -143, ChaiError.UNKNOWN ),
        ALL_READ_ONLY( -144, ChaiError.UNKNOWN ),
        SOME_NAMES_EXIST( -145, ChaiError.UNKNOWN ),
        ALL_NAMES_EXIST( -146, ChaiError.UNKNOWN ),
        NO_READ_PRIVILEGE( -147, ChaiError.UNKNOWN ),
        NO_WRITE_PRIVILEGE( -148, ChaiError.UNKNOWN ),
        FILE_DETACHED( -149, ChaiError.UNKNOWN ),
        INSUFFICIENT_MEMORY( -150, ChaiError.UNKNOWN ),
        NO_ALLOC_SPACE( -150, ChaiError.UNKNOWN ),
        TARGET_NOT_A_SUBDIR( -150, ChaiError.UNKNOWN ),
        NO_SPOOL_SPACE( -151, ChaiError.UNKNOWN ),
        INVALID_VOLUME( -152, ChaiError.UNKNOWN ),
        DIRECTORY_FULL( -153, ChaiError.UNKNOWN ),
        RENAME_ACROSS_VOLUME( -154, ChaiError.UNKNOWN ),
        BAD_DIR_HANDLE( -155, ChaiError.UNKNOWN ),
        INVALID_PATH( -156, ChaiError.UNKNOWN ),
        NO_SUCH_EXTENSION( -156, ChaiError.UNKNOWN ),
        NO_DIR_HANDLES( -157, ChaiError.UNKNOWN ),
        BAD_FILE_NAME( -158, ChaiError.UNKNOWN ),
        DIRECTORY_ACTIVE( -159, ChaiError.UNKNOWN ),
        DIRECTORY_NOT_EMPTY( -160, ChaiError.UNKNOWN ),
        DIRECTORY_IO_ERROR( -161, ChaiError.UNKNOWN ),
        IO_LOCKED( -162, ChaiError.UNKNOWN ),
        TRANSACTION_RESTARTED( -163, ChaiError.UNKNOWN ),
        RENAME_DIR_INVALID( -164, ChaiError.UNKNOWN ),
        INVALID_OPENCREATE_MODE( -165, ChaiError.UNKNOWN ),
        ALREADY_IN_USE( -166, ChaiError.UNKNOWN ),
        INVALID_RESOURCE_TAG( -167, ChaiError.UNKNOWN ),
        ACCESS_DENIED( -168, ChaiError.UNKNOWN ),
        DSERR_LOGIN_SIGNING_REQUIRED( -188, ChaiError.UNKNOWN ),
        DSERR_LOGIN_ENCRYPT_REQUIRED( -189, ChaiError.UNKNOWN ),
        INVALID_DATA_STREAM( -190, ChaiError.UNKNOWN ),
        INVALID_NAME_SPACE( -191, ChaiError.UNKNOWN ),
        NO_ACCOUNTING_PRIVILEGES( -192, ChaiError.UNKNOWN ),
        NO_ACCOUNT_BALANCE( -193, ChaiError.UNKNOWN ),
        CREDIT_LIMIT_EXCEEDED( -194, ChaiError.UNKNOWN ),
        TOO_MANY_HOLDS( -195, ChaiError.UNKNOWN ),
        ACCOUNTING_DISABLED( -196, ChaiError.UNKNOWN ),
        LOGIN_LOCKOUT( -197, ChaiError.INTRUDER_LOCKOUT, Flag.Authentication ),
        NO_CONSOLE_RIGHTS( -198, ChaiError.UNKNOWN ),
        Q_IO_FAILURE( -208, ChaiError.UNKNOWN ),
        NO_QUEUE( -209, ChaiError.UNKNOWN ),
        NO_Q_SERVER( -210, ChaiError.UNKNOWN ),
        NO_Q_RIGHTS( -211, ChaiError.UNKNOWN ),
        Q_FULL( -212, ChaiError.UNKNOWN ),
        NO_Q_JOB( -213, ChaiError.UNKNOWN ),
        NO_Q_JOB_RIGHTS( -214, ChaiError.UNKNOWN ),
        UNENCRYPTED_NOT_ALLOWED( -214, ChaiError.UNKNOWN ),
        DUPLICATE_PASSWORD( -215, ChaiError.PASSWORD_PREVIOUSLYUSED ),
        Q_IN_SERVICE( -215, ChaiError.UNKNOWN ),
        Q_NOT_ACTIVE( -216, ChaiError.UNKNOWN ),
        PASSWORD_TOO_SHORT( -216, ChaiError.PASSWORD_TOO_SHORT ),
        MAXIMUM_LOGINS_EXCEEDED( -217, ChaiError.UNKNOWN ),
        Q_STN_NOT_SERVER( -217, ChaiError.UNKNOWN ),
        BAD_LOGIN_TIME( -218, ChaiError.UNKNOWN ),
        Q_HALTED( -218, ChaiError.UNKNOWN ),
        NODE_ADDRESS_VIOLATION( -219, ChaiError.UNKNOWN ),
        Q_MAX_SERVERS( -219, ChaiError.UNKNOWN ),
        LOG_ACCOUNT_EXPIRED( -220, ChaiError.ACCOUNT_EXPIRED, Flag.Authentication ),
        BAD_PASSWORD( -222, ChaiError.PASSWORD_BADPASSWORD, Flag.Authentication ),
        PASSWORD_EXPIRED( -223, ChaiError.PASSWORD_EXPIRED, Flag.Authentication ),
        NO_LOGIN_CONN_AVAILABLE( -224, ChaiError.UNKNOWN ),
        WRITE_TO_GROUP_PROPERTY( -232, ChaiError.UNKNOWN ),
        MEMBER_ALREADY_EXISTS( -233, ChaiError.UNKNOWN ),
        NO_SUCH_MEMBER( -234, ChaiError.UNKNOWN ),
        PROPERTY_NOT_GROUP( -235, ChaiError.UNKNOWN ),
        NO_SUCH_VALUE_SET( -236, ChaiError.UNKNOWN ),
        PROPERTY_ALREADY_EXISTS( -237, ChaiError.UNKNOWN ),
        OBJECT_ALREADY_EXISTS( -238, ChaiError.UNKNOWN ),
        ILLEGAL_NAME( -239, ChaiError.UNKNOWN ),
        ILLEGAL_WILDCARD( -240, ChaiError.UNKNOWN ),
        BINDERY_SECURITY( -241, ChaiError.UNKNOWN ),
        NO_OBJECT_READ_RIGHTS( -242, ChaiError.UNKNOWN ),
        NO_OBJECT_RENAME_RIGHTS( -243, ChaiError.UNKNOWN ),
        NO_OBJECT_DELETE_RIGHTS( -244, ChaiError.UNKNOWN ),
        NO_OBJECT_CREATE_RIGHTS( -245, ChaiError.UNKNOWN ),
        NO_PROPERTY_DELETE_RIGHTS( -246, ChaiError.UNKNOWN ),
        NO_PROPERTY_CREATE_RIGHTS( -247, ChaiError.UNKNOWN ),
        NO_PROPERTY_WRITE_RIGHTS( -248, ChaiError.UNKNOWN ),
        NO_PROPERTY_READ_RIGHTS( -249, ChaiError.UNKNOWN ),
        TEMP_REMAP( -250, ChaiError.UNKNOWN ),
        NO_SUCH_PROPERTY( -251, ChaiError.UNKNOWN ),
        UNKNOWN_REQUEST( -251, ChaiError.UNKNOWN ),
        NO_SUCH_OBJECT( -252, ChaiError.NO_SUCH_ENTRY ),
        MESSAGE_QUEUE_FULL( -252, ChaiError.UNKNOWN ),
        TARGET_ALREADY_HAS_MSG( -252, ChaiError.UNKNOWN ),
        BAD_STATION_NUMBER( -253, ChaiError.UNKNOWN ),
        BINDERY_LOCKED( -254, ChaiError.UNKNOWN ),
        DIR_LOCKED( -254, ChaiError.UNKNOWN ),
        DSERR_TIMEOUT( -254, ChaiError.UNKNOWN ),
        LOGIN_DISABLED( -254, ChaiError.ACCOUNT_DISABLED, Flag.Authentication ),
        SPOOL_DELETE( -254, ChaiError.UNKNOWN ),
        TRUSTEE_NOT_FOUND( -254, ChaiError.UNKNOWN ),
        BAD_PARAMETER( -255, ChaiError.UNKNOWN ),
        BAD_SPOOL_PRINTER( -255, ChaiError.UNKNOWN ),
        CLOSE_FCB( -255, ChaiError.UNKNOWN ),
        ERR_OF_SOME_SORT( -255, ChaiError.UNKNOWN ),
        FILE_EXISTS( -255, ChaiError.UNKNOWN ),
        FILE_NAME( -255, ChaiError.UNKNOWN ),
        HARD_FAILURE( -255, ChaiError.UNKNOWN ),
        IO_BOUND( -255, ChaiError.UNKNOWN ),
        MUST_FORCE_DOWN( -255, ChaiError.UNKNOWN ),
        NO_FILES_FOUND( -255, ChaiError.UNKNOWN ),
        NO_SPOOL_FILE( -255, ChaiError.UNKNOWN ),
        NO_TRUSTEE_CHANGE_PRIV( -255, ChaiError.UNKNOWN ),
        TARGET_NOT_ACCEPTING_MSGS( -255, ChaiError.UNKNOWN ),
        TARGET_NOT_LOGGED_IN( -255, ChaiError.UNKNOWN ),
        PREEMPT_COMM( -286, ChaiError.UNKNOWN ),
        CLOSE_COMM( -287, ChaiError.UNKNOWN ),
        OPEN_COMM( -288, ChaiError.UNKNOWN ),
        ALREADY_EXISTS( -289, ChaiError.UNKNOWN ),
        INVALID_COUNT( -290, ChaiError.UNKNOWN ),
        TIMEOUT( -291, ChaiError.UNKNOWN ),
        FATAL( -292, ChaiError.UNKNOWN ),
        MEMORY_ERR( -293, ChaiError.UNKNOWN ),
        VRDRIVER_INTERFACE_MISMATCH( -294, ChaiError.UNKNOWN ),
        VRDRIVER_CREATE_FAIL( -295, ChaiError.UNKNOWN ),
        VRDRIVER_MISSING( -296, ChaiError.UNKNOWN ),
        JVM_CREATE_FAIL( -297, ChaiError.UNKNOWN ),
        JVM_INIT_FAIL( -298, ChaiError.UNKNOWN ),
        JRE_LOAD_FAIL( -299, ChaiError.UNKNOWN ),
        NO_JRE( -300, ChaiError.UNKNOWN ),
        NOT_ENOUGH_MEMORY( -301, ChaiError.UNKNOWN ),
        BAD_KEY( -302, ChaiError.UNKNOWN ),
        BAD_CONTEXT( -303, ChaiError.UNKNOWN ),
        BUFFER_FULL( -304, ChaiError.UNKNOWN ),
        LIST_EMPTY( -305, ChaiError.UNKNOWN ),
        BAD_SYNTAX( -306, ChaiError.UNKNOWN ),
        BUFFER_EMPTY( -307, ChaiError.UNKNOWN ),
        BAD_VERB( -308, ChaiError.UNKNOWN ),
        EXPECTED_IDENTIFIER( -309, ChaiError.UNKNOWN ),
        EXPECTED_EQUALS( -310, ChaiError.UNKNOWN ),
        ATTR_TYPE_EXPECTED( -311, ChaiError.UNKNOWN ),
        ATTR_TYPE_NOT_EXPECTED( -312, ChaiError.UNKNOWN ),
        FILTER_TREE_EMPTY( -313, ChaiError.UNKNOWN ),
        INVALID_OBJECT_NAME( -314, ChaiError.UNKNOWN ),
        EXPECTED_RDN_DELIMITER( -315, ChaiError.UNKNOWN ),
        TOO_MANY_TOKENS( -316, ChaiError.UNKNOWN ),
        INCONSISTENT_MULTIAVA( -317, ChaiError.UNKNOWN ),
        COUNTRY_NAME_TOO_LONG( -318, ChaiError.UNKNOWN ),
        SYSTEM_ERROR( -319, ChaiError.UNKNOWN ),
        CANT_ADD_ROOT( -320, ChaiError.UNKNOWN ),
        UNABLE_TO_ATTACH( -321, ChaiError.UNKNOWN ),
        INVALID_HANDLE( -322, ChaiError.UNKNOWN ),
        BUFFER_ZERO_LENGTH( -323, ChaiError.UNKNOWN ),
        INVALID_REPLICA_TYPE( -324, ChaiError.UNKNOWN ),
        INVALID_ATTR_SYNTAX( -325, ChaiError.UNKNOWN ),
        INVALID_FILTER_SYNTAX( -326, ChaiError.UNKNOWN ),
        CONTEXT_CREATION( -328, ChaiError.UNKNOWN ),
        INVALID_UNION_TAG( -329, ChaiError.UNKNOWN ),
        INVALID_SERVER_RESPONSE( -330, ChaiError.UNKNOWN ),
        NULL_POINTER( -331, ChaiError.UNKNOWN ),
        NO_SERVER_FOUND( -332, ChaiError.UNKNOWN ),
        NO_CONNECTION( -333, ChaiError.UNKNOWN ),
        RDN_TOO_LONG( -334, ChaiError.UNKNOWN ),
        DUPLICATE_TYPE( -335, ChaiError.UNKNOWN ),
        DATA_STORE_FAILURE( -336, ChaiError.UNKNOWN ),
        NOT_LOGGED_IN( -337, ChaiError.UNKNOWN ),
        INVALID_PASSWORD_CHARS( -338, ChaiError.UNKNOWN ),
        FAILED_SERVER_AUTHENT( -339, ChaiError.UNKNOWN ),
        TRANSPORT( -340, ChaiError.UNKNOWN ),
        NO_SUCH_SYNTAX( -341, ChaiError.UNKNOWN ),
        INVALID_DS_NAME( -342, ChaiError.UNKNOWN ),
        ATTR_NAME_TOO_LONG( -343, ChaiError.UNKNOWN ),
        INVALID_TDS( -344, ChaiError.UNKNOWN ),
        INVALID_DS_VERSION( -345, ChaiError.UNKNOWN ),
        UNICODE_TRANSLATION( -346, ChaiError.UNKNOWN ),
        SCHEMA_NAME_TOO_LONG( -347, ChaiError.UNKNOWN ),
        UNICODE_FILE_NOT_FOUND( -348, ChaiError.UNKNOWN ),
        UNICODE_ALREADY_LOADED( -349, ChaiError.UNKNOWN ),
        NOT_CONTEXT_OWNER( -350, ChaiError.UNKNOWN ),
        ATTEMPT_TO_AUTHENTICATE_0( -351, ChaiError.UNKNOWN ),
        NO_WRITABLE_REPLICAS( -352, ChaiError.UNKNOWN ),
        DN_TOO_LONG( -353, ChaiError.UNKNOWN ),
        RENAME_NOT_ALLOWED( -354, ChaiError.UNKNOWN ),
        ERR_NOT_NDS_FOR_NT( -355, ChaiError.UNKNOWN ),
        ERR_NDS_FOR_NT_NO_DOMAIN( -356, ChaiError.UNKNOWN ),
        ERR_NDS_FOR_NT_SYNC_DISABLED( -357, ChaiError.UNKNOWN ),
        ERR_ITR_INVALID_HANDLE( -358, ChaiError.UNKNOWN ),
        ERR_ITR_INVALID_POSITION( -359, ChaiError.UNKNOWN ),
        ERR_ITR_INVALID_SEARCH_DATA( -360, ChaiError.UNKNOWN ),
        ERR_ITR_INVALID_SCOPE( -361, ChaiError.UNKNOWN ),
        ERR_ITR_MAX_COUNT( -362, ChaiError.UNKNOWN ),
        UNI_ALREADY_LOADED( -489, ChaiError.UNKNOWN ),
        UNI_FUTURE_OPCODE( -490, ChaiError.UNKNOWN ),
        UNI_NO_SUCH_FILE( -491, ChaiError.UNKNOWN ),
        UNI_TOO_MANY_FILES( -492, ChaiError.UNKNOWN ),
        UNI_NO_PERMISSION( -493, ChaiError.UNKNOWN ),
        UNI_NO_MEMORY( -494, ChaiError.UNKNOWN ),
        UNI_LOAD_FAILED( -495, ChaiError.UNKNOWN ),
        UNI_HANDLE_BAD( -496, ChaiError.UNKNOWN ),
        UNI_HANDLE_MISMATCH( -497, ChaiError.UNKNOWN ),
        UNI_RULES_CORRUPT( -498, ChaiError.UNKNOWN ),
        UNI_NO_DEFAULT( -499, ChaiError.UNKNOWN ),
        UNI_INSUFFICIENT_BUFFER( -500, ChaiError.UNKNOWN ),
        UNI_OPEN_FAILED( -501, ChaiError.UNKNOWN ),
        UNI_NO_LOAD_DIR( -502, ChaiError.UNKNOWN ),
        UNI_BAD_FILE_HANDLE( -503, ChaiError.UNKNOWN ),
        UNI_READ_FAILED( -504, ChaiError.UNKNOWN ),
        UNI_TRANS_CORRUPT( -505, ChaiError.UNKNOWN ),
        NO_SUCH_ENTRY( -601, ChaiError.NO_SUCH_ENTRY ),
        NO_SUCH_VALUE( -602, ChaiError.NO_SUCH_VALUE ),
        NO_SUCH_ATTRIBUTE( -603, ChaiError.NO_SUCH_ATTRIBUTE ),
        NO_SUCH_CLASS( -604, ChaiError.UNKNOWN ),
        NO_SUCH_PARTITION( -605, ChaiError.UNKNOWN ),
        ENTRY_ALREADY_EXISTS( -606, ChaiError.UNKNOWN ),
        NOT_EFFECTIVE_CLASS( -607, ChaiError.UNKNOWN ),
        ILLEGAL_ATTRIBUTE( -608, ChaiError.UNKNOWN ),
        MISSING_MANDATORY( -609, ChaiError.UNKNOWN ),
        ILLEGAL_DS_NAME( -610, ChaiError.UNKNOWN ),
        ILLEGAL_CONTAINMENT( -611, ChaiError.UNKNOWN ),
        CANT_HAVE_MULTIPLE_VALUES( -612, ChaiError.UNKNOWN ),
        SYNTAX_VIOLATION( -613, ChaiError.UNKNOWN ),
        DUPLICATE_VALUE( -614, ChaiError.UNKNOWN ),
        ATTRIBUTE_ALREADY_EXISTS( -615, ChaiError.UNKNOWN ),
        MAXIMUM_ENTRIES_EXIST( -616, ChaiError.UNKNOWN ),
        DATABASE_FORMAT( -617, ChaiError.UNKNOWN ),
        INCONSISTENT_DATABASE( -618, ChaiError.UNKNOWN ),
        INVALID_COMPARISON( -619, ChaiError.UNKNOWN ),
        COMPARISON_FAILED( -620, ChaiError.UNKNOWN ),
        TRANSACTIONS_DISABLED( -621, ChaiError.UNKNOWN ),
        INVALID_TRANSPORT( -622, ChaiError.UNKNOWN ),
        SYNTAX_INVALID_IN_NAME( -623, ChaiError.UNKNOWN ),
        REPLICA_ALREADY_EXISTS( -624, ChaiError.UNKNOWN ),
        TRANSPORT_FAILURE( -625, ChaiError.UNKNOWN ),
        ALL_REFERRALS_FAILED( -626, ChaiError.UNKNOWN ),
        CANT_REMOVE_NAMING_VALUE( -627, ChaiError.UNKNOWN ),
        OBJECT_CLASS_VIOLATION( -628, ChaiError.UNKNOWN ),
        ENTRY_IS_NOT_LEAF( -629, ChaiError.UNKNOWN ),
        DIFFERENT_TREE( -630, ChaiError.UNKNOWN ),
        ILLEGAL_REPLICA_TYPE( -631, ChaiError.UNKNOWN ),
        SYSTEM_FAILURE( -632, ChaiError.UNKNOWN ),
        INVALID_ENTRY_FOR_ROOT( -633, ChaiError.UNKNOWN ),
        NO_REFERRALS( -634, ChaiError.UNKNOWN ),
        REMOTE_FAILURE( -635, ChaiError.UNKNOWN ),
        UNREACHABLE_SERVER( -636, ChaiError.UNKNOWN ),
        PREVIOUS_MOVE_IN_PROGRESS( -637, ChaiError.UNKNOWN ),
        NO_CHARACTER_MAPPING( -638, ChaiError.UNKNOWN ),
        INCOMPLETE_AUTHENTICATION( -639, ChaiError.UNKNOWN ),
        INVALID_CERTIFICATE( -640, ChaiError.UNKNOWN ),
        INVALID_REQUEST( -641, ChaiError.UNKNOWN ),
        INVALID_ITERATION( -642, ChaiError.UNKNOWN ),
        SCHEMA_IS_NONREMOVABLE( -643, ChaiError.UNKNOWN ),
        SCHEMA_IS_IN_USE( -644, ChaiError.UNKNOWN ),
        CLASS_ALREADY_EXISTS( -645, ChaiError.UNKNOWN ),
        BAD_NAMING_ATTRIBUTES( -646, ChaiError.UNKNOWN ),
        NOT_ROOT_PARTITION( -647, ChaiError.UNKNOWN ),
        INSUFFICIENT_STACK( -648, ChaiError.UNKNOWN ),
        INSUFFICIENT_BUFFER( -649, ChaiError.UNKNOWN ),
        AMBIGUOUS_CONTAINMENT( -650, ChaiError.UNKNOWN ),
        AMBIGUOUS_NAMING( -651, ChaiError.UNKNOWN ),
        DUPLICATE_MANDATORY( -652, ChaiError.UNKNOWN ),
        DUPLICATE_OPTIONAL( -653, ChaiError.UNKNOWN ),
        PARTITION_BUSY( -654, ChaiError.UNKNOWN ),
        MULTIPLE_REPLICAS( -655, ChaiError.UNKNOWN ),
        CRUCIAL_REPLICA( -656, ChaiError.UNKNOWN ),
        SCHEMA_SYNC_IN_PROGRESS( -657, ChaiError.UNKNOWN ),
        SKULK_IN_PROGRESS( -658, ChaiError.UNKNOWN ),
        TIME_NOT_SYNCHRONIZED( -659, ChaiError.UNKNOWN ),
        RECORD_IN_USE( -660, ChaiError.UNKNOWN ),
        DS_VOLUME_NOT_MOUNTED( -661, ChaiError.UNKNOWN ),
        DS_VOLUME_IO_FAILURE( -662, ChaiError.UNKNOWN ),
        DS_LOCKED( -663, ChaiError.UNKNOWN, Flag.Temporary ),
        OLD_EPOCH( -664, ChaiError.UNKNOWN ),
        NEW_EPOCH( -665, ChaiError.UNKNOWN ),
        INCOMPATIBLE_DS_VERSION( -666, ChaiError.UNKNOWN ),
        PARTITION_ROOT( -667, ChaiError.UNKNOWN ),
        ENTRY_NOT_CONTAINER( -668, ChaiError.UNKNOWN ),
        FAILED_AUTHENTICATION( -669, ChaiError.FAILED_AUTHENTICATION, Flag.Authentication ),
        INVALID_CONTEXT( -670, ChaiError.UNKNOWN ),
        NO_SUCH_PARENT( -671, ChaiError.UNKNOWN ),
        NO_ACCESS( -672, ChaiError.NO_ACCESS ),
        REPLICA_NOT_ON( -673, ChaiError.UNKNOWN ),
        INVALID_NAME_SERVICE( -674, ChaiError.UNKNOWN ),
        INVALID_TASK( -675, ChaiError.UNKNOWN ),
        INVALID_CONN_HANDLE( -676, ChaiError.UNKNOWN ),
        INVALID_IDENTITY( -677, ChaiError.UNKNOWN ),
        DUPLICATE_ACL( -678, ChaiError.UNKNOWN ),
        PARTITION_ALREADY_EXISTS( -679, ChaiError.UNKNOWN ),
        TRANSPORT_MODIFIED( -680, ChaiError.UNKNOWN ),
        ALIAS_OF_AN_ALIAS( -681, ChaiError.UNKNOWN ),
        AUDITING_FAILED( -682, ChaiError.UNKNOWN ),
        INVALID_API_VERSION( -683, ChaiError.UNKNOWN ),
        SECURE_NCP_VIOLATION( -684, ChaiError.UNKNOWN ),
        MOVE_IN_PROGRESS( -685, ChaiError.UNKNOWN ),
        NOT_LEAF_PARTITION( -686, ChaiError.UNKNOWN ),
        CANNOT_ABORT( -687, ChaiError.UNKNOWN ),
        CACHE_OVERFLOW( -688, ChaiError.UNKNOWN ),
        INVALID_SUBORDINATE_COUNT( -689, ChaiError.UNKNOWN ),
        INVALID_RDN( -690, ChaiError.UNKNOWN ),
        MOD_TIME_NOT_CURRENT( -691, ChaiError.UNKNOWN ),
        INCORRECT_BASE_CLASS( -692, ChaiError.UNKNOWN ),
        MISSING_REFERENCE( -693, ChaiError.UNKNOWN ),
        LOST_ENTRY( -694, ChaiError.UNKNOWN ),
        AGENT_ALREADY_REGISTERED( -695, ChaiError.UNKNOWN ),
        DS_LOADER_BUSY( -696, ChaiError.UNKNOWN ),
        DS_CANNOT_RELOAD( -697, ChaiError.UNKNOWN ),
        REPLICA_IN_SKULK( -698, ChaiError.UNKNOWN ),
        FATAL_ERR( -699, ChaiError.UNKNOWN ),
        OBSOLETE_API( -700, ChaiError.UNKNOWN ),
        SYNCHRONIZATION_DISABLED( -701, ChaiError.UNKNOWN ),
        INVALID_PARAMETER( -702, ChaiError.UNKNOWN ),
        DUPLICATE_TEMPLATE( -703, ChaiError.UNKNOWN ),
        NO_MASTER_REPLICA( -704, ChaiError.UNKNOWN ),
        DUPLICATE_CONTAINMENT( -705, ChaiError.UNKNOWN ),
        NOT_SIBLING( -706, ChaiError.UNKNOWN ),
        INVALID_SIGNATURE( -707, ChaiError.UNKNOWN ),
        INVALID_RESPONSE( -708, ChaiError.UNKNOWN ),
        INSUFFICIENT_SOCKETS( -709, ChaiError.UNKNOWN ),
        DATABASE_READ_FAIL( -710, ChaiError.UNKNOWN ),
        INVALID_CODE_PAGE( -711, ChaiError.UNKNOWN ),
        INVALID_ESCAPE_CHAR( -712, ChaiError.UNKNOWN ),
        INVALID_DELIMITERS( -713, ChaiError.UNKNOWN ),
        NOT_IMPLEMENTED( -714, ChaiError.UNKNOWN ),
        CHECKSUM_FAILURE( -715, ChaiError.UNKNOWN ),
        CHECKSUMMING_NOT_SUPPORTED( -716, ChaiError.UNKNOWN ),
        CRC_FAILURE( -717, ChaiError.UNKNOWN ),
        INVALID_ENTRY_HANDLE( -718, ChaiError.UNKNOWN ),
        INVALID_VALUE_HANDLE( -719, ChaiError.UNKNOWN ),
        CONNECTION_DENIED( -720, ChaiError.UNKNOWN ),
        NO_SUCH_FEDERATION_LINK( -721, ChaiError.UNKNOWN ),
        OP_SCHEMA_MISMATCH( -722, ChaiError.UNKNOWN ),
        STREAM_NOT_FOUND( -723, ChaiError.UNKNOWN ),
        DCLIENT_UNAVAILABLE( -724, ChaiError.UNKNOWN ),
        MASV_NO_ACCESS( -725, ChaiError.UNKNOWN ),
        MASV_INVALID_REQUEST( -726, ChaiError.UNKNOWN ),
        MASV_FAILURE( -727, ChaiError.UNKNOWN ),
        MASV_ALREADY_EXISTS( -728, ChaiError.UNKNOWN ),
        MASV_NOT_FOUND( -729, ChaiError.UNKNOWN ),
        MASV_BAD_RANGE( -730, ChaiError.UNKNOWN ),
        VALUE_DATA( -731, ChaiError.UNKNOWN ),
        DATABASE_LOCKED( -732, ChaiError.UNKNOWN ),
        DATABASE_ALREADY_EXISTS( -733, ChaiError.UNKNOWN ),
        DATABASE_NOT_FOUND( -734, ChaiError.UNKNOWN ),
        NOTHING_TO_ABORT( -735, ChaiError.UNKNOWN ),
        END_OF_STREAM( -736, ChaiError.UNKNOWN ),
        NO_SUCH_TEMPLATE( -737, ChaiError.UNKNOWN ),
        SAS_LOCKED( -738, ChaiError.UNKNOWN ),
        INVALID_SAS_VERSION( -739, ChaiError.UNKNOWN ),
        SAS_ALREADY_REGISTERED( -740, ChaiError.UNKNOWN ),
        NAME_TYPE_NOT_SUPPORTED( -741, ChaiError.UNKNOWN ),
        WRONG_DS_VERSION( -742, ChaiError.UNKNOWN ),
        INVALID_CONTROL_FUNCTION( -743, ChaiError.UNKNOWN ),
        INVALID_CONTROL_STATE( -744, ChaiError.UNKNOWN ),
        ERR_CACHE_IN_USE( -745, ChaiError.UNKNOWN ),
        ERR_ZERO_CREATION_TIME( -746, ChaiError.UNKNOWN ),
        ERR_WOULD_BLOCK( -747, ChaiError.UNKNOWN ),
        ERR_CONN_TIMEOUT( -748, ChaiError.UNKNOWN ),
        ERR_TOO_MANY_REFERRALS( -749, ChaiError.UNKNOWN ),
        ERR_OPERATION_CANCELLED( -750, ChaiError.UNKNOWN ),
        ERR_UNKNOWN_TARGET( -751, ChaiError.UNKNOWN ),
        ERR_GUID_FAILURE( -752, ChaiError.UNKNOWN ),
        ERR_INCOMPATIBLE_OS( -753, ChaiError.UNKNOWN ),
        ERR_CALLBACK_CANCEL( -754, ChaiError.UNKNOWN ),
        ERR_INVALID_SYNC_DATA( -755, ChaiError.UNKNOWN ),
        ERR_STREAM_EXISTS( -756, ChaiError.UNKNOWN ),
        ERR_AUXILIARY_HAS_CONTAINMENT( -757, ChaiError.UNKNOWN ),
        ERR_AUXILIARY_NOT_CONTAINER( -758, ChaiError.UNKNOWN ),
        ERR_AUXILIARY_NOT_EFFECTIVE( -759, ChaiError.UNKNOWN ),
        ERR_AUXILIARY_ON_ALIAS( -760, ChaiError.UNKNOWN ),
        ERR_HAVE_SEEN_STATE( -761, ChaiError.UNKNOWN ),
        ERR_VERB_LOCKED( -762, ChaiError.UNKNOWN ),
        ERR_VERB_EXCEEDS_TABLE_LENGTH( -763, ChaiError.UNKNOWN ),
        ERR_BOF_HIT( -764, ChaiError.UNKNOWN ),
        ERR_EOF_HIT( -765, ChaiError.UNKNOWN ),
        ERR_INCOMPATIBLE_REPLICA_VER( -766, ChaiError.UNKNOWN ),
        ERR_QUERY_TIMEOUT( -767, ChaiError.UNKNOWN ),
        ERR_QUERY_MAX_COUNT( -768, ChaiError.UNKNOWN ),
        ERR_DUPLICATE_NAMING( -769, ChaiError.UNKNOWN ),
        ERR_NO_TRANS_ACTIVE( -770, ChaiError.UNKNOWN ),
        ERR_TRANS_ACTIVE( -771, ChaiError.UNKNOWN ),
        ERR_ILLEGAL_TRANS_OP( -772, ChaiError.UNKNOWN ),
        ERR_ITERATOR_SYNTAX( -773, ChaiError.UNKNOWN ),
        ERR_REPAIRING_DIB( -774, ChaiError.UNKNOWN ),
        ERR_INVALID_OID_FORMAT( -775, ChaiError.UNKNOWN ),
        ERR_DS_AGENT_CLOSING( -776, ChaiError.UNKNOWN ),
        ERR_SPARSE_FILTER_VIOLATION( -777, ChaiError.UNKNOWN ),
        ERR_VPVECTOR_CORRELATION_ERR( -778, ChaiError.UNKNOWN ),
        ERR_CANNOT_GO_REMOTE( -779, ChaiError.UNKNOWN ),
        ERR_REQUEST_NOT_SUPPORTED( -780, ChaiError.UNKNOWN ),
        ERR_ENTRY_NOT_LOCAL( -781, ChaiError.UNKNOWN ),
        ERR_ROOT_UNREACHABLE( -782, ChaiError.UNKNOWN ),
        ERR_VRDIM_NOT_INITIALIZED( -783, ChaiError.UNKNOWN ),
        ERR_WAIT_TIMEOUT( -784, ChaiError.UNKNOWN ),
        ERR_DIB_ERROR( -785, ChaiError.UNKNOWN ),
        ERR_DIB_IO_FAILURE( -786, ChaiError.UNKNOWN ),
        ERR_ILLEGAL_SCHEMA_ATTRIBUTE( -787, ChaiError.UNKNOWN ),
        ERR_SCHEMA_PARTITION( -788, ChaiError.UNKNOWN ),
        ERR_INVALID_TEMPLATE( -789, ChaiError.UNKNOWN ),
        ERR_OPENING_FILE( -790, ChaiError.UNKNOWN ),
        ERR_DIRECT_OPENING_FILE( -791, ChaiError.UNKNOWN ),
        ERR_CREATING_FILE( -792, ChaiError.UNKNOWN ),
        ERR_DIRECT_CREATING_FILE( -793, ChaiError.UNKNOWN ),
        ERR_READING_FILE( -794, ChaiError.UNKNOWN ),
        ERR_DIRECT_READING_FILE( -795, ChaiError.UNKNOWN ),
        ERR_WRITING_FILE( -796, ChaiError.UNKNOWN ),
        ERR_DIRECT_WRITING_FILE( -797, ChaiError.UNKNOWN ),
        ERR_POSITIONING_IN_FILE( -798, ChaiError.UNKNOWN ),
        ERR_GETTING_FILE_SIZE( -799, ChaiError.UNKNOWN ),

        UNSUPPORTED_OPERATION( Integer.MAX_VALUE, ChaiError.UNSUPPORTED_OPERATION ),
        UNKNOWN( -1, ChaiError.UNKNOWN ),;

        private enum Flag
        {
            Temporary,
            Authentication,
        }

        private final int edirErrorCode;
        private final ChaiError chaiErrorCode;
        private final boolean temporary;
        private final boolean authentication;

        EdirError(
                final int errorCodeNumber,
                final ChaiError chaiErrorCode,
                final Flag... flags
        )
        {
            this.edirErrorCode = errorCodeNumber;
            this.chaiErrorCode = chaiErrorCode;
            this.temporary = StringHelper.enumArrayContainsValue( flags, Flag.Temporary );
            this.authentication = StringHelper.enumArrayContainsValue( flags, Flag.Authentication );
        }

        public int getEdirErrorCode()
        {
            return edirErrorCode;
        }

        public boolean isPermanent()
        {
            return !temporary;
        }

        public boolean isAuthentication()
        {
            return authentication;
        }

        public ChaiError getChaiErrorCode()
        {
            return chaiErrorCode;
        }
    }
}
