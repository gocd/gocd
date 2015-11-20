# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr
  # Initial support for JaCoCo coverage reports.
  # WARNING: Experimental and may change radically.
  module JaCoCo
    class << self
      VERSION = '0.7.2.201409121644'

      def version
        @version || Buildr.settings.build['jacoco'] || VERSION
      end

      def version=(value)
        @version = value
      end

      def agent_spec
        ["org.jacoco:org.jacoco.agent:jar:runtime:#{version}"]
      end

      def ant_spec
        [
          "org.jacoco:org.jacoco.report:jar:#{version}",
          "org.jacoco:org.jacoco.core:jar:#{version}",
          "org.jacoco:org.jacoco.ant:jar:#{version}",
          'org.ow2.asm:asm-debug-all:jar:5.0.1'
        ]
      end
    end

    class Config

      attr_writer :enabled

      def enabled?
        @enabled.nil? ? true : @enabled
      end

      attr_writer :destfile

      def destfile
        @destfile || "#{self.report_dir}/jacoco.cov"
      end

      attr_writer :output

      def output
        @output || 'file'
      end

      attr_accessor :sessionid
      attr_accessor :address
      attr_accessor :port
      attr_accessor :classdumpdir
      attr_accessor :dumponexit
      attr_accessor :append
      attr_accessor :exclclassloader

      def includes
        @includes ||= []
      end

      def excludes
        @excludes ||= []
      end

      attr_writer :report_dir

      def report_dir
        @report_dir || project._(:reports, :jacoco)
      end

      attr_writer :generate_xml

      def generate_xml?
        @generate_xml.nil? ? false : @generate_xml
      end

      attr_writer :xml_output_file

      def xml_output_file
        @xml_output_file || "#{self.report_dir}/jacoco.xml"
      end

      attr_writer :generate_html

      def generate_html?
        @generate_html.nil? ? false : @generate_html
      end

      attr_writer :html_output_directory

      def html_output_directory
        @html_output_directory || "#{self.report_dir}/jacoco"
      end

      protected

      def initialize(project)
        @project = project
      end

      attr_reader :project

    end

    module ProjectExtension
      include Extension

      def jacoco
        @jacoco ||= Buildr::JaCoCo::Config.new(project)
      end

      after_define do |project|
        unless project.test.compile.target.nil? || !project.jacoco.enabled?
          project.test.setup do
            agent_jar = Buildr.artifacts(Buildr::JaCoCo.agent_spec).each(&:invoke).map(&:to_s).join('')
            options = []
            %w(destfile append exclclassloader sessionid dumponexit output address port classdumpdir).each do |option|
              value = project.jacoco.send(option.to_sym)
              options << "#{option}=#{value}" unless value.nil?
            end
            options << "includes=#{project.jacoco.includes.join(':')}" unless project.jacoco.includes.empty?
            options << "excludes=#{project.jacoco.excludes.join(':')}" unless project.jacoco.excludes.empty?

            agent_config = "-javaagent:#{agent_jar}=#{options.join(',')}"
            project.test.options[:java_args] = (project.test.options[:java_args] || []) + [agent_config]
          end
          namespace 'jacoco' do
            if project.jacoco.generate_xml?
              desc 'Generate JaCoCo reports.'
              task 'reports' do
                Buildr.ant 'jacoco' do |ant|
                  ant.taskdef(:resource => 'org/jacoco/ant/antlib.xml') do |ant|
                    ant.classpath :path => Buildr.artifacts(Buildr::JaCoCo.ant_spec).each(&:invoke).map(&:to_s).join(File::PATH_SEPARATOR)
                  end
                  ant.report do |ant|
                    ant.executiondata do |ant|
                      ant.file :file => project.jacoco.destfile
                    end

                    ant.structure(:name => project.name) do |ant|
                      if project.compile.target
                        ant.classfiles do |ant|
                          ant.fileset :dir => project.compile.target
                        end
                      end
                      ant.sourcefiles(:encoding => 'UTF-8') do |ant|
                        project.compile.sources.each do |path|
                          ant.fileset :dir => path.to_s
                        end
                      end
                    end

                    ant.xml :destfile => project.jacoco.xml_output_file if project.jacoco.generate_xml?
                    ant.html :destdir => project.jacoco.html_output_directory if project.jacoco.generate_html?
                  end
                end
              end
            end
          end
        end
      end
      namespace 'jacoco' do
        desc 'Generate JaCoCo reports.'
        task 'report' do
          Buildr.ant 'jacoco' do |ant|
            ant.taskdef(:resource => 'org/jacoco/ant/antlib.xml') do |ant|
              ant.classpath :path => Buildr.artifacts(Buildr::JaCoCo.ant_spec).each(&:invoke).map(&:to_s).join(File::PATH_SEPARATOR)
            end
            ant.report do |ant|
              ant.executiondata do |ant|
                Buildr.projects.each do |project|
                  ant.fileset :file=>project.jacoco.destfile if File.exist?(project.jacoco.destfile)
                end
              end

              ant.structure(:name => 'Jacoco Report') do |ant|
                ant.classfiles do |ant|
                  Buildr.projects.map(&:compile).map(&:target).flatten.map(&:to_s).each do |src|
                    ant.fileset :dir=>src.to_s if File.exist?(src)
                  end
                end
                ant.sourcefiles(:encoding => 'UTF-8') do |ant|
                  Buildr.projects.map(&:compile).map(&:sources).flatten.map(&:to_s).each do |src|
                    ant.fileset :dir=>src.to_s if File.exist?(src)
                  end
                end
              end

              ant.html :destdir => 'reports/jacoco'
              ant.xml :destfile => 'reports/jacoco/jacoco.xml'
              ant.csv :destfile => 'reports/jacoco/jacoco.csv'
            end
          end
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::JaCoCo::ProjectExtension
end
