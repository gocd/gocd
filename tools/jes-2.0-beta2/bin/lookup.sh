#! /bin/sh

echo "Performs a DNS Lookup using against a DNS server for a domain."
echo "Must be called from the directory where the script resides."
echo "Usage: ./lookup.sh <dns server address><dns search suffix><lookup record type>"
echo "         <domain to lookup>"
echo "Example: The dns server is located at address 192.168.1.1, the search suffix is "
echo "         example.com, the record type is MX (typical for a mail server) and the "
echo "         domain is ericdaugherty.com:"
echo "          ./lookup.sh 192.168.1.1 example.com MX ericdaugherty.com"
echo "Example: Letting dnsjava detect the settings and the lookup concerns a MX record"
echo "         of type MX for the ericdaugherty.com domain:"
echo "          ./lookup.sh \"\" \"\" MX ericdaugherty.com"
echo "Results:"


java -Ddns.server=$1 -Ddns.search=$2 -Ddnsjava.options=verbose=true -classpath ../lib/dnsjava-2.0.6.jar lookup -t $3 $4