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

  describe 'returning css files' do
    it 'returns mapped jasmine_css_files + css_files' do
      config = Jasmine::Configuration.new()
      config.add_path_mapper(lambda { |c| test_mapper1.new(c) })
      config.add_path_mapper(lambda { |c| test_mapper2.new(c) })
      config.add_path_mapper(lambda { |c| test_mapper3.new(c) })
      expect(config.css_files).to eq []
      config.jasmine_css_files = lambda { %w(jasmine_css) }
      config.css_files = lambda { %w(css) }
      expect(config.css_files).to eq %w(mapped_jasmine/jasmine_css/jasmine mapped_src/css/src)
    end
  end

  describe 'returning javascript files' do
    it 'returns the jasmine core files, then srcs, then specs, then boot' do
      config = Jasmine::Configuration.new()
      config.add_path_mapper(lambda { |c| test_mapper1.new(c) })
      config.add_path_mapper(lambda { |c| test_mapper2.new(c) })
      config.add_path_mapper(lambda { |c| test_mapper3.new(c) })
      expect(config.js_files).to eq []
      config.jasmine_files = lambda { %w(jasmine) }
      config.src_files = lambda  { %w(src) }
      config.boot_files = lambda { %w(boot) }
      config.spec_files = lambda { %w(spec) }
      config.helper_files = lambda { %w(helper) }
      expect(config.js_files).to eq %w(
        mapped_jasmine/jasmine/jasmine
        mapped_boot/boot/boot mapped_src/src/src
        mapped_spec/helper/spec
        mapped_spec/spec/spec)
    end
  end

  describe 'returning rack map' do
    it 'permits arbitrary rack app path mapping' do
      config = Jasmine::Configuration.new()
      result = double
      config.add_rack_path('some/path', lambda { result })
      map = config.rack_path_map
      expect(map['some/path']).to be
      expect(map['some/path'].call).to eq result
    end
  end

  describe 'rack apps' do
    it 'permits the addition of arbitary rack apps' do
      config = Jasmine::Configuration.new()
      app = double
      config.add_rack_app(app)
      expect(config.rack_apps).to eq [{ :app => app, :args => [], :block => nil }]
    end

    it 'permits the addition of arbitary rack apps with a config block' do
      config = Jasmine::Configuration.new()
      app = double
      block = lambda { 'foo' }
      config.add_rack_app(app, &block)
      expect(config.rack_apps).to eq [{ :app => app, :args => [], :block => block }]
    end

    it 'permits the addition of arbitary rack apps with arbitrary config' do
      config = Jasmine::Configuration.new()
      app = double
      config.add_rack_app(app, { :foo => 'bar' })
      expect(config.rack_apps).to eq [{ :app => app, :args => [{ :foo => 'bar' }], :block => nil }]
    end

    it 'permits the addition of arbitary rack apps with arbitrary config and a config block' do
      config = Jasmine::Configuration.new()
      app = double
      block = lambda { 'foo' }
      config.add_rack_app(app, { :foo => 'bar' }, &block)
      expect(config.rack_apps).to eq [{ :app => app, :args => [{ :foo => 'bar' }], :block => block }]
    end
  end

  describe 'host' do
    it 'should default to localhost' do
      expect(Jasmine::Configuration.new().host).to eq 'http://localhost'
    end

    it 'returns host if set' do
      config = Jasmine::Configuration.new()
      config.host = 'foo'
      expect(config.host).to eq 'foo'
    end
  end

  describe 'spec format' do
    it 'returns value if set' do
      config = Jasmine::Configuration.new()
      config.spec_format = 'fish'
      expect(config.spec_format).to eq 'fish'
    end
  end

  describe 'prevent phantomjs auto install' do
    it 'returns value if set' do
      config = Jasmine::Configuration.new()
      config.prevent_phantom_js_auto_install = true
      expect(config.prevent_phantom_js_auto_install).to eq true
    end
  end

  describe 'show full stack trace' do
    it 'returns value if set' do
      config = Jasmine::Configuration.new()
      config.show_full_stack_trace = true
      expect(config.show_full_stack_trace).to eq true
    end
  end

  describe 'jasmine ports' do
    it 'returns new CI port and caches return value' do
      config = Jasmine::Configuration.new()
      allow(Jasmine).to receive(:find_unused_port).and_return('1234')
      expect(config.port(:ci)).to eq '1234'
      allow(Jasmine).to receive(:find_unused_port).and_return('4321')
      expect(config.port(:ci)).to eq '1234'
    end

    it 'returns ci port if configured' do
      config = Jasmine::Configuration.new()
      config.ci_port = '5678'
      allow(Jasmine).to receive(:find_unused_port).and_return('1234')
      expect(config.port(:ci)).to eq '5678'
    end

    it 'returns configured server port' do
      config = Jasmine::Configuration.new()
      config.server_port = 'fish'
      expect(config.port(:server)).to eq 'fish'
    end

    it 'returns default server port' do
      config = Jasmine::Configuration.new()

      expect(config.port(:server)).to eq 8888
    end
  end

  describe 'jasmine formatters' do
    it 'returns value if set' do
      config = Jasmine::Configuration.new()
      config.formatters = ['pants']
      expect(config.formatters).to eq ['pants']
    end

    it 'returns defaults' do
      config = Jasmine::Configuration.new()

      expect(config.formatters).to eq [Jasmine::Formatters::Console]
    end
  end

  describe 'custom runner' do
    it 'stores and returns what is passed' do
      config = Jasmine::Configuration.new
      foo = double(:foo)
      config.runner = foo
      expect(config.runner).to eq foo
    end

    it 'does nothing by default' do
      config = Jasmine::Configuration.new
      config.runner.call('hi')
    end
  end
end

