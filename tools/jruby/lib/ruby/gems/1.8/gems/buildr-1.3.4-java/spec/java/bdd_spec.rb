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

require File.join(File.dirname(__FILE__), '../spec_helpers')

describe Buildr::RSpec do

  def foo(*args, &prc)
    define('foo', *args) do 
      test.using :rspec, :output => false
      if prc
        instance_eval(&prc)
      else
        self
      end
    end
  end

  it 'should be selected by :rspec name' do
    foo { test.framework.should eql(:rspec) }
  end

  it 'should read passed specs from result yaml' do
    success = File.expand_path('src/spec/ruby/success_spec.rb')
    write(success, 'describe("success") { it("is true") { nil.should be_nil } }')
    foo do
      test.invoke
      test.passed_tests.should eql([success])
    end
  end

  it 'should read result yaml to obtain the list of failed specs' do
    success = File.expand_path('src/spec/ruby/success_spec.rb')
    write(success, 'describe("success") { it("is true") { nil.should be_nil } }')
    failure = File.expand_path('src/spec/ruby/failure_spec.rb')
    write(failure, 'describe("failure") { it("is false") { true.should == false } }')
    error = File.expand_path('src/spec/ruby/error_spec.rb')
    write(error, 'describe("error") { it("raises") { lambda; } }')
    foo do
      lambda { test.invoke }.should raise_error(/Tests failed/)
      test.tests.should include(success, failure, error)
      test.failed_tests.sort.should eql([failure, error].sort)
      test.passed_tests.should eql([success])
    end
  end

end if RUBY_PLATFORM =~ /java/ || ENV['JRUBY_HOME'] # RSpec

describe Buildr::JtestR do

  def foo(*args, &prc)
    define('foo', *args) do
      test.using :jtestr, :output => false
      if prc
        instance_eval(&prc)
      else
        self
      end
    end
  end

  it 'should be selected by :jtestr name' do
    foo { test.framework.should eql(:jtestr) }
  end

  it 'should apply to projects having test_unit sources' do
    define('one', :base_dir => 'one') do
      write _('src/spec/ruby/one_test.rb')
      JtestR.applies_to?(self).should be_true
    end
    define('two', :base_dir => 'two') do
      write _('src/spec/ruby/twoTest.rb')
      JtestR.applies_to?(self).should be_true
    end
    define('three', :base_dir => 'three') do
      write _('src/spec/ruby/tc_three.rb')
      JtestR.applies_to?(self).should be_true
    end
    define('four', :base_dir => 'four') do
      write _('src/spec/ruby/ts_four.rb')
      JtestR.applies_to?(self).should be_true
    end
  end

  it 'should apply to projects having rspec sources' do
    define('one', :base_dir => 'one') do
      write _('src/spec/ruby/one_spec.rb')
      JtestR.applies_to?(self).should be_true
    end
  end

  it 'should apply to projects having expectations sources' do
    define('one', :base_dir => 'one') do
      write _('src/spec/ruby/one_expect.rb')
      JtestR.applies_to?(self).should be_true
    end
  end

  it 'should apply to projects having junit sources' do
    define('one', :base_dir => 'one') do
      write _('src/test/java/example/OneTest.java', <<-JAVA)
        package example;
        public class OneTest extends junit.framework.TestCase { }
      JAVA
      JtestR.applies_to?(self).should be_true
    end
  end

  it 'should apply to projects having testng sources' do
    define('one', :base_dir => 'one') do
      write _('src/test/java/example/OneTest.java', <<-JAVA)
        package example;
        public class OneTest { 
           @org.testng.annotations.Test
           public void testNothing() {}
        }
      JAVA
      JtestR.applies_to?(self).should be_true
    end
  end

  it 'should use a java compiler if java sources found' do
    foo do
      write _('src/spec/java/Something.java'), 'public class Something {}'
      test.compile.language.should eql(:java)
    end
  end

  it 'should load user jtestr_config.rb' do
    foo do 
      hello = _('hello')
      write('src/spec/ruby/jtestr_config.rb', "File.open('#{hello}', 'w') { |f| f.write 'HELLO' }")
      write('src/spec/ruby/some_spec.rb')
      test.invoke
      File.should be_exist(hello)
      File.read(hello).should == 'HELLO'
    end
  end

  it 'should run junit tests' do
    write('src/test/java/example/SuccessTest.java', <<-JAVA)
        package example;
        public class SuccessTest extends junit.framework.TestCase { 
           public void testSuccess() { assertTrue(true); }
        }
    JAVA
    write('src/test/java/example/FailureTest.java', <<-JAVA)
        package example;
        public class FailureTest extends junit.framework.TestCase { 
           public void testFailure() { assertTrue(false); }
        }
    JAVA
    foo do
      lambda { test.invoke }.should raise_error(/Tests failed/)
      test.tests.should include('example.SuccessTest', 'example.FailureTest')
      test.failed_tests.should include('example.FailureTest')
      test.passed_tests.should include('example.SuccessTest')
    end
  end

  it 'should run testng tests' do 
    write('src/test/java/example/Success.java', <<-JAVA)
        package example;
        public class Success {
          @org.testng.annotations.Test
          public void annotatedSuccess() { org.testng.Assert.assertTrue(true); }
        }
    JAVA
    write('src/test/java/example/Failure.java', <<-JAVA)
        package example;
        public class Failure {
          @org.testng.annotations.Test
          public void annotatedFail() { org.testng.Assert.fail("FAIL"); }
        }
    JAVA
    foo do
      lambda { test.invoke }.should raise_error(/Tests failed/)
      test.tests.should include('example.Success', 'example.Failure')
      test.failed_tests.should include('example.Failure')
      test.passed_tests.should include('example.Success')
    end
  end

  it 'should run test_unit' do 
    success = File.expand_path('src/spec/ruby/success_test.rb')
    write(success, <<-TESTUNIT)
      require 'test/unit'
      class TC_Success < Test::Unit::TestCase
        def test_success
          assert true
        end
      end
    TESTUNIT
    failure = File.expand_path('src/spec/ruby/failure_test.rb')
    write(failure, <<-TESTUNIT)
      require 'test/unit'
      class TC_Failure < Test::Unit::TestCase
        def test_failure
          assert false
        end
      end
    TESTUNIT
    error = File.expand_path('src/spec/ruby/error_test.rb')
    write(error, <<-TESTUNIT)
      require 'test/unit'
      class TC_Error < Test::Unit::TestCase
        def test_error
          lambda;
        end
      end
    TESTUNIT
    foo do
      lambda { test.invoke }.should raise_error(/Tests failed/)
      test.tests.should include(success, failure, error)
      test.failed_tests.should include(failure, error)
      test.passed_tests.should include(success)
    end
  end

  it 'should run expectations' do
    success = File.expand_path('src/spec/ruby/success_expect.rb')
    write(success, 'Expectations { expect(true) { true } }')
    failure = File.expand_path('src/spec/ruby/failure_expect.rb')
    write(failure, 'Expectations { expect(true) { false } }')
    error = File.expand_path('src/spec/ruby/error_expect.rb')
    write(error, 'Expectations { expect(nil) { lambda; } }')
    foo do
      lambda { test.invoke }.should raise_error(/Tests failed/)
      test.tests.should include(success, failure, error)
      test.failed_tests.should include(failure, error)
      test.passed_tests.should include(success)
    end
  end

  it 'should run rspecs' do
    success = File.expand_path('src/spec/ruby/success_spec.rb')
    write(success, 'describe("success") { it("is true") { nil.should be_nil } }')
    failure = File.expand_path('src/spec/ruby/failure_spec.rb')
    write(failure, 'describe("failure") { it("is false") { true.should == false } }')
    error = File.expand_path('src/spec/ruby/error_spec.rb')
    write(error, 'describe("error") { it("raises") { lambda; } }')
    pending =  File.expand_path('src/spec/ruby/pending_spec.rb')
    write(pending, 'describe("peding") { it "is not implemented" }')
    foo do
      lambda { test.invoke }.should raise_error(/Tests failed/)
      test.tests.should include(success, failure, error)
      test.failed_tests.should include(failure, error)
      test.passed_tests.should include(success)
    end
  end


end if RUBY_PLATFORM =~ /java/ || ENV['JRUBY_HOME'] # JtestR

describe Buildr::JBehave do
  def foo(*args, &prc)
    define('foo', *args) do 
      test.using :jbehave
      if prc
        instance_eval(&prc)
      else
        self
      end
    end
  end

  it 'should apply to projects having JBehave sources' do
    define('one', :base_dir => 'one') do
      write _('src/spec/java/SomeBehaviour.java'), 'public class SomeBehaviour {}'
      JBehave.applies_to?(self).should be_true
    end
    define('two', :base_dir => 'two') do
      write _('src/test/java/SomeBehaviour.java'), 'public class SomeBehaviour {}'
      JBehave.applies_to?(self).should be_false
    end
    define('three', :base_dir => 'three') do
      write _('src/spec/java/SomeBehavior.java'), 'public class SomeBehavior {}'
      JBehave.applies_to?(self).should be_true
    end
    define('four', :base_dir => 'four') do
      write _('src/test/java/SomeBehavior.java'), 'public class SomeBehavior {}'
      JBehave.applies_to?(self).should be_false
    end
  end

  it 'should be selected by :jbehave name' do
    foo { test.framework.should eql(:jbehave) }
  end

  it 'should select a java compiler for its sources' do 
    write 'src/test/java/SomeBehavior.java', 'public class SomeBehavior {}'
    foo do
      test.compile.language.should eql(:java)
    end
  end

  it 'should include JBehave dependencies' do
    foo do
      test.compile.dependencies.should include(artifact("org.jbehave:jbehave:jar::#{JBehave.version}"))
      test.dependencies.should include(artifact("org.jbehave:jbehave:jar::#{JBehave.version}"))
    end
  end

  it 'should include JMock dependencies' do
    foo do
      test.compile.dependencies.should include(artifact("jmock:jmock:jar:#{JMock.version}"))
      test.dependencies.should include(artifact("jmock:jmock:jar:#{JMock.version}"))
    end
  end

  it 'should include classes whose name ends with Behavior' do
    write 'src/spec/java/some/FooBehavior.java', <<-JAVA
      package some;
      public class FooBehavior {
        public void shouldFoo() { assert true; }
      }
    JAVA
    write 'src/spec/java/some/NotATest.java', <<-JAVA
      package some;
      public class NotATest { }
    JAVA
    foo.tap do |project|
      project.test.invoke
      project.test.tests.should include('some.FooBehavior')
    end
  end


  it 'should include classes implementing Behaviours' do
    write 'src/spec/java/some/MyBehaviours.java',  <<-JAVA
      package some;
      public class MyBehaviours implements 
      org.jbehave.core.behaviour.Behaviours {
        public Class[] getBehaviours() {
           return new Class[] { some.FooBehave.class };
        }
      }
    JAVA
    write 'src/spec/java/some/FooBehave.java', <<-JAVA
      package some;
      public class FooBehave {
        public void shouldFoo() { assert true; }
      }
    JAVA
    write 'src/spec/java/some/NotATest.java', <<-JAVA
      package some;
      public class NotATest { }
    JAVA
    foo.tap do |project|
      project.test.invoke
      project.test.tests.should include('some.MyBehaviours')
    end
  end

end # JBehave
