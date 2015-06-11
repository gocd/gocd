module RSpec
  module Matchers
    module BuiltIn
      class Eql < BaseMatcher
        def match(expected, actual)
          actual.eql? expected
        end

        def failure_message_for_should
          "\nexpected: #{expected.inspect}\n     got: #{actual.inspect}\n\n(compared using eql?)\n"
        end

        def failure_message_for_should_not
          "\nexpected: value != #{expected.inspect}\n     got: #{actual.inspect}\n\n(compared using eql?)\n"
        end

        def diffable?
          true
        end
      end
    end
  end
end
