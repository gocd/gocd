module RSpec
  module Mocks
    # Provides configuration options for rspec-mocks.
    class Configuration

      def initialize
        @yield_receiver_to_any_instance_implementation_blocks = false
        @should_warn_about_any_instance_blocks = true
        @marshal_patched = false
      end

      def yield_receiver_to_any_instance_implementation_blocks?
        @yield_receiver_to_any_instance_implementation_blocks
      end

      def yield_receiver_to_any_instance_implementation_blocks=(arg)
        @should_warn_about_any_instance_blocks = false
        @yield_receiver_to_any_instance_implementation_blocks = arg
      end

      def should_warn_about_any_instance_blocks?
        @should_warn_about_any_instance_blocks
      end

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

      def patch_marshal_to_support_partial_doubles=(val)
        @marshal_patched = val
      end

      def marshal_patched?
        @marshal_patched
      end
    end

    def self.configuration
      @configuration ||= Configuration.new
    end

    configuration.syntax = [:should, :expect]
  end
end

