require "spec_helper"

describe "double" do
  it "is an alias for stub and mock" do
    expect(double()).to be_a(RSpec::Mocks::Double)
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

  it 'does not get string vs symbol messages confused' do
    dbl = double("foo" => 1)
    allow(dbl).to receive(:foo).and_return(2)
    expect(dbl.foo).to eq(2)
    expect { reset dbl }.not_to raise_error
  end

  context "after it has been torn down" do
    let(:dbl) { double }

    before do
      expect(dbl).to receive(:foo).at_least(:once)
      allow(dbl).to receive(:bar)
      dbl.foo

      RSpec::Mocks.verify
      RSpec::Mocks.teardown
      RSpec::Mocks.setup
    end

    it 'warns when stubbing new methods (with receive)' do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1)
      allow(dbl).to receive(:bazz).and_return(3)
      expect(dbl.bazz).to eq(3)
    end

    it 'warns when mocking new methods' do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1)
      expect(dbl).to receive(:bazz)
      dbl.bazz
    end

    it 'warns when turned into a null object' do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1)
      dbl.as_null_object
      dbl.foo.bar.bazz.goo
    end

    it 'warns when checked for nullness' do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1)
      dbl.null_object?
    end
  end

  context 'when frozen' do
    it 'warns of deprecation' do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1)
      double.freeze
    end

    it 'is really frozen' do
      expect(double.freeze).to be_frozen
    end
  end

  context 'when it has turned into a null object and been frozen' do
    before do
      double.as_null_object.freeze
    end

    context 'on tearing down' do
      it 'does not raise error' do
        expect { RSpec::Mocks.verify }.not_to raise_error
      end
    end
  end

  context 'when being deserialized from YAML' do
    let(:yaml) { YAML.dump(double) }

    it 'does not raise error' do
      expect { YAML.load(yaml) }.not_to raise_error
    end
  end
end
