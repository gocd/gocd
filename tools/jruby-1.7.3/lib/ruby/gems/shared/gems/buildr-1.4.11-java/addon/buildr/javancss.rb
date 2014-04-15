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
  # Provides the <code>javancss:html</code> and <code>javancss:xml</code> tasks.
  # Require explicitly using <code>require "buildr/javancss"</code>.
  module JavaNCSS

    class << self

      # The specs for requirements
      def dependencies
        [
          'org.codehaus.javancss:javancss:jar:32.53',
          'javancss:ccl:jar:29.50',
          'javancss:jhbasic:jar:29.50'
        ]
      end

      def javancss(output_file, source_paths, options = {})
        dependencies = (options[:dependencies] || []) + self.dependencies
        cp = Buildr.artifacts(dependencies).each(&:invoke).map(&:to_s)

        args = []
        args << "-all"
        args << "-xml"
        args << "-out"
        args << output_file
        args << "-recursive"
        source_paths.each do |source_path|
          args << source_path
        end

        begin
          Java::Commands.java 'javancss.Main', *(args + [{:classpath => cp, :properties => options[:properties], :java_args => options[:java_args]}])
        rescue => e
          raise e if options[:fail_on_error]
        end
      end
    end

    class Config
      def enabled?
        !!@enabled
      end

      attr_writer :enabled

      def html_enabled?
        File.exist?(self.style_file)
      end

      attr_writer :config_directory

      def config_directory
        @config_directory || project._(:source, :main, :etc, :javancss)
      end

      attr_writer :report_dir

      def report_dir
        @report_dir || project._(:reports, :javancss)
      end

      attr_writer :fail_on_error

      def fail_on_error?
        @fail_on_error.nil? ? false : @fail_on_error
      end

      attr_writer :xml_output_file

      def xml_output_file
        @xml_output_file || "#{self.report_dir}/javancss.xml"
      end

      attr_writer :html_output_file

      def html_output_file
        @html_output_file || "#{self.report_dir}/javancss.html"
      end

      attr_writer :style_file

      def style_file
        @style_file || "#{self.config_directory}/javancss2html.xsl"
      end

      def source_paths
        @source_paths ||= [self.project.compile.sources, self.project.test.compile.sources]
      end

      protected

      def initialize(project)
        @project = project
      end

      attr_reader :project

    end

    module ProjectExtension
      include Extension

      def javancss
        @javancss ||= Buildr::JavaNCSS::Config.new(project)
      end

      after_define do |project|
        if project.javancss.enabled?
          desc "Generate JavaNCSS xml report."
          project.task("javancss:xml") do
            puts "JavaNCSS: Analyzing source code..."
            mkdir_p File.dirname(project.javancss.xml_output_file)
            Buildr::JavaNCSS.javancss(project.javancss.xml_output_file,
                                      project.javancss.source_paths.flatten.compact,
                                      :fail_on_error => project.javancss.fail_on_error?)
          end

          if project.javancss.html_enabled?
            xml_task = project.task("javancss:xml")
            desc "Generate JavaNCSS html report."
            project.task("javancss:html" => xml_task) do
              puts "JavaNCSS: Generating report"
              mkdir_p File.dirname(project.javancss.html_output_file)
              Buildr.ant "javancss" do |ant|
                ant.xslt :in => project.javancss.xml_output_file,
                         :out => project.javancss.html_output_file,
                         :style => project.javancss.style_file
              end
            end

          end
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::JavaNCSS::ProjectExtension
end
