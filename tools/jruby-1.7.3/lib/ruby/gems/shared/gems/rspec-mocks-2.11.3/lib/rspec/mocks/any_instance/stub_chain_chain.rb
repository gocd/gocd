module RSpec
  module Mocks
    module AnyInstance
      # @private
      class StubChainChain < StubChain

        # @private
        def initialize(*args, &block)
          record(:stub_chain, *args, &block)
        end

        private

        def invocation_order
          @invocation_order ||= {
            :stub_chain => [nil],
            :and_return => [:stub_chain],
            :and_raise => [:stub_chain],
            :and_yield => [:stub_chain]
          }
        end
      end
    end
  end
end