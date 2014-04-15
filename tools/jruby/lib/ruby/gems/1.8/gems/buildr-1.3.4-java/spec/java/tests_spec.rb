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


describe Buildr::JUnit do
  it 'should be the default test framework when test cases are in java' do
    write 'src/test/java/com/exampe/FirstTest.java', <<-JAVA
      package com.example;
      public class FirstTest extends junit.framework.TestCase { }
    JAVA
    define 'foo'
    project('foo').test.framework.should eql(:junit)
  end

  it 'should be picked if the test language is Java' do
    define 'foo' do
      test.compile.using(:javac)
      test.framework.should eql(:junit)
    end
  end

  it 'should include JUnit dependencies' do
    define('foo') { test.using(:junit) }
    project('foo').test.compile.dependencies.should include(artifact("junit:junit:jar:#{JUnit.version}"))
    project('foo').test.dependencies.should include(artifact("junit:junit:jar:#{JUnit.version}"))
  end
  
  it 'should have REQUIRES up to version 1.5 since it was deprecated in 1.3.3' do
    Buildr::VERSION.should < '1.5'
    lambda { JUnit::REQUIRES }.should_not raise_error
  end
  
  it 'should pick JUnit version from junit build settings' do
    Buildr::JUnit.instance_eval { @dependencies = nil }
    write 'build.yaml', 'junit: 1.2.3'
    define('foo') { test.using(:junit) }
    project('foo').test.compile.dependencies.should include(artifact("junit:junit:jar:1.2.3"))
  end
  
  it 'should include JMock dependencies' do
    define('foo') { test.using(:junit) }
    project('foo').test.compile.dependencies.should include(artifact("jmock:jmock:jar:#{JMock.version}"))
    project('foo').test.dependencies.should include(artifact("jmock:jmock:jar:#{JMock.version}"))
  end

  it 'should pick JUnit version from junit build settings' do
    Buildr::JUnit.instance_eval { @dependencies = nil } # JUnit caches JMock dependencies
    Buildr::JMock.instance_eval { @dependencies = nil }
    write 'build.yaml', 'jmock: 1.2.3'
    define('foo') { test.using(:junit) }
    project('foo').test.compile.dependencies.should include(artifact("jmock:jmock:jar:1.2.3"))
  end
  
  it 'should include public classes extending junit.framework.TestCase' do
    write 'src/test/java/com/example/FirstTest.java', <<-JAVA
      package com.example;
      public class FirstTest extends junit.framework.TestCase {
        public void testNothing() { }
      }
    JAVA
    write 'src/test/java/com/example/AnotherOne.java', <<-JAVA
      package com.example;
      public class AnotherOne extends junit.framework.TestCase {
        public void testNothing() { }
      }
    JAVA
    define('foo').test.invoke
    project('foo').test.tests.should include('com.example.FirstTest', 'com.example.AnotherOne')
  end

  it 'should include public classes with annotated test cases' do
    write 'src/test/java/com/example/FirstTest.java', <<-JAVA
      package com.example;
      import org.junit.Test;
      public class FirstTest {
        public void utilityMethod() { }
        @Test
        public void annotated() { }
      }
    JAVA
    define('foo').test.invoke
    project('foo').test.tests.should include('com.example.FirstTest')
  end

  it 'should include public classes with RunWith annotation' do
    write 'src/test/java/com/example/TestSuite.java', <<-JAVA
      package com.example;
      import org.junit.Test;
      public class TestSuite {
        @Test
        public void annotated() { }
      }
    JAVA
    write 'src/test/java/com/example/RunSuite.java', <<-JAVA
      package com.example;
      import org.junit.runner.RunWith;
      import org.junit.runners.Suite;
      @RunWith(Suite.class)
      @Suite.SuiteClasses({TestSuite.class})
      public class RunSuite {
      }
    JAVA
    define('foo').test.invoke
    project('foo').test.tests.should include('com.example.RunSuite')
  end

  it 'should ignore classes not extending junit.framework.TestCase' do
    write 'src/test/java/NotATest.java', <<-JAVA
      public class NotATest { }
    JAVA
    define('foo').test.invoke
    project('foo').test.tests.should be_empty
  end

  it 'should ignore inner classes' do
    write 'src/test/java/InnerClassTest.java', <<-JAVA
      public class InnerClassTest extends junit.framework.TestCase {
        public void testNothing() { }

        public class InnerTest extends junit.framework.TestCase {
          public void testNothing() { }
        }
      }
    JAVA
    define('foo').test.invoke
    project('foo').test.tests.should eql(['InnerClassTest'])
  end

  it 'should ignore abstract classes' do
    write 'src/test/java/AbstractClassTest.java', <<-JAVA
      public abstract class AbstractClassTest extends junit.framework.TestCase {
        public void testNothing() { }
      }
    JAVA
    define('foo').test.invoke
    project('foo').test.tests.should be_empty
  end

  it 'should ignore classes with no tests in them' do
    write 'src/test/java/NoTests.java', <<-JAVA
      public class NoTests {
      }
    JAVA
    define('foo').test.invoke
    project('foo').test.tests.should be_empty
  end

  it 'should pass when JUnit test case passes' do
    write 'src/test/java/PassingTest.java', <<-JAVA
      public class PassingTest extends junit.framework.TestCase {
        public void testNothing() {}
      }
    JAVA
    lambda { define('foo').test.invoke }.should_not raise_error
  end

  it 'should fail when JUnit test case fails' do
    write 'src/test/java/FailingTest.java', <<-JAVA
      public class FailingTest extends junit.framework.TestCase {
        public void testFailure() {
          assertTrue(false);
        }
      }
    JAVA
    lambda { define('foo').test.invoke }.should raise_error(RuntimeError, /Tests failed/) rescue nil
  end

  it 'should report failed test names' do
    write 'src/test/java/FailingTest.java', <<-JAVA
      public class FailingTest extends junit.framework.TestCase {
        public void testFailure() {
          assertTrue(false);
        }
      }
    JAVA
    define('foo').test.invoke rescue
    project('foo').test.failed_tests.should include('FailingTest')
  end

  it 'should report to reports/junit' do
    write 'src/test/java/PassingTest.java', <<-JAVA
      public class PassingTest extends junit.framework.TestCase {
        public void testNothing() {}
      }
    JAVA
    define 'foo' do
      test.report_to.should be(file('reports/junit'))
    end
    project('foo').test.invoke
    project('foo').file('reports/junit/TEST-PassingTest.txt').should exist
    project('foo').file('reports/junit/TEST-PassingTest.xml').should exist
  end

  it 'should pass properties to JVM' do
    write 'src/test/java/PropertyTest.java', <<-JAVA
      public class PropertyTest extends junit.framework.TestCase {
        public void testProperty() {
          assertEquals("value", System.getProperty("name"));
        }
      }
    JAVA
    define('foo').test.using :properties=>{ 'name'=>'value' }
    project('foo').test.invoke
  end

  it 'should pass environment to JVM' do
    write 'src/test/java/EnvironmentTest.java', <<-JAVA
      public class EnvironmentTest extends junit.framework.TestCase {
        public void testEnvironment() {
          assertEquals("value", System.getenv("NAME"));
        }
      }
    JAVA
    define('foo').test.using :environment=>{ 'NAME'=>'value' }
    project('foo').test.invoke
  end

  it 'should set current directory' do
    mkpath 'baz'
    expected = File.expand_path('baz')
    expected.gsub!('/', '\\') if expected =~ /^[A-Z]:/ # Java returns back slashed paths for windows
    write 'baz/src/test/java/CurrentDirectoryTest.java', <<-JAVA
      public class CurrentDirectoryTest extends junit.framework.TestCase {
        public void testCurrentDirectory() throws Exception {
          assertEquals(#{expected.inspect}, new java.io.File(".").getCanonicalPath());
        }
      }
    JAVA
    define 'bar' do
      define 'baz' do
        test.include 'CurrentDirectoryTest'
      end
    end
    project('bar:baz').test.invoke
  end

  def fork_tests(mode)
    write 'src/test/java/Shared.java', <<-JAVA
      public class Shared {
        public static boolean flag = false;
      }
    JAVA
    write 'src/test/java/TestCase1.java', <<-JAVA
      public class TestCase1 extends junit.framework.TestCase {
        public void testSameVM() {
          assertFalse(Shared.flag);
          Shared.flag = true;
        }
      }
    JAVA
    write 'src/test/java/TestCase2.java', <<-JAVA
      public class TestCase2 extends junit.framework.TestCase {
        public void testSameVM() {
          assertFalse(Shared.flag);
          Shared.flag = true;
        }
      }
    JAVA
    define 'foo' do
      test.using :fork=>mode, :fail_on_failure=>false
    end
    project('foo').test.invoke
  end

  it 'should run all test cases in same VM if fork is once' do
    fork_tests :once
    project('foo').test.failed_tests.size.should eql(1)
  end

  it 'should run each test case in separate same VM if fork is each' do
    fork_tests :each
    project('foo').test.failed_tests.should be_empty
  end
  
  after do
    # Yes, this is ugly.  Better solution?
    Buildr::JUnit.instance_eval { @dependencies = nil }
    Buildr::JMock.instance_eval { @dependencies = nil }
  end
end


describe Buildr::JUnit, 'report' do
  it 'should default to the target directory reports/junit' do
    JUnit.report.target.should eql('reports/junit')
  end

  it 'should generate report into the target directory' do
    JUnit.report.target = 'test-report'
    lambda { task('junit:report').invoke }.should change { File.exist?(JUnit.report.target) }.to(true)
  end

  it 'should clean after itself' do
    mkpath JUnit.report.target
    lambda { task('clean').invoke }.should change { File.exist?(JUnit.report.target) }.to(false)
  end

  it 'should generate a consolidated XML report' do
    lambda { task('junit:report').invoke }.should change { File.exist?('reports/junit/TESTS-TestSuites.xml') }.to(true)
  end

  it 'should default to generating a report with frames' do
    JUnit.report.frames.should be_true
  end

  it 'should generate single page when frames is false' do
    JUnit.report.frames = false
    task('junit:report').invoke
    file('reports/junit/html/junit-noframes.html').should exist
  end

  it 'should generate frame page when frames is false' do
    JUnit.report.frames = true
    task('junit:report').invoke
    file('reports/junit/html/index.html').should exist
  end

  it 'should generate reports from all projects that ran test cases' do
    write 'src/test/java/TestSomething.java', <<-JAVA
      public class TestSomething extends junit.framework.TestCase {
        public void testNothing() {}
      }
    JAVA
    define 'foo'
    project('foo').test.invoke
    task('junit:report').invoke
    FileList['reports/junit/html/*TestSomething.html'].size.should be(1)
  end

  after do
    JUnit.instance_eval { @report = nil }
  end
end


describe Buildr::TestNG do
  it 'should be selectable in project' do
    define 'foo' do
      test.using(:testng)
      test.framework.should eql(:testng)
    end
  end

  it 'should be selectable in parent project' do
    write 'bar/src/test/java/TestCase.java'
    define 'foo' do
      test.using(:testng)
      define 'bar'
    end
    project('foo:bar').test.framework.should eql(:testng)
  end

  it 'should include TestNG dependencies' do
    define('foo') { test.using :testng }
    project('foo').test.compile.dependencies.should include(artifact("org.testng:testng:jar:jdk15:#{TestNG.version}"))
    project('foo').test.dependencies.should include(artifact("org.testng:testng:jar:jdk15:#{TestNG.version}"))
  end

  it 'should include TestNG dependencies' do
    define('foo') { test.using :testng }
    project('foo').test.compile.dependencies.should include(artifact("jmock:jmock:jar:#{JMock.version}"))
    project('foo').test.dependencies.should include(artifact("jmock:jmock:jar:#{JMock.version}"))
  end

  it 'should include classes using TestNG annotations' do
    write 'src/test/java/com/example/AnnotatedClass.java', <<-JAVA
      package com.example;
      @org.testng.annotations.Test
      public class AnnotatedClass { }
    JAVA
    write 'src/test/java/com/example/AnnotatedMethod.java', <<-JAVA
      package com.example;
      public class AnnotatedMethod {
        @org.testng.annotations.Test
        public void annotated() { }
      }
    JAVA
    define('foo') { test.using(:testng) }
    project('foo').test.invoke
    project('foo').test.tests.should include('com.example.AnnotatedClass', 'com.example.AnnotatedMethod')
  end

  it 'should ignore classes not using TestNG annotations' do
    write 'src/test/java/NotATestClass.java', 'public class NotATestClass {}'
    define('foo') { test.using(:testng) }
    project('foo').test.invoke
    project('foo').test.tests.should be_empty
  end

  it 'should ignore inner classes' do
    write 'src/test/java/InnerClassTest.java', <<-JAVA
      @org.testng.annotations.Test
      public class InnerClassTest {
        public class InnerTest {
        }
      }
    JAVA
    define('foo') { test.using(:testng) }
    project('foo').test.invoke
    project('foo').test.tests.should eql(['InnerClassTest'])
  end

  it 'should pass when TestNG test case passes' do
    write 'src/test/java/PassingTest.java', <<-JAVA
      public class PassingTest {
        @org.testng.annotations.Test
        public void testNothing() {}
      }
    JAVA
    define('foo') { test.using(:testng) }
    lambda { project('foo').test.invoke }.should_not raise_error
  end

  it 'should fail when TestNG test case fails' do
    write 'src/test/java/FailingTest.java', <<-JAVA
      public class FailingTest {
        @org.testng.annotations.Test
        public void testNothing() {
          org.testng.AssertJUnit.assertTrue(false);
        }
      }
    JAVA
    define('foo') { test.using(:testng) }
    lambda { project('foo').test.invoke }.should raise_error(RuntimeError, /Tests failed/)
  end

  it 'should report failed test names' do
    write 'src/test/java/FailingTest.java', <<-JAVA
      public class FailingTest {
        @org.testng.annotations.Test
        public void testNothing() {
          org.testng.AssertJUnit.assertTrue(false);
        }
      }
    JAVA
    define('foo') { test.using(:testng) }
    project('foo').test.invoke rescue nil
    project('foo').test.failed_tests.should include('FailingTest')
  end

  it 'should report to reports/testng' do
    define('foo') { test.using(:testng) }
    project('foo').test.report_to.should be(project('foo').file('reports/testng'))
  end

  it 'should generate reports' do
    write 'src/test/java/PassingTest.java', <<-JAVA
      public class PassingTest {
        @org.testng.annotations.Test
        public void testNothing() {}
      }
    JAVA
    define('foo') { test.using(:testng) }
    lambda { project('foo').test.invoke }.should change { File.exist?('reports/testng/foo/index.html') }.to(true)
  end
end
