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


require File.join(File.dirname(__FILE__), 'test_coverage_spec')
Sandbox.require_optional_extension 'buildr/java/cobertura'
artifacts(Buildr::Cobertura::dependencies).map(&:invoke)


describe Buildr::Cobertura do
  before do
    # Reloading the extension because the sandbox removes all its actions
    Buildr.module_eval { remove_const :Cobertura }
    load File.expand_path('../lib/buildr/java/cobertura.rb')
    @tool_module = Buildr::Cobertura 
  end
  
  it_should_behave_like 'test coverage tool'
  
  describe 'project-specific' do
    
    describe 'data file' do
      it 'should have a default value' do
        define('foo').cobertura.data_file.should point_to_path('reports/cobertura.ser')
      end
      
      it 'should be overridable' do
        define('foo') { cobertura.data_file = path_to('target/data.cobertura') }
        project('foo').cobertura.data_file.should point_to_path('target/data.cobertura')
      end
      
      it 'should be created during instrumentation' do
        write 'src/main/java/Foo.java', 'public class Foo {}'
        define('foo')
        task('foo:cobertura:instrument').invoke
        file(project('foo').cobertura.data_file).should exist
      end
    end
      
    describe 'instrumentation' do
      before do
        ['Foo', 'Bar'].each { |cls| write File.join('src/main/java', "#{cls}.java"), "public class #{cls} {}" }
      end
      
      it 'should instrument only included classes' do
        define('foo') { cobertura.include 'Foo' }
        task("foo:cobertura:instrument").invoke
        Dir.chdir('target/instrumented/classes') { Dir.glob('*').sort.should == ['Foo.class'] }
      end
      
      it 'should not instrument excluded classes' do
        define('foo') { cobertura.exclude 'Foo' }
        task("foo:cobertura:instrument").invoke
        Dir.chdir('target/instrumented/classes') { Dir.glob('*').sort.should == ['Bar.class'] }
      end
      
      it 'should instrument classes that are included but not excluded' do
        write 'src/main/java/Baz.java', 'public class Baz {}'
        define('foo') { cobertura.include('Ba').exclude('ar') }
        task("foo:cobertura:instrument").invoke
        Dir.chdir('target/instrumented/classes') { Dir.glob('*').sort.should == ['Baz.class'] }
      end
    end
  end
end
