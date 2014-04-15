require 'mkmf'
require 'rbconfig'

if CONFIG['CC'] =~ /gcc/
  $CFLAGS += ' -Wall'
  #$CFLAGS += ' -O0 -ggdb'
end

have_header("ruby/st.h") || have_header("st.h")
have_header("re.h")
create_makefile 'parser'
