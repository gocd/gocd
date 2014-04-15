@echo off

rem This script performs a DNS Lookup against the specified
rem server for a given domain.
rem usage: lookup.bat <dns server address> <domain to lookup>

@echo Performs a DNS Lookup using against a DNS server for a domain.
@echo usage: lookup.bat (dns server address) (domain to lookup)
@echo Results:


java -Ddns.server=%1 -classpath ../lib/lookup.class;../lib/dnsjava.jar lookup -t MX %2


