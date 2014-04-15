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


describe 'javac compiler' do
  it 'should identify itself from source directories' do
    write 'src/main/java/com/example/Test.java', 'package com.example; class Test {}' 
    define('foo').compile.compiler.should eql(:javac)
  end

  it 'should identify from source directories using custom layout' do
    write 'src/com/example/Code.java', 'package com.example; class Code {}' 
    write 'testing/com/example/Test.java', 'package com.example; class Test {}' 
    custom = Layout.new
    custom[:source, :main, :java] = 'src'
    custom[:source, :test, :java] = 'testing'
    define 'foo', :layout=>custom do
      compile.compiler.should eql(:javac)
      test.compile.compiler.should eql(:javac)
    end
  end

  it 'should identify from compile source directories' do
    write 'src/com/example/Code.java', 'package com.example; class Code {}' 
    write 'testing/com/example/Test.java', 'package com.example; class Test {}' 
    define 'foo' do
      lambda { compile.from 'src' }.should change { compile.compiler }.to(:javac)
      lambda { test.compile.from 'testing' }.should change { test.compile.compiler }.to(:javac)
    end
  end

  it 'should report the language as :java' do
    define('foo').compile.using(:javac).language.should eql(:java)
  end

  it 'should set the target directory to target/classes' do
    define 'foo' do
      lambda { compile.using(:javac) }.should change { compile.target.to_s }.to(File.expand_path('target/classes'))
    end
  end

  it 'should not override existing target directory' do
    define 'foo' do
      compile.into('classes')
      lambda { compile.using(:javac) }.should_not change { compile.target }
    end
  end

  it 'should not change existing list of sources' do
    define 'foo' do
      compile.from('sources')
      lambda { compile.using(:javac) }.should_not change { compile.sources }
    end
  end

  it 'should include classpath dependencies' do
    write 'src/dependency/Dependency.java', 'class Dependency {}'
    define 'dependency', :version=>'1.0' do
      compile.from('src/dependency').into('target/dependency')
      package(:jar)
    end
    write 'src/test/DependencyTest.java', 'class DependencyTest { Dependency _var; }'
    define('foo').compile.from('src/test').with(project('dependency')).invoke
    file('target/classes/DependencyTest.class').should exist
  end

  it 'should include tools.jar dependency' do
    write 'src/main/java/UseApt.java', <<-JAVA
      import com.sun.mirror.apt.AnnotationProcessor;
      public class UseApt { }
    JAVA
    define('foo').compile.invoke
    file('target/classes/UseApt.class').should exist
  end
end


describe 'javac compiler options' do
  def compile_task
    @compile_task ||= define('foo').compile.using(:javac)
  end

  def javac_args
    compile_task.instance_eval { @compiler }.send(:javac_args)
  end

  it 'should set warnings option to false by default' do
    compile_task.options.warnings.should be_false
  end

  it 'should set wranings option to true when running with --verbose option' do
    verbose true
    compile_task.options.warnings.should be_true
  end

  it 'should use -nowarn argument when warnings is false' do
    compile_task.using(:warnings=>false)
    javac_args.should include('-nowarn') 
  end

  it 'should not use -nowarn argument when warnings is true' do
    compile_task.using(:warnings=>true)
    javac_args.should_not include('-nowarn') 
  end

  it 'should not use -verbose argument by default' do
    javac_args.should_not include('-verbose') 
  end

  it 'should use -verbose argument when running with --trace option' do
    trace true
    javac_args.should include('-verbose') 
  end

  it 'should set debug option to true by default' do
    compile_task.options.debug.should be_true
  end

  it 'should set debug option to false based on Buildr.options' do
    Buildr.options.debug = false
    compile_task.options.debug.should be_false
  end

  it 'should set debug option to false based on debug environment variable' do
    ENV['debug'] = 'no'
    compile_task.options.debug.should be_false
  end

  it 'should set debug option to false based on DEBUG environment variable' do
    ENV['DEBUG'] = 'no'
    compile_task.options.debug.should be_false
  end

  it 'should use -g argument when debug option is true' do
    compile_task.using(:debug=>true)
    javac_args.should include('-g')
  end

  it 'should not use -g argument when debug option is false' do
    compile_task.using(:debug=>false)
    javac_args.should_not include('-g')
  end

  it 'should set deprecation option to false by default' do
    compile_task.options.deprecation.should be_false
  end

  it 'should use -deprecation argument when deprecation is true' do
    compile_task.using(:deprecation=>true)
    javac_args.should include('-deprecation')
  end

  it 'should not use -deprecation argument when deprecation is false' do
    compile_task.using(:deprecation=>false)
    javac_args.should_not include('-deprecation')
  end

  it 'should not set source option by default' do
    compile_task.options.source.should be_nil
    javac_args.should_not include('-source')
  end

  it 'should not set target option by default' do
    compile_task.options.target.should be_nil
    javac_args.should_not include('-target')
  end

  it 'should use -source nn argument if source option set' do
    compile_task.using(:source=>'1.5')
    javac_args.should include('-source', '1.5')
  end

  it 'should use -target nn argument if target option set' do
    compile_task.using(:target=>'1.5')
    javac_args.should include('-target', '1.5')
  end

  it 'should set lint option to false by default' do
    compile_task.options.lint.should be_false
  end

  it 'should use -lint argument if lint option is true' do
    compile_task.using(:lint=>true)
    javac_args.should include('-Xlint')
  end

  it 'should use -lint argument with value of option' do
    compile_task.using(:lint=>'all')
    javac_args.should include('-Xlint:all')
  end

  it 'should use -lint argument with value of option as array' do
    compile_task.using(:lint=>['path', 'serial'])
    javac_args.should include('-Xlint:path,serial')
  end

  it 'should not set other option by default' do
    compile_task.options.other.should be_nil
  end

  it 'should pass other argument if other option is string' do
    compile_task.using(:other=>'-Xprint')
    javac_args.should include('-Xprint')
  end

  it 'should pass other argument if other option is array' do
    compile_task.using(:other=>['-Xstdout', 'msgs'])
    javac_args.should include('-Xstdout', 'msgs')
  end

  it 'should complain about options it doesn\'t know' do
    write 'source/Test.java', 'class Test {}'
    compile_task.using(:unknown=>'option')
    lambda { compile_task.from('source').invoke }.should raise_error(ArgumentError, /no such option/i)
  end

  it 'should inherit options from parent' do
    define 'foo' do
      compile.using(:warnings=>true, :debug=>true, :deprecation=>true, :source=>'1.5', :target=>'1.4')
      define 'bar' do
        compile.using(:javac)
        compile.options.warnings.should be_true
        compile.options.debug.should be_true
        compile.options.deprecation.should be_true
        compile.options.source.should eql('1.5')
        compile.options.target.should eql('1.4')
      end
    end
  end

  after do
    Buildr.options.debug = nil
    ENV.delete "debug"
    ENV.delete "DEBUG"
  end
end


describe Project, '#javadoc' do
  def sources
    @sources ||= (1..3).map { |i| "Test#{i}" }.
      each { |name| write "src/main/java/foo/#{name}.java", "package foo; public class #{name}{}" }.
      map { |name| "src/main/java/foo/#{name}.java" }
  end

  it 'should return the project\'s Javadoc task' do
    define('foo') { compile.using(:javac) }
    project('foo').javadoc.name.should eql('foo:javadoc')
  end

  it 'should return a Javadoc task' do
    define('foo') { compile.using(:javac) }
    project('foo').javadoc.should be_kind_of(Javadoc::JavadocTask)
  end

  it 'should set target directory to target/javadoc' do
    define 'foo' do
      compile.using(:javac)
      javadoc.target.to_s.should point_to_path('target/javadoc')
    end
  end

  it 'should create file task for target directory' do
    define 'foo' do
      compile.using(:javac)
      javadoc.should_receive(:invoke_prerequisites)
    end
    project('foo').file('target/javadoc').invoke
  end

  it 'should respond to into() and return self' do
    define 'foo' do
      compile.using(:javac)
      javadoc.into('docs').should be(javadoc)
    end
  end

  it 'should respond to into() and change target directory' do
    define 'foo' do
      compile.using(:javac)
      javadoc.into('docs')
      javadoc.should_receive(:invoke_prerequisites)
    end
    file('docs').invoke
  end

  it 'should respond to from() and return self' do
    task = nil
    define('foo') { task = javadoc.from('srcs') }
    task.should be(project('foo').javadoc)
  end

  it 'should respond to from() and add sources' do
    define 'foo' do
      compile.using(:javac)
      javadoc.from('srcs').should be(javadoc)
    end
  end

  it 'should respond to from() and add file task' do
    define 'foo' do
      compile.using(:javac)
      javadoc.from('srcs').should be(javadoc)
    end
    project('foo').javadoc.source_files.first.should point_to_path('srcs')
  end

  it 'should respond to from() and add project\'s sources and dependencies' do
    write 'bar/src/main/java/Test.java'
    define 'foo' do
      compile.using(:javac)
      define('bar') { compile.using(:javac).with 'group:id:jar:1.0' }
      javadoc.from project('foo:bar')
    end
    project('foo').javadoc.source_files.first.should point_to_path('bar/src/main/java/Test.java')
    project('foo').javadoc.classpath.map(&:to_spec).should include('group:id:jar:1.0')
  end

  it 'should generate javadocs from project' do
    sources
    define('foo') { compile.using(:javac) }
    project('foo').javadoc.source_files.sort.should == sources.sort.map { |f| File.expand_path(f) }
  end

  it 'should include compile dependencies' do
    define('foo') { compile.using(:javac).with 'group:id:jar:1.0' }
    project('foo').javadoc.classpath.map(&:to_spec).should include('group:id:jar:1.0')
  end

  it 'should respond to include() and return self' do
    define 'foo' do
      compile.using(:javac)
      javadoc.include('srcs').should be(javadoc)
    end
  end

  it 'should respond to include() and add files' do
    included = sources.first
    define 'foo' do
      compile.using(:javac)
      javadoc.include included
    end
    project('foo').javadoc.source_files.should include(included)
  end

  it 'should respond to exclude() and return self' do
    define 'foo' do
      compile.using(:javac)
      javadoc.exclude('srcs').should be(javadoc)
    end
  end

  it 'should respond to exclude() and ignore files' do
    excluded = sources.first
    define 'foo' do
      compile.using(:javac)
      javadoc.exclude excluded
    end
    sources
    project('foo').javadoc.source_files.sort.should == sources[1..-1].map { |f| File.expand_path(f) }
  end

  it 'should respond to using() and return self' do
    define 'foo' do
      compile.using(:javac)
      javadoc.using(:windowtitle=>'Fooing').should be(javadoc)
    end
  end

  it 'should respond to using() and accept options' do
    define 'foo' do
      compile.using(:javac)
      javadoc.using :windowtitle=>'Fooing'
    end
    project('foo').javadoc.options[:windowtitle].should eql('Fooing')
  end

  it 'should pick -windowtitle from project name' do
    define 'foo' do
      compile.using(:javac)
      javadoc.options[:windowtitle].should eql('foo')

      define 'bar' do
        compile.using(:javac)
        javadoc.options[:windowtitle].should eql('foo:bar')
      end
    end
  end

  it 'should pick -windowtitle from project description' do
    desc 'My App'
    define 'foo' do
      compile.using(:javac)
      javadoc.options[:windowtitle].should eql('My App')
    end
  end

  it 'should produce documentation' do
    sources
    define('foo') { compile.using(:javac) }
    project('foo').javadoc.invoke
    (1..3).map { |i| "target/javadoc/foo/Test#{i}.html" }.each { |f| file(f).should exist }
  end

  it 'should fail on error' do
    write 'Test.java', 'class Test {}'
    define 'foo' do
      compile.using(:javac)
      javadoc.include 'Test.java'
    end
    lambda { project('foo').javadoc.invoke }.should raise_error(RuntimeError, /Failed to generate Javadocs/)
  end

  it 'should be local task' do
    define 'foo' do
      define('bar') { compile.using(:javac) }
    end
    project('foo:bar').javadoc.should_receive(:invoke_prerequisites)
    in_original_dir(project('foo:bar').base_dir) { task('javadoc').invoke }
  end

  it 'should not recurse' do
    define 'foo' do
      compile.using(:javac)
      define('bar') { compile.using(:javac) }
    end
    project('foo:bar').javadoc.should_not_receive(:invoke_prerequisites)
    project('foo').javadoc.invoke
  end
end

