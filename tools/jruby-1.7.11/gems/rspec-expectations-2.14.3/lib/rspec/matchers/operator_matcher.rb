module RSpec
  module Matchers
    class OperatorMatcher
      class << self
        def registry
          @registry ||= {}
        end

        def register(klass, operator, matcher)
          registry[klass] ||= {}
          registry[klass][operator] = matcher
        end

        def unregister(klass, operator)
          registry[klass] && registry[klass].delete(operator)
        end

        def get(klass, operator)
          klass.ancestors.each { |ancestor|
            matcher = registry[ancestor] && registry[ancestor][operator]
            return matcher if matcher
          }

          nil
        end
      end

      def initialize(actual)
        @actual = actual
      end

      def self.use_custom_matcher_or_delegate(operator)
        define_method(operator) do |expected|
          if uses_generic_implementation_of?(operator) && matcher = OperatorMatcher.get(@actual.class, operator)
            @actual.__send__(::RSpec::Matchers.last_should, matcher.new(expected))
          else
            eval_match(@actual, operator, expected)
          end
        end

        negative_operator = operator.sub(/^=/, '!')
        if negative_operator != operator && respond_to?(negative_operator)
          define_method(negative_operator) do |expected|
            opposite_should = ::RSpec::Matchers.last_should == :should ? :should_not : :should
            raise "RSpec does not support `#{::RSpec::Matchers.last_should} #{negative_operator} expected`.  " +
              "Use `#{opposite_should} #{operator} expected` instead."
          end
        end
      end

      ['==', '===', '=~', '>', '>=', '<', '<='].each do |operator|
        use_custom_matcher_or_delegate operator
      end

      def fail_with_message(message)
        RSpec::Expectations.fail_with(message, @expected, @actual)
      end

      def description
        "#{@operator} #{@expected.inspect}"
      end

    private

      if Method.method_defined?(:owner) # 1.8.6 lacks Method#owner :-(
        def uses_generic_implementation_of?(op)
          Expectations.method_handle_for(@actual, op).owner == ::Kernel
        rescue NameError
          false
        end
      else
        def uses_generic_implementation_of?(op)
          # This is a bit of a hack, but:
          #
          # {}.method(:=~).to_s # => "#<Method: Hash(Kernel)#=~>"
          #
          # In the absence of Method#owner, this is the best we
          # can do to see if the method comes from Kernel.
          Expectations.method_handle_for(@actual, op).to_s.include?('(Kernel)')
        rescue NameError
          false
        end
      end

      def eval_match(actual, operator, expected)
        ::RSpec::Matchers.last_matcher = self
        @operator, @expected = operator, expected
        __delegate_operator(actual, operator, expected)
      end
    end

    module BuiltIn
      class PositiveOperatorMatcher < OperatorMatcher
        def __delegate_operator(actual, operator, expected)
          if actual.__send__(operator, expected)
            true
          elsif ['==','===', '=~'].include?(operator)
            fail_with_message("expected: #{expected.inspect}\n     got: #{actual.inspect} (using #{operator})")
          else
            fail_with_message("expected: #{operator} #{expected.inspect}\n     got: #{operator.gsub(/./, ' ')} #{actual.inspect}")
          end
        end
      end

      class NegativeOperatorMatcher < OperatorMatcher
        def __delegate_operator(actual, operator, expected)
          return false unless actual.__send__(operator, expected)
          return fail_with_message("expected not: #{operator} #{expected.inspect}\n         got: #{operator.gsub(/./, ' ')} #{actual.inspect}")
        end
      end
    end
  end
end
