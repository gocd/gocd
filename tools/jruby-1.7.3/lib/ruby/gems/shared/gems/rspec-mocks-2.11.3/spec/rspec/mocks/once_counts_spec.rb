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
        @double.rspec_verify
      end

      it "passes when called once with specified args" do
        @double.should_receive(:do_something).once.with("a", "b", "c")
        @double.do_something("a", "b", "c")
        @double.rspec_verify
      end

      it "passes when called once with unspecified args" do
        @double.should_receive(:do_something).once
        @double.do_something("a", "b", "c")
        @double.rspec_verify
      end

      it "fails when called with wrong args" do
        @double.should_receive(:do_something).once.with("a", "b", "c")
        lambda do
          @double.do_something("d", "e", "f")
        end.should raise_error(RSpec::Mocks::MockExpectationError)
        @double.rspec_reset
      end

      it "fails fast when called twice" do
        @double.should_receive(:do_something).once
        @double.do_something
        lambda do
          @double.do_something
        end.should raise_error(RSpec::Mocks::MockExpectationError)
      end
      
      it "fails when not called" do
        @double.should_receive(:do_something).once
        lambda do
          @double.rspec_verify
        end.should raise_error(RSpec::Mocks::MockExpectationError)
      end
    end
  end
end
