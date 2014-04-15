module RSpec
  module Matchers
    module BuiltIn
      class Change
        def initialize(receiver=nil, message=nil, &block)
          @message = message
          @value_proc = block || lambda {receiver.__send__(message)}
          @expected_after = @expected_before = @minimum = @maximum = @expected_delta = nil
          @eval_before = @eval_after = false
        end

        def matches?(event_proc)
          raise_block_syntax_error if block_given?

          @actual_before = evaluate_value_proc
          event_proc.call
          @actual_after = evaluate_value_proc

          (!change_expected? || changed?) && matches_before? && matches_after? && matches_expected_delta? && matches_min? && matches_max?
        end
        alias == matches?

        def raise_block_syntax_error
          raise SyntaxError.new(<<-MESSAGE)
block passed to should or should_not change must use {} instead of do/end
MESSAGE
        end

        def evaluate_value_proc
          case val = @value_proc.call
          when Enumerable
            val.dup
          else
            val
          end
        end

        def failure_message_for_should
          if @eval_before && !expected_matches_actual?(@expected_before, @actual_before)
            "#{message} should have initially been #{@expected_before.inspect}, but was #{@actual_before.inspect}"
          elsif @eval_after && !expected_matches_actual?(@expected_after, @actual_after)
            "#{message} should have been changed to #{@expected_after.inspect}, but is now #{@actual_after.inspect}"
          elsif @expected_delta
            "#{message} should have been changed by #{@expected_delta.inspect}, but was changed by #{actual_delta.inspect}"
          elsif @minimum
            "#{message} should have been changed by at least #{@minimum.inspect}, but was changed by #{actual_delta.inspect}"
          elsif @maximum
            "#{message} should have been changed by at most #{@maximum.inspect}, but was changed by #{actual_delta.inspect}"
          else
            "#{message} should have changed, but is still #{@actual_before.inspect}"
          end
        end

        def actual_delta
          @actual_after - @actual_before
        end

        def failure_message_for_should_not
          "#{message} should not have changed, but did change from #{@actual_before.inspect} to #{@actual_after.inspect}"
        end

        def by(expected_delta)
          @expected_delta = expected_delta
          self
        end

        def by_at_least(minimum)
          @minimum = minimum
          self
        end

        def by_at_most(maximum)
          @maximum = maximum
          self
        end      

        def to(to)
          @eval_after = true
          @expected_after = to
          self
        end

        def from (before)
          @eval_before = true
          @expected_before = before
          self
        end

        def description
          "change ##{message}"
        end

        private

        def message
          @message || "result"
        end

        def change_expected?
          @expected_delta != 0
        end

        def changed?
          @actual_before != @actual_after
        end

        def matches_before?
          @eval_before ? expected_matches_actual?(@expected_before, @actual_before) : true
        end

        def matches_after?
          @eval_after ? expected_matches_actual?(@expected_after, @actual_after) : true
        end

        def matches_expected_delta?
          @expected_delta ? (@actual_before + @expected_delta == @actual_after) : true
        end

        def matches_min?
          @minimum ? (@actual_after - @actual_before >= @minimum) : true
        end

        def matches_max?
          @maximum ? (@actual_after - @actual_before <= @maximum) : true
        end

        def expected_matches_actual?(expected, actual)
          expected === actual
        end
      end
    end
  end
end
