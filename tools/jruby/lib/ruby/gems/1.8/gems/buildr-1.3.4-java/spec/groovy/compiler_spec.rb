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

describe 'groovyc compiler' do 
  
  it 'should identify itself from groovy source directories' do
    write 'src/main/groovy/some/Hello.groovy', 'println "Hello Groovy"'
    write 'src/test/groovy/some/Hello.groovy', 'println "Hello Groovy"'
    define('foo') do 
      compile.compiler.should eql(:groovyc)
      test.compile.compiler.should eql(:groovyc)
    end
  end

  it 'should identify if groovy sources are found on java directories' do
    write 'src/main/java/some/Hello.groovy', 'println "Hello Groovy"'
    write 'src/test/java/some/Hello.groovy', 'println "Hello Groovy"'
    define('foo') do 
      compile.compiler.should eql(:groovyc)
      test.compile.compiler.should eql(:groovyc)
    end
  end

  it 'should identify itself even if groovy and java sources are found' do
    write 'src/main/java/some/Empty.java', 'package some; public interface Empty {}'
    write 'src/main/groovy/some/Hello.groovy', 'println "Hello Groovy"'
    write 'src/test/java/some/Empty.java', 'package some; public interface Empty {}'
    write 'src/test/groovy/some/Hello.groovy', 'println "Hello Groovy"'
    define('foo') do 
      compile.compiler.should eql(:groovyc)
      test.compile.compiler.should eql(:groovyc)
    end
  end

  it 'should identify from custom layout' do 
    write 'groovy/Hello.groovy', 'println "Hello world"'
    write 'testing/Hello.groovy', 'println "Hello world"'
    custom = Layout.new
    custom[:source, :main, :groovy] = 'groovy'
    custom[:source, :test, :groovy] = 'testing'
    define 'foo', :layout=>custom do
      compile.compiler.should eql(:groovyc)
      test.compile.compiler.should eql(:groovyc)
    end
  end
  
  it 'should identify from compile source directories' do
    write 'src/com/example/Code.groovy', 'println "monkey code"' 
    write 'testing/com/example/Test.groovy', 'println "some test"' 
    define 'foo' do
      lambda { compile.from 'src' }.should change { compile.compiler }.to(:groovyc)
      lambda { test.compile.from 'testing' }.should change { test.compile.compiler }.to(:groovyc)
    end
  end

  it 'should report the multi-language as :groovy, :java' do
    define('foo').compile.using(:groovyc).language.should == :groovy
  end

  it 'should set the target directory to target/classes' do
    define 'foo' do
      lambda { compile.using(:groovyc) }.should change { compile.target.to_s }.to(File.expand_path('target/classes'))
    end
  end

  it 'should not override existing target directory' do
    define 'foo' do
      compile.into('classes')
      lambda { compile.using(:groovyc) }.should_not change { compile.target }
    end
  end

  it 'should not change existing list of sources' do
    define 'foo' do
      compile.from('sources')
      lambda { compile.using(:groovyc) }.should_not change { compile.sources }
    end
  end
  
  it 'should compile groovy sources' do
    write 'src/main/groovy/some/Example.groovy', 'package some; class Example { static main(args) { println "Hello" } }'
    define('foo').compile.invoke
    file('target/classes/some/Example.class').should exist
  end

  it 'should include as classpath dependency' do
    write 'src/bar/groovy/some/Foo.groovy', 'package some; interface Foo {}'
    write 'src/main/groovy/some/Example.groovy', 'package some; class Example implements Foo { }'
    define('bar', :version => '1.0') do 
      compile.from('src/bar/groovy').into('target/bar')
      package(:jar)
    end
    lambda { define('foo').compile.with(project('bar').package(:jar)).invoke }.should run_task('foo:compile')
    file('target/classes/some/Example.class').should exist
  end
     
  it 'should cross compile java sources' do 
    write 'src/main/java/some/Foo.java', 'package some; public interface Foo { public void hello(); }'
    write 'src/main/java/some/Baz.java', 'package some; public class Baz extends Bar { }'
    write 'src/main/groovy/some/Bar.groovy', 'package some; class Bar implements Foo { def void hello() { } }'
    define('foo').compile.invoke
    %w{Foo Bar Baz}.each { |f| file("target/classes/some/#{f}.class").should exist }
  end

  it 'should cross compile test java sources' do 
    write 'src/test/java/some/Foo.java', 'package some; public interface Foo { public void hello(); }'
    write 'src/test/java/some/Baz.java', 'package some; public class Baz extends Bar { }'
    write 'src/test/groovy/some/Bar.groovy', 'package some; class Bar implements Foo { def void hello() { } }'
    define('foo').test.compile.invoke
    %w{Foo Bar Baz}.each { |f| file("target/test/classes/some/#{f}.class").should exist }
  end

  it 'should package classes into a jar file' do
    write 'src/main/groovy/some/Example.groovy', 'package some; class Example { }'
    define('foo', :version => '1.0').package.invoke
    file('target/foo-1.0.jar').should exist
    Zip::ZipFile.open(project('foo').package(:jar).to_s) do |jar|
      jar.file.exist?('some/Example.class').should be_true
    end
  end

end

describe 'groovyc compiler options' do
  
  def groovyc(&prc)
    define('foo') do
      compile.using(:groovyc)
      @compiler = compile.instance_eval { @compiler }
      class << @compiler
        public :groovyc_options, :javac_options
      end
      if block_given?
        instance_eval(&prc)
      else
        return compile
      end
    end
  end
  
  it 'should set warning option to false by default' do 
    groovyc do
      compile.options.warnings.should be_false
      @compiler.javac_options[:nowarn].should be_true
    end
  end

  it 'should set warning option to true when running with --verbose option' do
    verbose true
    groovyc do
      compile.options.warnings.should be_true
      @compiler.javac_options[:nowarn].should be_false
    end
  end

  it 'should not set verbose option by default' do
    groovyc.options.verbose.should be_false
  end

  it 'should set verbose option when running with --trace option' do
    trace true
    groovyc.options.verbose.should be_true
  end
  
  it 'should set debug option to false based on Buildr.options' do
    Buildr.options.debug = false
    groovyc.options.debug.should be_false
  end

  it 'should set debug option to false based on debug environment variable' do
    ENV['debug'] = 'no'
    groovyc.options.debug.should be_false
  end

  it 'should set debug option to false based on DEBUG environment variable' do
    ENV['DEBUG'] = 'no'
    groovyc.options.debug.should be_false
  end

  it 'should set deprecation option to false by default' do
    groovyc.options.deprecation.should be_false
  end

  it 'should use deprecation argument when deprecation is true' do
    groovyc do
      compile.using(:deprecation=>true)
      compile.options.deprecation.should be_true
      @compiler.javac_options[:deprecation].should be_true
    end
  end

  it 'should not use deprecation argument when deprecation is false' do
    groovyc do
      compile.using(:deprecation=>false)
      compile.options.deprecation.should be_false
      @compiler.javac_options[:deprecation].should_not be_true
    end
  end

  it 'should set optimise option to false by default' do
    groovyc.options.optimise.should be_false
  end

  it 'should use optimize argument when deprecation is true' do
    groovyc do
      compile.using(:optimise=>true)
      @compiler.javac_options[:optimize].should be_true
    end
  end

  it 'should not use optimize argument when deprecation is false' do
    groovyc do
      compile.using(:optimise=>false)
      @compiler.javac_options[:optimize].should be_false
    end
  end

  after do
    Buildr.options.debug = nil
    ENV.delete "debug"
    ENV.delete "DEBUG"
  end

end
