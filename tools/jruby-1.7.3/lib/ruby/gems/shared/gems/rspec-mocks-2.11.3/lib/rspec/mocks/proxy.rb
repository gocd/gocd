module RSpec
  module Mocks
    # @private
    class Proxy
      class << self
        # @private
        def warn_about_expectations_on_nil
          defined?(@warn_about_expectations_on_nil) ? @warn_about_expectations_on_nil : true
        end

        # @private
        def warn_about_expectations_on_nil=(new_value)
          @warn_about_expectations_on_nil = new_value
        end

        # @private
        def allow_message_expectations_on_nil
          @warn_about_expectations_on_nil = false

          # ensure nil.rspec_verify is called even if an expectation is not set in the example
          # otherwise the allowance would effect subsequent examples
          RSpec::Mocks::space.add(nil) unless RSpec::Mocks::space.nil?
        end

        # @private
        def allow_message_expectations_on_nil?
          !warn_about_expectations_on_nil
        end
      end

      # @private
      def initialize(object, name=nil, options={})
        @object = object
        @name = name
        @error_generator = ErrorGenerator.new object, name, options
        @expectation_ordering = RSpec::Mocks::space.expectation_ordering
        @messages_received = []
        @options = options
        @already_proxied_respond_to = false
        @null_object = false
      end

      # @private
      def null_object?
        @null_object
      end

      # @private
      # Tells the object to ignore any messages that aren't explicitly set as
      # stubs or message expectations.
      def as_null_object
        @null_object = true
        @object
      end

      # @private
      def already_proxied_respond_to
        @already_proxied_respond_to = true
      end

      # @private
      def already_proxied_respond_to?
        @already_proxied_respond_to
      end

      # @private
      def add_message_expectation(location, method_name, opts={}, &block)        
        meth_double = method_double[method_name]

        if null_object? && !block
          meth_double.add_default_stub(@error_generator, @expectation_ordering, location, opts) do
            @object
          end
        end

        meth_double.add_expectation @error_generator, @expectation_ordering, location, opts, &block
      end

      # @private
      def add_negative_message_expectation(location, method_name, &implementation)
        method_double[method_name].add_negative_expectation @error_generator, @expectation_ordering, location, &implementation
      end

      # @private
      def add_stub(location, method_name, opts={}, &implementation)
        method_double[method_name].add_stub @error_generator, @expectation_ordering, location, opts, &implementation
      end

      # @private
      def remove_stub(method_name)
        method_double[method_name].remove_stub
      end

      # @private
      def verify
        method_doubles.each {|d| d.verify}
      ensure
        reset
      end

      # @private
      def reset
        method_doubles.each {|d| d.reset}
      end

      # @private
      def received_message?(method_name, *args, &block)
        @messages_received.any? {|array| array == [method_name, args, block]}
      end

      # @private
      def has_negative_expectation?(message)
        method_double[message].expectations.detect {|expectation| expectation.negative_expectation_for?(message)}
      end

      # @private
      def record_message_received(message, *args, &block)
        @messages_received << [message, args, block]
      end

      # @private
      def message_received(message, *args, &block)
        expectation = find_matching_expectation(message, *args)
        stub = find_matching_method_stub(message, *args)

        if (stub && expectation && expectation.called_max_times?) || (stub && !expectation)
          expectation.increase_actual_received_count! if expectation && expectation.actual_received_count_matters?
          if expectation = find_almost_matching_expectation(message, *args)
            expectation.advise(*args) unless expectation.expected_messages_received?
          end
          stub.invoke(*args, &block)
        elsif expectation
          expectation.invoke(*args, &block)
        elsif expectation = find_almost_matching_expectation(message, *args)
          expectation.advise(*args) if null_object? unless expectation.expected_messages_received?
          raise_unexpected_message_args_error(expectation, *args) unless (has_negative_expectation?(message) or null_object?)
        elsif stub = find_almost_matching_stub(message, *args)
          stub.advise(*args)
          raise_missing_default_stub_error(stub, *args)
        elsif @object.is_a?(Class)
          @object.superclass.__send__(message, *args, &block)
        else
          @object.__send__(:method_missing, message, *args, &block)
        end
      end

      # @private
      def raise_unexpected_message_error(method_name, *args)
        @error_generator.raise_unexpected_message_error method_name, *args
      end

      # @private
      def raise_unexpected_message_args_error(expectation, *args)
        @error_generator.raise_unexpected_message_args_error(expectation, *args)
      end

      # @private
      def raise_missing_default_stub_error(expectation, *args)
        @error_generator.raise_missing_default_stub_error(expectation, *args)
      end

      private

      def method_double
        @method_double ||= Hash.new {|h,k| h[k] = MethodDouble.new(@object, k, self) }
      end

      def method_doubles
        method_double.values
      end

      def find_matching_expectation(method_name, *args)
        (method_double[method_name].expectations.find {|expectation| expectation.matches?(method_name, *args) && !expectation.called_max_times?}) || 
          method_double[method_name].expectations.find {|expectation| expectation.matches?(method_name, *args)}
      end

      def find_almost_matching_expectation(method_name, *args)
        method_double[method_name].expectations.find do |expectation|
          expectation.matches_name_but_not_args(method_name, *args) && !expectation.called_max_times?
        end || method_double[method_name].expectations.find do |expectation|
          expectation.matches_name_but_not_args(method_name, *args)
        end
      end

      def find_matching_method_stub(method_name, *args)
        method_double[method_name].stubs.find {|stub| stub.matches?(method_name, *args)}
      end

      def find_almost_matching_stub(method_name, *args)
        method_double[method_name].stubs.find {|stub| stub.matches_name_but_not_args(method_name, *args)}
      end
    end
  end
end
