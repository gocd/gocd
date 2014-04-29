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

# need to test both with and without SCALA_HOME
share_as :ScalacCompiler do

  it 'should identify itself from source directories' do
    write 'src/main/scala/com/example/Test.scala', 'package com.example; class Test { val i = 1 }'
    define('foo').compile.compiler.should eql(:scalac)
  end

  it 'should report the language as :scala' do
    define('foo').compile.using(:scalac).language.should eql(:scala)
  end

  it 'should set the target directory to target/classes' do
    define 'foo' do
      lambda { compile.using(:scalac) }.should change { compile.target.to_s }.to(File.expand_path('target/classes'))
    end
  end

  it 'should not override existing target directory' do
    define 'foo' do
      compile.into('classes')
      lambda { compile.using(:scalac) }.should_not change { compile.target }
    end
  end

  it 'should not change existing list of sources' do
    define 'foo' do
      compile.from('sources')
      lambda { compile.using(:scalac) }.should_not change { compile.sources }
    end
  end

  it 'should include as classpath dependency' do
    write 'src/dependency/Dependency.scala', 'class Dependency {}'
    define 'dependency', :version=>'1.0' do
      compile.from('src/dependency').into('target/dependency')
      package(:jar)
    end
    write 'src/test/DependencyTest.scala', 'class DependencyTest { var d: Dependency = _ }'
    lambda { define('foo').compile.from('src/test').with(project('dependency')).invoke }.should run_task('foo:compile')
    file('target/classes/DependencyTest.class').should exist
  end

  def define_test1_project
    write 'src/main/scala/com/example/Test1.scala', 'package com.example; class Test1 { val i = 1 }'
    define 'test1', :version=>'1.0' do
      package(:jar)
    end
  end

  it 'should compile a simple .scala file into a .class file' do
    define_test1_project
    task('test1:compile').invoke
    file('target/classes/com/example/Test1.class').should exist
  end

  it 'should package .class into a .jar file' do
    define_test1_project
    task('test1:package').invoke
    file('target/test1-1.0.jar').should exist
    Zip::ZipFile.open(project('test1').package(:jar).to_s) do |zip|
      zip.file.exist?('com/example/Test1.class').should be_true
    end
  end

  it 'should compile scala class depending on java class in same project' do
    write 'src/main/java/com/example/Foo.java', 'package com.example; public class Foo {}'
    write 'src/main/scala/com/example/Bar.scala', 'package com.example; class Bar extends Foo'
    define 'test1', :version=>'1.0' do
      package(:jar)
    end
    task('test1:package').invoke
    file('target/test1-1.0.jar').should exist
    Zip::ZipFile.open(project('test1').package(:jar).to_s) do |zip|
      zip.file.exist?('com/example/Foo.class').should be_true
      zip.file.exist?('com/example/Bar.class').should be_true
    end
  end

  it 'should compile java class depending on scala class in same project' do
    write 'src/main/scala/com/example/Foo.scala', 'package com.example; class Foo'
    write 'src/main/java/com/example/Bar.java',  'package com.example; public class Bar extends Foo {}'
    define 'test1', :version=>'1.0' do
      package(:jar)
    end
    task('test1:package').invoke
    file('target/test1-1.0.jar').should exist
    Zip::ZipFile.open(project('test1').package(:jar).to_s) do |zip|
      zip.file.exist?('com/example/Foo.class').should be_true
      zip.file.exist?('com/example/Bar.class').should be_true
    end
  end
end

# Only run this test if the test environment has SCALA_HOME specified.
# Allows the Test Suite to run on TravisCI
if ENV['SCALA_HOME']
  describe 'scala compiler (installed in SCALA_HOME)' do
    it 'requires present SCALA_HOME' do
      ENV['SCALA_HOME'].should_not be_nil
    end

    it_should_behave_like ScalacCompiler
  end
end

describe 'scala compiler (downloaded from repository)' do
  old_home = ENV['SCALA_HOME']

  before :all do
    ENV['SCALA_HOME'] = nil
  end

  it 'requires absent SCALA_HOME' do
    ENV['SCALA_HOME'].should be_nil
  end

  it_should_behave_like ScalacCompiler

  after :all do
    ENV['SCALA_HOME'] = old_home
  end
end

share_as :ScalacCompiler_CommonOptions do

  it 'should set warnings option to false by default' do
    compile_task.options.warnings.should be_false
  end

  it 'should set warnings option to true when running with --verbose option' do
    verbose true
    compile_task.options.warnings.should be_true
  end

  it 'should use -nowarn argument when warnings is false' do
    compile_task.using(:warnings=>false)
    scalac_args.should include('-nowarn')
  end

  it 'should not use -nowarn argument when warnings is true' do
    compile_task.using(:warnings=>true)
    scalac_args.should_not include('-nowarn')
  end

  it 'should not use -verbose argument by default' do
    scalac_args.should_not include('-verbose')
  end

  it 'should use -verbose argument when running with --trace=scalac option' do
    Buildr.application.options.trace_categories = [:scalac]
    scalac_args.should include('-verbose')
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

  it 'should set deprecation option to false by default' do
    compile_task.options.deprecation.should be_false
  end

  it 'should use -deprecation argument when deprecation is true' do
    compile_task.using(:deprecation=>true)
    scalac_args.should include('-deprecation')
  end

  it 'should not use -deprecation argument when deprecation is false' do
    compile_task.using(:deprecation=>false)
    scalac_args.should_not include('-deprecation')
  end

  it 'should set optimise option to false by default' do
    compile_task.options.optimise.should be_false
  end

  it 'should use -optimise argument when deprecation is true' do
    compile_task.using(:optimise=>true)
    scalac_args.should include('-optimise')
  end

  it 'should not use -optimise argument when deprecation is false' do
    compile_task.using(:optimise=>false)
    scalac_args.should_not include('-optimise')
  end

  it 'should not set target option by default' do
    compile_task.options.target.should be_nil
    scalac_args.should_not include('-target')
  end

  it 'should use -target:xxx argument if target option set' do
    compile_task.using(:target=>'1.5')
    scalac_args.should include('-target:jvm-1.5')
  end

  it 'should not set other option by default' do
    compile_task.options.other.should be_nil
  end

  it 'should pass other argument if other option is string' do
    compile_task.using(:other=>'-unchecked')
    scalac_args.should include('-unchecked')
  end

  it 'should pass other argument if other option is array' do
    compile_task.using(:other=>['-unchecked', '-Xprint-types'])
    scalac_args.should include('-unchecked', '-Xprint-types')
  end

  it 'should complain about options it doesn\'t know' do
    write 'source/Test.scala', 'class Test {}'
    compile_task.using(:unknown=>'option')
    lambda { compile_task.from('source').invoke }.should raise_error(ArgumentError, /no such option/i)
  end

  it 'should inherit options from parent' do
    define 'foo' do
      compile.using(:warnings=>true, :debug=>true, :deprecation=>true, :target=>'1.4')
      define 'bar' do
        compile.using(:scalac)
        compile.options.warnings.should be_true
        compile.options.debug.should be_true
        compile.options.deprecation.should be_true
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


describe 'scala compiler 2.8 options' do

  it_should_behave_like ScalacCompiler_CommonOptions

  def compile_task
    @compile_task ||= define('foo').compile.using(:scalac)
  end

  def scalac_args
    compile_task.instance_eval { @compiler }.send(:scalac_args)
  end

  it 'should use -g argument when debug option is true' do
    compile_task.using(:debug=>true)
    scalac_args.should include('-g')
  end

  it 'should not use -g argument when debug option is false' do
    compile_task.using(:debug=>false)
    scalac_args.should_not include('-g')
  end
end if Buildr::Scala.version?(2.8)

describe 'scala compiler 2.9 options' do

  it_should_behave_like ScalacCompiler_CommonOptions

  def compile_task
    @compile_task ||= define('foo').compile.using(:scalac)
  end

  def scalac_args
    compile_task.instance_eval { @compiler }.send(:scalac_args)
  end

  # these specs fail. Somehow the compiler is still in version 2.8
  it 'should use -g:vars argument when debug option is true' do
    compile_task.using(:debug=>true)
    scalac_args.should include('-g:vars')
  end

  it 'should use -g:whatever argument when debug option is \'whatever\'' do
    compile_task.using(:debug=>:whatever)
    scalac_args.should include('-g:whatever')
  end

  it 'should not use -g argument when debug option is false' do
    compile_task.using(:debug=>false)
    scalac_args.should_not include('-g')
  end

end if Buildr::Scala.version?(2.9)

if Java.java.lang.System.getProperty("java.runtime.version") >= "1.6"

describe 'zinc compiler (enabled through Buildr.settings)' do
  before :each do
    Buildr.settings.build['scalac.incremental'] = true
  end

  it 'should compile with zinc' do
    write 'src/main/scala/com/example/Test.scala', 'package com.example; class Test { val i = 1 }'
    project = define('foo')
    compile_task = project.compile.using(:scalac)
    compiler = compile_task.instance_eval { @compiler }
    compiler.send(:zinc?).should eql(true)
    compiler.should_receive(:compile_with_zinc).once
    compile_task.invoke
  end

  after :each do
    Buildr.settings.build['scalac.incremental'] = nil
  end

  it_should_behave_like ScalacCompiler
end

describe 'zinc compiler (enabled through project.scala_options)' do

  it 'should compile with zinc' do
    write 'src/main/scala/com/example/Test.scala', 'package com.example; class Test { val i = 1 }'
    project = define('foo')
    project.scalac_options.incremental = true
    compile_task = project.compile.using(:scalac)
    compiler = compile_task.instance_eval { @compiler }
    compiler.send(:zinc?).should eql(true)
    compiler.should_receive(:compile_with_zinc).once
    compile_task.invoke
  end
end

elsif Buildr::VERSION >= '1.5'
  raise "JVM version guard in #{__FILE__} should be removed since it is assumed that Java 1.5 is no longer supported."
end
