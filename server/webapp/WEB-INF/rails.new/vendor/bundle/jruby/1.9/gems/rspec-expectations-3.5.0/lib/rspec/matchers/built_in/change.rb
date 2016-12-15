module RSpec
  module Matchers
    module BuiltIn
      # @api private
      # Provides the implementation for `change`.
      # Not intended to be instantiated directly.
      class Change < BaseMatcher
        # @api public
        # Specifies the delta of the expected change.
        def by(expected_delta)
          ChangeRelatively.new(@change_details, expected_delta, :by) do |actual_delta|
            values_match?(expected_delta, actual_delta)
          end
        end

        # @api public
        # Specifies a minimum delta of the expected change.
        def by_at_least(minimum)
          ChangeRelatively.new(@change_details, minimum, :by_at_least) do |actual_delta|
            actual_delta >= minimum
          end
        end

        # @api public
        # Specifies a maximum delta of the expected change.
        def by_at_most(maximum)
          ChangeRelatively.new(@change_details, maximum, :by_at_most) do |actual_delta|
            actual_delta <= maximum
          end
        end

        # @api public
        # Specifies the new value you expect.
        def to(value)
          ChangeToValue.new(@change_details, value)
        end

        # @api public
        # Specifies the original value.
        def from(value)
          ChangeFromValue.new(@change_details, value)
        end

        # @private
        def matches?(event_proc)
          @event_proc = event_proc
          return false unless Proc === event_proc
          raise_block_syntax_error if block_given?
          @change_details.perform_change(event_proc)
          @change_details.changed?
        end

        def does_not_match?(event_proc)
          raise_block_syntax_error if block_given?
          !matches?(event_proc) && Proc === event_proc
        end

        # @api private
        # @return [String]
        def failure_message
          "expected #{@change_details.message} to have changed, " \
          "but #{positive_failure_reason}"
        end

        # @api private
        # @return [String]
        def failure_message_when_negated
          "expected #{@change_details.message} not to have changed, " \
          "but #{negative_failure_reason}"
        end

        # @api private
        # @return [String]
        def description
          "change #{@change_details.message}"
        end

        # @private
        def supports_block_expectations?
          true
        end

      private

        def initialize(receiver=nil, message=nil, &block)
          @change_details = ChangeDetails.new(receiver, message, &block)
        end

        def raise_block_syntax_error
          raise SyntaxError, "Block not received by the `change` matcher. " \
          "Perhaps you want to use `{ ... }` instead of do/end?"
        end

        def positive_failure_reason
          return "was not given a block" unless Proc === @event_proc
          "is still #{description_of @change_details.actual_before}"
        end

        def negative_failure_reason
          return "was not given a block" unless Proc === @event_proc
          "did change from #{description_of @change_details.actual_before} " \
          "to #{description_of @change_details.actual_after}"
        end
      end

      # Used to specify a relative change.
      # @api private
      class ChangeRelatively < BaseMatcher
        def initialize(change_details, expected_delta, relativity, &comparer)
          @change_details = change_details
          @expected_delta = expected_delta
          @relativity     = relativity
          @comparer       = comparer
        end

        # @private
        def failure_message
          "expected #{@change_details.message} to have changed " \
          "#{@relativity.to_s.tr('_', ' ')} " \
          "#{description_of @expected_delta}, but #{failure_reason}"
        end

        # @private
        def matches?(event_proc)
          @event_proc = event_proc
          return false unless Proc === event_proc
          @change_details.perform_change(event_proc)
          @comparer.call(@change_details.actual_delta)
        end

        # @private
        def does_not_match?(_event_proc)
          raise NotImplementedError, "`expect { }.not_to change " \
            "{ }.#{@relativity}()` is not supported"
        end

        # @private
        def description
          "change #{@change_details.message} " \
          "#{@relativity.to_s.tr('_', ' ')} #{description_of @expected_delta}"
        end

        # @private
        def supports_block_expectations?
          true
        end

      private

        def failure_reason
          return "was not given a block" unless Proc === @event_proc
          "was changed by #{description_of @change_details.actual_delta}"
        end
      end

      # @api private
      # Base class for specifying a change from and/or to specific values.
      class SpecificValuesChange < BaseMatcher
        # @private
        MATCH_ANYTHING = ::Object.ancestors.last

        def initialize(change_details, from, to)
          @change_details  = change_details
          @expected_before = from
          @expected_after  = to
        end

        # @private
        def matches?(event_proc)
          @event_proc = event_proc
          return false unless Proc === event_proc
          @change_details.perform_change(event_proc)
          @change_details.changed? && matches_before? && matches_after?
        end

        # @private
        def description
          "change #{@change_details.message} #{change_description}"
        end

        # @private
        def failure_message
          return not_given_a_block_failure unless Proc === @event_proc
          return before_value_failure      unless matches_before?
          return did_not_change_failure    unless @change_details.changed?
          after_value_failure
        end

        # @private
        def supports_block_expectations?
          true
        end

      private

        def matches_before?
          values_match?(@expected_before, @change_details.actual_before)
        end

        def matches_after?
          values_match?(@expected_after, @change_details.actual_after)
        end

        def before_value_failure
          "expected #{@change_details.message} " \
          "to have initially been #{description_of @expected_before}, " \
          "but was #{description_of @change_details.actual_before}"
        end

        def after_value_failure
          "expected #{@change_details.message} " \
          "to have changed to #{description_of @expected_after}, " \
          "but is now #{description_of @change_details.actual_after}"
        end

        def did_not_change_failure
          "expected #{@change_details.message} " \
          "to have changed #{change_description}, but did not change"
        end

        def did_change_failure
          "expected #{@change_details.message} not to have changed, but " \
          "did change from #{description_of @change_details.actual_before} " \
          "to #{description_of @change_details.actual_after}"
        end

        def not_given_a_block_failure
          "expected #{@change_details.message} to have changed " \
          "#{change_description}, but was not given a block"
        end
      end

      # @api private
      # Used to specify a change from a specific value
      # (and, optionally, to a specific value).
      class ChangeFromValue < SpecificValuesChange
        def initialize(change_details, expected_before)
          @description_suffix = nil
          super(change_details, expected_before, MATCH_ANYTHING)
        end

        # @api public
        # Specifies the new value you expect.
        def to(value)
          @expected_after     = value
          @description_suffix = " to #{description_of value}"
          self
        end

        # @private
        def does_not_match?(event_proc)
          if @description_suffix
            raise NotImplementedError, "`expect { }.not_to change { }.to()` " \
              "is not supported"
          end

          @event_proc = event_proc
          return false unless Proc === event_proc
          @change_details.perform_change(event_proc)
          !@change_details.changed? && matches_before?
        end

        # @private
        def failure_message_when_negated
          return not_given_a_block_failure unless Proc === @event_proc
          return before_value_failure unless matches_before?
          did_change_failure
        end

      private

        def change_description
          "from #{description_of @expected_before}#{@description_suffix}"
        end
      end

      # @api private
      # Used to specify a change to a specific value
      # (and, optionally, from a specific value).
      class ChangeToValue < SpecificValuesChange
        def initialize(change_details, expected_after)
          @description_suffix = nil
          super(change_details, MATCH_ANYTHING, expected_after)
        end

        # @api public
        # Specifies the original value.
        def from(value)
          @expected_before    = value
          @description_suffix = " from #{description_of value}"
          self
        end

        # @private
        def does_not_match?(_event_proc)
          raise NotImplementedError, "`expect { }.not_to change { }.to()` " \
            "is not supported"
        end

      private

        def change_description
          "to #{description_of @expected_after}#{@description_suffix}"
        end
      end

      # @private
      # Encapsulates the details of the before/after values.
      class ChangeDetails
        attr_reader :message, :actual_before, :actual_after

        def initialize(receiver=nil, message=nil, &block)
          if receiver && !message
            raise(
              ArgumentError,
              "`change` requires either an object and message " \
              "(`change(obj, :msg)`) or a block (`change { }`). " \
              "You passed an object but no message."
            )
          end
          @message    = message ? "##{message}" : "result"
          @value_proc = block || lambda { receiver.__send__(message) }
        end

        def perform_change(event_proc)
          @actual_before = evaluate_value_proc
          event_proc.call
          @actual_after = evaluate_value_proc
        end

        def changed?
          @actual_before != @actual_after
        end

        def actual_delta
          @actual_after - @actual_before
        end

      private

        def evaluate_value_proc
          case val = @value_proc.call
          when IO # enumerable, but we don't want to dup it.
            val
          when Enumerable, String
            val.dup
          else
            val
          end
        end
      end
    end
  end
end
