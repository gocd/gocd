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
  module Sonar

    class << self

      # The specs for requirements
      def dependencies
        [
          'org.codehaus.sonar-plugins:sonar-ant-task:jar:1.3'
        ]
      end

      def sonar(jdbc_url, jdbc_driver_class_name, jdbc_username, jdbc_password, host_url, project_name, key, sources, binaries, libraries)

        # Build the artifacts for FindBugs to analyse
        Buildr.artifacts(binaries).each(&:invoke)

        cp = Buildr.artifacts(self.dependencies).each(&:invoke).map(&:to_s).join(File::PATH_SEPARATOR)

        args = {
          :key => key,
          :version => '1',
          'xmlns:sonar' => 'antlib:org.sonar.ant'
        }

        Buildr.ant('sonar') do |ant|
          ant.taskdef  :name => 'sonar', :classname => 'org.sonar.ant.SonarTask', :classpath => cp

          ant.property :name => 'sonar.projectName', :value => project_name

          ant.property :name => 'sonar.jdbc.url', :value => jdbc_url
          ant.property :name => 'sonar.jdbc.driverClassName', :value => jdbc_driver_class_name
          ant.property :name => 'sonar.jdbc.username', :value => jdbc_username
          ant.property :name => 'sonar.jdbc.password', :value => jdbc_password
          ant.property :name => 'sonar.host.url', :value => host_url

          ant.property :name => 'sonar.checkstyle.generateXml', :value => 'true'

          ant.property :name => 'sonar.sources', :value => sources.join(',')
          ant.property :name => 'sonar.binaries', :value => binaries.join(',')
          ant.property :name => 'sonar.libraries', :value => libraries.join(',')

          ant.sonar args

        end
      end
    end

    class Config

      attr_accessor :enabled
      attr_accessor :jdbc_url
      attr_accessor :jdbc_driver_class_name
      attr_accessor :jdbc_username
      attr_accessor :jdbc_password
      attr_accessor :host_url
      attr_accessor :key
      attr_accessor :project_name

      attr_writer :sources
      def sources
        @sources ||= []
      end

      attr_writer :binaries
      def binaries
        @binaries ||= []
      end

      attr_writer :libraries
      def libraries
        @libraries ||= []
      end

      def enabled?
        !!@enabled
      end

      protected

      def initialize(project)
        @project = project
      end

      attr_reader :project

    end

    module ProjectExtension
      include Extension

      def sonar
        @sonar ||= Buildr::Sonar::Config.new(project)
      end

      after_define do |project|
        if project.sonar.enabled?
          desc 'Execute Sonar code analysis'
          project.task('sonar') do
            puts 'Sonar: Analyzing source code...'

            sources = project.sonar.sources.flatten.compact
            binaries = project.sonar.binaries.flatten.compact
            libraries = project.sonar.libraries.flatten.compact

            Buildr::Sonar.sonar(
              project.sonar.jdbc_url,
              project.sonar.jdbc_driver_class_name,
              project.sonar.jdbc_username,
              project.sonar.jdbc_password,
              project.sonar.host_url,
              project.sonar.project_name,
              project.sonar.key,
              sources,
              binaries,
              libraries
            )
          end
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::Sonar::ProjectExtension
end