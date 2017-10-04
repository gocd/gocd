require 'spec_helper'

module RSpec
  module Mocks
    describe "#once" do
      before(:each) do
        @double = double
      end

      it "passes when called once" do
        @double.should_receive(:do_something).once
        @double.do_something
        verify @double
      end

      it "passes when called once with specified args" do
        @double.should_receive(:do_something).once.with("a", "b", "c")
        @double.do_something("a", "b", "c")
        verify @double
      end

      it "passes when called once with unspecified args" do
        @double.should_receive(:do_something).once
        @double.do_something("a", "b", "c")
        verify @double
      end

      it "fails when called with wrong args" do
        @double.should_receive(:do_something).once.with("a", "b", "c")
        expect {
          @double.do_something("d", "e", "f")
        }.to raise_error(RSpec::Mocks::MockExpectationError)
        reset @double
      end

      it "fails fast when called twice" do
        @double.should_receive(:do_something).once
        @double.do_something
        expect {
          @double.do_something
        }.to raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "fails when not called" do
        @double.should_receive(:do_something).once
        expect {
          verify @double
        }.to raise_error(RSpec::Mocks::MockExpectationError)
      end
    end
  end
end
