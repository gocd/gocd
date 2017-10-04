require 'spec_helper'

module RSpec
  module Mocks
    describe 'and_return' do
      let(:obj) { double('obj') }

      context 'when no argument is passed' do
        it 'warns of deprection' do
          expect_warn_deprecation_with_call_site(__FILE__, __LINE__ + 1, '`and_return` without arguments')
          obj.stub(:foo).and_return
          expect(obj.foo).to be_nil
        end
      end
    end
  end
end
