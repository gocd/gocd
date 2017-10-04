require 'spec_helper'

describe "a matcher defined using the matcher DSL" do
  def question?
    :answer
  end

  def ok
    "ok"
  end

  it "supports calling custom matchers from within other custom matchers" do
    RSpec::Matchers.define :be_ok do
      match { |actual| actual == ok }
    end

    RSpec::Matchers.define :be_well do
      match { |actual| expect(actual).to be_ok }
    end

    expect(ok).to be_well
  end

  it "has access to methods available in the scope of the example" do
    RSpec::Matchers::define(:matcher_a) {}
    expect(matcher_a.question?).to eq(:answer)
  end

  it "raises when method is missing from local scope as well as matcher" do
    RSpec::Matchers::define(:matcher_b) {}
    expect { matcher_b.i_dont_exist }.to raise_error(NameError)
  end

  it "clears user instance variables between invocations" do
    RSpec::Matchers::define(:be_just_like) do |expected|
      match do |actual|
        @foo ||= expected
        @foo == actual
      end
    end

    expect(3).to be_just_like(3)
    expect(4).to be_just_like(4)
  end

  describe "#respond_to?" do
    it "returns true for methods in example scope" do
      RSpec::Matchers::define(:matcher_c) {}
      expect(matcher_c).to respond_to(:question?)
    end

    it "returns false for methods not defined in matcher or example scope" do
      RSpec::Matchers::define(:matcher_d) {}
      expect(matcher_d).not_to respond_to(:i_dont_exist)
    end
  end
end
