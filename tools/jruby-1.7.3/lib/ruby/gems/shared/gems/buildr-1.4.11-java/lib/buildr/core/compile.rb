# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr

  # The underlying compiler used by CompileTask.
  # To add a new compiler, extend Compiler::Base and add your compiler using:
  #   Buildr::Compiler.add MyCompiler
  module Compiler

    class << self

      # Returns true if the specified compiler exists.
      def has?(name)
        compilers.any? { |compiler| compiler.to_sym == name.to_sym }
      end

      # Select a compiler by its name.
      def select(name)
        compilers.detect { |compiler| compiler.to_sym == name.to_sym }
      end

      # Adds a compiler to the list of supported compiler.
      #
      # For example:
      #   Buildr::Compiler << Buildr::Javac
      def add(compiler)
        @compilers ||= []
        @compilers |= [compiler]
      end
      alias :<< :add

      # Returns a list of available compilers.
      def compilers
        @compilers ||= []
      end

      private

      # Only used by our specs.
      def compilers=(compilers)
        @compilers = compilers
      end
    end

    # Base class for all compilers, with common functionality.  Extend and over-ride as you see fit
    # (see Javac as an example).
    class Base #:nodoc:

      class << self

        # The compiler's identifier (e.g. :javac).  Inferred from the class name.
        def to_sym
          @symbol ||= name.split('::').last.downcase.to_sym
        end

        # The compiled language (e.g. :java).
        attr_reader :language
        # Source directories to use if none were specified (e.g. 'java').  Defaults to #language.
        attr_reader :sources
        # Extension for source files (e.g. 'java').  Defaults to language.
        attr_reader :source_ext
        # The target path (e.g. 'classes')
        attr_reader :target
        # Extension for target files (e.g. 'class').
        attr_reader :target_ext
        # The default packaging type (e.g. :jar).
        attr_reader :packaging

        # Returns true if this compiler applies to any source code found in the listed source
        # directories.  For example, Javac returns true if any of the source directories contains
        # a .java file.  The default implementation looks to see if there are any files in the
        # specified path with the extension #source_ext.
        def applies_to?(project, task)
          paths = task.sources + [sources].flatten.map { |src| Array(project.path_to(:source, task.usage, src.to_sym)) }
          paths.flatten!
          ext_glob = Array(source_ext).join(',')

          paths.each { |path|
            Find.find(path) {|found|
              if (!File.directory?(found)) && found.match(/.*\.#{Array(source_ext).join('|')}/)
                return true
              end
              } if File.exist? path
            }
          false
        end

        # Implementations can use this method to specify various compiler attributes.
        # For example:
        #   specify :language=>:java, :target=>'classes', :target_ext=>'class', :packaging=>:jar
        def specify(attrs)
          attrs[:sources] ||= attrs[:language].to_s
          attrs[:source_ext] ||= attrs[:language].to_s
          attrs.each { |name, value| instance_variable_set("@#{name}", value) }
        end

        # Returns additional dependencies required by this language.  For example, since the
        # test framework picks on these, you can use the JUnit framework with Scala.
        # Defaults to obtaining a list of artifact specifications from the REQUIRES constant.
        def dependencies
          []
        end

      end

      # Construct a new compiler with the specified options.  Note that options may
      # change before the compiler is run.
      def initialize(project, options)
        @project = project
        @options = options
      end

      # Options for this compiler.
      attr_reader :options

      # Determines if the compiler needs to run by checking if the target files exist,
      # and if any source files or dependencies are newer than corresponding target files.
      def needed?(sources, target, dependencies)
        map = compile_map(sources, target)
        return false if map.empty?
        return true unless File.exist?(target.to_s)
        source_files_not_yet_compiled = map.select { |source, target| !File.exist?(target) }.to_a
        trace "Compile needed because source file #{source_files_not_yet_compiled[0][0]} has no corresponding #{source_files_not_yet_compiled[0][1]}" unless source_files_not_yet_compiled.empty?
        return true if map.any? { |source, target| !File.exist?(target) || File.stat(source).mtime > File.stat(target).mtime }
        oldest = map.map { |source, target| File.stat(target).mtime }.min
        return dependencies.any? { |path| file(path).timestamp > oldest }
      end

      # Compile all files lists in sources (files and directories) into target using the
      # specified dependencies.
      def compile(sources, target, dependencies)
        raise 'Not implemented'
      end

      # Returns additional dependencies required by this language.  For example, since the
      # test framework picks on these, you can use the JUnit framework with Scala.
      def dependencies
        self.class.dependencies
      end

    protected

      # Use this to complain about CompileTask options not supported by this compiler.
      #
      # For example:
      #   def compile(files, task)
      #     check_options task, OPTIONS
      #     . . .
      #   end
      def check_options(options, *supported)
        unsupported = options.to_hash.keys - supported.flatten
        raise ArgumentError, "No such option: #{unsupported.join(' ')}" unless unsupported.empty?
      end

      # Expands a list of source directories/files into a list of files that have the #source_ext extension.
      def files_from_sources(sources)
        ext_glob = Array(self.class.source_ext).join(',')
        sources.flatten.map { |source| File.directory?(source) ? FileList["#{source}/**/*.{#{ext_glob}}"] : source }.
          flatten.reject { |file| File.directory?(file) }.map { |file| File.expand_path(file) }.uniq
      end

      # The compile map is a hash that associates source files with target files based
      # on a list of source directories and target directory.  The compile task uses this
      # to determine if there are source files to compile, and which source files to compile.
      # The default method maps all files in the source directories with #source_ext into
      # paths in the target directory with #target_ext (e.g. 'source/foo.java'=>'target/foo.class').
      def compile_map(sources, target)
        target_ext = self.class.target_ext
        ext_glob = Array(self.class.source_ext).join(',')
        sources.flatten.map{|f| File.expand_path(f)}.inject({}) do |map, source|
          if File.directory?(source)
            FileList["#{source}/**/*.{#{ext_glob}}"].reject { |file| File.directory?(file) }.
              each { |file| map[file] = File.join(target, Util.relative_path(file, source).ext(target_ext)) }
          else
            # try to extract package name from .java or .scala files
            if ['.java', '.scala', '.groovy'].include? File.extname(source)
              package = findFirst(source, /^\s*package\s+([^\s;]+)\s*;?\s*/)
              map[source] = package ? File.join(target, package[1].gsub('.', '/'), File.basename(source).ext(target_ext)) : target
            elsif
              map[source] = target
            end
          end
          map
        end
      end

    private

      def findFirst(file, pattern)
        match = nil
        File.open(file, "r") do |infile|
          while (line = infile.gets)
            match = line.match(pattern)
            break if match
          end
        end
        match
      end

    end
  end


  # Compile task.
  #
  # Attempts to determine which compiler to use based on the project layout, for example,
  # uses the Javac compiler if it finds any .java files in src/main/java.  You can also
  # select the compiler explicitly:
  #   compile.using(:scalac)
  #
  # Accepts multiple source directories that are invoked as prerequisites before compilation.
  # You can pass a task as a source directory:
  #   compile.from(apt)
  #
  # Likewise, dependencies are invoked before compiling. All dependencies are evaluated as
  # #artifacts, so you can pass artifact specifications and even projects:
  #   compile.with('module1.jar', 'log4j:log4j:jar:1.0', project('foo'))
  #
  # Creates a file task for the target directory, so executing that task as a dependency will
  # execute the compile task first.
  #
  # Compiler options are inherited form a parent task, e.g. the foo:bar:compile task inherits
  # its options from the foo:compile task. Even if foo is an empty project that does not compile
  # any classes itself, you can use it to set compile options for all its sub-projects.
  #
  # Normally, the project will take care of setting the source and target directory, and you
  # only need to set options and dependencies. See Project#compile.
  class CompileTask < Rake::Task

    def initialize(*args) #:nodoc:
      super
      parent_task = Project.parent_task(name)
      inherit = lambda { |hash, key| parent_task.options[key] } if parent_task.respond_to?(:options)
      @options = OpenObject.new &inherit
      @sources = FileList[]
      @dependencies = FileList[]

      enhance do |task|
        unless sources.empty?
          raise 'No compiler selected and can\'t determine which compiler to use' unless compiler
          raise 'No target directory specified' unless target
          mkpath target.to_s
          info "Compiling #{task.name.gsub(/:[^:]*$/, '')} into #{target.to_s}"
          @compiler.compile(sources.map(&:to_s), target.to_s, dependencies.map(&:to_s))
          # By touching the target we let other tasks know we did something,
          # and also prevent recompiling again for dependencies.
          touch target.to_s
        end
      end
    end

    # Source directories.
    attr_accessor :sources

    # :call-seq:
    #   from(*sources) => self
    #
    # Adds source directories and files to compile, and returns self.
    #
    # For example:
    #   compile.from('src/java').into('classes').with('module1.jar')
    def from(*sources)
      @sources |= sources.flatten
      guess_compiler if @compiler.nil? && sources.flatten.any? { |source| File.exist?(source.to_s) }
      self
    end

    # *Deprecated*: Use dependencies instead.
    def classpath
      Buildr.application.deprecated 'Use dependencies instead.'
      dependencies
    end

    # *Deprecated*: Use dependencies= instead.
    def classpath=(artifacts)
      Buildr.application.deprecated 'Use dependencies= instead.'
      self.dependencies = artifacts
    end

    # Compilation dependencies.
    attr_accessor :dependencies

    # :call-seq:
    #   with(*artifacts) => self
    #
    # Adds files and artifacts as dependencies, and returns self.
    #
    # Calls #artifacts on the arguments, so you can pass artifact specifications,
    # tasks, projects, etc. Use this rather than setting the dependencies array directly.
    #
    # For example:
    #   compile.with('module1.jar', 'log4j:log4j:jar:1.0', project('foo'))
    def with(*specs)
      @dependencies |= Buildr.artifacts(specs.flatten).uniq
      self
    end

    # The target directory for the compiled code.
    attr_reader :target

    # :call-seq:
    #   into(path) => self
    #
    # Sets the target directory and returns self. This will also set the compile task
    # as a prerequisite to a file task on the target directory.
    #
    # For example:
    #   compile(src_dir).into(target_dir).with(artifacts)
    # Both compile.invoke and file(target_dir).invoke will compile the source files.
    def into(path)
      @target = file(path.to_s).enhance([self]) unless @target.to_s == path.to_s
      self
    end

    # Returns the compiler options.
    attr_reader :options

    # :call-seq:
    #   using(options) => self
    #
    # Sets the compiler options from a hash and returns self.  Can also be used to
    # select the compiler.
    #
    # For example:
    #   compile.using(:warnings=>true, :source=>'1.5')
    #   compile.using(:scala)
    def using(*args)
      args.pop.each { |key, value| options.send "#{key}=", value } if Hash === args.last
      self.compiler = args.pop until args.empty?
      self
    end

    # Returns the compiler if known.  The compiler is either automatically selected
    # based on existing source directories (e.g. src/main/java), or by requesting
    # a specific compiler (see #using).
    def compiler
      guess_compiler unless @compiler
      @compiler && @compiler.class.to_sym
    end

    # Returns the compiled language, if known.  See also #compiler.
    def language
      compiler && @compiler.class.language
    end

    # Returns the default packaging type for this compiler, if known.
    def packaging
      compiler && @compiler.class.packaging
    end

    def timestamp #:nodoc:
      # If we compiled successfully, then the target directory reflects that.
      # If we didn't, see needed?
      target ? target.timestamp : Rake::EARLY
    end

    # The project this task belongs to.
    attr_reader :project

    # The usage, one of :main or :test.
    attr_reader :usage

  protected

    # Selects which compiler to use.
    def compiler=(name) #:nodoc:
      cls = Compiler.select(name) or raise ArgumentError, "No #{name} compiler available. Did you install it?"
      return self if cls === @compiler
      @compiler = cls.new(project, options)
      from Array(cls.sources).map { |path| project.path_to(:source, usage, path) }.
        select { |path| File.exist?(path) } if sources.empty?
      into project.path_to(:target, usage, cls.target) unless target
      with Array(@compiler.dependencies)
      self
    end

    # Associates this task with project and particular usage (:main, :test).
    def associate_with(project, usage) #:nodoc:
      @project, @usage = project, usage
      guess_compiler
    end

    # Try to guess if we have a compiler to match source files.
    def guess_compiler #:nodoc:
      candidate = Compiler.compilers.detect { |cls| cls.applies_to?(project, self) }
      self.compiler = candidate if candidate
    end

  private

    def needed? #:nodoc:
      return false if sources.empty?
      # Fail during invoke.
      return true unless @compiler && target
      return @compiler.needed?(sources.map(&:to_s), target.to_s, dependencies.map(&:to_s))
    end

    def invoke_prerequisites(args, chain) #:nodoc:
      @sources = Array(@sources).map(&:to_s).uniq
      @dependencies = FileList[@dependencies.uniq]
      @prerequisites |= @dependencies + @sources
      super
    end

  end


  # The resources task is executed by the compile task to copy resource files over
  # to the target directory. You can enhance this task in the normal way, but mostly
  # you will use the task's filter.
  #
  # For example:
  #   resources.filter.using 'Copyright'=>'Acme Inc, 2007'
  class ResourcesTask < Rake::Task

    # Returns the filter used to copy resources over. See Buildr::Filter.
    attr_reader :filter

    def initialize(*args) #:nodoc:
      super
      @filter = Buildr::Filter.new
      @filter.using Buildr.settings.profile['filter'] if Hash === Buildr.settings.profile['filter']
      enhance do
        target.invoke if target
      end
    end

    # :call-seq:
    #   include(*files) => self
    #
    # Includes the specified files in the filter and returns self.
    def include(*files)
      filter.include *files
      self
    end

    # :call-seq:
    #   exclude(*files) => self
    #
    # Excludes the specified files in the filter and returns self.
    def exclude(*files)
      filter.exclude *files
      self
    end

    # :call-seq:
    #   from(*sources) => self
    #
    # Adds additional directories from which to copy resources.
    #
    # For example:
    #   resources.from _('src/etc')
    def from(*sources)
      filter.from *sources
      self
    end

    # Returns the list of source directories (each being a file task).
    def sources
      filter.sources
    end

    # :call-seq:
    #   target => task
    #
    # Returns the filter's target directory as a file task.
    def target
      filter.into @project.path_to(:target, @usage, :resources) unless filter.target || sources.empty?
      filter.target
    end

    def prerequisites #:nodoc:
      super + filter.sources.flatten
    end

  protected

    # Associates this task with project and particular usage (:main, :test).
    def associate_with(project, usage) #:nodoc:
      @project, @usage = project, usage
    end

  end


  # Methods added to Project for compiling, handling of resources and generating source documentation.
  module Compile

    include Extension

    first_time do
      desc 'Compile all projects'
      Project.local_task('compile') { |name| "Compiling #{name}" }
    end

    before_define(:compile) do |project|
      resources = ResourcesTask.define_task('resources')
      resources.send :associate_with, project, :main
      project.path_to(:source, :main, :resources).tap { |dir| resources.from dir if File.exist?(dir) }

      compile = CompileTask.define_task('compile'=>resources)
      compile.send :associate_with, project, :main
      project.recursive_task('compile')
    end

    after_define(:compile) do |project|
      if project.compile.target
        # This comes last because the target path is set inside the project definition.
        project.build project.compile.target
        project.clean do
          rm_rf project.compile.target.to_s, :verbose=>false
        end
      end
    end


    # :call-seq:
    #   compile(*sources) => CompileTask
    #   compile(*sources) { |task| .. } => CompileTask
    #
    # The compile task does what its name suggests. This method returns the project's
    # CompileTask. It also accepts a list of source directories and files to compile
    # (equivalent to calling CompileTask#from on the task), and a block for any
    # post-compilation work.
    #
    # The compile task attempts to guess which compiler to use.  For example, if it finds
    # any Java files in the src/main/java directory, it will use the Java compiler and
    # create class files in the target/classes directory.
    #
    # You can also configure it yourself by telling it which compiler to use, pointing
    # it as source directories and chooing a different target directory.
    #
    # For example:
    #   # Include Log4J and the api sub-project artifacts.
    #   compile.with 'log4j:log4j:jar:1.2', project('api')
    #   # Include Apt-generated source files.
    #   compile.from apt
    #   # For JavaC, force target compatibility.
    #   compile.options.source = '1.6'
    #   # Run the OpenJPA bytecode enhancer after compilation.
    #   compile { open_jpa_enhance }
    #   # Pick a given compiler.
    #   compile.using(:scalac).from('src/scala')
    #
    # For more information, see CompileTask.
    def compile(*sources, &block)
      task('compile').from(sources).enhance &block
    end

    # :call-seq:
    #   resources(*prereqs) => ResourcesTask
    #   resources(*prereqs) { |task| .. } => ResourcesTask
    #
    # The resources task is executed by the compile task to copy resources files
    # from the resource directory into the target directory. By default the resources
    # task copies files from the src/main/resources into the target/resources directory.
    #
    # This method returns the project's resources task. It also accepts a list of
    # prerequisites and a block, used to enhance the resources task.
    #
    # Resources files are copied and filtered (see Buildr::Filter for more information).
    # The default filter uses the profile properties for the current environment.
    #
    # For example:
    #   resources.from _('src/etc')
    #   resources.filter.using 'Copyright'=>'Acme Inc, 2007'
    #
    # Or in your profiles.yaml file:
    #   common:
    #     Copyright: Acme Inc, 2007
    def resources(*prereqs, &block)
      task('resources').enhance prereqs, &block
    end

  end


  class Options

    # Returns the debug option (environment variable DEBUG).
    def debug
      (ENV['DEBUG'] || ENV['debug']) !~ /(no|off|false)/
    end

    # Sets the debug option (environment variable DEBUG).
    #
    # You can turn this option off directly, or by setting the environment variable
    # DEBUG to +no+. For example:
    #   buildr build DEBUG=no
    #
    # The release tasks runs a build with <tt>DEBUG=no</tt>.
    def debug=(flag)
      ENV['debug'] = nil
      ENV['DEBUG'] = flag.to_s
    end

  end

end


class Buildr::Project
  include Buildr::Compile
end
