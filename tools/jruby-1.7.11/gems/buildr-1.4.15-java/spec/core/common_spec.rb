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

describe Buildr.method(:struct) do
  before do
    @hash = { :foo=>'foo:jar', :bar=>'bar:jar' }
    @struct = struct(@hash)
  end

  it 'should be object with key-value pairs' do
    @struct.foo.should eql('foo:jar')
    @struct.bar.should eql('bar:jar')
  end

  it 'should fail when requesting non-existent key' do
    lambda { @struct.foobar }.should raise_error(NoMethodError)
  end

  it 'should return members when requested' do
    @struct.members.map(&:to_s).sort.should eql(@hash.keys.map(&:to_s).sort)
  end

  it 'should return valued when requested' do
    @struct.values.sort.should eql(@hash.values.sort)
  end
end


describe Buildr.method(:write) do
  it 'should create path' do
    write 'foo/test'
    File.directory?('foo').should be_true
    File.exist?('foo/test').should be_true
  end

  it 'should write content to file' do
    write 'test', 'content'
    File.read('test').should eql('content')
  end

  it 'should retrieve content from block, if block given' do
    write('test') { 'block' }
    File.read('test').should eql('block')
  end

  it 'should write empty file if no content provided' do
    write 'test'
    File.read('test').should eql('')
  end

  it 'should return content as a string' do
    write('test', 'content').should eql('content')
  end

  it 'should return empty string if no content provided' do
    write('test').should eql('')
  end
end


describe Buildr.method(:read) do
  before do
    write @file = 'test', @content = 'content'
  end

  it 'should return contents of named file' do
    read(@file).should eql(@content)
  end

  it 'should yield to block if block given' do
    read @file do |content|
      content.should eql(@content)
    end
  end

  it 'should return block response if block given' do
    read(@file) { 5 }.should be(5)
  end
end


describe Buildr.method(:download) do
  before do
    @content = 'we has download!'
    @http = double('http')
    @http.stub(:request).and_return(Net::HTTPNotModified.new(nil, nil, nil))
  end

  def tasks()
    [ download('http://localhost/download'), download('downloaded'=>'http://localhost/download') ]
  end

  it 'should be a file task' do
    tasks.each { |task| task.should be_kind_of(Rake::FileTask) }
  end

  it 'should accept a String and download from that URL' do
    define 'foo' do
      download('http://localhost/download').tap do |task|
        task.source.should_receive(:read).and_yield [@content]
        task.invoke
        task.should contain(@content)
      end
    end
  end

  it 'should accept a URI and download from that URL' do
    define 'foo' do
      download(URI.parse('http://localhost/download')).tap do |task|
        task.source.should_receive(:read).and_yield [@content]
        task.invoke
        task.should contain(@content)
      end
    end
  end

  it 'should accept a path and String and download from that URL' do
    define 'foo' do
      download('downloaded'=>'http://localhost/download').tap do |task|
        task.source.should_receive(:read).and_yield [@content]
        task.invoke
        task.should contain(@content)
      end
    end
  end

  it 'should accept an artifact and String and download from that URL' do
    define 'foo' do
      artifact('com.example:library:jar:2.0').tap do |artifact|
        download(artifact=>'http://localhost/download').source.should_receive(:read).and_yield [@content]
        artifact.invoke
        artifact.should contain(@content)
      end
    end
  end

  it 'should accept a path and URI and download from that URL' do
    define 'foo' do
      download('downloaded'=>URI.parse('http://localhost/download')).tap do |task|
        task.source.should_receive(:read).and_yield [@content]
        task.invoke
        task.should contain(@content)
      end
    end
  end

  it 'should create path for download' do
    define 'foo' do
      download('path/downloaded'=>URI.parse('http://localhost/download')).tap do |task|
        task.source.should_receive(:read).and_yield [@content]
        task.invoke
        task.should contain(@content)
      end
    end
  end

  it 'should fail if resource not found' do
    tasks.each do |task|
      task.source.should_receive(:read).and_raise URI::NotFoundError
      lambda { task.invoke }.should raise_error(URI::NotFoundError)
    end
    tasks.last.should_not exist
  end

  it 'should fail on any other error' do
    tasks.each do |task|
      task.source.should_receive(:read).and_raise RuntimeError
      lambda { task.invoke }.should raise_error(RuntimeError)
    end
    tasks.last.should_not exist
  end

  it 'should execute only if file does not already exist' do
    define 'foo' do
      download('downloaded'=>'http://localhost/download').tap do |task|
        task.source.should_not_receive(:read)
        write task.to_s, 'not really'
        task.invoke
      end
    end
  end

  it 'should execute without a proxy if none specified' do
    Net::HTTP.should_receive(:new).with('localhost', 80).twice.and_return(@http)
    tasks.each(&:invoke)
  end

  it 'should pass Buildr proxy options' do
    Buildr.options.proxy.http = 'http://proxy:8080'
    Net::HTTP.should_receive(:new).with('localhost', 80, 'proxy', 8080, nil, nil).twice.and_return(@http)
    tasks.each(&:invoke)
  end

  it 'should set HTTP proxy from HTTP_PROXY environment variable' do
    ENV['HTTP_PROXY'] = 'http://proxy:8080'
    Net::HTTP.should_receive(:new).with('localhost', 80, 'proxy', 8080, nil, nil).twice.and_return(@http)
    tasks.each(&:invoke)
  end
end


describe Buildr.method(:filter) do
  def source
    File.expand_path('src')
  end

  it 'should return a Filter for the source' do
    filter(source).should be_kind_of(Filter)
  end

  it 'should use the source directory' do
    filter(source).sources.should include(file(source))
  end

  it 'should use the source directories' do
    dirs = ['first', 'second']
    filter('first', 'second').sources.should include(*dirs.map { |dir| file(File.expand_path(dir)) })
  end

  it 'should accept a file task' do
    task = file(source)
    filter(task).sources.each { |source| source.should be(task) }
  end
end


describe Buildr::Filter do
  before do
    @filter = Filter.new
    1.upto(4) do |i|
      write "src/file#{i}", "file#{i} raw"
    end
    @early = Time.now - 1000
  end

  it 'should respond to :from and return self' do
    @filter.from('src').should be(@filter)
  end

  it 'should respond to :from and add source directory' do
    lambda { @filter.from('src') }.should change { @filter.sources }
  end

  it 'should respond to :from and add source directories' do
    dirs = ['first', 'second']
    @filter.from(*dirs)
    @filter.sources.should include(*dirs.map { |dir| file(File.expand_path(dir)) })
  end

  it 'should return source directories as file task' do
    @filter.from('src').sources.each { |source| source.should be_kind_of(Rake::FileTask) }
  end

  it 'should return source directories as expanded path' do
    @filter.from('src').sources.each { |source| source.to_s.should eql(File.expand_path('src')) }
  end

  it 'should respond to :into and return self' do
    @filter.into('target').should be(@filter)
  end

  it 'should respond to :into and set target directory' do
    lambda { @filter.into('src') }.should change { @filter.target }
    @filter.into('target').target.should be(file(File.expand_path('target')))
  end

  it 'should return target directory as file task' do
    @filter.into('target').target.should be_kind_of(Rake::FileTask)
  end

  it 'should return target directory as expanded path' do
    @filter.into('target').target.to_s.should eql(File.expand_path('target'))
  end

  it 'should respond to :using and return self' do
    @filter.using().should be(@filter)
  end

  it 'should respond to :using and set mapping from the argument' do
    mapping = { 'foo'=>'bar' }
    lambda { @filter.using mapping }.should change { @filter.mapping }.to(mapping)
  end

  it 'should respond to :using and set mapping from the block' do
    @filter.using { 5 }.mapping.call.should be(5)
  end

  it 'should respond to :include and return self' do
    @filter.include('file').should be(@filter)
  end

  it 'should respond to :include and use these inclusion patterns' do
    @filter.from('src').into('target').include('file2', 'file3').run
    Dir['target/*'].sort.should eql(['target/file2', 'target/file3'])
  end

  it 'should respond to :include with regular expressions and use these inclusion patterns' do
    @filter.from('src').into('target').include(/file[2|3]/).run
    Dir['target/*'].sort.should eql(['target/file2', 'target/file3'])
  end

  it 'should respond to :include with a Proc and use these inclusion patterns' do
    @filter.from('src').into('target').include(lambda {|file| file[-1, 1].to_i%2 == 0}).run
    Dir['target/*'].sort.should eql(['target/file2', 'target/file4'])
  end

  it 'should respond to :include with a FileTask and use these inclusion patterns' do
    @filter.from('src').into('target').include(file('target/file2'), file('target/file4')).run
    Dir['target/*'].sort.should eql(['target/file2', 'target/file4'])
  end

  it 'should respond to :exclude and return self' do
    @filter.exclude('file').should be(@filter)
  end

  it 'should respond to :exclude and use these exclusion patterns' do
    @filter.from('src').into('target').exclude('file2', 'file3').run
    Dir['target/*'].sort.should eql(['target/file1', 'target/file4'])
  end

  it 'should respond to :exclude with regular expressions and use these exclusion patterns' do
    @filter.from('src').into('target').exclude(/file[2|3]/).run
    Dir['target/*'].sort.should eql(['target/file1', 'target/file4'])
  end

  it 'should respond to :exclude with a Proc and use these exclusion patterns' do
    @filter.from('src').into('target').exclude(lambda {|file| file[-1, 1].to_i%2 == 0}).run
    Dir['target/*'].sort.should eql(['target/file1', 'target/file3'])
  end

  it 'should respond to :exclude with a FileTask and use these exclusion patterns' do
    @filter.from('src').into('target').exclude(file('target/file1'), file('target/file3')).run
    Dir['target/*'].sort.should eql(['target/file2', 'target/file4'])
  end

  it 'should respond to :exclude with a FileTask, use these exclusion patterns and depend on those tasks' do
    file1 = false
    file2 = false
    @filter.from('src').into('target').exclude(file('target/file1').enhance { file1 = true }, file('target/file3').enhance {file2 = true }).run
    Dir['target/*'].sort.should eql(['target/file2', 'target/file4'])
    @filter.target.invoke
    file1.should be_true
    file2.should be_true
  end

  it 'should copy files over' do
    @filter.from('src').into('target').run
    Dir['target/*'].sort.each do |file|
      read(file).should eql("#{File.basename(file)} raw")
    end
  end

  it 'should copy dot files over' do
    write 'src/.config', '# configuration'
    @filter.from('src').into('target').run
    read('target/.config').should eql('# configuration')
  end

  it 'should copy empty directories as well' do
    mkpath 'src/empty'
    @filter.from('src').into('target').run
    File.directory?('target/empty').should be_true
  end

  it 'should copy files from multiple source directories' do
    4.upto(6) { |i| write "src2/file#{i}", "file#{i} raw" }
    @filter.from('src', 'src2').into('target').run
    Dir['target/*'].each do |file|
      read(file).should eql("#{File.basename(file)} raw")
    end
    Dir['target/*'].should include(*(1..6).map { |i| "target/file#{i}" })
  end

  it 'should copy files recursively' do
    mkpath 'src/path1' ; write 'src/path1/left'
    mkpath 'src/path2' ; write 'src/path2/right'
    @filter.from('src').into('target').run
    Dir['target/**/*'].should include(*(1..4).map { |i| "target/file#{i}" })
    Dir['target/**/*'].should include('target/path1/left', 'target/path2/right')
  end

  it 'should apply hash mapping using Maven style' do
    1.upto(4) { |i| write "src/file#{i}", "file#{i} with ${key1} and ${key2}" }
    @filter.from('src').into('target').using('key1'=>'value1', 'key2'=>'value2').run
    Dir['target/*'].each do |file|
      read(file).should eql("#{File.basename(file)} with value1 and value2")
    end
  end

  it 'should apply hash mapping using Ant style' do
    1.upto(4) { |i| write "src/file#{i}", "file#{i} with @key1@ and @key2@" }
    @filter.from('src').into('target').using(:ant, 'key1'=>'value1', 'key2'=>'value2').run
    Dir['target/*'].each do |file|
      read(file).should eql("#{File.basename(file)} with value1 and value2")
    end
  end

  it 'should apply hash mapping using Ruby style' do
    1.upto(4) { |i| write "src/file#{i}", "file#{i} with \#{key1} and \#{key2}" }
    @filter.from('src').into('target').using(:ruby, 'key1'=>'value1', 'key2'=>'value2').run
    Dir['target/*'].each do |file|
      read(file).should eql("#{File.basename(file)} with value1 and value2")
    end
  end

  it 'should use erb when given a binding' do
    1.upto(4) { |i| write "src/file#{i}", "file#{i} with <%= key1 %> and <%= key2 * 2 %>" }
    key1 = 'value1'
    key2 = 12
    @filter.from('src').into('target').using(binding).run
    Dir['target/*'].each do |file|
      read(file).should eql("#{File.basename(file)} with value1 and 24")
    end
  end

  it 'should apply hash mapping using erb' do
    1.upto(4) { |i| write "src/file#{i}", "file#{i} with <%= key1 %> and <%= key2 * 2 %>" }
    @filter.from('src').into('target').using(:erb, 'key1'=>'value1', 'key2'=> 12).run
    Dir['target/*'].each do |file|
      read(file).should eql("#{File.basename(file)} with value1 and 24")
    end
  end

  it 'should use an object binding when using erb' do
    1.upto(4) { |i| write "src/file#{i}", "file#{i} with <%= key1 %> and <%= key2 * 2 %>" }
    obj = Struct.new(:key1, :key2).new('value1', 12)
    @filter.from('src').into('target').using(:erb, obj).run
    Dir['target/*'].each do |file|
      read(file).should eql("#{File.basename(file)} with value1 and 24")
    end
  end

  it 'should use a given block context when using erb' do
    1.upto(4) { |i| write "src/file#{i}", "file#{i} with <%= key1 %> and <%= key2 * 2 %>" }
    key1 = 'value1'
    key2 = 12
    @filter.from('src').into('target').using(:erb){}.run
    Dir['target/*'].each do |file|
      read(file).should eql("#{File.basename(file)} with value1 and 24")
    end
  end

  it 'should using Maven mapper by default' do
    @filter.using('key1'=>'value1', 'key2'=>'value2').mapper.should eql(:maven)
  end

  it 'should apply hash mapping with boolean values' do
    write "src/file", "${key1} and ${key2}"
    @filter.from('src').into('target').using(:key1=>true, :key2=>false).run
    read("target/file").should eql("true and false")
  end

  it 'should apply hash mapping using regular expression' do
    1.upto(4) { |i| write "src/file#{i}", "file#{i} with #key1# and #key2#" }
    @filter.from('src').into('target').using(/#(.*?)#/, 'key1'=>'value1', 'key2'=>'value2').run
    Dir['target/*'].each do |file|
      read(file).should eql("#{File.basename(file)} with value1 and value2")
    end
  end

  it 'should apply proc mapping' do
    @filter.from('src').into('target').using { |file, content| 'proc mapped' }.run
    Dir['target/*'].each do |file|
      read(file).should eql('proc mapped')
    end
  end

  it 'should apply proc mapping with relative file name' do
    @filter.from('src').into('target').using { |file, content| file.should =~ /^file\d$/ }.run
  end

  it 'should apply proc mapping with file content' do
    @filter.from('src').into('target').using { |file, content| content.should =~ /^file\d raw/ }.run
  end

  it 'should make target directory' do
    lambda { @filter.from('src').into('target').run }.should change { File.exist?('target') }.to(true)
  end

  it 'should touch target directory' do
    mkpath 'target' ; File.utime @early, @early, 'target'
    @filter.from('src').into('target').run
    File.stat('target').mtime.should be_within(10).of(Time.now)
  end

  it 'should not touch target directory unless running' do
    mkpath 'target' ; File.utime @early, @early, 'target'
    @filter.from('src').into('target').exclude('*').run
    File.mtime('target').should be_within(10).of(@early)
  end

  it 'should run only on new files' do
    # Make source files older so they're not copied twice.
    Dir['src/**/*'].each { |file| File.utime(@early, @early, file) }
    @filter.from('src').into('target').run
    @filter.from('src').into('target').using { |file, content| file.should eql('file2') }.run
  end

  it 'should return true when run copies any files' do
    @filter.from('src').into('target').run.should be(true)
  end

  it 'should return false when run does not copy any files' do
    # Make source files older so they're not copied twice.
    Dir['src/**/*'].each { |file| File.utime(@early, @early, file) }
    @filter.from('src').into('target').run
    @filter.from('src').into('target').run.should be(false)
  end

  it 'should fail if source directory doesn\'t exist' do
    lambda { Filter.new.from('srced').into('target').run }.should raise_error(RuntimeError, /doesn't exist/)
  end

  it 'should fail is target directory not set' do
    lambda { Filter.new.from('src').run }.should raise_error(RuntimeError, /No target directory/)
  end

  it 'should copy read-only files as writeable' do
    Dir['src/*'].each { |file| File.chmod(0444, file) }
    @filter.from('src').into('target').run
    Dir['target/*'].sort.each do |file|
      File.readable?(file).should be_true
      File.writable?(file).should be_true
      (File.stat(file).mode & 0o200).should == 0o200
    end
  end

  it 'should preserve mode bits except readable' do
    mode = 0o600
    Dir['src/*'].each { |file| File.chmod(mode, file) }
    @filter.from('src').into('target').run
    Dir['target/*'].sort.each do |file|
      (File.stat(file).mode & mode).should == mode
    end
  end
end

describe Filter::Mapper do

  module MooMapper
    def moo_config(*args, &block)
      raise ArgumentError, "Expected moo block" unless block_given?
      { :moos => args, :callback => block }
    end

    def moo_transform(content, path = nil)
      content.gsub(/moo+/i) do |str|
        moos = yield :moos # same than config[:moos]
        moo = moos[str.size - 3] || str
        config[:callback].call(moo)
      end
    end
  end

  it 'should allow plugable mapping types' do
    mapper = Filter::Mapper.new.extend(MooMapper)
    mapper.using(:moo, 'ooone', 'twoo') do |str|
      i = nil; str.capitalize.gsub(/\w/) { |s| s.send( (i = !i) ? 'upcase' : 'downcase' ) }
    end
    mapper.transform('Moo cow, mooo cows singing mooooo').should == 'OoOnE cow, TwOo cows singing MoOoOo'
  end

end

describe Buildr.method(:options) do
  it 'should return an Options object' do
    options.should be_kind_of(Options)
  end

  it 'should return an Options object each time' do
    options.should be(options)
  end

  it 'should return the same Options object when called on Object, Buildr or Project' do
    options.should be(Buildr.options)
    define('foo') { options.should be(Buildr.options) }
  end
end

describe Buildr::Options, 'proxy.exclude' do
  before do
    options.proxy.http = 'http://myproxy:8080'
    options.proxy.exclude.clear
    @domain = 'domain'
    @host = "host.#{@domain}"
    @uri = URI("http://#{@host}")
    @no_proxy_args = [@host, 80]
    @proxy_args = @no_proxy_args + ['myproxy', 8080, nil, nil]
    @http = double('http')
    @http.stub(:request).and_return(Net::HTTPNotModified.new(nil, nil, nil))
  end

  it 'should be an array' do
    options.proxy.exclude.should be_empty
    options.proxy.exclude = @domain
    options.proxy.exclude.should include(@domain)
  end

  it 'should support adding to array' do
    options.proxy.exclude << @domain
    options.proxy.exclude.should include(@domain)
  end

  it 'should support resetting array' do
    options.proxy.exclude = @domain
    options.proxy.exclude = nil
    options.proxy.exclude.should be_empty
  end

  it 'should use proxy when not excluded' do
    Net::HTTP.should_receive(:new).with(*@proxy_args).and_return(@http)
    @uri.read :proxy=>options.proxy
  end

  it 'should use proxy unless excluded' do
    options.proxy.exclude = "not.#{@domain}"
    Net::HTTP.should_receive(:new).with(*@proxy_args).and_return(@http)
    @uri.read :proxy=>options.proxy
  end

  it 'should not use proxy if excluded' do
    options.proxy.exclude = @host
    Net::HTTP.should_receive(:new).with(*@no_proxy_args).and_return(@http)
    @uri.read :proxy=>options.proxy
  end

  it 'should support multiple host names' do
    options.proxy.exclude = ['optimus', 'prime']
    Net::HTTP.should_receive(:new).with('optimus', 80).and_return(@http)
    URI('http://optimus').read :proxy=>options.proxy
    Net::HTTP.should_receive(:new).with('prime', 80).and_return(@http)
    URI('http://prime').read :proxy=>options.proxy
    Net::HTTP.should_receive(:new).with('bumblebee', *@proxy_args[1..-1]).and_return(@http)
    URI('http://bumblebee').read :proxy=>options.proxy
  end

  it 'should support glob pattern on host name' do
    options.proxy.exclude = "*.#{@domain}"
    Net::HTTP.should_receive(:new).with(*@no_proxy_args).and_return(@http)
    @uri.read :proxy=>options.proxy
  end
end


describe Hash, '::from_java_properties' do
  it 'should return hash' do
    hash = Hash.from_java_properties(<<-PROPS)
name1=value1
name2=value2
    PROPS
    hash.should == {'name1'=>'value1', 'name2'=>'value2'}
  end

  it 'should ignore comments and empty lines' do
    hash = Hash.from_java_properties(<<-PROPS)

name1=value1

name2=value2

PROPS
    hash.should == {'name1'=>'value1', 'name2'=>'value2'}
  end

  it 'should allow multiple lines' do
    hash = Hash.from_java_properties(<<-PROPS)
name1=start\
 end

name2=first\
 second\
 third

PROPS
    hash.should == {'name1'=>'start end', 'name2'=>'first second third'}
  end

  it 'should handle \t, \r, \n and \f' do
    hash = Hash.from_java_properties(<<-PROPS)

name1=with\tand\r

name2=with\\nand\f

name3=double\\\\hash
PROPS
    hash.should == {'name1'=>"with\tand", 'name2'=>"with\nand\f", 'name3'=>'double\hash'}
  end

  it 'should ignore whitespace' do
    hash = Hash.from_java_properties('name1 = value1')
    hash.should == {'name1'=>'value1'}
  end
end


describe Hash, '#to_java_properties' do
  it 'should return name/value pairs' do
    props = {'name1'=>'value1', 'name2'=>'value2'}.to_java_properties
    props.split("\n").size.should be(2)
    props.split("\n").should include('name1=value1')
    props.split("\n").should include('name2=value2')
  end

  it 'should handle \t, \r, \n and \f' do
    props = {'name1'=>"with\tand\r", 'name2'=>"with\nand\f", 'name3'=>'double\hash'}.to_java_properties
    props.split("\n").should include("name1=with\\tand\\r")
    props.split("\n").should include("name2=with\\nand\\f")
    props.split("\n").should include("name3=double\\\\hash")
  end
end
