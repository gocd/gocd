if defined?(require_relative)
  # @private
  def require_rspec(path)
    require_relative path
  end
else
  # @private
  def require_rspec(path)
    require "rspec/#{path}"
  end
end

require 'set'
require 'rbconfig'
require_rspec 'core/filter_manager'
require_rspec 'core/dsl'
require_rspec 'core/extensions'
require_rspec 'core/load_path'
require_rspec 'core/deprecation'
require_rspec 'core/backward_compatibility'
require_rspec 'core/reporter'

require_rspec 'core/metadata_hash_builder'
require_rspec 'core/hooks'
require_rspec 'core/subject'
require_rspec 'core/let'
require_rspec 'core/metadata'
require_rspec 'core/pending'

require_rspec 'core/world'
require_rspec 'core/configuration'
require_rspec 'core/project_initializer'
require_rspec 'core/option_parser'
require_rspec 'core/configuration_options'
require_rspec 'core/command_line'
require_rspec 'core/runner'
require_rspec 'core/example'
require_rspec 'core/shared_example_group'
require_rspec 'core/example_group'
require_rspec 'core/version'

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
    @configuration ||= RSpec::Core::Configuration.new
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
    /mswin|mingw/ === ::RbConfig::CONFIG['host_os']
  end

  module Core
  end

  def self.const_missing(name)
    case name
      when :Matchers
        # Load rspec-expectations when RSpec::Matchers is referenced. This allows
        # people to define custom matchers (using `RSpec::Matchers.define`) before
        # rspec-core has loaded rspec-expectations (since it delays the loading of
        # it to allow users to configure a different assertion/expectation
        # framework). `autoload` can't be used since it works with ruby's built-in
        # require (e.g. for files that are available relative to a load path dir),
        # but not with rubygems' extended require.
        require 'rspec/expectations'
        ::RSpec::Matchers
      else super
    end
  end
end

require_rspec 'core/backward_compatibility'
