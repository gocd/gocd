require 'jar_dependencies'

module Jars
  class MavenExec

    def find_spec( allow_no_file )
      specs = Dir[ '*.gemspec' ]
      case specs.size
      when 0
        raise 'no gemspec found' unless allow_no_file
      when 1
        specs.first
      else
        raise 'more then one gemspec found. please specify a specfile' unless allow_no_file
      end
    end
    private :find_spec

    attr_reader :basedir, :spec, :specfile

    def initialize( spec = nil )
      setup( spec )
    end

    def setup( spec = nil, allow_no_file = false )
      spec ||= find_spec( allow_no_file )

      case spec
      when String
        @specfile = File.expand_path( spec )
        @basedir = File.dirname( @specfile )
        spec =  Dir.chdir( File.dirname(@specfile) ) do
          eval( File.read( @specfile ) )
        end
      when Gem::Specification
        if File.exists?( spec.spec_file )
          @basedir = spec.gem_dir
          @specfile = spec.spec_file
        else
          # this happens with bundle and local gems
          # there the spec_file is "not installed" but inside
          # the gem_dir directory
          Dir.chdir( spec.gem_dir ) do
            setup( nil, true )
          end
        end
      when NilClass
      else
        raise 'spec must be either String or Gem::Specification'
      end

      @spec = spec
    rescue
      # for all those strange gemspec we skip looking for jar-dependencies
    end

    def ruby_maven_install_options=( options )
      @options = options.dup
      @options.delete( :ignore_dependencies )
    end

    def resolve_dependencies_list( file )
      do_resolve_dependencies( *setup_arguments( 'jar_pom.rb', 'dependency:copy-dependencies', 'dependency:list', "-DoutputFile=#{file}" ) )
    end

    def resolve_dependencies( file )
      do_resolve_dependencies( *setup_arguments( 'jars_lock_pom.rb', 'dependency:copy-dependencies', '-DexcludeTransitive=true' , "-Djars.lock=#{file}") )
    end

    private

    def do_resolve_dependencies( *args )
      lazy_load_maven

      maven = Maven::Ruby::Maven.new
      maven.verbose = Jars.verbose?
      maven.exec( *args )
    end

    def setup_arguments( pom, *goals )
      args = goals.dup
      args << '-DoutputAbsoluteArtifactFilename=true'
      args << '-DincludeTypes=jar'
      args << '-DoutputScope=true'
      args << '-DuseRepositoryLayout=true'
      args << "-DoutputDirectory=#{Jars.home}"
      args << '-f' << "#{File.dirname( __FILE__ )}/#{pom}"
      args << "-Djars.specfile=#{@specfile}"

      if Jars.debug?
        args << '-X'
      elsif not Jars.verbose?
        args << '--quiet'
      end

      # TODO what todo with https proxy ?
      # FIX this proxy settings seems not to work
      if (proxy = Gem.configuration[ :http_proxy ]).is_a?( String )
        require 'uri'; uri = URI.parse( proxy )
        args << "-DproxySet=true"
        args << "-DproxyHost=#{uri.host}"
        args << "-DproxyPort=#{uri.port}"
      end

      if Jars.maven_settings
        args << '-s'
        args << Jars.maven_settings
      end

      args << "-Dmaven.repo.local=#{java.io.File.new( Jars.local_maven_repo ).absolute_path}"

      args
    end

    def lazy_load_maven
      add_gem_to_load_path( 'ruby-maven-libs' )
      add_gem_to_load_path( 'ruby-maven' )
      require 'maven/ruby/maven'
    end

    def find_spec_via_rubygems( name )
      require 'rubygems/dependency'
      dep = Gem::Dependency.new( name )
      dep.matching_specs( true ).last
    end

    def add_gem_to_load_path( name )
      # if the gem is already activated => good
      return if Gem.loaded_specs[ name ]
      # just install gem if needed and add it to the load_path
      # and leave activated gems as they are
      unless spec = find_spec_via_rubygems( name )
        spec = install_gem( name )
      end
      unless spec
        raise "failed to resolve gem '#{name}' if you're using Bundler add it as a dependency"
      end
      $LOAD_PATH << File.join( spec.full_gem_path, spec.require_path )
    end

    def install_gem( name )
      require 'rubygems/dependency_installer'
      jars = Gem.loaded_specs[ 'jar-dependencies' ]
      dep = jars.dependencies.detect { |d| d.name == name }
      req = dep.nil? ? Gem::Requirement.create( '>0' ) : dep.requirement
      inst = Gem::DependencyInstaller.new( @options ||= {} )
      inst.install( name, req ).first
    rescue => e
      warn e.backtrace.join( "\n" ) if Jars.verbose?
      raise "there was an error installing '#{name}'. please install it manually: #{e.inspect}"
    end
  end
end
