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


describe Project do
  it 'should be findable' do
    foo = define('foo')
    project('foo').should be(foo)
  end

  it 'should not exist unless defined' do
    lambda { project('foo') }.should raise_error(RuntimeError, /No such project/)
  end

  it 'should fail to be defined if its name is already used for a task' do
    lambda { define('test') }.should raise_error(RuntimeError, /Invalid project name/i)
    define 'valid' do
      lambda { define('build') }.should raise_error(RuntimeError, /Invalid project name/i)
    end
  end

  it 'should exist once defined' do
    define 'foo'
    lambda { project('foo') }.should_not raise_error
  end

  it 'should always return same project for same name' do
    foo, bar = define('foo'), define('bar')
    foo.should_not be(bar)
    foo.should be(project('foo'))
    bar.should be(project('bar'))
  end

  it 'should show up in projects list if defined' do
    define('foo')
    projects.map(&:name).should include('foo')
  end

  it 'should not show up in projects list unless defined' do
    projects.map(&:name).should_not include('foo')
  end

  it 'should be findable from within a project' do
    define('foo')
    project('foo').project('foo').should be(project('foo'))
  end

  it 'should cease to exist when project list cleared' do
    define 'foo'
    projects.map(&:name).should include('foo')
    Project.clear
    projects.map(&:name).should be_empty
  end

  it 'should be defined only once' do
    lambda { define 'foo' }.should_not raise_error
    lambda { define 'foo' }.should raise_error
  end

  it 'should be definable in any order' do
    Buildr.define('baz') { define('bar') { project('foo:bar') } }
    Buildr.define('foo') { define('bar') }
    lambda { project('foo') }.should_not raise_error
  end

  it 'should detect circular dependency' do
    Buildr.define('baz') { define('bar') { project('foo:bar') } }
    Buildr.define('foo') { define('bar') { project('baz:bar') } }
    lambda { project('foo') }.should raise_error(RuntimeError, /Circular dependency/)
  end
end

describe Project, ' property' do
  it 'should be set if passed as argument' do
    define 'foo', 'version'=>'1.1'
    project('foo').version.should eql('1.1')
  end

  it 'should be set if assigned in body' do
    define('foo') { self.version = '1.2' }
    project('foo').version.should eql('1.2')
  end

  it 'should take precedence when assigned in body' do
    define('foo', 'version'=>'1.1') { self.version = '1.2' }
    project('foo').version.should eql('1.2')
  end

  it 'should inherit from parent (for some properties)' do
    define('foo', 'version'=>'1.2', :group=>'foobar') { define 'bar' }
    project('foo:bar').version.should eql('1.2')
    project('foo:bar').group.should eql('foobar')
  end

  it 'should have different value if set in sub-project' do
    define 'foo', 'version'=>'1.2', :group=>'foobar' do
      define 'bar', :version=>'1.3' do
        self.group = 'barbaz'
      end
    end
    project('foo:bar').version.should eql('1.3')
    project('foo:bar').group.should eql('barbaz')
  end
end


describe Project, ' block' do
  it 'should execute once' do
    define('foo') { self.name.should eql('foo') }
  end

  it 'should execute in describe of project' do
    define('foo') { self.version = '1.3' }
    project('foo').version.should eql('1.3')
  end

  it 'should execute by passing project' do
    define('foo') { |project| project.version = '1.3' }
    project('foo').version.should eql('1.3')
  end

  it 'should execute in namespace of project' do
    define('foo') { define('bar') { Buildr.application.current_scope.should eql(['foo', 'bar']) } }
  end
end


describe Project, '#base_dir' do
  it 'should be pwd if not specified' do
    define('foo').base_dir.should eql(Dir.pwd)
  end

  it 'should come from property, if specified' do
    foo = define('foo', :base_dir=>'tmp')
    foo.base_dir.should point_to_path('tmp')
  end

  it 'should be expanded path' do
    foo = define('foo', :base_dir=>'tmp')
    foo.base_dir.should eql(File.expand_path('tmp'))
  end

  it 'should be relative to parent project' do
    define('foo') { define('bar') { define 'baz' } }
    project('foo:bar:baz').base_dir.should point_to_path('bar/baz')
  end

  it 'should be settable only if not read' do
    lambda { define('foo', :base_dir=>'tmp') }.should_not raise_error
    lambda { define('bar', :base_dir=>'tmp') { self.base_dir = 'bar' } }.should raise_error(Exception, /Cannot set/)
  end
end


describe Layout do
  before :each do
    @layout = Layout.new
  end

  it 'should expand empty to itself' do
    @layout.expand.should eql('')
    @layout.expand('').should eql('')
  end

  it 'should expand array of symbols' do
    @layout.expand(:foo, :bar).should eql('foo/bar')
  end

  it 'should expand array of names' do
    @layout.expand('foo', 'bar').should eql('foo/bar')
  end

  it 'should map symbol to path' do
    @layout[:foo] = 'baz'
    @layout.expand(:foo, :bar).should eql('baz/bar')
  end

  it 'should map symbols to path' do
    @layout[:foo, :bar] = 'none'
    @layout.expand(:foo, :bar).should eql('none')
  end

  it 'should map strings to path' do
    @layout[:foo, "bar"] = 'none'
    @layout.expand(:foo, :bar).should eql('none')
    @layout.expand(:foo, 'bar').should eql('none')
  end

  it 'should ignore nil elements' do
    @layout[:foo, :bar] = 'none'
    @layout.expand(:foo, nil, :bar).should eql('none')
    @layout.expand(nil, :foo).should eql('foo')
  end

  it 'should return nil if path not mapped' do
    @layout[:foo].should be_nil
  end

  it 'should return path from symbol' do
    @layout[:foo] = 'path'
    @layout[:foo].should eql('path')
  end

  it 'should return path from symbol' do
    @layout[:foo, :bar] = 'path'
    @layout[:foo, :bar].should eql('path')
  end

  it 'should do eager mapping' do
    @layout[:one] = 'none'
    @layout[:one, :two] = '1..2'
    @layout.expand(:one, :two, :three).should eql('1..2/three')
  end

end


describe Project, '#layout' do
  before :each do
    @layout = Layout.new
  end

  it 'should exist by default' do
    define('foo').layout.should respond_to(:expand)
  end

  it 'should be clone of default layout' do
    define 'foo' do
      layout.should_not be(Layout.default)
      layout.expand(:test, :main).should eql(Layout.default.expand(:test, :main))
    end
  end

  it 'should come from property, if specified' do
    foo = define('foo', :layout=>@layout)
    foo.layout.should eql(@layout)
  end

  it 'should inherit from parent project' do
    define 'foo', :layout=>@layout do
      layout[:foo] = 'foo'
      define 'bar'
    end
    project('foo:bar').layout[:foo].should eql('foo')
  end

  it 'should clone when inheriting from parent project' do
    define 'foo', :layout=>@layout do
      layout[:foo] = 'foo'
      define 'bar' do
        layout[:foo] = 'bar'
      end
    end
    project('foo').layout[:foo].should eql('foo')
    project('foo:bar').layout[:foo].should eql('bar')
  end

  it 'should be settable only if not read' do
    lambda { define('foo', :layout=>@layout) }.should_not raise_error
    lambda { define('bar', :layout=>@layout) { self.layout = @layout.clone } }.should raise_error(Exception, /Cannot set/)
  end

end


describe Project, '#path_to' do
  it 'should return absolute paths as is' do
    define('foo').path_to('/tmp').should eql(File.expand_path('/tmp'))
  end

  it 'should resolve empty path to project\'s base directory' do
    define('foo').path_to.should eql(project('foo').base_dir)
  end

  it 'should resolve relative paths' do
    define('foo').path_to('tmp').should eql(File.expand_path('tmp'))
  end

  it 'should accept multiple arguments' do
    define('foo').path_to('foo', 'bar').should eql(File.expand_path('foo/bar'))
  end

  it 'should handle relative paths' do
    define('foo').path_to('..', 'bar').should eql(File.expand_path('../bar'))
  end

  it 'should resolve symbols using layout' do
    define('foo').layout[:foo] = 'bar'
    project('foo').path_to(:foo).should eql(File.expand_path('bar'))
    project('foo').path_to(:foo, 'tmp').should eql(File.expand_path('bar/tmp'))
  end

  it 'should resolve path for sub-project' do
    define('foo') { define 'bar' }
    project('foo:bar').path_to('foo').should eql(File.expand_path('foo', project('foo:bar').base_dir))
  end

  it 'should be idempotent for relative paths' do
    define 'foo'
    path = project('foo').path_to('bar')
    project('foo').path_to(path).should eql(path)
  end
end


describe Project, '#on_define' do
  it 'should be called when project is defined' do
    names = []
    Project.on_define { |project| names << project.name }
    define 'foo' ; define 'bar'
    names.should eql(['foo', 'bar'])
  end

  it 'should be called with project object' do
    Project.on_define { |project| project.name.should eql('foo') }
    define('foo')
  end

  it 'should be called with project object and set properties' do
    Project.on_define { |project| project.version.should eql('2.0') }
    define('foo', :version=>'2.0')
  end

  it 'should execute in namespace of project' do
    scopes = []
    Project.on_define { |project| scopes << Buildr.application.current_scope }
    define('foo') { define 'bar' }
    scopes.should eql([['foo'], ['foo', 'bar']])
  end

  it 'should be called before project block' do
    order = []
    Project.on_define { |project| order << 'on_define' }
    define('foo') { order << 'define' }
    order.should eql(['on_define', 'define'])
  end

  it 'should accept enhancement and call it after project block' do
    order = []
    Project.on_define { |project| project.enhance { order << 'enhance' } }
    define('foo') { order << 'define' }
    order.should eql(['define', 'enhance'])
  end

  it 'should accept enhancement and call it with project' do
    Project.on_define { |project| project.enhance { |project| project.name.should eql('foo') } }
    define('foo')
  end

  it 'should execute enhancement in namespace of project' do
    scopes = []
    Project.on_define { |project| project.enhance { scopes << Buildr.application.current_scope } }
    define('foo') { define 'bar' }
    scopes.should eql([['foo'], ['foo', 'bar']])
  end

  it 'should be removed in version 1.5 since it was deprecated in version 1.3' do
    Buildr::VERSION.should < '1.5'
  end
end


describe Rake::Task, ' recursive' do
  before do
    @order = []
    Project.on_define do |project| # TODO on_define is deprecated
      project.recursive_task('doda') { @order << project.name }
    end
    define('foo') { define('bar') { define('baz') } }
  end

  it 'should invoke same task in child project' do
    task('foo:doda').invoke
    @order.should include('foo:bar:baz')
    @order.should include('foo:bar')
    @order.should include('foo')
  end

  it 'should invoke in depth-first order' do
    task('foo:doda').invoke
    @order.should eql([ 'foo:bar:baz', 'foo:bar', 'foo' ])
  end

  it 'should not invoke task in parent project' do
    task('foo:bar:baz:doda').invoke
    @order.should eql([ 'foo:bar:baz' ])
  end
end


describe 'Sub-project' do
  it 'should point at parent project' do
    define('foo') { define 'bar' }
    project('foo:bar').parent.should be(project('foo'))
  end

  it 'should be defined only within parent project' do
    lambda { define('foo:bar') }.should raise_error
  end

  it 'should have unique name' do
    lambda do
      define 'foo' do
        define 'bar'
        define 'bar'
      end
    end.should raise_error
  end

  it 'should be findable from root' do
    define('foo') { define 'bar' }
    projects.map(&:name).should include('foo:bar')
  end

  it 'should be findable from parent project' do
    define('foo') { define 'bar' }
    project('foo').projects.map(&:name).should include('foo:bar')
  end

  it 'should be findable during project definition' do
    define 'foo' do
      bar = define 'bar' do
        baz = define 'baz'
        project('baz').should eql(baz)
      end
      # Note: evaluating bar:baz first unearthed a bug that doesn't happen
      # if we evaluate bar, then bar:baz.
      project('bar:baz').should be(bar.project('baz'))
      project('bar').should be(bar)
    end
  end

  it 'should be findable only if exists' do
    define('foo') { define 'bar' }
    lambda { project('foo').project('baz') }.should raise_error(RuntimeError, /No such project/)
  end

  it 'should always execute its definition ' do
    ordered = []
    define 'foo' do
      ordered << self.name
      define('bar') { ordered << self.name }
      define('baz') { ordered << self.name }
    end
    ordered.should eql(['foo', 'foo:bar', 'foo:baz'])
  end

  it 'should execute in order of dependency' do
    ordered = []
    define 'foo' do
      ordered << self.name
      define('bar') { project('foo:baz') ; ordered << self.name }
      define('baz') { ordered << self.name }
    end
    ordered.should eql(['foo', 'foo:baz', 'foo:bar'])
  end

  it 'should warn of circular dependency' do
    lambda do
      define 'foo' do
        define('bar') { project('foo:baz') }
        define('baz') { project('foo:bar') }
      end
    end.should raise_error(RuntimeError, /Circular dependency/)
  end
end


describe 'Top-level project' do
  it 'should have no parent' do
    define('foo')
    project('foo').parent.should be_nil
  end
end


describe Buildr, '#project' do
  it 'should raise error if no such project' do
    lambda { project('foo') }.should raise_error(RuntimeError, /No such project/)
  end

  it 'should return a project if exists' do
    foo = define('foo')
    project('foo').should be(foo)
  end

  it 'should define a project if a block is given' do
    foo = project('foo') {}
    project('foo').should be(foo)
  end

  it 'should define a project if properties and a block are given' do
    foo = project('foo', :version => '1.2') {}
    project('foo').should be(foo)
  end

  it 'should find a project by its full name' do
    bar, baz = nil
    define('foo') { bar = define('bar') { baz = define('baz')  } }
    project('foo:bar').should be(bar)
    project('foo:bar:baz').should be(baz)
  end

  it 'should find a project from any context' do
    bar, baz = nil
    define('foo') { bar = define('bar') { baz = define('baz')  } }
    project('foo:bar').project('foo:bar:baz').should be(baz)
    project('foo:bar:baz').project('foo:bar').should be(bar)
  end

  it 'should find a project from its parent or sibling project' do
    define 'foo' do
      define 'bar'
      define 'baz'
    end
    project('foo').project('bar').should be(project('foo:bar'))
    project('foo').project('baz').should be(project('foo:baz'))
    project('foo:bar').project('baz').should be(project('foo:baz'))
  end

  it 'should fine a project from its parent by proximity' do
    define 'foo' do
      define('bar') { define 'baz' }
      define 'baz'
    end
    project('foo').project('baz').should be(project('foo:baz'))
    project('foo:bar').project('baz').should be(project('foo:bar:baz'))
  end

  it 'should invoke project before returning it' do
    define('foo').should_receive(:invoke).once
    project('foo')
  end

  it 'should fail if called without a project name' do
    lambda { project }.should raise_error(ArgumentError)
  end

  it 'should return self if called on a project without a name' do
    define('foo') { project.should be(self) }
  end

  it 'should evaluate parent project before returning' do
    # Note: gets around our define that also invokes the project.
    Buildr.define('foo') { define('bar'); define('baz') }
    project('foo:bar').should eql(projects[1])
  end
end


describe Buildr, '#projects' do
  it 'should only return defined projects' do
    projects.should eql([])
    define 'foo'
    projects.should eql([project('foo')])
  end

  it 'should return all defined projects' do
    define 'foo'
    define('bar') { define 'baz' }
    projects.should include(project('foo'))
    projects.should include(project('bar'))
    projects.should include(project('bar:baz'))
  end

  it 'should return only named projects' do
    define 'foo' ; define 'bar' ; define 'baz'
    projects('foo', 'bar').should include(project('foo'))
    projects('foo', 'bar').should include(project('bar'))
    projects('foo', 'bar').should_not include(project('baz'))
  end

  it 'should complain if named project does not exist' do
    define 'foo'
    projects('foo').should include(project('foo'))
    lambda { projects('bar') }.should raise_error(RuntimeError, /No such project/)
  end

  it 'should find a project from its parent or sibling project' do
    define 'foo' do
      define 'bar'
      define 'baz'
    end
    project('foo').projects('bar').should eql(projects('foo:bar'))
    project('foo').projects('baz').should eql(projects('foo:baz'))
    project('foo:bar').projects('baz').should eql(projects('foo:baz'))
  end

  it 'should fine a project from its parent by proximity' do
    define 'foo' do
      define('bar') { define 'baz' }
      define 'baz'
    end
    project('foo').projects('baz').should eql(projects('foo:baz'))
    project('foo:bar').projects('baz').should eql(projects('foo:bar:baz'))
  end

  it 'should evaluate all projects before returning' do
    # Note: gets around our define that also invokes the project.
    Buildr.define('foo') { define('bar'); define('baz') }
    projects.should eql(projects('foo', 'foo:bar', 'foo:baz'))
  end
end


describe Rake::Task, ' local directory' do
  before do
    @task = Project.local_task(task(('doda')))
    Project.on_define { |project| task('doda') { |task| @task.from project.name } }
  end

  it 'should execute project in local directory' do
    define 'foo'
    @task.should_receive(:from).with('foo')
    @task.invoke
  end

  it 'should execute sub-project in local directory' do
    @task.should_receive(:from).with('foo:bar')
    define('foo') { define 'bar' }
    in_original_dir(project('foo:bar').base_dir) { @task.invoke }
  end

  it 'should do nothing if no project in local directory' do
    @task.should_not_receive(:from)
    define('foo') { define 'bar' }
    in_original_dir('../not_foo') { @task.invoke }
  end

  it 'should find closest project that matches current directory' do
    mkpath 'bar/src/main'
    define('foo') { define 'bar' }
    @task.should_receive(:from).with('foo:bar')
    in_original_dir('bar/src/main') { @task.invoke }
  end
end


describe Project, '#task' do
  it 'should create a regular task' do
    define('foo') { task('bar') }
    Buildr.application.lookup('foo:bar').should_not be_nil
  end

  it 'should return a task defined in the project' do
    define('foo') { task('bar') }
    project('foo').task('bar').should be_instance_of(Rake::Task)
  end

  it 'should not create task outside project definition' do
    define 'foo'
    lambda { project('foo').task('bar') }.should raise_error(RuntimeError, /no task foo:bar/)
  end

  it 'should include project name as prefix' do
    define('foo') { task('bar') }
    project('foo').task('bar').name.should eql('foo:bar')
  end

  it 'should ignore namespace if starting with color' do
    define 'foo' do
      task(':bar').name.should == 'bar'
    end
    Rake::Task.task_defined?('bar').should be_true
  end

  it 'should accept single dependency' do
    define('foo') { task('bar'=>'baz') }
    project('foo').task('bar').prerequisites.should include('baz')
  end

  it 'should accept multiple dependencies' do
    define('foo') { task('bar'=>['baz1', 'baz2']) }
    project('foo').task('bar').prerequisites.should include('baz1')
    project('foo').task('bar').prerequisites.should include('baz2')
  end

  it 'should execute task exactly once' do
    define('foo') do
      task 'baz'
      task 'bar'=>'baz'
    end
    lambda { project('foo').task('bar').invoke }.should run_tasks(['foo:baz', 'foo:bar'])
  end

  it 'should create a file task' do
    define('foo') { file('bar') }
    Buildr.application.lookup(File.expand_path('bar')).should_not be_nil
  end

  it 'should create file task with absolute path' do
    define('foo') { file('/tmp') }
    Buildr.application.lookup(File.expand_path('/tmp')).should_not be_nil
  end

  it 'should create file task relative to project base directory' do
    define('foo', :base_dir=>'tmp') { file('bar') }
    Buildr.application.lookup(File.expand_path('tmp/bar')).should_not be_nil
  end

  it 'should accept single dependency' do
    define('foo') { file('bar'=>'baz') }
    project('foo').file('bar').prerequisites.should include('baz')
  end

  it 'should accept multiple dependencies' do
    define('foo') { file('bar'=>['baz1', 'baz2']) }
    project('foo').file('bar').prerequisites.should include('baz1')
    project('foo').file('bar').prerequisites.should include('baz2')
  end

  it 'should accept hash arguments' do
    define('foo') do
      task 'bar'=>'bar_dep'
      file 'baz'=>'baz_dep'
    end
    project('foo').task('bar').prerequisites.should include('bar_dep')
    project('foo').file('baz').prerequisites.should include('baz_dep')
  end

  it 'should return a file task defined in the project' do
    define('foo') { file('bar') }
    project('foo').file('bar').should be_instance_of(Rake::FileTask)
  end

  it 'should create file task relative to project definition' do
    define('foo') { define 'bar' }
    project('foo:bar').file('baz').name.should point_to_path('bar/baz')
  end

  it 'should execute task exactly once' do
    define('foo') do
      task 'baz'
      file 'bar'=>'baz'
    end
    lambda { project('foo').file('bar').invoke }.should run_tasks(['foo:baz', project('foo').path_to('bar')])
  end
end


=begin
describe Buildr::Generate do
  it 'should be able to create buildfile from directory structure' do
    write 'src/main/java/Foo.java', ''
    write 'one/two/src/main/java/Foo.java', ''
    write 'one/three/src/main/java/Foo.java', ''
    write 'four/src/main/java/Foo.java', ''
    script = Buildr::Generate.from_directory(Dir.pwd)
    instance_eval(script.join("\n"), "generated buildfile")
    # projects should have been defined
    root = Dir.pwd.pathmap('%n')
    names = [root, "#{root}:one:two", "#{root}:one:three", "#{root}:four"]
    # the top level project has the directory name.
    names.each { |name| lambda { project(name) }.should_not raise_error }
  end
end
=end
