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
        %w(com.google.gwt:gwt-dev:jar:2.5.1)
      end

      def gwtc_main(modules, source_artifacts, output_dir, unit_cache_dir, options = {})
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

        if options[:enable_closure_compiler].nil? || options[:enable_closure_compiler]
          args << "-XenableClosureCompiler"
        end

        args += modules

        properties = options[:properties] ? options[:properties].dup : {}
        properties["gwt.persistentunitcache"] = "true"
        properties["gwt.persistentunitcachedir"] = unit_cache_dir

        Java::Commands.java 'com.google.gwt.dev.Compiler', *(args + [{:classpath => cp, :properties => properties, :java_args => options[:java_args], :pathing_jar => false}])
      end

      def superdev_dependencies
        self.dependencies + %w(com.google.gwt:gwt-codeserver:jar:2.5.1)
      end

      def gwt_superdev(module_name, source_artifacts, work_dir, options = {})

        cp = Buildr.artifacts(self.superdev_dependencies).each(&:invoke).map(&:to_s) + Buildr.artifacts(source_artifacts).each(&:invoke).map(&:to_s)

        args = []
        args << "-port" << (options[:port] || 5050)
        args << "-workDir" << work_dir
        (options[:src] || []).each do |src|
          args << "-src" << src
        end
        args << module_name

        properties = options[:properties] ? options[:properties].dup : {}

        java_args = options[:java_args] ? options[:java_args].dup : {}

        Java::Commands.java 'com.google.gwt.dev.codeserver.CodeServer', *(args + [{:classpath => cp, :properties => properties, :java_args => java_args, :pathing_jar => false}])
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
        dependencies = options[:dependencies] ? artifacts(options[:dependencies]) : project.compile.dependencies

        unit_cache_dir = project._(:target, :gwt, :unit_cache_dir, output_key)

        task = project.file(output_dir) do
          Buildr::GWT.gwtc_main(module_names, dependencies + artifacts, output_dir, unit_cache_dir, options.dup)
        end
        task.enhance(dependencies)
        task.enhance([project.compile])
        project.assets.paths << task
        task
      end

      def gwt_superdev_runner(module_name, options = {})
        dependencies = artifacts(options[:dependencies]) || project.compile.dependencies

        desc "Run Superdev mode"
        project.task("superdev") do
          work_dir = project._(:target, :gwt, :superdev)
          mkdir_p work_dir
          Buildr::GWT.gwt_superdev(module_name, dependencies, work_dir, options)
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::GWT::ProjectExtension
end
