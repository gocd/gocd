module RSpec
  module Mocks
    module AnyInstance
      # @private
      class StubChain < Chain

        # @private
        def initialize(*args, &block)
          record(:stub, *args, &block)
        end

        # @private
        def expectation_fulfilled?
          true
        end

        private

        def invocation_order
          @invocation_order ||= {
            :stub => [nil],
            :with => [:stub],
            :and_return => [:with, :stub],
            :and_raise => [:with, :stub],
            :and_yield => [:with, :stub]
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