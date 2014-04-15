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


describe Artifact do
  before do
    @spec = { :group=>'com.example', :id=>'library', :type=>:jar, :version=>'2.0' }
    @artifact = artifact(@spec)
    @classified = artifact(@spec.merge(:classifier=>'all'))
    @snapshot = artifact(@spec.merge({ :version=>'2.1-SNAPSHOT' }))
  end

  it 'should act as one' do
    @artifact.should respond_to(:to_spec)
  end

  it 'should have an artifact identifier' do
    @artifact.id.should eql('library')
  end

  it 'should have a group identifier' do
    @artifact.group.should eql('com.example')
  end

  it 'should have a version number' do
    @artifact.version.should eql('2.0')
  end

  it 'should know if it is a snapshot' do
    @artifact.should_not be_snapshot
    @classified.should_not be_snapshot
    @snapshot.should be_snapshot
  end

  it 'should have a file type' do
    @artifact.type.should eql(:jar)
  end

  it 'should understand classifier' do
    @artifact.classifier.should be_nil
    @classified.classifier.should eql('all')
  end

  it 'should return hash specification' do
    @artifact.to_hash.should == @spec
    @artifact.to_spec_hash.should == @spec
    @classified.to_hash.should == @spec.merge(:classifier=>'all')
  end

  it 'should return string specification' do
    @artifact.to_spec.should eql('com.example:library:jar:2.0')
    @classified.to_spec.should eql('com.example:library:jar:all:2.0')
  end

  it 'should have associated POM artifact' do
    @artifact.pom.to_hash.should == @artifact.to_hash.merge(:type=>:pom)
  end

  it 'should have one POM artifact for all classifiers' do
    @classified.pom.to_hash.should == @classified.to_hash.merge(:type=>:pom).except(:classifier)
  end
  
  it 'should have associated sources artifact' do
    @artifact.sources_artifact.to_hash.should == @artifact.to_hash.merge(:classifier=>'sources')
  end
  
  it 'should download file if file does not exist' do
    lambda { @artifact.invoke }.should raise_error(Exception, /No remote repositories/)
    lambda { @classified.invoke }.should raise_error(Exception, /No remote repositories/)
  end

  it 'should not download file if file exists' do
    write repositories.locate(@artifact)
    lambda { @artifact.invoke }.should_not raise_error
    write repositories.locate(@classified)
    lambda { @classified.invoke }.should_not raise_error
  end

  it 'should handle lack of POM gracefully' do
    repositories.remote = 'http://example.com'
    URI.should_receive(:download).twice { |uri, target, options| raise URI::NotFoundError if uri.to_s.ends_with('.pom') }
    lambda { @artifact.invoke }.should_not raise_error
  end

  it 'should pass if POM provided' do
    repositories.remote = 'http://example.com'
    @artifact.pom.enhance { |task| write task.name, @artifact.pom_xml }
    write repositories.locate(@artifact)
    lambda { @artifact.invoke }.should_not raise_error
  end

  it 'should pass if POM not required' do
    repositories.remote = 'http://example.com'
    class << @artifact ; def pom() ; end ; end
    write repositories.locate(@artifact)
    lambda { @artifact.invoke }.should_not raise_error
  end

  it 'should not download file if dry-run' do
    dryrun do 
      lambda { @artifact.invoke }.should_not raise_error
      lambda { @classified.invoke }.should_not raise_error
    end
  end

  it 'should resolve to path in local repository' do
    @artifact.to_s.should == File.join(repositories.local, 'com/example/library/2.0/library-2.0.jar')
    @classified.to_s.should == File.join(repositories.local, 'com/example/library/2.0/library-2.0-all.jar')
  end

  it 'should return a list of all registered artifact specifications' do
    define('foo', :version=>'1.0') { package :jar }
    Artifact.list.should include(@artifact.to_spec)
    Artifact.list.should include(@classified.to_spec)
    Artifact.list.should include('foo:foo:jar:1.0')
  end
end


describe Repositories, 'local' do
  it 'should default to .m2 path' do
    # For convenience, sandbox actually sets the local repository to a temp directory
    repositories.local = nil
    repositories.local.should eql(File.expand_path('.m2/repository', ENV['HOME']))
  end

  it 'should be settable' do
    repositories.local = '.m2/local'
    repositories.local.should eql(File.expand_path('.m2/local'))
  end

  it 'should reset to default' do
    repositories.local = '.m2/local'
    repositories.local = nil
    repositories.local.should eql(File.expand_path('~/.m2/repository'))
  end

  it 'should locate file from string specification' do
    repositories.local = nil
    repositories.locate('com.example:library:jar:2.0').should eql(
      File.expand_path('~/.m2/repository/com/example/library/2.0/library-2.0.jar'))
  end

  it 'should locate file from hash specification' do
    repositories.local = nil
    repositories.locate(:group=>'com.example', :id=>'library', :version=>'2.0').should eql(
      File.expand_path('~/.m2/repository/com/example/library/2.0/library-2.0.jar'))
  end

  it 'should load path from settings file' do
    write 'home/.buildr/settings.yaml', <<-YAML
    repositories:
      local: my_repo
    YAML
    repositories.local.should eql(File.expand_path('my_repo'))
  end
end


describe Repositories, 'remote' do
  before do
    @repos = [ 'http://www.ibiblio.org/maven2', 'http://repo1.maven.org/maven2' ]
  end

  it 'should be empty initially' do
    repositories.remote.should be_empty
  end

  it 'should be settable' do
    repositories.remote = @repos.first
    repositories.remote.should eql([@repos.first])
  end

  it 'should be settable from array' do
    repositories.remote = @repos
    repositories.remote.should eql(@repos)
  end

  it 'should add and return repositories in order' do
    @repos.each { |url| repositories.remote << url }
    repositories.remote.should eql(@repos)
  end

  it 'should be used to download artifact' do
    repositories.remote = 'http://example.com'
    URI.should_receive(:download).twice.and_return { |uri, target, options| write target }
    lambda { artifact('com.example:library:jar:2.0').invoke }.
      should change { File.exist?(File.join(repositories.local, 'com/example/library/2.0/library-2.0.jar')) }.to(true)
  end

  it 'should lookup in array order' do
    repositories.remote = [ 'http://example.com', 'http://example.org' ]
    order = ['com', 'org']
    URI.should_receive(:download).any_number_of_times do |uri, target, options|
      order.shift if order.first && uri.to_s[order.first]
      fail URI::NotFoundError unless order.empty?
      write target
    end
    lambda { artifact('com.example:library:jar:2.0').invoke }.should change { order.empty? }
  end

  it 'should fail if artifact not found' do
    repositories.remote = 'http://example.com'
    URI.should_receive(:download).once.ordered.and_return { fail URI::NotFoundError }
    lambda { artifact('com.example:library:jar:2.0').invoke }.should raise_error(RuntimeError, /Failed to download/)
    File.exist?(File.join(repositories.local, 'com/example/library/2.0/library-2.0.jar')).should be_false
  end

  it 'should support artifact classifier' do
    repositories.remote = 'http://example.com'
    URI.should_receive(:download).twice.and_return { |uri, target, options| write target }
    lambda { artifact('com.example:library:jar:all:2.0').invoke }.
      should change { File.exist?(File.join(repositories.local, 'com/example/library/2.0/library-2.0-all.jar')) }.to(true)
  end

  it 'should deal well with repositories URL that lack the last slash' do
    repositories.remote = 'http://example.com/base'
    uri = nil
    URI.should_receive(:download).twice.and_return { |uri, target, options| }
    artifact('group:id:jar:1.0').invoke
    uri.to_s.should eql('http://example.com/base/group/id/1.0/id-1.0.pom')
  end

  it 'should deal well with repositories URL that have the last slash' do
    repositories.remote = 'http://example.com/base/'
    uri = nil
    URI.should_receive(:download).twice.and_return { |uri, target, options| }
    artifact('group:id:jar:1.0').invoke
    uri.to_s.should eql('http://example.com/base/group/id/1.0/id-1.0.pom')
  end
  
  it 'should resolve m2-style deployed snapshots' do
    metadata = <<-XML
    <?xml version='1.0' encoding='UTF-8'?>
    <metadata>
      <groupId>com.example</groupId>
      <artifactId>library</artifactId>
      <version>2.1-SNAPSHOT</version>
      <versioning>
        <snapshot>
          <timestamp>20071012.190008</timestamp>
          <buildNumber>8</buildNumber>
        </snapshot>
        <lastUpdated>20071012190008</lastUpdated>
      </versioning>
    </metadata>
    XML
    repositories.remote = 'http://example.com'
    URI.should_receive(:download).twice.with(uri(/2.1-SNAPSHOT\/library-2.1-SNAPSHOT.(jar|pom)$/), anything()).
      and_return { fail URI::NotFoundError }
    URI.should_receive(:download).twice.with(uri(/2.1-SNAPSHOT\/maven-metadata.xml$/), duck_type(:write)).
      and_return { |uri, target, options| target.write(metadata) }
    URI.should_receive(:download).twice.with(uri(/2.1-SNAPSHOT\/library-2.1-20071012.190008-8.(jar|pom)$/), /2.1-SNAPSHOT\/library-2.1-SNAPSHOT.(jar|pom)$/).
      and_return { |uri, target, options| write target }
    lambda { artifact('com.example:library:jar:2.1-SNAPSHOT').invoke }.
      should change { File.exist?(File.join(repositories.local, 'com/example/library/2.1-SNAPSHOT/library-2.1-SNAPSHOT.jar')) }.to(true)
  end
  
  it 'should handle missing maven metadata by reporting the artifact unavailable' do
    repositories.remote = 'http://example.com'
    URI.should_receive(:download).with(uri(/2.1-SNAPSHOT\/library-2.1-SNAPSHOT.jar$/), anything()).
      and_return { fail URI::NotFoundError }
    URI.should_receive(:download).with(uri(/2.1-SNAPSHOT\/maven-metadata.xml$/), duck_type(:write)).
      and_return { fail URI::NotFoundError }
    lambda { artifact('com.example:library:jar:2.1-SNAPSHOT').invoke }.should raise_error(RuntimeError, /Failed to download/)
    File.exist?(File.join(repositories.local, 'com/example/library/2.1-SNAPSHOT/library-2.1-SNAPSHOT.jar')).should be_false
  end
  
  it 'should handle missing m2 snapshots by reporting the artifact unavailable' do
    metadata = <<-XML
    <?xml version='1.0' encoding='UTF-8'?>
    <metadata>
      <groupId>com.example</groupId>
      <artifactId>library</artifactId>
      <version>2.1-SNAPSHOT</version>
      <versioning>
        <snapshot>
          <timestamp>20071012.190008</timestamp>
          <buildNumber>8</buildNumber>
        </snapshot>
        <lastUpdated>20071012190008</lastUpdated>
      </versioning>
    </metadata>
    XML
    repositories.remote = 'http://example.com'
    URI.should_receive(:download).with(uri(/2.1-SNAPSHOT\/library-2.1-SNAPSHOT.jar$/), anything()).
      and_return { fail URI::NotFoundError }
    URI.should_receive(:download).with(uri(/2.1-SNAPSHOT\/maven-metadata.xml$/), duck_type(:write)).
      and_return { |uri, target, options| target.write(metadata) }
    URI.should_receive(:download).with(uri(/2.1-SNAPSHOT\/library-2.1-20071012.190008-8.jar$/), anything()).
      and_return { fail URI::NotFoundError }
    lambda { artifact('com.example:library:jar:2.1-SNAPSHOT').invoke }.should raise_error(RuntimeError, /Failed to download/)
    File.exist?(File.join(repositories.local, 'com/example/library/2.1-SNAPSHOT/library-2.1-SNAPSHOT.jar')).should be_false
  end

  it 'should load with all repositories specified in settings file' do
    write 'home/.buildr/settings.yaml', <<-YAML
    repositories:
      remote:
      - http://example.com
      - http://example.org
    YAML
    repositories.remote.should include('http://example.com', 'http://example.org')
  end

  it 'should load with all repositories specified in build.yaml file' do
    write 'build.yaml', <<-YAML
    repositories:
      remote:
      - http://example.com
      - http://example.org
    YAML
    repositories.remote.should include('http://example.com', 'http://example.org')
  end

  it 'should load with all repositories specified in settings and build.yaml files' do
    write 'home/.buildr/settings.yaml', <<-YAML
    repositories:
      remote:
      - http://example.com
    YAML
    write 'build.yaml', <<-YAML
    repositories:
      remote:
      - http://example.org
    YAML
    repositories.remote.should include('http://example.com', 'http://example.org')
  end
end


describe Repositories, 'release_to' do
  it 'should accept URL as first argument' do
    repositories.release_to = 'http://example.com'
    repositories.release_to.should == { :url=>'http://example.com' }
  end

  it 'should accept hash with options' do
    repositories.release_to = { :url=>'http://example.com', :username=>'john' }
    repositories.release_to.should == { :url=>'http://example.com', :username=>'john' }
  end

  it 'should allow the hash to be manipulated' do
    repositories.release_to = 'http://example.com'
    repositories.release_to.should == { :url=>'http://example.com' }
    repositories.release_to[:username] = 'john'
    repositories.release_to.should == { :url=>'http://example.com', :username=>'john' }
  end

  it 'should load URL from settings file' do
    write 'home/.buildr/settings.yaml', <<-YAML
    repositories:
      release_to: http://john:secret@example.com
    YAML
    repositories.release_to.should == { :url=>'http://john:secret@example.com' }
  end

  it 'should load URL, username and password from settings file' do
    write 'home/.buildr/settings.yaml', <<-YAML
    repositories:
      release_to:
        url: http://example.com
        username: john
        password: secret
    YAML
    repositories.release_to.should == { :url=>'http://example.com', :username=>'john', :password=>'secret' }
  end
end


describe Buildr, '#artifact' do
  before { @spec = { :group=>'com.example', :id=>'library', :type=>'jar', :version=>'2.0' } }

  it 'should accept hash specification' do
    artifact(:group=>'com.example', :id=>'library', :type=>'jar', :version=>'2.0').should respond_to(:invoke)
  end

  it 'should reject partial hash specifier' do
    lambda { artifact(@spec.merge(:group=>nil)) }.should raise_error
    lambda { artifact(@spec.merge(:id=>nil)) }.should raise_error
    lambda { artifact(@spec.merge(:version=>nil)) }.should raise_error
  end

  it 'should complain about invalid key' do
    lambda { artifact(@spec.merge(:error=>true)) }.should raise_error(ArgumentError, /no such option/i)
  end
  
  it 'should use JAR type by default' do
    artifact(@spec.merge(:type=>nil)).should respond_to(:invoke)
  end

  it 'should accept string specification' do
    artifact('com.example:library:jar:2.0').should respond_to(:invoke)
  end

  it 'should reject partial string specifier' do
    artifact('com.example:library::2.0') 
    lambda { artifact('com.example:library:jar') }.should raise_error
    lambda { artifact('com.example:library:jar:') }.should raise_error
    lambda { artifact('com.example:library::2.0') }.should_not raise_error
    lambda { artifact('com.example::jar:2.0') }.should raise_error
    lambda { artifact(':library:jar:2.0') }.should raise_error
  end

  it 'should create a task naming the artifact in the local repository' do
    file = File.join(repositories.local, 'com', 'example', 'library', '2.0', 'library-2.0.jar')
    Rake::Task.task_defined?(file).should be_false
    artifact('com.example:library:jar:2.0').name.should eql(file)
  end

  it 'should use from method to install artifact from existing file' do
    write 'test.jar'
    artifact = artifact('group:id:jar:1.0').from('test.jar')
    lambda { artifact.invoke }.should change { File.exist?(artifact.to_s) }.to(true)
  end

  it 'should reference artifacts defined on build.yaml by using ruby symbols' do
    write 'build.yaml', <<-YAML
      artifacts: 
        j2ee: geronimo-spec:geronimo-spec-j2ee:jar:1.4-rc4
    YAML
    Buildr.application.send(:load_artifact_ns)
    artifact(:j2ee).to_s.pathmap('%f').should == 'geronimo-spec-j2ee-1.4-rc4.jar'
  end
end


describe Buildr, '#artifacts' do
  it 'should return a list of artifacts from all its arguments' do
    specs = [ 'saxon:saxon:jar:8.4', 'saxon:saxon-dom:jar:8.4', 'saxon:saxon-xpath:jar:8.4' ]
    artifacts(*specs).should eql(specs.map { |spec| artifact(spec) })
  end

  it 'should accept nested arrays' do
    specs = [ 'saxon:saxon:jar:8.4', 'saxon:saxon-dom:jar:8.4', 'saxon:saxon-xpath:jar:8.4' ]
    artifacts([[specs[0]]], [[specs[1]], specs[2]]).should eql(specs.map { |spec| artifact(spec) })
  end

  it 'should accept struct' do
    specs = struct(:main=>'saxon:saxon:jar:8.4', :dom=>'saxon:saxon-dom:jar:8.4', :xpath=>'saxon:saxon-xpath:jar:8.4')
    artifacts(specs).should eql(specs.values.map { |spec| artifact(spec) })
  end

  it 'should ignore duplicates' do
    artifacts('saxon:saxon:jar:8.4', 'saxon:saxon:jar:8.4').size.should be(1)
  end

  it 'should accept and return existing tasks' do
    artifacts(task('foo'), task('bar')).should eql([task('foo'), task('bar')])
  end

  it 'should accept filenames and expand them' do
    artifacts('test').map(&:to_s).should eql([File.expand_path('test')])
  end

  it 'should accept filenames and return filenames' do
    artifacts('c:test').first.should be_kind_of(String)
  end

  it 'should accept project and return all its packaging tasks' do
    define 'foobar', :group=>'group', :version=>'1.0' do
      package :jar, :id=>'code'
      package :war, :id=>'webapp'
    end
    foobar = project('foobar')
    artifacts(foobar).should eql([
      task(foobar.path_to('target/code-1.0.jar')),
      task(foobar.path_to('target/webapp-1.0.war'))
    ])
  end

  it 'should complain about an invalid specification' do
    lambda { artifacts(5) }.should raise_error
    lambda { artifacts('group:no:version:') }.should raise_error
  end
end


describe Buildr, '#group' do
  it 'should accept list of artifact identifiers' do
    list = group('saxon', 'saxon-dom', 'saxon-xpath', :under=>'saxon', :version=>'8.4')
    list.should include(artifact('saxon:saxon:jar:8.4'))
    list.should include(artifact('saxon:saxon-dom:jar:8.4'))
    list.should include(artifact('saxon:saxon-xpath:jar:8.4'))
    list.size.should be(3)
  end

  it 'should accept array with artifact identifiers' do
    list = group(%w{saxon saxon-dom saxon-xpath}, :under=>'saxon', :version=>'8.4')
    list.should include(artifact('saxon:saxon:jar:8.4'))
    list.should include(artifact('saxon:saxon-dom:jar:8.4'))
    list.should include(artifact('saxon:saxon-xpath:jar:8.4'))
    list.size.should be(3)
  end
  
  it 'should accept a type' do
    list = group('struts-bean', 'struts-html', :under=>'struts', :type=>'tld', :version=>'1.1')
    list.should include(artifact('struts:struts-bean:tld:1.1'))
    list.should include(artifact('struts:struts-html:tld:1.1'))
    list.size.should be(2)
  end
  
end

describe Buildr, '#install' do
  before do
    @spec = 'group:id:jar:1.0'
    write @file = 'test.jar'
  end

  it 'should return the install task' do
    install.should be(task('install'))
  end

  it 'should accept artifacts to install' do
    install artifact(@spec)
    lambda { install @file }.should raise_error(ArgumentError)
  end

  it 'should install artifact when install task is run' do
    write @file
    install artifact(@spec).from(@file)
    lambda { install.invoke }.should change { File.exist?(artifact(@spec).to_s) }.to(true)
  end

  it 'should install POM alongside artifact' do
    write @file
    install artifact(@spec).from(@file)
    lambda { install.invoke }.should change { File.exist?(artifact(@spec).pom.to_s) }.to(true)
  end
end


describe Buildr, '#upload' do
  before do
    @spec = 'group:id:jar:1.0'
    write @file = 'test.jar'
    repositories.release_to = 'sftp://example.com/base'
  end

  it 'should return the upload task' do
    upload.should be(task('upload'))
  end

  it 'should accept artifacts to upload' do
    upload artifact(@spec)
    lambda { upload @file }.should raise_error(ArgumentError)
  end

  it 'should upload artifact when upload task is run' do
    write @file
    upload artifact(@spec).from(@file)
    URI.should_receive(:upload).once.
      with(URI.parse('sftp://example.com/base/group/id/1.0/id-1.0.jar'), artifact(@spec).to_s, anything)
    URI.should_receive(:upload).once.
      with(URI.parse('sftp://example.com/base/group/id/1.0/id-1.0.pom'), artifact(@spec).pom.to_s, anything)
    upload.invoke
  end
end


describe ActsAsArtifact, '#upload' do
  it 'should be used to upload artifact' do
    artifact = artifact('com.example:library:jar:2.0')
    # Prevent artifact from downloading anything.
    write repositories.locate(artifact)
    write repositories.locate(artifact.pom)
    URI.should_receive(:upload).once.
      with(URI.parse('sftp://example.com/base/com/example/library/2.0/library-2.0.pom'), artifact.pom.to_s, anything)
    URI.should_receive(:upload).once.
      with(URI.parse('sftp://example.com/base/com/example/library/2.0/library-2.0.jar'), artifact.to_s, anything)
    verbose(false) { artifact.upload(:url=>'sftp://example.com/base') }
  end

  it 'should support artifact classifier' do
    artifact = artifact('com.example:library:jar:all:2.0')
    # Prevent artifact from downloading anything.
    write repositories.locate(artifact)
    write repositories.locate(artifact.pom)
    URI.should_receive(:upload).at_least(:once).
      with(URI.parse('sftp://example.com/base/com/example/library/2.0/library-2.0.pom'), artifact.pom.to_s, anything)
    URI.should_receive(:upload).at_least(:once).
      with(URI.parse('sftp://example.com/base/com/example/library/2.0/library-2.0-all.jar'), artifact.to_s, anything)
    verbose(false) { artifact.upload(:url=>'sftp://example.com/base') }
  end

  it 'should complain without any repository configuration' do
    artifact = artifact('com.example:library:jar:2.0')
    # Prevent artifact from downloading anything.
    write repositories.locate(artifact)
    write repositories.locate(artifact.pom)
    lambda { artifact.upload }.should raise_error(Exception, /where to upload/)
  end

  it 'should accept repositories.upload setting' do
    artifact = artifact('com.example:library:jar:2.0')
    # Prevent artifact from downloading anything.
    write repositories.locate(artifact)
    write repositories.locate(artifact.pom)
    URI.should_receive(:upload).at_least(:once)
    repositories.release_to = 'sftp://example.com/base'
    artifact.upload
    lambda { artifact.upload }.should_not raise_error
  end
end


describe Rake::Task, ' artifacts' do
  it 'should download all specified artifacts' do
    artifact 'group:id:jar:1.0'
    repositories.remote = 'http://example.com'
    URI.should_receive(:download).twice.and_return { |uri, target, options| write target }
    task('artifacts').invoke
  end

  it 'should fail if failed to download an artifact' do
    artifact 'group:id:jar:1.0'
    lambda { task('artifacts').invoke }.should raise_error(RuntimeError, /No remote repositories/)
  end

  it 'should succeed if artifact already exists' do
    write repositories.locate(artifact('group:id:jar:1.0'))
    suppress_stdout do
      lambda { task('artifacts').invoke }.should_not raise_error
    end
  end
end


describe Rake::Task, ' artifacts:sources' do

  before do
    task('artifacts:sources').clear
    repositories.remote = 'http://example.com'
  end
  
  it 'should download sources for all specified artifacts' do
    artifact 'group:id:jar:1.0'
    URI.should_receive(:download).any_number_of_times.and_return { |uri, target| write target }
    lambda { task('artifacts:sources').invoke }.should change { File.exist?('home/.m2/repository/group/id/1.0/id-1.0-sources.jar') }.to(true)
  end
  
  it "should not try to download sources for the project's artifacts" do
    define('foo', :version=>'1.0') { package(:jar) }
    URI.should_not_receive(:download)
    task('artifacts:sources').invoke
  end
  
  describe 'when the source artifact does not exist' do
    
    before do
      artifact 'group:id:jar:1.0'
      URI.should_receive(:download).any_number_of_times.and_raise(URI::NotFoundError)
    end
    
    it 'should not fail' do
      lambda { task('artifacts:sources').invoke }.should_not raise_error
    end
    
    it 'should inform the user' do
      lambda { task('artifacts:sources').invoke }.should show_info('Failed to download group:id:jar:sources:1.0. Skipping it.')
    end
  end
end


describe Buildr, '#transitive' do
  before do
    repositories.remote = 'http://example.com'
    @simple = [ 'saxon:saxon:jar:8.4', 'saxon:saxon-dom:jar:8.4', 'saxon:saxon-xpath:jar:8.4' ]
    @simple.map { |spec| artifact(spec).pom }.each { |task| write task.name, task.pom_xml }
    @provided = @simple.first
    @complex = 'group:app:jar:1.0'
    write artifact(@complex).pom.to_s, <<-XML
<project>
  <artifactId>app</artifactId>
  <groupId>group</groupId>
  <dependencies>
    <dependency>
      <artifactId>saxon</artifactId>
      <groupId>saxon</groupId>
      <version>8.4</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <artifactId>saxon-dom</artifactId>
      <groupId>saxon</groupId>
      <version>8.4</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <artifactId>saxon-xpath</artifactId>
      <groupId>saxon</groupId>
      <version>8.4</version>
    </dependency>
    <dependency>
      <artifactId>saxon-nosuch</artifactId>
      <groupId>saxon</groupId>
      <version>8.4</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
XML
    @transitive = 'master:app:war:1.0'
    write artifact(@transitive).pom.to_s, <<-XML
<project>
  <artifactId>app</artifactId>
  <groupId>group</groupId>
  <dependencies>
    <dependency>
      <artifactId>app</artifactId>
      <groupId>group</groupId>
      <version>1.0</version>
    </dependency>
  </dependencies>
</project>
XML
  end

  it 'should return a list of artifacts from all its arguments' do
    specs = [ 'saxon:saxon:jar:8.4', 'saxon:saxon-dom:jar:8.4', 'saxon:saxon-xpath:jar:8.4' ]
    transitive(*specs).should eql(specs.map { |spec| artifact(spec) })
  end

  it 'should accept nested arrays' do
    specs = [ 'saxon:saxon:jar:8.4', 'saxon:saxon-dom:jar:8.4', 'saxon:saxon-xpath:jar:8.4' ]
    transitive([[specs[0]]], [[specs[1]], specs[2]]).should eql(specs.map { |spec| artifact(spec) })
  end

  it 'should accept struct' do
    specs = struct(:main=>'saxon:saxon:jar:8.4', :dom=>'saxon:saxon-dom:jar:8.4', :xpath=>'saxon:saxon-xpath:jar:8.4')
    transitive(specs).should eql(specs.values.map { |spec| artifact(spec) })
  end

  it 'should ignore duplicates' do
    transitive('saxon:saxon:jar:8.4', 'saxon:saxon:jar:8.4').size.should be(1)
  end

  it 'should accept and return existing tasks' do
    transitive(task('foo'), task('bar')).should eql([task('foo'), task('bar')])
  end

  it 'should accept filenames and expand them' do
    transitive('test').map(&:to_s).should eql([File.expand_path('test')])
  end

  it 'should accept filenames and return file task' do
    transitive('c:test').first.should be_kind_of(Rake::FileTask)
  end

  it 'should accept project and return all its packaging tasks' do
    define 'foobar', :group=>'group', :version=>'1.0' do
      package :jar, :id=>'code'
      package :war, :id=>'webapp'
    end
    foobar = project('foobar')
    transitive(foobar).should eql([
      task(foobar.path_to('target/code-1.0.jar')),
      task(foobar.path_to('target/webapp-1.0.war'))
    ])
  end

  it 'should complain about an invalid specification' do
    lambda { transitive(5) }.should raise_error
    lambda { transitive('group:no:version:') }.should raise_error
  end

  it 'should bring artifact and its dependencies' do
    transitive(@complex).should eql(artifacts(@complex, @simple))
  end

  it 'should bring dependencies of POM without artifact itself' do
    transitive(@complex.sub(/jar/, 'pom')).should eql(artifacts(@simple))
  end

  it 'should bring artifact and transitive depenencies' do
    transitive(@transitive).should eql(artifacts(@transitive, @complex, @simple - [@provided]))
  end
end
