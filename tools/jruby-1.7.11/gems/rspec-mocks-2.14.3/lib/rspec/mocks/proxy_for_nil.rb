module RSpec
  module Mocks
    # @private
    class ProxyForNil < Proxy

      def initialize
        @warn_about_expectations = true
        super nil
      end
      attr_accessor :warn_about_expectations
      alias warn_about_expectations? warn_about_expectations

      def add_message_expectation(location, method_name, opts={}, &block)
        warn(method_name) if warn_about_expectations?
        super
      end

      def add_negative_message_expectation(location, method_name, &implementation)
        warn(method_name) if warn_about_expectations?
        super
      end

      def add_stub(location, method_name, opts={}, &implementation)
        warn(method_name) if warn_about_expectations?
        super
      end

      private

      def warn method_name
        non_rspec_caller = caller.find { |line| !line.include?('lib/rspec/mocks') }
        Kernel.warn("An expectation of :#{method_name} was set on nil. Called from #{non_rspec_caller}. Use allow_message_expectations_on_nil to disable warnings.")
      end

    end
  end
end
