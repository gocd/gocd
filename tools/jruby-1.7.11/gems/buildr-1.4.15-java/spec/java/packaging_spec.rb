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
require File.expand_path(File.join(File.dirname(__FILE__), '..', 'packaging', 'packaging_helper'))


describe Project, '#manifest' do
  it 'should include user name' do
    ENV['USER'] = 'MysteriousJoe'
    define('foo').manifest['Build-By'].should eql('MysteriousJoe')
  end

  it 'should include JDK version' do
    define('foo').manifest['Build-Jdk'].should =~ /^1\.\d+\.\w+$/
  end

  it 'should include project comment' do
    desc 'My Project'
    define('foo').manifest['Implementation-Title'].should eql('My Project')
  end

  it 'should include project name if no comment' do
    define('foo').manifest['Implementation-Title'].should eql('foo')
  end

  it 'should include project version' do
    define('foo', :version=>'2.1').manifest['Implementation-Version'].should eql('2.1')
  end

  it 'should not include project version unless specified' do
    define('foo').manifest['Implementation-Version'].should be_nil
  end

  it 'should inherit from parent project' do
    define('foo', :version=>'2.1') { define 'bar' }
    project('foo:bar').manifest['Implementation-Version'].should eql('2.1')
  end

end


shared_examples_for 'package with manifest' do
  before do
    @long_line = 'No line may be longer than 72 bytes (not characters), in its UTF8-encoded form. If a value would make the initial line longer than this, it should be continued on extra lines (each starting with a single SPACE).'
  end

  def package_with_manifest(manifest = nil)
    packaging = @packaging
    @project = define('foo', :version=>'1.2') do
      package packaging
      package(packaging).with(:manifest=>manifest) unless manifest.nil?
    end
  end

  def inspect_manifest(package = nil)
    package ||= project('foo').package(@packaging)
    package.invoke
    yield Buildr::Packaging::Java::Manifest.from_zip(package)
  end

  it 'should include default header when no options specified' do
    ENV['USER'] = 'MysteriousJoe'
    package_with_manifest # Nothing for default.
    inspect_manifest do |manifest|
      manifest.sections.size.should be(1)
      manifest.main.should == {
        'Manifest-Version'        =>'1.0',
        'Created-By'              =>'Buildr',
        'Implementation-Title'    =>@project.name,
        'Implementation-Version'  =>'1.2',
        'Build-Jdk'               =>ENV_JAVA['java.version'],
        'Build-By'                =>'MysteriousJoe'
      }
    end
  end

  it 'should not exist when manifest=false' do
    package_with_manifest false
    @project.package(@packaging).invoke
    Zip::ZipFile.open(@project.package(@packaging).to_s) do |zip|
      zip.file.exist?('META-INF/MANIFEST.MF').should be_false
    end
  end

  it 'should generate a new manifest for a file that does not have one' do
    Zip::ZipOutputStream.open 'tmp.zip' do |zip|
      zip.put_next_entry 'empty.txt'
    end
    begin
      manifest = Buildr::Packaging::Java::Manifest.from_zip('tmp.zip')
      manifest.each do |key, val|
        Buildr::Packaging::Java::Manifest::STANDARD_HEADER.should include(key)
      end
    ensure
      rm 'tmp.zip'
    end
  end

  it 'should map manifest from hash' do
    package_with_manifest 'Foo'=>1, :bar=>'Bar'
    inspect_manifest do |manifest|
      manifest.sections.size.should be(1)
      manifest.main['Manifest-Version'].should eql('1.0')
      manifest.main['Created-By'].should eql('Buildr')
      manifest.main['Foo'].should eql('1')
      manifest.main['bar'].should eql('Bar')
    end
  end

  it 'should close the temporary file used for packaging the MANIFEST.MF file' do
    package_with_manifest 'Foo'=>1, :bar=>'Bar'
    package = project('foo').package(@packaging)
    package.invoke
    module AccessManifestTMP
      attr_reader :manifest_tmp
    end
    (package.dup.extend(AccessManifestTMP).manifest_tmp.closed?).should be_true
  end

  it 'should end hash manifest with EOL' do
    package_with_manifest 'Foo'=>1, :bar=>'Bar'
    package = project('foo').package(@packaging)
    package.invoke
    Zip::ZipFile.open(package.to_s) { |zip| zip.file.read('META-INF/MANIFEST.MF').should =~ /#{Buildr::Packaging::Java::Manifest::LINE_SEPARATOR}$/ }
  end

  it 'should break hash manifest lines longer than 72 characters using continuations' do
    package_with_manifest 'foo'=>@long_line
    package = project('foo').package(@packaging)
    inspect_manifest do |manifest|
      manifest.main['foo'].should == @long_line
    end
  end

  it 'should map manifest from array' do
    package_with_manifest [ { :foo=>'first' }, { :bar=>'second' } ]
    inspect_manifest do |manifest|
      manifest.sections.size.should be(2)
      manifest.main['Manifest-Version'].should eql('1.0')
      manifest.main['foo'].should eql('first')
      manifest.sections.last['bar'].should eql('second')
    end
  end

  it 'should end array manifest with EOL' do
    package_with_manifest [ { :foo=>'first' }, { :bar=>'second' } ]
    package = project('foo').package(@packaging)
    package.invoke
    Zip::ZipFile.open(package.to_s) { |zip| zip.file.read('META-INF/MANIFEST.MF')[-1].should == ?\n }
  end

  it 'should break array manifest lines longer than 72 characters using continuations' do
    package_with_manifest ['foo'=>@long_line]
    package = project('foo').package(@packaging)
    inspect_manifest do |manifest|
      manifest.main['foo'].should == @long_line
    end
  end

  it 'should put Name: at beginning of section' do
    package_with_manifest [ {}, { 'Name'=>'first', :Foo=>'first', :bar=>'second' } ]
    package = project('foo').package(@packaging)
    package.invoke
    inspect_manifest do |manifest|
      manifest.sections[1]["Name"].should == "first"
    end
  end

  it 'should create manifest from proc' do
    package_with_manifest lambda { 'Meta: data' }
    inspect_manifest do |manifest|
      manifest.sections.size.should be(1)
      manifest.main['Manifest-Version'].should eql('1.0')
      manifest.main['Meta'].should eql('data')
    end
  end

  it 'should create manifest from file' do
    write 'MANIFEST.MF', 'Meta: data'
    package_with_manifest 'MANIFEST.MF'
    inspect_manifest do |manifest|
      manifest.sections.size.should be(1)
      manifest.main['Meta'].should eql('data')
    end
  end

  it 'should give 644 permissions to the manifest' do
    package_with_manifest  [ {}, { 'Name'=>'first', :Foo=>'first', :bar=>'second' } ]
    package ||= project('foo').package(@packaging)
    package.invoke
    Zip::ZipFile.open(package.to_s) do |zip|
      permissions = format("%o", zip.file.stat('META-INF/MANIFEST.MF').mode)
      expected_mode = Buildr::Util.win_os? ? /666$/ : /644$/
      permissions.should match expected_mode
    end
  end

  it 'should not add manifest version twice' do
    write 'MANIFEST.MF', 'Manifest-Version: 1.9'
    package_with_manifest 'MANIFEST.MF'
    package ||= project('foo').package(@packaging)
    package.invoke
    Zip::ZipFile.open(package.to_s) do |zip|
      zip.read('META-INF/MANIFEST.MF').scan(/(Manifest-Version)/m).size.should == 1
    end
  end

  it 'should give precedence to version specified in manifest file' do
    write 'MANIFEST.MF', 'Manifest-Version: 1.9'
    package_with_manifest 'MANIFEST.MF'
    inspect_manifest do |manifest|
      manifest.main['Manifest-Version'].should == '1.9'
    end
  end

  it 'should create manifest from task' do
    file 'MANIFEST.MF' do |task|
      write task.to_s, 'Meta: data'
    end
    package_with_manifest 'MANIFEST.MF'
    inspect_manifest do |manifest|
      manifest.sections.size.should be(1)
      manifest.main['Manifest-Version'].should eql('1.0')
      manifest.main['Meta'].should eql('data')
    end
  end

  it 'should respond to with() and accept manifest' do
    write 'DISCLAIMER'
    mkpath 'target/classes'
    packaging = @packaging
    define('foo', :version=>'1.0') { package(packaging).with :manifest=>{'Foo'=>'data'} }
    inspect_manifest { |manifest| manifest.main['Foo'].should eql('data') }
  end

  it 'should include META-INF directory' do
    packaging = @packaging
    package = define('foo', :version=>'1.0') { package(packaging) }.packages.first
    package.invoke
    Zip::ZipFile.open(package.to_s) do |zip|
      zip.entries.map(&:to_s).should include('META-INF/')
    end
  end

  it 'should inherit manifest from parent project' do
    packaging = @packaging
    package = nil
    define('foo', :version => '1.0') do
      manifest['Foo'] = '1'
      package(packaging)
      define('bar', :version => '1.0') do
        manifest['bar'] = 'Bar'
        package(:jar)
        package = packages.first
      end
    end
    inspect_manifest(package) do |manifest|
      manifest.sections.size.should be(1)
      manifest.main['Manifest-Version'].should eql('1.0')
      manifest.main['Created-By'].should eql('Buildr')
      manifest.main['Foo'].should eql('1')
      manifest.main['bar'].should eql('Bar')
    end
  end

  it 'should not modify manifest of parent project' do
    packaging = @packaging
    define('foo', :version => '1.0') do
      manifest['Foo'] = '1'
      package(packaging)
      define('bar', :version => '1.0') do
        manifest['bar'] = 'Bar'
        package(:jar)
      end
      define('baz', :version => '1.0') do
        manifest['baz'] = 'Baz'
        package(:jar)
      end
    end
    inspect_manifest(project('foo').packages.first) do |manifest|
      manifest.sections.size.should be(1)
      manifest.main['Manifest-Version'].should eql('1.0')
      manifest.main['Created-By'].should eql('Buildr')
      manifest.main['Foo'].should eql('1')
      manifest.main['bar'].should be_nil
      manifest.main['baz'].should be_nil
    end
    inspect_manifest(project('foo:bar').packages.first) do |manifest|
      manifest.sections.size.should be(1)
      manifest.main['Manifest-Version'].should eql('1.0')
      manifest.main['Created-By'].should eql('Buildr')
      manifest.main['Foo'].should eql('1')
      manifest.main['bar'].should eql('Bar')
      manifest.main['baz'].should be_nil
    end
    inspect_manifest(project('foo:baz').packages.first) do |manifest|
      manifest.sections.size.should be(1)
      manifest.main['Manifest-Version'].should eql('1.0')
      manifest.main['Created-By'].should eql('Buildr')
      manifest.main['Foo'].should eql('1')
      manifest.main['baz'].should eql('Baz')
      manifest.main['bar'].should be_nil
    end
  end
end


describe Project, '#meta_inf' do
  it 'should by an array' do
    define('foo').meta_inf.should be_kind_of(Array)
  end

  it 'should include LICENSE file if found' do
    write 'LICENSE'
    define('foo').meta_inf.first.should point_to_path('LICENSE')
  end

  it 'should be empty unless LICENSE exists' do
    define('foo').meta_inf.should be_empty
  end

  it 'should inherit from parent project' do
    write 'LICENSE'
    define('foo') { define 'bar' }
    project('foo:bar').meta_inf.first.should point_to_path('LICENSE')
  end

  it 'should expect LICENSE file parent project' do
    write 'bar/LICENSE'
    define('foo') { define 'bar' }
    project('foo:bar').meta_inf.should be_empty
  end
end


shared_examples_for 'package with meta_inf' do

  def package_with_meta_inf(meta_inf = nil)
    packaging = @packaging
    @project = Buildr.define('foo', :version=>'1.2') do
      package packaging
      package(packaging).with(:meta_inf=>meta_inf) if meta_inf
    end
  end

  def inspect_meta_inf
    package = project('foo').package(@packaging)
    package.invoke
    assumed = Array(@meta_inf_ignore)
    Zip::ZipFile.open(package.to_s) do |zip|
      entries = zip.entries.map(&:name).select { |f| File.dirname(f) == 'META-INF' }.map { |f| File.basename(f) }
      assumed.each { |f| entries.should include(f) }
      yield entries - assumed if block_given?
    end
  end

  it 'should default to LICENSE file' do
    write 'LICENSE'
    package_with_meta_inf
    inspect_meta_inf { |files| files.should eql(['LICENSE']) }
  end

  it 'should be empty if no LICENSE file' do
    package_with_meta_inf
    inspect_meta_inf { |files| files.should be_empty }
  end

  it 'should include file specified by :meta_inf option' do
    write 'README'
    package_with_meta_inf 'README'
    inspect_meta_inf { |files| files.should eql(['README']) }
  end

  it 'should include files specified by :meta_inf option' do
    files = ['README', 'DISCLAIMER'].each { |file| write file }
    package_with_meta_inf files
    inspect_meta_inf { |files| files.should eql(files) }
  end

  it 'should include file task specified by :meta_inf option' do
    file('README') { |task| write task.to_s }
    package_with_meta_inf file('README')
    inspect_meta_inf { |files| files.should eql(['README']) }
  end

  it 'should include file tasks specified by :meta_inf option' do
    files = ['README', 'DISCLAIMER'].each { |file| file(file) { |task| write task.to_s } }
    package_with_meta_inf files.map { |f| file(f) }
    inspect_meta_inf { |files| files.should eql(files) }
  end

  it 'should complain if cannot find file' do
    package_with_meta_inf 'README'
    lambda { inspect_meta_inf }.should raise_error(RuntimeError, /README/)
  end

  it 'should complain if cannot build task' do
    file('README')  { fail 'Failed' }
    package_with_meta_inf 'README'
    lambda { inspect_meta_inf }.should raise_error(RuntimeError, /Failed/)
  end

  it 'should respond to with() and accept manifest and meta_inf' do
    write 'DISCLAIMER'
    mkpath 'target/classes'
    packaging = @packaging
    define('foo', :version=>'1.0') { package(packaging).with :meta_inf=>'DISCLAIMER' }
    inspect_meta_inf { |files| files.should eql(['DISCLAIMER']) }
  end
end


describe Packaging, 'jar' do
  it_should_behave_like 'packaging'
  before { @packaging = :jar }
  it_should_behave_like 'package with manifest'
  it_should_behave_like 'package with meta_inf'
  before { @meta_inf_ignore = 'MANIFEST.MF' }

  it 'should place the manifest as the first entry of the file' do
    write 'src/main/java/Test.java', 'class Test {}'
    define('foo', :version=>'1.0') { package(:jar) }
    project('foo').package(:jar).invoke
    Zip::ZipFile.open(project('foo').package(:jar).to_s) do |jar|
      entries_to_s = jar.entries.map(&:to_s).delete_if {|entry| entry[-1,1] == "/"}
      # Sometimes META-INF/ is counted as first entry, which is fair game.
      (entries_to_s.first == 'META-INF/MANIFEST.MF' || entries_to_s[1] == 'META-INF/MANIFEST.MF').should be_true
    end
  end

  it 'should use files from compile directory if nothing included' do
    write 'src/main/java/Test.java', 'class Test {}'
    define('foo', :version=>'1.0') { package(:jar) }
    project('foo').package(:jar).invoke
    Zip::ZipFile.open(project('foo').package(:jar).to_s) do |jar|
      jar.entries.map(&:to_s).sort.should include('META-INF/MANIFEST.MF', 'Test.class')
    end
  end

  it 'should use files from resources directory if nothing included' do
    write 'src/main/resources/test/important.properties'
    define('foo', :version=>'1.0') { package(:jar) }
    project('foo').package(:jar).invoke
    Zip::ZipFile.open(project('foo').package(:jar).to_s) do |jar|
      jar.entries.map(&:to_s).sort.should include('test/important.properties')
    end
  end

  it 'should include class directories' do
    write 'src/main/java/code/Test.java', 'package code ; class Test {}'
    define('foo', :version=>'1.0') { package(:jar) }
    project('foo').package(:jar).invoke
    Zip::ZipFile.open(project('foo').package(:jar).to_s) do |jar|
      jar.entries.map(&:to_s).sort.should include('code/')
    end
  end

  it 'should include resource files starting with dot' do
    write 'src/main/resources/test/.config'
    define('foo', :version=>'1.0') { package(:jar) }
    project('foo').package(:jar).invoke
    Zip::ZipFile.open(project('foo').package(:jar).to_s) do |jar|
      jar.entries.map(&:to_s).sort.should include('test/.config')
    end
  end

  it 'should include empty resource directories' do
    mkpath 'src/main/resources/empty'
    define('foo', :version=>'1.0') { package(:jar) }
    project('foo').package(:jar).invoke
    Zip::ZipFile.open(project('foo').package(:jar).to_s) do |jar|
      jar.entries.map(&:to_s).sort.should include('empty/')
    end
  end

  it 'should raise error when calling with() with nil value' do
    lambda {
      define('foo', :version=>'1.0') { package(:jar).with(nil) }
    }.should raise_error
  end

  it 'should exclude resources when ordered to do so' do
    write 'src/main/resources/foo.xml', ''
    foo = define('foo', :version => '1.0') { package(:jar).exclude('foo.xml')}
    foo.package(:jar).invoke
    Zip::ZipFile.open(foo.package(:jar).to_s) do |jar|
      jar.entries.map(&:to_s).sort.should_not include('foo.xml')
    end
  end

end


describe Packaging, 'war' do
  it_should_behave_like 'packaging'
  before { @packaging = :war }
  it_should_behave_like 'package with manifest'
  it_should_behave_like 'package with meta_inf'
  before { @meta_inf_ignore = 'MANIFEST.MF' }

  def make_jars
    artifact('group:id:jar:1.0') { |t| write t.to_s }
    artifact('group:id:jar:2.0') { |t| write t.to_s }
  end

  def inspect_war
    project('foo').package(:war).invoke
    Zip::ZipFile.open(project('foo').package(:war).to_s) do |war|
      yield war.entries.map(&:to_s).sort
    end
  end

  it 'should use files from webapp directory if nothing included' do
    write 'src/main/webapp/test.html'
    define('foo', :version=>'1.0') { package(:war) }
    inspect_war { |files| files.should include('test.html') }
  end

  it 'should use files from added assets directory if nothing included' do
    write 'generated/main/webapp/test.html'
    define('foo', :version => '1.0') { assets.paths << 'generated/main/webapp/'; package(:war) }
    inspect_war { |files| files.should include('test.html') }
  end

  it 'should use files from generated assets directory if nothing included' do
    write 'generated/main/webapp/test.html'
    define('foo', :version => '1.0') do
      target_dir = _('generated/main/webapp')
      assets.paths << project.file(target_dir) do
        mkdir_p target_dir
        touch "#{target_dir}/test.html"
        touch target_dir
      end
      package(:war)
    end
    inspect_war { |files| files.should include('test.html') }
  end

  it 'should accept files from :classes option', :retry => (Buildr::Util.win_os? ? 4 : 1) do
    write 'classes/test'
    define('foo', :version=>'1.0') { package(:war).with(:classes=>'classes') }
    inspect_war { |files| files.should include('WEB-INF/classes/test') }
  end

  it 'should use files from compile directory if nothing included' do
    write 'src/main/java/Test.java', 'class Test {}'
    define('foo', :version=>'1.0') { package(:war) }
    inspect_war { |files| files.should include('WEB-INF/classes/Test.class') }
  end

  it 'should ignore compile directory if no source files to compile' do
    define('foo', :version=>'1.0') { package(:war) }
    inspect_war { |files| files.should_not include('target/classes') }
  end

  it 'should include only specified classes directories' do
    write 'src/main/java'
    define('foo', :version=>'1.0') { package(:war).with :classes=>_('additional') }
    project('foo').package(:war).classes.should_not include(project('foo').file('target/classes'))
    project('foo').package(:war).classes.should include(project('foo').file('additional'))
  end

  it 'should use files from resources directory if nothing included' do
    write 'src/main/resources/test/important.properties'
    define('foo', :version=>'1.0') { package(:war) }
    inspect_war { |files| files.should include('WEB-INF/classes/test/important.properties') }
  end

  it 'should include empty resource directories' do
    mkpath 'src/main/resources/empty'
    define('foo', :version=>'1.0') { package(:war) }
    inspect_war { |files| files.should include('WEB-INF/classes/empty/') }
  end

  it 'should accept file from :libs option' do
    write 'lib/foo.jar'
    define('foo', :version=>'1.0') { package(:war).libs << 'lib/foo.jar' }
    inspect_war { |files| files.should include('META-INF/MANIFEST.MF', 'WEB-INF/lib/foo.jar') }
  end


  it 'should accept artifacts from :libs option' do
    make_jars
    define('foo', :version=>'1.0') { package(:war).with(:libs=>'group:id:jar:1.0') }
    inspect_war { |files| files.should include('META-INF/MANIFEST.MF', 'WEB-INF/lib/id-1.0.jar') }
  end

  it 'should accept artifacts from :libs option' do
    make_jars
    define('foo', :version=>'1.0') { package(:war).with(:libs=>['group:id:jar:1.0', 'group:id:jar:2.0']) }
    inspect_war { |files| files.should include('META-INF/MANIFEST.MF', 'WEB-INF/lib/id-1.0.jar', 'WEB-INF/lib/id-2.0.jar') }
  end

  it 'should use artifacts from compile classpath if no libs specified' do
    make_jars
    define('foo', :version=>'1.0') { compile.with 'group:id:jar:1.0', 'group:id:jar:2.0' ; package(:war) }
    inspect_war { |files| files.should include('META-INF/MANIFEST.MF', 'WEB-INF/lib/id-1.0.jar', 'WEB-INF/lib/id-2.0.jar') }
  end

  it 'should use artifacts from compile classpath if no libs specified, leaving the user specify which to exclude as files' do
    make_jars
    define('foo', :version=>'1.0') { compile.with 'group:id:jar:1.0', 'group:id:jar:2.0' ; package(:war).path('WEB-INF/lib').exclude('id-2.0.jar')  }
    inspect_war { |files| files.should include('META-INF/MANIFEST.MF', 'WEB-INF/lib/id-1.0.jar') }
  end

  it 'should use artifacts from compile classpath if no libs specified, leaving the user specify which to exclude as files with glob expressions' do
    make_jars
    define('foo', :version=>'1.0') { compile.with 'group:id:jar:1.0', 'group:id:jar:2.0' ; package(:war).path('WEB-INF/lib').exclude('**/id-2.0.jar')   }
    inspect_war { |files| files.should include('META-INF/MANIFEST.MF', 'WEB-INF/lib/id-1.0.jar') }
  end

  it 'should exclude files regardless of the path where they are included, using wildcards' do
    make_jars
    define('foo', :version=>'1.0') { compile.with 'group:id:jar:1.0', 'group:id:jar:2.0' ; package(:war).exclude('**/id-2.0.jar')   }
    inspect_war { |files| files.should include('META-INF/MANIFEST.MF', 'WEB-INF/lib/id-1.0.jar') }
  end

  it 'should exclude files regardless of the path where they are included, specifying target path entirely' do
     make_jars
     define('foo', :version=>'1.0') { compile.with 'group:id:jar:1.0', 'group:id:jar:2.0' ; package(:war).exclude('WEB-INF/lib/id-2.0.jar')   }
     inspect_war { |files| files.should include('META-INF/MANIFEST.MF', 'WEB-INF/lib/id-1.0.jar') }
   end

  it 'should exclude files regardless of the path where they are included for war files' do
     write 'src/main/java/com/example/included/Test.java', 'package com.example.included; class Test {}'
     write 'src/main/java/com/example/excluded/Test.java', 'package com.example.excluded; class Test {}'
     define('foo', :version=>'1.0') do
       package(:war).enhance do |war|
         war.exclude('WEB-INF/classes/com/example/excluded/**.class')
       end
     end
     inspect_war do |files|
       files.should include('WEB-INF/classes/com/example/included/Test.class')
       files.should_not include('WEB-INF/classes/com/example/excluded/Test.class')
     end
   end

  it 'should include only specified libraries' do
    define 'foo', :version=>'1.0' do
      compile.with 'group:id:jar:1.0'
      package(:war).with :libs=>'additional:id:jar:1.0'
    end
    project('foo').package(:war).libs.should_not include(artifact('group:id:jar:1.0'))
    project('foo').package(:war).libs.should include(artifact('additional:id:jar:1.0'))
  end

end


describe Packaging, 'aar' do
  it_should_behave_like 'packaging'
  before { @packaging = :aar }
  it_should_behave_like 'package with manifest'
  it_should_behave_like 'package with meta_inf'
  before do
    write 'src/main/axis2/services.xml'
    @meta_inf_ignore = ['MANIFEST.MF', 'services.xml']
  end

  def make_jars
    artifact('group:id:jar:1.0') { |t| write t.to_s }
    artifact('group:id:jar:2.0') { |t| write t.to_s }
  end

  def inspect_aar
    project('foo').package(:aar).invoke
    Zip::ZipFile.open(project('foo').package(:aar).to_s) do |aar|
      yield aar.entries.map(&:to_s).sort
    end
  end

  it 'should automatically include services.xml and any *.wsdl files under src/main/axis2' do
    write 'src/main/axis2/my-service.wsdl'
    define('foo', :version=>'1.0') { package(:aar) }
    inspect_aar { |files| files.should include('META-INF/MANIFEST.MF', 'META-INF/services.xml', 'META-INF/my-service.wsdl') }
  end

  it 'should accept files from :include option' do
    write 'test'
    define('foo', :version=>'1.0') { package(:aar).include 'test' }
    inspect_aar { |files| files.should include('META-INF/MANIFEST.MF', 'test') }
  end

  it 'should use files from compile directory if nothing included' do
    write 'src/main/java/Test.java', 'class Test {}'
    define('foo', :version=>'1.0') { package(:aar) }
    inspect_aar { |files| files.should include('Test.class') }
  end

  it 'should use files from resources directory if nothing included' do
    write 'src/main/resources/test/important.properties'
    define('foo', :version=>'1.0') { package(:aar) }
    inspect_aar { |files| files.should include('test/important.properties') }
  end

  it 'should include empty resource directories' do
    mkpath 'src/main/resources/empty'
    define('foo', :version=>'1.0') { package(:aar) }
    inspect_aar { |files| files.should include('empty/') }
  end

  it 'should accept file from :libs option' do
    make_jars
    define('foo', :version=>'1.0') { package(:aar).with :libs=>'group:id:jar:1.0' }
    inspect_aar { |files| files.should include('META-INF/MANIFEST.MF', 'lib/id-1.0.jar') }
  end

  it 'should accept file from :libs option' do
    make_jars
    define('foo', :version=>'1.0') { package(:aar).with :libs=>['group:id:jar:1.0', 'group:id:jar:2.0'] }
    inspect_aar { |files| files.should include('META-INF/MANIFEST.MF', 'lib/id-1.0.jar', 'lib/id-2.0.jar') }
  end

  it 'should NOT use artifacts from compile classpath if no libs specified' do
    make_jars
    define('foo', :version=>'1.0') { compile.with 'group:id:jar:1.0', 'group:id:jar:2.0' ; package(:aar) }
    inspect_aar { |files| files.should include('META-INF/MANIFEST.MF') }
  end

  it 'should return all libraries from libs attribute' do
    define 'foo', :version=>'1.0' do
      compile.with 'group:id:jar:1.0'
      package(:aar).with :libs=>'additional:id:jar:1.0'
    end
    project('foo').package(:aar).libs.should_not include(artifact('group:id:jar:1.0'))
    project('foo').package(:aar).libs.should include(artifact('additional:id:jar:1.0'))
  end

end


describe Packaging, 'ear' do
  it_should_behave_like 'packaging'
  before { @packaging = :ear }
  it_should_behave_like 'package with manifest'
  it_should_behave_like 'package with meta_inf'
  before { @meta_inf_ignore = ['MANIFEST.MF', 'application.xml'] }

  def inspect_ear
    project('foo').package(:ear).invoke
    Zip::ZipFile.open(project('foo').package(:ear).to_s) do |ear|
      yield ear.entries.map(&:to_s).sort
    end
  end

  def inspect_application_xml
    project('foo').package(:ear).invoke
    Zip::ZipFile.open(project('foo').package(:ear).to_s) do |ear|
      yield REXML::Document.new(ear.read('META-INF/application.xml')).root
    end
  end

  def inspect_classpath(package)
    project('foo').package(:ear).invoke
    Zip::ZipFile.open(project('foo').package(:ear).to_s) do |ear|
      File.open('tmp.zip', 'wb') do |tmp|
        tmp.write ear.file.read(package)
      end
      manifest = Buildr::Packaging::Java::Manifest.from_zip('tmp.zip')
      yield manifest.main['Class-Path'].split(' ')
    end
  end

  it 'should set display name from project id' do
    define 'foo', :version=>'1.0' do
      package(:ear).display_name.should eql('foo')
      define 'bar' do
        package(:ear).display_name.should eql('foo-bar')
      end
    end
  end

  it 'should set display name in application.xml' do
    define 'foo', :version=>'1.0' do
      package(:ear)
    end
    inspect_application_xml { |xml| xml.get_text('/application/display-name').should == 'foo' }
  end

  it 'should accept different display name' do
    define 'foo', :version=>'1.0' do
      package(:ear).display_name = 'bar'
    end
    inspect_application_xml { |xml| xml.get_text('/application/display-name').should == 'bar' }
  end

  it 'should set description in application.xml to project comment if not specified' do
    desc "MyDescription"
    define 'foo', :version=>'1.0' do
      package(:ear)
    end
    inspect_application_xml { |xml| xml.get_text('/application/description').should == 'MyDescription' }
  end

  it 'should not set description in application.xml if not specified and no project comment' do
    define 'foo', :version=>'1.0' do
      package(:ear)
    end
    inspect_application_xml { |xml| xml.get_text('/application/description').should be_nil }
  end

  it 'should set description in application.xml if specified' do
    define 'foo', :version=>'1.0' do
      package(:ear).description = "MyDescription"
    end
    inspect_application_xml { |xml| xml.get_text('/application/description').should == 'MyDescription' }
  end

  it 'should add security-roles to application.xml if given' do
    define 'foo', :version=>'1.0' do
	  package(:ear).security_roles << {:id=>'sr1',
		:description=>'System Administrator', :name=>'systemadministrator'}
	end
	inspect_application_xml do |xml|
		xml.get_text("/application/security-role[@id='sr1']/description").to_s.should eql('System Administrator')
		xml.get_text("/application/security-role[@id='sr1']/role-name").to_s.should eql('systemadministrator')
	end
  end

  it 'should map WARs to /war directory' do
    define 'foo', :version=>'1.0' do
      package(:ear) << package(:war)
    end
    inspect_ear { |files| files.should include('war/foo-1.0.war') }
  end

  it 'should map EJBs to /ejb directory' do
    define 'foo', :version=>'1.0' do
      package(:ear).add :ejb=>package(:jar)
    end
    inspect_ear { |files| files.should include('ejb/foo-1.0.jar') }
  end

  it 'should not modify original artifact for its components' do
    define 'one', :version => '1.0' do
      write 'src/main/resources/one.txt', '1'
      package(:jar)
    end

    define 'two', :version => '1.0' do
      write 'src/main/resources/two.txt', '2'
      package(:jar)
    end

    define 'foo', :version => '1.0' do
      package(:ear).add project(:one).package(:jar)
      package(:ear).add :ejb => project(:two).package(:jar)
    end

    inspect_ear { |files| files.should include('lib/one-1.0.jar', 'ejb/two-1.0.jar') }

    Buildr::Packaging::Java::Manifest.from_zip(project('one').package(:jar)).main['Class-Path'].should be_nil
    Buildr::Packaging::Java::Manifest.from_zip(project('two').package(:jar)).main['Class-Path'].should be_nil

    inspect_classpath 'ejb/two-1.0.jar' do |classpath|
      classpath.should include('../lib/one-1.0.jar')
    end
  end

  it 'should map JARs to /lib directory' do
    define 'foo', :version=>'1.0' do
      package(:ear) << package(:jar)
    end
    inspect_ear { |files| files.should include('lib/foo-1.0.jar') }
  end

  it 'should accept component type with :type option' do
    define 'foo', :version=>'1.0' do
      package(:ear).add package(:jar), :type=>:ejb
    end
    inspect_ear { |files| files.should include('ejb/foo-1.0.jar') }
  end

  it 'should accept component and its type as type=>artifact' do
    define 'foo', :version=>'1.0' do
      package(:ear).add :ejb=>package(:jar)
    end
    inspect_ear { |files| files.should include('ejb/foo-1.0.jar') }
  end

  it 'should map typed JARs to /jar directory' do
    define 'foo', :version=>'1.0' do
      package(:ear).add :jar=>package(:jar)
    end
    inspect_ear { |files| files.should include('jar/foo-1.0.jar') }
  end

  it 'should add multiple components at a time using the type=>component style' do
    define 'bar', :version => '1.5' do
      package(:war) # must be added as a webapp
      package(:jar) # must be added as a shared lib
      package(:zip) # this one should be excluded
    end
    define 'baz', :version => '1.5' do
      package(:jar, :id => 'one')
      package(:jar, :id => 'two')
    end
    define 'foo', :version => '1.0' do
      package(:ear).add :lib => project('baz'),
                        :war => project('bar').package(:war),
                        :ejb => project('bar').package(:jar)
    end
    inspect_ear do |files|
      files.should include(*%w{ lib/one-1.5.jar lib/two-1.5.jar war/bar-1.5.war ejb/bar-1.5.jar  })
      files.should_not satisfy { files.any? { |f| f =~ /\.zip$/ } }
    end
  end

  it 'should add all EAR supported packages when given a project argument' do
    define 'bar', :version => '1.5' do
      package(:war) # must be added as a webapp
      package(:jar) # must be added as a shared lib
      package(:zip) # this one should be excluded
    end
    define 'baz', :version => '1.5' do
      package(:war)
      package(:jar)
    end
    define 'foo', :version => '1.0' do
      package(:ear).add projects(:bar, :baz)
    end
    inspect_ear do |files|
      files.should include('war/bar-1.5.war', 'lib/bar-1.5.jar', 'lib/baz-1.5.jar', 'war/baz-1.5.war')
      files.should_not satisfy { files.any? { |f| f =~ /\.zip$/ } }
    end
  end

  it 'should complain about unknown component type' do
    define 'foo', :version=>'1.0' do
      lambda { package(:ear).add package(:zip) }.should raise_error(RuntimeError, /ear component/i)
    end
  end

  it 'should allow unknown component types with explicit type' do
    define 'foo', :version=>'1.0' do
      package(:ear).add :lib=>package(:zip)
    end
    inspect_ear { |files| files.should include('lib/foo-1.0.zip') }
  end

  it 'should accept alternative directory name' do
    define 'foo', :version=>'1.0' do
      package(:ear).add package(:jar), :path=>'trash'
    end
    inspect_ear { |files| files.should include('trash/foo-1.0.jar') }
  end

  it 'should accept customization of directory map' do
    define 'foo', :version=>'1.0' do
      package(:ear).dirs[:jar] = 'jarred'
      package(:ear).add :jar=>package(:jar)
    end
    inspect_ear { |files| files.should include('jarred/foo-1.0.jar') }
  end

  it 'should accept customization of directory map with nil paths in application.xml' do
    define 'foo', :version=>'1.0' do
      package(:ear).dirs[:war] = nil
      package(:ear).add :war=>package(:war)
      package(:ear).add package(:jar)
    end
    inspect_ear { |files| files.should include('foo-1.0.war') }
    inspect_application_xml do |xml|
      xml.get_text("/application/module[@id='foo']/web/web-uri").to_s.should eql('foo-1.0.war')
    end
  end

  it 'should accept customization of directory map with nil paths in the classpath' do
    define 'foo', :version=>'1.0' do
      package(:ear).dirs[:lib] = nil
      package(:ear).add :war=>package(:war)
      package(:ear) << package(:jar)
    end
    inspect_classpath 'war/foo-1.0.war' do |classpath|
      classpath.should include('../foo-1.0.jar')
    end
  end

  it 'should list WAR components in application.xml' do
    define 'foo', :version=>'1.0' do
      package(:ear) << package(:war) << package(:war, :id=>'bar')
    end
    inspect_application_xml do |xml|
      xml.get_elements("/application/module[@id='foo'][web]").should_not be_empty
      xml.get_elements("/application/module[@id='bar'][web]").should_not be_empty
    end
  end

  it 'should specify web-uri for WAR components in application.xml' do
    define 'foo', :version=>'1.0' do
      package(:ear) << package(:war)
      package(:ear).add package(:war, :id=>'bar'), :path=>'ws'
    end
    inspect_application_xml do |xml|
      xml.get_text("/application/module[@id='foo']/web/web-uri").to_s.should eql('war/foo-1.0.war')
      xml.get_text("/application/module[@id='bar']/web/web-uri").to_s.should eql('ws/bar-1.0.war')
    end
  end

  it 'should specify context-root for WAR components in application.xml' do
    define 'foo', :version=>'1.0' do
      package(:ear) << package(:war)
      package(:ear).add package(:war, :id=>'bar')
    end
    inspect_application_xml do |xml|
      xml.get_text("/application/module[@id='foo']/web/context-root").to_s.should eql('/foo')
      xml.get_text("/application/module[@id='bar']/web/context-root").to_s.should eql('/bar')
    end
  end

  it 'should accept context-root for WAR components in application.xml' do
    define 'foo', :version=>'1.0' do
      package(:ear).add package(:war), :context_root=>'rooted'
    end
    inspect_application_xml do |xml|
      xml.get_text("/application/module[@id='foo']/web/context-root").to_s.should eql('/rooted')
    end
  end

  it 'should allow disabling the context root' do
    define 'foo', :version=>'1.0' do
      package(:ear).add package(:war), :context_root=>false
    end
    inspect_application_xml do |xml|
      xml.get_elements("/application/module[@id='foo']/web/context-root").should be_empty
    end
  end

  it 'should list EJB components in application.xml' do
    define 'foo', :version=>'1.0' do
      package(:ear).add :ejb=>package(:jar)
      package(:ear).add :ejb=>package(:jar, :id=>'bar')
    end
    inspect_application_xml do |xml|
      xml.get_text("/application/module[@id='foo']/ejb").to_s.should eql('ejb/foo-1.0.jar')
      xml.get_text("/application/module[@id='bar']/ejb").to_s.should eql('ejb/bar-1.0.jar')
    end
  end

  it 'should list JAR components in application.xml' do
    define 'foo', :version=>'1.0' do
      package(:ear) << { :jar=>package(:jar) } << { :jar=>package(:jar, :id=>'bar') }
    end
    inspect_application_xml do |xml|
      jars = xml.get_elements('/application/jar').map(&:texts).map(&:join)
      jars.should include('jar/foo-1.0.jar', 'jar/bar-1.0.jar')
    end
  end

  it 'should update WAR component classpath to include libraries' do
    define 'foo', :version=>'1.0' do
      package(:ear) << package(:jar, :id=>'lib1') << package(:jar, :id=>'lib2')
      package(:ear).add package(:war)
    end
    inspect_classpath 'war/foo-1.0.war' do |classpath|
      classpath.should include('../lib/lib1-1.0.jar', '../lib/lib2-1.0.jar')
    end
  end

  it 'should update WAR component classpath but skip internal libraries' do
    define 'foo', :version=>'1.0' do
      package(:ear) << package(:jar, :id=>'lib1') << package(:jar, :id=>'lib2')
      package(:war).with(:libs=>package(:jar, :id=>'lib1'))
      package(:ear).add package(:war)
    end
    inspect_classpath 'war/foo-1.0.war' do |classpath|
      classpath.should_not include('../lib/lib1-1.0.jar')
      classpath.should include('../lib/lib2-1.0.jar')
    end
  end

  it 'should update EJB component classpath to include libraries' do
    define 'foo', :version=>'1.0' do
      package(:ear) << package(:jar, :id=>'lib1') << package(:jar, :id=>'lib2')
      package(:ear).add :ejb=>package(:jar, :id=>'foo')
    end
    inspect_classpath 'ejb/foo-1.0.jar' do |classpath|
      classpath.should include('../lib/lib1-1.0.jar', '../lib/lib2-1.0.jar')
    end
  end

  it 'should update JAR component classpath to include libraries' do
    define 'foo', :version=>'1.0' do
      package(:ear) << package(:jar, :id=>'lib1') << package(:jar, :id=>'lib2')
      package(:ear).add :jar=>package(:jar, :id=>'foo')
    end
    inspect_classpath 'jar/foo-1.0.jar' do |classpath|
      classpath.should include('../lib/lib1-1.0.jar', '../lib/lib2-1.0.jar')
    end
  end

  it 'should deal with very long classpaths' do
    define 'foo', :version=>'1.0' do
      20.times { |i| package(:ear) << package(:jar, :id=>"lib#{i}") }
      package(:ear).add :jar=>package(:jar, :id=>'foo')
    end
    inspect_classpath 'jar/foo-1.0.jar' do |classpath|
      classpath.should include('../lib/lib1-1.0.jar', '../lib/lib2-1.0.jar')
    end
  end


  it 'should generate relative classpaths for top level EJB' do
    define 'foo', :version => '1.0' do
      package(:ear).add package(:jar, :id => 'one'), :path => '.'
      package(:ear).add package(:jar, :id => 'two'), :path => 'dos'
      package(:ear).add package(:jar, :id => 'three'), :path => 'tres'
      package(:ear).add :ejb => package(:jar, :id => 'ejb1'), :path => '.'
    end
    inspect_classpath 'ejb1-1.0.jar' do |classpath|
      classpath.should include(*%w{ one-1.0.jar dos/two-1.0.jar tres/three-1.0.jar })
    end
  end

  it 'should generate relative classpaths for second level EJB' do
    define 'foo', :version => '1.0' do
      package(:ear).add package(:jar, :id => 'one'), :path => '.'
      package(:ear).add package(:jar, :id => 'two'), :path => 'dos'
      package(:ear).add package(:jar, :id => 'three'), :path => 'tres'
      package(:ear).add :ejb => package(:jar, :id => 'ejb2'), :path => 'dos'
    end
    inspect_classpath 'dos/ejb2-1.0.jar' do |classpath|
      classpath.should include(*%w{ ../one-1.0.jar two-1.0.jar ../tres/three-1.0.jar })
    end
  end

  it 'should generate relative classpaths for nested EJB' do
    define 'foo', :version => '1.0' do
      package(:ear).add package(:jar, :id => 'one'), :path => '.'
      package(:ear).add package(:jar, :id => 'two'), :path => 'dos'
      package(:ear).add package(:jar, :id => 'three'), :path => 'dos/tres'
      package(:ear).add package(:jar, :id => 'four'), :path => 'dos/cuatro'
      package(:ear).add :ejb => package(:jar, :id => 'ejb4'), :path => 'dos/cuatro'
    end
    inspect_classpath 'dos/cuatro/ejb4-1.0.jar' do |classpath|
      classpath.should include(*%w{ ../../one-1.0.jar ../two-1.0.jar ../tres/three-1.0.jar four-1.0.jar })
    end
  end

end


describe Packaging, 'sources' do
  it_should_behave_like 'packaging'
  before { @packaging, @package_type = :sources, :jar }

  it 'should create package of type :jar and classifier \'sources\'' do
    define 'foo', :version=>'1.0' do
      package(:sources).type.should eql(:jar)
      package(:sources).classifier.should eql('sources')
      package(:sources).name.should match(/foo-1.0-sources.jar$/)
    end
  end

  it 'should contain source and resource files' do
    write 'src/main/java/Source.java'
    write 'src/main/resources/foo.properties', 'foo=bar'
    define('foo', :version=>'1.0') { package(:sources) }
    project('foo').task('package').invoke
    project('foo').packages.first.should contain('Source.java')
    project('foo').packages.first.should contain('foo.properties')
  end

  it 'should create sources jar if resources exists (but not sources)' do
    write 'src/main/resources/foo.properties', 'foo=bar'
    define('foo', :version=>'1.0') { package(:sources) }
    project('foo').package(:sources).invoke
    project('foo').packages.first.should contain('foo.properties')
  end

  it 'should be a ZipTask' do
    define 'foo', :version=>'1.0' do
      package(:sources).should be_kind_of(ZipTask)
    end
  end
end

describe Packaging, 'javadoc' do
  it_should_behave_like 'packaging'
  before { @packaging, @package_type = :javadoc, :jar }

  it 'should create package of type :zip and classifier \'javadoc\'' do
    define 'foo', :version=>'1.0' do
      package(:javadoc).type.should eql(:jar)
      package(:javadoc).classifier.should eql('javadoc')
      package(:javadoc).name.pathmap('%f').should eql('foo-1.0-javadoc.jar')
    end
  end

  it 'should contain Javadocs' do
    write 'src/main/java/Source.java', 'public class Source {}'
    define('foo', :version=>'1.0') { package(:javadoc) }
    project('foo').task('package').invoke
    project('foo').packages.first.should contain('Source.html', 'index.html')
  end

  it 'should use project description in window title' do
    write 'src/main/java/Source.java', 'public class Source {}'
    desc 'My Project'
    define('foo', :version=>'1.0') { package(:javadoc) }
    project('foo').task('package').invoke
    project('foo').packages.first.entry('index.html').should contain('My Project')
  end

  it 'should be a ZipTask' do
    define 'foo', :version=>'1.0' do
      package(:javadoc).should be_kind_of(ZipTask)
    end
  end
end

describe Packaging, 'test_jar' do
  it_should_behave_like 'packaging'
  before { @packaging, @package_type = :test_jar, :jar }

  it 'should create package of type :jar and classifier \'tests\'' do
    define 'foo', :version=>'1.0' do
      package(:test_jar).type.should eql(:jar)
      package(:test_jar).classifier.should eql('tests')
      package(:test_jar).name.should match(/foo-1.0-tests.jar$/)
    end
  end

  it 'should contain test source and resource files' do
    write 'src/test/java/Test.java', 'public class Test {}'
    write 'src/test/resources/test.properties', 'foo=bar'
    define('foo', :version=>'1.0') { package(:test_jar) }
    project('foo').task('package').invoke
    project('foo').packages.first.should contain('Test.class')
    project('foo').packages.first.should contain('test.properties')
  end

  it 'should create test jar if resources exists (but not sources)' do
    write 'src/test/resources/test.properties', 'foo=bar'
    define('foo', :version=>'1.0') { package(:test_jar) }
    project('foo').package(:test_jar).invoke
    project('foo').packages.first.should contain('test.properties')
  end

  it 'should be a ZipTask' do
    define 'foo', :version=>'1.0' do
      package(:test_jar).should be_kind_of(ZipTask)
    end
  end
end

shared_examples_for 'package_with_' do

  def prepare(options = {})
    packager = "package_with_#{@packaging}"
    write 'src/main/java/Source.java'
    write 'baz/src/main/java/Source.java'
    define 'foo', :version=>'1.0' do
      send packager, options
      define 'bar' ; define 'baz'
    end
  end

  def applied_to
    projects.select { |project| project.packages.first }.map(&:name)
  end

  it 'should create package of the right packaging with classifier' do
    prepare
    project('foo').packages.first.to_s.should =~ /foo-1.0-#{@packaging}.#{@ext}/
  end

  it 'should create package for projects that have source files' do
    prepare
    applied_to.should include('foo', 'foo:baz')
  end

  it 'should not create package for projects that have no source files' do
    prepare
    applied_to.should_not include('foo:bar')
  end

  it 'should limit to projects specified by :only' do
    prepare :only=>'baz'
    applied_to.should eql(['foo:baz'])
  end

  it 'should limit to projects specified by :only array' do
    prepare :only=>['baz']
    applied_to.should eql(['foo:baz'])
  end

  it 'should ignore project specified by :except' do
    prepare :except=>'baz'
    applied_to.should eql(['foo'])
  end

  it 'should ignore projects specified by :except array' do
    prepare :except=>['baz']
    applied_to.should eql(['foo'])
  end
end

describe 'package_with_sources' do
  it_should_behave_like 'package_with_'
  before { @packaging, @ext = :sources, 'jar' }
end

describe 'package_with_javadoc' do
  it_should_behave_like 'package_with_'
  before { @packaging, @ext = :javadoc, 'jar' }
end
