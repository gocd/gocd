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


describe Project, '#doc' do
  def sources
    @sources ||= (1..3).map { |i| "Test#{i}" }.
      each { |name| write "src/main/java/foo/#{name}.java", "package foo; public class #{name}{}" }.
      map { |name| "src/main/java/foo/#{name}.java" }
  end

  it 'should return the project\'s Javadoc task' do
    define('foo') { compile.using(:javac) }
    project('foo').doc.name.should eql('foo:doc')
  end

  it 'should return a DocTask' do
    define('foo') { compile.using(:javac) }
    project('foo').doc.should be_kind_of(Doc::DocTask)
  end

  it 'should set target directory to target/doc' do
    define 'foo' do
      compile.using(:javac)
      doc.target.to_s.should point_to_path('target/doc')
    end
  end

  it 'should create file task for target directory' do
    define 'foo' do
      compile.using(:javac)
      doc.should_receive(:invoke_prerequisites)
    end
    project('foo').file('target/doc').invoke
  end

  it 'should respond to into() and return self' do
    define 'foo' do
      compile.using(:javac)
      doc.into('docs').should be(doc)
    end
  end

  it 'should respond to into() and change target directory' do
    define 'foo' do
      compile.using(:javac)
      doc.into('docs')
      doc.should_receive(:invoke_prerequisites)
    end
    file('docs').invoke
  end

  it 'should respond to from() and return self' do
    task = nil
    define('foo') do
      compile.using(:javac)
      task = doc.from('srcs')
    end
    task.should be(project('foo').doc)
  end

  it 'should respond to from() and add sources' do
    define 'foo' do
      compile.using(:javac)
      doc.from('srcs').should be(doc)
    end
  end

  it 'should respond to from() and add file task' do
    define 'foo' do
      compile.using(:javac)
      doc.from('srcs').should be(doc)
    end
    project('foo').doc.source_files.first.should point_to_path('srcs')
  end

  it 'should respond to from() and add project\'s sources and dependencies' do
    write 'bar/src/main/java/Test.java'
    define 'foo' do
      compile.using(:javac)
      define('bar') { compile.using(:javac).with 'group:id:jar:1.0' }
      doc.from project('foo:bar')
    end
    project('foo').doc.source_files.first.should point_to_path('bar/src/main/java/Test.java')
    project('foo').doc.classpath.map(&:to_spec).should include('group:id:jar:1.0')
  end

  it 'should generate docs from project' do
    sources
    define('foo') { compile.using(:javac) }
    project('foo').doc.source_files.sort.should == sources.sort.map { |f| File.expand_path(f) }
  end

  it 'should include compile dependencies' do
    define('foo') { compile.using(:javac).with 'group:id:jar:1.0' }
    project('foo').doc.classpath.map(&:to_spec).should include('group:id:jar:1.0')
  end

  it 'should respond to include() and return self' do
    define 'foo' do
      compile.using(:javac)
      doc.include('srcs').should be(doc)
    end
  end

  it 'should respond to include() and add files' do
    included = sources.first
    define 'foo' do
      compile.using(:javac)
      doc.include included
    end
    project('foo').doc.source_files.should include(File.expand_path(included))
  end

  it 'should respond to exclude() and return self' do
    define 'foo' do
      compile.using(:javac)
      doc.exclude('srcs').should be(doc)
    end
  end

  it 'should respond to exclude() and ignore files' do
    excluded = sources.first
    define 'foo' do
      compile.using(:javac)
      doc.exclude excluded
    end
    sources
    project('foo').doc.source_files.sort.should == sources[1..-1].map { |f| File.expand_path(f) }
  end

  it 'should respond to using() and return self' do
    define 'foo' do
      compile.using(:javac)
      doc.using(:foo=>'Fooing').should be(doc)
    end
  end

  it 'should respond to using() and accept options' do
    define 'foo' do
      compile.using(:javac)
      doc.using :foo=>'Fooing'
    end
    project('foo').doc.options[:foo].should eql('Fooing')
  end

  it 'should produce documentation' do
    sources
    define('foo') { compile.using(:javac) }
    project('foo').doc.invoke
    (1..3).map { |i| "target/doc/foo/Test#{i}.html" }.each { |f| file(f).should exist }
  end

  it 'should fail on error' do
    write 'Test.java', 'class Test {}'
    define 'foo' do
      compile.using(:javac)
      doc.include 'Test.java'
    end
    lambda { project('foo').doc.invoke }.should raise_error(RuntimeError, /Failed to generate Javadocs/)
  end

  it 'should be local task' do
    define 'foo' do
      define('bar') { compile.using(:javac) }
    end
    project('foo:bar').doc.should_receive(:invoke_prerequisites)
    in_original_dir(project('foo:bar').base_dir) { task('doc').invoke }
  end

  it 'should not recurse' do
    define 'foo' do
      compile.using(:javac)
      define('bar') { compile.using(:javac) }
    end
    project('foo:bar').doc.should_not_receive(:invoke_prerequisites)
    project('foo').doc.invoke
  end
end

