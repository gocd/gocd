module RSpec
  module Mocks
    # @private
    class ErrorGenerator
      attr_writer :opts

      def initialize(target, name, options={})
        @declared_as = options[:__declared_as] || 'Mock'
        @target = target
        @name = name
      end

      # @private
      def opts
        @opts ||= {}
      end

      # @private
      def raise_unexpected_message_error(message, *args)
        __raise "#{intro} received unexpected message :#{message}#{arg_message(*args)}"
      end

      # @private
      def raise_unexpected_message_args_error(expectation, *args)
        expected_args = format_args(*expectation.expected_args)
        actual_args = format_received_args(*args)
        __raise "#{intro} received #{expectation.message.inspect} with unexpected arguments\n  expected: #{expected_args}\n       got: #{actual_args}"
      end

      # @private
      def raise_missing_default_stub_error(expectation, *args)
        expected_args = format_args(*expectation.expected_args)
        actual_args = format_received_args(*args)
        __raise "#{intro} received #{expectation.message.inspect} with unexpected arguments\n  expected: #{expected_args}\n       got: #{actual_args}\n Please stub a default value first if message might be received with other args as well. \n"
      end

      # @private
      def raise_similar_message_args_error(expectation, *args_for_multiple_calls)
        expected_args = format_args(*expectation.expected_args)
        actual_args = args_for_multiple_calls.collect {|a| format_received_args(*a)}.join(", ")
        __raise "#{intro} received #{expectation.message.inspect} with unexpected arguments\n  expected: #{expected_args}\n       got: #{actual_args}"
      end

      # @private
      def raise_expectation_error(message, expected_received_count, argument_list_matcher, actual_received_count, expectation_count_type, *args)
        expected_part = expected_part_of_expectation_error(expected_received_count, expectation_count_type, argument_list_matcher)
        received_part = received_part_of_expectation_error(actual_received_count, *args)
        __raise "(#{intro}).#{message}#{format_args(*args)}\n    #{expected_part}\n    #{received_part}"
      end

      # @private
      def received_part_of_expectation_error(actual_received_count, *args)
        "received: #{count_message(actual_received_count)}" +
          method_call_args_description(args)
      end

      # @private
      def expected_part_of_expectation_error(expected_received_count, expectation_count_type, argument_list_matcher)
        "expected: #{count_message(expected_received_count, expectation_count_type)}" +
          method_call_args_description(argument_list_matcher.expected_args)
      end

      # @private
      def method_call_args_description(args)
        if args.first.is_a?(ArgumentMatchers::AnyArgsMatcher)
          " with any arguments"
        elsif args.first.is_a?(ArgumentMatchers::NoArgsMatcher)
          " with no arguments"
        elsif args.length > 0
          " with arguments: #{args.inspect.gsub(/\A\[(.+)\]\z/, '(\1)')}"
        else
          ""
        end
      end

      # @private
      def describe_expectation(message, expected_received_count, actual_received_count, *args)
        "have received #{message}#{format_args(*args)} #{count_message(expected_received_count)}"
      end

      # @private
      def raise_out_of_order_error(message)
        __raise "#{intro} received :#{message} out of order"
      end

      # @private
      def raise_block_failed_error(message, detail)
        __raise "#{intro} received :#{message} but passed block failed with: #{detail}"
      end

      # @private
      def raise_missing_block_error(args_to_yield)
        __raise "#{intro} asked to yield |#{arg_list(*args_to_yield)}| but no block was passed"
      end

      # @private
      def raise_wrong_arity_error(args_to_yield, arity)
        __raise "#{intro} yielded |#{arg_list(*args_to_yield)}| to block with arity of #{arity}"
      end

      # @private
      def raise_only_valid_on_a_partial_mock(method)
        __raise "#{intro} is a pure mock object. `#{method}` is only " +
                "available on a partial mock object."
      end

      # @private
      def raise_expectation_on_unstubbed_method(method)
        __raise "#{intro} expected to have received #{method}, but that " +
                "method has not been stubbed."
      end

      # @private
      def raise_expectation_on_mocked_method(method)
        __raise "#{intro} expected to have received #{method}, but that " +
                "method has been mocked instead of stubbed."
      end

      def self.raise_double_negation_error(wrapped_expression)
        raise "Isn't life confusing enough? You've already set a " +
              "negative message expectation and now you are trying to " +
              "negate it again with `never`. What does an expression like " +
              "`#{wrapped_expression}.not_to receive(:msg).never` even mean?"
      end

      private

      def intro
        if @name
          "#{@declared_as} #{@name.inspect}"
        elsif TestDouble === @target
          @declared_as
        elsif Class === @target
          "<#{@target.inspect} (class)>"
        elsif @target
          @target
        else
          "nil"
        end
      end

      def __raise(message)
        message = opts[:message] unless opts[:message].nil?
        Kernel::raise(RSpec::Mocks::MockExpectationError, message)
      end

      def arg_message(*args)
        " with " + format_args(*args)
      end

      def format_args(*args)
        args.empty? ? "(no args)" : "(" + arg_list(*args) + ")"
      end

      def arg_list(*args)
        args.collect {|arg| arg.respond_to?(:description) ? arg.description : arg.inspect}.join(", ")
      end

      def format_received_args(*args)
        args.empty? ? "(no args)" : "(" + received_arg_list(*args) + ")"
      end

      def received_arg_list(*args)
        args.collect(&:inspect).join(", ")
      end

      def count_message(count, expectation_count_type=nil)
        return "at least #{pretty_print(count.abs)}" if count < 0 || expectation_count_type == :at_least
        return "at most #{pretty_print(count)}" if expectation_count_type == :at_most
        return pretty_print(count)
      end

      def pretty_print(count)
        "#{count} time#{count == 1 ? '' : 's'}"
      end

    end
  end
end
