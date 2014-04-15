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

  # Provides the <code>emma:html</code> and <code>emma:xml</code> tasks.
  # Require explicitly using <code>require "buildr/emma"</code>.
  #
  # You can generate emma reports for a single project
  # using the project name as prefix:
  #
  #   project_name:emma:html
  #
  # You can also specify which classes to include/exclude from instrumentation by
  # passing a class name regexp to the <code>emma.include</code> or
  # <code>emma.exclude</code> methods.
  #
  #   define 'someModule' do
  #      emma.include 'some.package.*'
  #      emma.exclude 'some.foo.util.SimpleUtil'
  #   end
  module Emma

    VERSION = '2.0.5312'

    class << self

      def version
        Buildr.settings.build['emma'] || VERSION
      end

      def dependencies
        @dependencies ||= ["emma:emma_ant:jar:#{version}", "emma:emma:jar:#{version}"]
      end

      def report_to format=nil
        File.expand_path('reports/emma')
      end

      def data_file()
        File.join(report_to, 'coverage.es')
      end

      def ant

        Buildr.ant 'emma' do |ant|
          ant.taskdef :resource=>'emma_ant.properties',
            :classpath=>Buildr.artifacts(dependencies).each(&:invoke).map(&:to_s).join(File::PATH_SEPARATOR)
          ant.emma :verbosity=>(trace?(:emma) ? 'verbose' : 'warning') do
            yield ant
          end
        end
      end
    end

    class EmmaConfig # :nodoc:

      def initialize(project)
        @project = project
      end

      attr_reader :project
      private :project

      attr_writer :metadata_file, :coverage_file, :instrumented_dir, :report_dir

      def coverage_file
        @coverage_file ||= File.join(report_dir, 'coverage.ec')
      end

      def metadata_file
        @metadata_file ||= File.join(report_dir, 'coverage.em')
      end

      def instrumented_dir
        @instrumented_dir ||= project.path_to(:target, :instrumented, :classes)
      end

      def report_dir
        @report_dir ||= project.path_to(:reports, :emma)
      end

      def report_to format
        report_dir
      end

      # :call-seq:
      #   project.emma.include(*classPatterns)
      #
      def include(*classPatterns)
        includes.push(*classPatterns)
        self
      end

      def includes
        @includeClasses ||= []
      end

      # :call-seq:
      #   project.emma.exclude(*classPatterns)
      #
      def exclude(*classPatterns)
        excludes.push(*classPatterns)
        self
      end

      def excludes
        @excludeClasses ||= []
      end

      def sources
        project.compile.sources
      end
    end

    module EmmaExtension # :nodoc:
      include Buildr::Extension

      def emma
        @emma_config ||= EmmaConfig.new(self)
      end

      after_define do |project|
        emma = project.emma

        namespace 'emma' do
          unless project.compile.target.nil?
            # Instrumented bytecode goes in a different directory. This task creates before running the test
            # cases and monitors for changes in the generate bytecode.
            instrumented = project.file(emma.instrumented_dir => project.compile.target) do |task|
              unless project.compile.sources.empty?
                info "Instrumenting classes with emma metadata file #{emma.metadata_file}"
                Emma.ant do |ant|
                  ant.instr :instrpath=>project.compile.target.to_s, :destdir=>task.to_s, :metadatafile=>emma.metadata_file do
                    ant.filter :includes=>emma.includes.join(', ') unless emma.includes.empty?
                    ant.filter :excludes=>emma.excludes.join(', ') unless emma.excludes.empty?
                  end
                end
                touch task.to_s
              end
            end

            task 'instrument' => instrumented

            # We now have two target directories with bytecode.
            project.test.dependencies.unshift emma.instrumented_dir
            project.test.with Emma.dependencies
            project.test.options[:properties]["emma.coverage.out.file"] = emma.coverage_file

            [:xml, :html].each do |format|
              task format => ['instrument', 'test'] do
                missing_required_files = [emma.metadata_file, emma.coverage_file].reject { |f| File.exist?(f) }
                if missing_required_files.empty?
                  info "Creating test coverage reports in #{emma.report_dir}"
                  mkdir_p emma.report_dir
                  Emma.ant do |ant|
                    ant.report do
                      ant.infileset :file=>emma.metadata_file
                      ant.infileset :file=>emma.coverage_file
                      ant.send format, :outfile=>File.join(emma.report_to(format),"coverage.#{format}")
                      ant.sourcepath do
                        emma.sources.flatten.each do |src|
                          ant.dirset(:dir=>src.to_s) if File.exist?(src.to_s)
                        end
                      end
                    end
                  end
                else
                  info "No test coverage report for #{project}. Missing: #{missing_required_files.join(', ')}"
                end
              end
            end
          end
        end

        project.clean do
          rm_rf [emma.report_dir, emma.coverage_file, emma.metadata_file, emma.instrumented_dir]
        end

      end

    end

    class Buildr::Project
      include EmmaExtension
    end

    namespace "emma" do

      Project.local_task('instrument') { |name| "Instrumenting #{name}" }

      [:xml, :html].each do |format|
        desc "Run the test cases and produce code coverage reports in #{format}"
        task format => ['instrument', 'test'] do
          info "Creating test coverage reports in #{format}"
          mkdir_p report_to(format)
          Emma.ant do |ant|
            ant.merge :outfile=>data_file do
              Buildr.projects.each do |project|
                [project.emma.metadata_file, project.emma.coverage_file].each do |data_file|
                  ant.fileset :file=>data_file if File.exist?(data_file)
                end
              end
            end
            ant.report do
              ant.infileset :file=>data_file
              ant.send format, :outfile=>File.join(report_to(format), "coverage.#{format}")
              ant.sourcepath do
                Buildr.projects.map(&:emma).map(&:sources).flatten.map(&:to_s).each do |src|
                  ant.dirset :dir=>src if File.exist?(src)
                end
              end
            end
          end
        end
      end

      task :clean do
        rm_rf [report_to, data_file]
      end
      end

    task :clean do
      task('emma:clean').invoke if Dir.pwd == Rake.application.original_dir
    end

  end
end
