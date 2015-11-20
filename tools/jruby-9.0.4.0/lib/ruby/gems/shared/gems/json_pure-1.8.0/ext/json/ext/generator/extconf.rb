require 'mkmf'

unless $CFLAGS.gsub!(/ -O[\dsz]?/, ' -O3')
  $CFLAGS << ' -O3'
end
if CONFIG['CC'] =~ /gcc/
  $CFLAGS << ' -Wall'
  unless $DEBUG && !$CFLAGS.gsub!(/ -O[\dsz]?/, ' -O0 -ggdb')
    $CFLAGS << ' -O0 -ggdb'
  end
end

$defs << "-DJSON_GENERATOR"
create_makefile 'json/ext/generator'
