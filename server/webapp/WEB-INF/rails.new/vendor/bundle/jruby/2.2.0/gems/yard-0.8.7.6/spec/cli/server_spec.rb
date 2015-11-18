require File.dirname(__FILE__) + '/../spec_helper'

class Server::WebrickAdapter; def start; end end

describe YARD::CLI::Server do
  before do
    CLI::Yardoc.stub!(:run)
    @no_verify_libraries = false
    @set_libraries = true
    @no_adapter_mock = false
    @libraries = {}
    @options = {:single_library => true, :caching => false}
    @server_options = {:Port => 8808}
    @adapter = mock(:adapter)
    @adapter.stub!(:setup)
    new_cli
  end

  after(:all) do
    Server::Adapter.shutdown
  end

  def new_cli
    @cli = subject
  end

  def rack_required
    begin; require 'rack'; rescue LoadError; pending "rack required for this test" end
  end

  def bundler_required
    begin; require 'bundler'; rescue LoadError; pending "bundler required for this test" end
  end

  def unstub_adapter
    @no_adapter_mock = true
  end

  def run(*args)
    if @set_libraries && @libraries.empty?
      library = Server::LibraryVersion.new(
        File.basename(Dir.pwd), nil, File.expand_path('.yardoc'))
      @libraries = {library.name => [library]}
    end
    unless @no_verify_libraries
      @libraries.values.each do |libs|
        libs.each do |lib|
          yfile = File.expand_path(lib.yardoc_file)
          File.stub!(:exist?).with(yfile).and_return(true)
        end
      end
    end
    unless @no_adapter_mock
      @cli.stub!(:adapter).and_return(@adapter)
      @adapter.should_receive(:new).
        with(@libraries, @options, @server_options).and_return(@adapter)
      @adapter.should_receive(:start)
    end

    @cli.run(*args.flatten)
    assert_libraries @libraries, @cli.libraries
  end

  def assert_libraries(expected_libs, actual_libs)
    actual_libs.should == expected_libs
    expected_libs.each do |name, libs|
      libs.each_with_index do |expected,i|
        actual = actual_libs[name][i]
        [:source, :source_path, :yardoc_file].each do |m|
          actual.send(m).should == expected.send(m)
        end
      end
    end
  end

  # Mocks the existence of a file.
  def mock_file(filename, content = nil)
    File.stub!(:exist?).with(filename).and_return(true)
    File.stub!(:read_binary).with(filename).and_return(content) if content
    filename_e = File.expand_path(filename)
    mock_file(filename_e) unless filename_e == filename
  end

  describe 'when .yardopts file exists' do
    before :each do
      Registry.yardoc_file = Registry::DEFAULT_YARDOC_FILE
      Dir.stub!(:pwd).and_return('/path/to/bar')
      @name = 'bar'
    end

    it "should use .yardoc as the yardoc db if .yardopts doesn't specify an alternate path" do
      mock_file '/path/to/bar/.yardopts', '--protected'
      @libraries[@name] = [Server::LibraryVersion.new(@name, nil, File.expand_path('/path/to/bar/.yardoc'))]
      @libraries.values[0][0].source_path = File.expand_path('/path/to/bar')
      run
    end

    it "should use the yardoc db location specified by .yardopts" do
      mock_file '/path/to/bar/.yardopts', '--db foo'
      @libraries[@name] = [Server::LibraryVersion.new(@name, nil, File.expand_path('/path/to/bar/foo'))]
      @libraries.values[0][0].source_path = File.expand_path('/path/to/bar')
      run
    end

    it "should parse .yardopts when the library list is odd" do
      mock_file '/path/to/bar/.yardopts', '--db foo'
      @libraries['a'] = [Server::LibraryVersion.new('a', nil, File.expand_path('/path/to/bar/foo'))]
      @libraries.values[0][0].source_path = File.expand_path('/path/to/bar')
      run 'a'
    end
  end

  describe "when .yardopts file doesn't exist" do
    before :each do
      File.stub(:exist?).with(/^(.*[\\\/])?\.yardopts$/).and_return(false)
    end

    it "should default to .yardoc if no library is specified" do
      Dir.should_receive(:pwd).at_least(:once).and_return(File.expand_path('/path/to/foo'))
      @libraries['foo'] = [Server::LibraryVersion.new('foo', nil, File.expand_path('/path/to/foo/.yardoc'))]
      run
    end

    it "should use .yardoc as yardoc file if library list is odd" do
      @libraries['a'] = [Server::LibraryVersion.new('a', nil, File.expand_path('.yardoc'))]
      run 'a'
    end

    it "should force multi library if more than one library is listed" do
      File.stub(:exist?).with('b').and_return(true)
      @options[:single_library] = false
      @libraries['a'] = [Server::LibraryVersion.new('a', nil, File.expand_path('b'))]
      @libraries['c'] = [Server::LibraryVersion.new('c', nil, File.expand_path('.yardoc'))]
      run %w(a b c)
    end

    it "should fail if specified directory does not exist" do
      @set_libraries = false
      File.stub(:exist?).with('b').and_return(false)
      log.should_receive(:warn).with(/Cannot find yardoc db for a: "b"/)
      run %w(a b)
    end
  end

  describe 'General options' do
    before do
      File.stub(:exist?).with(/\.yardopts$/).and_return(false)
    end

    it "should accept -m, --multi-library" do
      @options[:single_library] = false
      run '-m'
      run '--multi-library'
    end

    it "should accept -c, --cache" do
      @options[:caching] = true
      run '-c'
      run '--cache'
    end

    it "should accept -r, --reload" do
      @options[:incremental] = true
      run '-r'
      run '--reload'
    end

    it "should accept -d, --daemon" do
      @server_options[:daemonize] = true
      run '-d'
      run '--daemon'
    end

    it "should accept -B, --bind" do
      @server_options[:Host] = 'example.com'
      run '-B', 'example.com'
      run '--bind', 'example.com'
    end    

    it "should bind address with WebRick adapter" do
      @server_options[:Host] = 'example.com'
      run '-B', 'example.com', '-a', 'webrick'
      run '--bind', 'example.com', '-a', 'webrick'
    end  

    it "should bind address with Rack adapter" do
      @server_options[:Host] = 'example.com'
      run '-B', 'example.com', '-a', 'rack'
      run '--bind', 'example.com', '-a', 'rack'
    end          

    it "should accept -p, --port" do
      @server_options[:Port] = 10
      run '-p', '10'
      run '--port', '10'
    end

    it "should accept --docroot" do
      @server_options[:DocumentRoot] = Dir.pwd + '/__foo/bar'
      run '--docroot', '__foo/bar'
    end

    it "should accept -a webrick to create WEBrick adapter" do
      @cli.should_receive(:adapter=).with(YARD::Server::WebrickAdapter)
      run '-a', 'webrick'
    end

    it "should accept -a rack to create Rack adapter" do
      rack_required
      @cli.should_receive(:adapter=).with(YARD::Server::RackAdapter)
      run '-a', 'rack'
    end

    it "should default to Rack adapter if exists on system" do
      rack_required
      @cli.should_receive(:require).with('rubygems').and_return(false)
      @cli.should_receive(:require).with('rack').and_return(true)
      @cli.should_receive(:adapter=).with(YARD::Server::RackAdapter)
      @cli.send(:select_adapter)
    end

    it "should fall back to WEBrick adapter if Rack is not on system" do
      @cli.should_receive(:require).with('rubygems').and_return(false)
      @cli.should_receive(:require).with('rack').and_raise(LoadError)
      @cli.should_receive(:adapter=).with(YARD::Server::WebrickAdapter)
      @cli.send(:select_adapter)
    end

    it "should accept -s, --server" do
      @server_options[:server] = 'thin'
      run '-s', 'thin'
      run '--server', 'thin'
    end

    it "should accept -g, --gems" do
      @no_verify_libraries = true
      @options[:single_library] = false
      @libraries['gem1'] = [Server::LibraryVersion.new('gem1', '1.0.0', nil, :gem)]
      @libraries['gem2'] = [Server::LibraryVersion.new('gem2', '1.0.0', nil, :gem)]
      gem1 = mock(:gem1)
      gem1.stub!(:name).and_return('gem1')
      gem1.stub!(:version).and_return('1.0.0')
      gem1.stub!(:full_gem_path).and_return('/path/to/foo')
      gem2 = mock(:gem2)
      gem2.stub!(:name).and_return('gem2')
      gem2.stub!(:version).and_return('1.0.0')
      gem2.stub!(:full_gem_path).and_return('/path/to/bar')
      specs = {'gem1' => gem1, 'gem2' => gem2}
      source = mock(:source_index)
      source.stub!(:find_name).and_return do |k, ver|
        k == '' ? specs.values : specs.grep(k).map {|name| specs[name] }
      end
      Gem.stub!(:source_index).and_return(source)
      run '-g'
      run '--gems'
    end

    it "should accept -G, --gemfile" do
      bundler_required
      @no_verify_libraries = true
      @options[:single_library] = false

      @libraries['gem1'] = [Server::LibraryVersion.new('gem1', '1.0.0', nil, :gem)]
      @libraries['gem2'] = [Server::LibraryVersion.new('gem2', '1.0.0', nil, :gem)]
      gem1 = mock(:gem1)
      gem1.stub!(:name).and_return('gem1')
      gem1.stub!(:version).and_return('1.0.0')
      gem1.stub!(:full_gem_path).and_return('/path/to/foo')
      gem2 = mock(:gem2)
      gem2.stub!(:name).and_return('gem2')
      gem2.stub!(:version).and_return('1.0.0')
      gem2.stub!(:full_gem_path).and_return('/path/to/bar')
      specs = {'gem1' => gem1, 'gem2' => gem2}
      lockfile_parser = mock(:new)
      lockfile_parser.stub!(:specs).and_return([gem1, gem2])
      Bundler::LockfileParser.stub!(:new).and_return(lockfile_parser)

      File.should_receive(:exist?).at_least(2).times.with("Gemfile.lock").and_return(true)
      File.stub!(:read)

      run '-G'
      run '--gemfile'

      File.should_receive(:exist?).with("different_name.lock").and_return(true)
      run '--gemfile', 'different_name'
    end

    it "should warn if lockfile is not found (with -G)" do
      bundler_required
      File.should_receive(:exist?).with(/\.yardopts$/).at_least(:once).and_return(false)
      File.should_receive(:exist?).with('somefile.lock').and_return(false)
      log.should_receive(:warn).with(/Cannot find somefile.lock/)
      run '-G', 'somefile'
    end

    it "should error if Bundler not available (with -G)" do
      @cli.should_receive(:require).with('bundler').and_raise(LoadError)
      log.should_receive(:error).with(/Bundler not available/)
      run '-G'
    end

    it "should load template paths after adapter template paths" do
      unstub_adapter
      @cli.adapter = Server::WebrickAdapter
      run '-t', 'foo'
      Templates::Engine.template_paths.last.should == 'foo'
    end

    it "should load ruby code (-e) after adapter" do
      unstub_adapter
      @cli.adapter = Server::WebrickAdapter
      path = File.dirname(__FILE__) + '/tmp.adapterscript.rb'
      begin
        File.open(path, 'w') do |f|
          f.puts "YARD::Templates::Engine.register_template_path 'foo'"
          f.flush
          run '-e', f.path
          Templates::Engine.template_paths.last.should == 'foo'
        end
      ensure
        File.unlink(path)
      end
    end
  end
end
