module RSpec
  module Matchers
    module MatchAliases
      def ==(other)
        return true if equal?(other)

        matched = matches?(other)

        if matched
          RSpec.deprecate("Using `matcher == value` as an alias for `#matches?`", :replacement => "`matcher === value`")
        end

        matched
      end

      def ===(other)
        matches?(other)
      end
    end
  end
end

