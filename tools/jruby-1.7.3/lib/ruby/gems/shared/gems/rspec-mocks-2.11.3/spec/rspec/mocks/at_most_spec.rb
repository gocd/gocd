require 'spec_helper'

module RSpec
  module Mocks
    describe "at_most" do
      before(:each) do
        @double = double
      end

      it "passes when at_most(n) is called exactly n times" do
        @double.should_receive(:do_something).at_most(2).times
        @double.do_something
        @double.do_something
        @double.rspec_verify
      end

      it "passes when at_most(n) is called less than n times" do
        @double.should_receive(:do_something).at_most(2).times
        @double.do_something
        @double.rspec_verify
      end

      it "passes when at_most(n) is never called" do
        @double.should_receive(:do_something).at_most(2).times
        @double.rspec_verify
      end

      it "passes when at_most(:once) is called once" do
        @double.should_receive(:do_something).at_most(:once)
        @double.do_something
        @double.rspec_verify
      end

      it "passes when at_most(:once) is never called" do
        @double.should_receive(:do_something).at_most(:once)
        @double.rspec_verify
      end

      it "passes when at_most(:twice) is called once" do
        @double.should_receive(:do_something).at_most(:twice)
        @double.do_something
        @double.rspec_verify
      end

      it "passes when at_most(:twice) is called twice" do
        @double.should_receive(:do_something).at_most(:twice)
        @double.do_something
        @double.do_something
        @double.rspec_verify
      end

      it "passes when at_most(:twice) is never called" do
        @double.should_receive(:do_something).at_most(:twice)
        @double.rspec_verify
      end

      it "returns the value given by a block when at_most(:once) method is called" do
        @double.should_receive(:to_s).at_most(:once) { "testing" }
        @double.to_s.should eq "testing"
        @double.rspec_verify
      end

      it "fails fast when at_most(n) times method is called n plus 1 times" do
        @double.should_receive(:do_something).at_most(2).times
        @double.do_something
        @double.do_something
        lambda do
          @double.do_something
        end.should raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "fails fast when at_most(:once) and is called twice" do
        @double.should_receive(:do_something).at_most(:once)
        @double.do_something
        lambda do
          @double.do_something
        end.should raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "fails fast when at_most(:twice) and is called three times" do
        @double.should_receive(:do_something).at_most(:twice)
        @double.do_something
        @double.do_something
        lambda do
          @double.do_something
        end.should raise_error(RSpec::Mocks::MockExpectationError)
      end
    end
  end
end
