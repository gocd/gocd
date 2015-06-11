require_rspec = if defined?(require_relative)
  lambda do |path|
    require_relative path
  end
else
  lambda do |path|
    require "rspec/#{path}"
  end
end

require 'set'
require 'time'
require 'rbconfig'
require_rspec['core/filter_manager']
require_rspec['core/dsl']
require_rspec['core/extensions/kernel']
require_rspec['core/extensions/instance_eval_with_args']
require_rspec['core/extensions/module_eval_with_args']
require_rspec['core/extensions/ordered']
require_rspec['core/deprecation']
require_rspec['core/backward_compatibility']
require_rspec['core/reporter']

require_rspec['core/metadata_hash_builder']
require_rspec['core/hooks']
require_rspec['core/memoized_helpers']
require_rspec['core/metadata']
require_rspec['core/pending']
require_rspec['core/formatters']

require_rspec['core/world']
require_rspec['core/configuration']
require_rspec['core/project_initializer']
require_rspec['core/option_parser']
require_rspec['core/configuration_options']
require_rspec['core/command_line']
require_rspec['core/runner']
require_rspec['core/example']
require_rspec['core/shared_example_group/collection']
require_rspec['core/shared_example_group']
require_rspec['core/example_group']
require_rspec['core/version']

module RSpec
  autoload :SharedContext, 'rspec/core/shared_context'

  # @private
  def self.wants_to_quit
  # Used internally to determine what to do when a SIGINT is received
    world.wants_to_quit
  end

  # @private
  # Used internally to determine what to do when a SIGINT is received
  def self.wants_to_quit=(maybe)
    world.wants_to_quit=(maybe)
  end

  # @private
  # Internal container for global non-configuration data
  def self.world
    @world ||= RSpec::Core::World.new
  end

  # @private
  # Used internally to set the global object
  def self.world=(new_world)
    @world = new_world
  end

  # @private
  # Used internally to ensure examples get reloaded between multiple runs in
  # the same process.
  def self.reset
    @world = nil
    @configuration = nil
  end

  # Returns the global [Configuration](RSpec/Core/Configuration) object. While you
  # _can_ use this method to access the configuration, the more common
  # convention is to use [RSpec.configure](RSpec#configure-class_method).
  #
  # @example
  #     RSpec.configuration.drb_port = 1234
  # @see RSpec.configure
  # @see Core::Configuration
  def self.configuration
    if block_given?
      RSpec.warn_deprecation <<-WARNING

*****************************************************************
DEPRECATION WARNING

* RSpec.configuration with a block is deprecated and has no effect.
* please use RSpec.configure with a block instead.

Called from #{caller(0)[1]}
*****************************************************************

WARNING
    end
    @configuration ||= RSpec::Core::Configuration.new
  end

  # @private
  # Used internally to set the global object
  def self.configuration=(new_configuration)
    @configuration = new_configuration
  end

  # Yields the global configuration to a block.
  # @yield [Configuration] global configuration
  #
  # @example
  #     RSpec.configure do |config|
  #       config.add_formatter 'documentation'
  #     end
  # @see Core::Configuration
  def self.configure
    yield configuration if block_given?
  end

  # @private
  # Used internally to clear remaining groups when fail_fast is set
  def self.clear_remaining_example_groups
    world.example_groups.clear
  end

  # @private
  def self.windows_os?
    RbConfig::CONFIG['host_os'] =~ /cygwin|mswin|mingw|bccwin|wince|emx/
  end

  module Core
    # @private
    # This avoids issues with reporting time caused by examples that
    # change the value/meaning of Time.now without properly restoring
    # it.
    class Time
      class << self
        define_method(:now,&::Time.method(:now))
      end
    end
  end

  MODULES_TO_AUTOLOAD = {
    :Matchers     => "rspec/expectations",
    :Expectations => "rspec/expectations",
    :Mocks        => "rspec/mocks"
  }

  def self.const_missing(name)
    # Load rspec-expectations when RSpec::Matchers is referenced. This allows
    # people to define custom matchers (using `RSpec::Matchers.define`) before
    # rspec-core has loaded rspec-expectations (since it delays the loading of
    # it to allow users to configure a different assertion/expectation
    # framework). `autoload` can't be used since it works with ruby's built-in
    # require (e.g. for files that are available relative to a load path dir),
    # but not with rubygems' extended require.
    #
    # As of rspec 2.14.1, we no longer require `rspec/mocks` and
    # `rspec/expectations` when `rspec` is required, so we want
    # to make them available as an autoload. For more info, see:
    require MODULES_TO_AUTOLOAD.fetch(name) { return super }
    ::RSpec.const_get(name)
  end
end

require_rspec['core/backward_compatibility']
