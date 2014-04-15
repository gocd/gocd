require 'rubygems/config_file'
require 'rbconfig'
require 'jruby/util'

module Gem

  ConfigFile::PLATFORM_DEFAULTS['install'] = '--no-rdoc --no-ri --env-shebang'
  ConfigFile::PLATFORM_DEFAULTS['update']  = '--no-rdoc --no-ri --env-shebang'

  class << self
    alias_method :original_ruby, :ruby
    def ruby
      ruby_path = original_ruby
      if jarred_path?(ruby_path)
        ruby_path = "java -jar #{ruby_path.sub(/^file:/,"").sub(/!.*/,"")}"
      end
      ruby_path
    end

    def jarred_path?(p)
      p =~ /^file:/
    end
  end

  # Default home directory path to be used if an alternate value is not
  # specified in the environment.
  #
  # JRuby: We don't want gems installed in lib/jruby/gems, but rather
  # to preserve the old location: lib/ruby/gems.
  def self.default_dir
    dir = RbConfig::CONFIG["default_gem_home"]
    dir ||= File.join(ConfigMap[:libdir], 'ruby', 'gems', 'shared')
    dir
  end

  # Default locations for RubyGems' .rb and bin files
  def self.default_rubygems_dirs
    [
        File.join(ConfigMap[:libdir], 'ruby', 'shared'),
        ConfigMap[:bindir]
    ]
  end

  ##
  # Is this a windows platform?
  #
  # JRuby: Look in CONFIG['host_os'] as well.
  def self.win_platform?
    if @@win_platform.nil? then
      @@win_platform = !!WIN_PATTERNS.find { |r| RUBY_PLATFORM =~ r || RbConfig::CONFIG["host_os"] =~ r }
    end

    @@win_platform
  end

  # Allow specifying jar and classpath type gem path entries
  def self.path_separator
    return File::PATH_SEPARATOR unless File::PATH_SEPARATOR == ':'
    /(?<!jar:file|jar|file|classpath):/
  end
end

## JAR FILES: Allow gem path entries to contain jar files
class Gem::Specification
  class << self
    # Replace existing dirs
    def dirs
      @@dirs ||= Gem.path.collect {|dir|
        if File.file?(dir) && dir =~ /\.jar$/
          "file:#{dir}!/specifications"
        elsif File.directory?(dir) || dir =~ /^file:/
          File.join(dir, "specifications")
        end
      }.compact + spec_directories_from_classpath
    end

    # Replace existing dirs=
    def dirs= dirs
      self.reset

      # ugh
      @@dirs = Array(dirs).map { |dir| File.join dir, "specifications" } + spec_directories_from_classpath
    end

    def spec_directories_from_classpath
      stuff = JRuby::Util.classloader_resources("specifications")
    end
  end
end
## END JAR FILES

if (Gem::win_platform?)
  module Process
    def self.uid
      0
    end
  end
end

# Check for jruby_native and load it if present. jruby_native
# indicates the native launcher is installed and will override
# env-shebang and possibly other options.
begin
  require 'rubygems/defaults/jruby_native'
rescue LoadError
end
