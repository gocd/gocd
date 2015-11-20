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

module Buildr #:nodoc:
  module Compiler #:nodoc:
    class ExternalJavac< Buildr::Compiler::Javac

      OPTIONS = [:jvm, :warnings, :debug, :deprecation, :source, :target, :lint, :other]

      specify :language=>:java, :sources => 'java', :source_ext => 'java',
              :target=>'classes', :target_ext=>'class', :packaging=>:jar


      def compile(sources, target, dependencies) #:nodoc:
        check_options options, OPTIONS
        cmd_args = []
        # tools.jar contains the Java compiler.
        dependencies << Java.tools_jar if Java.tools_jar
        cmd_args << '-classpath' << dependencies.join(File::PATH_SEPARATOR) unless dependencies.empty?
        source_paths = sources.select { |source| File.directory?(source) }
        cmd_args << '-sourcepath' << source_paths.join(File::PATH_SEPARATOR) unless source_paths.empty?
        cmd_args << '-d' << File.expand_path(target)
        cmd_args += externaljavac_args
        Tempfile.open("external") {|tmp|
          tmp.write files_from_sources(sources).join(' ')
          cmd_args << "@#{tmp.path}"
        }
        unless Buildr.application.options.dryrun
          javac_path = File.join(options[:jvm] || ENV['JAVA_HOME'], "bin", "javac")
          final_args = cmd_args.insert(0, javac_path).push('2>&1').join(' ')
          trace(final_args)
          info %x[#{final_args}]
          fail 'Failed to compile, see errors above' unless $?.success?
        end
      end

      private

      # See arg list here: http://publib.boulder.ibm.com/infocenter/rsahelp/v7r0m0/index.jsp?topic=/org.eclipse.jdt.doc.isv/guide/jdt_api_compile.htm
      def externaljavac_args #:nodoc:
        args = []
        args << '-nowarn' unless options[:warnings]
        args << '-verbose' if trace?(:javac)
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
end

Buildr::Compiler.compilers << Buildr::Compiler::ExternalJavac
