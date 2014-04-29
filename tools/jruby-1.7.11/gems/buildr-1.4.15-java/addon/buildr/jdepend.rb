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
  # Addes the <code>projectname:jdepend:swing</code>, <code>projectname:jdepend:text</code> and
  # <code>projectname:jdepend:xml</code> tasks.
  #
  # Require explicitly using <code>require "buildr/jdepend"</code>.
  module JDepend

    class << self

      # The specs for requirements
      def dependencies
        [
          'jdepend:jdepend:jar:2.9.1'
        ]
      end

      def jdepend(output_file, target_paths, options = {})
        dependencies = (options[:dependencies] || []) + self.dependencies
        cp = Buildr.artifacts(dependencies).each(&:invoke).map(&:to_s)

        args = []
        if output_file
          args << "-file"
          args << output_file
        end
        target_paths.each do |target_path|
          file(target_path).invoke
          args << target_path.to_s
        end

        # If no output file then we must be trying to run the swing app
        command = output_file ? 'jdepend.xmlui.JDepend' : 'jdepend.swingui.JDepend'

        begin
          Java::Commands.java command, *(args + [{:classpath => cp, :properties => options[:properties], :java_args => options[:java_args]}])
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
        @config_directory || project._(:source, :main, :etc, :jdepend)
      end

      attr_writer :report_dir

      def report_dir
        @report_dir || project._(:reports, :jdepend)
      end

      attr_writer :fail_on_error

      def fail_on_error?
        @fail_on_error.nil? ? false : @fail_on_error
      end

      attr_writer :xml_output_file

      def xml_output_file
        @xml_output_file || "#{self.report_dir}/jdepend.xml"
      end

      attr_writer :html_output_file

      def html_output_file
        @html_output_file || "#{self.report_dir}/jdepend.html"
      end

      attr_writer :style_file

      def style_file
        @style_file || "#{self.config_directory}/jdepend.xsl"
      end

      def target_paths
        @target_paths ||= [self.project.compile.target, self.project.test.compile.target]
      end

      def to_options
        {
            :fail_on_error => project.jdepend.fail_on_error?,
            # Set user home so that jdepend.properties will be loaded from there if present
            :properties => { 'user.home' => project.jdepend.config_directory }
        }
      end

      protected

      def initialize(project)
        @project = project
      end

      attr_reader :project

    end

    module ProjectExtension
      include Extension

      def jdepend
        @jdepend ||= Buildr::JDepend::Config.new(project)
      end

      after_define do |project|
        if project.jdepend.enabled?
          desc "Generate JDepend xml report."
          project.task("jdepend:xml") do
            puts "JDepend: Analyzing source code..."
            mkdir_p File.dirname(project.jdepend.xml_output_file)
            Buildr::JDepend.jdepend(project.jdepend.xml_output_file,
                                    project.jdepend.target_paths.flatten.compact,
                                    project.jdepend.to_options)
          end

          desc "Run JDepend with Swing UI."
          project.task("jdepend:swing") do
            puts "JDepend: Analyzing source code..."
            Buildr::JDepend.jdepend(nil,
                                    project.jdepend.target_paths.flatten.compact,
                                    project.jdepend.to_options)
          end

          if project.jdepend.html_enabled?
            xml_task = project.task("jdepend:xml")
            desc "Generate JDepend html report."
            project.task("jdepend:html" => xml_task) do
              puts "JDepend: Generating report"
              mkdir_p File.dirname(project.jdepend.html_output_file)
              Buildr.ant "jdepend" do |ant|
                ant.xslt :in => project.jdepend.xml_output_file,
                         :out => project.jdepend.html_output_file,
                         :style => project.jdepend.style_file
              end
            end

          end
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::JDepend::ProjectExtension
end
