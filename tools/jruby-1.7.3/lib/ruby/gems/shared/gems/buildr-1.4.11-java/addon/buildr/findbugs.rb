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
        [
            'com.google.code.findbugs:findbugs-ant:jar:1.3.9',
            'com.google.code.findbugs:findbugs:jar:1.3.9',
            'com.google.code.findbugs:bcel:jar:1.3.9',
            'com.google.code.findbugs:jsr305:jar:1.3.9',
            'com.google.code.findbugs:jFormatString:jar:1.3.9',
            'com.google.code.findbugs:annotations:jar:1.3.9',
            'dom4j:dom4j:jar:1.6.1',
            'jaxen:jaxen:jar:1.1.1',
            'jdom:jdom:jar:1.0',
            'xom:xom:jar:1.0',
            'com.ibm.icu:icu4j:jar:2.6.1',
            'asm:asm:jar:3.1',
            'asm:asm-analysis:jar:3.1',
            'asm:asm-tree:jar:3.1',
            'asm:asm-commons:jar:3.1',
            'asm:asm-util:jar:3.1',
            'asm:asm-xml:jar:3.1',
            'commons-lang:commons-lang:jar:2.4'
        ]
      end

      def findbugs(output_file, source_paths, analyze_paths, options = { })
        dependencies = (options[:dependencies] || []) + self.dependencies
        cp = Buildr.artifacts(dependencies).each { |a| a.invoke() if a.respond_to?(:invoke) }.map(&:to_s).join(File::PATH_SEPARATOR)

        args = {
            :output => "xml:withMessages",
            :outputFile => output_file,
            :effort => 'max',
            :pluginList => '',
            :classpath => cp,
            :timeout => "90000000",
            :debug => "false"
        }
        args[:failOnError] = true if options[:fail_on_error]
        args[:excludeFilter] = options[:exclude_filter] if options[:exclude_filter]
        args[:jvmargs] = options[:java_args] if options[:java_args]

        Buildr.ant('findBugs') do |ant|
          ant.taskdef :name =>'findBugs',
                      :classname =>'edu.umd.cs.findbugs.anttask.FindBugsTask',
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

      def html_enabled?
        File.exist?(self.style_file)
      end

      attr_writer :config_directory

      def config_directory
        @config_directory || project._(:source, :main, :etc, :findbugs)
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

      attr_writer :style_file

      def style_file
        @style_file || "#{self.config_directory}/findbugs-report.xsl"
      end

      attr_writer :filter_file

      def filter_file
        @filter_file || "#{self.config_directory}/filter.xml"
      end

      def properties
        @properties ||= { }
      end

      attr_writer :java_args

      def java_args
        @java_args || "-server -Xss1m -Xmx800m -Duser.language=en -Duser.region=EN "
      end

      def source_paths
        @source_paths ||= [self.project.compile.sources, self.project.test.compile.sources]
      end

      def analyze_paths
        @analyze_path ||= [self.project.compile.target]
      end

      def extra_dependencies
        @extra_dependencies ||= [self.project.compile.dependencies, self.project.test.compile.dependencies]
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
          desc "Generate findbugs xml report."
          project.task("findbugs:xml") do
            puts "Findbugs: Analyzing source code..."
            mkdir_p File.dirname(project.findbugs.xml_output_file)

            options =
              {
                :properties => project.findbugs.properties,
                :fail_on_error => project.findbugs.fail_on_error?,
                :extra_dependencies => project.findbugs.extra_dependencies
              }
            options[:exclude_filter] = project.findbugs.filter_file if File.exist?(project.findbugs.filter_file)

            Buildr::Findbugs.findbugs(project.findbugs.xml_output_file,
                                      project.findbugs.source_paths.flatten.compact,
                                      project.findbugs.analyze_paths.flatten.compact,
                                      options)
          end

          if project.findbugs.html_enabled?
            xml_task = project.task("findbugs:xml")
            desc "Generate findbugs html report."
            project.task("findbugs:html" => xml_task) do
              puts "Findbugs: Generating report"
              mkdir_p File.dirname(project.findbugs.html_output_file)
              Buildr.ant "findbugs" do |ant|
                ant.style :in => project.findbugs.xml_output_file,
                          :out => project.findbugs.html_output_file,
                          :style => project.findbugs.style_file
              end
            end
          end
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::Findbugs::ProjectExtension
end
