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
  module GWT

    class << self
      # The specs for requirements
      def dependencies
        ['com.google.gwt:gwt-dev:jar:2.4.0']
      end

      def gwtc_main(modules, source_artifacts, output_dir, options = {})
        cp = Buildr.artifacts(self.dependencies).each(&:invoke).map(&:to_s) + Buildr.artifacts(source_artifacts).each(&:invoke).map(&:to_s)
        style = options[:style] || "OBFUSCATED," # "PRETTY", "DETAILED"
        log_level = options[:log_level] #  ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL
        workers = options[:workers] || 2

        args = []
        if log_level
          args << "-logLevel"
          args << log_level
        end
        args << "-strict"
        args << "-style"
        args << style
        args << "-localWorkers"
        args << workers
        args << "-war"
        args << output_dir
        if options[:compile_report_dir]
          args << "-compileReport"
          args << "-extra"
          args << options[:compile_report_dir]
        end

        if options[:draft_compile]
          args << "-draftCompile"
        end

        args += modules

        Java::Commands.java 'com.google.gwt.dev.Compiler', *(args + [{:classpath => cp, :properties => options[:properties], :java_args => options[:java_args]}])
      end
    end

    module ProjectExtension
      include Extension

      def gwt(module_names, options = {})
        output_key = options[:output_key] || project.id
        output_dir = project._(:target, :generated, :gwt, output_key)
        artifacts = (project.compile.sources + project.resources.sources).collect do |a|
          a.is_a?(String) ? file(a) : a
        end
        dependencies = artifacts(options[:dependencies]) || project.compile.dependencies
        task = file(output_dir) do
          Buildr::GWT.gwtc_main(module_names, dependencies + artifacts, output_dir, options.dup)
        end
        task.enhance(dependencies)
        task.enhance([project.compile])
        task
      end
    end
  end
end

class Buildr::Project
  include Buildr::GWT::ProjectExtension
end