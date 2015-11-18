module RSpec
  module Mocks
    # Provides configuration options for rspec-mocks.
    class Configuration
      # Adds `stub` and `should_receive` to the given
      # modules or classes. This is usually only necessary
      # if you application uses some proxy classes that
      # "strip themselves down" to a bare minimum set of
      # methods and remove `stub` and `should_receive` in
      # the process.
      #
      # @example
      #
      #   RSpec.configure do |rspec|
      #     rspec.mock_with :rspec do |mocks|
      #       mocks.add_stub_and_should_receive_to Delegator
      #     end
      #   end
      #
      def add_stub_and_should_receive_to(*modules)
        modules.each do |mod|
          Syntax.enable_should(mod)
        end
      end

      def syntax=(values)
        if Array(values).include?(:expect)
          Syntax.enable_expect
        else
          Syntax.disable_expect
        end

        if Array(values).include?(:should)
          Syntax.enable_should
        else
          Syntax.disable_should
        end
      end

      def syntax
        syntaxes = []
        syntaxes << :should  if Syntax.should_enabled?
        syntaxes << :expect if Syntax.expect_enabled?
        syntaxes
      end
    end

    def self.configuration
      @configuration ||= Configuration.new
    end

    configuration.syntax = [:should, :expect]
  end
end

