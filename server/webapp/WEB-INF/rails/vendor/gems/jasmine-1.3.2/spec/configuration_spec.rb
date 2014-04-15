require 'spec_helper'

describe Jasmine::Configuration do
  let(:test_mapper1) do
    Class.new do
      def initialize(config)
        @config = config
      end
      def map_src_paths(paths)
        paths.map { |f| "mapped_src/#{f}" }
      end
      def map_jasmine_paths(paths)
        paths.map { |f| "mapped_jasmine/#{f}" }
      end
      def map_spec_paths(paths)
        paths.map { |f| "mapped_spec/#{f}" }
      end
      def map_boot_paths(paths)
        paths.map { |f| "mapped_boot/#{f}" }
      end
    end
  end
  let(:test_mapper2) do
    Class.new do
      def initialize(config)
        @config = config
      end
      def map_src_paths(paths)
        paths.map { |f| "#{f}/src" }
      end
      def map_jasmine_paths(paths)
        paths.map { |f| "#{f}/jasmine" }
      end
      def map_spec_paths(paths)
        paths.map { |f| "#{f}/spec" }
      end
      def map_boot_paths(paths)
        paths.map { |f| "#{f}/boot" }
      end
    end
  end
  let(:test_mapper3) do
    Class.new do
      def initialize(config)
        @config = config
      end
    end
  end

  describe "returning css files" do
    it "returns mapped jasmine_css_files + css_files" do
      config = Jasmine::Configuration.new()
      config.add_path_mapper(lambda { |c| test_mapper1.new(c) })
      config.add_path_mapper(lambda { |c| test_mapper2.new(c) })
      config.add_path_mapper(lambda { |c| test_mapper3.new(c) })
      config.css_files.should == []
      config.jasmine_css_files = lambda { ["jasmine_css"] }
      config.css_files = lambda { ["css"] }
      config.css_files.should == ['mapped_jasmine/jasmine_css/jasmine', 'mapped_src/css/src']
    end
  end

  describe "returning javascript files" do
    it "returns the jasmine core files, then srcs, then specs, then boot" do
      config = Jasmine::Configuration.new()
      config.add_path_mapper(lambda { |c| test_mapper1.new(c) })
      config.add_path_mapper(lambda { |c| test_mapper2.new(c) })
      config.add_path_mapper(lambda { |c| test_mapper3.new(c) })
      config.js_files.should == []
      config.jasmine_files = lambda { ['jasmine'] }
      config.src_files = lambda  { ['src'] }
      config.boot_files = lambda { ['boot'] }
      config.spec_files = lambda { ['spec'] }
      config.js_files.should == [
        'mapped_jasmine/jasmine/jasmine',
        'mapped_src/src/src',
        'mapped_spec/spec/spec',
        'mapped_boot/boot/boot',
      ]
    end
  end

  describe "returning rack map" do
    it "permits arbitrary rack app path mapping" do
      config = Jasmine::Configuration.new()
      result = double
      config.add_rack_path('some/path', lambda { result })
      map = config.rack_path_map
      map['some/path'].should be
      map['some/path'].call.should == result
    end

  end

  describe "rack apps" do
    it "permits the addition of arbitary rack apps" do
      config = Jasmine::Configuration.new()
      app = double
      config.add_rack_app(app)
      config.rack_apps.should == [[app, nil]]
    end
    it "permits the addition of arbitary rack apps with arbitrary config" do
      config = Jasmine::Configuration.new()
      app = double
      block = lambda { "foo" }
      config.add_rack_app(app, &block)
      config.rack_apps.should == [[app, block]]
    end
  end

  describe "port" do
    it "returns new port and caches return value" do
      config = Jasmine::Configuration.new()
      Jasmine.stub(:find_unused_port).and_return('1234')
      config.port.should == '1234'
      Jasmine.stub(:find_unused_port).and_return('4321')
      config.port.should == '1234'
    end
    it "returns port if configured" do
      config = Jasmine::Configuration.new()
      config.port = '5678'
      Jasmine.stub(:find_unused_port).and_return('1234')
      config.port.should == '5678'
    end
  end

  describe "browser" do
    it "should default to firefox" do
      Jasmine::Configuration.new().browser.should == 'firefox'
    end

    it "returns browser if set" do
      config = Jasmine::Configuration.new()
      config.browser = 'foo'
      config.browser.should == 'foo'
    end
  end

  describe "result_batch_size" do
    it "should default to 50" do
      Jasmine::Configuration.new().result_batch_size.should == 50
    end

    it "returns result_batch_size if set" do
      config = Jasmine::Configuration.new()
      config.result_batch_size = 25
      config.result_batch_size.should == 25
    end
  end

  describe "host" do
    it "should default to localhost" do
      Jasmine::Configuration.new().host.should == 'http://localhost'
    end

    it "returns host if set" do
      config = Jasmine::Configuration.new()
      config.host = 'foo'
      config.host.should == 'foo'
    end
  end
end

