require "spec_helper"

describe "double" do
  it "is an alias for stub and mock" do
    expect(double()).to be_a(RSpec::Mocks::Mock)
  end

  it "uses 'Double' in failure messages" do
    double = double('name')
    expect {double.foo}.to raise_error(/Double "name" received/)
  end

  describe "deprecated aliases" do
    it "warns if #stub is used" do
      expect(RSpec).to receive(:deprecate).with("stub", :replacement => "double")
      stub("TestDouble")
    end

    it "warns if #mock is used" do
      expect(RSpec).to receive(:deprecate).with("mock", :replacement => "double")
      mock("TestDouble")
    end
  end
end
