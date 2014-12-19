require 'helper'

module Arel
  module Nodes
    describe 'False' do
      describe 'equality' do
        it 'is equal to other false nodes' do
          array = [False.new, False.new]
          assert_equal 1, array.uniq.size
        end

        it 'is not equal with other nodes' do
          array = [False.new, Node.new]
          assert_equal 2, array.uniq.size
        end
      end
    end
  end
end

