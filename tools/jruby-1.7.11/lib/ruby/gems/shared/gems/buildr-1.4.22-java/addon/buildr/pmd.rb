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
  # Provides the <code>pmd:rule:xml</code>, <code>pmd:rule:html</code>, <code>pmd:cpd:xml</code>
  # and <code>pmd:cpd:html</code> tasks.
  #
  # Require explicitly using <code>require 'buildr/pmd'</code>.
  module Pmd

    class << self

      # The specs for requirements
      def dependencies
        [
          'net.sourceforge.pmd:pmd:jar:5.1.3',
          'jaxen:jaxen:jar:1.1.1',
          'commons-io:commons-io:jar:2.2',
          'com.beust:jcommander:jar:1.27',
          'asm:asm:jar:3.2'
        ]
      end

      def pmd(rule_set_files, format, output_file_prefix, source_paths, options = {})
        dependencies = (options[:dependencies] || []) + self.dependencies
        cp = Buildr.artifacts(dependencies).each(&:invoke).map(&:to_s)
        (options[:rule_set_paths] || []).each {|p| cp << p}

        rule_sets = rule_set_files.dup

        Buildr.artifacts(options[:rule_set_artifacts] || []).each do |artifact|
          a = artifact.to_s
          dirname = File.dirname(a)
          rule_sets << a[dirname.length + 1, a.length]
          cp << File.dirname(a)
          artifact.invoke
        end

        puts 'PMD: Analyzing source code...'
        mkdir_p File.dirname(output_file_prefix)

        Buildr.ant('pmd-report') do |ant|
          ant.taskdef :name=> 'pmd', :classpath => cp.join(';'), :classname => 'net.sourceforge.pmd.ant.PMDTask'
          ant.pmd :shortFilenames => true, :rulesetfiles => rule_sets.join(',') do
            ant.formatter :type => format, :toFile => "#{output_file_prefix}.#{format}"
            source_paths.each do |src|
              ant.fileset :dir=> src, :includes=>'**/*.java'
            end
          end

        end
      end

      def cpd(format, output_file_prefix, source_paths, options = {})
        dependencies = (options[:dependencies] || []) + self.dependencies
        cp = Buildr.artifacts(dependencies).each(&:invoke).map(&:to_s)
        minimum_token_count = options[:minimum_token_count] || 100
        encoding = options[:encoding] || 'UTF-8'

        puts 'PMD-CPD: Analyzing source code...'
        mkdir_p File.dirname(output_file_prefix)

        Buildr.ant('cpd-report') do |ant|
          ant.taskdef :name=> 'cpd', :classpath => cp.join(';'), :classname => 'net.sourceforge.pmd.cpd.CPDTask'
          ant.cpd :format => format, :minimumTokenCount => minimum_token_count, :encoding => encoding, :outputFile => "#{output_file_prefix}.#{format}" do
            source_paths.each do |src|
              ant.fileset :dir=> src, :includes=>'**/*.java'
            end
          end

        end
      end
    end

    class Config

      attr_writer :enabled

      def enabled?
        !!@enabled
      end

      attr_writer :rule_set_files

      def rule_set_files
        @rule_set_files ||= (self.rule_set_artifacts.empty? ? ['rulesets/java/basic.xml', 'rulesets/java/imports.xml', 'rulesets/java/unusedcode.xml', 'rulesets/java/finalizers.xml', 'rulesets/java/braces.xml'] : [])
      end

      # Support specification of rule sets that are distributed as part of a maven repository
      def rule_set_artifacts
        @rule_set_artifacts ||= []
      end

      attr_writer :rule_set_paths

      def rule_set_paths
        @rule_set_paths ||= []
      end

      attr_writer :report_dir

      def report_dir
        @report_dir || project._(:reports, :pmd)
      end

      attr_writer :output_file_prefix

      def output_file_prefix
        @output_file_prefix || "#{self.report_dir}/pmd"
      end

      attr_writer :cpd_output_file_prefix

      def cpd_output_file_prefix
        @cpd_output_file_prefix || "#{self.report_dir}/cpd"
      end

      def source_paths
        @source_paths ||= [self.project.compile.sources, self.project.test.compile.sources].flatten.compact
      end

      def flat_source_paths
        source_paths.flatten.compact
      end

      protected

      def initialize(project)
        @project = project
      end

      attr_reader :project
    end

    module ProjectExtension
      include Extension

      def pmd
        @pmd ||= Buildr::Pmd::Config.new(project)
      end

      after_define do |project|
        if project.pmd.enabled?
          desc 'Generate pmd xml report.'
          project.task('pmd:rule:xml') do
            Buildr::Pmd.pmd(project.pmd.rule_set_files, 'xml', project.pmd.output_file_prefix, project.pmd.flat_source_paths, :rule_set_paths => project.pmd.rule_set_paths, :rule_set_artifacts => project.pmd.rule_set_artifacts)
          end

          desc 'Generate pmd html report.'
          project.task('pmd:rule:html') do
            Buildr::Pmd.pmd(project.pmd.rule_set_files, 'html', project.pmd.output_file_prefix, project.pmd.flat_source_paths, :rule_set_paths => project.pmd.rule_set_paths, :rule_set_artifacts => project.pmd.rule_set_artifacts)
          end

          desc 'Generate pmd cpd xml report.'
          project.task('pmd:cpd:xml') do
            Buildr::Pmd.cpd('xml', project.pmd.cpd_output_file_prefix, project.pmd.flat_source_paths)
          end

          desc 'Generate pmd cpd text report.'
          project.task('pmd:cpd:text') do
            Buildr::Pmd.cpd('text', project.pmd.cpd_output_file_prefix, project.pmd.flat_source_paths)
          end
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::Pmd::ProjectExtension
end
