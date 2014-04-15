require 'rspec/expectations/syntax'

module RSpec
  module Matchers
    # Provides configuration options for rspec-expectations.
    class Configuration
      # Configures the supported syntax.
      # @param [Array<Symbol>, Symbol] values the syntaxes to enable
      # @example
      #   RSpec.configure do |rspec|
      #     rspec.expect_with :rspec do |c|
      #       c.syntax = :should
      #       # or
      #       c.syntax = :expect
      #       # or
      #       c.syntax = [:should, :expect]
      #     end
      #   end
      def syntax=(values)
        if Array(values).include?(:expect)
          Expectations::Syntax.enable_expect
        else
          Expectations::Syntax.disable_expect
        end

        if Array(values).include?(:should)
          Expectations::Syntax.enable_should
        else
          Expectations::Syntax.disable_should
        end
      end

      # The list of configured syntaxes.
      # @return [Array<Symbol>] the list of configured syntaxes.
      def syntax
        syntaxes = []
        syntaxes << :should if Expectations::Syntax.should_enabled?
        syntaxes << :expect if Expectations::Syntax.expect_enabled?
        syntaxes
      end

      # Adds `should` and `should_not` to the given classes
      # or modules. This can be used to ensure `should` works
      # properly on things like proxy objects (particular
      # `Delegator`-subclassed objects on 1.8).
      #
      # @param [Array<Module>] modules the list of classes or modules
      #   to add `should` and `should_not` to.
      def add_should_and_should_not_to(*modules)
        modules.each do |mod|
          Expectations::Syntax.enable_should(mod)
        end
      end
    end

    # The configuration object
    # @return [RSpec::Matchers::Configuration] the configuration object
    def self.configuration
      @configuration ||= Configuration.new
    end

    # set default syntax
    configuration.syntax = [:expect, :should]
  end
end

