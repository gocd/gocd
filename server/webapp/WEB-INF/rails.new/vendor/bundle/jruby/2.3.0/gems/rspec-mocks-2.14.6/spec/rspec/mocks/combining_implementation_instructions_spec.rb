require 'spec_helper'

module RSpec
  module Mocks
    describe "Combining implementation instructions" do
      it 'can combine and_yield and and_return' do
        dbl = double
        dbl.stub(:foo).and_yield(5).and_return(3)

        expect { |b|
          expect(dbl.foo(&b)).to eq(3)
        }.to yield_with_args(5)
      end

      describe "combining and_yield, a block implementation and and_return" do
        def verify_combined_implementation
          dbl = double
          (yield dbl).and_yield(5).and_return(3)

          expect { |b|
            expect(dbl.foo(:arg, &b)).to eq(3)
          }.to yield_with_args(5)

          expect(@block_called).to be_true
        end

        it 'works when passing a block to `stub`' do
          verify_combined_implementation do |dbl|
            dbl.stub(:foo) { @block_called = true }
          end
        end

        it 'works when passing a block to `with`' do
          verify_combined_implementation do |dbl|
            dbl.stub(:foo).with(:arg) { @block_called = true }
          end
        end

        it 'works when passing a block to `exactly`' do
          verify_combined_implementation do |dbl|
            dbl.should_receive(:foo).exactly(:once) { @block_called = true }
          end
        end

        it 'works when passing a block to `at_least`' do
          verify_combined_implementation do |dbl|
            dbl.should_receive(:foo).at_least(:once) { @block_called = true }
          end
        end

        it 'works when passing a block to `at_most`' do
          verify_combined_implementation do |dbl|
            dbl.should_receive(:foo).at_most(:once) { @block_called = true }
          end
        end

        it 'works when passing a block to `times`' do
          verify_combined_implementation do |dbl|
            dbl.should_receive(:foo).exactly(1).times { @block_called = true }
          end
        end

        it 'works when passing a block to `any_number_of_times`' do
          verify_combined_implementation do |dbl|
            dbl.should_receive(:foo).any_number_of_times { @block_called = true }
          end
        end

        it 'works when passing a block to `once`' do
          verify_combined_implementation do |dbl|
            dbl.should_receive(:foo).once { @block_called = true }
          end
        end

        it 'works when passing a block to `twice`' do
          the_double = nil

          verify_combined_implementation do |dbl|
            the_double = dbl
            dbl.should_receive(:foo).twice { @block_called = true }
          end

          the_double.foo { |a| } # to ensure it is called twice
        end

        it 'works when passing a block to `ordered`' do
          verify_combined_implementation do |dbl|
            dbl.should_receive(:foo).ordered { @block_called = true }
          end
        end
      end

      it 'can combine and_yield and and_return with a block' do
        dbl = double
        dbl.stub(:foo).and_yield(5).and_return { :return }

        expect { |b|
          expect(dbl.foo(&b)).to eq(:return)
        }.to yield_with_args(5)
      end

      it 'can combine and_yield and and_raise' do
        dbl = double
        dbl.stub(:foo).and_yield(5).and_raise("boom")

        expect { |b|
          expect { dbl.foo(&b) }.to raise_error("boom")
        }.to yield_with_args(5)
      end

      it 'can combine and_yield, a block implementation and and_raise' do
        dbl = double
        block_called = false
        dbl.stub(:foo) { block_called = true }.and_yield(5).and_raise("boom")

        expect { |b|
          expect { dbl.foo(&b) }.to raise_error("boom")
        }.to yield_with_args(5)

        expect(block_called).to be_true
      end

      it 'can combine and_yield and and_throw' do
        dbl = double
        dbl.stub(:foo).and_yield(5).and_throw(:bar)

        expect { |b|
          expect { dbl.foo(&b) }.to throw_symbol(:bar)
        }.to yield_with_args(5)
      end

      it 'can combine and_yield, a block implementation and and_throw' do
        dbl = double
        block_called = false
        dbl.stub(:foo) { block_called = true }.and_yield(5).and_throw(:bar)

        expect { |b|
          expect { dbl.foo(&b) }.to throw_symbol(:bar)
        }.to yield_with_args(5)

        expect(block_called).to be_true
      end

      it 'returns `nil` from all terminal actions to discourage further configuration' do
        expect(double.stub(:foo).and_return(1)).to be_nil
        expect(double.stub(:foo).and_raise("boom")).to be_nil
        expect(double.stub(:foo).and_throw(:foo)).to be_nil
      end

      it 'allows the terminal action to be overriden' do
        dbl = double
        stubbed_double = dbl.stub(:foo)

        stubbed_double.and_return(1)
        expect(dbl.foo).to eq(1)

        stubbed_double.and_return(3)
        expect(dbl.foo).to eq(3)

        stubbed_double.and_raise("boom")
        expect { dbl.foo }.to raise_error("boom")

        stubbed_double.and_throw(:bar)
        expect { dbl.foo }.to throw_symbol(:bar)
      end

      it 'allows the inner implementation block to be overriden' do
        dbl = double
        stubbed_double = dbl.stub(:foo)

        stubbed_double.with(:arg) { :with_block }
        expect(dbl.foo(:arg)).to eq(:with_block)

        stubbed_double.at_least(:once) { :at_least_block }
        expect(dbl.foo(:arg)).to eq(:at_least_block)
      end

      it 'can combine and_call_original, with, and_return' do
        obj = Struct.new(:value).new('original')
        obj.stub(:value).and_call_original
        obj.stub(:value).with(:arg).and_return('value')
        expect(obj.value).to eq 'original'
        expect(obj.value(:arg)).to eq 'value'
      end

      it 'raises an error if `and_call_original` is followed by any other instructions' do
        dbl = [1, 2, 3]
        stubbed = dbl.stub(:size)
        stubbed.and_call_original

        msg_fragment = /cannot be modified further/

        expect { stubbed.and_yield }.to raise_error(msg_fragment)
        expect { stubbed.and_return(1) }.to raise_error(msg_fragment)
        expect { stubbed.and_raise("a") }.to raise_error(msg_fragment)
        expect { stubbed.and_throw(:bar) }.to raise_error(msg_fragment)

        expect { stubbed.once { } }.to raise_error(msg_fragment)

        expect(dbl.size).to eq(3)
      end
    end
  end
end

