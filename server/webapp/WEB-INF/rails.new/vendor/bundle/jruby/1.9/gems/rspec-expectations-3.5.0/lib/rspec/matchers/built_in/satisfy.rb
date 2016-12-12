module RSpec
  module Matchers
    module BuiltIn
      # @api private
      # Provides the implementation for `satisfy`.
      # Not intended to be instantiated directly.
      class Satisfy < BaseMatcher
        # @private
        attr_reader :description

        def initialize(description="satisfy block", &block)
          @description = description
          @block = block
        end

        # @private
        def matches?(actual, &block)
          @block = block if block
          @actual = actual
          @block.call(actual)
        end

        # @api private
        # @return [String]
        def failure_message
          "expected #{actual_formatted} to #{description}"
        end

        # @api private
        # @return [String]
        def failure_message_when_negated
          "expected #{actual_formatted} not to #{description}"
        end
      end
    end
  end
end
