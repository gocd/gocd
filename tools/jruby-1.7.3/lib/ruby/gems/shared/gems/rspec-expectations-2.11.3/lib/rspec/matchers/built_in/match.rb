module RSpec
  module Matchers
    module BuiltIn
      class Match < BaseMatcher

        def match(expected, actual)
          actual.match expected
        end
      end
    end
  end
end
