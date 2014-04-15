#ifndef JRUBY_WIN32_H
#define	JRUBY_WIN32_H

#define	JRUBY_WIN32

#ifndef RUBY_DLLSPEC
#  define RUBY_DLLSPEC __declspec(dllimport)
#endif

#include <winsock.h>
#include <winsock2.h>
#include <malloc.h>

#endif
