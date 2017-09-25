module RSpec
  module Matchers
    module BuiltIn
      class BeAnInstanceOf < BaseMatcher
        def match(expected, actual)
          actual.instance_of? expected
        end

        def description
          "be an instance of #{expected}"
        end
      end
    end
  end
end
