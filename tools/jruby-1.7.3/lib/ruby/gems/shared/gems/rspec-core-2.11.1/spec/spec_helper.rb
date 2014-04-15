require 'rubygems'

begin
  require 'spork'
rescue LoadError
  module Spork
    def self.prefork
      yield
    end

    def self.each_run
      yield
    end
  end
end

Spork.prefork do
  require 'rspec/autorun'
  require 'autotest/rspec2'
  require 'aruba/api'
  require 'fakefs/spec_helpers'

  Dir['./spec/support/**/*.rb'].map {|f| require f}

  class NullObject
    private
    def method_missing(method, *args, &block)
      # ignore
    end
  end

  def sandboxed(&block)
    @orig_config = RSpec.configuration
    @orig_world  = RSpec.world
    new_config = RSpec::Core::Configuration.new
    new_world  = RSpec::Core::World.new(new_config)
    RSpec.instance_variable_set(:@configuration, new_config)
    RSpec.instance_variable_set(:@world, new_world)
    object = Object.new
    object.extend(RSpec::Core::SharedExampleGroup)

    (class << RSpec::Core::ExampleGroup; self; end).class_eval do
      alias_method :orig_run, :run
      def run(reporter=nil)
        @orig_mock_space = RSpec::Mocks::space
        RSpec::Mocks::space = RSpec::Mocks::Space.new
        orig_run(reporter || NullObject.new)
      ensure
        RSpec::Mocks::space = @orig_mock_space
      end
    end

    object.instance_eval(&block)
  ensure
    (class << RSpec::Core::ExampleGroup; self; end).class_eval do
      remove_method :run
      alias_method :run, :orig_run
      remove_method :orig_run
    end

    RSpec.instance_variable_set(:@configuration, @orig_config)
    RSpec.instance_variable_set(:@world, @orig_world)
  end

  def in_editor?
    ENV.has_key?('TM_MODE') || ENV.has_key?('EMACS') || ENV.has_key?('VIM')
  end

  RSpec.configure do |c|
    # structural
    c.alias_it_behaves_like_to 'it_has_behavior'
    c.around {|example| sandboxed { example.run }}
    c.include(RSpecHelpers)
    c.include Aruba::Api, :example_group => {
      :file_path => /spec\/command_line/
    }

    # runtime options
    c.treat_symbols_as_metadata_keys_with_true_values = true
    c.color = !in_editor?
    c.filter_run :focus
    c.include FakeFS::SpecHelpers, :fakefs
    c.run_all_when_everything_filtered = true
    c.filter_run_excluding :ruby => lambda {|version|
      case version.to_s
      when "!jruby"
        RUBY_ENGINE == "jruby"
      when /^> (.*)/
        !(RUBY_VERSION.to_s > $1)
      else
        !(RUBY_VERSION.to_s =~ /^#{version.to_s}/)
      end
    }
  end
end

Spork.each_run do
end
