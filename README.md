ldapchai
========
LDAP Chai is an easy-to-use Java LDAP interface.  LDAP Chai is a wrapper interface that makes working with LDAP simple and vendor agnostic.
  
LDAP Chai can wrap either the Java-provided JNDI LDAP interface or the open JLDAP library.

Contrasted to the Java JNDI LDAP interface, LDAP Chai does not require dealing with cumbersome classes like DirContext?, Attributes and NamingEnumerations?. Instead, nearly all arguments and return values are presented as Java Strings and use the standard Java Collections Framework of Lists, Sets and Maps. In fact, LDAP Chai helps remove much of the burdensome "boiler-plate" code often needed when dealing with LDAP APIs.

LDAP Chai is well suited for business applications and general purpose LDAP development

Features:

* Full use of Java 1.5 language enhancements including generics, enums and annotations
* LDAP multi-server failover support
* LDAP idle connection watch-dog automatically opens and closes connections based on activity
* Support for NMAS and non-NMAS (PWM) style challenge/response settings
* Simple methods for the most common operations, such as reading an int or Date.
* Pluggable connection implementation; included implementations wrap the standard Java JNDI ldap interface or JLDAP API.
* Included pluggable LDAP-vendor implementations include:
  * NetIQ eDirectory
  * Microsoft Active Directory
  * Directory Server 389
  * Oracle DS
  * OpenLDAP
* LDAP vendor agnostic error codes and error handling 
* LDAP vendor agnostic password handlers and password policy readers 
