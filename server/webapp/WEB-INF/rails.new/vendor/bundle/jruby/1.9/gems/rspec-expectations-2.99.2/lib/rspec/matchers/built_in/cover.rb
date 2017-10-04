module RSpec
  module Matchers
    module BuiltIn
      class Cover < BaseMatcher
        def initialize(*expected)
          @expected = expected
        end

        def matches?(range)
          @actual = range
          @expected.all? { |e| range.cover?(e) }
        end

        def does_not_match?(range)
          @actual = range
          expected.none? { |e| range.cover?(e) }
        end
      end
    end
  end
end
