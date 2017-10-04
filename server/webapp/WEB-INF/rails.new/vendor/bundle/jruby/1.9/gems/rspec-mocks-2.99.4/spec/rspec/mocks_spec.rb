require "spec_helper"

describe RSpec::Mocks do
  describe "::setup" do
    it "prints a deprecation warning when given a non-rspec host" do
      o = Object.new
      expect(RSpec).to receive(:deprecate).with(
        "The host argument to `RSpec::Mocks.setup`",
        :replacement=>"`include RSpec::Mocks::ExampleMethods` in #{o}"
      )
      RSpec::Mocks::setup(o)
    end

    it "prints the deprecation warning with the correct line" do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1)
      RSpec::Mocks::setup(Object.new)
    end

    it "does not print a deprecation warning when self (the example group) is passed." do
      expect(RSpec).not_to receive(:deprecate)
      RSpec::Mocks::setup(self)
    end

    context "with an existing Mock::Space" do
      before do
        @orig_space = RSpec::Mocks::space
      end

      after do
        RSpec::Mocks::space = @orig_space
      end

      it "memoizes the space" do
        RSpec::Mocks::setup
        space = RSpec::Mocks::space
        RSpec::Mocks::setup
        expect(RSpec::Mocks::space).to equal(space)
      end
    end
  end

  describe "::verify" do
    it "delegates to the space" do
      foo = double
      foo.should_receive(:bar)
      expect do
        RSpec::Mocks::verify
      end.to raise_error(RSpec::Mocks::MockExpectationError)
    end
  end

  describe "::teardown" do
    it "delegates to the space" do
      foo = "foo"
      foo.stub(:reverse) { "reversed" }
      RSpec::Mocks.teardown
      RSpec::Mocks.setup
      expect(foo.reverse).to eq("oof")
    end
  end

  describe ".configuration" do
    it 'returns a memoized configuration instance' do
      expect(RSpec::Mocks.configuration).to be_a(RSpec::Mocks::Configuration)
      expect(RSpec::Mocks.configuration).to be(RSpec::Mocks.configuration)
    end
  end

  context 'when requiring spec/mocks (as was valid for rspec 1)' do
    it 'prints a deprecation warning' do
      expect(::RSpec).to receive(:deprecate).with("require 'spec/mocks'", :replacement => "require 'rspec/mocks'")
      load "spec/mocks.rb"
    end
  end

  def file_contents_for(lib, filename)
    # http://rubular.com/r/HYpUMftlG2
    path = $LOAD_PATH.find { |p| p.match(/\/rspec-#{lib}(-[a-f0-9]+)?\/lib/) }
    file = File.join(path, filename)
    File.read(file)
  end

  it 'has an up-to-date caller_filter file' do
    mocks = file_contents_for("mocks", "rspec/mocks/caller_filter.rb")
    core  = file_contents_for("core",  "rspec/core/caller_filter.rb")

    expect(mocks).to eq(core)
  end
end
