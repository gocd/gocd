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

describe Project, :run do

  it 'should return the project\'s run task' do
    define('foo')
    project('foo').run.name.should eql('foo:run')
  end

  it 'should return a RunTask' do
    define('foo')
    project('foo').run.should be_kind_of(Run::RunTask)
  end

  it 'should include compile dependencies' do
    define('foo') do
      compile.using(:javac).with 'group:compile:jar:1.0'
      test.compile.using(:javac).with 'group:test:jar:1.0'
    end
    project('foo').run.classpath.should include(artifact('group:compile:jar:1.0'))
  end
  
  it 'should not include test dependencies' do
    define('foo') do
      compile.using(:javac).with 'group:compile:jar:1.0'
      test.compile.using(:javac).with 'group:test:jar:1.0'
    end
    project('foo').run.classpath.should_not include(artifact('group:test:jar:1.0'))
  end

  it 'should respond to using() and return self' do
    define 'foo' do
      run.using(:foo=>'Fooing').should be(run)
    end
  end

  it 'should respond to using() and accept options' do
    define 'foo' do
      run.using :foo=>'Fooing'
    end
    project('foo').run.options[:foo].should eql('Fooing')
  end

  it 'should select runner using run.using' do
    define 'foo' do
      run.using :java
    end
    project('foo').run.runner.should be_a(Run::JavaRunner)
  end

  it 'should select runner based on compile language' do
    write 'src/main/java/Test.java', 'class Test {}'
    define 'foo' do
      # compile language detected as :java
    end
    project('foo').run.runner.should be_a(Run::JavaRunner)
  end
  
  it "should run with the project resources" do
    write 'src/main/java/Test.java', 'class Test {}'
    write 'src/main/resources/test.properties', ''
    define 'foo'
    project('foo').run.classpath.should include project('foo').resources.target
  end

  it 'should depend on project''s compile task' do
    define 'foo'
    project('foo').run.prerequisites.should include(project('foo').compile)
  end

  it 'should be local task' do
    define 'foo' do
      define('bar')
    end
    project('foo:bar').run.should_receive(:invoke_prerequisites)
    project('foo:bar').run.should_receive(:run)
    in_original_dir(project('foo:bar').base_dir) { task('run').invoke }
  end

  it 'should not recurse' do
    define 'foo' do
      define('bar') { run.using :java, :main => 'foo' }
    end
    project('foo:bar').run.should_not_receive(:invoke_prerequisites)
    project('foo:bar').run.should_not_receive(:run)
    project('foo').run.should_receive(:run)
    project('foo').run.invoke
  end

end

