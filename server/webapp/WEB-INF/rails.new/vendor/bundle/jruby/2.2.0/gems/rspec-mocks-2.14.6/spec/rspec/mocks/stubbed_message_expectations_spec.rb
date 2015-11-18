require 'spec_helper'

describe "expection set on previously stubbed method" do
  it "fails if message is not received after expectation is set" do
    double = double(:msg => nil)
    double.msg
    double.should_receive(:msg)
    expect { verify double }.to raise_error(RSpec::Mocks::MockExpectationError)
  end

  it "outputs arguments of similar calls" do
    double = double('double', :foo => true)
    double.should_receive(:foo).with('first')
    double.foo('second')
    double.foo('third')
    expect {
      verify double
    }.to raise_error(%Q|Double "double" received :foo with unexpected arguments\n  expected: ("first")\n       got: ("second"), ("third")|)
    reset double
  end

  context "with argument constraint on stub" do
    it "matches any args if no arg constraint set on expectation" do
      double = double("mock")
      double.stub(:foo).with(3).and_return("stub")
      double.should_receive(:foo).at_least(:once).and_return("expectation")
      double.foo
      verify double
    end

    it "matches specific args set on expectation" do
      double = double("mock")
      double.stub(:foo).with(3).and_return("stub")
      double.should_receive(:foo).at_least(:once).with(4).and_return("expectation")
      double.foo(4)
      verify double
    end

    it "fails if expectation's arg constraint is not met" do
      double = double("mock")
      double.stub(:foo).with(3).and_return("stub")
      double.should_receive(:foo).at_least(:once).with(4).and_return("expectation")
      double.foo(3)
      expect { verify double }.to raise_error(/expected: \(4\)\s+got: \(3\)/)
    end

    it 'distinguishes between individual values and arrays properly' do
      dbl = double
      dbl.stub(:foo).with('a', ['b'])

      expect {
        dbl.foo(['a'], 'b')
      }.to raise_error { |e|
        expect(e.message).to include('expected: ("a", ["b"])', 'got: (["a"], "b")')
      }
    end
  end
end
