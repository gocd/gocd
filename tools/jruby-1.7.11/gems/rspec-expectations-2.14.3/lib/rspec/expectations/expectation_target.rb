module RSpec
  module Expectations
    # Wraps the target of an expectation.
    # @example
    #   expect(something) # => ExpectationTarget wrapping something
    #
    #   # used with `to`
    #   expect(actual).to eq(3)
    #
    #   # with `not_to`
    #   expect(actual).not_to eq(3)
    class ExpectationTarget
      class << self
        attr_accessor :deprecated_should_enabled
        alias deprecated_should_enabled? deprecated_should_enabled
      end

      # @api private
      def initialize(target)
        @target = target
      end

      # Runs the given expectation, passing if `matcher` returns true.
      # @example
      #   expect(value).to eq(5)
      #   expect { perform }.to raise_error
      # @param [Matcher]
      #   matcher
      # @param [String] message optional message to display when the expectation fails
      # @return [Boolean] true if the expectation succeeds (else raises)
      # @see RSpec::Matchers
      def to(matcher=nil, message=nil, &block)
        prevent_operator_matchers(:to, matcher)
        RSpec::Expectations::PositiveExpectationHandler.handle_matcher(@target, matcher, message, &block)
      end

      # Runs the given expectation, passing if `matcher` returns false.
      # @example
      #   expect(value).not_to eq(5)
      # @param [Matcher]
      #   matcher
      # @param [String] message optional message to display when the expectation fails
      # @return [Boolean] false if the negative expectation succeeds (else raises)
      # @see RSpec::Matchers
      def not_to(matcher=nil, message=nil, &block)
        prevent_operator_matchers(:not_to, matcher)
        RSpec::Expectations::NegativeExpectationHandler.handle_matcher(@target, matcher, message, &block)
      end
      alias to_not not_to

      def self.enable_deprecated_should
        return if deprecated_should_enabled?

        def should(*args)
          RSpec.deprecate "`expect { }.should`", :replacement => "`expect { }.to`"
          @target.should(*args)
        end

        def should_not(*args)
          RSpec.deprecate "`expect { }.should_not`", :replacement => "`expect { }.not_to`"
          @target.should_not(*args)
        end

        self.deprecated_should_enabled = true
      end

      def self.disable_deprecated_should
        return unless deprecated_should_enabled?

        remove_method :should
        remove_method :should_not

        self.deprecated_should_enabled = false
      end

    private

      def prevent_operator_matchers(verb, matcher)
        return if matcher

        raise ArgumentError, "The expect syntax does not support operator matchers, " +
                             "so you must pass a matcher to `##{verb}`."
      end
    end
  end
end

