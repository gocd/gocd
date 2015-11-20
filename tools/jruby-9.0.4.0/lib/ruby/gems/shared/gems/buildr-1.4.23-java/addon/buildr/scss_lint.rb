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
  # Provides the <code>scss_lint:html</code> and <code>scss_lint:xml</code> tasks.
  # Require explicitly using <code>require "buildr/scss_lint"</code>.
  module ScssLint
    class << self

      def scss_lint(output_file, source_paths, options = {})
        args = []
        if ENV['BUNDLE_GEMFILE']
          args << 'bundle'
          args << 'exec'
        end
        args << 'scss-lint'
        if options[:configuration_file]
          args << '--config'
          args << options[:configuration_file]
        end
        if options[:file_excludes]
          args << '--exclude'
          args << options[:file_excludes].join(',')
        end
        if options[:formatter]
          args << '--format'
          args << options[:formatter]
        end
        if options[:linter_includes] && !options[:linter_includes].empty?
          args << '--include-linter'
          args << options[:linter_includes].join(',')
        end
        if options[:linter_excludes] && !options[:linter_excludes].empty?
          args << '--exclude-linter'
          args << options[:linter_excludes].join(',')
        end

        source_paths.each do |source_path|
          args << source_path
        end

        mkdir_p File.dirname(output_file)
        File.open(output_file, 'wb') do |f|
          f.write `#{args.join(' ')}`
        end
      end
    end

    class Config
      def enabled?
        File.exist?(self.configuration_file)
      end

      def html_enabled?
        File.exist?(self.style_file)
      end

      attr_writer :config_directory

      def config_directory
        @config_directory || project._(:source, :main, :etc, :scss_lint)
      end

      attr_writer :report_dir

      def report_dir
        @report_dir || project._(:reports, :scss_lint)
      end

      attr_writer :file_excludes

      def file_excludes
        @file_excludes ||= []
      end

      attr_writer :linter_includes

      def linter_includes
        @linter_includes ||= []
      end

      attr_writer :linter_excludes

      def linter_excludes
        @linter_excludes ||= []
      end

      attr_writer :configuration_file

      def configuration_file
        @configuration_file || "#{self.config_directory}/checks.yml"
      end

      attr_writer :format

      def format
        @format || 'XML'
      end

      attr_writer :xml_output_file

      def xml_output_file
        @xml_output_file || "#{self.report_dir}/scss_lint.xml"
      end

      attr_writer :html_output_file

      def html_output_file
        @html_output_file || "#{self.report_dir}/scss_lint.html"
      end

      attr_writer :style_file

      def style_file
        unless @style_file
          project_xsl = "#{self.config_directory}/scss_lint-report.xsl"
          if File.exist?(project_xsl)
            @style_file = project_xsl
          else
            @style_file = "#{File.dirname(__FILE__)}/scss_lint-report.xsl"
          end
        end
        @style_file
      end

      def source_paths
        @source_paths ||= [self.project._(:source, :main, :webapp, :sass)]
      end

      protected

      def initialize(project)
        @project = project
      end

      attr_reader :project

    end

    module ProjectExtension
      include Extension

      def scss_lint
        @scss_lint ||= Buildr::ScssLint::Config.new(project)
      end

      after_define do |project|
        if project.scss_lint.enabled?
          desc "Generate scss-lint xml report."
          project.task("scss_lint:xml") do
            source_paths = project.scss_lint.source_paths.flatten.compact
            source_paths.each do |path|
              path.respond_to?(:invoke) ? path.invoke : project.file(path).invoke
            end
            puts "ScssLint: Analyzing source code..."
            Buildr::ScssLint.scss_lint(project.scss_lint.xml_output_file,
                                       source_paths,
                                       :formatter => project.scss_lint.format,
                                       :configuration_file => project.scss_lint.configuration_file,
                                       :file_excludes => project.scss_lint.file_excludes,
                                       :linter_includes => project.scss_lint.linter_includes,
                                       :linter_excludes => project.scss_lint.linter_excludes)
          end

          if project.scss_lint.html_enabled?
            xml_task = project.task("scss_lint:xml")
            desc "Generate scss_lint html report."
            project.task("scss_lint:html" => xml_task) do
              puts "ScssLint: Generating report"
              mkdir_p File.dirname(project.scss_lint.html_output_file)
              Buildr.ant "scss_lint" do |ant|
                ant.xslt :in => project.scss_lint.xml_output_file,
                         :out => project.scss_lint.html_output_file,
                         :style => project.scss_lint.style_file
              end
            end

          end
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::ScssLint::ProjectExtension
end
