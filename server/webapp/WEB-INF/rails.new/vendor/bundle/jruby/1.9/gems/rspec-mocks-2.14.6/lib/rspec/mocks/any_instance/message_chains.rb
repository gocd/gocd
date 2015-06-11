module RSpec
  module Mocks
    module AnyInstance
      # @private
      class MessageChains < Hash
        def initialize
          super {|h,k| h[k] = []}
        end

        # @private
        def add(method_name, chain)
          self[method_name] << chain
          chain
        end

        # @private
        def remove_stub_chains_for!(method_name)
          self[method_name].reject! {|chain| chain.is_a?(StubChain)}
        end

        # @private
        def has_expectation?(method_name)
          self[method_name].find {|chain| chain.is_a?(ExpectationChain)}
        end

        # @private
        def all_expectations_fulfilled?
          all? {|method_name, chains| chains.all? {|chain| chain.expectation_fulfilled?}}
        end

        # @private
        def unfulfilled_expectations
          map do |method_name, chains|
            method_name.to_s if chains.last.is_a?(ExpectationChain) unless chains.last.expectation_fulfilled?
          end.compact
        end

        # @private
        def received_expected_message!(method_name)
          self[method_name].each {|chain| chain.expectation_fulfilled!}
        end

        # @private
        def playback!(instance, method_name)
          raise_if_second_instance_to_receive_message(instance)
          self[method_name].each {|chain| chain.playback!(instance)}
        end

        private

        def raise_if_second_instance_to_receive_message(instance)
          @instance_with_expectation ||= instance if instance.is_a?(ExpectationChain)
          if instance.is_a?(ExpectationChain) && !@instance_with_expectation.equal?(instance)
            raise RSpec::Mocks::MockExpectationError, "Exactly one instance should have received the following message(s) but didn't: #{unfulfilled_expectations.sort.join(', ')}"
          end
        end
      end
    end
  end
end
