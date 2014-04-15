module RSpec
  module Matchers
    module BuiltIn
      class BeWithin
        def initialize(delta)
          @delta = delta
        end

        def matches?(actual)
          @actual = actual
          raise needs_expected     unless defined? @expected 
          raise needs_subtractable unless @actual.respond_to? :-
          (@actual - @expected).abs <= @delta
        end
        alias == matches?

        def of(expected)
          @expected = expected
          self
        end

        def failure_message_for_should
          "expected #{@actual} to #{description}"
        end

        def failure_message_for_should_not
          "expected #{@actual} not to #{description}"
        end

        def description
          "be within #{@delta} of #{@expected}"
        end

        private

        def needs_subtractable
          ArgumentError.new "The actual value (#{@actual.inspect}) must respond to `-`"
        end

        def needs_expected
          ArgumentError.new "You must set an expected value using #of: be_within(#{@delta}).of(expected_value)"
        end
      end
    end
  end
end
