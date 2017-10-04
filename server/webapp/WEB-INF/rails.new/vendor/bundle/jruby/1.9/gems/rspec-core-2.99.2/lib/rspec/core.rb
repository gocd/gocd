require_rspec = if defined?(require_relative)
  lambda do |path|
    require_relative path
  end
else # for 1.8.7
  lambda do |path|
    require "rspec/#{path}"
  end
end

require 'set'
require 'time'
require 'rbconfig'
require_rspec['core/caller_filter']
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

  class << self
    # @private
    # Counts the number of times RSpec.reset needs to be called to prevent
    # a warning about reset's no longer being implicitly invoked.
    attr_accessor :resets_required
    RSpec.resets_required = 0
  end

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
  # Query to check if the user has called RSpec.reset
  def self.user_has_called_reset?
    @user_has_called_reset ||= false
  end

  # @private
  # Warns that RSpec 3.0.0 will no longer call reset for users
  def self.warn_about_calling_reset
    RSpec.warn_deprecation(<<-EOD)
Calling `RSpec::Core::Runner.run` will no longer implicitly invoke
`RSpec.reset` as of RSpec 3.0.0. If you need RSpec to be reset between your
calls to `RSpec::Core::Runner.run` please invoke `RSpec.reset` manually in the
appropriate place.
EOD
  end

  # @public
  # Used internally to ensure examples get reloaded between multiple runs in
  # the same process.
  def self.reset
    RSpec.resets_required -= 1
    internal_reset
  end

  # @private
  # @see RSpec.reset
  # Warns if the user has invoked RSpec.run twice in the same process.
  def self.internal_reset
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

Called from #{CallerFilter.first_non_rspec_line}
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

  # The example being executed.
  #
  # The primary audience for this method is library authors who need access
  # to the example currently being executed and also want to support all
  # versions of RSpec 2 and 3.
  #
  # @example
  #
  #     RSpec.configure do |c|
  #       # context.example is deprecated, but RSpec.current_example is not
  #       # available until RSpec 3.0.
  #       fetch_current_example = RSpec.respond_to?(:current_example) ?
  #         proc { RSpec.current_example } : proc { |context| context.example }
  #
  #       c.before(:each) do
  #         example = fetch_current_example.call(self)
  #
  #         # ...
  #       end
  #     end
  #
  def self.current_example
    Thread.current[:_rspec_current_example]
  end

  # Set the current example being executed.
  # @api private
  def self.current_example=(example)
    Thread.current[:_rspec_current_example] = example
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

    # @private path to executable file
    def self.path_to_executable
      File.expand_path('../../../exe/rspec', __FILE__)
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
