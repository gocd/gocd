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

require File.expand_path(File.join(File.dirname(__FILE__), '..', 'spec_helpers'))

if Java.java.lang.System.getProperty("java.runtime.version") >= "1.6"

# TODO's
#  -test passing System props
#  -test passing ENV variables
#  -test exclude group
#  -test include Suite's
#  -test exclude Suite's
#
describe "scalatest version" do

  case
  when Buildr::Scala.version?(2.8)
    it 'should be 1.3 for scala 2.8' do
      Scala::ScalaTest.dependencies.should include("org.scalatest:scalatest:jar:1.3")
    end
  when Buildr::Scala.version?(2.9)
    it 'should be 1.8 for scala 2.9' do
      Scala::ScalaTest.dependencies.should include("org.scalatest:scalatest_2.9.2:jar:1.8")
    end
  end
end

describe Buildr::Scala::ScalaTest do

  it 'should be the default test framework when test cases are in Scala' do
    write 'src/test/scala/com/example/MySuite.scala', <<-SCALA
      package com.example
      import org.scalatest.FunSuite
      class MySuite extends FunSuite {
        test("addition") {
          val sum = 1 + 1
          assert(sum === 2)
        }
      }
    SCALA
    define 'foo'
    project('foo').test.framework.should eql(:scalatest)
  end

  it 'should include Scalatest dependencies' do
    define('foo') { test.using(:scalatest) }
    project('foo').test.compile.dependencies.should include(*artifacts(Scala::ScalaTest.dependencies))
    project('foo').test.dependencies.should include(*artifacts(Scala::ScalaTest.dependencies))
  end

  it 'should include JMock dependencies' do
    define('foo') { test.using(:scalatest) }
    project('foo').test.compile.dependencies.should include(*artifacts(JMock.dependencies))
    project('foo').test.dependencies.should include(*artifacts(JMock.dependencies))
  end

  it 'should include ScalaCheck dependencies' do
    define('foo') { test.using(:scalatest) }
    project('foo').test.compile.dependencies.should include(*artifacts(Scala::Check.dependencies))
    project('foo').test.dependencies.should include(*artifacts(Scala::Check.dependencies))
  end

  it 'should set current directory' do
    mkpath 'baz'
    expected = File.expand_path('baz')
    expected.gsub!('/', '\\') if expected =~ /^[A-Z]:/ # Java returns back slashed paths for windows
    write 'baz/src/test/scala/CurrentDirectoryTestSuite.scala', <<-SCALA
      class CurrentDirectoryTestSuite extends org.scalatest.FunSuite {
        test("testCurrentDirectory") {
          assert("value" === System.getenv("NAME"))
          assert(#{expected.inspect} === new java.io.File(".").getCanonicalPath())
        }
      }
    SCALA
    define 'bar' do
      define 'baz' do
        test.include 'CurrentDirectoryTest'
      end
    end
    project('bar:baz').test.invoke
  end

  it 'should include public classes extending org.scalatest.FunSuite' do
    write 'src/test/scala/com/example/MySuite.scala', <<-SCALA
      package com.example
      import org.scalatest.FunSuite
      class MySuite extends FunSuite {
        test("addition") {
          val sum = 1 + 1
          assert(sum === 2)
        }
      }
    SCALA
    define('foo').test.invoke
    project('foo').test.tests.should include('com.example.MySuite')
  end

  it 'should ignore classes not extending org.scalatest.FunSuite' do
    write 'src/test/scala/com/example/NotASuite.scala', <<-SCALA
      package com.example
      class Another {
      }
    SCALA
    define('foo').test.invoke
    project('foo').test.tests.should be_empty
  end

  it 'should ignore inner classes' do
    write 'src/test/scala/com/example/InnerClassTest.scala', <<-SCALA
      package com.example
      import org.scalatest.FunSuite
      class InnerClassTest extends FunSuite {
        test("addition") {
          val sum = 1 + 1
          assert(sum === 2)
        }

        class InnerSuite extends FunSuite {
          test("addition") {
            val sum = 1 + 1
            assert(sum === 2)
          }
        }
      }
    SCALA
    define('foo').test.invoke
    project('foo').test.tests.should eql(['com.example.InnerClassTest'])
  end

  it 'should pass when ScalaTest test case passes' do
    write 'src/test/scala/PassingSuite.scala', <<-SCALA
      class PassingSuite extends org.scalatest.FunSuite {
        test("addition") {
          val sum = 1 + 1
          assert(sum === 2)
        }
      }
    SCALA
    lambda { define('foo').test.invoke }.should_not raise_error
  end

  it 'should fail when ScalaTest test case fails' do
    write 'src/test/scala/FailingSuite.scala', <<-SCALA
      class FailingSuite extends org.scalatest.FunSuite {
        test("failing") {
          assert(false)
        }
      }
    SCALA
    lambda { define('foo').test.invoke }.should raise_error(RuntimeError, /Tests failed/) rescue nil
  end

  it 'should report failed test names' do
    write 'src/test/scala/FailingSuite.scala', <<-SCALA
      class FailingSuite extends org.scalatest.FunSuite {
        test("failing") {
          assert(false)
        }
      }
    SCALA
    define('foo').test.invoke rescue
    project('foo').test.failed_tests.should include('FailingSuite')
  end

  it 'should report to reports/scalatest/TEST-TestSuiteName.xml' do
    write 'src/test/scala/PassingSuite.scala', <<-SCALA
      class PassingSuite extends org.scalatest.FunSuite {
        test("passing") {
          assert(true)
        }
      }
    SCALA
    define 'foo' do
      test.report_to.should be(file('reports/scalatest'))
    end
    project('foo').test.invoke
    project('foo').file('reports/scalatest/TEST-PassingSuite.xml').should exist
  end

  it 'should report to reports/scalatest/TEST-TestSuiteName.txt' do
    write 'src/test/scala/PassingSuite.scala', <<-SCALA
      class PassingSuite extends org.scalatest.FunSuite {
        test("passing") {
          assert(true)
        }
      }
    SCALA
    define 'foo' do
      test.report_to.should be(file('reports/scalatest'))
    end
    project('foo').test.invoke
    project('foo').file('reports/scalatest/TEST-PassingSuite.txt').should exist
  end

  it 'should pass properties to Suite' do
    write 'src/test/scala/PropertyTestSuite.scala', <<-SCALA
      import org.scalatest._
      class PropertyTestSuite extends FunSuite {
        var configMap = Map[String, Any]()
        test("testProperty") {
          assert(configMap("name") === "value")
        }

        protected override def runTests(testName: Option[String], reporter: Reporter, stopper: Stopper,
                                        filter: Filter, configMap: Map[String, Any],
                                        distributor: Option[Distributor], tracker: Tracker) {
          this.configMap = configMap
          super.runTests(testName, reporter, stopper, filter, configMap, distributor, tracker)
        }
      }
    SCALA
    define('foo').test.using :properties=>{ 'name'=>'value' }
    project('foo').test.invoke
  end

  it 'should run with ScalaCheck automatic test case generation' do
    write 'src/test/scala/MySuite.scala', <<-SCALA
      import org.scalatest.FunSuite
      import org.scalatest.prop.Checkers
      import org.scalacheck.Arbitrary._
      import org.scalacheck.Prop._

      class MySuite extends FunSuite with Checkers {

        test("list concatenation") {
          val x = List(1, 2, 3)
          val y = List(4, 5, 6)
          assert(x ::: y === List(1, 2, 3, 4, 5, 6))
          check((a: List[Int], b: List[Int]) => a.size + b.size == (a ::: b).size)
        }

        test("list concatenation using a test method") {
          check((a: List[Int], b: List[Int]) => a.size + b.size == (a ::: b).size)
        }
      }
    SCALA
    define('foo')
    project('foo').test.invoke
    project('foo').test.passed_tests.should include('MySuite')
  end

  it 'should fail if ScalaCheck test case fails' do
    write 'src/test/scala/StringSuite.scala', <<-SCALA
      import org.scalatest.FunSuite
      import org.scalatest.prop.Checkers
      import org.scalacheck.Arbitrary._
      import org.scalacheck.Prop._

      class StringSuite extends FunSuite with Checkers {
        test("startsWith") {
          check( (a: String, b: String) => (a+b).startsWith(a) )
        }

        test("endsWith") {
          check( (a: String, b: String) => (a+b).endsWith(b) )
        }

        // Is this really always true?
        test("concat") {
          check( (a: String, b: String) => (a+b).length > a.length && (a+b).length > b.length )
        }

        test("substring2") {
          check( (a: String, b: String) => (a+b).substring(a.length) == b )
        }

        test("substring3") {
          check( (a: String, b: String, c: String) =>
                   (a+b+c).substring(a.length, a.length+b.length) == b )
        }
      }
    SCALA
    define('foo')
    project('foo').test.invoke rescue
    project('foo').test.failed_tests.should include('StringSuite')
  end

end

elsif Buildr::VERSION >= '1.5'
  raise "JVM version guard in #{__FILE__} should be removed since it is assumed that Java 1.5 is no longer supported."
end
