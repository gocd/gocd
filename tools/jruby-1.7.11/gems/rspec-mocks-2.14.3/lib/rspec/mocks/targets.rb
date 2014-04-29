module RSpec
  module Mocks
    UnsupportedMatcherError  = Class.new(StandardError)
    NegationUnsupportedError = Class.new(StandardError)

    class TargetBase
      def initialize(target)
        @target = target
      end

      def self.delegate_to(matcher_method, options = {})
        method_name = options.fetch(:from) { :to }
        class_eval(<<-RUBY)
        def #{method_name}(matcher, &block)
          unless Matchers::Receive === matcher
            raise UnsupportedMatcherError, "only the `receive` matcher is supported " +
              "with `\#{expression}(...).\#{#{method_name.inspect}}`, but you have provided: \#{matcher}"
          end

          matcher.__send__(#{matcher_method.inspect}, @target, &block)
        end
        RUBY
      end

      def self.disallow_negation(method)
        define_method method do |*args|
          raise NegationUnsupportedError,
            "`#{expression}(...).#{method} receive` is not supported since it " +
            "doesn't really make sense. What would it even mean?"
        end
      end

    private

      def expression
        self.class::EXPRESSION
      end
    end

    class AllowanceTarget < TargetBase
      EXPRESSION = :allow
      delegate_to :setup_allowance
      disallow_negation :not_to
      disallow_negation :to_not
    end

    class ExpectationTarget < TargetBase
      EXPRESSION = :expect
      delegate_to :setup_expectation
      delegate_to :setup_negative_expectation, :from => :not_to
      delegate_to :setup_negative_expectation, :from => :to_not
    end

    class AnyInstanceAllowanceTarget < TargetBase
      EXPRESSION = :expect_any_instance_of
      delegate_to :setup_any_instance_allowance
      disallow_negation :not_to
      disallow_negation :to_not
    end

    class AnyInstanceExpectationTarget < TargetBase
      EXPRESSION = :expect_any_instance_of
      delegate_to :setup_any_instance_expectation
      delegate_to :setup_any_instance_negative_expectation, :from => :not_to
      delegate_to :setup_any_instance_negative_expectation, :from => :to_not
    end
  end
end

