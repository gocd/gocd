require 'spec_helper'

describe "expection set on previously stubbed method" do
  it "fails if message is not received after expectation is set" do
    double = double(:msg => nil)
    double.msg
    double.should_receive(:msg)
    lambda { double.rspec_verify }.should raise_error(RSpec::Mocks::MockExpectationError)
  end

  it "outputs arguments of similar calls" do
    double = double('double', :foo => true)
    double.should_receive(:foo).with('first')
    double.foo('second')
    double.foo('third')
    lambda do
      double.rspec_verify
    end.should raise_error(%Q|Double "double" received :foo with unexpected arguments\n  expected: ("first")\n       got: ("second"), ("third")|)
    double.rspec_reset
  end

  context "with argument constraint on stub" do
    it "matches any args if no arg constraint set on expectation" do
      double = double("mock")
      double.stub(:foo).with(3).and_return("stub")
      double.should_receive(:foo).at_least(:once).and_return("expectation")
      double.foo
      double.rspec_verify
    end

    it "matches specific args set on expectation" do
      double = double("mock")
      double.stub(:foo).with(3).and_return("stub")
      double.should_receive(:foo).at_least(:once).with(4).and_return("expectation")
      double.foo(4)
      double.rspec_verify
    end

    it "fails if expectation's arg constraint is not met" do
      double = double("mock")
      double.stub(:foo).with(3).and_return("stub")
      double.should_receive(:foo).at_least(:once).with(4).and_return("expectation")
      double.foo(3)
      expect { double.rspec_verify }.to raise_error(/expected: \(4\)\s+got: \(3\)/)
    end
  end
end
