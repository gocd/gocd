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

describe "Scaladoc" do

  it 'should pick -doc-title from project name by default' do
    define 'foo' do
      compile.using(:scalac)

      define 'bar' do
        compile.using(:scalac)
      end
    end

    project('foo').doc.options[:"doc-title"].should eql('foo')
    project('foo:bar').doc.options[:"doc-title"].should eql('foo:bar')
  end

  it 'should pick -doc-title from project description by default, if available' do
    desc 'My App'
    define 'foo' do
      compile.using(:scalac)
    end
    project('foo').doc.options[:"doc-title"].should eql('My App')
  end

  it 'should not override explicit "doc-title" option' do
    define 'foo' do
      compile.using(:scalac)
      doc.using "doc-title" => 'explicit'
    end
    project('foo').doc.options[:"doc-title"].should eql('explicit')
  end

if Java.java.lang.System.getProperty("java.runtime.version") >= "1.6"

  it 'should convert :windowtitle to -doc-title for Scala 2.8.1 and later' do
    write 'src/main/scala/com/example/Test.scala', 'package com.example; class Test { val i = 1 }'
    define('foo') do
      doc.using :windowtitle => "foo"
    end
    actual = Java.scala.tools.nsc.ScalaDoc.new
    scaladoc = Java.scala.tools.nsc.ScalaDoc.new
    Java.scala.tools.nsc.ScalaDoc.should_receive(:new) do
      scaladoc
    end
    scaladoc.should_receive(:process) do |args|
      # Convert Java Strings to Ruby Strings, if needed.
      xargs = args.map { |a| a.is_a?(String) ? a : a.toString }
      xargs.should include("-doc-title")
      xargs.should_not include("-windowtitle")
      actual.process(args).should eql(true)
    end
    project('foo').doc.invoke
  end unless Buildr::Scala.version?(2.7, "2.8.0")

elsif Buildr::VERSION >= '1.5'
  raise "JVM version guard in #{__FILE__} should be removed since it is assumed that Java 1.5 is no longer supported."
end

end

if Java.java.lang.System.getProperty("java.runtime.version") >= "1.6"

describe "package(:scaladoc)" do
  it "should generate target/project-version-scaladoc.jar" do
    write 'src/main/scala/Foo.scala', 'class Foo'
    define 'foo', :version=>'1.0' do
      package(:scaladoc)
    end

    scaladoc = project('foo').package(:scaladoc)
    scaladoc.should point_to_path('target/foo-1.0-scaladoc.jar')

    lambda {
      project('foo').task('package').invoke
    }.should change { File.exist?('target/foo-1.0-scaladoc.jar') }.to(true)

    scaladoc.should exist
    scaladoc.should contain('index.html')
    scaladoc.should contain('Foo.html')
  end
end

elsif Buildr::VERSION >= '1.5'
  raise "JVM version guard in #{__FILE__} should be removed since it is assumed that Java 1.5 is no longer supported."
end
