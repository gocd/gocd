require 'spec_helper'

module RSpec
  module Expectations
    # so our examples below can set expectations about the target
    ExpectationTarget.send(:attr_reader, :target)

    describe ExpectationTarget do
      context 'when constructed via #expect' do
        it 'constructs a new instance targetting the given argument' do
          expect(7).target.should eq(7)
        end

        it 'constructs a new instance targetting the given block' do
          block = lambda {}
          expect(&block).target.should be(block)
        end

        it 'raises an ArgumentError when given an argument and a block' do
          lambda { expect(7) { } }.should raise_error(ArgumentError)
        end

        it 'raises an ArgumentError when given neither an argument nor a block' do
          lambda { expect }.should raise_error(ArgumentError)
        end

        it 'can be passed nil' do
          expect(nil).target.should be_nil
        end

        it 'passes a valid positive expectation' do
          expect(5).to eq(5)
        end

        it 'passes a valid negative expectation' do
          expect(5).to_not eq(4)
          expect(5).not_to eq(4)
        end

        it 'fails an invalid positive expectation' do
          lambda { expect(5).to eq(4) }.should fail_with(/expected: 4.*got: 5/m)
        end

        it 'fails an invalid negative expectation' do
          message = /expected 5 not to be a kind of Fixnum/
          lambda { expect(5).to_not be_a(Fixnum) }.should fail_with(message)
          lambda { expect(5).not_to be_a(Fixnum) }.should fail_with(message)
        end

        it 'does not support operator matchers from #to' do
          expect {
            expect(3).to == 3
          }.to raise_error(ArgumentError)
        end

        it 'does not support operator matchers from #not_to' do
          expect {
            expect(3).not_to == 4
          }.to raise_error(ArgumentError)
        end
      end
    end
  end
end

