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
  module Shell
    include Extension

    class << self
      def providers
        @providers ||= []
      end

      def select_by_lang(lang)
        fail 'Unable to define shell task for nil language' if lang.nil?
        providers.detect { |e| e.languages.nil? ? false : e.languages.include?(lang.to_sym) }
      end

      alias_method :select, :select_by_lang

      def select_by_name(name)
        fail 'Unable to define run task for nil' if name.nil?
        providers.detect { |e| e.to_sym == name.to_sym }
      end

      def define_task(project, name, provider = nil)
        ShellTask.define_task(name).tap do |t|
          t.send(:associate_with, project)
          t.enhance([project.compile]) do |t|
            # double-enhance to execute the provider last
            t.enhance { |t| t.run }
          end
          t.using provider.to_sym if provider
        end
      end
    end

    first_time do
      Project.local_task 'shell'

      providers.each { |provider| Project.local_task "shell:#{provider.to_sym}" }
    end

    before_define(:shell => :compile) do |project|
      define_task(project, "shell")
      providers.each { |provider| define_task(project, "shell:#{provider.to_sym}", provider) }
    end

    after_define(:shell => :compile) do |project|
      unless project.shell.provider
        provider = providers.find { |p| p.languages.include? project.compile.language if p.languages }
        if provider
          project.shell.using provider.to_sym
          project.shell.with project.test.compile.dependencies
        end
      end
    end

    # Base class for any shell provider.
    class Base
      attr_reader :project # :nodoc:

      class << self
        attr_accessor :shell_name, :languages

        def specify(options)
          @shell_name ||= options[:name]
          @languages ||= options[:languages]
        end

        def to_sym
          @shell_name || name.split('::').last.downcase.to_sym
        end
      end

      def initialize(project)
        @project = project
      end

      def launch(task)
        fail 'Not implemented'
      end

    end

    class ShellTask < Rake::Task
      attr_reader :project # :nodoc:

      # Classpath dependencies.
      attr_accessor :classpath

      # Returns the run options.
      attr_reader :options

      # Underlying shell provider
      attr_reader :provider

      def initialize(*args) # :nodoc:
        super
        @options = {}
        @classpath = []
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
      #   shell.using :properties => {'foo' => 'bar'}
      #   shell.using :bsh
      def using(*args)
        if Hash === args.last
          args.pop.each { |key, value| @options[key.to_sym] = value }
        end

        until args.empty?
          new_shell = Shell.select_by_name(args.pop)
          @provider = new_shell.new(project) unless new_shell.nil?
        end

        self
      end

      def run
        fail "No shell provider defined in project '#{project.name}' for language '#{project.compile.language.inspect}'" unless provider
        provider.launch(self)
      end

      def prerequisites #:nodoc:
        super + classpath
      end

      def java_args
        @options[:java_args] || (ENV['JAVA_OPTS'] || ENV['JAVA_OPTIONS']).to_s.split
      end

      def properties
        @options[:properties] || {}
      end

    private
      def associate_with(project)
        @project ||= project
      end

    end

    # :call-seq:
    #   shell(&block) => ShellTask
    #
    # This method returns the project's shell task. It also accepts a block to be executed
    # when the shell task is invoked.
    def shell(&block)
      task('shell').tap do |t|
        t.enhance &block if block
      end
    end
  end

  class Project
    include Shell
  end
end
