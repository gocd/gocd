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
      match { |actual| actual.should be_ok }
    end

    ok.should be_well
  end

  it "has access to methods available in the scope of the example" do
    RSpec::Matchers::define(:ignore) {}
    ignore.question?.should eq(:answer)
  end

  it "raises when method is missing from local scope as well as matcher" do
    RSpec::Matchers::define(:ignore) {}
    expect { ignore.i_dont_exist }.to raise_error(NameError)
  end

  it "clears user instance variables between invocations" do
    RSpec::Matchers::define(:be_just_like) do |expected|
      match do |actual|
        @foo ||= expected
        @foo == actual
      end
    end

    3.should be_just_like(3)
    4.should be_just_like(4)
  end

  describe "#respond_to?" do
    it "returns true for methods in example scope" do
      RSpec::Matchers::define(:ignore) {}
      ignore.should respond_to(:question?)
    end

    it "returns false for methods not defined in matcher or example scope" do
      RSpec::Matchers::define(:ignore) {}
      ignore.should_not respond_to(:i_dont_exist)
    end
  end
end
