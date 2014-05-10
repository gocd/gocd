ENV['RC_ARCHS'] = '' if RUBY_PLATFORM =~ /darwin/

# :stopdoc:

require 'mkmf'

RbConfig::MAKEFILE_CONFIG['CC'] = ENV['CC'] if ENV['CC']

ROOT = File.expand_path(File.join(File.dirname(__FILE__), '..', '..'))
LIBDIR = RbConfig::CONFIG['libdir']
@libdir_basename = "lib" # shrug, ruby 2.0 won't work for me.
INCLUDEDIR = RbConfig::CONFIG['includedir']

if defined?(RUBY_ENGINE) && RUBY_ENGINE == 'macruby'
  $LIBRUBYARG_STATIC.gsub!(/-static/, '')
end

$CFLAGS << " #{ENV["CFLAGS"]}"
$LIBS << " #{ENV["LIBS"]}"

windows_p = RbConfig::CONFIG['target_os'] == 'mingw32' || RbConfig::CONFIG['target_os'] =~ /mswin/

if windows_p
  $CFLAGS << " -DXP_WIN -DXP_WIN32 -DUSE_INCLUDED_VASPRINTF"
elsif RbConfig::CONFIG['target_os'] =~ /solaris/
  $CFLAGS << " -DUSE_INCLUDED_VASPRINTF"
else
  $CFLAGS << " -g -DXP_UNIX"
end

if RbConfig::MAKEFILE_CONFIG['CC'] =~ /mingw/
  $CFLAGS << " -DIN_LIBXML"
  $LIBS << " -lz" # TODO why is this necessary?
end

if RbConfig::MAKEFILE_CONFIG['CC'] =~ /gcc/
  $CFLAGS << " -O3" unless $CFLAGS[/-O\d/]
  $CFLAGS << " -Wall -Wcast-qual -Wwrite-strings -Wconversion -Wmissing-noreturn -Winline"
end

if windows_p
  # I'm cross compiling!
  HEADER_DIRS = [INCLUDEDIR]
  LIB_DIRS = [LIBDIR]
  XML2_HEADER_DIRS = [File.join(INCLUDEDIR, "libxml2"), INCLUDEDIR]

else
  if ENV['NOKOGIRI_USE_SYSTEM_LIBRARIES']
    HEADER_DIRS = [
      # First search /opt/local for macports
      '/opt/local/include',

      # Then search /usr/local for people that installed from source
      '/usr/local/include',

      # Check the ruby install locations
      INCLUDEDIR,

      # Finally fall back to /usr
      '/usr/include',
      '/usr/include/libxml2',
    ]

    LIB_DIRS = [
      # First search /opt/local for macports
      '/opt/local/lib',

      # Then search /usr/local for people that installed from source
      '/usr/local/lib',

      # Check the ruby install locations
      LIBDIR,

      # Finally fall back to /usr
      '/usr/lib',
    ]

    XML2_HEADER_DIRS = [
      '/opt/local/include/libxml2',
      '/usr/local/include/libxml2',
      File.join(INCLUDEDIR, "libxml2")
    ] + HEADER_DIRS

    # If the user has homebrew installed, use the libxml2 inside homebrew
    brew_prefix = `brew --prefix libxml2 2> /dev/null`.chomp
    unless brew_prefix.empty?
      LIB_DIRS.unshift File.join(brew_prefix, 'lib')
      XML2_HEADER_DIRS.unshift File.join(brew_prefix, 'include/libxml2')
    end

  else
    require 'mini_portile'
    require 'yaml'

    common_recipe = lambda do |recipe|
      recipe.target = File.join(ROOT, "ports")
      recipe.files = ["ftp://ftp.xmlsoft.org/libxml2/#{recipe.name}-#{recipe.version}.tar.gz"]

      checkpoint = "#{recipe.target}/#{recipe.name}-#{recipe.version}-#{recipe.host}.installed"
      unless File.exist?(checkpoint)
        recipe.cook
        FileUtils.touch checkpoint
      end
      recipe.activate
    end

    dependencies = YAML.load_file(File.join(ROOT, "dependencies.yml"))

    libxml2_recipe = MiniPortile.new("libxml2", dependencies["libxml2"]).tap do |recipe|
      recipe.configure_options = [
        "--enable-shared",
        "--disable-static",
        "--without-python",
        "--without-readline",
        "--with-c14n",
        "--with-debug",
        "--with-threads"
      ]
      common_recipe.call recipe
    end

    libxslt_recipe = MiniPortile.new("libxslt", dependencies["libxslt"]).tap do |recipe|
      recipe.configure_options = [
        "--enable-shared",
        "--disable-static",
        "--without-python",
        "--without-crypto",
        "--with-debug",
        "--with-libxml-prefix=#{libxml2_recipe.path}"
      ]
      common_recipe.call recipe
    end

    $LDFLAGS << " -Wl,-rpath,#{libxml2_recipe.path}/lib"
    $LDFLAGS << " -Wl,-rpath,#{libxslt_recipe.path}/lib"

    $CFLAGS << " -DNOKOGIRI_USE_PACKAGED_LIBRARIES -DNOKOGIRI_LIBXML2_PATH='\"#{libxml2_recipe.path}\"' -DNOKOGIRI_LIBXSLT_PATH='\"#{libxslt_recipe.path}\"'"

    HEADER_DIRS = [libxml2_recipe, libxslt_recipe].map { |f| File.join(f.path, "include") }
    LIB_DIRS = [libxml2_recipe, libxslt_recipe].map { |f| File.join(f.path, "lib") }
    XML2_HEADER_DIRS = HEADER_DIRS + [File.join(libxml2_recipe.path, "include", "libxml2")]
  end
end

dir_config('zlib', HEADER_DIRS, LIB_DIRS)
dir_config('iconv', HEADER_DIRS, LIB_DIRS)
dir_config('xml2', XML2_HEADER_DIRS, LIB_DIRS)
dir_config('xslt', HEADER_DIRS, LIB_DIRS)

def asplode(lib)
  abort "-----\n#{lib} is missing.  please visit http://nokogiri.org/tutorials/installing_nokogiri.html for help with installing dependencies.\n-----"
end

pkg_config('libxslt')
pkg_config('libxml-2.0')
pkg_config('libiconv')

def have_iconv?
  %w{ iconv_open libiconv_open }.any? do |method|
    have_func(method, 'iconv.h') or
      have_library('iconv', method, 'iconv.h') or
      find_library('iconv', method, 'iconv.h')
  end
end

asplode "libxml2"  unless find_header('libxml/parser.h')
asplode "libxslt"  unless find_header('libxslt/xslt.h')
asplode "libexslt" unless find_header('libexslt/exslt.h')
asplode "libiconv" unless have_iconv?
asplode "libxml2"  unless find_library("xml2", 'xmlParseDoc')
asplode "libxslt"  unless find_library("xslt", 'xsltParseStylesheetDoc')
asplode "libexslt" unless find_library("exslt", 'exsltFuncRegister')

unless have_func('xmlHasFeature')
  abort "-----\nThe function 'xmlHasFeature' is missing from your installation of libxml2.  Likely this means that your installed version of libxml2 is old enough that nokogiri will not work well.  To get around this problem, please upgrade your installation of libxml2.

Please visit http://nokogiri.org/tutorials/installing_nokogiri.html for more help!"
end

have_func 'xmlFirstElementChild'
have_func('xmlRelaxNGSetParserStructuredErrors')
have_func('xmlRelaxNGSetParserStructuredErrors')
have_func('xmlRelaxNGSetValidStructuredErrors')
have_func('xmlSchemaSetValidStructuredErrors')
have_func('xmlSchemaSetParserStructuredErrors')

if ENV['CPUPROFILE']
  unless find_library('profiler', 'ProfilerEnable', *LIB_DIRS)
    abort "google performance tools are not installed"
  end
end

create_makefile('nokogiri/nokogiri')

# :startdoc:
