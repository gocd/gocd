require "mkmf"

if RUBY_VERSION >= "1.9"
  STDERR.print("Ruby version #{RUBY_VERSION} is too new\n")
  exit(1)
elsif RUBY_VERSION >= "1.8"
  if RUBY_RELEASE_DATE < "2005-03-22"
    STDERR.print("Ruby release date #{RUBY_RELEASE_DATE} is too old\n")
    exit(1)
  end
else
  STDERR.print("Ruby version is not compatible for this version.\n")
  exit(1)
end

# Allow use customization of compile options. For example, the
# following lines could be put in config_options to to turn off
# optimization:
#   $CFLAGS='-fPIC -fno-strict-aliasing -g3 -ggdb -O2 -fPIC'
config_file = File.join(File.dirname(__FILE__), 'config_options.rb')
load config_file if File.exist?(config_file)

create_makefile("ruby_debug")
