require 'rubygems/config_file'
require 'etc'

module Gem

  ConfigFile::PLATFORM_DEFAULTS['install'] = '--env-shebang'
  ConfigFile::PLATFORM_DEFAULTS['update']  = '--env-shebang'

  class << self
    alias_method :original_ensure_gem_subdirectories, :ensure_gem_subdirectories
    def ensure_gem_subdirectories(gemdir)
      original_ensure_gem_subdirectories(gemdir) if writable_path? gemdir
    end

    alias_method :original_set_paths, :set_paths
    def set_paths(gpaths)
      original_set_paths(gpaths)
      @gem_path.reject! {|p| !readable_path? p }
    end

    def readable_path?(p)
      p =~ /^file:/ || File.exists?(p)
    end

    def writable_path?(p)
      p !~ /^file:/ && File.exists?(p)
    end
  end

  # Default home directory path to be used if an alternate value is not
  # specified in the environment.
  #
  # JRuby: We don't want gems installed in lib/jruby/gems, but rather
  # to preserve the old location: lib/ruby/gems.
  def self.default_dir
    # TODO: use ~/.gems as the default dir when running under the complete jar, so the user can install gems?
    File.join ConfigMap[:libdir], 'ruby', 'gems', ConfigMap[:ruby_version]
  end

  ##
  # The path to the running Ruby interpreter.
  #
  # JRuby: Don't append ConfigMap[:EXEEXT] to @jruby, since that would
  # make it jruby.bat.bat on Windows.
  def self.ruby
    if @ruby.nil? then
      @ruby = File.join(ConfigMap[:bindir],
                        ConfigMap[:ruby_install_name])
      # @ruby << ConfigMap[:EXEEXT]
    end

    @ruby
  end

  ##
  # Is this a windows platform?
  #
  # JRuby: Look in CONFIG['host_os'] as well.
  def self.win_platform?
    if @@win_platform.nil? then
      @@win_platform = !!WIN_PATTERNS.find { |r| RUBY_PLATFORM =~ r || Config::CONFIG["host_os"] =~ r }
    end

    @@win_platform
  end

end

## JAR FILES: Allow gem path entries to contain jar files
require 'rubygems/source_index'
class Gem::SourceIndex
  class << self
    def installed_spec_directories
      # TODO: fix remaining glob tests
      Gem.path.collect do |dir|
        if File.file?(dir) && dir =~ /\.jar$/
          "file:#{dir}!/specifications"
        elsif File.directory?(dir) || dir =~ /^file:/
          File.join(dir, "specifications")
        end
      end.compact + spec_directories_from_classpath
    end

    def spec_directories_from_classpath
      require 'jruby'
      require 'uri'

      JRuby.runtime.getJRubyClassLoader.getResources("specifications").map do |u|

        if u.getProtocol == 'jar' and u.getFile =~ /^file:/
          file_url = URI.unescape(u.getFile)
        else
          file_url = u.getFile
        end

        file_url
      end
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

