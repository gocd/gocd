# encoding: utf-8
require 'spec_helper'


describe RSpec::Expectations, "#fail_with with diff of arrays" do
  before { RSpec::Matchers.configuration.stub(:color? => false) }

  it "splits items with newlines" do
    expected_diff = "\nDiff:\n@@ -1 +1,3 @@\n+a\\nb\n+c\\nd\n"
    expect {
      RSpec::Expectations.fail_with("", [], ["a\nb", "c\nd"])
    }.to fail_with(expected_diff)
  end

  it "shows inner arrays on a single line" do
    expected_diff = "\nDiff:\n@@ -1 +1,3 @@\n+a\\nb\n+[\"c\\nd\"]\n"
    expect {
      RSpec::Expectations.fail_with("", [], ["a\nb", ["c\nd"]])
    }.to fail_with(expected_diff)
  end
end

describe RSpec::Expectations, "#fail_with with diff" do
  let(:differ) { double("differ") }

  before(:each) do
    RSpec::Expectations.stub(:differ) { differ }
  end

  it "calls differ if expected/actual are not strings (or numbers or procs)" do
    differ.should_receive(:diff_as_object).and_return("diff")
    expect {
      RSpec::Expectations.fail_with "the message", Object.new, Object.new
    }.to fail_with("the message\nDiff:diff")
  end

  context "with two strings" do
    context "and actual is multiline" do
      it "calls differ" do
        differ.should_receive(:diff_as_string).and_return("diff")
        expect {
          RSpec::Expectations.fail_with "the message", "expected\nthis", "actual"
        }.to fail_with("the message\nDiff:diff")
      end
    end

    context "and expected is multiline" do
      it "calls differ" do
        differ.should_receive(:diff_as_string).and_return("diff")
        expect {
          RSpec::Expectations.fail_with "the message", "expected", "actual\nthat"
        }.to fail_with("the message\nDiff:diff")
      end
    end

    context "and both are single line strings" do
      it "does not call differ" do
        differ.should_not_receive(:diff_as_string)
        expect {
          RSpec::Expectations.fail_with("the message", "expected", "actual")
        }.to fail_with("the message")
      end
    end

    context "and they are UTF-16LE encoded", :if => String.method_defined?(:encode) do
      it 'does not diff when they are not multiline' do
        differ.should_not_receive(:diff_as_string)

        str_1 = "This is a pile of poo: 💩".encode("UTF-16LE")
        str_2 = "This is a pile of poo: 💩".encode("UTF-16LE")

        expect {
          RSpec::Expectations.fail_with("the message", str_1, str_2)
        }.to fail_with("the message")
      end

      it 'diffs when they are multiline' do
        differ.should_receive(:diff_as_string).and_return("diff")

        str_1 = "This is a pile of poo:\n💩".encode("UTF-16LE")
        str_2 = "This is a pile of poo:\n💩".encode("UTF-16LE")

        expect {
          RSpec::Expectations.fail_with("the message", str_1, str_2)
        }.to fail_with("the message\nDiff:diff")
      end
    end
  end

  it "does not call differ if no expected/actual" do
    expect {
      RSpec::Expectations.fail_with "the message"
    }.to fail_with("the message")
  end

  it "does not call differ expected is Numeric" do
    expect {
      RSpec::Expectations.fail_with "the message", 1, "1"
    }.to fail_with("the message")
  end

  it "does not call differ when actual is Numeric" do
    expect {
      RSpec::Expectations.fail_with "the message", "1", 1
    }.to fail_with("the message")
  end

  it "does not call differ if expected or actual are procs" do
    expect {
      RSpec::Expectations.fail_with "the message", lambda {}, lambda {}
    }.to fail_with("the message")
  end
end

