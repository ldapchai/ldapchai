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

package com.novell.ldapchai.impl.ad.entry;

import com.novell.ldapchai.*;
import com.novell.ldapchai.exception.ChaiErrors;
import com.novell.ldapchai.exception.ChaiOperationException;
import com.novell.ldapchai.exception.ChaiPasswordPolicyException;
import com.novell.ldapchai.exception.ChaiUnavailableException;
import com.novell.ldapchai.impl.AbstractChaiUser;
import com.novell.ldapchai.provider.ChaiProvider;
import com.novell.ldapchai.util.DefaultChaiPasswordPolicy;
import com.novell.ldapchai.util.SearchHelper;
import com.novell.ldapchai.util.StringHelper;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class UserImpl extends AbstractChaiUser implements User, Top, ChaiUser {

    private static final int COMPUTED_ACCOUNT_CONTROL_ACCOUNT_ACTIVE = 0x0002;
    private static final int COMPUTED_ACCOUNT_CONTROL_UC_LOCKOUT = 0x0010;
    private static final int COMPUTED_ACCOUNT_CONTROL_UC_PASSWORD_EXPIRED = 0x800000;
    private static final int ADS_UF_DONT_EXPIRE_PASSWD = 0x10000;

    UserImpl(final String userDN, final ChaiProvider chaiProvider)
    {
        super(userDN, chaiProvider);
    }

    public void addGroupMembership(ChaiGroup theGroup) throws ChaiOperationException, ChaiUnavailableException {
        theGroup.addAttribute("member",this.getEntryDN());
    }

    public ChaiPasswordPolicy getPasswordPolicy() throws ChaiUnavailableException, ChaiOperationException {

        final Map<String, String> policyMap = new LinkedHashMap<String, String>();

        // defaults for ad policy
        policyMap.put(ChaiPasswordRule.AllowNumeric.getKey(), String.valueOf(true));
        policyMap.put(ChaiPasswordRule.AllowSpecial.getKey(), String.valueOf(true));
        policyMap.put(ChaiPasswordRule.CaseSensitive.getKey(), String.valueOf(true));

        //read minimum password length from domain
        final Matcher domainMatcher = Pattern.compile("(dc=[a-z0-9-]+[,]*)+", Pattern.CASE_INSENSITIVE).matcher(this.getEntryDN());
        if (domainMatcher.find()) {
            final String domainDN = domainMatcher.group();
            final String minPwdLength = this.getChaiProvider().readStringAttribute(domainDN, "minPwdLength");
            if (minPwdLength != null && minPwdLength.length() > 0) {
                policyMap.put(ChaiPasswordRule.MinimumLength.getKey(), minPwdLength);
            }
        }

        // Read PSO policy object.
        final String psoObject = this.readStringAttribute(ChaiConstant.ATTR_AD_PASSWORD_POLICY_RESULTANT_PSO);
        if (psoObject != null && psoObject.length() > 0) {
            final MsDSPasswordSettingsImpl msDSPasswordSetting = new MsDSPasswordSettingsImpl(psoObject,this.getChaiProvider());
            for (final String loopKey : msDSPasswordSetting.getKeys()) {
                policyMap.put(loopKey, msDSPasswordSetting.getValue(loopKey));
            }
        }

        return DefaultChaiPasswordPolicy.createDefaultChaiPasswordPolicy(policyMap);
    }

    public String readPassword() throws ChaiUnavailableException, ChaiOperationException {
        throw new UnsupportedOperationException("ChaiUser#readPassword not implemented in ad-impl ldapChai API");
    }

    public void removeGroupMembership(ChaiGroup theGroup) throws ChaiOperationException, ChaiUnavailableException {
        theGroup.deleteAttribute("member",this.getEntryDN());
    }

    public boolean testPassword(String passwordValue) throws ChaiUnavailableException, ChaiPasswordPolicyException {
        throw new UnsupportedOperationException("ChaiUser#testPassword not implemented in ad-impl ldapChai API");
    }

    public boolean testPasswordPolicy(String testPassword) throws ChaiUnavailableException, ChaiPasswordPolicyException {
        return false;
    }

    public void unlock()
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.writeStringAttribute("lockoutTime","0");
    }

    public void setPassword(final String newPassword)
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException
    {
        final String quotedPwd = '"' + newPassword + '"';
        final byte[]  littleEndianEncodedPwd;
        try {
            littleEndianEncodedPwd = quotedPwd.getBytes("UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("unexpected error, missing 'UTF-16LE' character encoder",e);
        }
        final byte[][] multiBA = new byte[][] { littleEndianEncodedPwd };

        writeBinaryAttribute("unicodePwd",multiBA);
    }

    public boolean isPasswordExpired()
            throws ChaiUnavailableException, ChaiOperationException
    {
        // modern versions of ad have a (somewhat) sane way of checking account lockout; heaven forbid a boolean attribute.
        final String computedBit = readStringAttribute("msDS-User-Account-Control-Computed");
        if (computedBit != null && computedBit.length() > 0) {
            final int intValue = Integer.parseInt(computedBit);
            return ((intValue & COMPUTED_ACCOUNT_CONTROL_UC_PASSWORD_EXPIRED) == COMPUTED_ACCOUNT_CONTROL_UC_PASSWORD_EXPIRED);
        }

        return false;

    }


    public final String readGivenName()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readStringAttribute(ATTR_GIVEN_NAME);
    }

    public final Date readLastLoginTime()
            throws ChaiOperationException, ChaiUnavailableException
    {
        return this.readDateAttribute(ATTR_LAST_LOGIN);
    }

    public final void changePassword(final String oldPassword, final String newPassword)
            throws ChaiUnavailableException, ChaiPasswordPolicyException, ChaiOperationException {
        final String quotedOldPwd = '"' + oldPassword + '"';
        final String quotedNewPwd = '"' + newPassword + '"';
        final byte[] littleEndianEncodedOldPwd;
        final byte[] littleEndianEncodedNewPwd;
        try {
            littleEndianEncodedOldPwd = quotedOldPwd.getBytes("UTF-16LE");
            littleEndianEncodedNewPwd = quotedNewPwd.getBytes("UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("unexpected error, missing 'UTF-16LE' character encoder",e);
        }

        try {
            replaceBinaryAttribute("unicodePwd", littleEndianEncodedOldPwd, littleEndianEncodedNewPwd);
        } catch (ChaiOperationException e) {
            throw new ChaiPasswordPolicyException(e.getMessage(), ChaiErrors.getErrorForMessage(e.getMessage()));
        }

    }

    public void expirePassword()
            throws ChaiOperationException, ChaiUnavailableException
    {
        this.writeStringAttribute(ChaiConstant.ATTR_LDAP_LOGIN_INTRUDER_RESET_TIME, "19700101010101Z");
    }

    public boolean isLocked()
            throws ChaiOperationException, ChaiUnavailableException
    {
        // modern versions of ad have a (somewhat) sane way of checking account lockout; heaven forbid a boolean attribute.
        final String computedBit = readStringAttribute("msDS-User-Account-Control-Computed");
        if (computedBit != null && computedBit.length() > 0) {
            final int intValue = Integer.parseInt(computedBit);
            return ((intValue & COMPUTED_ACCOUNT_CONTROL_UC_LOCKOUT) == COMPUTED_ACCOUNT_CONTROL_UC_LOCKOUT);
        }

        // older ad versions have an insane way of checking account lockout.  what could possibly go wrong?
        final Date lockoutTime = this.readDateAttribute("lockoutTime");  // read lockout time of user.
        if (lockoutTime != null) {
            ChaiEntry parentEntry = this.getParentEntry();
            long lockoutDurationMs = 0;
            int recursionCount  = 0; // should never need this, but provided for sanity
            while (lockoutDurationMs == 0 && parentEntry != null && recursionCount < 50) {
                if (parentEntry.compareStringAttribute("objectClass","domainDNS")) { // find the domain dns parent entry of the user
                    lockoutDurationMs = Long.parseLong(parentEntry.readStringAttribute("lockoutDuration")); // read the duration of lockouts from the domainDNS entry
                    lockoutDurationMs = Math.abs(lockoutDurationMs); // why is it stored as a negative value?  who knows.
                    lockoutDurationMs = lockoutDurationMs / 10000; // convert from 100 nanosecond intervals to milliseconds.  It's important that intruders don't sneak into the default 30 minute window a few nanoseconds early.  Thanks again MS.
                }
                parentEntry = parentEntry.getParentEntry();
                recursionCount++;
            }

            final Date futureUnlockTime = new Date(lockoutTime.getTime() + lockoutDurationMs);
            return System.currentTimeMillis() <= futureUnlockTime.getTime();
        }
        return false;
    }

    public Date readPasswordModificationDate() throws ChaiOperationException, ChaiUnavailableException {
        return this.readDateAttribute("pwdLastSet");
    }

    public Date readPasswordExpirationDate()
            throws ChaiUnavailableException, ChaiOperationException
    {
        final String[] attrsToRead = new String[] {"pwdLastSet", "userAccountControl" };

        final Map<String,String> readAttrs = readStringAttributes(new HashSet<String>(Arrays.asList(attrsToRead)));

        final String uacStrValue = readAttrs.get("userAccountControl");

        if (uacStrValue != null && uacStrValue.length() > 0) {
            final int intValue = StringHelper.convertStrToInt(uacStrValue,0);
            if ((ADS_UF_DONT_EXPIRE_PASSWD & intValue) == ADS_UF_DONT_EXPIRE_PASSWD) {
                //user password does not expire
                return null;
            }
        }

        // now read domain object
        long maxPwdAgeMs = 0;
        {
            final String maxPwdAgeString = readDomainValue("maxPwdAge");
            if (maxPwdAgeString != null && maxPwdAgeString.length() > 0) {
                long v = Long.parseLong(maxPwdAgeString);
                v = Math.abs(v); // why is it stored as a negative value?  who knows.
                v = v / 10000; // convert from 100 nanosecond intervals to milliseconds.  It's important that intruders don't sneak into the default 30 minute window a few nanoseconds early.  Thanks again MS.
                maxPwdAgeMs = v;
            }
        }

        if (maxPwdAgeMs == 0) {
            return null;  // passwords never expire according to the domain policy.
        }

        long pwdLastSetMs = 0;
        {
            final String maxPwdAgeString = readAttrs.get("pwdLastSet");
            if (maxPwdAgeString != null && maxPwdAgeString.length() > 0) {
                long v = Long.parseLong(maxPwdAgeString);
                v = Math.abs(v); // why is it stored as a negative value?  who knows.
                v = v / 10000; // convert from 100 nanosecond intervals to milliseconds.  It's important that intruders don't sneak into the default 30 minute window a few nanoseconds early.  Thanks again MS.
                pwdLastSetMs = v;
            }
        }

        final long pwdExpirateTimeMs = pwdLastSetMs + maxPwdAgeMs;
        return new Date(pwdExpirateTimeMs);
    }

    private String readDomainValue(final String attribute) throws ChaiUnavailableException, ChaiOperationException {
        ChaiEntry parentEntry = this.getParentEntry();
        int recursionCount  = 0; // should never need this, but provided for sanity
        while (parentEntry != null && recursionCount < 50) {
            if (parentEntry.compareStringAttribute("objectClass","domainDNS")) { // find the domain dns parent entry of the user
                return parentEntry.readStringAttribute(attribute); // read the desired attribute.
            }
            parentEntry = parentEntry.getParentEntry();
            recursionCount++;
        }
        return null;
    }

    @Override
    public Date readDateAttribute(final String attributeName) throws ChaiUnavailableException, ChaiOperationException {
        return ADEntries.readDateAttribute(this, attributeName);
    }

    @Override
    public void writeDateAttribute(final String attributeName, final Date date) throws ChaiUnavailableException, ChaiOperationException {
        ADEntries.writeDateAttribute(this, attributeName, date);
    }

    @Override
    public String readGUID() throws ChaiOperationException, ChaiUnavailableException {
        return ADEntries.readGUID(this);
    }

    public boolean isAccountEnabled() throws ChaiOperationException, ChaiUnavailableException {
        final String disabledUserSearchFilter = "(useraccountcontrol:1.2.840.113556.1.4.803:=2)";
        final SearchHelper searchHelper = new SearchHelper();
        searchHelper.setFilter(disabledUserSearchFilter);
        searchHelper.setSearchScope(ChaiProvider.SEARCH_SCOPE.BASE);
        final Map<String, Map<String, String>> results = this.getChaiProvider().search(this.getEntryDN(),searchHelper);
        for (final String resultDN : results.keySet()) {
            if (resultDN != null && resultDN.equals(this.getEntryDN())) {
                return false;
            }
        }
        return true;
    }
}
