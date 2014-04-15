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


module TestHelper
  def touch_last_successful_test_run(test_task, timestamp = Time.now)
    test_task.instance_eval do
      record_successful_run
      File.utime(timestamp, timestamp, last_successful_run_file)
    end
  end
end


describe Buildr::TestTask do
  def test_task
    @test_task ||= define('foo').test
  end

  it 'should respond to :compile and return compile task' do
    test_task.compile.should be_kind_of(Buildr::CompileTask)
  end

  it 'should respond to :compile and add sources to compile' do
    test_task.compile 'sources'
    test_task.compile.sources.should include('sources')
  end

  it 'should respond to :compile and add action for test:compile' do
    write 'src/test/java/Test.java', 'class Test {}'
    test_task.compile { task('action').invoke }
    lambda { test_task.compile.invoke }.should run_tasks('action')
  end

  it 'should execute compile tasks first' do
    write 'src/main/java/Nothing.java', 'class Nothing {}'
    write 'src/test/java/Test.java', 'class Test {}'
    define 'foo'
    lambda { project('foo').test.compile.invoke }.should run_tasks(['foo:compile', 'foo:test:compile'])
  end

  it 'should respond to :resources and return resources task' do
    test_task.resources.should be_kind_of(Buildr::ResourcesTask)
  end

  it 'should respond to :resources and add prerequisites to test:resources' do
    file('prereq').should_receive :invoke_prerequisites
    test_task.resources 'prereq'
    test_task.compile.invoke
  end

  it 'should respond to :resources and add action for test:resources' do
    task 'action'
    test_task.resources { task('action').invoke }
    lambda { test_task.resources.invoke }.should run_tasks('action')
  end

  it 'should respond to :setup and return setup task' do
    test_task.setup.name.should =~ /test:setup$/
  end

  it 'should respond to :setup and add prerequisites to test:setup' do
    test_task.setup 'prereq'
    test_task.setup.prerequisites.should include('prereq')
  end

  it 'should respond to :setup and add action for test:setup' do
    task 'action'
    test_task.setup { task('action').invoke }
    lambda { test_task.setup.invoke }.should run_tasks('action')
  end

  it 'should respond to :teardown and return teardown task' do
    test_task.teardown.name.should =~ /test:teardown$/
  end

  it 'should respond to :teardown and add prerequisites to test:teardown' do
    test_task.teardown 'prereq'
    test_task.teardown.prerequisites.should include('prereq')
  end

  it 'should respond to :teardown and add action for test:teardown' do
    task 'action'
    test_task.teardown { task('action').invoke }
    lambda { test_task.teardown.invoke }.should run_tasks('action')
  end

  it 'should respond to :with and return self' do
    test_task.with.should be(test_task)
  end

  it 'should respond to :with and add artifacfs to compile task dependencies' do
    test_task.with 'test.jar', 'acme:example:jar:1.0'
    test_task.compile.dependencies.should include(File.expand_path('test.jar'))
    test_task.compile.dependencies.should include(artifact('acme:example:jar:1.0'))
  end

  it 'should respond to :with and add artifacfs to task dependencies' do
    test_task.with 'test.jar', 'acme:example:jar:1.0'
    test_task.dependencies.should include(File.expand_path('test.jar'))
    test_task.dependencies.should include(artifact('acme:example:jar:1.0'))
  end
  
  it 'should response to :options and return test framework options' do
    test_task.using :foo=>'bar'
    test_task.options[:foo].should eql('bar')
  end

  it 'should respond to :using and return self' do
    test_task.using.should be(test_task)
  end

  it 'should respond to :using and set value options' do
    test_task.using('foo'=>'FOO', 'bar'=>'BAR')
    test_task.options[:foo].should eql('FOO')
    test_task.options[:bar].should eql('BAR')
  end

  it 'should respond to :using with deprecated parameter style and set value options to true, up to version 1.5 since this usage was deprecated in version 1.3' do
    Buildr::VERSION.should < '1.5'
    test_task.using('foo', 'bar')
    test_task.options[:foo].should eql(true)
    test_task.options[:bar].should eql(true)
  end

  it 'should start without pre-selected test framework' do
    test_task.framework.should be_nil
  end

  it 'should respond to :using and select test framework' do
    test_task.using(:testng)
    test_task.framework.should eql(:testng)
  end

  it 'should infer test framework from compiled language' do
    lambda { test_task.compile.using(:javac) }.should change { test_task.framework }.to(:junit)
  end

  it 'should respond to :include and return self' do
    test_task.include.should be(test_task)
  end

  it 'should respond to :include and add inclusion patterns' do
    test_task.include 'Foo', 'Bar'
    test_task.send(:include?, 'Foo').should be_true
    test_task.send(:include?, 'Bar').should be_true
  end

  it 'should respond to :exclude and return self' do
    test_task.exclude.should be(test_task)
  end

  it 'should respond to :exclude and add exclusion patterns' do
    test_task.exclude 'FooTest', 'BarTest'
    test_task.send(:include?, 'FooTest').should be_false
    test_task.send(:include?, 'BarTest').should be_false
    test_task.send(:include?, 'BazTest').should be_true
  end

  it 'should execute setup task before running tests' do
    mock = mock('actions')
    test_task.setup { mock.setup }
    test_task.enhance { mock.tests }
    mock.should_receive(:setup).ordered
    mock.should_receive(:tests).ordered
    test_task.invoke
  end

  it 'should execute teardown task after running tests' do
    mock = mock('actions')
    test_task.teardown { mock.teardown }
    test_task.enhance { mock.tests }
    mock.should_receive(:tests).ordered
    mock.should_receive(:teardown).ordered
    test_task.invoke
  end

  it 'should not execute teardown if setup failed' do
    test_task.setup { fail }
    lambda { test_task.invoke rescue nil }.should_not run_task(test_task.teardown)
  end

  it 'should use the main compile dependencies' do
    define('foo') { compile.using(:javac).with 'group:id:jar:1.0' }
    project('foo').test.dependencies.should include(artifact('group:id:jar:1.0'))
  end

  it 'should include the main compile target in its dependencies' do
    define('foo') { compile.using(:javac) }
    project('foo').test.dependencies.should include(project('foo').compile.target)
  end

  it 'should include the main compile target in its dependencies, even when using non standard directories' do
    write 'src/java/Nothing.java', 'class Nothing {}'
    define('foo') { compile path_to('src/java') }
    project('foo').test.dependencies.should include(project('foo').compile.target)
  end

  it 'should include the main resources target in its dependencies' do
    write 'src/main/resources/config.xml'
    define('foo').test.dependencies.should include(project('foo').resources.target)
  end

  it 'should not use the test compile dependencies' do
    define('foo') { test.compile.using(:javac).with 'group:id:jar:1.0' }
    project('foo').test.dependencies.should_not include(artifact('group:id:jar:1.0'))
  end

  it 'should include the test compile target in its dependencies' do
    define('foo') { test.compile.using(:javac) }
    project('foo').test.dependencies.should include(project('foo').test.compile.target)
  end

  it 'should include the test compile target in its dependencies, even when using non standard directories' do
    write 'src/test/Test.java', 'class Test {}'
    define('foo') { test.compile path_to('src/test') }
    project('foo').test.dependencies.should include(project('foo').test.compile.target)
  end

  it 'should add test compile target ahead of regular compile target' do
    write 'src/main/java/Code.java'
    write 'src/test/java/Test.java'
    define 'foo'
    depends = project('foo').test.dependencies
    depends.index(project('foo').test.compile.target).should < depends.index(project('foo').compile.target)
  end

  it 'should include the test resources target in its dependencies' do
    write 'src/test/resources/config.xml'
    define('foo').test.dependencies.should include(project('foo').test.resources.target)
  end

  it 'should add test resource target ahead of regular resource target' do
    write 'src/main/resources/config.xml'
    write 'src/test/resources/config.xml'
    define 'foo'
    depends = project('foo').test.dependencies
    depends.index(project('foo').test.resources.target).should < depends.index(project('foo').resources.target)
  end
  
  it 'should not have a last successful run timestamp before the tests are run' do
    test_task.timestamp.should == Rake::EARLY
  end

  it 'should clean after itself (test files)' do
    define('foo') { test.compile.using(:javac) }
    mkpath project('foo').test.compile.target.to_s
    lambda { task('clean').invoke }.should change { File.exist?(project('foo').test.compile.target.to_s) }.to(false)
  end

  it 'should clean after itself (reports)' do
    define 'foo'
    mkpath project('foo').test.report_to.to_s
    lambda { task('clean').invoke }.should change { File.exist?(project('foo').test.report_to.to_s) }.to(false)
  end
end


describe Buildr::TestTask, 'with no tests' do
  it 'should pass' do
    lambda { define('foo').test.invoke }.should_not raise_error
  end

  it 'should report no failed tests' do
    lambda { verbose(true) { define('foo').test.invoke } }.should_not show_error(/fail/i)
  end
  
  it 'should return no failed tests' do
    define('foo') { test.using(:junit) }
    project('foo').test.invoke
    project('foo').test.failed_tests.should be_empty
  end

  it 'should return no passing tests' do
    define('foo') { test.using(:junit) }
    project('foo').test.invoke
    project('foo').test.passed_tests.should be_empty
  end

  it 'should execute teardown task' do
    lambda { define('foo').test.invoke }.should run_task('foo:test:teardown')
  end
end


describe Buildr::TestTask, 'with passing tests' do
  def test_task
    @test_task ||= begin
      define 'foo' do
        test.using(:junit)
        test.instance_eval do
          @framework.stub!(:tests).and_return(['PassingTest1', 'PassingTest2'])
          @framework.stub!(:run).and_return(['PassingTest1', 'PassingTest2'])
        end
      end
      project('foo').test
    end
  end

  it 'should pass' do
    lambda { test_task.invoke }.should_not raise_error
  end

  it 'should report no failed tests' do
    lambda { verbose(true) { test_task.invoke } }.should_not show_error(/fail/i)
  end
  
  it 'should return passed tests' do
    test_task.invoke
    test_task.passed_tests.should == ['PassingTest1', 'PassingTest2']
  end

  it 'should return no failed tests' do
    test_task.invoke
    test_task.failed_tests.should be_empty
  end

  it 'should execute teardown task' do
    lambda { test_task.invoke }.should run_task('foo:test:teardown')
  end
  
  it 'should update the last successful run timestamp' do
    before = Time.now ; test_task.invoke ; after = Time.now
    (before-1..after+1).should include(test_task.timestamp)
  end
end


describe Buildr::TestTask, 'with failed test' do
  include TestHelper
  
  def test_task
    @test_task ||= begin
      define 'foo' do
        test.using(:junit)
        test.instance_eval do
          @framework.stub!(:tests).and_return(['FailingTest', 'PassingTest'])
          @framework.stub!(:run).and_return(['PassingTest'])
        end
      end
      project('foo').test
    end
  end

  it 'should fail' do
    lambda { test_task.invoke }.should raise_error(RuntimeError, /Tests failed/)
  end

  it 'should report failed tests' do
    lambda { verbose(true) { test_task.invoke rescue nil } }.should show_error(/FailingTest/)
  end

  it 'should return failed tests' do
    test_task.invoke rescue nil
    test_task.failed_tests.should == ['FailingTest']
  end

  it 'should return passing tests as well' do
    test_task.invoke rescue nil
    test_task.passed_tests.should == ['PassingTest']
  end

  it 'should not fail if fail_on_failure is false' do
    test_task.using(:fail_on_failure=>false).invoke
    lambda { test_task.invoke }.should_not raise_error
  end

  it 'should report failed tests even if fail_on_failure is false' do
    test_task.using(:fail_on_failure=>false)
    lambda { verbose(true) { test_task.invoke } }.should show_error(/FailingTest/)
  end

  it 'should return failed tests even if fail_on_failure is false' do
    test_task.using(:fail_on_failure=>false).invoke
    test_task.failed_tests.should == ['FailingTest']
  end

  it 'should execute teardown task' do
    lambda { test_task.invoke rescue nil }.should run_task('foo:test:teardown')
  end
  
  it 'should not update the last successful run timestamp' do
    a_second_ago = Time.now - 1
    touch_last_successful_test_run test_task, a_second_ago
    test_task.invoke rescue nil
    test_task.timestamp.should <= a_second_ago
  end
end


describe Buildr::Project, '#test' do
  it 'should return the project\'s test task' do
    define('foo') { test.should be(task('test')) }
  end

  it 'should accept prerequisites for task' do
    define('foo') { test 'prereq' }
    project('foo').test.prerequisites.should include('prereq')
  end

  it 'should accept actions for task' do
    task 'action'
    define('foo') { test { task('action').invoke } }
    lambda { project('foo').test.invoke }.should run_tasks('action')
  end

  it 'should set fail_on_failure true by default' do
    define('foo').test.options[:fail_on_failure].should be_true
  end

  it 'should set fork mode by default' do
    define('foo').test.options[:fork].should == :once
  end

  it 'should set properties to empty hash by default' do
    define('foo').test.options[:properties].should == {}
  end

  it 'should set environment variables to empty hash by default' do
    define('foo').test.options[:environment].should == {}
  end

  it 'should inherit options from parent project' do
    define 'foo' do
      test.using :fail_on_failure=>false, :fork=>:each, :properties=>{ :foo=>'bar' }, :environment=>{ 'config'=>'config.yaml' }
      define 'bar' do
        test.using :junit
        test.options[:fail_on_failure].should be_false
        test.options[:fork].should == :each
        test.options[:properties][:foo].should == 'bar'
        test.options[:environment]['config'].should == 'config.yaml'
      end
    end
  end

  it 'should clone options from parent project when using #using' do
    define 'foo' do
      define 'bar' do
        test.using :fail_on_failure=>false, :fork=>:each, :properties=>{ :foo=>'bar' }, :environment=>{ 'config'=>'config.yaml' }
        test.using :junit
      end.invoke
      test.options[:fail_on_failure].should be_true
      test.options[:fork].should == :once
      test.options[:properties].should == {}
      test.options[:environment].should == {}
    end
  end
  
  it 'should clone options from parent project when using #options' do
    define 'foo' do
      define 'bar' do
        test.options[:fail_on_failure] = false
        test.options[:fork] = :each
        test.options[:properties][:foo] = 'bar'
        test.options[:environment]['config'] = 'config.yaml'
        test.using :junit
      end.invoke
      test.options[:fail_on_failure].should be_true
      test.options[:fork].should == :once
      test.options[:properties].should == {}
      test.options[:environment].should == {}
    end
  end
  
  it 'should accept to set a test property in the top project' do
    define 'foo' do
        test.options[:properties][:foo] = 'bar'
    end
    project('foo').test.options[:properties][:foo].should == 'bar'
  end
  
  it 'should accept to set a test property in a subproject' do
    define 'foo' do
      define 'bar' do
        test.options[:properties][:bar] = 'baz'
      end
    end
    project('foo:bar').test.options[:properties][:bar].should == 'baz'
  end
  
  it 'should not change options of unrelated projects when using #options' do
    define 'foo' do
      test.options[:properties][:foo] = 'bar'
    end
    define 'bar' do
      test.options[:properties].should == {}
    end
  end
  
  it "should run from project's build task" do
    write 'src/main/java/Foo.java'
    write 'src/test/java/FooTest.java'
    define('foo')
    lambda { task('foo:build').invoke }.should run_task('foo:test')
  end
end


describe Buildr::Project, '#test.compile' do
  it 'should identify compiler from project' do
    write 'src/test/java/com/example/Test.java'
    define('foo') do
      test.compile.compiler.should eql(:javac)
    end
  end

  it 'should include identified sources' do
    write 'src/test/java/Test.java'
    define('foo') do
      test.compile.sources.should include(_('src/test/java'))
    end
  end

  it 'should compile to target/test/<code>' do
    define 'foo', :target=>'targeted' do
      test.compile.using(:javac)
      test.compile.target.should eql(file('targeted/test/classes'))
    end
  end

  it 'should use main compile dependencies' do
    define 'foo' do
      compile.using(:javac).with 'group:id:jar:1.0'
      test.compile.using(:javac)
    end
    project('foo').test.compile.dependencies.should include(artifact('group:id:jar:1.0'))
  end

  it 'should include the main compiled target in its dependencies' do
    define 'foo' do
      compile.using(:javac).into 'bytecode'
      test.compile.using(:javac)
    end
    project('foo').test.compile.dependencies.should include(file('bytecode'))
  end

  it 'should include the test framework dependencies' do
    define 'foo' do
      test.compile.using(:javac)
      test.using(:junit)
    end
    project('foo').test.compile.dependencies.should include(*artifacts(JUnit.dependencies))
  end

  it 'should clean after itself' do
    write 'src/test/java/Nothing.java', 'class Nothing {}'
    define('foo') { test.compile.into 'bytecode' }
    project('foo').test.compile.invoke
    lambda { project('foo').clean.invoke }.should change { File.exist?('bytecode') }.to(false)
  end
end


describe Buildr::Project, '#test.resources' do
  it 'should ignore resources unless they exist' do
    define('foo').test.resources.sources.should be_empty
    project('foo').test.resources.target.should be_nil
  end

  it 'should pick resources from src/test/resources if found' do
    mkpath 'src/test/resources'
    define('foo') { test.resources.sources.should include(file('src/test/resources')) }
  end

  it 'should copy to the resources target directory' do
    write 'src/test/resources/config.xml', '</xml>'
    define('foo', :target=>'targeted').test.invoke
    file('targeted/test/resources/config.xml').should contain('</xml>')
  end

  it 'should create target directory even if no files to copy' do
    define('foo').test.resources.filter.into('resources')
    lambda { file(File.expand_path('resources')).invoke }.should change { File.exist?('resources') }.to(true)
  end

  it 'should execute alongside compile task' do
    task 'action'
    define('foo') { test.resources { task('action').invoke } }
    lambda { project('foo').test.compile.invoke }.should run_tasks('action')
  end
end


describe Buildr::TestTask, '#invoke' do
  include TestHelper
  
  def test_task
    @test_task ||= define('foo') {
      test.using(:junit)
      test.instance_eval do
        @framework.stub!(:tests).and_return(['PassingTest'])
        @framework.stub!(:run).and_return(['PassingTest'])
      end
    }.test
  end
  
  it 'should require dependencies to exist' do
    lambda { test_task.with('no-such.jar').invoke }.should \
      raise_error(RuntimeError, /Don't know how to build/)
  end

  it 'should run all dependencies as prerequisites' do
    file(File.expand_path('no-such.jar')) { task('prereq').invoke }
    lambda { test_task.with('no-such.jar').invoke }.should run_tasks(['prereq', 'foo:test'])
  end

  it 'should run tests if they have never run' do
    lambda { test_task.invoke }.should run_task('foo:test')
  end
  
  it 'should not run tests if test option is off' do
    Buildr.options.test = false
    lambda { test_task.invoke }.should_not run_task('foo:test')
  end
  
  describe 'when there was a successful test run already' do
    before do
      @a_second_ago = Time.now - 1
      src = ['main/java/Foo.java', 'main/resources/config.xml', 'test/java/FooTest.java', 'test/resources/config-test.xml'].map { |f| File.join('src', f) }
      target = ['classes/Foo.class', 'resources/config.xml', 'test/classes/FooTest.class', 'test/resources/config-test.xml'].map { |f| File.join('target', f) }
      files = ['buildfile'] + src + target
      files.each { |file| write file }
      (files + files.map { |file| file.pathmap('%d') }).each { |file| File.utime(@a_second_ago, @a_second_ago, file) }
      touch_last_successful_test_run test_task, @a_second_ago
    end
    
    it 'should not run tests if nothing changed' do
      lambda { test_task.invoke }.should_not run_task('foo:test')
    end
    
    it 'should run tests if options.test is :all' do
      Buildr.options.test = :all
      lambda { test_task.invoke }.should run_task('foo:test')
    end
    
    it 'should run tests if main compile target changed' do
      touch project('foo').compile.target.to_s
      lambda { test_task.invoke }.should run_task('foo:test')
    end
    
    it 'should run tests if test compile target changed' do
      touch test_task.compile.target.to_s
      lambda { test_task.invoke }.should run_task('foo:test')
    end
    
    it 'should run tests if main resources changed' do
      touch project('foo').resources.target.to_s
      lambda { test_task.invoke }.should run_task('foo:test')
    end
    
    it 'should run tests if test resources changed' do
      touch test_task.resources.target.to_s
      lambda { test_task.invoke }.should run_task('foo:test')
    end
    
    it 'should run tests if compile-dependent project changed' do
      write 'bar/src/main/java/Bar.java', 'public class Bar {}'
      define('bar', :version=>'1.0', :base_dir=>'bar') { package :jar }
      project('foo').compile.with project('bar')
      lambda { test_task.invoke }.should run_task('foo:test')
    end
    
    it 'should run tests if test-dependent project changed' do
      write 'bar/src/main/java/Bar.java', 'public class Bar {}'
      define('bar', :version=>'1.0', :base_dir=>'bar') { package :jar }
      test_task.with project('bar')
      lambda { test_task.invoke }.should run_task('foo:test')
    end
    
    it 'should run tests if buildfile changed' do
      touch 'buildfile'
      lambda { test_task.invoke }.should run_task('foo:test')
    end
  end
end

describe Rake::Task, 'test' do
  it 'should be recursive' do
    define('foo') { define 'bar' }
    lambda { task('test').invoke }.should run_tasks('foo:test', 'foo:bar:test')
  end

  it 'should be local task' do
    define('foo') { define 'bar' }
    lambda do
      in_original_dir project('foo:bar').base_dir do
        task('test').invoke
      end
    end.should run_task('foo:bar:test').but_not('foo:test')
  end

  it 'should stop at first failure' do
    define('foo') { test { fail } }
    define('bar') { test { fail } }
    lambda { task('test').invoke rescue nil }.should run_tasks('foo:test').but_not('bar:test')
  end

  it 'should ignore failure if options.test is :all' do
    define('foo') { test { fail } }
    define('bar') { test { fail } }
    options.test = :all 
    lambda { task('test').invoke rescue nil }.should run_tasks('foo:test', 'bar:test')
  end

  it 'should ignore failure if environment variable test is \'all\'' do
    define('foo') { test { fail } }
    define('bar') { test { fail } }
    ENV['test'] = 'all'
    lambda { task('test').invoke rescue nil }.should run_tasks('foo:test', 'bar:test')
  end

  it 'should ignore failure if environment variable TEST is \'all\'' do
    define('foo') { test { fail } }
    define('bar') { test { fail } }
    ENV['TEST'] = 'all'
    lambda { task('test').invoke rescue nil }.should run_tasks('foo:test', 'bar:test')
  end

  it 'should execute no tests if options.test is false' do
    define('foo') { test { fail } }
    define('bar') { test { fail } }
    options.test = false
    lambda { task('test').invoke rescue nil }.should_not run_tasks('foo:test', 'bar:test')
  end

  it 'should execute no tests if environment variable test is \'no\'' do
    define('foo') { test { fail } }
    define('bar') { test { fail } }
    ENV['test'] = 'no'
    lambda { task('test').invoke rescue nil }.should_not run_tasks('foo:test', 'bar:test')
  end

  it 'should execute no tests if environment variable TEST is \'no\'' do
    define('foo') { test { fail } }
    define('bar') { test { fail } }
    ENV['TEST'] = 'no'
    lambda { task('test').invoke rescue nil }.should_not run_tasks('foo:test', 'bar:test')
  end
end


describe 'test rule' do
  include TestHelper
  
  it 'should execute test task on local project' do
    define('foo') { define 'bar' }
    lambda { task('test:something').invoke }.should run_task('foo:test')
  end

  it 'should reset tasks to specific pattern' do
    define 'foo' do
      test.using(:junit)
      test.instance_eval { @framework.stub!(:tests).and_return(['something', 'nothing']) }
      define 'bar' do
        test.using(:junit)
        test.instance_eval { @framework.stub!(:tests).and_return(['something', 'nothing']) }
      end
    end
    task('test:something').invoke
    ['foo', 'foo:bar'].map { |name| project(name) }.each do |project|
      project.test.tests.should include('something')
      project.test.tests.should_not include('nothing')
    end
  end

  it 'should apply *name* pattern' do
    define 'foo' do
      test.using(:junit)
      test.instance_eval { @framework.stub!(:tests).and_return(['prefix-something-suffix']) }
    end
    task('test:something').invoke
    project('foo').test.tests.should include('prefix-something-suffix')
  end

  it 'should not apply *name* pattern if asterisks used' do
    define 'foo' do
      test.using(:junit)
      test.instance_eval { @framework.stub!(:tests).and_return(['prefix-something', 'prefix-something-suffix']) }
    end
    task('test:*something').invoke
    project('foo').test.tests.should include('prefix-something')
    project('foo').test.tests.should_not include('prefix-something-suffix')
  end

  it 'should accept multiple tasks separated by commas' do
    define 'foo' do
      test.using(:junit)
      test.instance_eval { @framework.stub!(:tests).and_return(['foo', 'bar', 'baz']) }
    end
    task('test:foo,bar').invoke
    project('foo').test.tests.should include('foo')
    project('foo').test.tests.should include('bar')
    project('foo').test.tests.should_not include('baz')
  end

  it 'should execute only the named tests' do
    write 'src/test/java/TestSomething.java',
      'public class TestSomething extends junit.framework.TestCase { public void testNothing() {} }'
    write 'src/test/java/TestFails.java',
      'public class TestFails extends junit.framework.TestCase { public void testFailure() { fail(); } }'
    define 'foo'
    task('test:Something').invoke
  end
  
  it 'should execute the named tests even if the test task is not needed' do
    define 'foo' do
      test.using(:junit)
      test.instance_eval { @framework.stub!(:tests).and_return(['something', 'nothing']) }
    end
    touch_last_successful_test_run project('foo').test
    task('test:something').invoke
    project('foo').test.tests.should include('something')
  end
  
  it 'should not update the last successful test run timestamp' do
    define 'foo' do
      test.using(:junit)
      test.instance_eval { @framework.stub!(:tests).and_return(['something', 'nothing']) }
    end
    a_second_ago = Time.now - 1
    touch_last_successful_test_run project('foo').test, a_second_ago
    task('test:something').invoke
    project('foo').test.timestamp.should <= a_second_ago
  end
end


describe Buildr::Options, 'test' do
  it 'should be true by default' do
    Buildr.options.test.should be_true
  end

  ['skip', 'no', 'off', 'false'].each do |value|
    it "should be false if test environment variable is '#{value}'" do
      lambda { ENV['test'] = value }.should change { Buildr.options.test }.to(false)
    end
  end

  ['skip', 'no', 'off', 'false'].each do |value|
    it "should be false if TEST environment variable is '#{value}'" do
      lambda { ENV['TEST'] = value }.should change { Buildr.options.test }.to(false)
    end
  end

  it 'should be :all if test environment variable is all' do
    lambda { ENV['test'] = 'all' }.should change { Buildr.options.test }.to(:all)
  end

  it 'should be :all if TEST environment variable is all' do
    lambda { ENV['TEST'] = 'all' }.should change { Buildr.options.test }.to(:all)
  end

  it 'should be true and warn for any other value' do
    ENV['TEST'] = 'funky'
    lambda { Buildr.options.test.should be(true) }.should show_warning(/expecting the environment variable/i)
  end
end


describe Buildr, 'integration' do
  it 'should return the same task from all contexts' do
    task = task('integration')
    define 'foo' do
      integration.should be(task)
      define 'bar' do
        integration.should be(task)
      end
    end
    integration.should be(task)
  end

  it 'should respond to :setup and return setup task' do
    setup = integration.setup
    define('foo') { integration.setup.should be(setup) }
  end

  it 'should respond to :setup and add prerequisites to integration:setup' do
    define('foo') { integration.setup 'prereq' }
    integration.setup.prerequisites.should include('prereq')
  end

  it 'should respond to :setup and add action for integration:setup' do
    action = task('action')
    define('foo') { integration.setup { action.invoke } }
    lambda { integration.setup.invoke }.should run_tasks(action)
  end

  it 'should respond to :teardown and return teardown task' do
    teardown = integration.teardown
    define('foo') { integration.teardown.should be(teardown) }
  end

  it 'should respond to :teardown and add prerequisites to integration:teardown' do
    define('foo') { integration.teardown 'prereq' }
    integration.teardown.prerequisites.should include('prereq')
  end

  it 'should respond to :teardown and add action for integration:teardown' do
    action = task('action')
    define('foo') { integration.teardown { action.invoke } }
    lambda { integration.teardown.invoke }.should run_tasks(action)
  end
end


describe Rake::Task, 'integration' do
  it 'should be a local task' do
    define('foo') { test.using :integration }
    define('bar', :base_dir=>'other') { test.using :integration }
    lambda { task('integration').invoke }.should run_task('foo:test').but_not('bar:test')
  end

  it 'should be a recursive task' do
    define 'foo' do
      test.using :integration
      define('bar') { test.using :integration }
    end
    lambda { task('integration').invoke }.should run_tasks('foo:test', 'foo:bar:test')
  end

  it 'should find nested integration tests' do
    define 'foo' do
      define('bar') { test.using :integration }
    end
    lambda { task('integration').invoke }.should run_tasks('foo:bar:test').but_not('foo:test')
  end

  it 'should ignore nested regular tasks' do
    define 'foo' do
      test.using :integration
      define('bar') { test.using :integration=>false }
    end
    lambda { task('integration').invoke }.should run_tasks('foo:test').but_not('foo:bar:test')
  end

  it 'should agree not to run the same tasks as test' do
    define 'foo' do
      define 'bar' do
        test.using :integration
        define('baz') { test.using :integration=>false }
      end
    end
    lambda { task('test').invoke }.should run_tasks('foo:test', 'foo:bar:baz:test').but_not('foo:bar:test')
    lambda { task('integration').invoke }.should run_tasks('foo:bar:test').but_not('foo:test', 'foo:bar:baz:test')
  end

  it 'should run setup task before any project integration tests' do
    define('foo') { test.using :integration }
    define('bar') { test.using :integration }
    lambda { task('integration').invoke }.should run_tasks([integration.setup, 'bar:test'], [integration.setup, 'foo:test'])
  end

  it 'should run teardown task after all project integrations tests' do
    define('foo') { test.using :integration }
    define('bar') { test.using :integration }
    lambda { task('integration').invoke }.should run_tasks(['bar:test', integration.teardown], ['foo:test', integration.teardown])
  end

  it 'should run test cases marked for integration' do
    write 'src/test/java/FailingTest.java', 
      'public class FailingTest extends junit.framework.TestCase { public void testNothing() { assertTrue(false); } }'
    define('foo') { test.using :integration }
    lambda { task('test').invoke }.should_not raise_error
    lambda { task('integration').invoke }.should raise_error(RuntimeError, /tests failed/i)
  end

  it 'should run setup and teardown tasks marked for integration' do
    define('foo') { test.using :integration }
    lambda { task('test').invoke }.should run_tasks().but_not('foo:test:setup', 'foo:test:teardown')
    lambda { task('integration').invoke }.should run_tasks('foo:test:setup', 'foo:test:teardown')
  end

  it 'should run test actions marked for integration' do
    task 'action'
    define 'foo' do
      test.using :integration, :junit
    end
    lambda { task('test').invoke }.should_not change { project('foo').test.passed_tests }
    lambda { task('integration').invoke }.should change { project('foo').test.passed_tests }
    project('foo').test.passed_tests.should be_empty
  end

  it 'should not fail if test=all' do
    write 'src/test/java/FailingTest.java', 
      'public class FailingTest extends junit.framework.TestCase { public void testNothing() { assertTrue(false); } }'
    define('foo') { test.using :integration }
    options.test = :all
    lambda { task('integration').invoke }.should_not raise_error
  end

  it 'should execute by local package task' do
    define 'foo', :version=>'1.0' do
      test.using :integration
      package :jar
    end
    lambda { task('package').invoke }.should run_tasks(['foo:package', 'foo:test'])
  end

  it 'should execute by local package task along with unit tests' do
    define 'foo', :version=>'1.0' do
      test.using :integration
      package :jar
      define('bar') { test.using :integration=>false }
    end
    lambda { task('package').invoke }.should run_tasks(['foo:package', 'foo:test'],
      ['foo:bar:test', 'foo:bar:package'])
  end

  it 'should not execute by local package task if test=no' do
    define 'foo', :version=>'1.0' do
      test.using :integration
      package :jar
    end
    options.test = false
    lambda { task('package').invoke }.should run_task('foo:package').but_not('foo:test')
  end
end


describe 'integration rule' do
  it 'should execute integration tests on local project' do
    define 'foo' do
      test.using :junit, :integration
      define 'bar'
    end
    lambda { task('integration:something').invoke }.should run_task('foo:test')
  end

  it 'should reset tasks to specific pattern' do
    define 'foo' do
      test.using :junit, :integration
      test.instance_eval { @framework.stub!(:tests).and_return(['something', 'nothing']) }
      define 'bar' do
        test.using :junit, :integration
        test.instance_eval { @framework.stub!(:tests).and_return(['something', 'nothing']) }
      end
    end
    task('integration:something').invoke
    ['foo', 'foo:bar'].map { |name| project(name) }.each do |project|
      project.test.tests.should include('something')
      project.test.tests.should_not include('nothing')
    end
  end

  it 'should apply *name* pattern' do
    define 'foo' do
      test.using :junit, :integration
      test.instance_eval { @framework.stub!(:tests).and_return(['prefix-something-suffix']) }
    end
    task('integration:something').invoke
    project('foo').test.tests.should include('prefix-something-suffix')
  end

  it 'should not apply *name* pattern if asterisks used' do
    define 'foo' do
      test.using :junit, :integration
      test.instance_eval { @framework.stub!(:tests).and_return(['prefix-something', 'prefix-something-suffix']) }
    end
    task('integration:*something').invoke
    project('foo').test.tests.should include('prefix-something')
    project('foo').test.tests.should_not include('prefix-something-suffix')
  end

  it 'should accept multiple tasks separated by commas' do
    define 'foo' do
      test.using :junit, :integration
      test.instance_eval { @framework.stub!(:tests).and_return(['foo', 'bar', 'baz']) }
    end
    task('integration:foo,bar').invoke
    project('foo').test.tests.should include('foo')
    project('foo').test.tests.should include('bar')
    project('foo').test.tests.should_not include('baz')
  end

  it 'should execute only the named tests' do
    write 'src/test/java/TestSomething.java',
      'public class TestSomething extends junit.framework.TestCase { public void testNothing() {} }'
    write 'src/test/java/TestFails.java',
      'public class TestFails extends junit.framework.TestCase { public void testFailure() { fail(); } }'
    define('foo') { test.using :junit, :integration }
    task('integration:Something').invoke
  end
end
