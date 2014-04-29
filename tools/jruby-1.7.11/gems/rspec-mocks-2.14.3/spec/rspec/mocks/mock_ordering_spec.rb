require 'spec_helper'

module RSpec
  module Mocks

    describe "ordering" do
      before { @double = double("test double") }
      after  { reset @double }

      it "passes when messages are received in order" do
        @double.should_receive(:one).ordered
        @double.should_receive(:two).ordered
        @double.should_receive(:three).ordered
        @double.one
        @double.two
        @double.three
      end

      it "passes when messages are received in order across objects" do
        a = double("a")
        b = double("b")
        a.should_receive(:one).ordered
        b.should_receive(:two).ordered
        a.should_receive(:three).ordered
        a.one
        b.two
        a.three
      end

      it "fails when messages are received out of order (2nd message 1st)" do
        @double.should_receive(:one).ordered
        @double.should_receive(:two).ordered
        expect {
          @double.two
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :two out of order")
      end

      it "fails when messages are received out of order (3rd message 1st)" do
        @double.should_receive(:one).ordered
        @double.should_receive(:two).ordered
        @double.should_receive(:three).ordered
        @double.one
        expect {
          @double.three
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :three out of order")
      end

      it "fails when messages are received out of order (3rd message 2nd)" do
        @double.should_receive(:one).ordered
        @double.should_receive(:two).ordered
        @double.should_receive(:three).ordered
        @double.one
        expect {
          @double.three
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :three out of order")
      end

      it "fails when messages are out of order across objects" do
        a = double("test double")
        b = double("another test double")
        a.should_receive(:one).ordered
        b.should_receive(:two).ordered
        a.should_receive(:three).ordered
        a.one
        expect {
          a.three
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :three out of order")
        reset a
        reset b
      end

      it "ignores order of non ordered messages" do
        @double.should_receive(:ignored_0)
        @double.should_receive(:ordered_1).ordered
        @double.should_receive(:ignored_1)
        @double.should_receive(:ordered_2).ordered
        @double.should_receive(:ignored_2)
        @double.should_receive(:ignored_3)
        @double.should_receive(:ordered_3).ordered
        @double.should_receive(:ignored_4)
        @double.ignored_3
        @double.ordered_1
        @double.ignored_0
        @double.ordered_2
        @double.ignored_4
        @double.ignored_2
        @double.ordered_3
        @double.ignored_1
        verify @double
      end

      it "supports duplicate messages" do
        @double.should_receive(:a).ordered
        @double.should_receive(:b).ordered
        @double.should_receive(:a).ordered

        @double.a
        @double.b
        @double.a
      end
    end
  end
end
