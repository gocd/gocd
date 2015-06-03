require 'spec_helper'

module RSpec
  module Mocks
    describe "a double stubbed with multiple return values" do
      let(:a_double) { double }

      before do
        a_double.stub(:foo).and_return(:val_1, nil)
      end

      it 'can still set a message expectation with a single return value' do
        a_double.should_receive(:foo).once.and_return(:val_1)
        expect(a_double.foo).to eq(:val_1)
      end
    end

    describe "a message expectation with multiple return values and no specified count" do
      before(:each) do
        @double = double
        @return_values = [1,2,3]
        @double.should_receive(:do_something).and_return(@return_values[0],@return_values[1],@return_values[2])
      end

      it "returns values in order" do
        expect(@double.do_something).to eq @return_values[0]
        expect(@double.do_something).to eq @return_values[1]
        expect(@double.do_something).to eq @return_values[2]
        verify @double
      end

      it "falls back to a previously stubbed value" do
        @double.stub :do_something => :stub_result
        expect(@double.do_something).to eq @return_values[0]
        expect(@double.do_something).to eq @return_values[1]
        expect(@double.do_something).to eq @return_values[2]
        expect(@double.do_something).to eq :stub_result
      end

      it "fails when there are too few calls (if there is no stub)" do
        @double.do_something
        @double.do_something
        expect { verify @double }.to raise_error
      end

      it "fails when there are too many calls (if there is no stub)" do
        @double.do_something
        @double.do_something
        @double.do_something
        @double.do_something
        expect { verify @double }.to raise_error
      end
    end

    describe "a message expectation with multiple return values with a specified count equal to the number of values" do
      before(:each) do
        @double = double
        @return_values = [1,2,3]
        @double.should_receive(:do_something).exactly(3).times.and_return(@return_values[0],@return_values[1],@return_values[2])
      end

      it "returns values in order to consecutive calls" do
        expect(@double.do_something).to eq @return_values[0]
        expect(@double.do_something).to eq @return_values[1]
        expect(@double.do_something).to eq @return_values[2]
        verify @double
      end
    end

    describe "a message expectation with multiple return values specifying at_least less than the number of values" do
      before(:each) do
        @double = double
        @double.should_receive(:do_something).at_least(:twice).with(no_args).and_return(11, 22)
      end

      it "uses the last return value for subsequent calls" do
        expect(@double.do_something).to equal(11)
        expect(@double.do_something).to equal(22)
        expect(@double.do_something).to equal(22)
        verify @double
      end

      it "fails when called less than the specified number" do
        expect(@double.do_something).to equal(11)
        expect { verify @double }.to raise_error(RSpec::Mocks::MockExpectationError)
      end

      context "when method is stubbed too" do
        before { @double.stub(:do_something).and_return :stub_result }

        it "uses the last value for subsequent calls" do
          expect(@double.do_something).to equal(11)
          expect(@double.do_something).to equal(22)
          expect(@double.do_something).to equal(22)
          verify @double
        end

        it "fails when called less than the specified number" do
          expect(@double.do_something).to equal(11)
          expect { verify @double }.to raise_error(RSpec::Mocks::MockExpectationError)
        end
      end
    end

    describe "a message expectation with multiple return values with a specified count larger than the number of values" do
      before(:each) do
        @double = RSpec::Mocks::Mock.new("double")
        @double.should_receive(:do_something).exactly(3).times.and_return(11, 22)
      end

      it "uses the last return value for subsequent calls" do
        expect(@double.do_something).to equal(11)
        expect(@double.do_something).to equal(22)
        expect(@double.do_something).to equal(22)
        verify @double
      end

      it "fails when called less than the specified number" do
        @double.do_something
        @double.do_something
        expect { verify @double }.to raise_error
      end

      it "fails fast when called greater than the specified number" do
        @double.do_something
        @double.do_something
        @double.do_something
        expect { @double.do_something }.to raise_error
      end
    end
  end
end
