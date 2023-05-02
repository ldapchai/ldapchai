# Changelog

## [0.8.5] - Released 2023-05-02
- Continued hash iteration count default improvements based on performance benchmarks
- Improved JNDI error handling for some concurrency scenarios
- Added support for concurrent hash generation during ResponseSet creation

## [0.8.4] - Released 2023-02-08
- Revert logging to Log4j and replace library with reload4j
  -  Will re-address SLF4J in a future update
- Update C/R hash iteration defaults to modern values
  - 1,000,000 for default PBKDF2/SHA512 method
- Improved lock management for fail-over wrapper

## [0.8.3] - Released 2022-10-30
- Change logging API to SLF4J
- Correct readGUID() for DirectoryServer389 to properly read nsUniqueId attribute
- Update dependencies
- Update build to improve reproducibility
+ Add directory handler for FreeIPA (thanks edvalley!)
+ Add directory handler for ApacheDS 
+ Add chaiEntry#hasChildren method

## [0.8.2] - Released 2022-12-01
- Swap jdom with chaixml libraries
- Swap external B64 library with JDK B64 library
- Update dependencies
- OpenLDAP error map fixes 
+ Javadoc additions and corrections
+ JDK 17 compatability 

