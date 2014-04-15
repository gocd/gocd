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
require File.join(File.dirname(__FILE__), 'packaging_helper')


describe Project, '#group' do
  it 'should default to project name' do
    desc 'My Project'
    define('foo').group.should eql('foo')
  end

  it 'should be settable' do
    define('foo', :group=>'bar').group.should eql('bar')
  end

  it 'should inherit from parent project' do
    define('foo', :group=>'groupie') { define 'bar' }
    project('foo:bar').group.should eql('groupie')
  end
end


describe Project, '#version' do
  it 'should default to nil' do
    define('foo').version.should be_nil
  end

  it 'should be settable' do
    define('foo', :version=>'2.1').version.should eql('2.1')
  end

  it 'should inherit from parent project' do
    define('foo', :version=>'2.1') { define 'bar' }
    project('foo:bar').version.should eql('2.1')
  end

end


describe Project, '#id' do
  it 'should be same as project name' do
    define('foo').id.should eql('foo')
  end

  it 'should replace colons with dashes' do
    define('foo', :version=>'2.1') { define 'bar' }
    project('foo:bar').id.should eql('foo-bar')
  end

  it 'should not be settable' do
    lambda { define 'foo', :id=>'bar' }.should raise_error(NoMethodError)
  end
end


describe Project, '#package' do
  it 'should default to id from project' do
    define('foo', :version=>'1.0') do
      package(:jar).id.should eql('foo')
    end
  end

  it 'should default to composed id for nested projects' do
    define('foo', :version=>'1.0') do
      define 'bar' do
        package(:jar).id.should eql('foo-bar')
      end
    end
  end

  it 'should take id from option if specified' do
    define 'foo', :version=>'1.0' do
      package(:jar, :id=>'bar').id.should eql('bar')
      define 'bar' do
        package(:jar, :id=>'baz').id.should eql('baz')
      end
    end
  end

  it 'should default to group from project' do
    define 'foo', :version=>'1.0' do
      package(:jar).group.should eql('foo')
      define 'bar' do
        package(:jar).group.should eql('foo')
      end
    end
  end

  it 'should take group from option if specified' do
    define 'foo', :version=>'1.0' do
      package(:jar, :group=>'foos').group.should eql('foos')
      define 'bar' do
        package(:jar, :group=>'bars').group.should eql('bars')
      end
    end
  end

  it 'should default to version from project' do
    define 'foo', :version=>'1.0' do
      package(:jar).version.should eql('1.0')
      define 'bar' do
        package(:jar).version.should eql('1.0')
      end
    end
  end

  it 'should take version from option if specified' do
    define 'foo', :version=>'1.0' do
      package(:jar, :version=>'1.1').version.should eql('1.1')
      define 'bar' do
        package(:jar, :version=>'1.2').version.should eql('1.2')
      end
    end
  end

  it 'should accept package type as first argument' do
    define 'foo', :version=>'1.0' do
      package(:war).type.should eql(:war)
      define 'bar' do
        package(:jar).type.should eql(:jar)
      end
    end
  end

  it 'should support optional type' do
    define 'foo', :version=>'1.0' do
      package.type.should eql(:zip)
      package(:classifier=>'srcs').type.should eql(:zip)
    end
    define 'bar', :version=>'1.0' do
      compile.using :javac
      package(:classifier=>'srcs').type.should eql(:jar)
    end
  end

  it 'should assume :zip package type unless specified' do
    define 'foo', :version=>'1.0' do
      package.type.should eql(:zip)
      define 'bar' do
        package.type.should eql(:zip)
      end
    end
  end

  it 'should infer packaging type from compiler' do
    define 'foo', :version=>'1.0' do
      compile.using :javac
      package.type.should eql(:jar)
    end
  end

  it 'should fail if packaging not supported' do
    lambda { define('foo') { package(:weirdo) } }.should raise_error(RuntimeError, /Don't know how to create a package/)
  end

  it 'should default to no classifier' do
    define 'foo', :version=>'1.0' do
      package.classifier.should be_nil
      define 'bar' do
        package.classifier.should be_nil
      end
    end
  end

  it 'should accept classifier from option' do
    define 'foo', :version=>'1.0' do
      package(:classifier=>'srcs').classifier.should eql('srcs')
      define 'bar' do
        package(:classifier=>'docs').classifier.should eql('docs')
      end
    end
  end

  it 'should return a file task' do
    define('foo', :version=>'1.0') { package(:jar) }
    project('foo').package(:jar).should be_kind_of(Rake::FileTask) 
  end

  it 'should return a task that acts as artifact' do
    define('foo', :version=>'1.0') { package(:jar) }
    project('foo').package(:jar).should respond_to(:to_spec)
    project('foo').package(:jar).to_spec.should eql('foo:foo:jar:1.0')
  end

  it 'should create different tasks for each spec' do
    define 'foo', :version=>'1.0' do
      package(:jar)
      package(:war)
      package(:jar, :id=>'bar')
      package(:jar, :classifier=>'srcs')
    end
    project('foo').packages.uniq.size.should be(4)
  end

  it 'should not create multiple packages for the same spec' do
    define 'foo', :version=>'1.0' do
      package(:war)
      package(:war)
      package(:jar, :id=>'bar')
      package(:jar, :id=>'bar')
    end
    project('foo').packages.uniq.size.should be(2)
  end

  it 'should return the same task for subsequent calls' do
    define 'foo', :version=>'1.0' do
      package.should eql(package)
      package(:jar, :classifier=>'resources').should be(package(:jar, :classifier=>'resources'))
    end
  end

  it 'should return a packaging task even if file already exists' do
    write 'target/foo-1.0.zip', ''
    define 'foo', :version=>'1.0' do
      package.should be_kind_of(ZipTask)
    end
  end

  it 'should register task as artifact' do
    define 'foo', :version=>'1.0' do
      package(:jar, :id=>'bar')
      package(:war)
    end
    project('foo').packages.should eql(artifacts('foo:bar:jar:1.0', 'foo:foo:war:1.0'))
  end

  it 'should create in target path' do
    define 'foo', :version=>'1.0' do
      package(:war).should point_to_path('target/foo-1.0.war')
      package(:jar, :id=>'bar').should point_to_path('target/bar-1.0.jar')
      package(:zip, :classifier=>'srcs').should point_to_path('target/foo-1.0-srcs.zip')
    end
  end

  it 'should create prerequisite for package task' do
    define 'foo', :version=>'1.0' do
      package(:war)
      package(:jar, :id=>'bar')
      package(:jar, :classifier=>'srcs')
    end
    project('foo').task('package').prerequisites.should include(*project('foo').packages)
  end

  it 'should create task requiring a build' do
    define 'foo', :version=>'1.0' do
      package(:war).prerequisites.should include(build)
      package(:jar, :id=>'bar').prerequisites.should include(build)
      package(:jar, :classifier=>'srcs').prerequisites.should include(build)
    end
  end

  it 'should create a POM artifact in local repository' do
    define 'foo', :version=>'1.0' do
      package.pom.should be(artifact('foo:foo:pom:1.0'))
      repositories.locate('foo:foo:pom:1.0').should eql(package.pom.to_s)
    end
  end

  it 'should create POM artifact ignoring classifier' do
    define 'foo', :version=>'1.0' do
      package(:jar, :classifier=>'srcs').pom.should be(artifact('foo:foo:pom:1.0'))
    end
  end

  it 'should create POM artifact that creates its own POM' do
    define('foo', :group=>'bar', :version=>'1.0') { package(:jar, :classifier=>'srcs') }
    pom = project('foo').packages.first.pom
    pom.invoke
    read(pom.to_s).should eql(<<-POM
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>bar</groupId>
  <artifactId>foo</artifactId>
  <version>1.0</version>
</project>
POM
    )
  end

  it 'should not require downloading artifact or POM' do
    #task('artifacts').instance_eval { @actions.clear }
    define('foo', :group=>'bar', :version=>'1.0') { package(:jar) }
    lambda { task('artifacts').invoke }.should_not raise_error
  end

end





describe Project, '#package file' do
  it 'should be a file task' do
    define 'foo' do
      package(:zip, :file=>'foo.zip').should be_kind_of(Rake::FileTask)
    end
  end

  it 'should not require id, project or version' do
    define 'foo', :group=>nil do
      lambda { package(:zip, :file=>'foo.zip') }.should_not raise_error
      lambda { package(:zip, :file=>'bar.zip', :id=>'error') }.should raise_error
      lambda { package(:zip, :file=>'bar.zip', :group=>'error') }.should raise_error
      lambda { package(:zip, :file=>'bar.zip', :version=>'error') }.should raise_error
    end
  end

  it 'should not provide project or version' do
    define 'foo' do
      package(:zip, :file=>'foo.zip').tap do |pkg|
        pkg.should_not respond_to(:group)
        pkg.should_not respond_to(:version)
      end
    end
  end

  it 'should provide packaging type' do
    define 'foo', :version=>'1.0' do
      zip = package(:zip, :file=>'foo.zip')
      jar = package(:jar, :file=>'bar.jar')
      zip.type.should eql(:zip)
      jar.type.should eql(:jar)
    end
  end

  it 'should assume packaging type from extension if unspecified' do
    define 'foo', :version=>'1.0' do
      package(:file=>'foo.zip').class.should be(Buildr::ZipTask)
      define 'bar' do
        package(:file=>'bar.jar').class.should be(Buildr::Packaging::Java::JarTask)
      end
    end
  end

  it 'should support different packaging types' do
    define 'foo', :version=>'1.0' do
      package(:jar, :file=>'foo.jar').class.should be(Buildr::Packaging::Java::JarTask)
    end
    define 'bar' do
      package(:type=>:war, :file=>'bar.war').class.should be(Buildr::Packaging::Java::WarTask)
    end
  end

  it 'should fail if packaging not supported' do
    lambda { define('foo') { package(:weirdo, :file=>'foo.zip') } }.should raise_error(RuntimeError, /Don't know how to create a package/)
  end

  it 'should create different tasks for each file' do
    define 'foo', :version=>'1.0' do
      package(:zip, :file=>'foo.zip')
      package(:jar, :file=>'foo.jar')
    end
    project('foo').packages.uniq.size.should be(2)
  end

  it 'should return the same task for subsequent calls' do
    define 'foo', :version=>'1.0' do
      package(:zip, :file=>'foo.zip').should eql(package(:file=>'foo.zip'))
    end
  end

  it 'should point to specified file' do
    define 'foo', :version=>'1.0' do
      package(:zip, :file=>'foo.zip').should point_to_path('foo.zip')
      package(:zip, :file=>'target/foo-1.0.zip').should point_to_path('target/foo-1.0.zip')
    end
  end

  it 'should create prerequisite for package task' do
    define 'foo', :version=>'1.0' do
      package(:zip, :file=>'foo.zip')
    end
    project('foo').task('package').prerequisites.should include(*project('foo').packages)
  end

  it 'should create task requiring a build' do
    define 'foo', :version=>'1.0' do
      package(:zip, :file=>'foo.zip').prerequisites.should include(build)
    end
  end

  it 'should create specified file during build' do
    define 'foo', :version=>'1.0' do
      package(:zip, :file=>'foo.zip')
    end
    lambda { project('foo').task('package').invoke }.should change { File.exist?('foo.zip') }.to(true)
  end

  it 'should do nothing for installation/upload' do
    define 'foo', :version=>'1.0' do
      package(:zip, :file=>'foo.zip')
    end
    lambda do
      task('install').invoke
      task('upload').invoke
      task('uninstall').invoke
    end.should_not raise_error
  end

end







describe Rake::Task, ' package' do
  it 'should be local task' do
    define 'foo', :version=>'1.0' do
      package
      define('bar') { package }
    end
    in_original_dir project('foo:bar').base_dir do
      task('package').invoke
      project('foo').package.should_not exist
      project('foo:bar').package.should exist
    end
  end

  it 'should be recursive task' do
    define 'foo', :version=>'1.0' do
      package
      define('bar') { package }
    end
    task('package').invoke
    project('foo').package.should exist
    project('foo:bar').package.should exist
  end

  it 'should create package in target directory' do
    define 'foo', :version=>'1.0' do
      package
      define('bar') { package }
    end
    task('package').invoke
    FileList['**/target/*.zip'].map.sort.should == ['bar/target/foo-bar-1.0.zip', 'target/foo-1.0.zip']
  end
end


describe Rake::Task, ' install' do
  it 'should be local task' do
    define 'foo', :version=>'1.0' do
      package
      define('bar') { package }
    end
    in_original_dir project('foo:bar').base_dir do
      task('install').invoke
      artifacts('foo:foo:zip:1.0', 'foo:foo:pom:1.0').each { |t| t.should_not exist }
      artifacts('foo:foo-bar:zip:1.0', 'foo:foo-bar:pom:1.0').each { |t| t.should exist }
    end
  end

  it 'should be recursive task' do
    define 'foo', :version=>'1.0' do
      package
      define('bar') { package }
    end
    task('install').invoke
    artifacts('foo:foo:zip:1.0', 'foo:foo:pom:1.0', 'foo:foo-bar:zip:1.0', 'foo:foo-bar:pom:1.0').each { |t| t.should exist }
  end

  it 'should create package in local repository' do
    define 'foo', :version=>'1.0' do
      package
      define('bar') { package }
    end
    task('install').invoke
    FileList[repositories.local + '/**/*'].reject { |f| File.directory?(f) }.sort.should == [
      File.expand_path('foo/foo/1.0/foo-1.0.zip', repositories.local),
      File.expand_path('foo/foo/1.0/foo-1.0.pom', repositories.local),
      File.expand_path('foo/foo-bar/1.0/foo-bar-1.0.zip', repositories.local),
      File.expand_path('foo/foo-bar/1.0/foo-bar-1.0.pom', repositories.local)].sort
  end
end


describe Rake::Task, ' uninstall' do
  it 'should be local task' do
    define 'foo', :version=>'1.0' do
      package
      define('bar') { package }
    end
    task('install').invoke
    in_original_dir project('foo:bar').base_dir do
      task('uninstall').invoke
      FileList[repositories.local + '/**/*'].reject { |f| File.directory?(f) }.sort.should == [
        File.expand_path('foo/foo/1.0/foo-1.0.zip', repositories.local),
        File.expand_path('foo/foo/1.0/foo-1.0.pom', repositories.local)].sort
    end
  end

  it 'should be recursive task' do
    define 'foo', :version=>'1.0' do
      package
      define('bar') { package }
    end
    task('install').invoke
    task('uninstall').invoke
    FileList[repositories.local + '/**/*'].reject { |f| File.directory?(f) }.sort.should be_empty
  end
end


describe Rake::Task, ' upload' do
  before do
    repositories.release_to = "file://#{File.expand_path('remote')}"
  end
  
  it 'should be local task' do
    define 'foo', :version=>'1.0' do
      package
      define('bar') { package }
    end
    in_original_dir project('foo:bar').base_dir do
      lambda { task('upload').invoke }.should run_task('foo:bar:upload').but_not('foo:upload')
    end
  end

  it 'should be recursive task' do
    define 'foo', :version=>'1.0' do
      package
      define('bar') { package }
    end
    lambda { task('upload').invoke }.should run_tasks('foo:upload', 'foo:bar:upload')
  end

  it 'should upload artifact and POM' do
    define('foo', :version=>'1.0') { package :jar }
    task('upload').invoke
    { 'remote/foo/foo/1.0/foo-1.0.jar'=>project('foo').package(:jar),
      'remote/foo/foo/1.0/foo-1.0.pom'=>project('foo').package(:jar).pom }.each do |upload, package|
      read(upload).should eql(read(package))
    end
  end

  it 'should upload signatures for artifact and POM' do
    define('foo', :version=>'1.0') { package :jar }
    task('upload').invoke
    { 'remote/foo/foo/1.0/foo-1.0.jar'=>project('foo').package(:jar),
      'remote/foo/foo/1.0/foo-1.0.pom'=>project('foo').package(:jar).pom }.each do |upload, package|
      read("#{upload}.md5").split.first.should eql(Digest::MD5.hexdigest(read(package)))
      read("#{upload}.sha1").split.first.should eql(Digest::SHA1.hexdigest(read(package)))
    end
  end
end


describe Packaging, 'zip' do
  it_should_behave_like 'packaging'
  before { @packaging = :zip }

  it 'should not include META-INF directory' do
    define('foo', :version=>'1.0') { package(:zip) }
    project('foo').package(:zip).invoke
    Zip::ZipFile.open(project('foo').package(:zip).to_s) do |zip|
      zip.entries.map(&:to_s).should_not include('META-INF/')
    end
  end
end


describe Packaging, ' tar' do
  before { @packaging = :tar }
  it_should_behave_like 'packaging'
end


describe Packaging, ' tgz' do
  before { @packaging = :tgz }
  it_should_behave_like 'packaging'
end
