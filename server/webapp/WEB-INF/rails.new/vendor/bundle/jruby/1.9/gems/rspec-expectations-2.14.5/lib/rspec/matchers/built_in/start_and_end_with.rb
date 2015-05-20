module RSpec
  module Matchers
    module BuiltIn
      class StartAndEndWith < BaseMatcher
        def initialize(*expected)
          @expected = expected.length == 1 ? expected.first : expected
        end

        def matches?(actual)
          @actual = actual.respond_to?(:[]) ? actual : (raise ArgumentError.new("#{actual.inspect} does not respond to :[]"))
          begin
            @expected.respond_to?(:length) ? subset_matches?(@expected, @actual) : element_matches?(@expected, @actual)
          rescue ArgumentError
            raise ArgumentError.new("#{actual.inspect} does not have ordered elements")
          end
        end

        def failure_message_for_should
          "expected #{@actual.inspect} to #{self.class.name.split('::').last.sub(/With/,'').downcase} with #{@expected.inspect}"
        end

        def failure_message_for_should_not
          "expected #{@actual.inspect} not to #{self.class.name.split('::').last.sub(/With/,'').downcase} with #{@expected.inspect}"
        end
      end

      class StartWith < StartAndEndWith
        def subset_matches?(expected, actual)
          actual[0, expected.length] == expected
        end

        def element_matches?(expected, actual)
          @actual[0] == @expected
        end
      end

      class EndWith < StartAndEndWith
        def subset_matches?(expected, actual)
          actual[-expected.length, expected.length] == expected
        end

        def element_matches?(expected, actual)
          actual[-1] == expected
        end
      end
    end
  end
end
