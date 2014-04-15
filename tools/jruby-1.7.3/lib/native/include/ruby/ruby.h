/**********************************************************************

  ruby/ruby.h -

  $Author: yugui $
  created at: Thu Jun 10 14:26:32 JST 1993

  Copyright (C) 1993-2008 Yukihiro Matsumoto
  Copyright (C) 2000  Network Applied Communication Laboratory, Inc.
  Copyright (C) 2000  Information-technology Promotion Agency, Japan

**********************************************************************/

#ifndef JRUBY_RUBY_H
#define JRUBY_RUBY_H
#define JRUBY

#include <stdint.h>
#include <limits.h>
#include <assert.h>

// A number of extensions expect these to be already included
#include <stddef.h>
#include <stdlib.h>
#include <sys/time.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <ctype.h>

#include <st_sizes.h>

// Some platform specific includes
#if defined(__WIN32__) || defined(__MINGW32__)
#   include "jruby_win32.h"
#else
#   define RUBY_DLLSPEC
#       include <sys/select.h>
#       include <pthread.h>
#endif

#ifdef RUBY_EXTCONF_H
#include RUBY_EXTCONF_H
#endif

#ifdef  __cplusplus
extern "C" {
#if 0
} /* satisfy cc-mode */
#endif
#endif

#ifndef  HAVE_PROTOTYPES
# define HAVE_PROTOTYPES 1
#endif
#ifndef  HAVE_STDARG_PROTOTYPES
# define HAVE_STDARG_PROTOTYPES 1
#endif

#ifndef NORETURN
#define NORETURN(x) __attribute__ ((noreturn)) x
#endif

#ifndef EXTERN
#define EXTERN extern
#endif

#undef _
#ifdef HAVE_PROTOTYPES
# define _(args) args
#else
# define _(args) ()
#endif

#undef __
#ifdef HAVE_STDARG_PROTOTYPES
# define __(args) args
#else
# define __(args) ()
#endif

#ifdef __cplusplus
# define ANYARGS ...
#else
# define ANYARGS
#endif

#define LONG_LONG long long

/** In MRI, ID represents an interned string, i.e. a Symbol. */
typedef uintptr_t ID;
/** In MRI, VALUE represents an object. */
typedef uintptr_t VALUE;
typedef intptr_t SIGNED_VALUE;

#ifndef RSHIFT
# define RSHIFT(x,y) ((x)>>(int)y)
#endif

/** Zero out N elements of type starting at given pointer. */
#define MEMZERO(p,type,n) memset((p), 0, (sizeof(type) * (n)))
/** Copies n objects of type from p2 to p1. Behavior is undefined if objects overlap. */
#define MEMCPY(p1,p2,type,n) memcpy((p1), (p2), sizeof(type)*(n))
/** Copies n objects of type from p2 to p1. Objects may overlap. */
#define MEMMOVE(p1,p2,type,n) memmove((p1), (p2), sizeof(type)*(n))
/** Compares n objects of type. */
#define MEMCMP(p1,p2,type,n) memcmp((p1), (p2), sizeof(type)*(n))

#define FIXNUM_MAX (LONG_MAX>>1)
#define FIXNUM_MIN RSHIFT((long)LONG_MIN,1)

#define FIXNUM_P(f) (((SIGNED_VALUE)(f))&FIXNUM_FLAG)
#define POSFIXABLE(f) ((f) < FIXNUM_MAX+1)
#define NEGFIXABLE(f) ((f) >= FIXNUM_MIN)
#define FIXABLE(f) (POSFIXABLE(f) && NEGFIXABLE(f))

#define IMMEDIATE_MASK 0x3
#define IMMEDIATE_P(x) ((VALUE)(x) & IMMEDIATE_MASK)
#define SPECIAL_CONST_P(x) (IMMEDIATE_P(x) || !RTEST(x))

#define FIXNUM_FLAG 0x1
#define SYMBOL_FLAG 0x0e
#define SYMBOL_P(x) (((VALUE)(x)&0xff)==SYMBOL_FLAG)
#define ID2SYM(x) ((VALUE)(((long)(x))<<8|SYMBOL_FLAG))
#define SYM2ID(x) RSHIFT((unsigned long)x,8)

#define OBJ_FROZEN(x) jruby_obj_frozen(x)
#define OBJ_INFECT(o1, o2) jruby_infect((o1), (o2))
#define OBJ_TAINT(obj) rb_obj_taint((obj))
#define OBJ_TAINTED(obj) rb_obj_tainted((obj))

#define BUILTIN_TYPE(handle) rb_type(handle)

/** The false object. */
#define Qfalse ((VALUE)0)
/** The true object. */
#define Qtrue  ((VALUE)2)
/** The nil object. */
#define Qnil   ((VALUE)4)
/** The undef object. Value for placeholder */
#define Qundef ((VALUE)6)

struct RBasic {
    VALUE unused0;
    VALUE unused1;
};

struct RString {
    struct RBasic basic;
    long len;
    char *ptr;
    long capa;
};

struct RArray {
    struct RBasic basic;
    long len;
    union {
        long capa;
        VALUE shared;
    } aux;
    VALUE *ptr;
};

struct RIO {
    int fd;
    FILE* f;
    int mode; /* mode flags: FMODE_XXXs */
    VALUE obj;
};

typedef struct RIO rb_io_t;
#if defined(__WIN32__) || defined(__MINGW32__)
#else
    typedef struct RIO OpenFile; // 1.8 compat
#endif
#define HAVE_RB_IO_T 1
#define FMODE_READABLE              1
#define FMODE_WRITABLE              2
#define FMODE_READWRITE             3
#define FMODE_BINMODE               4
#define FMODE_APPEND                64

struct RFloat {
    struct RBasic basic;
    double value;
};

struct RData {
    struct RBasic basic;
    void (*dmark)(void *);
    void (*dfree)(void *);
    void* data;
};

typedef enum JRubyType {
    T_NONE,
    T_NIL,
    T_OBJECT,
    T_CLASS,
    T_ICLASS,
    T_MODULE,
    T_FLOAT,
    T_STRING,
    T_REGEXP,
    T_ARRAY,
    T_FIXNUM,
    T_HASH,
    T_STRUCT,
    T_BIGNUM,
    T_FILE,

    T_TRUE,
    T_FALSE,
    T_DATA,
    T_MATCH,
    T_SYMBOL,

    T_BLKTAG,
    T_UNDEF,
    T_VARMAP,
    T_SCOPE,
    T_NODE,
} JRubyType;

#define T_MASK (0x1f)


#ifdef __GNUC__
#define rb_special_const_p(obj) \
    __extension__ ({VALUE special_const_obj = (obj); (int)(SPECIAL_CONST_P(special_const_obj) ? Qtrue : Qfalse);})
#else
static inline int
rb_special_const_p(VALUE obj)
{
    if (SPECIAL_CONST_P(obj)) return (int)Qtrue;
    return (int)Qfalse;
}
#endif

RUBY_DLLSPEC int rb_type(VALUE);
RUBY_DLLSPEC void rb_check_type(VALUE, int);
#define Check_Type(v,t) rb_check_type((VALUE)(v),t)

#define xmalloc ruby_xmalloc
#define xmalloc2 ruby_xmalloc2
#define xcalloc ruby_xcalloc
#define xrealloc ruby_xrealloc
#define xrealloc2 ruby_xrealloc2
#define xfree ruby_xfree

RUBY_DLLSPEC void *xmalloc(size_t);
RUBY_DLLSPEC void *xmalloc2(size_t,size_t);
RUBY_DLLSPEC void *xcalloc(size_t,size_t);
RUBY_DLLSPEC void *xrealloc(void*,size_t);
RUBY_DLLSPEC void *xrealloc2(void*,size_t,size_t);
RUBY_DLLSPEC void xfree(void*);

/* need to include <ctype.h> to use these macros */
#ifndef ISPRINT
#define ISASCII(c) isascii((int)(unsigned char)(c))
#undef ISPRINT
#define ISPRINT(c) (ISASCII(c) && isprint((int)(unsigned char)(c)))
#define ISSPACE(c) (ISASCII(c) && isspace((int)(unsigned char)(c)))
#define ISUPPER(c) (ISASCII(c) && isupper((int)(unsigned char)(c)))
#define ISLOWER(c) (ISASCII(c) && islower((int)(unsigned char)(c)))
#define ISALNUM(c) (ISASCII(c) && isalnum((int)(unsigned char)(c)))
#define ISALPHA(c) (ISASCII(c) && isalpha((int)(unsigned char)(c)))
#define ISDIGIT(c) (ISASCII(c) && isdigit((int)(unsigned char)(c)))
#define ISXDIGIT(c) (ISASCII(c) && isxdigit((int)(unsigned char)(c)))
#endif

/* Interface macros */

/** Allocate memory for type. Must NOT be used to allocate Ruby objects. */
#define ALLOC(type) (type*)xmalloc(sizeof(type))

/** Allocate memory for N of type. Must NOT be used to allocate Ruby objects. */
#define ALLOC_N(type,n) (type*)xmalloc(sizeof(type)*(n))

/** Reallocate memory allocated with ALLOC or ALLOC_N. */
#define REALLOC_N(var,type,n) (var)=(type*)xrealloc((char*)(var),sizeof(type)*(n))

/** Interrupt checking (no-op). */
#define CHECK_INTS        /* No-op */

/** Test macros */
#define RTEST(v) (((v) & ~Qnil) != 0)
#define NIL_P(v) ((v) == Qnil)
#define TYPE(x) rb_type((VALUE)(x))
#define CLASS_OF(x) rb_class_of((VALUE)(x))

#define RB_TYPE_P(obj, type) ( \
	((type) == T_FIXNUM) ? FIXNUM_P(obj) : \
	((type) == T_TRUE) ? ((obj) == Qtrue) : \
	((type) == T_FALSE) ? ((obj) == Qfalse) : \
	((type) == T_NIL) ? ((obj) == Qnil) : \
	((type) == T_UNDEF) ? ((obj) == Qundef) : \
	((type) == T_SYMBOL) ? SYMBOL_P(obj) : \
	(!SPECIAL_CONST_P(obj) && BUILTIN_TYPE(obj) == (type)))

#ifdef __GNUC__
#define rb_type_p(obj, type) \
    __extension__ (__builtin_constant_p(type) ? RB_TYPE_P((obj), (type)) : \
		   rb_type(obj) == (type))
#else
#define rb_type_p(obj, type) (rb_type(obj) == (type))
#endif


/** The length of string str. */
#define RSTRING_LEN(str)  jruby_str_length((str))
/** The pointer to the string str's data. */
#define RSTRING_PTR(str)  jruby_str_cstr((str))
/** Pointer to the MRI string structure */
#define RSTRING(str) jruby_rstring((str))
#define STR2CSTR(str)         rb_str2cstr((VALUE)(str), 0)
/** Modifies the VALUE object in place by calling rb_obj_as_string(). */
#define StringValue(v)        rb_string_value(&(v))
#define StringValuePtr(v)     rb_string_value_ptr(&(v))
#define StringValueCStr(str)  rb_string_value_cstr(&(str))

RUBY_DLLSPEC void rb_check_safe_obj(VALUE);
RUBY_DLLSPEC void rb_check_safe_str(VALUE);
#define SafeStringValue(v) do {\
    StringValue(v);\
    rb_check_safe_obj(v);\
} while (0)
/* obsolete macro - use SafeStringValue(v) */
#define Check_SafeStr(v) rb_check_safe_str((VALUE)(v))

RUBY_DLLSPEC VALUE rb_str_export(VALUE);
#define ExportStringValue(v) do {\
    SafeStringValue(v);\
   (v) = rb_str_export(v);\
} while (0)
RUBY_DLLSPEC VALUE rb_str_export_locale(VALUE);

RUBY_DLLSPEC VALUE rb_get_path(VALUE);
#define FilePathValue(v) (RB_GC_GUARD(v) = rb_get_path(v))

RUBY_DLLSPEC VALUE rb_get_path_no_checksafe(VALUE);
#define FilePathStringValue(v) ((v) = rb_get_path_no_checksafe(v))

RUBY_DLLSPEC void rb_secure(int);
RUBY_DLLSPEC int rb_safe_level(void);
RUBY_DLLSPEC void rb_set_safe_level(int);
RUBY_DLLSPEC void rb_set_safe_level_force(int);
RUBY_DLLSPEC void rb_secure_update(VALUE);
RUBY_DLLSPEC NORETURN(void rb_insecure_operation(void));

/** The length of the array. */
#define RARRAY_LEN(ary) jruby_ary_len(ary)
/** Returns a pointer to a VALUE[] that mirrors the data in
 * the ruby array. */
#define RARRAY_PTR(ary) RARRAY(ary)->ptr
/** Pointer to the MRI array structure */
#define RARRAY(ary) jruby_rarray(ary)

#define RHASH_SIZE(h) FIX2INT(rb_hash_size(h))
#define RHASH_LEN(h) FIX2INT(rb_hash_size(h))
#define RHASH ({ JRuby does not support RHASH })
#define RHASH_TBL ({ JRuby does not support RHASH_TBL })

#define RFLOAT(v) jruby_rfloat(v)
#define RFLOAT_VALUE(v) jruby_float_value(v)

#define DATA_PTR(dta) (RDATA(dta)->data)
#define RDATA(dta) jruby_rdata((dta))

#define OBJ_FREEZE(obj) (rb_obj_freeze(obj))

#define RREGEXP_SRC(reg) rb_reg_source(reg)
#define RREGEXP_OPTIONS(reg) rb_reg_options(reg)
/* regexp options */
#define RE_OPTION_DEFAULT    0U
#define RE_OPTION_NONE       RE_OPTION_DEFAULT
#define RE_OPTION_IGNORECASE 1U
#define RE_OPTION_EXTENDED   (RE_OPTION_IGNORECASE << 1)
#define RE_OPTION_MULTILINE  (RE_OPTION_EXTENDED << 1)
#define RE_OPTION_SINGLELINE (RE_OPTION_MULTILINE << 1)
/* 1.9 */
#define ONIG_OPTION_DEFAULT    RE_OPTION_DEFAULT
#define ONIG_OPTION_NONE       RE_OPTION_NONE
#define ONIG_OPTION_IGNORECASE RE_OPTION_IGNORECASE
#define ONIG_OPTION_EXTEND     RE_OPTION_EXTENDED
#define ONIG_OPTION_MULTILINE  RE_OPTION_MULTILINE
#define ONIG_OPTION_SINGLELINE RE_OPTION_SINGLELINE


/* End of interface macros */

/**
 *  Process arguments using a template rather than manually.
 *
 *  The first two arguments are simple: the number of arguments given
 *  and an array of the args. Usually you get these as parameters to
 *  your function.
 *
 *  The spec works like this: it must have one (or more) of the following
 *  specifiers, and the specifiers that are given must always appear
 *  in the order given here. If the first character is a digit (0-9),
 *  it is the number of required parameters. If there is a second digit
 *  (0-9), it is the number of optional parameters. The next character
 *  may be "*", indicating a "splat" i.e. it consumes all remaining
 *  parameters. Finally, the last character may be "&", signifying
 *  that the block given (or Qnil) should be stored.
 *
 *  The remaining arguments are pointers to the variables in which
 *  the aforementioned format assigns the scanned parameters. For
 *  example in some imaginary function:
 *
 *    VALUE required1, required2, optional, splat, block
 *    rb_scan_args(argc, argv, "21*&", &required1, &required2,
 *                                     &optional,
 *                                     &splat,
 *                                     &block);
 *
 *  The required parameters must naturally always be exact. The
 *  optional parameters are set to nil when parameters run out.
 *  The splat is always an Array, but may be empty if there were
 *  no parameters that were not consumed by required or optional.
 *  Lastly, the block may be nil.
 */
RUBY_DLLSPEC int rb_scan_args(int argc, const VALUE* argv, const char* spec, ...);
/** Returns true on first load, false if already loaded or raises. */
RUBY_DLLSPEC VALUE rb_require(const char* name);

RUBY_DLLSPEC void rb_raise(VALUE exc, const char *fmt, ...) __attribute__((noreturn));
RUBY_DLLSPEC VALUE rb_rescue(VALUE(*)(ANYARGS),VALUE,VALUE(*)(ANYARGS),VALUE);
RUBY_DLLSPEC VALUE rb_rescue2(VALUE(*)(ANYARGS),VALUE,VALUE(*)(ANYARGS),VALUE,...);
RUBY_DLLSPEC VALUE rb_ensure(VALUE(*)(ANYARGS),VALUE,VALUE(*)(ANYARGS),VALUE);
RUBY_DLLSPEC VALUE rb_protect(VALUE (*func)(VALUE), VALUE data, int* status);
RUBY_DLLSPEC void rb_jump_tag(int status);
RUBY_DLLSPEC void rb_throw(const char* symbol, VALUE result) __attribute__((noreturn));

RUBY_DLLSPEC void rb_fatal(const char *fmt, ...) __attribute__((noreturn));
RUBY_DLLSPEC void rb_sys_fail(const char *msg) __attribute__((noreturn));
RUBY_DLLSPEC void rb_bug(const char*, ...) __attribute__((noreturn));
RUBY_DLLSPEC VALUE rb_exc_new(VALUE, const char*, long);
RUBY_DLLSPEC VALUE rb_exc_new2(VALUE, const char*);
RUBY_DLLSPEC VALUE rb_exc_new3(VALUE, VALUE);
RUBY_DLLSPEC void rb_exc_raise(VALUE) __attribute__((noreturn));

RUBY_DLLSPEC void rb_num_zerodiv(void);
RUBY_DLLSPEC long rb_num2long(VALUE);
RUBY_DLLSPEC unsigned long rb_num2ulong(VALUE);
RUBY_DLLSPEC int rb_num2int(VALUE);
RUBY_DLLSPEC unsigned int rb_num2uint(VALUE);
RUBY_DLLSPEC char rb_num2chr(VALUE);
RUBY_DLLSPEC double rb_num2dbl(VALUE);
RUBY_DLLSPEC long rb_fix2int(VALUE);
RUBY_DLLSPEC unsigned long rb_fix2uint(VALUE);
RUBY_DLLSPEC long long rb_num2ll(VALUE);
RUBY_DLLSPEC unsigned long long rb_num2ull(VALUE);
RUBY_DLLSPEC double rb_num2dbl(VALUE);
RUBY_DLLSPEC long rb_big2long(VALUE);
#define rb_big2int(x) rb_big2long(x)
RUBY_DLLSPEC unsigned long rb_big2ulong(VALUE);
#define rb_big2uint(x) rb_big2ulong(x)
RUBY_DLLSPEC long long rb_big2ll(VALUE);
RUBY_DLLSPEC double rb_big2dbl(VALUE);
RUBY_DLLSPEC VALUE rb_big2str(VALUE, int);
RUBY_DLLSPEC int rb_cmpint(VALUE, VALUE, VALUE);
RUBY_DLLSPEC void rb_cmperr(VALUE, VALUE) __attribute__((noreturn));

RUBY_DLLSPEC VALUE rb_int2inum(long);
RUBY_DLLSPEC VALUE rb_uint2inum(unsigned long);
RUBY_DLLSPEC VALUE rb_ll2inum(long long);
RUBY_DLLSPEC VALUE rb_ull2inum(unsigned long long);
RUBY_DLLSPEC VALUE rb_int2big(long long);
RUBY_DLLSPEC VALUE rb_uint2big(unsigned long long);
RUBY_DLLSPEC VALUE rb_Integer(VALUE);

/** Converts an object to an Integer by calling #to_int. */
RUBY_DLLSPEC VALUE rb_to_int(VALUE);
/** Converts an object to an Integer using the specified method */
RUBY_DLLSPEC VALUE rb_to_integer(VALUE, const char*);

/** Convert a Fixnum into an int. */
static inline int
FIX2INT(VALUE x)
{
    return ((int) RSHIFT((SIGNED_VALUE)x,1));
}

/** Convert a Fixnum into an unsigned int. */
static inline unsigned int
FIX2UINT(VALUE x)
{
    return ((unsigned int) ((((VALUE)(x))>>1)&LONG_MAX));
}

static inline long
FIX2LONG(VALUE x)
{
    return ((long) RSHIFT((SIGNED_VALUE)x,1));
}

static inline unsigned long
FIX2ULONG(VALUE x)
{
    return ((unsigned long) ((((VALUE)(x))>>1)&LONG_MAX));
}


/** Convert a VALUE into an int. */
static inline int
NUM2INT(VALUE x)
{
    return __builtin_expect(FIXNUM_P(x), 1) ? FIX2INT(x) : rb_num2int(x);
}

static inline unsigned int
NUM2UINT(VALUE x)
{
    return __builtin_expect(FIXNUM_P(x), 1) ? FIX2UINT(x) : rb_num2uint(x);
}

/** Convert a VALUE into a long int. */
static inline long
NUM2LONG(VALUE x)
{
    return __builtin_expect(FIXNUM_P(x), 1) ? FIX2LONG(x) : rb_num2long(x);
}


static inline unsigned long
NUM2ULONG(VALUE x)
{
    return __builtin_expect(FIXNUM_P(x), 1) ? FIX2ULONG(x) : rb_num2ulong(x);
}

static inline long long
NUM2LL(VALUE x)
{
    return __builtin_expect(FIXNUM_P(x), 1) ? FIX2LONG(x) : rb_num2ll(x);
}


static inline unsigned long long
NUM2ULL(VALUE x)
{
    return __builtin_expect(FIXNUM_P(x), 1) ? FIX2ULONG(x) : rb_num2ull(x);
}

/** Convert int to a Ruby Integer. */
static inline VALUE
INT2FIX(int i)
{
    return (((SIGNED_VALUE) i) << 1) | FIXNUM_FLAG;
}

/** Convert char to a Ruby Integer. */
static inline VALUE
CHR2FIX(char chr)
{
    return INT2FIX((long)((chr) & 0xff));
}

/** Convert unsigned int to a Ruby Integer. */
static inline VALUE
UINT2FIX(unsigned int i)
{
    return (((VALUE) i) << 1) | FIXNUM_FLAG;
}

/** Convert long int to a Ruby Integer. */
static inline VALUE
LONG2FIX(long i)
{
    return (((SIGNED_VALUE) i) << 1) | FIXNUM_FLAG;
}

/** Convert unsigned int to a Ruby Integer. */
static inline VALUE
ULONG2FIX(unsigned long i)
{
    return (((VALUE) i) << 1) | FIXNUM_FLAG;
}

/** Convert int to a Ruby Integer. */


static inline VALUE
INT2NUM(long v)
{
    return __builtin_expect(FIXABLE(v), 1) ? INT2FIX(v) : rb_int2inum(v);
}

static inline VALUE
UINT2NUM(unsigned long v)
{
    return __builtin_expect(POSFIXABLE(v), 1) ? UINT2FIX(v) : rb_uint2inum(v);
}

static inline VALUE
LONG2NUM(long v)
{
    return __builtin_expect(FIXABLE(v), 1) ? LONG2FIX(v) : rb_int2inum(v);
}

static inline VALUE
ULONG2NUM(unsigned long v)
{
    return __builtin_expect(POSFIXABLE(v), 1) ? ULONG2FIX(v) : rb_uint2inum(v);
}


static inline VALUE
LL2NUM(long long v)
{
    return __builtin_expect(FIXABLE(v), 1) ? LONG2FIX(v) : rb_ll2inum(v);
}

static inline VALUE
ULL2NUM(unsigned long long v)
{
    return __builtin_expect(POSFIXABLE(v), 1) ? ULONG2FIX(v) : rb_ull2inum(v);
}

/** Convert time_t to a Ruby Integer. */
#define TIMET2NUM(x) jruby_timet2num(x)
RUBY_DLLSPEC VALUE jruby_timet2num(time_t v);

/** Convert a VALUE into a chr */
#define NUM2CHR(x) rb_num2chr(x)
/** Convert a VALUE into a long long */
#define NUM2DBL(x) rb_num2dbl(x)

#define rb_int_new(v) rb_int2inum(v)


RUBY_DLLSPEC VALUE rb_funcall(VALUE obj, ID meth, int cnt, ...);
RUBY_DLLSPEC VALUE rb_funcall2(VALUE obj, ID meth, int cnt, VALUE*);
RUBY_DLLSPEC VALUE jruby_funcall2b(VALUE obj, ID meth, int cnt, VALUE*, VALUE);
/** Starts the lookup in the superclass to call a method on the current self */
RUBY_DLLSPEC VALUE rb_call_super(int argc, const VALUE *argv);

/** Returns a new, anonymous class inheriting from super_handle. */
RUBY_DLLSPEC VALUE rb_class_new(VALUE);
/** Calls the class method 'inherited' on super passing the class. */
RUBY_DLLSPEC VALUE rb_class_inherited(VALUE super, VALUE klass);
/** As Ruby's .new, with the given arguments. Returns the new object. */
RUBY_DLLSPEC VALUE rb_class_new_instance(int arg_count, VALUE* args, VALUE class_handle);
/** Returns the Class object this object is an instance of. */
RUBY_DLLSPEC VALUE rb_class_of(VALUE object_handle);
/** Returns String representation of the class' name. */
RUBY_DLLSPEC VALUE rb_class_name(VALUE class_handle);
/** C string representation of the class' name. You must free this string. */
RUBY_DLLSPEC char* rb_class2name(VALUE class_handle);
/** Convert a path string to a class */
RUBY_DLLSPEC VALUE rb_path2class(const char* path);
RUBY_DLLSPEC VALUE rb_path_to_class(VALUE pathname);
/** Include Module in another Module, just as Ruby's Module#include. */
RUBY_DLLSPEC void rb_include_module(VALUE self, VALUE module);
/** Return the object's singleton class */
RUBY_DLLSPEC VALUE rb_singleton_class(VALUE obj);
RUBY_DLLSPEC VALUE rb_class_inherited_p(VALUE mod, VALUE arg);
RUBY_DLLSPEC VALUE rb_class_superclass(VALUE klass);

RUBY_DLLSPEC VALUE rb_define_class(const char*,VALUE);
RUBY_DLLSPEC VALUE rb_define_module(const char*);
RUBY_DLLSPEC VALUE rb_define_class_under(VALUE, const char*, VALUE);
RUBY_DLLSPEC VALUE rb_define_module_under(VALUE, const char*);
/** Ruby's attr_* for given name. Nonzeros to toggle read/write. */
RUBY_DLLSPEC void rb_define_attr(VALUE module_handle, const char* attr_name, int readable, int writable);
RUBY_DLLSPEC void rb_define_method(VALUE,const char*,VALUE(*)(ANYARGS),int);
RUBY_DLLSPEC void rb_define_private_method(VALUE,const char*,VALUE(*)(ANYARGS),int);
RUBY_DLLSPEC void rb_define_protected_method(VALUE,const char*,VALUE(*)(ANYARGS),int);
RUBY_DLLSPEC void rb_define_module_function(VALUE,const char*,VALUE(*)(ANYARGS),int);
RUBY_DLLSPEC void rb_define_global_function(const char*,VALUE(*)(ANYARGS),int);
RUBY_DLLSPEC void rb_define_singleton_method(VALUE object, const char* meth, VALUE(*fn)(ANYARGS), int arity);
/* Not alias, but alias method */
RUBY_DLLSPEC void rb_alias(VALUE module, ID id_new, ID id_old);

#define HAVE_RB_DEFINE_ALLOC_FUNC 1
typedef VALUE (*rb_alloc_func_t)(VALUE);
RUBY_DLLSPEC void rb_define_alloc_func(VALUE, rb_alloc_func_t);

RUBY_DLLSPEC void rb_undef_method(VALUE, const char*);
RUBY_DLLSPEC void rb_undef(VALUE, ID);

RUBY_DLLSPEC void rb_define_class_variable(VALUE, const char*, VALUE);
RUBY_DLLSPEC void rb_cv_set(VALUE, const char*, VALUE);
RUBY_DLLSPEC VALUE rb_cv_get(VALUE, const char*);
/** Returns a value evaluating true if module has named class var. */
RUBY_DLLSPEC VALUE rb_cvar_defined(VALUE module_handle, ID name);
/** Returns class variable by (Symbol) name from module. */
RUBY_DLLSPEC VALUE rb_cvar_get(VALUE module_handle, ID name);
/** Set module's named class variable to given value. Returns the value. */
RUBY_DLLSPEC VALUE rb_cvar_set(VALUE module_handle, ID name, VALUE value);
#define rb_cvar_set(klass, name, value, ...) __extension__(rb_cvar_set(klass, name, value))

/** Return object's instance variable by name. @ optional. */
RUBY_DLLSPEC VALUE rb_iv_get(VALUE obj, const char* name);
/** Set instance variable by name to given value. Returns the value. @ optional. */
RUBY_DLLSPEC VALUE rb_iv_set(VALUE obj, const char* name, VALUE value);
/** Get object's instance variable. */
RUBY_DLLSPEC VALUE rb_ivar_get(VALUE obj, ID ivar_name);
/** Set object's instance variable to given value. */
RUBY_DLLSPEC VALUE rb_ivar_set(VALUE obj, ID ivar_name, VALUE value);
RUBY_DLLSPEC VALUE rb_ivar_defined(VALUE obj, ID ivar_name);

/** Nonzero if constant corresponding to Symbol exists in the Module. */
RUBY_DLLSPEC int rb_const_defined(VALUE, ID);
/** Returns non-zero if the constant is defined in the Module, not searching outside */
RUBY_DLLSPEC int rb_const_defined_at(VALUE, ID);
/** Retrieve constant from given module. */
RUBY_DLLSPEC VALUE rb_const_get(VALUE, ID);
/** Returns a constant defined in module only. */
RUBY_DLLSPEC VALUE rb_const_get_at(VALUE, ID);
/** Retrieve constant from given module. */
RUBY_DLLSPEC VALUE rb_const_get_from(VALUE, ID);
/** Set constant on the given module */
RUBY_DLLSPEC void rb_const_set(VALUE, ID, VALUE);

/** Alias method by old name as new name. */
RUBY_DLLSPEC void rb_define_alias(VALUE klass, const char *new_name, const char *old_name);

/* Array */
RUBY_DLLSPEC VALUE rb_Array(VALUE val);
RUBY_DLLSPEC VALUE rb_ary_new(void);
RUBY_DLLSPEC VALUE rb_ary_new2(long length);
RUBY_DLLSPEC VALUE rb_ary_new3(long size, ...);
RUBY_DLLSPEC VALUE rb_ary_new4(long n, const VALUE *);
RUBY_DLLSPEC VALUE rb_assoc_new(VALUE, VALUE);
RUBY_DLLSPEC int rb_ary_size(VALUE self);
RUBY_DLLSPEC VALUE rb_ary_push(VALUE array, VALUE val);
RUBY_DLLSPEC VALUE rb_ary_pop(VALUE array);
RUBY_DLLSPEC VALUE rb_ary_entry(VALUE array, long offset);
RUBY_DLLSPEC VALUE rb_ary_clear(VALUE array);
RUBY_DLLSPEC VALUE rb_ary_dup(VALUE array);
RUBY_DLLSPEC VALUE rb_ary_join(VALUE array1, VALUE array2);
RUBY_DLLSPEC VALUE rb_ary_reverse(VALUE array);
RUBY_DLLSPEC VALUE rb_ary_unshift(VALUE array, VALUE val);
RUBY_DLLSPEC VALUE rb_ary_shift(VALUE array);
RUBY_DLLSPEC void rb_ary_store(VALUE array, long offset, VALUE val);
RUBY_DLLSPEC VALUE rb_ary_includes(VALUE, VALUE);
RUBY_DLLSPEC VALUE rb_ary_delete(VALUE, VALUE);
RUBY_DLLSPEC VALUE rb_ary_delete_at(VALUE, long);
RUBY_DLLSPEC VALUE rb_ary_aref(int, VALUE*, VALUE);
RUBY_DLLSPEC VALUE rb_ary_to_s(VALUE);
RUBY_DLLSPEC void rb_mem_clear(VALUE*, int);
RUBY_DLLSPEC VALUE rb_ary_freeze(VALUE);
RUBY_DLLSPEC VALUE rb_ary_to_ary(VALUE);

RUBY_DLLSPEC VALUE rb_each(VALUE);
RUBY_DLLSPEC VALUE rb_iterate(VALUE (*ifunc)(VALUE), VALUE ary, VALUE(*cb)(ANYARGS), VALUE cb_data);

/** Returns a pointer to the readonly RArray structure
 * which exposes an MRI-like API to the C code.
 */
RUBY_DLLSPEC struct RArray* jruby_rarray(VALUE ary);
/** returns the length of the ruby array */
RUBY_DLLSPEC long jruby_ary_len(VALUE ary);

/* Hash */
RUBY_DLLSPEC VALUE rb_hash(VALUE);
RUBY_DLLSPEC VALUE rb_hash_new(void);
RUBY_DLLSPEC VALUE rb_hash_aref(VALUE hash, VALUE key);
#ifndef HAVE_RB_HASH_ASET
# define HAVE_RB_HASH_ASET 1
#endif
RUBY_DLLSPEC VALUE rb_hash_aset(VALUE hash, VALUE key, VALUE val);
RUBY_DLLSPEC VALUE rb_hash_delete(VALUE hash, VALUE key);
RUBY_DLLSPEC VALUE rb_hash_size(VALUE hash);
#ifndef HAVE_RB_HASH_FOREACH
# define HAVE_RB_HASH_FOREACH 1
#endif
RUBY_DLLSPEC void rb_hash_foreach(VALUE hash, int (*func)(ANYARGS), VALUE arg);
RUBY_DLLSPEC VALUE rb_hash_lookup(VALUE hash, VALUE key);

/* String */
RUBY_DLLSPEC VALUE rb_str_new(const char*, long);
RUBY_DLLSPEC VALUE rb_str_new2(const char*);
RUBY_DLLSPEC VALUE rb_str_new4(VALUE);
RUBY_DLLSPEC VALUE rb_str_new_cstr(const char*);
RUBY_DLLSPEC VALUE rb_tainted_str_new_cstr(const char*);
RUBY_DLLSPEC VALUE rb_tainted_str_new(const char*, long);
RUBY_DLLSPEC VALUE rb_str_buf_new(long);
#define rb_str_buf_new2(str) rb_str_new2(str)
RUBY_DLLSPEC VALUE rb_str_buf_append(VALUE, VALUE);
RUBY_DLLSPEC VALUE rb_str_buf_cat(VALUE, const char*, long);
RUBY_DLLSPEC VALUE rb_str_buf_cat2(VALUE, const char*);
RUBY_DLLSPEC VALUE rb_obj_as_string(VALUE);
RUBY_DLLSPEC VALUE rb_check_string_type(VALUE);
RUBY_DLLSPEC VALUE rb_str_dup(VALUE);
RUBY_DLLSPEC VALUE rb_str_dup_frozen(VALUE);
#define rb_str_new_frozen rb_str_dup_frozen
RUBY_DLLSPEC VALUE rb_str_plus(VALUE, VALUE);
RUBY_DLLSPEC VALUE rb_str_length(VALUE str);
RUBY_DLLSPEC VALUE rb_str_substr(VALUE, long, long);
RUBY_DLLSPEC VALUE rb_str_freeze(VALUE);
RUBY_DLLSPEC void rb_str_set_len(VALUE, long);
RUBY_DLLSPEC VALUE rb_str_resize(VALUE, long);
RUBY_DLLSPEC VALUE rb_str_cat(VALUE, const char*, long);
RUBY_DLLSPEC VALUE rb_str_cat2(VALUE, const char*);
RUBY_DLLSPEC VALUE rb_str_append(VALUE, VALUE);
RUBY_DLLSPEC VALUE rb_str_concat(VALUE, VALUE);
RUBY_DLLSPEC int rb_str_cmp(VALUE, VALUE);
RUBY_DLLSPEC void rb_str_update(VALUE, long, long, VALUE);
RUBY_DLLSPEC VALUE rb_str_inspect(VALUE);
RUBY_DLLSPEC VALUE rb_str_split(VALUE, const char*);
RUBY_DLLSPEC VALUE rb_str_intern(VALUE);
RUBY_DLLSPEC VALUE rb_str_length(VALUE);
#define rb_str_hash(str) rb_hash(str)
/**
 * Returns a pointer to the String, the length is returned
 * in len parameter, which can be NULL.
 */
RUBY_DLLSPEC char* rb_str2cstr(VALUE str_handle, long *len);
/** Deprecated alias for rb_obj_freeze */
RUBY_DLLSPEC VALUE rb_str_freeze(VALUE str);
/** Return Integer obtained from String#to_i using given base. */
RUBY_DLLSPEC VALUE rb_str2inum(VALUE str, int base);
#define rb_cstr2inum(str, base) rb_str2inum(rb_str_new_cstr(str), base)
#define rb_cstr_to_inum(str, base, badcheck) rb_cstr2inum(str, base)
/** Return a String using #to_str. Error raised if invalid conversion. */
RUBY_DLLSPEC VALUE rb_str_to_str(VALUE);
/** Return a String using #to_s. Error raised if invalid conversion. */
RUBY_DLLSPEC VALUE rb_String(VALUE);
/** Call #to_s on object pointed to and _replace_ it with the String. */
RUBY_DLLSPEC VALUE rb_string_value(VALUE* object_variable);
RUBY_DLLSPEC char* rb_string_value_ptr(VALUE* object_variable);
/** As rb_string_value but also returns a C string of the new String. */
RUBY_DLLSPEC char* rb_string_value_cstr(VALUE* object_variable);

RUBY_DLLSPEC struct RString* jruby_rstring(VALUE v);
RUBY_DLLSPEC int jruby_str_length(VALUE v);
RUBY_DLLSPEC char* jruby_str_cstr(VALUE v);
RUBY_DLLSPEC char* jruby_str_cstr_readonly(VALUE v);

#define rb_str_ptr_readonly(v) jruby_str_cstr_readonly((v))
#define rb_str_ptr(v) jruby_str_cstr((v))
#define rb_str_new_cstr(str) __extension__ (            \
{                                                       \
    (__builtin_constant_p(str) && str != NULL) ?        \
        rb_str_new(str, (long)strlen(str)) :            \
        rb_str_new_cstr(str);                           \
})
#define rb_tainted_str_new_cstr(str) __extension__ (    \
{                                                       \
    (__builtin_constant_p(str) && str != NULL) ?        \
        rb_tainted_str_new(str, (long)strlen(str)) :          \
        rb_tainted_str_new_cstr(str);                   \
})
#define rb_str_buf_new_cstr(str) __extension__ (        \
{                                                       \
    (__builtin_constant_p(str) && str != NULL) ?        \
        rb_str_buf_cat(rb_str_buf_new((long)strlen(str)), \
                       str, (long)strlen(str)) : \
        rb_str_buf_new_cstr(str);               \
})
#define rb_str_buf_cat2(str, ptr) __extension__ (       \
{                                                       \
    (__builtin_constant_p(ptr) && ptr != NULL) ?        \
        rb_str_buf_cat(str, ptr, (long)strlen(ptr)) :  \
        rb_str_buf_cat2(str, ptr);                      \
})
#define rb_str_cat2(str, ptr) __extension__ (           \
{                                                       \
    (__builtin_constant_p(ptr) && ptr != NULL) ?                       \
        rb_str_cat(str, ptr, (long)strlen(ptr)) :       \
        rb_str_cat2(str, ptr);                          \
})

#define rb_str_new2 rb_str_new_cstr
#define rb_str_new3 rb_str_dup
#define rb_str_new_shared rb_str_new3
#define rb_tainted_str_new2 rb_tainted_str_new_cstr

/** Returns the string associated with a symbol. */
RUBY_DLLSPEC const char *rb_id2name(ID sym);
/** Call #to_sym on object. */
RUBY_DLLSPEC ID rb_to_id(VALUE);

/** Returns a Struct with the specified fields. */
RUBY_DLLSPEC VALUE rb_struct_define(const char *name, ...);
RUBY_DLLSPEC VALUE rb_struct_aref(VALUE struct_handle, VALUE key);
RUBY_DLLSPEC VALUE rb_struct_aset(VALUE struct_handle, VALUE key, VALUE val);
RUBY_DLLSPEC VALUE rb_struct_new(VALUE klass, ...);

RUBY_DLLSPEC void* jruby_data(VALUE);
RUBY_DLLSPEC struct RData* jruby_rdata(VALUE);

typedef void (*RUBY_DATA_FUNC)(void*);

RUBY_DLLSPEC VALUE rb_data_object_alloc(VALUE,void*,RUBY_DATA_FUNC,RUBY_DATA_FUNC);

#define Data_Wrap_Struct(klass,mark,free,sval)\
    rb_data_object_alloc(klass,sval,(RUBY_DATA_FUNC)mark,(RUBY_DATA_FUNC)free)

#define Data_Make_Struct(klass,type,mark,free,sval) (\
    sval = ALLOC(type),\
    memset(sval, 0, sizeof(type)),\
    Data_Wrap_Struct(klass,mark,free,sval)\
)

#define Data_Get_Struct(obj,type,sval) do {\
    Check_Type(obj, T_DATA); \
    sval = (type*)DATA_PTR(obj);\
} while (0)

RUBY_DLLSPEC void rb_gc(void);
RUBY_DLLSPEC void rb_gc_mark_locations(VALUE*, VALUE*);
RUBY_DLLSPEC void rb_gc_mark(VALUE);
RUBY_DLLSPEC void rb_gc_mark_maybe(VALUE v);
/** Mark variable global */
RUBY_DLLSPEC void rb_global_variable(VALUE* handle_address);
RUBY_DLLSPEC void rb_gc_register_address(VALUE* address);
/** Unmark variable as global */
RUBY_DLLSPEC void rb_gc_unregister_address(VALUE* address);
/** Return the global variable. $ optional */
RUBY_DLLSPEC VALUE rb_gv_get(const char* name);
/** Set named global to given value, returning the value. $ optional. */
RUBY_DLLSPEC VALUE rb_gv_set(const char* name, VALUE value);
RUBY_DLLSPEC void rb_define_variable(const char *name, VALUE *var);
RUBY_DLLSPEC void rb_define_readonly_variable(const char* name, VALUE* value);
/** Sets the $KCODE global variable */
RUBY_DLLSPEC void rb_set_kcode(const char *code);
/** Return an array containing the names of all global variables */
RUBY_DLLSPEC VALUE rb_f_global_variables(void);

RUBY_DLLSPEC VALUE rb_eval_string(const char* string);
RUBY_DLLSPEC VALUE rb_obj_instance_eval(int, VALUE*, VALUE);

/** Print a warning if $VERBOSE is not nil. */
RUBY_DLLSPEC void rb_warn(const char *fmt, ...);
/** Print a warning if $VERBOSE is true. */
RUBY_DLLSPEC void rb_warning(const char *fmt, ...);

RUBY_DLLSPEC VALUE rb_f_sprintf(int argc, const VALUE* argv);

/** 1 if obj.respond_to? method_name evaluates true, 0 otherwise. */
RUBY_DLLSPEC int rb_respond_to(VALUE obj_handle, ID method_name);
RUBY_DLLSPEC int rb_obj_respond_to(VALUE, ID, int);
/** Returns object returned by invoking method on object if right type, or raises error. */
RUBY_DLLSPEC VALUE rb_convert_type(VALUE object_handle, int type, const char* type_name, const char* method_name);
/** Returns object returned by invoking method on object or nil */
RUBY_DLLSPEC VALUE rb_check_convert_type(VALUE val, int type, const char* type_name, const char* method);
RUBY_DLLSPEC VALUE rb_check_to_integer(VALUE object_handle, const char *method_name);
RUBY_DLLSPEC VALUE rb_check_array_type(VALUE val);
RUBY_DLLSPEC VALUE rb_check_string_type(VALUE val);

/** Define a constant in given Module's namespace. */
RUBY_DLLSPEC void rb_define_const(VALUE module, const char* name, VALUE obj);
/** Define a toplevel constant */
RUBY_DLLSPEC void rb_define_global_const(const char* name, VALUE obj);
RUBY_DLLSPEC ID rb_intern(const char*);
RUBY_DLLSPEC ID rb_intern2(const char*, long);
RUBY_DLLSPEC ID rb_intern_const(const char*);

RUBY_DLLSPEC int rb_is_class_id(ID symbol);
RUBY_DLLSPEC int rb_is_instance_id(ID symbol);
RUBY_DLLSPEC int rb_is_const_id(ID symbol);

#define CONST_ID_CACHE(result, str)                     \
    {                                                   \
        static ID rb_intern_id_cache;                   \
        if (__builtin_expect(!rb_intern_id_cache, 0))           \
            rb_intern_id_cache = rb_intern2(str, strlen(str));  \
        result rb_intern_id_cache;                      \
    }

#define rb_intern(str) \
    (__builtin_constant_p(str) \
        ? __extension__ (CONST_ID_CACHE(/**/, str)) : rb_intern(str))

#define rb_intern_const(str) \
    (__builtin_constant_p(str) \
        ? __extension__ (rb_intern2(str, strlen(str))) : rb_intern(str))

RUBY_DLLSPEC struct RFloat* jruby_rfloat(VALUE v);
RUBY_DLLSPEC VALUE rb_float_new(double value);
RUBY_DLLSPEC double jruby_float_value(VALUE v);
RUBY_DLLSPEC VALUE rb_Float(VALUE object_handle);

RUBY_DLLSPEC int jruby_big_bytes_used(VALUE obj);
RUBY_DLLSPEC VALUE jruby_big_sign(VALUE);
#define RBIGNUM_LEN(obj) jruby_big_bytes_used(obj)
#define RBIGNUM_SIGN(obj) jruby_big_sign(obj)
// fake out, used with RBIGNUM_LEN anyway, which provides the full answer
#define SIZEOF_BDIGITS 1

/** Call block with given argument(s) or raise error if no block given. */
RUBY_DLLSPEC VALUE rb_yield(VALUE argument);
RUBY_DLLSPEC VALUE rb_yield_splat(VALUE array);
RUBY_DLLSPEC VALUE rb_yield_values(int n, ...);

/** Return 1 if block given, 0 if not */
RUBY_DLLSPEC int rb_block_given_p(void);
/** Return the Proc for the implicit block */
RUBY_DLLSPEC VALUE rb_block_proc(void);
/** Create a proc with func as body and val as proc argument ({|*args, proc_arg| func }) */
RUBY_DLLSPEC VALUE rb_proc_new(VALUE (*func)(ANYARGS), VALUE val);

/** Freeze object and return it. */
RUBY_DLLSPEC VALUE rb_obj_freeze(VALUE obj);
/** Raise an error if the object is frozen */
RUBY_DLLSPEC void rb_check_frozen(VALUE obj);
/** Allocate uninitialised instance of given class. */
RUBY_DLLSPEC VALUE rb_obj_alloc(VALUE klass);
/** Call initialize */
RUBY_DLLSPEC void rb_obj_call_init(VALUE recv, int arg_count, VALUE* args);
/** String representation of the object's class' name. You must free this string. */
RUBY_DLLSPEC char* rb_obj_classname(VALUE object_handle);
/** Returns true-ish if module is object's class or other ancestor. */
RUBY_DLLSPEC VALUE rb_obj_is_kind_of(VALUE, VALUE);
RUBY_DLLSPEC VALUE rb_obj_is_instance_of(VALUE, VALUE);
/** Returns the Class object this object is an instance of. */
#define rb_obj_class(object) rb_class_of((object))
RUBY_DLLSPEC VALUE rb_obj_clone(VALUE obj);

RUBY_DLLSPEC void rb_extend_object(VALUE, VALUE);
RUBY_DLLSPEC VALUE rb_obj_taint(VALUE);
RUBY_DLLSPEC VALUE rb_obj_tainted(VALUE);
RUBY_DLLSPEC VALUE rb_any_to_s(VALUE obj);
RUBY_DLLSPEC VALUE rb_inspect(VALUE obj);
RUBY_DLLSPEC VALUE rb_obj_as_string(VALUE obj);
RUBY_DLLSPEC void jruby_infect(VALUE object1, VALUE object2);
RUBY_DLLSPEC VALUE rb_obj_dup(VALUE obj);
RUBY_DLLSPEC VALUE rb_obj_id(VALUE obj);
RUBY_DLLSPEC VALUE rb_equal(VALUE obj, VALUE other);
RUBY_DLLSPEC int rb_eql(VALUE, VALUE);

RUBY_DLLSPEC VALUE rb_attr_get(VALUE obj, ID id);

RUBY_DLLSPEC VALUE rb_exc_new(VALUE, const char*, long);
RUBY_DLLSPEC VALUE rb_exc_new2(VALUE, const char*);
RUBY_DLLSPEC VALUE rb_exc_new3(VALUE, VALUE);

#define rb_exc_new2(klass, ptr) __extension__ ( \
{                                               \
    (__builtin_constant_p(ptr)) ?               \
        rb_exc_new(klass, ptr, (long)strlen(ptr)) : \
        rb_exc_new2(klass, ptr);                \
})

RUBY_DLLSPEC VALUE rb_io_write(VALUE io, VALUE str);
RUBY_DLLSPEC VALUE rb_io_close(VALUE io);
RUBY_DLLSPEC int rb_io_fd(VALUE io);
RUBY_DLLSPEC void rb_io_check_readable(rb_io_t* io);
RUBY_DLLSPEC void rb_io_check_writable(rb_io_t* io);
RUBY_DLLSPEC void rb_io_check_closed(rb_io_t* io);
RUBY_DLLSPEC void rb_io_set_nonblock(rb_io_t* io);
RUBY_DLLSPEC int rb_io_wait_readable(int f);
RUBY_DLLSPEC int rb_io_wait_writable(int f);
RUBY_DLLSPEC VALUE rb_file_open(char* filename, char* mode);
#define HAVE_RB_IO_FD 1
// Writes the OpenFile struct pointer of val into ptr
RUBY_DLLSPEC rb_io_t* jruby_io_struct(VALUE io);
#define GetOpenFile(val, ptr) (ptr = jruby_io_struct(val))
#define GetReadFile(ptr) (ptr->f)
#define GetWriteFile(ptr) (ptr->f)

RUBY_DLLSPEC VALUE rb_range_new(VALUE, VALUE, int);

RUBY_DLLSPEC void rb_undef_alloc_func(VALUE);
RUBY_DLLSPEC void rb_need_block(void);

RUBY_DLLSPEC VALUE rb_marshal_dump(VALUE, VALUE);
RUBY_DLLSPEC VALUE rb_marshal_load(VALUE);

RUBY_DLLSPEC VALUE rb_reg_nth_match(long, VALUE);
RUBY_DLLSPEC VALUE rb_reg_match(VALUE re, VALUE str);
RUBY_DLLSPEC VALUE rb_reg_new(const char*, long, int);
RUBY_DLLSPEC VALUE rb_reg_source(VALUE);
RUBY_DLLSPEC int rb_reg_options(VALUE);
RUBY_DLLSPEC VALUE rb_reg_regcomp(VALUE);
RUBY_DLLSPEC VALUE rb_backref_get(void);

/* 1.9 provides these, so we will too: */
#define RUBY_UBF_IO ((rb_unblock_function_t *)-1)
#define RUBY_UBF_PROCESS ((rb_unblock_function_t *)-1)
/** Release the GIL and let func run in a parallel */
typedef VALUE rb_blocking_function_t(void *);
typedef void rb_unblock_function_t(void *);
RUBY_DLLSPEC VALUE rb_thread_blocking_region(rb_blocking_function_t func, void* data, rb_unblock_function_t, void*);
/** Block other threads and wait until the system select returns */
RUBY_DLLSPEC int rb_thread_select(int max, fd_set * read, fd_set * write, fd_set * except, struct timeval *timeout);
RUBY_DLLSPEC void rb_thread_wait_fd_rw(int fd, int read);
RUBY_DLLSPEC void rb_thread_wait_fd(int f);
RUBY_DLLSPEC int rb_thread_fd_writable(int f);
RUBY_DLLSPEC void rb_thread_wait_for(struct timeval time);
RUBY_DLLSPEC VALUE rb_thread_wakeup(VALUE);

/** The currently executing thread */
RUBY_DLLSPEC VALUE rb_thread_current(void);
/** Calls pass on the Ruby thread class */
RUBY_DLLSPEC void rb_thread_schedule(void);
/** Fake placeholder. Always returns 0 */
RUBY_DLLSPEC int rb_thread_alone(void);
/** Get and set thread locals */
RUBY_DLLSPEC VALUE rb_thread_local_aset(VALUE thread, ID id, VALUE value);
RUBY_DLLSPEC VALUE rb_thread_local_aref(VALUE thread, ID id);
RUBY_DLLSPEC VALUE rb_thread_create(VALUE (*fn)(ANYARGS), void* arg);

RUBY_DLLSPEC VALUE rb_time_new(time_t sec, long usec);

RUBY_DLLSPEC void rb_thread_stop_timer_thread(void);
RUBY_DLLSPEC void rb_thread_start_timer_thread(void);
RUBY_DLLSPEC void rb_thread_stop_timer(void);
RUBY_DLLSPEC void rb_thread_start_timer(void);

/** Global flag which marks the currently executing thread critical. */
extern RUBY_DLLSPEC VALUE rb_thread_critical;

/* Global Module objects. */
RUBY_DLLSPEC extern VALUE rb_mKernel;
RUBY_DLLSPEC extern VALUE rb_mComparable;
RUBY_DLLSPEC extern VALUE rb_mEnumerable;
RUBY_DLLSPEC extern VALUE rb_mErrno;
RUBY_DLLSPEC extern VALUE rb_mFileTest;
RUBY_DLLSPEC extern VALUE rb_mGC;
RUBY_DLLSPEC extern VALUE rb_mMath;
RUBY_DLLSPEC extern VALUE rb_mProcess;

/* Global Class objects */
RUBY_DLLSPEC extern VALUE rb_cObject;
RUBY_DLLSPEC extern VALUE rb_cArray;
RUBY_DLLSPEC extern VALUE rb_cBignum;
RUBY_DLLSPEC extern VALUE rb_cBinding;
RUBY_DLLSPEC extern VALUE rb_cClass;
RUBY_DLLSPEC extern VALUE rb_cDir;
RUBY_DLLSPEC extern VALUE rb_cData;
RUBY_DLLSPEC extern VALUE rb_cFalseClass;
RUBY_DLLSPEC extern VALUE rb_cFile;
RUBY_DLLSPEC extern VALUE rb_cFixnum;
RUBY_DLLSPEC extern VALUE rb_cFloat;
RUBY_DLLSPEC extern VALUE rb_cHash;
RUBY_DLLSPEC extern VALUE rb_cInteger;
RUBY_DLLSPEC extern VALUE rb_cIO;
RUBY_DLLSPEC extern VALUE rb_cMatch;
RUBY_DLLSPEC extern VALUE rb_cMethod;
RUBY_DLLSPEC extern VALUE rb_cModule;
RUBY_DLLSPEC extern VALUE rb_cNilClass;
RUBY_DLLSPEC extern VALUE rb_cNumeric;
RUBY_DLLSPEC extern VALUE rb_cProc;
RUBY_DLLSPEC extern VALUE rb_cRange;
RUBY_DLLSPEC extern VALUE rb_cRegexp;
RUBY_DLLSPEC extern VALUE rb_cString;
RUBY_DLLSPEC extern VALUE rb_cStruct;
RUBY_DLLSPEC extern VALUE rb_cSymbol;
RUBY_DLLSPEC extern VALUE rb_cThread;
RUBY_DLLSPEC extern VALUE rb_cTime;
RUBY_DLLSPEC extern VALUE rb_cTrueClass;

/* Exception classes. */
RUBY_DLLSPEC extern VALUE rb_eException;
RUBY_DLLSPEC extern VALUE rb_eStandardError;
RUBY_DLLSPEC extern VALUE rb_eSystemExit;
RUBY_DLLSPEC extern VALUE rb_eInterrupt;
RUBY_DLLSPEC extern VALUE rb_eSignal;
RUBY_DLLSPEC extern VALUE rb_eFatal;
RUBY_DLLSPEC extern VALUE rb_eArgError;
RUBY_DLLSPEC extern VALUE rb_eEOFError;
RUBY_DLLSPEC extern VALUE rb_eIndexError;
RUBY_DLLSPEC extern VALUE rb_eStopIteration;
RUBY_DLLSPEC extern VALUE rb_eRangeError;
RUBY_DLLSPEC extern VALUE rb_eIOError;
RUBY_DLLSPEC extern VALUE rb_eRuntimeError;
RUBY_DLLSPEC extern VALUE rb_eSecurityError;
RUBY_DLLSPEC extern VALUE rb_eSystemCallError;
RUBY_DLLSPEC extern VALUE rb_eThreadError;
RUBY_DLLSPEC extern VALUE rb_eTypeError;
RUBY_DLLSPEC extern VALUE rb_eZeroDivError;
RUBY_DLLSPEC extern VALUE rb_eNotImpError;
RUBY_DLLSPEC extern VALUE rb_eNoMemError;
RUBY_DLLSPEC extern VALUE rb_eNoMethodError;
RUBY_DLLSPEC extern VALUE rb_eFloatDomainError;
RUBY_DLLSPEC extern VALUE rb_eLocalJumpError;
RUBY_DLLSPEC extern VALUE rb_eSysStackError;
RUBY_DLLSPEC extern VALUE rb_eRegexpError;
RUBY_DLLSPEC extern VALUE rb_eScriptError;
RUBY_DLLSPEC extern VALUE rb_eNameError;
RUBY_DLLSPEC extern VALUE rb_eSyntaxError;
RUBY_DLLSPEC extern VALUE rb_eLoadError;

RUBY_DLLSPEC VALUE ruby_verbose(void);
RUBY_DLLSPEC VALUE ruby_debug(void);

RUBY_DLLSPEC extern const char* ruby_sourcefile;
RUBY_DLLSPEC const char *rb_sourcefile(void);
RUBY_DLLSPEC int rb_sourceline(void);

// TODO: get rjb to use a different #ifdef than "RUBINIUS"
// #define RUBINIUS 1
#define HAVE_RB_ERRINFO 1
#define HAVE_RB_SET_ERRINFO 1
RUBY_DLLSPEC VALUE rb_errinfo(void);
RUBY_DLLSPEC void rb_set_errinfo(VALUE err);

#define RUBY_METHOD_FUNC(func) ((VALUE (*)(ANYARGS))func)

#define ALLOCA_N(type,n) (type*)alloca(sizeof(type)*(n))

RUBY_DLLSPEC void ruby_setenv(const char* name, const char* value);
  
#undef setenv
#define setenv(name,val) ruby_setenv(name,val)

RUBY_DLLSPEC char* ruby_strdup(const char* str);
#undef strdup
#define strdup(s) ruby_strdup(s)

#ifdef  __cplusplus
#if 0
{ /* satisfy cc-mode */
#endif
}
#endif

#endif  /* JRUBY_RUBY_H */
