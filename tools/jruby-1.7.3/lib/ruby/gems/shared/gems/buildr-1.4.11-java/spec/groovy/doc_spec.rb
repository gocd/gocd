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

describe "Groovydoc" do

  it 'should pick :windowtitle from project name by default' do
    define 'foo' do
      doc.using :groovydoc

      define 'bar' do
        doc.using :groovydoc
      end
    end

    project('foo').doc.options[:windowtitle].should eql('foo')
    project('foo:bar').doc.options[:windowtitle].should eql('foo:bar')
  end

  it 'should pick :windowtitle from project description by default, if available' do
    desc 'My App'
    define 'foo' do
      doc.using :groovydoc
    end
    project('foo').doc.options[:windowtitle].should eql('My App')
  end

  it 'should not override explicit :windowtitle option' do
    define 'foo' do
      doc.using :groovydoc
      doc.using :windowtitle => 'explicit'
    end
    project('foo').doc.options[:windowtitle].should eql('explicit')
  end

  it 'should identify itself from groovy source directories' do
    write 'src/main/groovy/some/A.java', 'package some; public class A {}'
    write 'src/main/groovy/some/B.groovy', 'package some; public class B {}'
    define('foo') do
      doc.engine.should be_a(Buildr::Doc::Groovydoc)
    end
  end

  it 'should produce Groovydocs' do
    write 'src/main/groovy/some/A.java', 'package some; public class A {}'
    write 'src/main/groovy/some/B.groovy', 'package some; public class B {}'
    define('foo')
    project('foo').doc.invoke
    file('target/doc/index.html').should exist
  end
end
