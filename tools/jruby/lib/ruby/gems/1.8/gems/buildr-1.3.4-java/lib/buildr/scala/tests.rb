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


require 'buildr/core/build'
require 'buildr/core/compile'
require 'buildr/java/ant'
require 'buildr/java/tests'


module Buildr::Scala
  # Scala::Check is available when using Scala::Test or Scala::Specs
  module Check
    VERSION = '1.5'
    
    class << self
      def version
        Buildr.settings.build['scala.check'] || VERSION
      end
      
      def dependencies
        ["org.scalacheck:scalacheck:jar:#{version}"]
      end  

    private
      def const_missing(const)
        return super unless const == :REQUIRES # TODO: remove in 1.5
        Buildr.application.deprecated "Please use Scala::Check.dependencies/.version instead of ScalaCheck::REQUIRES/VERSION"
        dependencies
      end
    end
  end
  
  
  # ScalaTest framework, the default test framework for Scala tests.
  #
  # Support the following options:
  # * :properties  -- Hash of system properties available to the test case.
  # * :environment -- Hash of environment variables available to the test case.
  # * :java_args   -- Arguments passed as is to the JVM.
  class ScalaTest < Buildr::TestFramework::Java

    VERSION = '0.9.5'

    class << self
      def version
        Buildr.settings.build['scala.test'] || VERSION
      end
      
      def dependencies
        ["org.scala-tools.testing:scalatest:jar:#{version}"] + Check.dependencies + 
          JMock.dependencies + JUnit.dependencies
      end  

      def applies_to?(project) #:nodoc:
        !Dir[project.path_to(:source, :test, :scala, '**/*.scala')].empty?
      end 
      
    private
      def const_missing(const)
        return super unless const == :REQUIRES # TODO: remove in 1.5
        Buildr.application.deprecated "Please use Scala::Test.dependencies/.version instead of ScalaTest::REQUIRES/VERSION"
        dependencies
      end
    end          

    # annotation-based group inclusion
    attr_accessor :group_includes
    
    # annotation-based group exclusion
    attr_accessor :group_excludes
    
    def initialize(test_task, options)
      super
      @group_includes = []
      @group_excludes = []
    end

    def tests(dependencies) #:nodoc:
      filter_classes(dependencies, :interfaces => %w{org.scalatest.Suite})
    end

    def run(scalatest, dependencies) #:nodoc:
      mkpath task.report_to.to_s
      success = []

      reporter_options = 'TFGBSAR' # testSucceeded, testFailed, testIgnored, suiteAborted, runStopped, runAborted, runCompleted
      scalatest.each do |suite|
        info "ScalaTest #{suite.inspect}"
        # Use Ant to execute the ScalaTest task, gives us performance and reporting.
        reportFile = File.join(task.report_to.to_s, "TEST-#{suite}.txt")
        taskdef = Buildr.artifacts(self.class.dependencies).each(&:invoke).map(&:to_s)
        Buildr.ant('scalatest') do |ant|
          ant.taskdef :name=>'scalatest', :classname=>'org.scalatest.tools.ScalaTestTask',
            :classpath=>taskdef.join(File::PATH_SEPARATOR)
          ant.scalatest :runpath=>dependencies.join(File::PATH_SEPARATOR) do
            ant.suite    :classname=>suite
            ant.reporter :type=>'stdout', :config=>reporter_options
            ant.reporter :type=>'file', :filename=> reportFile, :config=>reporter_options
            ant.includes group_includes.join(" ") if group_includes
            ant.excludes group_excludes.join(" ") if group_excludes
            (options[:properties] || []).each { |name, value| ant.property :name=>name, :value=>value }
          end
        end
        
        # Parse for failures, errors, etc.
        # This is a bit of a pain right now because ScalaTest doesn't flush its
        # output synchronously before the Ant test finishes so we have to loop 
        # and wait for an indication that the test run was completed. 
        failed = false
        completed = false
        wait = 0
        while (!completed) do
          File.open(reportFile, "r") do |input|
            while (line = input.gets) do
              failed = (line =~ /(TEST FAILED -)|(RUN STOPPED)|(RUN ABORTED)/) unless failed
              completed |= (line =~ /Run completed\./)
              break if (failed || completed)
            end
          end
          wait += 1
          break if (failed || wait > 10) 
          unless completed
            sleep(1)
          end
        end
        success << suite if (completed && !failed)
      end
      
      success
    end # run

  end # ScalaTest
  
end


# Backwards compatibility stuff.  Remove in 1.5.
module Buildr
  ScalaCheck = Scala::Check
  ScalaTest = Scala::ScalaTest
end

Buildr::TestFramework << Buildr::Scala::ScalaTest
