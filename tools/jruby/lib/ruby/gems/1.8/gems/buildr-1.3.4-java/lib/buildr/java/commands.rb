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


# Base module for all things Java.
module Java

  # JDK commands: java, javac, javadoc, etc.
  module Commands

    class << self

      # :call-seq:
      #   java(class, *args, options?)
      #
      # Runs Java with the specified arguments.
      #
      # The last argument may be a Hash with additional options:
      # * :classpath -- One or more file names, tasks or artifact specifications.
      #   These are all expanded into artifacts, and all tasks are invoked.
      # * :java_args -- Any additional arguments to pass (e.g. -hotspot, -xms)
      # * :properties -- Hash of system properties (e.g. 'path'=>base_dir).
      # * :name -- Shows this name, otherwise shows the first argument (the class name).
      # * :verbose -- If true, prints the command and all its argument.
      def java(*args, &block)
        options = Hash === args.last ? args.pop : {}
        options[:verbose] ||= Buildr.application.options.trace || false
        rake_check_options options, :classpath, :java_args, :properties, :name, :verbose

        name = options[:name] || "java #{args.first}"
        cmd_args = [path_to_bin('java')]
        classpath = classpath_from(options)
        cmd_args << '-classpath' << classpath.join(File::PATH_SEPARATOR) unless classpath.empty?
        options[:properties].each { |k, v| cmd_args << "-D#{k}=#{v}" } if options[:properties]
        cmd_args += (options[:java_args] || (ENV['JAVA_OPTS'] || ENV['JAVA_OPTIONS']).to_s.split).flatten
        cmd_args += args.flatten.compact
        unless Buildr.application.options.dryrun
          info "Running #{name}"
          block = lambda { |ok, res| fail "Failed to execute #{name}, see errors above" unless ok } unless block
          puts cmd_args.join(' ') if Buildr.application.options.trace
          cmd_args = cmd_args.map(&:inspect).join(' ') if Util.win_os?
          system(*cmd_args).tap do |ok|
            block.call ok, $?
          end
        end
      end
  
      # :call-seq:
      #   apt(*files, options)
      #
      # Runs Apt with the specified arguments.
      #
      # The last argument may be a Hash with additional options:
      # * :compile -- If true, compile source files to class files.
      # * :source -- Specifies source compatibility with a given JVM release.
      # * :output -- Directory where to place the generated source files, or the
      #   generated class files when compiling.
      # * :classpath -- One or more file names, tasks or artifact specifications.
      #   These are all expanded into artifacts, and all tasks are invoked.
      def apt(*args)
        options = Hash === args.last ? args.pop : {}
        rake_check_options options, :compile, :source, :output, :classpath

        files = args.flatten.map(&:to_s).
          collect { |arg| File.directory?(arg) ? FileList["#{arg}/**/*.java"] : arg }.flatten
        cmd_args = [ Buildr.application.options.trace ? '-verbose' : '-nowarn' ]
        if options[:compile]
          cmd_args << '-d' << options[:output].to_s
        else
          cmd_args << '-nocompile' << '-s' << options[:output].to_s
        end
        cmd_args << '-source' << options[:source] if options[:source]
        classpath = classpath_from(options)
        tools = Java.tools_jar
        classpath << tools if tools
        cmd_args << '-classpath' << classpath.join(File::PATH_SEPARATOR) unless classpath.empty?
        cmd_args += files
        unless Buildr.application.options.dryrun
          info 'Running apt'
          trace (['apt'] + cmd_args).join(' ')
          Java.load
          Java.com.sun.tools.apt.Main.process(cmd_args.to_java(Java.java.lang.String)) == 0 or
            fail 'Failed to process annotations, see errors above'
        end
      end

      # :call-seq:
      #   javac(*files, options)
      #
      # Runs Javac with the specified arguments.
      #
      # The last argument may be a Hash with additional options:
      # * :output -- Target directory for all compiled class files.
      # * :classpath -- One or more file names, tasks or artifact specifications.
      #   These are all expanded into artifacts, and all tasks are invoked.
      # * :sourcepath -- Additional source paths to use.
      # * :javac_args -- Any additional arguments to pass (e.g. -extdirs, -encoding)
      # * :name -- Shows this name, otherwise shows the working directory.
      def javac(*args)
        options = Hash === args.last ? args.pop : {}
        rake_check_options options, :classpath, :sourcepath, :output, :javac_args, :name

        files = args.flatten.each { |f| f.invoke if f.respond_to?(:invoke) }.map(&:to_s).
          collect { |arg| File.directory?(arg) ? FileList["#{arg}/**/*.java"] : arg }.flatten
        name = options[:name] || Dir.pwd

        cmd_args = []
        classpath = classpath_from(options)
        cmd_args << '-classpath' << classpath.join(File::PATH_SEPARATOR) unless classpath.empty?
        cmd_args << '-sourcepath' << options[:sourcepath].join(File::PATH_SEPARATOR) if options[:sourcepath]
        cmd_args << '-d' << options[:output].to_s if options[:output]
        cmd_args += options[:javac_args].flatten if options[:javac_args]
        cmd_args += files
        unless Buildr.application.options.dryrun
          info "Compiling #{files.size} source files in #{name}"
          trace (['javac'] + cmd_args).join(' ')
          Java.load
          Java.com.sun.tools.javac.Main.compile(cmd_args.to_java(Java.java.lang.String)) == 0 or 
            fail 'Failed to compile, see errors above'
        end
      end

      # :call-seq:
      #   javadoc(*files, options)
      #
      # Runs Javadocs with the specified files and options.
      #
      # This method accepts the following special options:
      # * :output -- The output directory
      # * :classpath -- Array of classpath dependencies.
      # * :sourcepath -- Array of sourcepaths (paths or tasks).
      # * :name -- Shows this name, otherwise shows the working directory.
      #
      # All other options are passed to Javadoc as following:
      # * true -- As is, for example, :author=>true becomes -author
      # * false -- Prefixed, for example, :index=>false becomes -noindex
      # * string -- Option with value, for example, :windowtitle=>'My project' becomes -windowtitle "My project"
      # * array -- Option with set of values separated by spaces.
      def javadoc(*args)
        options = Hash === args.last ? args.pop : {}

        cmd_args = [ '-d', options[:output], Buildr.application.options.trace ? '-verbose' : '-quiet' ]
        options.reject { |key, value| [:output, :name, :sourcepath, :classpath].include?(key) }.
          each { |key, value| value.invoke if value.respond_to?(:invoke) }.
          each do |key, value|
            case value
            when true, nil
              cmd_args << "-#{key}"
            when false
              cmd_args << "-no#{key}"
            when Hash
              value.each { |k,v| cmd_args << "-#{key}" << k.to_s << v.to_s }
            else
              cmd_args += Array(value).map { |item| ["-#{key}", item.to_s] }.flatten
            end
          end
        [:sourcepath, :classpath].each do |option|
          options[option].to_a.flatten.tap do |paths|
            cmd_args << "-#{option}" << paths.flatten.map(&:to_s).join(File::PATH_SEPARATOR) unless paths.empty?
          end
        end
        cmd_args += args.flatten.uniq
        name = options[:name] || Dir.pwd
        unless Buildr.application.options.dryrun
          info "Generating Javadoc for #{name}"
          trace (['javadoc'] + cmd_args).join(' ')
          Java.load
          Java.com.sun.tools.javadoc.Main.execute(cmd_args.to_java(Java.java.lang.String)) == 0 or
            fail 'Failed to generate Javadocs, see errors above'
        end
      end

    protected

      # :call-seq:
      #   path_to_bin(cmd?) => path
      #
      # Returns the path to the specified Java command (with no argument to java itself).
      def path_to_bin(name = nil)
        home = ENV['JAVA_HOME'] or fail 'Are we forgetting something? JAVA_HOME not set.'
        File.expand_path(File.join(home, 'bin', name.to_s))
      end

      # :call-seq:
      #    classpath_from(options) => files
      #
      # Extracts the classpath from the options, expands it by calling artifacts, invokes
      # each of the artifacts and returns an array of paths.
      def classpath_from(options)
        Buildr.artifacts(options[:classpath] || []).map(&:to_s).
          map { |t| task(t).invoke; File.expand_path(t) }
      end

    end

  end

end

