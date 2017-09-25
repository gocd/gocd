module RSpec
  module Expectations
    # @api private
    # Provides methods for enabling and disabling the available
    # syntaxes provided by rspec-expectations.
    module Syntax
      extend self

      # @method should
      # Passes if `matcher` returns true.  Available on every `Object`.
      # @example
      #   actual.should eq expected
      #   actual.should match /expression/
      # @param [Matcher]
      #   matcher
      # @param [String] message optional message to display when the expectation fails
      # @return [Boolean] true if the expectation succeeds (else raises)
      # @see RSpec::Matchers

      # @method should_not
      # Passes if `matcher` returns false.  Available on every `Object`.
      # @example
      #   actual.should_not eq expected
      # @param [Matcher]
      #   matcher
      # @param [String] message optional message to display when the expectation fails
      # @return [Boolean] false if the negative expectation succeeds (else raises)
      # @see RSpec::Matchers

      # @method expect
      # Supports `expect(actual).to matcher` syntax by wrapping `actual` in an
      # `ExpectationTarget`.
      # @example
      #   expect(actual).to eq(expected)
      #   expect(actual).not_to eq(expected)
      # @return [ExpectationTarget]
      # @see ExpectationTarget#to
      # @see ExpectationTarget#not_to

      # @api private
      # Determines where we add `should` and `should_not`.
      def default_should_host
        @default_should_host ||= ::Object.ancestors.last
      end

      # @api private
      # Enables the `should` syntax.
      def enable_should(syntax_host = default_should_host)
        return if should_enabled?(syntax_host)

        syntax_host.module_eval do
          def should(matcher=nil, message=nil, &block)
            ::RSpec::Expectations::PositiveExpectationHandler.handle_matcher(self, matcher, message, &block)
          end

          def should_not(matcher=nil, message=nil, &block)
            ::RSpec::Expectations::NegativeExpectationHandler.handle_matcher(self, matcher, message, &block)
          end
        end

        ::RSpec::Expectations::ExpectationTarget.enable_deprecated_should if expect_enabled?
      end

      # @api private
      # Disables the `should` syntax.
      def disable_should(syntax_host = default_should_host)
        return unless should_enabled?(syntax_host)

        syntax_host.module_eval do
          undef should
          undef should_not
        end

        ::RSpec::Expectations::ExpectationTarget.disable_deprecated_should
      end

      # @api private
      # Enables the `expect` syntax.
      def enable_expect(syntax_host = ::RSpec::Matchers)
        return if expect_enabled?(syntax_host)

        syntax_host.module_eval do
          def expect(*target, &target_block)
            target << target_block if block_given?
            raise ArgumentError.new("You must pass an argument or a block to #expect but not both.") unless target.size == 1
            ::RSpec::Expectations::ExpectationTarget.new(target.first)
          end
        end

        ::RSpec::Expectations::ExpectationTarget.enable_deprecated_should if should_enabled?
      end

      # @api private
      # Disables the `expect` syntax.
      def disable_expect(syntax_host = ::RSpec::Matchers)
        return unless expect_enabled?(syntax_host)

        syntax_host.module_eval do
          undef expect
        end

        ::RSpec::Expectations::ExpectationTarget.disable_deprecated_should
      end

      # @api private
      # Indicates whether or not the `should` syntax is enabled.
      def should_enabled?(syntax_host = default_should_host)
        syntax_host.method_defined?(:should)
      end

      # @api private
      # Indicates whether or not the `expect` syntax is enabled.
      def expect_enabled?(syntax_host = ::RSpec::Matchers)
        syntax_host.method_defined?(:expect)
      end

      # @api private
      # Generates a positive expectation expression.
      def positive_expression(target_expression, matcher_expression)
        expression_generator.positive_expression(target_expression, matcher_expression)
      end

      # @api private
      # Generates a negative expectation expression.
      def negative_expression(target_expression, matcher_expression)
        expression_generator.negative_expression(target_expression, matcher_expression)
      end

      # @api private
      # Selects which expression generator to use based on the configured syntax.
      def expression_generator
        if expect_enabled?
          ExpectExpressionGenerator
        else
          ShouldExpressionGenerator
        end
      end

      # @api private
      # Generates expectation expressions for the `should` syntax.
      module ShouldExpressionGenerator
        def self.positive_expression(target_expression, matcher_expression)
          "#{target_expression}.should #{matcher_expression}"
        end

        def self.negative_expression(target_expression, matcher_expression)
          "#{target_expression}.should_not #{matcher_expression}"
        end
      end

      # @api private
      # Generates expectation expressions for the `expect` syntax.
      module ExpectExpressionGenerator
        def self.positive_expression(target_expression, matcher_expression)
          "expect(#{target_expression}).to #{matcher_expression}"
        end

        def self.negative_expression(target_expression, matcher_expression)
          "expect(#{target_expression}).not_to #{matcher_expression}"
        end
      end
    end
  end
end
