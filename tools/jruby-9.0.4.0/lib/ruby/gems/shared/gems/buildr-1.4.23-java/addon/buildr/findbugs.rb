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
  # Provides the <code>findbugs:html</code> and <code>findbugs:xml</code> tasks.
  # Require explicitly using <code>require "buildr/findbugs"</code>.
  module Findbugs

    class << self

      # The specs for requirements
      def dependencies
        %w(
          com.google.code.findbugs:findbugs:jar:3.0.0
          com.google.code.findbugs:jFormatString:jar:3.0.0
          com.google.code.findbugs:bcel-findbugs:jar:6.0
          com.google.code.findbugs:annotations:jar:3.0.0
          org.ow2.asm:asm-debug-all:jar:5.0.2
          commons-lang:commons-lang:jar:2.6
          dom4j:dom4j:jar:1.6.1
          jaxen:jaxen:jar:1.1.6
        )
      end

      def findbugs(output_file, source_paths, analyze_paths, options = {})
        dependencies = (options[:dependencies] || []) + self.dependencies
        cp = Buildr.artifacts(dependencies).each { |a| a.invoke() if a.respond_to?(:invoke) }.map(&:to_s).join(File::PATH_SEPARATOR)

        args = {
          :output => options[:output] || 'xml',
          :outputFile => output_file,
          :effort => 'max',
          :pluginList => '',
          :classpath => cp,
          :reportLevel => options[:report_level] || 'medium',
          :timeout => '90000000',
          :debug => 'false'
        }
        args[:failOnError] = true if options[:fail_on_error]
        args[:excludeFilter] = options[:exclude_filter] if options[:exclude_filter]
        args[:jvmargs] = options[:java_args] if options[:java_args]

        mkdir_p File.dirname(output_file)

        Buildr.ant('findBugs') do |ant|
          ant.taskdef :name => 'findBugs',
                      :classname => 'edu.umd.cs.findbugs.anttask.FindBugsTask',
                      :classpath => cp
          ant.findBugs args do
            source_paths.each do |source_path|
              ant.sourcePath :path => source_path.to_s
            end
            Buildr.artifacts(analyze_paths).each(&:invoke).each do |analyze_path|
              ant.auxAnalyzePath :path => analyze_path.to_s
            end
            if options[:properties]
              options[:properties].each_pair do |k, v|
                ant.systemProperty :name => k, :value => v
              end
            end
            if options[:extra_dependencies]
              ant.auxClasspath do |aux|
                Buildr.artifacts(options[:extra_dependencies]).each { |a| a.invoke() if a.respond_to?(:invoke) }.each do |dep|
                  aux.pathelement :location => dep.to_s
                end
              end
            end
          end
        end
      end
    end

    class Config

      attr_accessor :enabled

      def enabled?
        !!@enabled
      end

      attr_writer :config_directory

      def config_directory
        @config_directory || project._(:source, :main, :etc, :findbugs)
      end

      attr_writer :report_level

      def report_level
        @report_level || 'medium'
      end

      attr_writer :report_dir

      def report_dir
        @report_dir || project._(:reports, :findbugs)
      end

      attr_writer :fail_on_error

      def fail_on_error?
        @fail_on_error.nil? ? false : @fail_on_error
      end

      attr_writer :xml_output_file

      def xml_output_file
        @xml_output_file || "#{self.report_dir}/findbugs.xml"
      end

      attr_writer :html_output_file

      def html_output_file
        @html_output_file || "#{self.report_dir}/findbugs.html"
      end

      attr_writer :filter_file

      def filter_file
        @filter_file || "#{self.config_directory}/filter.xml"
      end

      def properties
        @properties ||= {}
      end

      attr_writer :java_args

      def java_args
        @java_args || '-server -Xss1m -Xmx800m -Duser.language=en -Duser.region=EN '
      end

      def source_paths
        @source_paths ||= [self.project.compile.sources, self.project.test.compile.sources].flatten.compact
      end

      def analyze_paths
        @analyze_path ||= [self.project.compile.target]
      end

      def extra_dependencies
        @extra_dependencies ||= [self.project.compile.dependencies, self.project.test.compile.dependencies].flatten.compact
      end

      protected

      def initialize(project)
        @project = project
      end

      attr_reader :project
    end

    module ProjectExtension
      include Extension

      def findbugs
        @findbugs ||= Buildr::Findbugs::Config.new(project)
      end

      after_define do |project|
        if project.findbugs.enabled?
          desc 'Generate findbugs xml report.'
          project.task('findbugs:xml') do
            puts 'Findbugs: Analyzing source code...'
            options =
              {
                :properties => project.findbugs.properties,
                :fail_on_error => project.findbugs.fail_on_error?,
                :extra_dependencies => project.findbugs.extra_dependencies
              }
            options[:exclude_filter] = project.findbugs.filter_file if File.exist?(project.findbugs.filter_file)
            options[:output] = 'xml:withMessages'
            options[:report_level] = project.findbugs.report_level

            Buildr::Findbugs.findbugs(project.findbugs.xml_output_file,
                                      project.findbugs.source_paths.flatten.compact,
                                      project.findbugs.analyze_paths.flatten.compact,
                                      options)
          end

          desc 'Generate findbugs html report.'
          project.task('findbugs:html') do
            puts 'Findbugs: Analyzing source code...'
            options =
              {
                :properties => project.findbugs.properties,
                :fail_on_error => project.findbugs.fail_on_error?,
                :extra_dependencies => project.findbugs.extra_dependencies
              }
            options[:exclude_filter] = project.findbugs.filter_file if File.exist?(project.findbugs.filter_file)
            options[:output] = 'html'
            options[:report_level] = project.findbugs.report_level

            Buildr::Findbugs.findbugs(project.findbugs.html_output_file,
                                      project.findbugs.source_paths.flatten.compact,
                                      project.findbugs.analyze_paths.flatten.compact,
                                      options)
          end
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::Findbugs::ProjectExtension
end
