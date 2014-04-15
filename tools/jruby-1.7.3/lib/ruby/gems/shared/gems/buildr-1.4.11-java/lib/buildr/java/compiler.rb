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
  module Compiler

    # Javac compiler:
    #   compile.using(:javac)
    # Used by default if .java files are found in the src/main/java directory (or src/test/java)
    # and sets the target directory to target/classes (or target/test/classes).
    #
    # Accepts the following options:
    # * :warnings    -- Issue warnings when compiling.  True when running in verbose mode.
    # * :debug       -- Generates bytecode with debugging information.  Set from the debug
    # environment variable/global option.
    # * :deprecation -- If true, shows deprecation messages.  False by default.
    # * :source      -- Source code compatibility.
    # * :target      -- Bytecode compatibility.
    # * :lint        -- Lint option is one of true, false (default), name (e.g. 'cast') or array.
    # * :other       -- Array of options passed to the compiler
    # (e.g. ['-implicit:none', '-encoding', 'iso-8859-1'])
    class Javac < Base

      OPTIONS = [:warnings, :debug, :deprecation, :source, :target, :lint, :other]

      specify :language=>:java, :target=>'classes', :target_ext=>'class', :packaging=>:jar

      def initialize(project, options) #:nodoc:
        super
        options[:debug] = Buildr.options.debug if options[:debug].nil?
        options[:warnings] ||= false
        options[:deprecation] ||= false
        options[:lint] ||= false
      end

      def compile(sources, target, dependencies) #:nodoc:
        check_options options, OPTIONS
        cmd_args = []
        # tools.jar contains the Java compiler.
        dependencies << Java.tools_jar if Java.tools_jar
        cmd_args << '-classpath' << dependencies.join(File::PATH_SEPARATOR) unless dependencies.empty?
        source_paths = sources.select { |source| File.directory?(source) }
        cmd_args << '-sourcepath' << source_paths.join(File::PATH_SEPARATOR) unless source_paths.empty?
        cmd_args << '-d' << File.expand_path(target)
        cmd_args += javac_args
        cmd_args += files_from_sources(sources)
        unless Buildr.application.options.dryrun
          trace((['javac'] + cmd_args).join(' '))
          Java.load
          Java.com.sun.tools.javac.Main.compile(cmd_args.to_java(Java.java.lang.String)) == 0 or
            fail 'Failed to compile, see errors above'
        end
      end

    private

      def javac_args #:nodoc:
        args = []
        args << '-nowarn' unless options[:warnings]
        args << '-verbose' if trace? :javac
        args << '-g' if options[:debug]
        args << '-deprecation' if options[:deprecation]
        args << '-source' << options[:source].to_s if options[:source]
        args << '-target' << options[:target].to_s if options[:target]
        case options[:lint]
          when Array  then args << "-Xlint:#{options[:lint].join(',')}"
          when String then args << "-Xlint:#{options[:lint]}"
          when true   then args << '-Xlint'
        end
        args + Array(options[:other])
      end

    end

  end


  # Methods added to Project to support the Java Annotation Processor.
  module Apt

    # :call-seq:
    #   apt(*sources) => task
    #
    # Returns a task that will use Java#apt to generate source files in target/generated/apt,
    # from all the source directories passed as arguments. Uses the compile.sources list if
    # on arguments supplied.
    #
    # For example:
    #
    def apt(*sources)
      sources = compile.sources if sources.empty?
      file(path_to(:target, 'generated/apt')=>sources) do |task|
        cmd_args = [ trace?(:apt) ? '-verbose' : '-nowarn' ]
        cmd_args << '-nocompile' << '-s' << task.name
        cmd_args << '-source' << compile.options.source if compile.options.source
        classpath = Buildr.artifacts(compile.dependencies).map(&:to_s).each { |t| task(t).invoke }
        cmd_args << '-classpath' << classpath.join(File::PATH_SEPARATOR) unless classpath.empty?
        cmd_args += (sources.map(&:to_s) - [task.name]).
          map { |file| File.directory?(file) ? FileList["#{file}/**/*.java"] : file }.flatten
        unless Buildr.application.options.dryrun
          info 'Running apt'
          trace (['apt'] + cmd_args).join(' ')
          Java.com.sun.tools.apt.Main.process(cmd_args.to_java(Java.java.lang.String)) == 0 or
            fail 'Failed to process annotations, see errors above'
        end
      end
    end

  end

end

Buildr::Compiler << Buildr::Compiler::Javac
class Buildr::Project
  include Buildr::Apt
end
