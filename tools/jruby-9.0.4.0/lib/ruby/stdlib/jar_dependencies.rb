#
# Copyright (C) 2014 Christian Meier
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of
# this software and associated documentation files (the "Software"), to deal in
# the Software without restriction, including without limitation the rights to
# use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
# the Software, and to permit persons to whom the Software is furnished to do so,
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
# FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
# IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
module Jars
  unless defined? Jars::MAVEN_SETTINGS
    MAVEN_SETTINGS = 'JARS_MAVEN_SETTINGS'.freeze
    LOCAL_MAVEN_REPO = 'JARS_LOCAL_MAVEN_REPO'.freeze
    # lock file to use
    LOCK = 'JARS_LOCK'.freeze
    # where the locally stored jars are search for or stored
    HOME = 'JARS_HOME'.freeze
    # skip the gem post install hook
    SKIP = 'JARS_SKIP'.freeze
    # do not require any jars if set to false
    REQUIRE = 'JARS_REQUIRE'.freeze
    # @private
    NO_REQUIRE = 'JARS_NO_REQUIRE'.freeze
    # no more warnings on conflict. this still requires jars but will
    # not warn. it is needed to load jars from (default) gems which
    # do contribute to any dependency manager (maven, gradle, jbundler)
    QUIET = 'JARS_QUIET'.freeze
    # show maven output
    VERBOSE = 'JARS_VERBOSE'.freeze
    # maven debug
    DEBUG = 'JARS_DEBUG'.freeze
    # vendor jars inside gem when installing gem
    VENDOR = 'JARS_VENDOR'.freeze
    # resolve jars from Jars.lock
    RESOLVE = 'JARS_RESOLVE'.freeze
  end

  class << self

    if defined? JRUBY_VERSION
      def to_prop( key )
        key = key.gsub( '_', '.' )
        ENV_JAVA[ ( key.downcase!; key ) ] ||
          ENV[ ( key.gsub!( '.', '_' ); key.upcase!; key ) ]
      end
    else
      def to_prop( key )
        ENV[ key.gsub( '.', '_' ).upcase ]
      end
    end

    def to_boolean( key )
      return nil if ( prop = to_prop( key ) ).nil?
      prop.empty? || prop.eql?('true')
    end

    def skip?
      to_boolean( SKIP )
    end

    def require?
      @require = nil unless instance_variable_defined?(:@require)
      if @require.nil?
        if ( require = to_boolean( REQUIRE ) ).nil?
          no_require = to_boolean( NO_REQUIRE )
          @require = no_require.nil? ? true : ! no_require
        else
          @require = require
        end
      end
      @require
    end
    attr_writer :require

    def quiet?
      ( @silent ||= false ) || to_boolean( QUIET )
    end
    
    # @deprecated
    def no_require?; ! require? end

    def verbose?
      to_boolean( VERBOSE )
    end

    def debug?
      to_boolean( DEBUG )
    end

    def vendor?
      to_boolean( VENDOR )
    end

    def resolve?
      to_boolean( RESOLVE )
    end

    def no_more_warnings
      @silent = true
    end

    def freeze_loading
      self.require = false
    end

    def lock
      to_prop( LOCK ) || 'Jars.lock'
    end

    def lock_path( basedir = nil )
      deps = self.lock
      return deps if File.exists?( deps )
      deps = File.join( basedir || '.', self.lock )
      deps if File.exists?( deps )
    end

    def local_maven_repo
      to_prop( LOCAL_MAVEN_REPO ) || home
    end

    def reset
      instance_variables.each { |var| instance_variable_set(var, nil) }
      ( @@jars ||= {} ).clear
    end

    def maven_user_settings
      unless instance_variable_defined?(:@_jars_maven_user_settings_)
        @_jars_maven_user_settings_ = nil
      end
      if ( @_jars_maven_user_settings_ ||= nil ).nil?
        if settings = absolute( to_prop( MAVEN_SETTINGS ) )
          unless File.exists?(settings)
            warn "configured ENV['#{MAVEN_SETTINGS}'] = '#{settings}' not found"
            settings = false
          end
        else # use maven default (user) settings
          settings = File.join( user_home, '.m2', 'settings.xml' )
          settings = false unless File.exists?(settings)
        end
        @_jars_maven_user_settings_ = settings
      end
      @_jars_maven_user_settings_ || nil
    end
    alias maven_settings maven_user_settings

    def maven_global_settings
      unless instance_variable_defined?(:@_jars_maven_global_settings_)
        @_jars_maven_global_settings_ = nil
      end
      if @_jars_maven_global_settings_.nil?
          if mvn_home = ENV[ 'M2_HOME' ] || ENV[ 'MAVEN_HOME' ]
            settings = File.join( mvn_home, 'conf/settings.xml' )
            settings = false unless File.exists?(settings)
          else
            settings = false
          end
          @_jars_maven_global_settings_ = settings
      end
      @_jars_maven_global_settings_ || nil
    end

    def home
      @_jars_home_ ||= absolute(to_prop(HOME)) ||
                       detect_local_repository(maven_user_settings) ||
                       detect_local_repository(maven_global_settings) ||
                       File.join( user_home, '.m2', 'repository' )
    end

    def require_jars_lock!( scope = :runtime )
      if jars_lock = Jars.lock_path
        @@jars_lock = jars_lock
        # funny error during spec where it tries to load it again
        # and finds it as gem instead of the LOAD_PATH
        require 'jars/classpath' unless defined? Jars::Classpath
        classpath = Jars::Classpath.new( nil, jars_lock )
        classpath.require( scope )
        no_more_warnings
      end
    end

    def require_jars_lock
      @@jars_lock ||= false
      unless @@jars_lock
        require_jars_lock!
        @@jars_lock ||= true
      end
    end

    def mark_as_required( group_id, artifact_id, *classifier_version )
      require_jar_with_block( group_id, artifact_id, *classifier_version ) do
      end
    end

    def require_jar( group_id, artifact_id, *classifier_version )
      require_jars_lock
      require_jar_with_block( group_id, artifact_id, *classifier_version ) do |group_id, artifact_id, version, classifier|
        do_require( group_id, artifact_id, version, classifier )
      end
    end

    def warn(msg)
      Kernel.warn(msg) unless quiet?
    end

    private

    def require_jar_with_block( group_id, artifact_id, *classifier_version )
      version = classifier_version[ -1 ]
      classifier = classifier_version[ -2 ]

      @@jars ||= {}
      coordinate = "#{group_id}:#{artifact_id}"
      coordinate << ":#{classifier}" if classifier
      if @@jars.key? coordinate
        if @@jars[ coordinate ] == version
          false
        else
          @@jars[ coordinate ] # version of already registered jar
        end
      else
        yield group_id, artifact_id, version, classifier
        @@jars[ coordinate ] = version
        return true
      end
    end

    def absolute( file )
      File.expand_path( file ) if file
    end

    def user_home
      ENV[ 'HOME' ] || begin
        user_home = Dir.home if Dir.respond_to?(:home)
        unless user_home
          user_home = ENV_JAVA[ 'user.home' ] if Object.const_defined?(:ENV_JAVA)
        end
        user_home
      end
    end

    def detect_local_repository(settings)
      return nil unless settings

      doc = File.read( settings )
      # TODO filter out xml comments
      local_repo = doc.sub( /<\/localRepository>.*/m, '' ).sub( /.*<localRepository>/m, '' )
      # replace maven like system properties embedded into the string
      local_repo.gsub!( /\$\{[a-zA-Z.]+\}/ ) do |a|
        ENV_JAVA[ a[2..-2] ] || a
      end
      if local_repo.empty? or not File.exists?( local_repo )
        local_repo = nil
      end
      local_repo
    rescue
      warn "error reading or parsing #{settings}"
      nil
    end

    def to_jar( group_id, artifact_id, version, classifier = nil )
      file = "#{group_id.gsub( '.', '/' )}/#{artifact_id}/#{version}/#{artifact_id}-#{version}"
      file << "-#{classifier}" if classifier
      file << '.jar'
      file
    end

    def do_require( *args )
      jar = to_jar( *args )
      file = File.join( home, jar )
      # use jar from local repository if exists
      if File.exists?( file )
        require file
      else
        # otherwise try to find it on the load path
        require jar
      end
    rescue LoadError => e
      raise "\n\n\tyou might need to reinstall the gem which depends on the missing jar or in case there is Jars.lock then JARS_RESOLVE=true will install the missing jars\n\n" + e.message + " (LoadError)"
    end

  end # class << self

end

def require_jar( *args )
  return nil unless Jars.require?
  result = Jars.require_jar( *args )
  if result.is_a? String
    Jars.warn "jar coordinate #{args[0..-2].join( ':' )} already loaded with version #{result}"
    return false
  end
  result
end
