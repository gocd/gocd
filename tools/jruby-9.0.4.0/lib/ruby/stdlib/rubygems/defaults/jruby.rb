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
        ruby_path = "#{org.jruby.util.ClasspathLauncher.jrubyCommand(JRuby.runtime)}"
      end
      ruby_path
    end

    def jarred_path?(p)
      p =~ /^(file|uri|jar|classpath):/
    end

    # A jar path looks like this on non-Windows platforms:
    #   file:/path/to/file.jar!/path/within/jar/to/file.txt
    # and like this on Windows:
    #   file:/C:/path/to/file.jar!/path/within/jar/to/file.txt
    #
    # This method returns:
    #   /path/to/file.jar
    # or
    #   C:/path/to/file.jar
    # as appropriate.
    def jar_path(p)
      path = p.sub(/^file:/, "").sub(/!.*/, "")
      path = path.sub(/^\//, "") if win_platform? && path =~ /^\/[A-Za-z]:/
      path
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
        File.join(ConfigMap[:libdir], 'ruby', 'stdlib'),
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
    /(?<!jar:file|jar|file|classpath|uri:classloader|uri):/
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
        elsif File.directory?(File.join(dir, "specifications")) || dir =~ /^file:/
          File.join(dir, "specifications")
        end
      }.compact + spec_directories_from_classpath
    end

    def add_dir dir
      new_dirs = [ dir ] + (@@dirs||[]).collect { |d| d.sub(/.specifications/, '') }
      self.reset

      # ugh
      @@dirs = new_dirs.map { |d| File.join d, "specifications" }
    end

    # Replace existing dirs=
    def dirs= dirs
      self.reset

      # ugh
      @@dirs = Array(dirs).map { |d| File.join d, "specifications" } + spec_directories_from_classpath
    end

    def spec_directories_from_classpath
      stuff = [ 'uri:classloader://specifications' ]
      JRuby.runtime.instance_config.extra_gem_paths.each do |path|
        stuff << File.join(path, 'specifications')
      end
      stuff += JRuby::Util.classloader_resources('specifications')
      # some classloader return directory info. use only the "protocols"
      # which jruby understands
      stuff.select { |s| File.directory?( s ) }
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

begin
  require 'jar_install_post_install_hook'
rescue LoadError
end
