# This Makefile has only been tested on linux.  It uses
# MinGW32 to cross-compile for windows.  To install and
# configure MinGW32 on linux, see
# http://www.mingw.org/MinGWiki/index.php/BuildMingwCross
#
# Marty Lamb

CC=gcc
WIN32_CC=/usr/local/mingw32/bin/mingw32-gcc
CFLAGS=-Wall -pedantic -s -O3

ng: src/c/ng.c
	@echo "Building ng client.  To build a Windows binary, type 'make ng.exe'"
	${CC} ${CFLAGS} -o ng src/c/ng.c

ng.exe: src/c/ng.c
	# any idea why the command line is so sensitive to the order of
	# the arguments?  If CFLAGS is at the beginning, it won't link.
	${WIN32_CC} -o ng.exe src/c/ng.c -lwsock32 -O3 ${CFLAGS}
	
clean:
	@echo "If you have a Windows binary, 'make clean' won't delete it."
	@echo "You must remove this manually.  Most users won't have MinGW"
	@echo "installed - so I'd rather not delete something they can't rebuild."
	rm ng
#	rm ng.exe
