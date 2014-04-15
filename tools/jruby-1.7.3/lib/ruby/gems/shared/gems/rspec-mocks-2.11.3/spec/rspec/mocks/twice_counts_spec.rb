require 'spec_helper'

module RSpec
  module Mocks
    describe "#twice" do
      before(:each) do
        @double = double("test double")
      end

      it "passes when called twice" do
        @double.should_receive(:do_something).twice
        @double.do_something
        @double.do_something
        @double.rspec_verify
      end

      it "passes when called twice with specified args" do
        @double.should_receive(:do_something).twice.with("1", 1)
        @double.do_something("1", 1)
        @double.do_something("1", 1)
        @double.rspec_verify
      end

      it "passes when called twice with unspecified args" do
        @double.should_receive(:do_something).twice
        @double.do_something("1")
        @double.do_something(1)
        @double.rspec_verify
      end

      it "fails fast when call count is higher than expected" do
        @double.should_receive(:do_something).twice
        @double.do_something
        @double.do_something
        lambda do
          @double.do_something
        end.should raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "fails when call count is lower than expected" do
        @double.should_receive(:do_something).twice
        @double.do_something
        lambda do
          @double.rspec_verify
        end.should raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "fails when called wrong args on the first call" do
        @double.should_receive(:do_something).twice.with("1", 1)
        lambda do
          @double.do_something(1, "1")
        end.should raise_error(RSpec::Mocks::MockExpectationError)
        @double.rspec_reset
      end

      it "fails when called with wrong args on the second call" do
        @double.should_receive(:do_something).twice.with("1", 1)
        @double.do_something("1", 1)
        lambda do
          @double.do_something(1, "1")
        end.should raise_error(RSpec::Mocks::MockExpectationError)
        @double.rspec_reset
      end
    end
  end
end
