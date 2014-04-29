module RSpec
  module Mocks
    module AnyInstance
      # @private
      class StubChainChain < StubChain

        private

        def create_message_expectation_on(instance)
          ::RSpec::Mocks::StubChain.stub_chain_on(instance, *@expectation_args, &@expectation_block)
        end

        def invocation_order
          @invocation_order ||= {
            :and_return => [nil],
            :and_raise => [nil],
            :and_yield => [nil]
          }
        end
      end
    end
  end
end
