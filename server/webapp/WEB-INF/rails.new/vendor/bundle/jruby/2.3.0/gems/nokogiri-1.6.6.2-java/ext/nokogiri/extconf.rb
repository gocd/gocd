# :stopdoc:
ENV['RC_ARCHS'] = '' if RUBY_PLATFORM =~ /darwin/

require 'mkmf'

ROOT = File.expand_path(File.join(File.dirname(__FILE__), '..', '..'))

#
# functions
#

def do_help
  print <<HELP
usage: ruby #{$0} [options]

    --disable-clean
        Do not clean out intermediate files after successful build.

    --disable-static
        Do not statically link bundled libraries.

    --with-iconv-dir=DIR
        Use the iconv library placed under DIR.

    --with-zlib-dir=DIR
        Use the zlib library placed under DIR.

    --use-system-libraries
        Use system libraries intead of building and using the bundled
        libraries.

    --with-xml2-dir=DIR / --with-xml2-config=CONFIG
    --with-xslt-dir=DIR / --with-xslt-config=CONFIG
    --with-exslt-dir=DIR / --with-exslt-config=CONFIG
        Use libxml2/libxslt/libexslt as specified.

    --enable-cross-build
        Do cross-build.
HELP
  exit! 0
end

def do_clean
  require 'pathname'
  require 'fileutils'

  root = Pathname(ROOT)
  pwd  = Pathname(Dir.pwd)

  # Skip if this is a development work tree
  unless (root + '.git').exist?
    message "Cleaning files only used during build.\n"

    # (root + 'tmp') cannot be removed at this stage because
    # nokogiri.so is yet to be copied to lib.

    # clean the ports build directory
    Pathname.glob(pwd.join('tmp', '*', 'ports')) { |dir|
      FileUtils.rm_rf(dir, verbose: true)
      FileUtils.rmdir(dir.parent, parents: true, verbose: true)
    }

    if enable_config('static')
      # ports installation can be safely removed if statically linked.
      FileUtils.rm_rf(root + 'ports', verbose: true)
    else
      FileUtils.rm_rf(root + 'ports' + 'archives', verbose: true)
    end
  end

  exit! 0
end

def nokogiri_try_compile
  args = if defined?(RUBY_VERSION) && RUBY_VERSION <= "1.9.2"
           ["int main() {return 0;}"]
         else
           ["int main() {return 0;}", "", {werror: true}]
         end
  try_compile(*args)
end


def add_cflags(flags)
  print "checking if the C compiler accepts #{flags}... "
  with_cflags("#{$CFLAGS} #{flags}") do
    if nokogiri_try_compile
      puts 'yes'
      true
    else
      puts 'no'
      false
    end
  end
end

def preserving_globals
  values = [
    $arg_config,
    $CFLAGS, $CPPFLAGS,
    $LDFLAGS, $LIBPATH, $libs
  ].map(&:dup)
  yield
ensure
  $arg_config,
  $CFLAGS, $CPPFLAGS,
  $LDFLAGS, $LIBPATH, $libs =
    values
end

def asplode(lib)
  abort "-----\n#{lib} is missing.  Please locate mkmf.log to investigate how it is failing.\n-----"
end

def have_iconv?(using = nil)
  checking_for(using ? "iconv using #{using}" : 'iconv') {
    ['', '-liconv'].any? { |opt|
      preserving_globals {
        yield if block_given?

        try_link(<<-'SRC', opt)
#include <stdlib.h>
#include <iconv.h>

int main(void)
{
    iconv_t cd = iconv_open("", "");
    iconv(cd, NULL, NULL, NULL, NULL);
    return EXIT_SUCCESS;
}
        SRC
      }
    }
  }
end

def iconv_configure_flags
  # If --with-iconv-dir or --with-opt-dir is given, it should be
  # the first priority
  %w[iconv opt].each { |name|
    if (config = preserving_globals { dir_config(name) }).any? &&
       have_iconv?("--with-#{name}-* flags") { dir_config(name) }
      idirs, ldirs = config.map { |dirs|
        Array(dirs).flat_map { |dir|
          dir.split(File::PATH_SEPARATOR)
        } if dirs
      }

      return [
        '--with-iconv=yes',
        *("CPPFLAGS=#{idirs.map { |dir| '-I' << dir }.join(' ')}".quote if idirs),
        *("LDFLAGS=#{ldirs.map { |dir| '-L' << dir }.join(' ')}".quote if ldirs),
      ]
    end
  }

  if have_iconv?
    return ['--with-iconv=yes']
  end

  if (config = preserving_globals { pkg_config('libiconv') }) &&
     have_iconv?('pkg-config libiconv') { pkg_config('libiconv') }
    cflags, ldflags, libs = config

    return [
      '--with-iconv=yes',
      "CPPFLAGS=#{cflags}".quote,
      "LDFLAGS=#{ldflags}".quote,
      "LIBS=#{libs}".quote,
    ]
  end

  asplode "libiconv"
end

def process_recipe(name, version, static_p, cross_p)
  MiniPortile.new(name, version).tap do |recipe|
    recipe.target = portsdir = File.join(ROOT, "ports")
    # Prefer host_alias over host in order to use i586-mingw32msvc as
    # correct compiler prefix for cross build, but use host if not set.
    recipe.host = RbConfig::CONFIG["host_alias"].empty? ? RbConfig::CONFIG["host"] : RbConfig::CONFIG["host_alias"]
    recipe.patch_files = Dir[File.join(portsdir, "patches", name, "*.patch")].sort

    yield recipe

    env = Hash.new { |hash, key|
      hash[key] = "#{ENV[key]}"  # (ENV[key].dup rescue '')
    }

    recipe.configure_options.flatten!

    recipe.configure_options.delete_if { |option|
      case option.shellsplit.first
      when /\A(\w+)=(.*)\z/
        env[$1] = $2
        true
      else
        false
      end
    }

    if static_p
      recipe.configure_options += [
        "--disable-shared",
        "--enable-static",
      ]
      env['CFLAGS'] = "-fPIC #{env['CFLAGS']}"
    else
      recipe.configure_options += [
        "--enable-shared",
        "--disable-static",
      ]
    end

    if cross_p
      recipe.configure_options += [
        "--target=#{recipe.host}",
        "--host=#{recipe.host}",
      ]
    end

    if RbConfig::CONFIG['target_cpu'] == 'universal'
      %w[CFLAGS LDFLAGS].each { |key|
        unless env[key].shellsplit.include?('-arch')
          env[key] << ' ' << RbConfig::CONFIG['ARCH_FLAG']
        end
      }
    end

    recipe.configure_options += env.map { |key, value|
      "#{key}=#{value}".shellescape
    }

    message <<-"EOS"
************************************************************************
IMPORTANT NOTICE:

Building Nokogiri with a packaged version of #{name}-#{version}#{'.' if recipe.patch_files.empty?}
    EOS

    unless recipe.patch_files.empty?
      message "with the following patches applied:\n"

      recipe.patch_files.each { |patch|
        message "\t- %s\n" % File.basename(patch)
      }
    end

    message <<-"EOS"

Team Nokogiri will keep on doing their best to provide security
updates in a timely manner, but if this is a concern for you and want
to use the system library instead; abort this installation process and
reinstall nokogiri as follows:

    gem install nokogiri -- --use-system-libraries
        [--with-xml2-config=/path/to/xml2-config]
        [--with-xslt-config=/path/to/xslt-config]

If you are using Bundler, tell it to use the option:

    bundle config build.nokogiri --use-system-libraries
    bundle install
    EOS

    message <<-"EOS" if name == 'libxml2'

Note, however, that nokogiri is not fully compatible with arbitrary
versions of libxml2 provided by OS/package vendors.
    EOS

    message <<-"EOS"
************************************************************************
    EOS

    checkpoint = "#{recipe.target}/#{recipe.name}-#{recipe.version}-#{recipe.host}.installed"
    unless File.exist?(checkpoint)
      recipe.cook
      FileUtils.touch checkpoint
    end
    recipe.activate
  end
end

def lib_a(ldflag)
  case ldflag
  when /\A-l(.+)/
    "lib#{$1}.#{$LIBEXT}"
  end
end

def using_system_libraries?
  arg_config('--use-system-libraries', !!ENV['NOKOGIRI_USE_SYSTEM_LIBRARIES'])
end

#
# monkey patches
#

# Workaround for Ruby bug #8074, introduced in Ruby 2.0.0, fixed in Ruby 2.1.0
# https://bugs.ruby-lang.org/issues/8074
@libdir_basename = "lib" if RUBY_VERSION < '2.1.0'

# Workaround for #1102
def monkey_patch_mini_portile
  MiniPortile.class_eval do
    def patch
      @patch_files.each do |full_path|
        next unless File.exists?(full_path)
        output "Running patch with #{full_path}..."
        execute('patch', %Q(patch -p1 < "#{full_path}"))
      end
    end
  end
end

#
# main
#

case
when arg_config('--help')
  do_help
when arg_config('--clean')
  do_clean
end

RbConfig::MAKEFILE_CONFIG['CC'] = ENV['CC'] if ENV['CC']

if defined?(RUBY_ENGINE) && RUBY_ENGINE == 'macruby'
  $LIBRUBYARG_STATIC.gsub!(/-static/, '')
end

$LIBS << " #{ENV["LIBS"]}"

# Read CFLAGS from ENV and make sure compiling works.
add_cflags(ENV["CFLAGS"])

case RbConfig::CONFIG['target_os']
when 'mingw32', /mswin/
  windows_p = true
  $CFLAGS << " -DXP_WIN -DXP_WIN32 -DUSE_INCLUDED_VASPRINTF"
when /solaris/
  $CFLAGS << " -DUSE_INCLUDED_VASPRINTF"
when /darwin/
  darwin_p = true
  # Let Apple LLVM/clang 5.1 ignore unknown compiler flags
  add_cflags("-Wno-error=unused-command-line-argument-hard-error-in-future")
else
  $CFLAGS << " -g -DXP_UNIX"
end

if RUBY_PLATFORM =~ /mingw/i
  # Work around a character escaping bug in MSYS by passing an arbitrary
  # double quoted parameter to gcc. See https://sourceforge.net/p/mingw/bugs/2142
  $CPPFLAGS << ' "-Idummypath"'
end

if RbConfig::MAKEFILE_CONFIG['CC'] =~ /gcc/
  $CFLAGS << " -O3" unless $CFLAGS[/-O\d/]
  $CFLAGS << " -Wall -Wcast-qual -Wwrite-strings -Wconversion -Wmissing-noreturn -Winline"
end

case
when using_system_libraries?
  message "Building nokogiri using system libraries.\n"

  dir_config('zlib')

  # Using system libraries means we rely on the system libxml2 with
  # regard to the iconv support.

  dir_config('xml2').any?  or pkg_config('libxml-2.0')
  dir_config('xslt').any?  or pkg_config('libxslt')
  dir_config('exslt').any? or pkg_config('libexslt')

  try_cpp(<<-SRC) or abort "libxml2 version 2.6.21 or later is required!"
#include <libxml/xmlversion.h>

#if LIBXML_VERSION < 20621
#error libxml2 is way too old
#endif
  SRC

  try_cpp(<<-SRC) or warn "libxml2 version 2.9.2 or later is highly recommended, but proceeding anyway."
#include <libxml/xmlversion.h>

#if LIBXML_VERSION < 20902
#error libxml2 is too old
#endif
  SRC
else
  message "Building nokogiri using packaged libraries.\n"

  require 'mini_portile'
  monkey_patch_mini_portile
  require 'yaml'

  static_p = enable_config('static', true) or
    message "Static linking is disabled.\n"

  dir_config('zlib')

  dependencies = YAML.load_file(File.join(ROOT, "dependencies.yml"))

  cross_build_p = enable_config("cross-build")
  if cross_build_p || windows_p
    zlib_recipe = process_recipe("zlib", dependencies["zlib"], static_p, cross_build_p) do |recipe|
      recipe.files = ["http://zlib.net/#{recipe.name}-#{recipe.version}.tar.gz"]
      class << recipe
        attr_accessor :cross_build_p

        def configure
          Dir.chdir work_path do
            mk = File.read 'win32/Makefile.gcc'
            File.open 'win32/Makefile.gcc', 'wb' do |f|
              f.puts "BINARY_PATH = #{path}/bin"
              f.puts "LIBRARY_PATH = #{path}/lib"
              f.puts "INCLUDE_PATH = #{path}/include"
              mk.sub!(/^PREFIX\s*=\s*$/, "PREFIX = #{host}-") if cross_build_p
              f.puts mk
            end
          end
        end

        def configured?
          Dir.chdir work_path do
            !! (File.read('win32/Makefile.gcc') =~ /^BINARY_PATH/)
          end
        end

        def compile
          execute "compile", "make -f win32/Makefile.gcc"
        end

        def install
          execute "install", "make -f win32/Makefile.gcc install"
        end
      end
      recipe.cross_build_p = cross_build_p
    end

    libiconv_recipe = process_recipe("libiconv", dependencies["libiconv"], static_p, cross_build_p) do |recipe|
      recipe.files = ["http://ftp.gnu.org/pub/gnu/libiconv/#{recipe.name}-#{recipe.version}.tar.gz"]
      recipe.configure_options += [
        "CPPFLAGS='-Wall'",
        "CFLAGS='-O2 -g'",
        "CXXFLAGS='-O2 -g'",
        "LDFLAGS="
      ]
    end
  else
    if darwin_p && !File.exist?('/usr/include/iconv.h')
      abort <<'EOM'.chomp
-----
The file "/usr/include/iconv.h" is missing in your build environment,
which means you haven't installed Xcode Command Line Tools properly.

To install Command Line Tools, try running `xcode-select --install` on
terminal and follow the instructions.  If it fails, open Xcode.app,
select from the menu "Xcode" - "Open Developer Tool" - "More Developer
Tools" to open the developer site, download the installer for your OS
version and run it.
-----
EOM
    end
  end

  unless windows_p
    preserving_globals {
      have_library('z', 'gzdopen', 'zlib.h')
    } or abort 'zlib is missing; necessary for building libxml2'
  end

  libxml2_recipe = process_recipe("libxml2", dependencies["libxml2"], static_p, cross_build_p) do |recipe|
    recipe.files = ["ftp://ftp.xmlsoft.org/libxml2/#{recipe.name}-#{recipe.version}.tar.gz"]
    recipe.configure_options += [
      "--without-python",
      "--without-readline",
      *(libiconv_recipe ? "--with-iconv=#{libiconv_recipe.path}" : iconv_configure_flags),
      "--with-c14n",
      "--with-debug",
      "--with-threads"
    ]
  end

  libxslt_recipe = process_recipe("libxslt", dependencies["libxslt"], static_p, cross_build_p) do |recipe|
    recipe.files = ["ftp://ftp.xmlsoft.org/libxml2/#{recipe.name}-#{recipe.version}.tar.gz"]
    recipe.configure_options += [
      "--without-python",
      "--without-crypto",
      "--with-debug",
      "--with-libxml-prefix=#{libxml2_recipe.path}"
    ]
  end

  $CFLAGS << ' ' << '-DNOKOGIRI_USE_PACKAGED_LIBRARIES'
  $LIBPATH = ["#{zlib_recipe.path}/lib"] | $LIBPATH if zlib_recipe
  $LIBPATH = ["#{libiconv_recipe.path}/lib"] | $LIBPATH if libiconv_recipe

  have_lzma = preserving_globals {
    have_library('lzma')
  }

  $libs = $libs.shellsplit.tap { |libs|
    [libxml2_recipe, libxslt_recipe].each { |recipe|
      libname = recipe.name[/\Alib(.+)\z/, 1]
      File.join(recipe.path, "bin", "#{libname}-config").tap { |config|
        # call config scripts explicit with 'sh' for compat with Windows
        $CPPFLAGS = `sh #{config} --cflags`.strip << ' ' << $CPPFLAGS
        `sh #{config} --libs`.strip.shellsplit.each { |arg|
          case arg
          when /\A-L(.+)\z/
            # Prioritize ports' directories
            if $1.start_with?(ROOT + '/')
              $LIBPATH = [$1] | $LIBPATH
            else
              $LIBPATH = $LIBPATH | [$1]
            end
          when /\A-l./
            libs.unshift(arg)
          else
            $LDFLAGS << ' ' << arg.shellescape
          end
        }
      }

      # Defining a macro that expands to a C string; double quotes are significant.
      $CPPFLAGS << ' ' << "-DNOKOGIRI_#{recipe.name.upcase}_PATH=\"#{recipe.path}\"".shellescape
      $CPPFLAGS << ' ' << "-DNOKOGIRI_#{recipe.name.upcase}_PATCHES=\"#{recipe.patch_files.map { |path| File.basename(path) }.join(' ')}\"".shellescape

      case libname
      when 'xml2'
        # xslt-config --libs or pkg-config libxslt --libs does not include
        # -llzma, so we need to add it manually when linking statically.
        if static_p && have_lzma
          # Add it at the end; GH #988
          libs << '-llzma'
        end
      when 'xslt'
        # xslt-config does not have a flag to emit options including
        # -lexslt, so add it manually.
        libs.unshift('-lexslt')
      end
    }
  }.shelljoin

  if static_p
    $libs = $libs.shellsplit.map { |arg|
      case arg
      when '-lxml2'
        File.join(libxml2_recipe.path, 'lib', lib_a(arg))
      when '-lxslt', '-lexslt'
        File.join(libxslt_recipe.path, 'lib', lib_a(arg))
      else
        arg
      end
    }.shelljoin
  end
end

{
  "xml2"  => ['xmlParseDoc',            'libxml/parser.h'],
  "xslt"  => ['xsltParseStylesheetDoc', 'libxslt/xslt.h'],
  "exslt" => ['exsltFuncRegister',      'libexslt/exslt.h'],
}.each { |lib, (func, header)|
  have_func(func, header) ||
  have_library(lib, func, header) ||
  have_library("lib#{lib}", func, header) or
    asplode("lib#{lib}")
}

have_func('xmlHasFeature') or abort "xmlHasFeature() is missing."
have_func('xmlFirstElementChild')
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

if enable_config('clean', true)
  # Do not clean if run in a development work tree.
  File.open('Makefile', 'at') { |mk|
    mk.print <<EOF
all: clean-ports

clean-ports: $(DLLIB)
	-$(Q)$(RUBY) $(srcdir)/extconf.rb --clean --#{static_p ? 'enable' : 'disable'}-static
EOF
  }
end

# :startdoc:
