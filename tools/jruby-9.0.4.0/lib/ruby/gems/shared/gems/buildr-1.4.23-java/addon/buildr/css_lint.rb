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
  # Provides the <code>css_lint:html</code> and <code>css_lint:xml</code> tasks.
  # Require explicitly using <code>require "buildr/css_lint"</code>.
  module CssLint
    class << self

      def css_lint(output_file, source_paths, options = {})
        args = []
        args << 'csslint'
        args << "--format=#{options[:format]}" if options[:format]
        args << '--quiet'
        [:errors, :warnings, :ignore].each do |severity|
          if options[severity] && !options[severity].empty?
            args << "--#{severity}=#{options[severity].join(',')}"
          end
        end
        if options[:excludes] && !options[:excludes].empty?
          args << "--exclude-list=#{options[:excludes].join(',')}"
        end

        source_paths.each do |source_path|
          args << source_path.to_s
        end

        command = args.join(' ')
        mkdir_p File.dirname(output_file)
        File.open(output_file, 'wb') do |f|
          f.write `#{command}`
        end
        if 0 != $?.exitstatus
          error = IO.read(output_file)
          rm_f output_file
          raise "Problem running csslint: #{command}\n#{error}"
        end
      end
    end

    class Config
      def enabled?
        !self.source_paths.empty?
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
        @report_dir || project._(:reports, :css_lint)
      end

      attr_writer :excludes

      def excludes
        @excludes ||= []
      end

      attr_writer :errors

      def errors
        @errors ||= []
      end

      attr_writer :warnings

      def warnings
        @warnings ||= []
      end

      attr_writer :ignore

      def ignore
        @ignore ||= []
      end

      attr_writer :format

      def format
        @format || 'csslint-xml'
      end

      attr_writer :xml_output_file

      def xml_output_file
        @xml_output_file || "#{self.report_dir}/css_lint.xml"
      end

      attr_writer :html_output_file

      def html_output_file
        @html_output_file || "#{self.report_dir}/css_lint.html"
      end

      attr_writer :style_file

      def style_file
        unless @style_file
          project_xsl = "#{self.config_directory}/css_lint-report.xsl"
          if File.exist?(project_xsl)
            @style_file = project_xsl
          else
            @style_file = "#{File.dirname(__FILE__)}/css_lint-report.xsl"
          end
        end
        @style_file
      end

      def source_paths
        unless @source_paths
          @source_paths = []
          dir = self.project._(:source, :main, :webapp, :css)
          @source_paths << dir if File.directory?(dir)
        end
        @source_paths
      end

      protected

      def initialize(project)
        @project = project
      end

      attr_reader :project

    end

    module ProjectExtension
      include Extension

      def css_lint
        @css_lint ||= Buildr::CssLint::Config.new(project)
      end

      after_define do |project|
        if project.css_lint.enabled?
          desc 'Generate css-lint xml report.'
          project.task('css_lint:xml') do
            source_paths = project.css_lint.source_paths.flatten.compact
            source_paths.each do |path|
              path.respond_to?(:invoke) ? path.invoke : project.file(path).invoke
            end

            puts 'CssLint: Analyzing CSS...'
            Buildr::CssLint.css_lint(project.css_lint.xml_output_file,
                                     source_paths,
                                     :format => project.css_lint.format,
                                     :excludes => project.css_lint.excludes,
                                     :ignore => project.css_lint.ignore,
                                     :warnings => project.css_lint.warnings,
                                     :errors => project.css_lint.errors)
          end

          if project.css_lint.html_enabled?
            xml_task = project.task('css_lint:xml')
            desc 'Generate css_lint html report.'
            project.task('css_lint:html' => xml_task) do
              puts "CssLint: Generating report"
              mkdir_p File.dirname(project.css_lint.html_output_file)
              Buildr.ant 'css_lint' do |ant|
                ant.xslt :in => project.css_lint.xml_output_file,
                         :out => project.css_lint.html_output_file,
                         :style => project.css_lint.style_file
              end
            end
          end
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::CssLint::ProjectExtension
end
