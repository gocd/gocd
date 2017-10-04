module RSpec
  module Expectations
    # Wraps the target of an expectation.
    #
    # @example
    #   expect(something)       # => ExpectationTarget wrapping something
    #   expect { do_something } # => ExpectationTarget wrapping the block
    #
    #   # used with `to`
    #   expect(actual).to eq(3)
    #
    #   # with `not_to`
    #   expect(actual).not_to eq(3)
    #
    # @note `ExpectationTarget` is not intended to be instantiated
    #   directly by users. Use `expect` instead.
    class ExpectationTarget
      class << self
        attr_accessor :deprecated_should_enabled
        alias deprecated_should_enabled? deprecated_should_enabled
      end

      # @private
      # Used as a sentinel value to be able to tell when the user
      # did not pass an argument. We can't use `nil` for that because
      # `nil` is a valid value to pass.
      UndefinedValue = Module.new

      # @api private
      def initialize(value)
        @target = value
      end

      # @private
      def self.for(value, block)
        if UndefinedValue.equal?(value)
          unless block
            raise ArgumentError, "You must pass either an argument or a block to `expect`."
          end
          BlockExpectationTarget.new(block)
        elsif block
          raise ArgumentError, "You cannot pass both an argument and a block to `expect`."
        else
          new(value)
        end
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
        prevent_operator_matchers(:to) unless matcher
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
        prevent_operator_matchers(:not_to) unless matcher
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

      def prevent_operator_matchers(verb)
        raise ArgumentError, "The expect syntax does not support operator matchers, " +
                             "so you must pass a matcher to `##{verb}`."
      end
    end

    # @private
    # Validates the provided matcher to ensure it supports block
    # expectations, in order to avoid user confusion when they
    # use a block thinking the expectation will be on the return
    # value of the block rather than the block itself.
    class BlockExpectationTarget < ExpectationTarget
      def to(matcher, message=nil, &block)
        enforce_block_expectation(matcher)
        super
      end

      def not_to(matcher, message=nil, &block)
        enforce_block_expectation(matcher)
        super
      end
      alias to_not not_to

    private

      def enforce_block_expectation(matcher)
        return if supports_block_expectations?(matcher)

        RSpec.deprecate("Using a matcher in a block expectation expression " +
                        "(e.g. `expect { }.to matcher`) that does not implement " +
                        "`supports_block_expectations?`",
                        :replacement => "a value expectation expression " +
                        "(e.g. `expect(value).to matcher`) or implement " +
                        "`supports_block_expectations?` on the provided matcher " +
                        "(#{description_of matcher})")
      end

      def supports_block_expectations?(matcher)
        matcher.supports_block_expectations?
      rescue NoMethodError
        false
      end

      def description_of(matcher)
        matcher.description
      rescue NoMethodError
        matcher.inspect
      end
    end
  end
end

