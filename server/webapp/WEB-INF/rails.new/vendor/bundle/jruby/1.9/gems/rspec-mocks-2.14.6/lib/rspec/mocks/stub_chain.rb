module RSpec
  module Mocks
    # @private
    class StubChain
      def self.stub_chain_on(object, *chain, &blk)
        new(object, *chain, &blk).stub_chain
      end

      attr_reader :object, :chain, :block

      def initialize(object, *chain, &blk)
        @object = object
        @chain, @block = format_chain(*chain, &blk)
      end

      def stub_chain
        if chain.length > 1
          if matching_stub = find_matching_stub
            chain.shift
            matching_stub.invoke(nil).stub_chain(*chain, &block)
          else
            next_in_chain = Mock.new
            object.stub(chain.shift) { next_in_chain }
            StubChain.stub_chain_on(next_in_chain, *chain, &block)
          end
        else
          object.stub(chain.shift, &block)
        end
      end

    private

      def format_chain(*chain, &blk)
        if Hash === chain.last
          hash = chain.pop
          hash.each do |k,v|
            chain << k
            blk = lambda { v }
          end
        end
        return chain.join('.').split('.'), blk
      end

      def find_matching_stub
        ::RSpec::Mocks.proxy_for(object).
          __send__(:find_matching_method_stub, chain.first.to_sym)
      end
    end
  end
end

