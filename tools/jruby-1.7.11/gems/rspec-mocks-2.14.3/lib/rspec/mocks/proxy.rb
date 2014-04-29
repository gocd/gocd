module RSpec
  module Mocks
    # @private
    class Proxy

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
      def build_expectation(method_name)
        meth_double = method_double[method_name]

        meth_double.build_expectation(
          @error_generator,
          @expectation_ordering
        )
      end

      # @private
      def replay_received_message_on(expectation)
        expected_method_name = expectation.message
        meth_double = method_double[expected_method_name]

        if meth_double.expectations.any?
          @error_generator.raise_expectation_on_mocked_method(expected_method_name)
        end

        unless null_object? || meth_double.stubs.any?
          @error_generator.raise_expectation_on_unstubbed_method(expected_method_name)
        end

        @messages_received.each do |(actual_method_name, args, _)|
          if expectation.matches?(actual_method_name, *args)
            expectation.invoke(nil)
          end
        end
      end

      # @private
      def check_for_unexpected_arguments(expectation)
        @messages_received.each do |(method_name, args, _)|
          if expectation.matches_name_but_not_args(method_name, *args)
            raise_unexpected_message_args_error(expectation, *args)
          end
        end
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
        @messages_received.clear
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
        record_message_received message, *args, &block
        expectation = find_matching_expectation(message, *args)
        stub = find_matching_method_stub(message, *args)

        if (stub && expectation && expectation.called_max_times?) || (stub && !expectation)
          expectation.increase_actual_received_count! if expectation && expectation.actual_received_count_matters?
          if expectation = find_almost_matching_expectation(message, *args)
            expectation.advise(*args) unless expectation.expected_messages_received?
          end
          stub.invoke(nil, *args, &block)
        elsif expectation
          expectation.invoke(stub, *args, &block)
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
        find_best_matching_expectation_for(method_name) do |expectation|
          expectation.matches?(method_name, *args)
        end
      end

      def find_almost_matching_expectation(method_name, *args)
        find_best_matching_expectation_for(method_name) do |expectation|
          expectation.matches_name_but_not_args(method_name, *args)
        end
      end

      def find_best_matching_expectation_for(method_name)
        first_match = nil

        method_double[method_name].expectations.each do |expectation|
          next unless yield expectation
          return expectation unless expectation.called_max_times?
          first_match ||= expectation
        end

        first_match
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
