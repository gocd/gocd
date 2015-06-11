module RSpec
  module Core
    class BacktraceCleaner

      DEFAULT_EXCLUSION_PATTERNS = [
        /\/lib\d*\/ruby\//,
        /org\/jruby\//,
        /bin\//,
        %r|/gems/|,
        /spec\/spec_helper\.rb/,
        /lib\/rspec\/(core|expectations|matchers|mocks)/
      ]

      attr_accessor :inclusion_patterns
      attr_accessor :exclusion_patterns

      def initialize(inclusion_patterns=nil, exclusion_patterns=DEFAULT_EXCLUSION_PATTERNS.dup)
        @exclusion_patterns = exclusion_patterns

        if inclusion_patterns.nil?
          @inclusion_patterns = (matches_an_exclusion_pattern? Dir.getwd) ? [Regexp.new(Dir.getwd)] : []
        else
          @inclusion_patterns = inclusion_patterns
        end
      end

      def exclude?(line)
        @inclusion_patterns.none? {|p| line =~ p} and matches_an_exclusion_pattern?(line)
      end

      def full_backtrace=(true_or_false)
        @exclusion_patterns = true_or_false ? [] : DEFAULT_EXCLUSION_PATTERNS.dup
      end

      def full_backtrace?
        @exclusion_patterns.empty?
      end

      private

      def matches_an_exclusion_pattern?(line)
        @exclusion_patterns.any? {|p| line =~ p}
      end
    end
  end
end
