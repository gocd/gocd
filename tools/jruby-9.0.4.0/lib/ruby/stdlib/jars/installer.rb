require 'jar_dependencies'
require 'jars/maven_exec'

module Jars
  class Installer

    class Dependency

      attr_reader :path, :file, :gav, :scope, :type, :coord

      def self.new( line )
        if line.match /:jar:|:pom:/
          super
        end
      end

      def setup_type( line )
        if line.index(':pom:')
          @type = :pom
        elsif line.index(':jar:')
          @type = :jar
        end
      end
      private :setup_type

      def setup_scope( line )
        @scope =
          case line
          when /:provided:/
            :provided
          when /:test:/
            :test
          else
            :runtime
          end
      end
      private :setup_scope

      def initialize( line )
        setup_type( line )

        line.sub!( /^\s+/, empty = '' )
        @coord = line.sub( /:[^:]+:([A-Z]:\\)?[^:]+$/, empty )
        first, second = @coord.split( /:#{type}:/ )
        group_id, artifact_id = first.split( /:/ )
        parts = group_id.split( '.' )
        parts << artifact_id
        parts << second.split( ':' )[ -1 ]
        parts << File.basename( line.sub( /.:/, empty ) )
        @path = File.join( parts ).strip

        setup_scope( line )

        reg = /:jar:|:pom:|:test:|:compile:|:runtime:|:provided:|:system:/
        @file = line.slice(@coord.length, line.length).sub(reg, empty).strip
        @system = line.index(':system:') != nil
        @gav = @coord.sub(reg, ':')
      end

      def system?
        @system
      end
    end

    def self.install_jars( write_require_file = false )
      new.install_jars( write_require_file )
    end

    def self.vendor_jars( write_require_file = false )
      new.vendor_jars( write_require_file )
    end

    def self.load_from_maven( file )
      result = []
      File.read( file ).each_line do |line|
        dep = Dependency.new( line )
        result << dep if dep && dep.scope == :runtime
      end
      result
    end

    def self.write_require_file( require_filename )
      FileUtils.mkdir_p( File.dirname( require_filename ) )
      comment = '# this is a generated file, to avoid over-writing it just delete this comment'
      if ! File.exists?( require_filename ) || File.read( require_filename ).match( comment )
        f = File.open( require_filename, 'w' )
        f.puts comment
        f.puts "require 'jar_dependencies'"
        f.puts
        f
      end
    end

    def self.vendor_file( dir, dep )
      vendored = File.join( dir, dep.path )
      FileUtils.mkdir_p( File.dirname( vendored ) )
      FileUtils.cp( dep.file, vendored ) unless dep.system?
    end

    def self.write_dep( file, dir, dep, vendor )
      return if dep.type != :jar || dep.scope != :runtime
      if dep.system?
        file.puts( "require( '#{dep.file}' )" ) if file
      elsif dep.scope == :runtime
        vendor_file( dir, dep ) if vendor
        file.puts( "require_jar( '#{dep.gav.gsub( ':', "', '" )}' )" ) if file
      end
    end

    def self.install_deps( deps, dir, require_filename, vendor )
      f = write_require_file( require_filename ) if require_filename
      deps.each do |dep|
        write_dep( f, dir, dep, vendor )
      end
      yield f if block_given? and f
    ensure
      f.close if f
    end

    def initialize( spec = nil )
      @mvn = MavenExec.new( spec )
    end

    def spec; @mvn.spec end

    def vendor_jars( write_require_file = true )
      return unless has_jars?
      case Jars.to_prop( Jars::VENDOR )
      when 'true'
        do_vendor = true
      when 'false'
        do_vendor = false
      else
        # if the spec_file does not exists this means it is a local gem
        # coming via bundle :path or :git
        do_vendor = File.exists?( spec.spec_file )
      end
      do_install( do_vendor, write_require_file )
    end

    def install_jars( write_require_file = true )
      return unless has_jars?
      do_install( false, write_require_file )
    end

    def ruby_maven_install_options=( options )
      @mvn.ruby_maven_install_options=( options )
    end

    def has_jars?
      # first look if there are any requirements in the spec
      # and then if gem depends on jar-dependencies for runtime.
      # only then install the jars declared in the requirements
      result = ( spec = self.spec ) && ! spec.requirements.empty? &&
        spec.dependencies.detect { |d| d.name == 'jar-dependencies' && d.type == :runtime } != nil
      if result && spec.platform.to_s != 'java'
        Jars.warn "\njar dependencies found on non-java platform gem - do not install jars\n"
        false
      else
        result
      end
    end
    alias_method :jars?, :has_jars?

    private

    def do_install( vendor, write_require_file )
      vendor_dir = File.join( @mvn.basedir, spec.require_path )
      jars_file = File.join( vendor_dir, "#{spec.name}_jars.rb" )

      # write out new jars_file it write_require_file is true or
      # check timestamps:
      # do not generate file if specfile is older then the generated file
      if ! write_require_file &&
          File.exists?( jars_file ) &&
          File.mtime( @mvn.specfile ) < File.mtime( jars_file )
        # leave jars_file as is
        jars_file = nil
      end
      self.class.install_deps( install_dependencies, vendor_dir,
                               jars_file, vendor )
    end

    def install_dependencies
      deps = File.join( @mvn.basedir, 'deps.lst' )

      puts "  jar dependencies for #{spec.spec_name} . . ." unless Jars.quiet?
      @mvn.resolve_dependencies_list( deps )

      self.class.load_from_maven( deps )
    ensure
      FileUtils.rm_f( deps ) if deps
    end
  end
  # to stay backward compatible
  JarInstaller = Installer unless defined? JarInstaller
end
