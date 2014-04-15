#if defined(_LP64) || defined(__LP64__)
# define SIZEOF_LONG 8
# define SIZEOF_VOIDP 8
# define SIZEOF_OFF_T 8
#elif defined(_LLP64) || defined(__LLP64__)
# define SIZEOF_LONG 4
# define SIZEOF_VOIDP 8
# define SIZEOF_OFF_T 4
#else
# define SIZEOF_LONG 4
# define SIZEOF_VOIDP 4
# define SIZEOF_OFF_T 4
#endif
#define SIZEOF_LONG_LONG 8
