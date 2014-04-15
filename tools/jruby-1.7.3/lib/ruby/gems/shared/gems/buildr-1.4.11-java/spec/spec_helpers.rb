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


# This file gets loaded twice when running 'spec spec/*' and not with pleasant results,
# so ignore the second attempt to load it.
unless defined?(SpecHelpers)

  require 'rubygems'

  # For testing we use the gem requirements specified on the buildr.gemspec
  spec = Gem::Specification.load(File.expand_path('../buildr.gemspec', File.dirname(__FILE__)))
  # Dependency.version_requirements deprecated in rubygems 1.3.6
  spec.dependencies.select {|dep| dep.type == :runtime }.each { |dep| gem dep.name, (dep.respond_to?(:requirement) ? dep.requirement.to_s : dep.version_requirements.to_s) }

  # Make sure to load from these paths first, we don't want to load any
  # code from Gem library.
  $LOAD_PATH.unshift File.expand_path('../lib', File.dirname(__FILE__)),
                     File.expand_path('../addon', File.dirname(__FILE__))

  # Buildr uses autoload extensively, but autoload when running specs creates
  # a problem -- we sandbox $LOADED_FEATURES, so we end up autoloading the same
  # module twice. This turns autoload into a require, which is not the right
  # thing, but will do for now.
  def autoload(symbol, path)
    require path
  end
  require 'buildr'
  # load ecj
  require 'buildr/java/ecj'
  #Make ecj appear as a compiler that doesn't apply:
  class Buildr::Compiler::Ecj
    class << self
      def applies_to?(project, task)
        false
      end
    end
  end

  # Give a chance for plugins to do a few things before requiring the sandbox.
  include SandboxHook if defined?(SandboxHook)

  require File.expand_path('sandbox', File.dirname(__FILE__))

  module SpecHelpers

    include Checks::Matchers

    [:info, :warn, :error, :puts].each do |severity|
      ::Object.class_eval do
        define_method severity do |*args|
          $messages ||= {}
          $messages[severity] ||= []
          $messages[severity].push(*args)
        end
      end
    end

    class << Buildr.application
      alias :deprecated_without_capture :deprecated
      def deprecated(message)
        verbose(true) { deprecated_without_capture message }
      end
    end

    class MessageWithSeverityMatcher
      def initialize(severity, message)
        @severity = severity
        @expect = message
      end

      def matches?(target)
        $messages = {@severity => []}
        target.call
        return Regexp === @expect ? $messages[@severity].join('\n') =~ @expect : $messages[@severity].include?(@expect.to_s)
      end

      def failure_message
        "Expected #{@severity} #{@expect.inspect}, " +
          ($messages[@severity].empty? ? "no #{@severity} issued" : "found #{$messages[@severity].inspect}")
      end

      def negative_failure_message
        "Found unexpected #{$messages[@severity].inspect}"
      end
    end

    # Test if an info message was shown.  You can use a string or regular expression.
    #
    # For example:
    #   lambda { info 'ze test' }.should show_info(/ze test/)
    def show_info(message)
      MessageWithSeverityMatcher.new :info, message
    end

    # Test if a warning was shown. You can use a string or regular expression.
    #
    # For example:
    #   lambda { warn 'ze test' }.should show_warning(/ze test/)
    def show_warning(message)
      MessageWithSeverityMatcher.new :warn, message
    end

    # Test if an error message was shown.  You can use a string or regular expression.
    #
    # For example:
    #   lambda { error 'ze test' }.should show_error(/ze test/)
    def show_error(message)
      MessageWithSeverityMatcher.new :error, message
    end

    # Test if any message was shown (puts).  You can use a string or regular expression.
    #
    # For example:
    #   lambda { puts 'ze test' }.should show(/ze test/)
    def show(message)
      MessageWithSeverityMatcher.new :puts, message
    end

    # Yields a block that should try exiting the application.
    # Accepts
    #
    # For example:
    #   test_exit(1) {  puts "Hello" ; exit(1) }.should show("Hello")
    #
    def test_exit(status = nil)
      return lambda {
        begin
          yield
          raise "Exit was not called!"
        rescue SystemExit => e
          raise "Exit status incorrect! Expected: #{status}, got #{e.status}" if status && (e.status != status)
        end
      }
    end

    class ::Rake::Task
      alias :execute_without_a_record :execute
      def execute(args)
        $executed ||= []
        $executed << name
        execute_without_a_record args
      end
    end

    class InvokeMatcher
      def initialize(*tasks)
        @expecting = tasks.map { |task| [task].flatten.map(&:to_s) }
      end

      def matches?(target)
        $executed = []
        target.call
        return false unless all_ran?
        return !@but_not.any_ran? if @but_not
        return true
      end

      def failure_message
        return @but_not.negative_failure_message if all_ran? && @but_not
        "Expected the tasks #{expected} to run, but #{remaining} did not run, or not in the order we expected them to." +
          "  Tasks that ran: #{$executed.inspect}"
      end

      def negative_failure_message
        if all_ran?
          "Expected the tasks #{expected} to not run, but they all ran."
        else
          "Expected the tasks #{expected} to not run, and all but #{remaining} ran."
        end
      end

      def but_not(*tasks)
        @but_not = InvokeMatcher.new(*tasks)
        self
      end

    protected

      def expected
        @expecting.map { |tests| tests.join('=>') }.join(', ')
      end

      def remaining
        @remaining.map { |tests| tests.join('=>') }.join(', ')
      end

      def all_ran?
        @remaining ||= $executed.inject(@expecting) do |expecting, executed|
          expecting.map { |tasks| tasks.first == executed ? tasks[1..-1] : tasks }.reject(&:empty?)
        end
        @remaining.empty?
      end

      def any_ran?
        all_ran?
        @remaining.size < @expecting.size
      end

    end

    # Tests that all the tasks ran, in the order specified. Can also be used to test that some
    # tasks and not others ran.
    #
    # Takes a list of arguments. Each argument can be a task name, matching only if that task ran.
    # Each argument can be an array of task names, matching only if all these tasks ran in that order.
    # So run_tasks('foo', 'bar') expects foo and bar to run in any order, but run_task(['foo', 'bar'])
    # expects foo to run before bar.
    #
    # You can call but_not on the matchers to specify that certain tasks must not execute.
    #
    # For example:
    #   # Either task
    #   lambda { task('compile').invoke }.should run_tasks('compile', 'resources')
    #   # In that order
    #   lambda { task('build').invoke }.should run_tasks(['compile', 'test'])
    #   # With exclusion
    #   lambda { task('build').invoke }.should run_tasks('compile').but_not('install')
    def run_tasks(*tasks)
      InvokeMatcher.new *tasks
    end

    # Tests that a task ran. Similar to run_tasks, but accepts a single task name.
    #
    # For example:
    #   lambda { task('build').invoke }.should run_task('test')
    def run_task(task)
      InvokeMatcher.new [task]
    end

    class UriPathMatcher
      def initialize(re)
        @expression = re
      end

      def matches?(uri)
        @uri = uri
        uri.path =~ @expression
      end

      def description
        "URI with path matching #{@expression} vs #{@uri}"
      end

      def failure_message_for_should
        "expected #{description}"
      end
    end

    # Matches a parsed URI's path against the given regular expression
    def uri(re)
      UriPathMatcher.new(re)
    end


    class AbsolutePathMatcher
      def initialize(path)
        @expected = File.expand_path(path.to_s)
      end

      def matches?(path)
        @provided = File.expand_path(path.to_s)
        @provided == @expected
      end

      def failure_message
        "Expected path #{@expected}, but found path #{@provided}"
      end

      def negative_failure_message
        "Expected a path other than #{@expected}"
      end
    end

    def point_to_path(path)
      AbsolutePathMatcher.new(path)
    end


    # Value covered by range. For example:
    #   (1..5).should cover(3)
    RSpec::Matchers.define :cover do |actual|
      match do |range|
        actual >= range.min && actual <= range.max
      end
    end


    def suppress_stdout
      stdout = $stdout
      $stdout = StringIO.new
      begin
        yield
      ensure
        $stdout = stdout
      end
    end

    def dryrun
      Buildr.application.options.dryrun = true
      begin
        suppress_stdout { yield }
      ensure
        Buildr.application.options.dryrun = false
      end
    end

    # We run tests with tracing off. Then things break. And we need to figure out what went wrong.
    # So just use trace() as you would use verbose() to find and squash the bug.
    def trace(value = nil)
      old_value = Buildr.application.options.trace
      Buildr.application.options.trace = value unless value.nil?
      if block_given?
        begin
          yield
        ensure
          Buildr.application.options.trace = old_value
        end
      end
      Buildr.application.options.trace
    end

    # Change the Buildr original directory, faking invocation from a different directory.
    def in_original_dir(dir)
      begin
        original_dir = Buildr.application.original_dir
        Buildr.application.instance_eval { @original_dir = File.expand_path(dir) }
        yield
      ensure
        Buildr.application.instance_eval { @original_dir = original_dir }
      end
    end


    # Buildr's define method creates a project definition but does not evaluate it
    # (that happens once the buildfile is loaded), and we include Buildr's define in
    # the test context so we can use it without prefixing with Buildr. This just patches
    # define to evaluate the project definition before returning it.
    def define(name, properties = nil, &block) #:yields:project
      Project.define(name, properties, &block).tap { |project| project.invoke }
    end

  end


  # Allow using matchers within the project definition.
  class Buildr::Project
    include ::RSpec::Matchers, SpecHelpers
  end


  ::RSpec.configure do |config|
    config.treat_symbols_as_metadata_keys_with_true_values = true
    # Make all Buildr methods accessible from test cases, and add various helper methods.
    config.include Buildr
    config.include SpecHelpers

    # Sandbox Buildr for each test.
    config.include Sandbox
  end

end
