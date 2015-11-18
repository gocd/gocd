module RSpec
  module Matchers
    module BuiltIn
      class BeAKindOf < BaseMatcher
        def match(expected, actual)
          actual.kind_of? expected
        end
      end
    end
  end
end
