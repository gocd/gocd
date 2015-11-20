
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

module Buildr #:nodoc:

  class TestFramework::Java < TestFramework::Base

    class << self

      def applies_to?(project) #:nodoc:
        project.test.compile.language == :java || project.test.compile.language == :groovy
      end

      def dependencies
        unless @dependencies
          super
          # Add buildr utility classes (e.g. JavaTestFilter)
          @dependencies |= [ File.join(File.dirname(__FILE__)) ]
        end
        @dependencies
      end
    end

  private

    # Add buildr utilities (JavaTestFilter) to classpath
    Java.classpath << lambda { dependencies }

    # :call-seq:
    #     filter_classes(dependencies, criteria)
    #
    # Return a list of classnames that match the given criteria.
    # The criteria parameter is a hash that must contain at least one of:
    #
    # * :class_names -- List of patterns to match against class name
    # * :interfaces -- List of java interfaces or java classes
    # * :class_annotations -- List of annotations on class level
    # * :method_annotations -- List of annotations on method level
    # * :fields -- List of java field names
    #
    def filter_classes(dependencies, criteria = {})
      return [] unless task.compile.target
      target = task.compile.target.to_s
      candidates = Dir["#{target}/**/*.class"].
        map { |file| Util.relative_path(file, target).ext('').gsub(File::SEPARATOR, '.') }.
        reject { |name| name =~ /\$./ }
      result = []
      if criteria[:class_names]
        result.concat candidates.select { |name| criteria[:class_names].flatten.any? { |pat| pat === name } }
      end
      begin
        Java.load
        filter = Java.org.apache.buildr.JavaTestFilter.new(dependencies.to_java(Java.java.lang.String))
        if criteria[:interfaces]
          filter.add_interfaces(criteria[:interfaces].to_java(Java.java.lang.String))
        end
        if criteria[:class_annotations]
          filter.add_class_annotations(criteria[:class_annotations].to_java(Java.java.lang.String))
        end
        if criteria[:method_annotations]
          filter.add_method_annotations(criteria[:method_annotations].to_java(Java.java.lang.String))
        end
        if criteria[:fields]
          filter.add_fields(criteria[:fields].to_java(Java.java.lang.String))
        end
        result.concat filter.filter(candidates.to_java(Java.java.lang.String)).map(&:to_s)
      rescue =>ex
        info "#{ex.class}: #{ex.message}"
        raise
      end
    end

  end


  # JMock is available when using JUnit and TestNG, JBehave.
  module JMock

    VERSION = '2.5.1'

    class << self
      def version
        Buildr.settings.build['jmock'] || VERSION
      end

      def dependencies(versions = {:hamcrest => "1.1"})
        two_or_later = version[0,1].to_i >= 2
        group = two_or_later ? "org.jmock" : "jmock"

        @dependencies ||= ["#{group}:jmock:jar:#{version}"]
        if two_or_later
          @dependencies << "org.jmock:jmock-junit#{Buildr::JUnit.version.to_s[0,1]}:jar:#{version}"
          @dependencies << "org.hamcrest:hamcrest-core:jar:#{versions[:hamcrest]}"
          @dependencies << "org.hamcrest:hamcrest-library:jar:#{versions[:hamcrest]}"
        end
        @dependencies
      end

    private
      def const_missing(const)
        return super unless const == :REQUIRES # TODO: remove in 1.5
        Buildr.application.deprecated "Please use JMock.dependencies/.version instead of JMock::REQUIRES/VERSION"
        dependencies
      end
    end
  end


  # JUnit test framework, the default test framework for Java tests.
  #
  # Support the following options:
  # * :fork        -- If true/:once (default), fork for each test class.  If :each, fork for each individual
  #                   test case.  If false, run all tests in the same VM (fast, but dangerous).
  # * :clonevm     -- If true clone the VM each time it is forked.
  # * :properties  -- Hash of system properties available to the test case.
  # * :environment -- Hash of environment variables available to the test case.
  # * :java_args   -- Arguments passed as is to the JVM.
  class JUnit < TestFramework::Java

    # Used by the junit:report task. Access through JUnit#report if you want to set various
    # options for that task, for example:
    #   JUnit.report.frames = false
    class Report

      # Parameters passed to the Ant JUnitReport task.
      attr_reader :params
      # True (default) to produce a report using frames, false to produce a single-page report.
      attr_accessor :frames
      # Directory for the report style (defaults to using the internal style).
      attr_accessor :style_dir
      # Target directory for generated report.
      attr_accessor :target

      def initialize
        @params = {}
        @frames = true
        @target = 'reports/junit'
      end

      # :call-seq:
      #   generate(projects, target?)
      #
      # Generates a JUnit report for these projects (must run JUnit tests first) into the
      # target directory. You can specify a target, or let it pick the default one from the
      # target attribute.
      def generate(projects, target = @target.to_s)
        html_in = File.join(target, 'html')
        rm_rf html_in ; mkpath html_in

        Buildr.ant('junit-report') do |ant|
          ant.taskdef :name=>'junitreport', :classname=>'org.apache.tools.ant.taskdefs.optional.junit.XMLResultAggregator',
            :classpath=>Buildr.artifacts(JUnit.ant_taskdef).each(&:invoke).map(&:to_s).join(File::PATH_SEPARATOR)
          ant.junitreport :todir=>target do
            projects.select { |project| project.test.framework == :junit }.
              map { |project| project.test.report_to.to_s }.select { |path| File.exist?(path) }.
              each { |path| ant.fileset(:dir=>path) { ant.include :name=>'TEST-*.xml' }  }
            options = { :format=>frames ? 'frames' : 'noframes' }
            options[:styledir] = style_dir if style_dir
            ant.report options.merge(:todir=>html_in) do
              params.each { |key, value| ant.param :name=>key, :expression=>value }
            end
          end
        end
      end

    end

    # JUnit version number.
    VERSION = '4.11'

    class << self
      # :call-seq:
      #    report()
      #
      # Returns the Report object used by the junit:report task. You can use this object to set
      # various options that affect your report, for example:
      #   JUnit.report.frames = false
      #   JUnit.report.params['title'] = 'My App'
      def report
        @report ||= Report.new
      end

      def version
        Buildr.settings.build['junit'] || VERSION
      end

      def dependencies
        four11_or_newer = version >= "4.11"
        @dependencies ||= ["junit:junit:jar:#{version}"]+ (four11_or_newer ? JMock.dependencies({:hamcrest => '1.3'}) : JMock.dependencies)
      end

      def ant_taskdef #:nodoc:
        "org.apache.ant:ant-junit:jar:#{Ant.version}"
      end

    private
      def const_missing(const)
        return super unless const == :REQUIRES # TODO: remove in 1.5
        Buildr.application.deprecated "Please use JUnit.dependencies/.version instead of JUnit::REQUIRES/VERSION"
        dependencies
      end
    end

    def tests(dependencies) #:nodoc:
      if (self.class.version.to_s[0,1].to_i < 4)
        filter_classes(dependencies, :interfaces => %w{junit.framework.TestCase})
      else
        filter_classes(dependencies,
                       :interfaces => %w{junit.framework.TestCase},
                       :class_annotations => %w{org.junit.runner.RunWith},
                       :method_annotations => %w{org.junit.Test})
      end

    end

    def run(tests, dependencies) #:nodoc:
      # Use Ant to execute the Junit tasks, gives us performance and reporting.
      Buildr.ant('junit') do |ant|
        case options[:fork]
        when false
          forking = {}
        when :each
          forking = { :fork=>true, :forkmode=>'perTest' }
        when nil, true, :once
          forking = { :fork=>true, :forkmode=>'once' }
        else
          fail 'Option fork must be :once, :each or false.'
        end
        mkpath task.report_to.to_s

        taskdef = Buildr.artifact(JUnit.ant_taskdef)
        taskdef.invoke
        ant.taskdef :name=>'junit', :classname=>'org.apache.tools.ant.taskdefs.optional.junit.JUnitTask', :classpath=>taskdef.to_s

        ant.junit forking.merge(:clonevm=> !!options[:clonevm], :dir=>task.send(:project).path_to) do
          ant.classpath :path=>dependencies.join(File::PATH_SEPARATOR)
          (options[:properties] || []).each { |key, value| ant.sysproperty :key=>key, :value=>value }
          (options[:environment] || []).each { |key, value| ant.env :key=>key, :value=>value }
          Array(options[:java_args]).each { |value| ant.jvmarg :value=>value }
          ant.formatter :type=>'plain'
          ant.formatter :type=>'plain', :usefile=>false # log test
          ant.formatter :type=>'xml'
          ant.batchtest :todir=>task.report_to.to_s, :failureproperty=>'failed' do
            ant.fileset :dir=>task.compile.target.to_s do
              tests.each { |test| ant.include :name=>File.join(*test.split('.')).ext('class') }
            end
          end
        end
        return tests unless ant.project.getProperty('failed')
      end
      # But Ant doesn't tell us what went kaput, so we'll have to parse the test files.
      tests.inject([]) do |passed, test|
        report_file = File.join(task.report_to.to_s, "TEST-#{test}.txt")
        if File.exist?(report_file)
          report = File.read(report_file)
          # The second line (if exists) is the status line and we scan it for its values.
          status = (report.split("\n")[1] || '').scan(/(run|failures|errors):\s*(\d+)/i).
            inject(Hash.new(0)) { |hash, pair| hash[pair[0].downcase.to_sym] = pair[1].to_i ; hash }
          passed << test if status[:failures] == 0 && status[:errors] == 0
        end
        passed
      end
    end

    namespace 'junit' do
      desc "Generate JUnit tests report in #{report.target}"
      task('report') do |task|
        report.generate Project.projects
        info "Generated JUnit tests report in #{report.target}"
      end
    end

    task('clean') { rm_rf report.target.to_s }

  end


  # TestNG test framework.  To use in your project:
  #   test.using :testng
  #
  # Support the following options:
  # * :properties -- Hash of properties passed to the test suite.
  # * :java_args -- Arguments passed to the JVM.
  # * :args -- Arguments passed to the TestNG command line runner.
  class TestNG < TestFramework::Java

    VERSION = '6.8.7'

    class << self
      def version
        Buildr.settings.build['testng'] || VERSION
      end

      def dependencies
        return ["org.testng:testng:jar:jdk15:#{version}"] + JMock.dependencies if version < "6.0"
        ["org.testng:testng:jar:#{version}",'com.beust:jcommander:jar:1.27'] + JMock.dependencies
      end

    private
      def const_missing(const)
        return super unless const == :REQUIRES # TODO: remove in 1.5
        Buildr.application.deprecated "Please use TestNG.dependencies/.version instead of TestNG::REQUIRES/VERSION"
        dependencies
      end
    end

    def tests(dependencies) #:nodoc:
      filter_classes(dependencies,
                     :class_annotations => %w{org.testng.annotations.Test},
                     :method_annotations => %w{org.testng.annotations.Test})
    end

    def run(tests, dependencies) #:nodoc:
      cmd_args = []
      cmd_args << '-suitename' << task.project.id
      cmd_args << '-sourcedir' << task.compile.sources.join(';') if TestNG.version < "6.0"
      cmd_args << '-log' << '2'
      cmd_args << '-d' << task.report_to.to_s
      exclude_args = options[:excludegroups] || []
      if !exclude_args.empty?
        cmd_args << '-excludegroups' << exclude_args.join(",")
      end
      groups_args = options[:groups] || []
      if !groups_args.empty?
        cmd_args << '-groups' << groups_args.join(",")
      end
      # run all tests in the same suite
      cmd_args << '-testclass' << (TestNG.version < "6.0" ? test : tests.join(','))

      cmd_args += options[:args] if options[:args]

      cmd_options = { :properties=>options[:properties], :java_args=>options[:java_args],
        :classpath=>dependencies, :name => "TestNG in #{task.send(:project).name}" }

      tmp = nil
      begin
        tmp = Tempfile.open("testNG")
        tmp.write cmd_args.join("\n")
        tmp.close
        Java::Commands.java ['org.testng.TestNG', "@#{tmp.path}"], cmd_options
        return tests
      rescue
        # testng-failed.xml contains the list of failed tests *only*
        report = File.read(File.join(task.report_to.to_s, 'testng-failed.xml'))
        failed = report.scan(/<class name="(.*?)">/im).flatten
        error "TestNG regexp returned unexpected failed tests #{failed.inspect}" unless (failed - tests).empty?
        # return the list of passed tests
        return tests - failed
      ensure
        tmp.close unless tmp.nil?
      end
    end

  end

  # A composite test framework that runs multiple other test frameworks.
  #
  # e.g.,
  #        test.using :multitest, :frameworks => [ Buildr::JUnit, Buildr::TestNG ], :options = {
  #          :junit => { :fork => true },
  #          :testng => { ... }
  #        }
  #
  class MultiTest < Buildr::TestFramework::Java
    # TODO: Support multiple test report locations, one per framework

    class << self
      def applies_to?(project)  #:nodoc:
        false # no auto-detection, should be set explicitly
      end
    end

    attr_accessor :frameworks

    def initialize(task, options) #:nodoc:
      super
      fail "Missing :frameworks option" unless options[:frameworks]
      @frameworks = options[:frameworks].map do |f|
        framework_options = (options[:options] || {})[f.to_sym] || {}
        f.new(task, framework_options)
      end
    end

    def dependencies #:nodoc:
      unless @dependencies
        @dependencies = TestFramework::Java.dependencies
        @dependencies += @frameworks.map { |f| f.dependencies }.flatten
      end
      @dependencies
    end


    def tests(dependencies)
      @frameworks.map { |f| f.tests(dependencies) }.flatten
    end

    def run(tests, dependencies)  #:nodoc:
      framework_for_test = @frameworks.inject({}) do |hash, f|
        f.tests(dependencies).each { |t| hash[t] = f }
        hash
      end

      tests_by_framework = tests.group_by { |t| framework_for_test[t] }

      passed = []
      tests_by_framework.each do |f, tests|
        passed += f.run(tests, dependencies)
      end
      passed
    end
  end # MultiTest

end # Buildr


Buildr::TestFramework << Buildr::JUnit
Buildr::TestFramework << Buildr::TestNG
Buildr::TestFramework << Buildr::MultiTest
