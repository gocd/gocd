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

  # Mixin for test frameworks using src/spec/{lang}
  class TestFramework::JavaBDD < TestFramework::Java #:nodoc:

    class << self
      attr_reader :lang, :bdd_dir
    end
    attr_accessor :lang, :bdd_dir

    def initialize(task, options)
      self.bdd_dir = self.class.bdd_dir
      project = task.project
      project.task('test:compile').tap do |comp|
        comp.send :associate_with, project, bdd_dir
        self.lang = comp.language || self.class.lang
      end
      project.task('test:resources').tap do |res|
        res.send :associate_with, project, bdd_dir
        res.filter.clear
        project.path_to(:source, bdd_dir, :resources).tap { |dir| res.from dir if File.exist?(dir) }
      end
      super
    end

  end

  module TestFramework::JRubyBased
    extend self

    VERSION = '1.6.2'

    class << self
      def version
        Buildr.settings.build['jruby'] || VERSION
      end

      def jruby_artifact
        "org.jruby:jruby-complete:jar:#{version}"
      end

      def dependencies
        unless @dependencies
          @dependencies = [jruby_artifact]
        end
        @dependencies
      end

      def included(mod)
        mod.extend ClassMethods
        super
      end
    end

    module ClassMethods
      def dependencies
        unless @dependencies
          super
          if !RUBY_PLATFORM[/java/] && !TestFramework::JRubyBased.jruby_installed?
            @dependencies |= TestFramework::JRubyBased.dependencies
          end
        end
        @dependencies
      end
    end

    def run(tests, dependencies)
      maybe_install_jruby
      dependencies |= [task.compile.target.to_s]

      spec_dir = task.project.path_to(:source, :spec, :ruby)
      report_dir = task.report_to.to_s
      rm_rf report_dir
      mkdir_p report_dir
      ENV['CI_REPORTS'] = report_dir

      runner = runner_config
      runner.content = runner_content(binding)

      Buildr.write(runner.file, runner.content)
      rm_f runner.result

      if RUBY_PLATFORM[/java/] && !options.fork
        runtime = new_runtime
        runtime.getObject.java.lang.System.getProperties().putAll(options[:properties] || {})
        runtime.getLoadService.require runner.file
      else
        cmd_options = task.options.only(:properties, :java_args)
        cmd_options.update(:classpath => dependencies, :project => task.project)
        jruby runner.file, tests, cmd_options rescue nil
      end

      fail "Missing result YAML file: #{runner.result}" unless File.exist? runner.result

      result = YAML.load(File.read(runner.result))
      if Exception === result
        raise [result.message, result.backtrace].flatten.join("\n")
      end
      tests - result.failed
    end

    def jruby_home
      @jruby_home ||= RUBY_PLATFORM =~ /java/ ? RbConfig::CONFIG['prefix'] :
        ( ENV['JRUBY_HOME'] || File.expand_path('~/.jruby') )
    end

    def jruby_installed?
      !Dir.glob(File.join(jruby_home, 'lib', 'jruby*.jar')).empty?
    end

  protected
    def maybe_install_jruby
      unless jruby_installed?
        jruby_artifact = Buildr.artifact(TestFramework::JRubyBased.jruby_artifact)
        msg = "JRUBY_HOME is not correctly set or points to an invalid JRuby installation: #{jruby_home}"
        say msg
        say ''
        say "You need to install JRuby version #{jruby_artifact.version} using your system package manager."

        fail msg unless jruby_installed?
      end
    end

    def jruby(*args)
      java_args = ['org.jruby.Main', *args]
      java_args << {} unless Hash === args.last
      cmd_options = java_args.last
      project = cmd_options.delete(:project)
      cmd_options[:classpath] ||= []
      if jruby_home && jruby_home != ''
        Dir.glob(File.join(jruby_home, 'lib', '*.jar')) { |jar| cmd_options[:classpath] << jar }
        cmd_options[:properties]['jruby.home'] = jruby_home
      end
      cmd_options[:java_args] ||= []
      cmd_options[:java_args] << '-Xmx512m' unless cmd_options[:java_args].detect {|a| a =~ /^-Xmx/}
      cmd_options[:properties] ||= {}
      Java::Commands.java(*java_args)
    end

    def new_runtime(cfg = {})
      config = Java.org.jruby.RubyInstanceConfig.new
      cfg.each_pair do |name, value|
        config.send("#{name}=", value)
      end
      yield config if block_given?
      Java.org.jruby.Ruby.newInstance config
    end

    def jruby_gem
      %{
       require 'jruby'
       def JRuby.gem(name, version = '>0', *args)
          require 'rbconfig'
          jruby_home = RbConfig::CONFIG['prefix']
          expected_version = '#{TestFramework::JRubyBased.version}'
          unless JRUBY_VERSION >= expected_version
            fail "Expected JRuby version \#{expected_version} installed at \#{jruby_home} but got \#{JRUBY_VERSION}"
          end
          require 'rubygems'
          begin
            Kernel.send :gem, name, version
          rescue LoadError, Gem::LoadError => e
            require 'rubygems/gem_runner'
            args = ['install', name, '--version', version] + args
            Gem::GemRunner.new.run(args)
            Kernel.send :gem, name, version
          end
       end
      }
    end

    def runner_config(runner = OpenStruct.new)
      [:requires, :gems, :output, :format].each do |key|
        runner.send("#{key}=", options[key])
      end
      runner.html_report ||= File.join(task.report_to.to_s, 'report.html')
      runner.result ||= task.project.path_to(:target, :spec, 'result.yaml')
      runner.file ||= task.project.path_to(:target, :spec, 'runner.rb')
      runner.requires ||= []
      runner.requires.unshift File.join(File.dirname(__FILE__), 'test_result')
      runner.gems ||= {}
      runner.rspec ||= ['--format', 'progress', '--format', 'documentation']
      runner.format.each { |format| runner.rspec << '--format' << format } if runner.format
      runner.rspec.push '--format', "Buildr::TestFramework::TestResult::YamlFormatter"
      runner.rspec.push '-o', runner.result
      runner
    end

  end

  # <a href="http://rspec.info">RSpec</a> is the defacto BDD framework for ruby.
  # To test your project with RSpec use:
  #   test.using :rspec
  #
  #
  # Support the following options:
  # * :gems       -- A hash of gems to install before running the tests.
  #                  The keys of this hash are the gem name, the value must be the required version.
  # * :requires   -- A list of ruby files to require before running the specs
  #                  Mainly used if an rspec format needs to require some file.
  # * :format     -- A list of valid Rspec --format option values. (defaults to 'progress')
  # * :output     -- File path to output dump. @false@ to suppress output
  # * :fork       -- Create a new JavaVM to run the tests on
  # * :properties -- Hash of properties passed to the test suite.
  # * :java_args  -- Arguments passed to the JVM.
  class RSpec < TestFramework::JavaBDD
    @lang = :ruby
    @bdd_dir = :spec

    include TestFramework::JRubyBased

    TESTS_PATTERN = [ /_spec.rb$/ ]
    OPTIONS = [:properties, :java_args]

    def self.applies_to?(project) #:nodoc:
      !Dir[project.path_to(:source, bdd_dir, lang, '**/*_spec.rb')].empty?
    end

    def tests(dependencies) #:nodoc:
      Dir[task.project.path_to(:source, bdd_dir, lang, '**/*_spec.rb')].select do |name|
        selector = ENV['SPEC']
        selector.nil? || Regexp.new(selector) === name
      end
    end

    def runner_config
      runner = super
      runner.gems.update 'rspec' => '~> 2.9.0'
      runner.requires.unshift 'rspec'
      runner
    end

    def runner_content(binding)
      runner_erb = %q{
        <%= jruby_gem %>
        <%= dependencies.inspect %>.each { |dep| $CLASSPATH << dep }
        <%= runner.gems.inspect %>.each { |ary| JRuby.gem(*ary.flatten) }
        <%= runner.requires.inspect %>.each { |rb| Kernel.require rb }
        <% if runner.output == false %>
          output = StringIO.new
        <% elsif runner.output.kind_of?(String) %>
          output = File.open(<%= result.output.inspect %>, 'w')
        <% else %>
          output = STDOUT
        <% end %>
        parser = ::RSpec::Core::Parser.new
        argv = <%= runner.rspec.inspect %> || []
        argv.push *<%= tests.inspect %>

        Buildr::TestFramework::TestResult::Error.guard('<%= runner.result %>') do
          ::RSpec::Core::CommandLine.new(argv).run(output, output)
        end
        exit 0 # let buildr figure the result from the yaml file
      }
      Filter::Mapper.new(:erb, binding).transform(runner_erb)
    end

  end

  # JBehave is a Java BDD framework. To use in your project:
  #   test.using :jbehave
  #
  # This framework will search in your project for:
  #   src/spec/java/**/*Behaviour.java
  #
  # JMock libraries are included on runtime.
  #
  # Support the following options:
  # * :properties -- Hash of properties to the test suite
  # * :java_args -- Arguments passed to the JVM
  class JBehave < TestFramework::JavaBDD
    @lang = :java
    @bdd_dir = :spec

    VERSION = '1.0.1'
    TESTS_PATTERN = [ /Behaviou?r$/ ] #:nodoc:

    class << self
      def version
        Buildr.settings.build['jbehave'] || VERSION
      end

      def dependencies
        unless @dependencies
          super
          @dependencies |= ["org.jbehave:jbehave:jar:#{version}", 'cglib:cglib-full:jar:2.0.2'] +
                           JMock.dependencies + JUnit.dependencies
        end
        @dependencies
      end

      def applies_to?(project) #:nodoc:
        %w{
          **/*Behaviour.java **/*Behavior.java
        }.any? { |glob| !Dir[project.path_to(:source, bdd_dir, lang, glob)].empty? }
      end

    private
      def const_missing(const)
        return super unless const == :REQUIRES # TODO: remove in 1.5
        Buildr.application.deprecated 'Please use JBehave.dependencies/.version instead of JBehave::REQUIRES/VERSION'
        dependencies
      end
    end

    def tests(dependencies) #:nodoc:
      filter_classes(dependencies, :class_names => TESTS_PATTERN,
                     :interfaces => %w{ org.jbehave.core.behaviour.Behaviours })
    end

    def run(tests, dependencies) #:nodoc:
      cmd_args = ['org.jbehave.core.BehaviourRunner']
      cmd_options = { :properties=>options[:properties], :java_args=>options[:java_args], :classpath=>dependencies }
      tests.inject([]) do |passed, test|
        begin
          Java::Commands.java cmd_args, test, cmd_options
          passed << test
        rescue
          passed
        end
      end
    end

  end

end

Buildr::TestFramework << Buildr::RSpec
Buildr::TestFramework << Buildr::JBehave

