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

  # Provides the <code>cobertura:html</code>, <code>cobertura:xml</code> and <code>cobertura:check</code> tasks.
  # Require explicitly using <code>require "buildr/java/cobertura"</code>.
  #
  # You can generate cobertura reports for a single project
  # using the project name as prefix:
  #
  #   project_name:cobertura:html
  #
  # You can also specify which classes to include/exclude from instrumentation by
  # passing a class name regexp to the <code>cobertura.include</code> or
  # <code>cobertura.exclude</code> methods.
  #
  #   define 'someModule' do
  #      cobertura.include 'some.package.*'
  #      cobertura.include /some.(foo|bar).*/
  #      cobertura.exclude 'some.foo.util.SimpleUtil'
  #      cobertura.exclude /*.Const(ants)?/i
  #   end
  #
  # You can also specify the top level directory to which the top level cobertura tasks
  # will generate reports by setting the value of the <code>Buildr::Cobertura.report_dir</code>
  # configuration parameter.
  #
  module Cobertura

    VERSION = '1.9.4.1'

    class << self
      def version
        Buildr.settings.build['cobertura'] || VERSION
      end
    end

    REQUIRES = ArtifactNamespace.for(self).tap do |ns|
      ns.cobertura! "net.sourceforge.cobertura:cobertura:jar:#{version}", '>=1.9'
      ns.log4j! 'log4j:log4j:jar:1.2.9', ">=1.2.9"
      ns.asm! 'asm:asm:jar:2.2.1', '>=2.2.1'
      ns.asm_tree! 'asm:asm-tree:jar:2.2.1', '>=2.2.1'
      ns.oro! 'oro:oro:jar:2.0.8', '>=2.0.8'
    end

    class << self
      def dependencies
        if (VersionRequirement.create('>=1.9.1').satisfied_by?(REQUIRES.cobertura.version))
          [:asm, :asm_tree].each { |s| REQUIRES[s] = '3.0' unless REQUIRES[s].selected? }
        end

        REQUIRES.artifacts
      end

      attr_writer :report_dir

      def report_dir
        @report_dir || "reports/cobertura"
      end

      def report_to(file = nil)
        File.expand_path(File.join(*[report_dir, file.to_s].compact))
      end

      def data_file
        File.expand_path("#{report_dir}.ser")
      end

    end

    class CoberturaConfig # :nodoc:

      def initialize(project)
        @project = project
      end

      attr_reader :project
      private :project

      attr_writer :data_file, :instrumented_dir, :report_dir

      def data_file
        @data_file ||= project.path_to(:reports, 'cobertura.ser')
      end

      def instrumented_dir
        @instrumented_dir ||= project.path_to(:target, :instrumented, :classes)
      end

      def report_dir
        @report_dir ||= project.path_to(:reports, :cobertura)
      end

      def report_to(file = nil)
        File.expand_path(File.join(*[report_dir, file.to_s].compact))
      end

      # :call-seq:
      #   project.cobertura.include(*classPatterns)
      #
      def include(*classPatterns)
        includes.push(*classPatterns.map { |p| String === p ? Regexp.new(p) : p })
        self
      end

      def includes
        @includeClasses ||= []
      end

      # :call-seq:
      #   project.cobertura.exclude(*classPatterns)
      #
      def exclude(*classPatterns)
        excludes.push(*classPatterns.map { |p| String === p ? Regexp.new(p) : p })
        self
      end

      def excludes
        @excludeClasses ||= []
      end

      def ignore(*regexps)
        ignores.push(*regexps)
      end

      def ignores
        @ignores ||= []
      end

      def sources
        project.compile.sources
      end

      def check
        @check ||= CoberturaCheck.new
      end
    end

    class CoberturaCheck
      attr_writer :branch_rate, :line_rate, :total_branch_rate, :total_line_rate, :package_line_rate, :package_branch_rate
      attr_reader :branch_rate, :line_rate, :total_branch_rate, :total_line_rate, :package_line_rate, :package_branch_rate
    end

    module CoberturaExtension # :nodoc:
      include Buildr::Extension

      def cobertura
        @cobertura_config ||= CoberturaConfig.new(self)
      end

      after_define do |project|
        cobertura = project.cobertura

        namespace 'cobertura' do
          unless project.compile.target.nil?
            # Instrumented bytecode goes in a different directory. This task creates before running the test
            # cases and monitors for changes in the generate bytecode.
            instrumented = project.file(cobertura.instrumented_dir => project.compile.target) do |task|
              mkdir_p task.to_s
              unless project.compile.sources.empty?
                info "Instrumenting classes with cobertura data file #{cobertura.data_file}"
                Buildr.ant "cobertura" do |ant|
                  ant.taskdef :resource=>"tasks.properties",
                    :classpath=>Buildr.artifacts(Cobertura.dependencies).each(&:invoke).map(&:to_s).join(File::PATH_SEPARATOR)
                  ant.send "cobertura-instrument", :todir=>task.to_s, :datafile=>cobertura.data_file do
                    includes, excludes = cobertura.includes, cobertura.excludes

                    classes_dir = project.compile.target.to_s
                    if includes.empty? && excludes.empty?
                      ant.fileset :dir => classes_dir do
                        ant.include :name => "**/*.class"
                      end
                    else
                      includes = [//] if includes.empty?
                      Dir.glob(File.join(classes_dir, "**/*.class")) do |cls|
                        cls_name = cls.gsub(/#{classes_dir}\/?|\.class$/, '').gsub('/', '.')
                        if includes.any? { |p| p === cls_name } && !excludes.any? { |p| p === cls_name }
                          ant.fileset :file => cls
                        end
                      end
                    end

                    cobertura.ignores.each { |r| ant.ignore :regex => r }
                  end
                end
              end
              touch task.to_s
            end

            task 'instrument' => instrumented

            # We now have two target directories with bytecode. It would make sense to remove compile.target
            # and add instrumented instead, but apparently Cobertura only creates some of the classes, so
            # we need both directories and instrumented must come first.
            project.test.dependencies.unshift cobertura.instrumented_dir
            project.test.with Cobertura.dependencies
            project.test.options[:properties]["net.sourceforge.cobertura.datafile"] = cobertura.data_file

            unless project.compile.sources.empty?
              [:xml, :html].each do |format|
                task format => ['instrument', 'test'] do
                  info "Creating test coverage reports in #{cobertura.report_to(format)}"
                  Buildr.ant "cobertura" do |ant|
                    ant.taskdef :resource=>"tasks.properties",
                      :classpath=>Buildr.artifacts(Cobertura.dependencies).each(&:invoke).map(&:to_s).join(File::PATH_SEPARATOR)
                    ant.send "cobertura-report", :format=>format,
                      :destdir=>cobertura.report_to(format), :datafile=>cobertura.data_file do
                      cobertura.sources.flatten.each do |src|
                        ant.fileset(:dir=>src.to_s) if File.exist?(src.to_s)
                      end
                    end
                  end
                end
              end
            end

            task :check => [:instrument, :test] do
              Buildr.ant "cobertura" do |ant|
                ant.taskdef :classpath=>Cobertura.dependencies.join(File::PATH_SEPARATOR), :resource=>"tasks.properties"
                params = { :datafile => Cobertura.data_file }

                # oh so ugly...
                params[:branchrate] = cobertura.check.branch_rate if cobertura.check.branch_rate
                params[:linerate] = cobertura.check.line_rate if cobertura.check.line_rate
                params[:totalbranchrate] = cobertura.check.total_branch_rate if cobertura.check.total_branch_rate
                params[:totallinerate] = cobertura.check.total_line_rate if cobertura.check.total_line_rate
                params[:packagebranchrate] = cobertura.check.package_branch_rate if cobertura.check.package_branch_rate
                params[:packagelinerate] = cobertura.check.package_line_rate if cobertura.check.package_line_rate

                ant.send("cobertura-check", params) do
                end
              end
            end

          end
        end

        project.clean do
          rm_rf [cobertura.report_to, cobertura.data_file, cobertura.instrumented_dir]
        end

      end

    end

    class Buildr::Project
      include CoberturaExtension
    end

    namespace "cobertura" do

      task "instrument" do
        Buildr.projects.each do |project|
          project.cobertura.data_file = data_file
          project.test.options[:properties]["net.sourceforge.cobertura.datafile"] = data_file
          instrument_task ="#{project.name}:cobertura:instrument"
          task(instrument_task).invoke if Rake::Task.task_defined?(instrument_task)
        end
      end

      [:xml, :html].each do |format|
        desc "Run the test cases and produce code coverage reports"
        task format => ["instrument", "test"] do
          report_target = report_to(format)
          if Buildr.projects.detect { |project| !project.compile.sources.empty? }
            info "Creating test coverage reports in #{report_target}"
            Buildr.ant "cobertura" do |ant|
              ant.taskdef :resource=>"tasks.properties",
                :classpath=>Buildr.artifacts(Cobertura.dependencies).each(&:invoke).map(&:to_s).join(File::PATH_SEPARATOR)
              ant.send "cobertura-report", :destdir=>report_target, :format=>format, :datafile=>data_file do
                Buildr.projects.map(&:cobertura).map(&:sources).flatten.each do |src|
                  ant.fileset :dir=>src.to_s if File.exist?(src.to_s)
                end
              end
            end
          end
        end
      end

      task "clean" do
        rm_rf [report_to, data_file]
      end
    end

    task "clean" do
      task("cobertura:clean").invoke if Dir.pwd == Rake.application.original_dir
    end

  end
end
