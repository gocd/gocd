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


describe 'ArchiveTask', :shared=>true do
  before do
    @dir = File.expand_path('test')
    @files = %w{Test1.txt Text2.html}.map { |file| File.expand_path(file, @dir) }.
      each { |file| write file, content_for(file) }
  end

  # Not too smart, we just create some content based on file name to make sure you read what you write.
  def content_for(file)
    "Content for #{File.basename(file)}"
  end

  # Create an archive not using the archive task, this way we do have a file in existence, but we don't
  # have an already invoked task.  Yield an archive task to the block which can use it to include files,
  # set options, etc.
  def create_without_task
    archive(@archive + '.tmp').tap do |task|
      yield task if block_given?
      task.invoke
      mv task.name, @archive
    end
  end

  def create_for_merge
    zip(@archive + '.src').include(@files).tap do |task|
      yield task
    end
  end

  it 'should point to archive file' do
    archive(@archive).name.should eql(@archive)
  end

  it 'should create file' do
    lambda { archive(@archive).invoke }.should change { File.exist?(@archive) }.to(true)
  end

  it 'should create empty archive if no files included' do
    archive(@archive).invoke
    inspect_archive { |archive| archive.should be_empty }
  end

  it 'should create empty archive if called #clean method' do
    archive(@archive).include(@files).clean.invoke
    inspect_archive { |archive| archive.should be_empty }
  end

  it 'should archive all included files' do
    archive(@archive).include(@files).invoke
    inspect_archive { |archive| @files.each { |f| archive[File.basename(f)].should eql(content_for(f)) } }
    inspect_archive.size.should eql(@files.size)
  end

  it 'should archive file tasks' do
    tasks = @files.map { |fn| file(fn) }
    archive(@archive).include(tasks).invoke
    inspect_archive { |archive| @files.each { |f| archive[File.basename(f)].should eql(content_for(f)) } }
    inspect_archive.size.should eql(@files.size)
  end

  it 'should invoke and archive file tasks' do
    file = file('included') { write 'included' }
    lambda { archive(@archive).include(file).invoke }.should change { File.exist?(file.to_s) }.to(true)
    inspect_archive.keys.should include('included')
  end

  it 'should include entry for directory' do
    archive(@archive).include(@dir).invoke
    inspect_archive { |archive| @files.each { |f| archive['test/' + File.basename(f)].should eql(content_for(f)) } }
  end

  it 'should not archive any excluded files' do
    archive(@archive).include(@files).exclude(@files.last).invoke
    inspect_archive do |archive|
      archive.keys.should include(File.basename(@files.first))
      archive.keys.should_not include(File.basename(@files.last))
    end
  end

  it 'should not archive any excluded files in included directories' do
    archive(@archive).include(@dir).exclude(@files.last).invoke
    inspect_archive do |archive|
      archive.keys.should include('test/' + File.basename(@files.first))
      archive.keys.should_not include('test/' + File.basename(@files.last))
    end
  end

  it 'should not archive any excluded files when using :from/:as' do
    archive(@archive).include(:from=>@dir).exclude(@files.last).invoke
    inspect_archive do |archive|
      archive.keys.should include(File.basename(@files.first))
      archive.keys.should_not include(File.basename(@files.last))
    end
  end

  it 'should exclude entire directory and all its children' do
    mkpath "#{@dir}/sub"
    write "#{@dir}/sub/test"
    archive(@archive).include(@dir).exclude("#{@dir}/sub").invoke
    inspect_archive do |archive|
      archive.keys.select { |file| file =~ /sub/ }.should be_empty 
    end
  end

  it 'should not archive any excluded files when pattern is *.ext' do
    write "test/file.txt"
    write "test/file.swf"
    archive(@archive).include(@dir).exclude('**/*.swf').invoke
    inspect_archive do |archive|
      archive.keys.should include('test/file.txt')
      archive.keys.should_not include('test/file.swf')
    end
  end

  it 'should archive files into specified path' do
    archive(@archive).include(@files, :path=>'code').invoke
    inspect_archive { |archive| @files.each { |f| archive['code/' + File.basename(f)].should eql(content_for(f)) } }
  end

  it 'should include entry for directory' do
    archive(@archive).include(@dir).invoke
    inspect_archive { |archive| @files.each { |f| archive['test/' + File.basename(f)].should eql(content_for(f)) } }
  end

  it 'should archive files into specified path' do
    archive(@archive).include(@files, :path=>'code').invoke
    inspect_archive { |archive| @files.each { |f| archive['code/' + File.basename(f)].should eql(content_for(f)) } }
  end

  it 'should archive directories into specified path' do
    archive(@archive).include(@dir, :path=>'code').invoke
    inspect_archive { |archive| @files.each { |f| archive['code/test/' + File.basename(f)].should eql(content_for(f)) } }
  end

  it 'should understand . in path' do
    archive(@archive).path('.').should == archive(@archive).path('')
    archive(@archive).path('foo').path('.').should == archive(@archive).path('foo')
  end

  it 'should understand .. in path' do
    archive(@archive).path('..').should == archive(@archive).path('')
    archive(@archive).path('foo').path('..').should == archive(@archive).path('')
    archive(@archive).path('foo/bar').path('..').should == archive(@archive).path('foo')
  end

  it 'should understand leading / in path' do
    archive(@archive).path('/').should == archive(@archive).path('')
    archive(@archive).path('foo/bar').path('/').should == archive(@archive).path('')
  end

  it 'should archive file into specified name' do
    archive(@archive).include(@files.first, :as=>'test/sample').invoke
    inspect_archive { |archive| @files.each { |f| archive['test/sample'].should eql(content_for(@files.first)) } }
  end

  it 'should archive file into specified name/path' do
    archive(@archive).include(@files.first, :as=>'test/sample', :path=>'path').invoke
    inspect_archive { |archive| @files.each { |f| archive['path/test/sample'].should eql(content_for(@files.first)) } }
  end

  it 'should archive files starting with dot' do
    write 'test/.config', '# configuration'
    archive(@archive).include('test').invoke
    inspect_archive { |archive| @files.each { |f| archive['test/.config'].should eql('# configuration') } }
  end

  it 'should archive directory into specified name' do
    archive(@archive).include(@dir, :as=>'code').invoke
    inspect_archive { |archive| @files.each { |f| archive['code/' + File.basename(f)].should eql(content_for(f)) } }
  end

  it 'should archive directory into specified name/path' do
    archive(@archive).include(@dir, :as=>'code', :path=>'path').invoke
    inspect_archive { |archive| @files.each { |f| archive['path/code/' + File.basename(f)].should eql(content_for(f)) } }
  end

  it 'should archive directory contents' do
    archive(@archive).include(@dir, :as=>'.').invoke
    inspect_archive { |archive| @files.each { |f| archive[File.basename(f)].should eql(content_for(f)) } }
  end

  it 'should archive directory contents into specified path' do
    archive(@archive).include(@dir, :as=>'.', :path=>'path').invoke
    inspect_archive { |archive| @files.each { |f| archive['path/' + File.basename(f)].should eql(content_for(f)) } }
  end

  it 'should not allow two files with the :as argument' do
    lambda { archive(@archive).include(@files.first, @files.last, :as=>'test/sample') }.should raise_error(RuntimeError, /one file/)
  end

  it 'should expand another archive file' do
    create_for_merge do |src|
      archive(@archive).merge(src)
      archive(@archive).invoke
      inspect_archive { |archive| @files.each { |f| archive[File.basename(f)].should eql(content_for(f)) } }
    end
  end

  it 'should expand another archive file with include pattern' do
    create_for_merge do |src|
      archive(@archive).merge(src).include(File.basename(@files.first))
      archive(@archive).invoke
      inspect_archive do |archive| 
        archive[File.basename(@files.first)].should eql(content_for(@files.first))
        archive[File.basename(@files.last)].should be_nil
      end
    end
  end
  
  it 'should expand another archive file with exclude pattern' do
    create_for_merge do |src|
      archive(@archive).merge(src).exclude(File.basename(@files.first))
      archive(@archive).invoke
      inspect_archive do |archive| 
        @files[1..-1].each { |f| archive[File.basename(f)].should eql(content_for(f)) } 
        archive[File.basename(@files.first)].should be_nil
      end
    end
  end

  it 'should expand another archive file with nested exclude pattern' do
    @files = %w{Test1.txt Text2.html}.map { |file| File.join(@dir, "foo", file) }.
      each { |file| write file, content_for(file) }
    zip(@archive + '.src').include(@dir).tap do |task|
      archive(@archive).merge(task).exclude('test/*')
      archive(@archive).invoke
      inspect_archive.should be_empty
    end
  end

  it 'should expand another archive file into path' do
    create_for_merge do |src|
      archive(@archive).path('test').merge(src)
      archive(@archive).invoke
      inspect_archive { |archive| @files.each { |f| archive['test/' + File.basename(f)].should eql(content_for(f)) } }
    end
  end

  it 'should expand another archive file into path with :path option' do
    create_for_merge do |src|
      archive(@archive).merge(src, :path=>'test')
      archive(@archive).invoke
      inspect_archive { |archive| @files.each { |f| archive['test/' + File.basename(f)].should eql(content_for(f)) } }
    end
  end

  it "should expand another archive file into path with :path=>'/'" do
    create_for_merge do |src|
      archive(@archive).merge(src, :path=>'/')
      archive(@archive).invoke
      inspect_archive { |archive| @files.each { |f| archive[File.basename(f)].should eql(content_for(f)) } }
    end
  end

  it 'should expand another archive file into path with merge option' do
    create_for_merge do |src|
      archive(@archive).include(src, :merge=>true)
      archive(@archive).invoke
      inspect_archive { |archive| @files.each { |f| archive[File.basename(f)].should eql(content_for(f)) } }
    end
  end

  it 'should update if one of the files is recent' do
    create_without_task { |archive| archive.include(@files) }
    # Touch archive file to some point in the past. This effectively makes
    # all included files newer.
    File.utime Time.now - 100, Time.now - 100, @archive
    archive(@archive).include(@files).invoke
    File.stat(@archive).mtime.should be_close(Time.now, 10) 
  end

  it 'should do nothing if all files are uptodate' do
    create_without_task { |archive| archive.include(@files) }
    # By touching all files in the past, there's nothing new to update.
    (@files + [@archive]).each { |f| File.utime Time.now - 100, Time.now - 100, f }
    archive(@archive).include(@files).invoke
    File.stat(@archive).mtime.should be_close(Time.now - 100, 10) 
  end

  it 'should update if one of the files is recent' do
    create_without_task { |archive| archive.include(@files) }
    # Change files, we expect to see new content.
    write @files.first, '/* Refreshed */'
    File.utime(Time.now - 100, Time.now - 100, @archive) # Touch archive file to some point in the past.
    archive(@archive).include(@files).invoke
    inspect_archive { |archive| archive[File.basename(@files.first)].should eql('/* Refreshed */') }
  end

  it 'should create new archive when updating' do
    create_without_task { |archive| archive.include(@files) }
    File.utime(Time.now - 100, Time.now - 100, @archive) # Touch archive file to some point in the past.
    archive(@archive).include(@files[1..-1]).invoke
    inspect_archive.size.should be(@files.size - 1)
  end

  it 'should not accept invalid options' do
    archive(@archive).include(@files)
    lambda { archive(@archive).with :option=>true }.should raise_error
  end
end


describe TarTask do
  it_should_behave_like 'ArchiveTask'
  before { @archive = File.expand_path('test.tar') }
  define_method(:archive) { |file| tar(file) }

  def inspect_archive
    entries = {}
    Archive::Tar::Minitar.open @archive, 'r' do |reader|
      reader.each { |entry| entries[entry.directory ? "#{entry.name}/" : entry.name] = entry.read }
    end
    yield entries if block_given?
    entries
  end
end


describe TarTask, ' gzipped' do
  it_should_behave_like 'ArchiveTask'
  before { @archive = File.expand_path('test.tgz') }
  define_method(:archive) { |file| tar(file) }

  def inspect_archive
    entries = {}
    Zlib::GzipReader.open @archive do |gzip| 
      Archive::Tar::Minitar.open gzip, 'r' do |reader|
        reader.each { |entry| entries[entry.directory ? "#{entry.name}/" : entry.name] = entry.read }
      end
    end
    yield entries if block_given?
    entries
  end
end


describe ZipTask do
  it_should_behave_like 'ArchiveTask'
  before { @archive = File.expand_path('test.zip') }
  define_method(:archive) { |file| zip(file) }

  def inspect_archive
    entries = {}
    Zip::ZipFile.open @archive do |zip|
      zip.entries.each do |entry|
        # Ignore the / directory created for empty ZIPs when using java.util.zip.
        entries[entry.to_s] = zip.read(entry) unless entry.to_s == '/'
      end
    end
    yield entries if block_given?
    entries
  end

  it 'should work with path object' do
    archive(@archive).path('code').include(@files)
    archive(@archive).invoke
    inspect_archive { |archive| archive.keys.should include('code/') }
  end
end


describe Unzip do
  before do
    @zip = File.expand_path('test.zip')
    @dir = File.expand_path('test')
    @files = %w{Test1.txt Text2.html}.map { |file| File.join(@dir, file) }.
      each { |file| write file, content_for(file) }
    @target = File.expand_path('target')
  end

  # Not too smart, we just create some content based on file name to
  # make sure you read what you write.
  def content_for(file)
    "Content for #{File.basename(file)}"
  end

  def with_zip(*args)
    zip(@zip).include(*args.empty? ? @files : args).invoke
    yield
  end
    
  it 'should touch target directory' do
    with_zip do
      mkdir @target
      File.utime(Time.now - 10, Time.now - 10, @target)
      unzip(@target=>@zip).target.invoke
    end
    File.stat(@target).mtime.should be_close(Time.now, 2)
  end

  it 'should expand files' do
    with_zip do
      unzip(@target=>@zip).target.invoke
      @files.each { |f| File.read(File.join(@target, File.basename(f))).should eql(content_for(f)) }
    end
  end

  it 'should expand all files' do
    with_zip do
      unzip(@target=>@zip).target.invoke
      FileList[File.join(@target, '*')].size.should be(@files.size)
    end
  end

  it 'should expand only included files' do
    with_zip do
      only = File.basename(@files.first)
      unzip(@target=>@zip).include(only).target.invoke
      FileList[File.join(@target, '*')].should include(File.expand_path(only, @target))
      FileList[File.join(@target, '*')].size.should be(1)
    end
  end

  it 'should expand all but excluded files' do
    with_zip do
      except = File.basename(@files.first)
      unzip(@target=>@zip).exclude(except).target.invoke
      FileList[File.join(@target, '*')].should_not include(File.expand_path(except, @target))
      FileList[File.join(@target, '*')].size.should be(@files.size - 1)
    end
  end

  it 'should include with nested path patterns' do
    with_zip @files, :path=>'test/path' do
      only = File.basename(@files.first)
      unzip(@target=>@zip).include(only).target.invoke
      FileList[File.join(@target, '*')].should be_empty

      Rake::Task.clear ; rm_rf @target
      unzip(@target=>@zip).include('test/path/' + only).target.invoke
      FileList[File.join(@target, 'test/path/*')].size.should be(1)

      Rake::Task.clear ; rm_rf @target
      unzip(@target=>@zip).include('test/**/*').target.invoke
      FileList[File.join(@target, 'test/path/*')].size.should be(2)
    end
  end

  it 'should include with relative path' do
    with_zip @files, :path=>'test/path' do
      only = File.basename(@files.first)
      unzip(@target=>@zip).tap { |unzip| unzip.from_path('test').include(only) }.target.invoke
      FileList[File.join(@target, '*')].should be_empty

      Rake::Task.clear ; rm_rf @target
      unzip(@target=>@zip).tap { |unzip| unzip.from_path('test').include('test/*') }.target.invoke
      FileList[File.join(@target, 'path/*')].should be_empty

      Rake::Task.clear ; rm_rf @target
      unzip(@target=>@zip).tap { |unzip| unzip.from_path('test').include('path/*' + only) }.target.invoke
      FileList[File.join(@target, 'path/*')].size.should be(1)

      Rake::Task.clear ; rm_rf @target
      unzip(@target=>@zip).tap { |unzip| unzip.from_path('test').include('path/*') }.target.invoke
      FileList[File.join(@target, 'path/*')].size.should be(2)
    end
  end
  
  it 'should exclude with relative path' do
    with_zip @files, :path=>'test' do
      except = File.basename(@files.first)
      unzip(@target=>@zip).tap { |unzip| unzip.from_path('test').exclude(except) }.target.invoke
      FileList[File.join(@target, '*')].should include(File.join(@target, File.basename(@files[1])))
      FileList[File.join(@target, '*')].size.should be(@files.size - 1)
    end
  end

  it "should handle relative paths without any includes or excludes" do
    lib_files = %w{Test3.so Test4.rb}.
      map { |file| File.join(@dir, file) }.
      each { |file| write file, content_for(file) }
    zip(@zip).include(@files, :path => 'src').include(lib_files, :path => 'lib').invoke

    unzip(@target=>@zip).tap { |unzip| unzip.from_path('lib') }.target.invoke
    FileList[File.join(@target, '**/*')].should have(2).files
  end

  it 'should return itself from root method' do
    task = unzip(@target=>@zip)
    task.root.should be(task)
    task.from_path('foo').root.should be(task)
  end

  it 'should return target task from target method' do
    task = unzip(@target=>@zip)
    task.target.should be(file(@target))
    task.from_path('foo').target.should be(file(@target))
  end

  it 'should alias from_path as path' do
    task = unzip(@target=>@zip)
    task.from_path('foo').should be(task.path('foo'))
  end
end
