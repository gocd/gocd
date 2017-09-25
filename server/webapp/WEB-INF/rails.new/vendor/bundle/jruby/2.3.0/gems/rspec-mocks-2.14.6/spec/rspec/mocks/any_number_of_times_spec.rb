require 'spec_helper'

describe "AnyNumberOfTimes" do
  before(:each) do
    @mock = RSpec::Mocks::Mock.new("test mock")
    allow(RSpec).to receive(:deprecate)
  end

  it "is deprecated" do
    expect(RSpec).to receive(:deprecate).with("any_number_of_times", :replacement => "stub")
    expect(@mock).to receive(:random_call).any_number_of_times
  end

  it "passes if any number of times method is called many times" do
    @mock.should_receive(:random_call).any_number_of_times
    (1..10).each do
      @mock.random_call
    end
  end

  it "passes if any number of times method is called once" do
    @mock.should_receive(:random_call).any_number_of_times
    @mock.random_call
  end

  it "passes if any number of times method is not called" do
    @mock.should_receive(:random_call).any_number_of_times
  end

  it "returns the mocked value when called after a similar stub" do
    @mock.stub(:message).and_return :stub_value
    @mock.should_receive(:message).any_number_of_times.and_return(:mock_value)
    expect(@mock.message).to eq :mock_value
    expect(@mock.message).to eq :mock_value
  end
end
