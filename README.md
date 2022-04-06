ldapchai
========

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.ldapchai/ldapchai/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.ldapchai/ldapchai/)

[![Javadocs](https://www.javadoc.io/badge/com.github.ldapchai/ldapchai.svg)](https://www.javadoc.io/doc/com.github.ldapchai/ldapchai)


LDAP Chai is an easy-to-use Java LDAP API.  It wraps low-level Java interfaces such as the JNDI API with
easy to use interfaces.  LDAPChai Includes the following features:

* LDAP Vendor abstraction - no vendor specific code needed
  * Password changes
  * Password expiration
  * Password policy reader
  * Error code normalization
  * Group read/write management
  * Account status (expiration, intruder lock) checks and resets
  * Normalized value reading and writing for date/timestamp, int, string and binary types 
* Vendors supported:
  * NetIQ eDirectory
  * Microsoft Active Directory
  * Directory Server 389
  * Oracle DS
  * OpenLDAP
  * Generic LDAP 
* LDAP multi-server failover support                                                                                      
* LDAP idle connection watch-dog automatically opens and closes connections based on activity
* Pagination support for handling large queries from AD and other page-limited LDAP servers.
* Pluggable LDAP API Provider support
  * JDK included JNDI-LDAP interface (default)
  * JLDAP
  * Apache Directory LDAP API 
  * Access to provider if there is a need to bypass chai API.
  
LDAP Chai is well suited for business applications and general purpose LDAP development.

All operations to LDAP Chai are request/response (no long term iterators on search) so that fail-over can
happen seamlessly. 


### 0.7 Update:

The 0.7 update changes the API in several ways that are not backward compatible:

* Minimum Java level increased to Java 8
* The factories are no longer static and must be instantiated and have their own lifecycle.
* ChaiFactory has been replaced with ChaiEntryFactory (which can be obtained via ChaiProvider.getEntryFactory())
* Deprecated APIs have been removed
* ChaiEntry:isValid() was renamed to ChaiEntry:exists()
