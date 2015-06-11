module RSpec
  module Matchers
    module BuiltIn
      class YieldProbe
        def self.probe(block)
          probe = new
          assert_valid_expect_block!(block)
          block.call(probe)
          probe.assert_used!
          probe
        end

        attr_accessor :num_yields, :yielded_args

        def initialize
          @used = false
          self.num_yields, self.yielded_args = 0, []
        end

        def to_proc
          @used = true

          probe = self
          Proc.new do |*args|
            probe.num_yields += 1
            probe.yielded_args << args
          end
        end

        def single_yield_args
          yielded_args.first
        end

        def yielded_once?(matcher_name)
          case num_yields
          when 1 then true
          when 0 then false
          else
            raise "The #{matcher_name} matcher is not designed to be used with a " +
                  "method that yields multiple times. Use the yield_successive_args " +
                  "matcher for that case."
          end
        end

        def successive_yield_args
          yielded_args.map do |arg_array|
            arg_array.size == 1 ? arg_array.first : arg_array
          end
        end

        def assert_used!
          return if @used
          raise "You must pass the argument yielded to your expect block on " +
                "to the method-under-test as a block. It acts as a probe that " +
                "allows the matcher to detect whether or not the method-under-test " +
                "yields, and, if so, how many times, and what the yielded arguments " +
                "are."
        end

        def self.assert_valid_expect_block!(block)
          return if block.arity == 1
          raise "Your expect block must accept an argument to be used with this " +
                "matcher. Pass the argument as a block on to the method you are testing."
        end
      end

      class YieldControl < BaseMatcher
        def initialize
          @expectation_type = nil
          @expected_yields_count = nil
        end

        def matches?(block)
          probe = YieldProbe.probe(block)

          if @expectation_type
            probe.num_yields.send(@expectation_type, @expected_yields_count)
          else
            probe.yielded_once?(:yield_control)
          end
        end

        def once
          exactly(1)
          self
        end

        def twice
          exactly(2)
          self
        end

        def exactly(number)
          set_expected_yields_count(:==, number)
          self
        end

        def at_most(number)
          set_expected_yields_count(:<=, number)
          self
        end

        def at_least(number)
          set_expected_yields_count(:>=, number)
          self
        end

        def times
          self
        end

        def failure_message_for_should
          'expected given block to yield control'.tap do |failure_message|
            failure_message << relativity_failure_message
          end
        end

        def failure_message_for_should_not
          'expected given block not to yield control'.tap do |failure_message|
            failure_message << relativity_failure_message
          end
        end

        private

        def set_expected_yields_count(relativity, n)
          @expectation_type = relativity
          @expected_yields_count = case n
                                   when Numeric then n
                                   when :once then 1
                                   when :twice then 2
                                   end
        end

        def relativity_failure_message
          return '' unless @expected_yields_count
          " #{human_readable_expecation_type}#{human_readable_count}"
        end

        def human_readable_expecation_type
          case @expectation_type
          when :<= then 'at most '
          when :>= then 'at least '
          else ''
          end
        end

        def human_readable_count
          case @expected_yields_count
          when 1 then "once"
          when 2 then "twice"
          else "#{@expected_yields_count} times"
          end
        end
      end

      class YieldWithNoArgs < BaseMatcher

        def matches?(block)
          @probe = YieldProbe.probe(block)
          @probe.yielded_once?(:yield_with_no_args) && @probe.single_yield_args.empty?
        end

        def failure_message_for_should
          "expected given block to yield with no arguments, but #{failure_reason}"
        end

        def failure_message_for_should_not
          "expected given block not to yield with no arguments, but did"
        end

      private

        def failure_reason
          if @probe.num_yields.zero?
            "did not yield"
          else
            "yielded with arguments: #{@probe.single_yield_args.inspect}"
          end
        end
      end

      class YieldWithArgs
        def initialize(*args)
          @expected = args
        end

        def matches?(block)
          @probe = YieldProbe.probe(block)
          @actual = @probe.single_yield_args
          @probe.yielded_once?(:yield_with_args) && args_match?
        end
        alias == matches?

        def failure_message_for_should
          "expected given block to yield with arguments, but #{positive_failure_reason}"
        end

        def failure_message_for_should_not
          "expected given block not to yield with arguments, but #{negative_failure_reason}"
        end

        def description
          desc = "yield with args"
          desc << "(" + @expected.map { |e| e.inspect }.join(", ") + ")" unless @expected.empty?
          desc
        end

      private

        def positive_failure_reason
          if @probe.num_yields.zero?
            "did not yield"
          else
            @positive_args_failure
          end
        end

        def negative_failure_reason
          if all_args_match?
            "yielded with expected arguments" +
              "\nexpected not: #{@expected.inspect}" +
              "\n         got: #{@actual.inspect} (compared using === and ==)"
          else
            "did"
          end
        end

        def args_match?
          if @expected.empty? # expect {...}.to yield_with_args
            @positive_args_failure = "yielded with no arguments" if @actual.empty?
            return !@actual.empty?
          end

          unless match = all_args_match?
            @positive_args_failure = "yielded with unexpected arguments" +
              "\nexpected: #{@expected.inspect}" +
              "\n     got: #{@actual.inspect} (compared using === and ==)"
          end

          match
        end

        def all_args_match?
          return false if @expected.size != @actual.size

          @expected.zip(@actual).all? do |expected, actual|
            expected === actual || actual == expected
          end
        end
      end

      class YieldSuccessiveArgs
        def initialize(*args)
          @expected = args
        end

        def matches?(block)
          @probe = YieldProbe.probe(block)
          @actual = @probe.successive_yield_args
          args_match?
        end
        alias == matches?

        def failure_message_for_should
          "expected given block to yield successively with arguments, but yielded with unexpected arguments" +
            "\nexpected: #{@expected.inspect}" +
            "\n     got: #{@actual.inspect} (compared using === and ==)"
        end

        def failure_message_for_should_not
          "expected given block not to yield successively with arguments, but yielded with expected arguments" +
              "\nexpected not: #{@expected.inspect}" +
              "\n         got: #{@actual.inspect} (compared using === and ==)"
        end

        def description
          desc = "yield successive args"
          desc << "(" + @expected.map { |e| e.inspect }.join(", ") + ")"
          desc
        end

      private

        def args_match?
          return false if @expected.size != @actual.size

          @expected.zip(@actual).all? do |expected, actual|
            expected === actual || actual == expected
          end
        end
      end
    end
  end
end

