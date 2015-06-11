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

# The Scala Module
module Buildr::Scala
  DEFAULT_VERSION = '2.9.2'

  class << self

    def version_str
      warn "Use of Scala.version_str is deprecated.  Use Scala.version instead"
      version
    end

    def installed_version
      unless @installed_version
        @installed_version = if Scalac.installed?
          begin
            # try to read the value from the properties file
            props = Zip::ZipFile.open(File.expand_path('lib/scala-library.jar', Scalac.scala_home)) do |zipfile|
              zipfile.read 'library.properties'
            end

            version_str = props.match(/version\.number\s*=\s*([^\s]+)/).to_a[1]

            if version_str
              md = version_str.match(/\d+\.\d[\d\.]*/) or
                fail "Unable to parse Scala version: #{version_str}"

              md[0].sub(/.$/, "") # remove trailing dot, if any
            end
          rescue => e
            warn "Unable to parse library.properties in $SCALA_HOME/lib/scala-library.jar: #{e}"
            nil
          end
        end
      end

      @installed_version
    end

    def version
      Buildr.settings.build['scala.version'] || installed_version || DEFAULT_VERSION
    end

    # check if version matches any of the given prefixes
    def version?(*v)
      v.any? { |v| version.index(v.to_s) == 0 }
    end

    # returns Scala version without build number.
    # e.g.  "2.9.0-1" => "2.9.0"
    def version_without_build
      version.split('-')[0]
    end
  end

  # Scalac compiler:
  #   compile.using(:scalac)
  # Used by default if .scala files are found in the src/main/scala directory (or src/test/scala)
  # and sets the target directory to target/classes (or target/test/classes).
  #
  # Accepts the following options:
  # * :warnings    -- Generate warnings if true (opposite of -nowarn).
  # * :deprecation -- Output source locations where deprecated APIs are used.
  # * :optimise    -- Generates faster bytecode by applying optimisations to the program.
  # * :target      -- Class file compatibility with specified release.
  # * :debug       -- Generate debugging info.
  # * :other       -- Array of options to pass to the Scalac compiler as is, e.g. -Xprint-types
  class Scalac < Buildr::Compiler::Base

    DEFAULT_ZINC_VERSION  = '0.1.0'
    DEFAULT_SBT_VERSION   = '0.12.0'
    DEFAULT_JLINE_VERSION = '1.0'

    class << self
      def scala_home
        env_home = ENV['SCALA_HOME']

        @home ||= (if !env_home.nil? && File.exists?(env_home + '/lib/scala-library.jar') && File.exists?(env_home + '/lib/scala-compiler.jar')
          env_home
        else
          nil
        end)
      end

      def installed?
        !scala_home.nil?
      end

      def use_installed?
        if installed? && Buildr.settings.build['scala.version']
          Buildr.settings.build['scala.version'] == Scala.installed_version
        else
          Buildr.settings.build['scala.version'].nil? && installed?
        end
      end

      def dependencies
        scala_dependencies = if use_installed?
          ['scala-library', 'scala-compiler'].map { |s| File.expand_path("lib/#{s}.jar", scala_home) }
        else
          REQUIRES.artifacts.map(&:to_s)
        end

        zinc_dependencies = ZINC_REQUIRES.artifacts.map(&:to_s)

        (scala_dependencies + zinc_dependencies).compact
      end

      def use_fsc
        use_installed? && ENV["USE_FSC"] =~ /^(yes|on|true)$/i
      end

      def applies_to?(project, task) #:nodoc:
        paths = task.sources + [sources].flatten.map { |src| Array(project.path_to(:source, task.usage, src.to_sym)) }
        paths.flatten!

        # Just select if we find .scala files
        paths.any? { |path| !Dir["#{path}/**/*.scala"].empty? }
      end
    end

    # The scalac compiler jars are added to classpath at load time,
    # if you want to customize artifact versions, you must set them on the
    #
    #      artifact_ns['Buildr::Compiler::Scalac'].library = '2.7.5'
    #
    # namespace before this file is required.  This is of course, only
    # if SCALA_HOME is not set or invalid.
    REQUIRES = ArtifactNamespace.for(self) do |ns|
      version = Buildr.settings.build['scala.version'] || DEFAULT_VERSION
      ns.library!      'org.scala-lang:scala-library:jar:>=' + version
      ns.compiler!     'org.scala-lang:scala-compiler:jar:>=' + version
      unless Buildr::Scala.version?(2.7, 2.8, 2.9)
        # added in Scala 2.10
        ns.reflect!      'org.scala-lang:scala-reflect:jar:>=' + version
        ns.actors!       'org.scala-lang:scala-actors:jar:>=' + version
      end
    end

    ZINC_REQUIRES = ArtifactNamespace.for(self) do |ns|
      zinc_version  = Buildr.settings.build['zinc.version']  || DEFAULT_ZINC_VERSION
      sbt_version   = Buildr.settings.build['sbt.version']   || DEFAULT_SBT_VERSION
      jline_version = Buildr.settings.build['jline.version'] || DEFAULT_JLINE_VERSION
      ns.zinc!          "com.typesafe.zinc:zinc:jar:>=#{zinc_version}"
      ns.sbt_interface! "com.typesafe.sbt:sbt-interface:jar:>=#{sbt_version}"
      ns.incremental!   "com.typesafe.sbt:incremental-compiler:jar:>=#{sbt_version}"
      ns.compiler_interface_sources! "com.typesafe.sbt:compiler-interface:jar:sources:>=#{sbt_version}"
      ns.jline!        "jline:jline:jar:>=#{jline_version}"
    end

    Javac = Buildr::Compiler::Javac

    OPTIONS = [:warnings, :deprecation, :optimise, :target, :debug, :other, :javac]

    # Lazy evaluation to allow change in buildfile
    Java.classpath << lambda { dependencies }

    specify :language=>:scala, :sources => [:scala, :java], :source_ext => [:scala, :java],
            :target=>'classes', :target_ext=>'class', :packaging=>:jar

    def initialize(project, options) #:nodoc:
      super
      options[:debug] = Buildr.options.debug if options[:debug].nil?
      options[:warnings] = verbose if options[:warnings].nil?
      options[:deprecation] ||= false
      options[:optimise] ||= false
      options[:make] ||= :transitivenocp if Scala.version? 2.8
      options[:javac] ||= {}
      @java = Javac.new(project, options[:javac])
    end

    def compile(sources, target, dependencies) #:nodoc:
      if zinc?
        compile_with_zinc(sources, target, dependencies)
      else
        compile_with_scalac(sources, target, dependencies)
      end
    end

    def compile_with_scalac(sources, target, dependencies) #:nodoc:
      check_options(options, OPTIONS + (Scala.version?(2.8) ? [:make] : []))

      java_sources = java_sources(sources)
      enable_dep_tracing = Scala.version?(2.8) && java_sources.empty?

      dependencies.unshift target if enable_dep_tracing

      cmd_args = []
      cmd_args << '-classpath' << dependencies.join(File::PATH_SEPARATOR)
      source_paths = sources.select { |source| File.directory?(source) }
      cmd_args << '-sourcepath' << source_paths.join(File::PATH_SEPARATOR) unless source_paths.empty?
      cmd_args << '-d' << File.expand_path(target)
      cmd_args += scalac_args

      if enable_dep_tracing
        dep_dir = File.expand_path(target)
        Dir.mkdir dep_dir unless File.exists? dep_dir

        cmd_args << '-make:' + options[:make].to_s
        cmd_args << '-dependencyfile'
        cmd_args << File.expand_path('.scala-deps', dep_dir)
      end

      cmd_args += files_from_sources(sources)

      unless Buildr.application.options.dryrun
        trace((['scalac'] + cmd_args).join(' '))

        if Scalac.use_fsc
          system(([File.expand_path('bin/fsc', Scalac.scala_home)] + cmd_args).join(' ')) or
            fail 'Failed to compile, see errors above'
        else
          Java.load
          begin
            Java.scala.tools.nsc.Main.process(cmd_args.to_java(Java.java.lang.String))
          rescue => e
            fail "Scala compiler crashed:\n#{e.inspect}"
          end
          fail 'Failed to compile, see errors above' if Java.scala.tools.nsc.Main.reporter.hasErrors
        end

        unless java_sources.empty?
          trace 'Compiling mixed Java/Scala sources'

          # TODO  includes scala-compiler.jar
          deps = dependencies + Scalac.dependencies + [ File.expand_path(target) ]
          @java.compile(java_sources, target, deps)
        end
      end
    end

    def compile_with_zinc(sources, target, dependencies) #:nodoc:

      dependencies.unshift target

      cmd_args = []
      cmd_args << '-sbt-interface' << REQUIRES.sbt_interface.artifact
      cmd_args << '-compiler-interface' << REQUIRES.compiler_interface_sources.artifact
      cmd_args << '-scala-library' << dependencies.find { |d| d =~ /scala-library/ }
      cmd_args << '-scala-compiler' << dependencies.find { |d| d =~ /scala-compiler/ }
      cmd_args << '-classpath' << dependencies.join(File::PATH_SEPARATOR)
      source_paths = sources.select { |source| File.directory?(source) }
      cmd_args << '-Ssourcepath' << ("-S" + source_paths.join(File::PATH_SEPARATOR)) unless source_paths.empty?
      cmd_args << '-d' << File.expand_path(target)
      cmd_args += scalac_args
      cmd_args << "-debug" if trace?(:scalac)

      cmd_args.map!(&:to_s)

      cmd_args += files_from_sources(sources)

      unless Buildr.application.options.dryrun
        trace((['com.typesafe.zinc.Main.main'] + cmd_args).join(' '))

        begin
          Java::Commands.java 'com.typesafe.zinc.Main', *(cmd_args + [{ :classpath => Scalac.dependencies}])
        rescue => e
          fail "Zinc compiler crashed:\n#{e.inspect}\n#{e.backtrace.join("\n")}"
        end
      end
    end

  protected

    # :nodoc: see Compiler:Base
    def compile_map(sources, target)
      target_ext = self.class.target_ext
      ext_glob = Array(self.class.source_ext).join(',')
      sources.flatten.map{|f| File.expand_path(f)}.inject({}) do |map, source|
        sources = if File.directory?(source)
          FileList["#{source}/**/*.{#{ext_glob}}"].reject { |file| File.directory?(file) }
        else
          [source]
        end

        sources.each do |source|
          # try to extract package name from .java or .scala files
          if ['.java', '.scala'].include? File.extname(source)
            name = File.basename(source).split(".")[0]
            package = findFirst(source, /^\s*package\s+([^\s;]+)\s*;?\s*/)
            packages = count(source, /^\s*package\s+([^\s;]+)\s*;?\s*/)
            found = findFirst(source, /((trait)|(class)|(object))\s+(#{name})/)

            # if there's only one package statement and we know the target name, then we can depend
            # directly on a specific file, otherwise, we depend on the general target
            if (found && packages == 1)
              map[source] = package ? File.join(target, package[1].gsub('.', '/'), name.ext(target_ext)) : target
            else
              map[source] = target
            end

          elsif
            map[source] = target
          end
        end

        map.each do |key,value|
          map[key] = first_file unless map[key]
        end

        map
      end
    end

  private

    def zinc?
      (options[:incremental] || @project.scalac_options.incremental || (Buildr.settings.build['scalac.incremental'].to_s == "true"))
    end

    def count(file, pattern)
      count = 0
      File.open(file, "r") do |infile|
        while (line = infile.gets)
          count += 1 if line.match(pattern)
        end
      end
      count
    end

    def java_sources(sources)
      sources.flatten.map { |source| File.directory?(source) ? FileList["#{source}/**/*.java"] : source } .
        flatten.reject { |file| File.directory?(file) || File.extname(file) != '.java' }.map { |file| File.expand_path(file) }.uniq
    end

    # Returns Scalac command line arguments from the set of options.
    def scalac_args #:nodoc:
      args = []
      args << "-nowarn" unless options[:warnings]
      args << "-verbose" if trace?(:scalac)
      if options[:debug] == true
        args << (Scala.version?(2.7, 2.8) ? "-g" : "-g:vars")
      elsif options[:debug]
        args << "-g:#{options[:debug]}"
      end
      args << "-deprecation" if options[:deprecation]
      args << "-optimise" if options[:optimise]
      args << "-target:jvm-" + options[:target].to_s if options[:target]
      args += Array(options[:other])
      if zinc?
        args.map { |arg| "-S" + arg } + Array(options[:zinc_options])
      else
        args
      end
    end
  end

  module ProjectExtension
    def scalac_options
      @scalac ||= ScalacOptions.new(self)
    end
  end

  class ScalacOptions
    attr_writer :incremental

    def initialize(project)
      @project = project
    end

    def incremental
      @incremental || (@project.parent ? @project.parent.scalac_options.incremental : nil)
    end
  end
end

# Scala compiler comes first, ahead of Javac, this allows it to pick
# projects that mix Scala and Java code by spotting Scala code first.
Buildr::Compiler.compilers.unshift Buildr::Scala::Scalac

class Buildr::Project #:nodoc:
  include Buildr::Scala::ProjectExtension
end
