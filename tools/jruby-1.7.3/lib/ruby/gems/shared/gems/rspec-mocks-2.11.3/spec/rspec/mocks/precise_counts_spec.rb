require 'spec_helper'

module RSpec
  module Mocks
    describe "PreciseCounts" do
      before(:each) do
        @double = double("test double")
      end

      it "fails when exactly n times method is called less than n times" do
        @double.should_receive(:do_something).exactly(3).times
        @double.do_something
        @double.do_something
        lambda do
          @double.rspec_verify
        end.should raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "fails fast when exactly n times method is called more than n times" do
        @double.should_receive(:do_something).exactly(3).times
        @double.do_something
        @double.do_something
        @double.do_something
        lambda do
          @double.do_something
        end.should raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "fails when exactly n times method is never called" do
        @double.should_receive(:do_something).exactly(3).times
        lambda do
          @double.rspec_verify
        end.should raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "passes if exactly n times method is called exactly n times" do
        @double.should_receive(:do_something).exactly(3).times
        @double.do_something
        @double.do_something
        @double.do_something
        @double.rspec_verify
      end

      it "returns the value given by a block when the exactly once method is called" do
        @double.should_receive(:to_s).exactly(:once) { "testing" }
        @double.to_s.should eq "testing"
        @double.rspec_verify
      end

      it "passes mutiple calls with different args" do
        @double.should_receive(:do_something).once.with(1)
        @double.should_receive(:do_something).once.with(2)
        @double.do_something(1)
        @double.do_something(2)
        @double.rspec_verify
      end

      it "passes multiple calls with different args and counts" do
        @double.should_receive(:do_something).twice.with(1)
        @double.should_receive(:do_something).once.with(2)
        @double.do_something(1)
        @double.do_something(2)
        @double.do_something(1)
        @double.rspec_verify
      end
    end
  end
end
