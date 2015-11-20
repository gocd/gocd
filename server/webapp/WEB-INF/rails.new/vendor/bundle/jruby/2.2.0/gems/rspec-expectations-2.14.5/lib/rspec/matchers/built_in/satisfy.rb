module RSpec
  module Matchers
    module BuiltIn
      class Satisfy
        def initialize(&block)
          @block = block
        end

        def matches?(actual, &block)
          @block = block if block
          @actual = actual
          @block.call(actual)
        end
        alias == matches?

        def failure_message_for_should
          "expected #{@actual} to satisfy block"
        end

        def failure_message_for_should_not
          "expected #{@actual} not to satisfy block"
        end

        def description
          "satisfy block"
        end
      end
    end
  end
end
