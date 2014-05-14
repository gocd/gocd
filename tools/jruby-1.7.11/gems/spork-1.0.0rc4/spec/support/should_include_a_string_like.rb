module RSpec
  module Matchers
    class IncludeAStringLike
      def initialize(substring_or_regex)
        case substring_or_regex
        when String
          @regex = Regexp.new(Regexp.escape(substring_or_regex))
        when Regexp
          @regex = substring_or_regex
        else
          raise ArgumentError, "don't know what to do with the #{substring_or_regex.class} you provided"
        end
      end

      def matches?(list_of_strings)
        @list_of_strings = list_of_strings
        @list_of_strings.any? { |s| s =~ @regex }
      end
      def failure_message
        "#{@list_of_strings.inspect} expected to include a string like #{@regex.inspect}"
      end
      def negative_failure_message
        "#{@list_of_strings.inspect} expected to not include a string like #{@regex.inspect}, but did"
      end
    end

    def include_a_string_like(substring_or_regex)
      IncludeAStringLike.new(substring_or_regex)
    end
  end
end
