module RSpec
  module Matchers
    module BuiltIn
      class Satisfy
        include MatchAliases

        def initialize(&block)
          @block = block
        end

        def matches?(actual, &block)
          @block = block if block
          @actual = actual
          @block.call(actual)
        end

        def failure_message_for_should
          "expected #{@actual} to satisfy block"
        end

        def failure_message_for_should_not
          "expected #{@actual} not to satisfy block"
        end

        def description
          "satisfy block"
        end

        # @private
        def supports_block_expectations?
          false
        end
      end
    end
  end
end
