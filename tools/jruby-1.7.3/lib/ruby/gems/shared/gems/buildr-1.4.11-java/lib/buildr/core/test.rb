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

  # The underlying test framework used by TestTask.
  # To add a new test framework, extend TestFramework::Base and add your framework using:
  #   Buildr::TestFramework << MyFramework
  module TestFramework

    class << self

      # Returns true if the specified test framework exists.
      def has?(name)
        frameworks.any? { |framework| framework.to_sym == name.to_sym }
      end

      # Select a test framework by its name.
      def select(name)
        frameworks.detect { |framework| framework.to_sym == name.to_sym }
      end

      # Identify which test framework applies for this project.
      def select_from(project)
        # Look for a suitable test framework based on the compiled language,
        # which may return multiple candidates, e.g. JUnit and TestNG for Java.
        # Pick the one used in the parent project, if not, whichever comes first.
        candidates = frameworks.select { |framework| framework.applies_to?(project) }
        parent = project.parent
        parent && candidates.detect { |framework| framework.to_sym == parent.test.framework } || candidates.first
      end

      # Adds a test framework to the list of supported frameworks.
      #
      # For example:
      #   Buildr::TestFramework << Buildr::JUnit
      def add(framework)
        @frameworks ||= []
        @frameworks |= [framework]
      end
      alias :<< :add

      # Returns a list of available test frameworks.
      def frameworks
        @frameworks ||= []
      end

    end

    # Base class for all test frameworks, with common functionality.  Extend and over-ride as you see fit
    # (see JUnit as an example).
    class Base

      class << self

        # The framework's identifier (e.g. :junit).  Inferred from the class name.
        def to_sym
          @symbol ||= name.split('::').last.downcase.to_sym
        end

        # Returns true if this framework applies to the current project.  For example, JUnit returns
        # true if the tests are written in Java.
        def applies_to?(project)
          raise 'Not implemented'
        end

        # Returns a list of dependencies for this framework.  Default is an empty list,
        # override to add dependencies.
        def dependencies
          @dependencies ||= []
        end

      end

      # Construct a new test framework with the specified options.  Note that options may
      # change before the framework is run.
      def initialize(test_task, options)
        @options = options
        @task = test_task
      end

      # Options for this test framework.
      attr_reader :options
      # The test task we belong to
      attr_reader :task

      # Returns a list of dependenices for this framework.  Defaults to calling the #dependencies
      # method on the class.
      def dependencies
        self.class.dependencies
      end

      # TestTask calls this method to return a list of test names that can be run in this project.
      # It then applies the include/exclude patterns to arrive at the list of tests that will be
      # run, and call the #run method with that list.
      #
      # This method should return a list suitable for using with the #run method, but also suitable
      # for the user to manage.  For example, JUnit locates all the tests in the test.compile.target
      # directory, and returns the class names, which are easier to work with than file names.
      def tests(dependencies)
        raise 'Not implemented'
      end

      # TestTask calls this method to run the named (and only those) tests.  This method returns
      # the list of tests that ran successfully.
      def run(tests, dependencies)
        raise 'Not implemented'
      end

    end

  end


  # The test task controls the entire test lifecycle.
  #
  # You can use the test task in three ways. You can access and configure specific test tasks,
  # e.g. enhance the #compile task, or run code during #setup/#teardown.
  #
  # You can use convenient methods that handle the most common settings. For example,
  # add dependencies using #with, or include only specific tests using #include.
  #
  # You can also enhance this task directly. This task will first execute the #compile task, followed
  # by the #setup task, run the unit tests, any other enhancements, and end by executing #teardown.
  #
  # The test framework is determined based on the available test files, for example, if the test
  # cases are written in Java, then JUnit is selected as the test framework.  You can also select
  # a specific test framework, for example, to use TestNG instead of JUnit:
  #   test.using :testng
  class TestTask < ::Rake::Task

    class << self

      # Used by the local test and integration tasks to
      # a) Find the local project(s),
      # b) Find all its sub-projects and narrow down to those that have either unit or integration tests,
      # c) Run all the (either unit or integration) tests, and
      # d) Ignore failure if necessary.
      def run_local_tests(integration) #:nodoc:
        Project.local_projects do |project|
          # !(foo ^ bar) tests for equality and accepts nil as false (and select is less obfuscated than reject on ^).
          projects = ([project] + project.projects).select { |project| !(project.test.options[:integration] ^ integration) }
          projects.each do |project|
            info "Testing #{project.name}"
            begin
              project.test.invoke
            rescue
              raise unless Buildr.options.test == :all
            end
          end
        end
      end

      # Used by the test/integration rule to only run tests that match the specified names.
      def only_run(tests) #:nodoc:
        tests = wildcardify(tests)
        # Since the tests may reside in a sub-project, we need to set the include/exclude pattern on
        # all sub-projects, but only invoke test on the local project.
        Project.projects.each { |project| project.test.send :only_run, tests }
      end

      # Used by the test/integration rule to only run tests that failed the last time.
      def only_run_failed() #:nodoc:
        # Since the tests may reside in a sub-project, we need to set the include/exclude pattern on
        # all sub-projects, but only invoke test on the local project.
        Project.projects.each { |project| project.test.send :only_run_failed }
      end

      # Used by the test/integration rule to clear all previously included/excluded tests.
      def clear()
        Project.projects.each do |project|
          project.test.send :clear
        end
      end

      # Used by the test/integration to include specific tests
      def include(includes)
        includes = wildcardify(Array(includes))
        Project.projects.each do |project|
          project.test.send :include, *includes if includes.size > 0
          project.test.send :forced_need=, true
        end
      end

      # Used by the test/integration to exclude specific tests
      def exclude(excludes)
        excludes = wildcardify(Array(excludes))
        Project.projects.each do |project|
          project.test.send :exclude, *excludes if excludes.size > 0
          project.test.send :forced_need=, true
        end
      end

    private

      def wildcardify(strings)
        strings.map { |name| name =~ /\*/ ? name : "*#{name}*" }
      end
    end

    # Default options already set on each test task.
    def default_options
      { :fail_on_failure=>true, :fork=>:once, :properties=>{}, :environment=>{} }
    end

    def initialize(*args) #:nodoc:
      super
      @dependencies = FileList[]
      @include = []
      @exclude = []
      @forced_need = false
      parent_task = Project.parent_task(name)
      if parent_task.respond_to?(:options)
        @options = OpenObject.new { |hash, key| hash[key] = parent_task.options[key].clone rescue hash[key] = parent_task.options[key] }
      else
        @options = OpenObject.new(default_options)
      end

      unless ENV["IGNORE_BUILDFILE"] =~ /(true)|(yes)/i
        enhance [ application.buildfile.name ]
        enhance application.buildfile.prerequisites
      end
      enhance do
        run_tests if framework
      end
    end

    # The dependencies used for running the tests. Includes the compiled files (compile.target)
    # and their dependencies. Will also include anything you pass to #with, shared between the
    # testing compile and run dependencies.
    attr_accessor :dependencies

    # *Deprecated*: Use dependencies instead.
    def classpath
      Buildr.application.deprecated 'Use dependencies instead.'
      @dependencies
    end

    # *Deprecated*: Use dependencies= instead.
    def classpath=(artifacts)
      Buildr.application.deprecated 'Use dependencies= instead.'
      @dependencies = artifacts
    end

    def execute(args) #:nodoc:
      if Buildr.options.test == false
        info "Skipping tests for #{project.name}"
        return
      end
      setup.invoke
      begin
        super
      rescue RuntimeError
        raise if options[:fail_on_failure] && Buildr.options.test != :all
      ensure
        teardown.invoke
      end
    end

    # :call-seq:
    #   compile(*sources) => CompileTask
    #   compile(*sources) { |task| .. } => CompileTask
    #
    # The compile task is similar to the Project's compile task. However, it compiles all
    # files found in the src/test/{source} directory into the target/test/{code} directory.
    # This task is executed by the test task before running any tests.
    #
    # Once the project definition is complete, all dependencies from the regular
    # compile task are copied over, so you only need to specify dependencies
    # specific to your tests. You can do so by calling #with on the test task.
    # The dependencies used here are also copied over to the junit task.
    def compile(*sources, &block)
      @project.task('test:compile').from(sources).enhance &block
    end

    # :call-seq:
    #   resources(*prereqs) => ResourcesTask
    #   resources(*prereqs) { |task| .. } => ResourcesTask
    #
    # Executes by the #compile task to copy resource files over. See Project#resources.
    def resources(*prereqs, &block)
      @project.task('test:resources').enhance prereqs, &block
    end

    # :call-seq:
    #   setup(*prereqs) => task
    #   setup(*prereqs) { |task| .. } => task
    #
    # Returns the setup task. The setup task is executed at the beginning of the test task,
    # after compiling the test files.
    def setup(*prereqs, &block)
      @project.task('test:setup').enhance prereqs, &block
    end

    # :call-seq:
    #   teardown(*prereqs) => task
    #   teardown(*prereqs) { |task| .. } => task
    #
    # Returns the teardown task. The teardown task is executed at the end of the test task.
    def teardown(*prereqs, &block)
      @project.task('test:teardown').enhance prereqs, &block
    end

    # :call-seq:
    #   with(*specs) => self
    #
    # Specify artifacts (specs, tasks, files, etc) to include in the dependencies list
    # when compiling and running tests.
    def with(*artifacts)
      @dependencies |= Buildr.artifacts(artifacts.flatten).uniq
      compile.with artifacts
      self
    end

    # Returns various test options.
    attr_reader :options

    # :call-seq:
    #   using(options) => self
    #
    # Sets various test options from a hash and returns self.  For example:
    #   test.using :fork=>:each, :properties=>{ 'url'=>'http://localhost:8080' }
    #
    # Can also be used to select the test framework, or to run these tests as
    # integration tests.  For example:
    #   test.using :testng
    #   test.using :integration
    #
    # The :fail_on_failure option specifies whether the task should fail if
    # any of the tests fail (default), or should report the failures but continue
    # running the build (when set to false).
    #
    # All other options depend on the capability of the test framework.  These options
    # should be used the same way across all frameworks that support them:
    # * :fork -- Fork once for each project (:once, default), for each test in each
    #     project (:each), or don't fork at all (false).
    # * :properties -- Properties pass to the test, e.g. in Java as system properties.
    # * :environment -- Environment variables.  This hash is made available in the
    #     form of environment variables.
    def using(*args)
      args.pop.each { |key, value| options[key.to_sym] = value } if Hash === args.last
      args.each do |name|
        if TestFramework.has?(name)
          self.framework = name
        elsif name == :integration
          options[:integration] = true
        else
          Buildr.application.deprecated "Please replace with using(:#{name}=>true)"
          options[name.to_sym] = true
        end
      end
      self
    end

    # :call-seq:
    #   include(*names) => self
    #
    # Include only the specified tests. Unless specified, the default is to include
    # all tests identified by the test framework. This method accepts multiple arguments
    # and returns self.
    #
    # Tests are specified by their full name, but you can use glob patterns to select
    # multiple tests, for example:
    #   test.include 'com.example.FirstTest'  # FirstTest only
    #   test.include 'com.example.*'          # All tests under com/example
    #   test.include 'com.example.Module*'    # All tests starting with Module
    #   test.include '*.{First,Second}Test'   # FirstTest, SecondTest
    def include(*names)
      @include += names
      self
    end

    # :call-seq:
    #   exclude(*names) => self
    #
    # Exclude the specified tests. This method accepts multiple arguments and returns self.
    # See #include for the type of arguments you can use.
    def exclude(*names)
      @exclude += names
      self
    end

    # Clear all test includes and excludes and returns self
    def clear
      @include = []
      @exclude = []
      self
    end

    # *Deprecated*: Use tests instead.
    def classes
      Buildr.application.deprecated 'Call tests instead of classes'
      tests
    end

    # After running the task, returns all tests selected to run, based on availability and include/exclude pattern.
    attr_reader :tests
    # After running the task, returns all the tests that failed, empty array if all tests passed.
    attr_reader :failed_tests
    # After running the task, returns all the tests that passed, empty array if no tests passed.
    attr_reader :passed_tests

    # :call-seq:
    #   framework => symbol
    #
    # Returns the test framework, e.g. :junit, :testng.
    def framework
      unless @framework
        # Start with all frameworks that apply (e.g. JUnit and TestNG for Java),
        # and pick the first (default) one, unless already specified in parent project.
        candidates = TestFramework.frameworks.select { |cls| cls.applies_to?(@project) }
        candidate = @project.parent && candidates.detect { |framework| framework.to_sym == @project.parent.test.framework } ||
          candidates.first
        self.framework = candidate if candidate
      end
      @framework && @framework.class.to_sym
    end

    # :call-seq:
    #   report_to => file
    #
    # Test frameworks that can produce reports, will write them to this directory.
    #
    # This is framework dependent, so unless you use the default test framework, call this method
    # after setting the test framework.
    def report_to
      @report_to ||= file(@project.path_to(:reports, framework)=>self)
    end

    # :call-seq:
    #   failures_to => file
    #
    # We record the list of failed tests for the current framework in this file.
    #
    #
    def failures_to
      @failures_to ||= file(@project.path_to(:target, "#{framework}-failed")=>self)
    end

    # :call-seq:
    #    last_failures => array
    #
    # We read the last test failures if any and return them.
    #
    def last_failures
      @last_failures ||= failures_to.exist? ? File.read(failures_to.to_s).split("\n") : []
    end

    # The path to the file that stores the time stamp of the last successful test run.
    def last_successful_run_file #:nodoc:
      File.join(report_to.to_s, 'last_successful_run')
    end

    # The time stamp of the last successful test run.  Or Rake::EARLY if no successful test run recorded.
    def timestamp #:nodoc:
      File.exist?(last_successful_run_file) ? File.mtime(last_successful_run_file) : Rake::EARLY
    end

    # The project this task belongs to.
    attr_reader :project

    # Whether the tests are forced
    attr_accessor :forced_need

  protected

    def associate_with(project)
      @project = project
    end

    def framework=(name)
      cls = TestFramework.select(name) or raise ArgumentError, "No #{name} test framework available. Did you install it?"
      #cls.inherit_options.reject { |name| options.has_key?(name) }.
      #  each { |name| options[name] = @parent_task.options[name] } if @parent_task.respond_to?(:options)
      @framework = cls.new(self, options)
      # Test framework dependency.
      with @framework.dependencies
    end

    # :call-seq:
    #   include?(name) => boolean
    #
    # Returns true if the specified test name matches the inclusion/exclusion pattern. Used to determine
    # which tests to execute.
    def include?(name)
      ((@include.empty? && !@forced_need)|| @include.any? { |pattern| File.fnmatch(pattern, name) }) &&
        !@exclude.any? { |pattern| File.fnmatch(pattern, name) }
    end

    # Runs the tests using the selected test framework.
    def run_tests
      dependencies = (Buildr.artifacts(self.dependencies + compile.dependencies) + [compile.target]).map(&:to_s).uniq
      rm_rf report_to.to_s
      rm_rf failures_to.to_s
      @tests = @framework.tests(dependencies).select { |test| include?(test) }.sort
      if @tests.empty?
        @passed_tests, @failed_tests = [], []
      else
        info "Running tests in #{@project.name}"
        begin
          # set the baseDir system property if not set
          @framework.options[:properties] = { 'baseDir' => compile.target.to_s }.merge(@framework.options[:properties] || {})
          @passed_tests = @framework.run(@tests, dependencies)
        rescue Exception=>ex
          error "Test framework error: #{ex.message}"
          error ex.backtrace.join("\n") if trace?
          @passed_tests = []
        end
        @failed_tests = @tests - @passed_tests
        unless @failed_tests.empty?
          Buildr::write(failures_to.to_s, @failed_tests.join("\n"))
          error "The following tests failed:\n#{@failed_tests.join("\n")}"
          fail 'Tests failed!'
        end
      end
      record_successful_run unless @forced_need
    end

    # Call this method when a test run is successful to record the current system time.
    def record_successful_run #:nodoc:
      mkdir_p report_to.to_s
      touch last_successful_run_file
    end

    # Limit running tests to specific list.
    def only_run(tests)
      @include = Array(tests)
      @exclude.clear
      @forced_need = true
    end

    # Limit running tests to those who failed the last time.
    def only_run_failed()
      @include = Array(last_failures)
      @forced_need = true
    end

    def invoke_prerequisites(args, chain) #:nodoc:
      @prerequisites |= FileList[@dependencies.uniq]
      super
    end

    def needed? #:nodoc:
      latest_prerequisite = @prerequisites.map { |p| application[p, @scope] }.max { |a,b| a.timestamp<=>b.timestamp }
      needed = (timestamp == Rake::EARLY) || latest_prerequisite.timestamp > timestamp
      trace "Testing#{needed ? ' ' : ' not '}needed. " +
        "Latest prerequisite change: #{latest_prerequisite.timestamp} (#{latest_prerequisite.to_s}). " +
        "Last successful test run: #{timestamp}."
      return needed || @forced_need || Buildr.options.test == :all
    end
  end


  # The integration tests task. Buildr has one such task (see Buildr#integration) that runs
  # all tests marked with :integration=>true, and has a setup/teardown tasks separate from
  # the unit tests.
  class IntegrationTestsTask < Rake::Task

    def initialize(*args) #:nodoc:
      super
      @setup = task("#{name}:setup")
      @teardown = task("#{name}:teardown")
      enhance do
        info 'Running integration tests...'
        TestTask.run_local_tests true
      end
    end

    def execute(args) #:nodoc:
      setup.invoke
      begin
        super
      ensure
        teardown.invoke
      end
    end

    # :call-seq:
    #   setup(*prereqs) => task
    #   setup(*prereqs) { |task| .. } => task
    #
    # Returns the setup task. The setup task is executed before running the integration tests.
    def setup(*prereqs, &block)
      @setup.enhance prereqs, &block
    end

    # :call-seq:
    #   teardown(*prereqs) => task
    #   teardown(*prereqs) { |task| .. } => task
    #
    # Returns the teardown task. The teardown task is executed after running the integration tests.
    def teardown(*prereqs, &block)
      @teardown.enhance prereqs, &block
    end

  end


  # Methods added to Project to support compilation and running of tests.
  module Test

    include Extension

    first_time do
      desc 'Run all tests'
      task('test') { TestTask.run_local_tests false }

      desc 'Run failed tests'
      task('test:failed') {
        TestTask.only_run_failed
        task('test').invoke
      }

      # This rule takes a suffix and runs that tests in the current project. For example;
      #   buildr test:MyTest
      # will run the test com.example.MyTest, if such a test exists for this project.
      #
      # If you want to run multiple test, separate them with a comma. You can also use glob
      # (* and ?) patterns to match multiple tests, see the TestTask#include method.
      rule /^test:.*$/ do |task|
        # The map works around a JRuby bug whereby the string looks fine, but fails in fnmatch.
        tests = task.name.scan(/test:(.*)/)[0][0].split(',').map(&:to_s)
        excludes, includes = tests.partition { |t| t =~ /^-/ }
        if excludes.empty?
          TestTask.only_run includes
        else
          # remove leading '-'
          excludes.map! { |t| t[1..-1] }

          TestTask.clear
          TestTask.include(includes.empty? ? ['*'] : includes)
          TestTask.exclude excludes
        end
        task('test').invoke
      end

      IntegrationTestsTask.define_task('integration')

      # Similar to test:[pattern] but for integration tests.
      rule /^integration:.*$/ do |task|
        unless task.name.split(':')[1] =~ /^(setup|teardown)$/
          # The map works around a JRuby bug whereby the string looks fine, but fails in fnmatch.
          TestTask.only_run task.name[/integration:(.*)/, 1].split(',').map { |t| "#{t}" }
          task('integration').invoke
        end
      end

    end

    before_define(:test) do |project|
      # Define a recursive test task, and pass it a reference to the project so it can discover all other tasks.
      test = TestTask.define_task('test')
      test.send :associate_with, project

      # Similar to the regular resources task but using different paths.
      resources = ResourcesTask.define_task('test:resources')
      resources.send :associate_with, project, :test
      project.path_to(:source, :test, :resources).tap { |dir| resources.from dir if File.exist?(dir) }

      # We define a module inline that will inject cancelling the task if tests are skipped.
      module SkipIfNoTest

        def self.extended(base)
          base.instance_eval {alias :execute_before_skip_if_no_test :execute}
          base.instance_eval {alias :execute :execute_after_skip_if_no_test}
        end

        def execute_after_skip_if_no_test(args) #:nodoc:
          if Buildr.options.test == false
            trace "Skipping #{to_s} for #{project.name} as tests are skipped"
            return
          end
          execute_before_skip_if_no_test(args)
        end
      end

      # Similar to the regular compile task but using different paths.
      compile = CompileTask.define_task('test:compile'=>[project.compile, resources])
      compile.extend SkipIfNoTest
      compile.send :associate_with, project, :test
      test.enhance [compile]

      # Define these tasks once, otherwise we may get a namespace error.
      test.setup ; test.teardown
    end



    after_define(:test => :compile) do |project|
      test = project.test
      # Dependency on compiled tests and resources.  Dependencies added using with.
      test.dependencies.concat [test.compile.target, test.resources.target].compact
      test.dependencies.concat test.compile.dependencies
      # Dependency on compiled code, its dependencies and resources.
      test.with [project.compile.target, project.resources.target].compact
      test.with project.compile.dependencies
      # Picking up the test frameworks adds further dependencies.
      test.framework

      project.build test unless test.options[:integration] || Buildr.options.test == :only

      project.clean do
        rm_rf test.compile.target.to_s if test.compile.target
        rm_rf test.report_to.to_s
      end
    end


    # :call-seq:
    #   test(*prereqs) => TestTask
    #   test(*prereqs) { |task| .. } => TestTask
    #
    # Returns the test task. The test task controls the entire test lifecycle.
    #
    # You can use the test task in three ways. You can access and configure specific
    # test tasks, e.g. enhance the compile task by calling test.compile, setup for
    # the tests by enhancing test.setup and so forth.
    #
    # You can use convenient methods that handle the most common settings. For example,
    # add dependencies using test.with, or include only specific tests using test.include.
    #
    # You can also enhance this task directly. This method accepts a list of arguments
    # that are used as prerequisites and an optional block that will be executed by the
    # test task.
    #
    # This task compiles the project and the tests (in that order) before running any tests.
    # It execute the setup task, runs all the tests, any enhancements, and ends with the
    # teardown tasks.
    def test(*prereqs, &block)
      task('test').enhance prereqs, &block
    end

    # :call-seq:
    #   integration { |task| .... }
    #   integration => IntegrationTestTask
    #
    # Use this method to return the integration tests task, or enhance it with a block to execute.
    #
    # There is one integration tests task you can execute directly, or as a result of running the package
    # task (or tasks that depend on it, like install and upload). It contains all the tests marked with
    # :integration=>true, all other tests are considered unit tests and run by the test task before packaging.
    # So essentially: build=>test=>packaging=>integration=>install/upload.
    #
    # You add new tests from projects that define integration tests using the regular test task,
    # but with the following addition:
    #   test.using :integration
    #
    # Use this method to enhance the setup and teardown tasks that are executed before (and after) all
    # integration tests are run, for example, to start a Web server or create a database.
    def integration(*deps, &block)
      Rake::Task['rake:integration'].enhance deps, &block
    end

  end


  # :call-seq:
  #   integration { |task| .... }
  #   integration => IntegrationTestTask
  #
  # Use this method to return the integration tests task.
  def integration(*deps, &block)
    Rake::Task['rake:integration'].enhance deps, &block
  end

  class Options

    # Runs tests after the build when true (default). This forces tests to execute
    # after the build, including when running build related tasks like install, upload and release.
    #
    # Set to false to not run any tests. Set to :all to run all tests, ignoring failures.
    #
    # This option is set from the environment variable 'test', so you can also do:

    # Returns the test option (environment variable TEST). Possible values are:
    # * :false -- Do not run any tests (also accepts 'no' and 'skip').
    # * :true -- Run all tests, stop on failure (default if not set).
    # * :all -- Run all tests, ignore failures.
    def test
      case value = ENV['TEST'] || ENV['test']
      when /^(no|off|false|skip)$/i
        false
      when /^all$/i
        :all
      when /^only$/i
        :only
      when /^(yes|on|true)$/i, nil
        true
      else
        warn "Expecting the environment variable test to be 'no' or 'all', not sure what to do with #{value}, so I'm just going to run all the tests and stop at failure."
        true
      end
    end

    # Sets the test option (environment variable TEST). Possible values are true, false or :all.
    #
    # You can also set this from the environment variable, e.g.:
    #
    #   buildr          # With tests
    #   buildr test=no  # Without tests
    #   buildr test=all # Ignore failures
    #   set TEST=no
    #   buildr          # Without tests
    def test=(flag)
      ENV['test'] = nil
      ENV['TEST'] = flag.to_s
    end

  end

  Buildr.help << <<-HELP
To run a full build without running any tests:
  buildr test=no
To run specific test:
  buildr test:MyTest
To run integration tests:
  buildr integration
    HELP

end


class Buildr::Project
  include Buildr::Test
end
