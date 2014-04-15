module RSpec
  module Matchers
    module BuiltIn
      class Include < BaseMatcher
        def initialize(*expected)
          @expected = expected
        end

        def matches?(actual)
          @actual = actual
          perform_match(:all?, :all?, @actual, @expected)
        end

        def does_not_match?(actual)
          @actual = actual
          perform_match(:none?, :any?, @actual, @expected)
        end

        def description
          "include#{expected_to_sentence}"
        end

        def diffable?
          true
        end

        private

        def perform_match(predicate, hash_predicate, actuals, expecteds)
          expecteds.send(predicate) do |expected|
            if comparing_hash_values?(actuals, expected)
              expected.send(hash_predicate) {|k,v| actuals[k] == v}
            elsif comparing_hash_keys?(actuals, expected)
              actuals.has_key?(expected)
            else
              actuals.include?(expected)
            end
          end
        end

        def comparing_hash_keys?(actual, expected)
          actual.is_a?(Hash) && !expected.is_a?(Hash)
        end

        def comparing_hash_values?(actual, expected)
          actual.is_a?(Hash) && expected.is_a?(Hash)
        end
      end
    end
  end
end
