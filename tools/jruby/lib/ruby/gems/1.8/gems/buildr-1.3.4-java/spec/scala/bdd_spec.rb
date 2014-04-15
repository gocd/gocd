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

describe Buildr::Scala::Specs do
  it 'should be the default when tests in src/spec/scala' do
    write 'src/spec/scala/com/example/MySpecs.scala', <<-SCALA
      package com.example
      object MySpecs extends org.specs.Specification {
        "it" should {
          "add" in {
            val sum = 1 + 1
            sum mustEqual 2
          }
        }
      }
    SCALA
    define 'foo'
    project('foo').test.framework.should eql(:specs)
  end
  
  it 'should include Specs dependencies' do
    define('foo') { test.using(:specs) }
    project('foo').test.compile.dependencies.should include(*artifacts(Scala::Specs.dependencies))
    project('foo').test.dependencies.should include(*artifacts(Scala::Specs.dependencies))
  end
  
  it 'should include ScalaCheck dependencies' do
    define('foo') { test.using(:specs) }
    project('foo').test.compile.dependencies.should include(*artifacts(Scala::Check.dependencies))
    project('foo').test.dependencies.should include(*artifacts(Scala::Check.dependencies))
  end

  it 'should include JMock dependencies' do
    define('foo') { test.using(:scalatest) }
    project('foo').test.compile.dependencies.should include(*artifacts(JMock.dependencies))
    project('foo').test.dependencies.should include(*artifacts(JMock.dependencies))
  end

  it 'should include public classes extending org.specs.Specification' do
    write 'src/spec/scala/com/example/MySpecs.scala', <<-SCALA
      package com.example
      object MySpecs extends org.specs.Specification {
        "it" should {
          "add" in {
            val sum = 1 + 1
            sum mustEqual 2
          }
        }
      }
    SCALA
    define('foo').test.invoke
    project('foo').test.tests.should include('com.example.MySpecs')
  end

  it 'should pass when spec passes' do
    write 'src/spec/scala/PassingSpecs.scala', <<-SCALA
      object PassingSpecs extends org.specs.Specification {
        "it" should {
          "add" in {
            val sum = 1 + 1
            sum mustEqual 2
          }
        }
      }
    SCALA
    lambda { define('foo').test.invoke }.should_not raise_error
  end

  it 'should fail when ScalaTest test case fails' do
    write 'src/spec/scala/FailingSpecs.scala', <<-SCALA
      object FailingSpecs extends org.specs.Specification {
        "it" should {
          "add" in {
            val sum = 1 + 1
            sum mustEqual 42
          }
        }
      }
    SCALA
    lambda { define('foo').test.invoke }.should raise_error(RuntimeError, /Tests failed/) rescue nil
  end

  it 'should report failed test names' do
    write 'src/spec/scala/FailingSpecs.scala', <<-SCALA
      object FailingSpecs extends org.specs.Specification {
        "it" should {
          "add" in {
            val sum = 1 + 1
            sum mustEqual 42
          }
        }
      }
    SCALA
    define('foo').test.invoke rescue
    project('foo').test.failed_tests.should include('FailingSpecs')
  end
  
  it 'should compile and run specifications with "Specs" suffix' do
    write 'src/spec/scala/HelloWorldSpecs.scala', <<-SCALA
      import org.specs._
  
      object HelloWorldSpecs extends Specification {
        "'hello world' has 11 characters" in {
          "hello world".size must beEqualTo(11)
        }
        "'hello world' matches 'h.* w.*'" in {
          "hello world" must beMatching("h.* w.*")
        }
      }
    SCALA
    define('foo')
    project('foo').test.invoke
    project('foo').test.passed_tests.should include('HelloWorldSpecs')
  end


  it 'should fail if specifications fail' do
    write 'src/spec/scala/StringSpecs.scala', <<-SCALA
      import org.specs._
      import org.specs.runner._
      
      object StringSpecs extends Specification {
        "empty string" should {
          "have a zero length" in {
            ("".length) mustEqual(1)
          }
        }
      }
    SCALA
    define('foo')
    project('foo').test.invoke rescue
    project('foo').test.failed_tests.should include('StringSpecs')
  end

end
