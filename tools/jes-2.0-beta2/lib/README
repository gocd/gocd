dnsjava v2.0

http://www.xbill.org/dnsjava
http://www.dnsjava.org/

Author:

Brian Wellington (bwelling@xbill.org)
March 12, 2004

Overview:

dnsjava is an implementation of DNS in Java.  It supports all defined record
types (including the DNSSEC types), and unknown types.  It can be used for
queries, zone transfers, and dynamic updates.  It includes a cache which can be
used by clients, and an authoritative only server.  It supports TSIG
authenticated messages, partial DNSSEC verification, and EDNS0.  It is fully
thread safe.  It can be used to replace the native DNS support in Java.

dnsjava was started as an excuse to learn Java.  It was useful for testing new
features in BIND without rewriting the C resolver.  It was then cleaned up and
extended in order to be used as a testing framework for DNS interoperability
testing.  The high level API and caching resolver were added to make it useful
to a wider audience.  The authoritative only server was added as proof of
concept.


Getting started:

Run 'ant' from the toplevel directory to build dnsjava (a Makefile is also
provided, but does not have all of the features of the ant script).  JDK 1.4
or higher is required.

To compile name service provider support (org.xbill.DNS.spi), run 'ant spi'.


Replacing the standard Java DNS functionality:

Beginning with Java 1.4, service providers can be loaded at runtime.  To load
the dnsjava service provider, build it as explained above and set the system
property:

	sun.net.spi.nameservice.provider.1=dns,dnsjava

This instructs the JVM to use the dnsjava service provide for DNS at the
highest priority.


Testing dnsjava:

Matt Rutherford <rutherfo@cs.colorado.edu> contributed a number of unit
tests, which are in the tests subdirectory.  The hierarchy under tests
mirrors the org.xbill.DNS classes.  To build the unit tests, run
'ant compile_tests', and to run then, run 'ant run_tests'.  The tests require
JUnit (http://www.junit.org) to be installed.

Some high-level test programs are in org/xbill/DNS/tests.


Limitations:

There's no standard way to determine what the local nameserver or DNS search
path is at runtime from within the JVM.  dnsjava attempts several methods
until one succeeds.

 - The properties 'dns.server' and 'dns.search' (comma delimited lists) are
   checked.  The servers can either be IP addresses or hostnames (which are
   resolved using Java's built in DNS support).
 - The sun.net.dns.ResolverConfiguration class is queried.
 - On Unix, /etc/resolv.conf is parsed.
 - On Windows, ipconfig/winipcfg is called and its output parsed.  This may
   fail for non-English versions on Windows.
 - As a last resort, "localhost" is used as the nameserver, and the search
   path is empty.

The underlying platform must use an ASCII encoding of characters.  This means
that dnsjava will not work on OS/390, for example.


Additional documentation:

Javadoc documentation is provided in the doc/ subdirectory of binary
distributions, and can be built with 'ant docs'.


License:

dnsjava is placed under the BSD license.  Several files are also under
additional licenses; see the individual files for details.

Copyright (c) 1999-2005, Brian Wellington
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the dnsjava project nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


Final notes:

 - Thanks to Network Associates, Inc. for sponsoring some of the original
   dnsjava work in 1999-2000.

 - Thanks to Nominum, Inc. for sponsoring some work on dnsjava from 2000 to
   the present.
