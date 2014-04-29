module RSpec
  module Mocks
    module AnyInstance
      # @private
      class StubChain < Chain

        # @private
        def expectation_fulfilled?
          true
        end

        private

        def create_message_expectation_on(instance)
          proxy = ::RSpec::Mocks.proxy_for(instance)
          expected_from = IGNORED_BACKTRACE_LINE
          proxy.add_stub(expected_from, *@expectation_args, &@expectation_block)
        end

        def invocation_order
          @invocation_order ||= {
            :with => [nil],
            :and_return => [:with, nil],
            :and_raise => [:with, nil],
            :and_yield => [:with, nil],
            :and_call_original => [:with, nil]
          }
        end

        def verify_invocation_order(rspec_method_name, *args, &block)
          unless invocation_order[rspec_method_name].include?(last_message)
            raise(NoMethodError, "Undefined method #{rspec_method_name}")
          end
        end
      end
    end
  end
end
