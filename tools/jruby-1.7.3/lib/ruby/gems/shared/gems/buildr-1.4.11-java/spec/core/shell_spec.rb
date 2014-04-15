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

describe Project, '.shell' do

  it 'should return the project\'s shell task' do
    define('foo')
    project('foo').shell.name.should eql('foo:shell')
  end

  it 'should return a ShellTask' do
    define('foo')
    project('foo').shell.should be_kind_of(Shell::ShellTask)
  end

  it 'should include compile and test.compile dependencies' do
    define('foo') do
      compile.using(:javac).with 'group:compile:jar:1.0'
      test.compile.using(:javac).with 'group:test:jar:1.0'
    end
    project('foo').shell.classpath.should include(artifact('group:compile:jar:1.0'))
    project('foo').shell.classpath.should include(artifact('group:test:jar:1.0'))
  end

  it 'should respond to using() and return self' do
    define 'foo' do
      shell.using(:foo=>'Fooing').should be(shell)
    end
  end

  it 'should respond to using() and accept options' do
    define 'foo' do
      shell.using :foo=>'Fooing'
    end
    project('foo').shell.options[:foo].should eql('Fooing')
  end

  it 'should select provider using shell.using' do
    define 'foo' do
      shell.using :bsh
    end
    project('foo').shell.provider.should be_a(Shell::BeanShell)
  end

  it 'should select runner based on compile language' do
    write 'src/main/java/Test.java', 'class Test {}'
    define 'foo' do
      # compile language detected as :java
    end
    project('foo').shell.provider.should be_a(Shell::BeanShell)
  end

  it 'should depend on project''s compile task' do
    define 'foo'
    project('foo').shell.prerequisites.should include(project('foo').compile)
  end

  it 'should be local task' do
    define 'foo' do
      define('bar') do
        shell.using :bsh
      end
    end
    task = project('foo:bar').shell
    task.should_receive(:invoke_prerequisites)
    task.should_receive(:run)
    in_original_dir(project('foo:bar').base_dir) { task('shell').invoke }
  end

  it 'should not recurse' do
    define 'foo' do
      shell.using :bsh
      define('bar') { shell.using :bsh }
    end
    project('foo:bar').shell.should_not_receive(:invoke_prerequisites)
    project('foo:bar').shell.should_not_receive(:run)
    project('foo').shell.should_receive(:run)
    project('foo').shell.invoke
  end

  it 'should call shell provider with task configuration' do
    define 'foo' do
      shell.using :bsh
    end
    shell = project('foo').shell
    shell.provider.should_receive(:launch).with(shell)
    project('foo').shell.invoke
  end
end

shared_examples_for "shell provider" do

  it 'should have launch method accepting shell task' do
    @instance.method(:launch).should_not be_nil
    @instance.method(:launch).arity.should === 1
  end

end

Shell.providers.each do |provider|
  describe provider do
    before do
      @provider = provider
      @project = define('foo') {}
      @instance = provider.new(@project)
      @project.shell.using @provider.to_sym
    end

    it_should_behave_like "shell provider"

    it 'should call Java::Commands.java with :java_args' do
      @project.shell.using :java_args => ["-Xx"]
      Java::Commands.should_receive(:java).with do |*args|
        args.last.should be_a(Hash)
        args.last.keys.should include(:java_args)
        args.last[:java_args].should include('-Xx')
      end
      project('foo').shell.invoke
    end

    it 'should call Java::Commands.java with :properties' do
      @project.shell.using :properties => {:foo => "bar"}
      Java::Commands.should_receive(:java).with do |*args|
        args.last.should be_a(Hash)
        args.last.keys.should include(:properties)
        args.last[:properties][:foo].should == "bar"
      end
      project('foo').shell.invoke
    end
  end
end

