require 'spec_helper'

module RSpec
  module Mocks
    describe "at_least" do
      before(:each) { @double = double }

      it "fails if method is never called" do
        @double.should_receive(:do_something).at_least(4).times
        expect {
          verify @double
        }.to raise_error(/expected: at least 4 times.*received: 0 times/m)
      end

      it "fails when called less than n times" do
        @double.should_receive(:do_something).at_least(4).times
        @double.do_something
        @double.do_something
        @double.do_something
        expect {
          verify @double
        }.to raise_error(/expected: at least 4 times.*received: 3 times/m)
      end

      it "fails when at least once method is never called" do
        @double.should_receive(:do_something).at_least(:once)
        expect {
          verify @double
        }.to raise_error(/expected: at least 1 time.*received: 0 times/m)
      end

      it "fails when at least twice method is called once" do
        @double.should_receive(:do_something).at_least(:twice)
        @double.do_something
        expect {
          verify @double
        }.to raise_error(/expected: at least 2 times.*received: 1 time/m)
      end

      it "fails when at least twice method is never called" do
        @double.should_receive(:do_something).at_least(:twice)
        expect {
          verify @double
        }.to raise_error(/expected: at least 2 times.*received: 0 times/m)
      end

      it "passes when at least n times method is called exactly n times" do
        @double.should_receive(:do_something).at_least(4).times
        @double.do_something
        @double.do_something
        @double.do_something
        @double.do_something
        verify @double
      end

      it "passes when at least n times method is called n plus 1 times" do
        @double.should_receive(:do_something).at_least(4).times
        @double.do_something
        @double.do_something
        @double.do_something
        @double.do_something
        @double.do_something
        verify @double
      end

      it "passes when at least once method is called once" do
        @double.should_receive(:do_something).at_least(:once)
        @double.do_something
        verify @double
      end

      it "passes when at least once method is called twice" do
        @double.should_receive(:do_something).at_least(:once)
        @double.do_something
        @double.do_something
        verify @double
      end

      it "passes when at least twice method is called three times" do
        @double.should_receive(:do_something).at_least(:twice)
        @double.do_something
        @double.do_something
        @double.do_something
        verify @double
      end

      it "passes when at least twice method is called twice" do
        @double.should_receive(:do_something).at_least(:twice)
        @double.do_something
        @double.do_something
        verify @double
      end

      it "returns the value given by a block when the at least once method is called" do
        @double.should_receive(:to_s).at_least(:once) { "testing" }
        expect(@double.to_s).to eq "testing"
        verify @double
      end

      context "when sent with 0" do
        before { RSpec.stub(:deprecate) }

        it "outputs a deprecation warning" do
          expect(RSpec).to receive(:deprecate).with("at_least\(0\) with should_receive", :replacement => "stub")
          expect(@double).to receive(:do_something).at_least(0).times
        end

        it "passes with no return if called once" do
          @double.should_receive(:do_something).at_least(0).times
          @double.do_something
        end

        it "passes with return block if called once" do
          @double.should_receive(:do_something).at_least(0).times { true }
          @double.do_something
        end

        it "passes with and_return if called once" do
          @double.should_receive(:do_something).at_least(0).times.and_return true
          @double.do_something
        end

        it "passes with no return if never called" do
          @double.should_receive(:do_something).at_least(0).times
        end

        it "passes with return block if never called" do
          @double.should_receive(:do_something).at_least(0).times { true }
        end

        it "passes with and_return if never called" do
          @double.should_receive(:do_something).at_least(0).times.and_return true
        end
      end

      it "uses a stub value if no value set" do
        @double.stub(:do_something => 'foo')
        @double.should_receive(:do_something).at_least(:once)
        expect(@double.do_something).to eq 'foo'
        expect(@double.do_something).to eq 'foo'
      end

      it "prefers its own return value over a stub" do
        @double.stub(:do_something => 'foo')
        @double.should_receive(:do_something).at_least(:once).and_return('bar')
        expect(@double.do_something).to eq 'bar'
        expect(@double.do_something).to eq 'bar'
      end
    end
  end
end
