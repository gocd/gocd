module RSpec
  module Mocks
    module Matchers
      class HaveReceived
        COUNT_CONSTRAINTS = %w(exactly at_least at_most times once twice)
        ARGS_CONSTRAINTS = %w(with)
        CONSTRAINTS = COUNT_CONSTRAINTS + ARGS_CONSTRAINTS

        def initialize(method_name)
          @method_name = method_name
          @constraints = []
          @subject = nil
        end

        def matches?(subject)
          @subject = subject
          @expectation = expect
          expected_messages_received?
        end

        def does_not_match?(subject)
          @subject = subject
          ensure_count_unconstrained
          @expectation = expect.never
          expected_messages_received?
        end

        def failure_message
          generate_failure_message
        end

        def negative_failure_message
          generate_failure_message
        end

        def description
          expect.description
        end

        CONSTRAINTS.each do |expectation|
          define_method expectation do |*args|
            @constraints << [expectation, *args]
            self
          end
        end

        private

        def expect
          expectation = mock_proxy.build_expectation(@method_name)
          apply_constraints_to expectation
          expectation
        end

        def apply_constraints_to(expectation)
          @constraints.each do |constraint|
            expectation.send(*constraint)
          end
        end

        def ensure_count_unconstrained
          if count_constraint
            raise RSpec::Mocks::MockExpectationError,
              "can't use #{count_constraint} when negative"
          end
        end

        def count_constraint
          @constraints.map(&:first).detect do |constraint|
            COUNT_CONSTRAINTS.include?(constraint)
          end
        end

        def generate_failure_message
          mock_proxy.check_for_unexpected_arguments(@expectation)
          @expectation.generate_error
        rescue RSpec::Mocks::MockExpectationError => error
          error.message
        end

        def expected_messages_received?
          mock_proxy.replay_received_message_on @expectation
          @expectation.expected_messages_received?
        end

        def mock_proxy
          RSpec::Mocks.proxy_for(@subject)
        end
      end
    end
  end
end

