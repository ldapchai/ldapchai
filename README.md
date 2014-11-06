ldapchai
========
LDAP Chai is an easy-to-use Java LDAP interface. Chai takes a java-centric view of working with LDAP, and provides easy to use methods for common and complex operations.

Contrasted to the Java JNDI LDAP interface, LDAP Chai does not require dealing with cumbersome classes like DirContext?, Attributes and NamingEnumerations?. Instead, nearly all arguments and return values are presented as Java Strings and use the standard Java Collections Framework of Lists, Sets and Maps. In fact, LDAP Chai helps remove much of the burdensome "boiler-plate" code often needed when dealing with LDAP APIs.

LDAP Chai is well suited for business applications and general purpose LDAP development

Features:

Full use of Java 1.5 language enhancements including generics, enums and annotations
LDAP multi-server failover support
LDAP connection watch dog, for managing high volume ldap connections.
Support for NMAS and non-NMAS (PWM) style challenge/response settings
Simple methods for the most common operations, such as reading an int or Date.
Pluggable connection implementation; included implementations wrap the standard Java JNDI ldap interface or JLDAP API.
Pluggable vendor implementation; included implementations wrap Microsoft Active Directory and Novell eDirectory
