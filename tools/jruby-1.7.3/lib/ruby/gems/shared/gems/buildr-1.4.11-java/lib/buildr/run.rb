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
  module Run
    include Extension

    class << self
      def runners
        @runners ||= []
      end

      def select_by_lang(lang)
        fail 'Unable to define run task for nil language' if lang.nil?
        runners.detect { |e| e.languages.nil? ? false : e.languages.include?(lang.to_sym) }
      end

      alias_method :select, :select_by_lang

      def select_by_name(name)
        fail 'Unable to define run task for nil' if name.nil?
        runners.detect { |e| e.to_sym == name.to_sym }
      end

    end

    # Base class for any run provider.  Defines most
    # common functionality (things like @lang@, @build?@ and friends).
    class Base
      attr_reader :project

      class << self
        attr_accessor :runner_name, :languages

        def specify(options)
          @runner_name ||= options[:name]
          @languages ||= options[:languages]
        end

        def to_sym
          @runner_name || name.split('::').last.downcase.to_sym
        end
      end

      def initialize(project)
        @project = project
      end

      def build?
        true
      end

      def launch
        fail 'Not implemented'
      end
    end

    class RunTask < Rake::Task
      # Classpath dependencies.
      attr_accessor :classpath

      # Returns the run options.
      attr_reader :options

      # Returns file dependencies
      attr_reader :files

      attr_reader :project # :nodoc:

      def initialize(*args) # :nodoc:
        super
        @options = {}
        @classpath = []
        @files = FileList[]
      end

      # :call-seq:
      #   with(*artifacts) => self
      #
      # Adds files and artifacts as classpath dependencies, and returns self.
      def with(*specs)
        @classpath |= Buildr.artifacts(specs.flatten).uniq
        self
      end

      # :call-seq:
      #   using(options) => self
      #
      # Sets the run options from a hash and returns self.
      #
      # For example:
      #   run.using :main=>'org.example.Main'
      def using(*args)
        args.pop.each { |key, value| @options[key.to_sym] = value } if Hash === args.last

        until args.empty?
          new_runner = Run.select_by_name(args.pop)
          @runner = new_runner.new(project) unless new_runner.nil?
        end

        self
      end

      # :call-seq:
      #   requires(*files) => self
      #
      # Adds additional files and directories as dependencies to the task and returns self.
      # When specifying a directory, includes all files in that directory.
      def requires(*files)
        @files.include *files.flatten.compact
        self
      end

      def runner
        @runner ||= guess_runner
      end

      def runner?
        @runner ||= begin
          guess_runner if project.compile.language
        rescue
          nil
        end
      end

      def run
        runner.run(self) if runner?
      end

      def prerequisites #:nodoc:
        super + @files + classpath
      end

    private

      def guess_runner
        runner = Run.select project.compile.language
        fail 'Unable to guess runner for project.' unless runner
        runner.new project
      end

      def associate_with(project)
        @project ||= project
      end
    end

    first_time do
      Project.local_task 'run'
    end

    before_define(:run => :test) do |project|
      RunTask.define_task('run').tap do |run|
        run.send(:associate_with, project)
        run.enhance([project.compile, project.test]) do |t|
          # double-enhance to execute the runner last
          run.enhance { |t| t.run }
        end
      end
    end

    after_define(:run => :test) do |project|
      project.run.with project.compile.dependencies
      project.run.with project.resources.target if project.resources.target
      project.run.with project.compile.target if project.compile.target
    end

    # :call-seq:
    #   run(&block) => RunTask
    #
    # This method returns the project's run task. It also accepts a block to be executed
    # when the run task is invoked.
    def run(&block)
      task('run').tap do |t|
        t.enhance &block if block
      end
    end

  end

  class Project
    include Run
  end
end
