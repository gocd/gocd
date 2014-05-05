require File.dirname(__FILE__) + '/../../spec_helper.rb'

describe Spec::Expectations, "#fail_with with no diff" do
  before(:each) do
    @old_differ = Spec::Expectations.differ
    Spec::Expectations.differ = nil
  end
  
  it "should handle just a message" do
    lambda {
      Spec::Expectations.fail_with "the message"
    }.should fail_with("the message")
  end
  
  after(:each) do
    Spec::Expectations.differ = @old_differ
  end
end

describe Spec::Expectations, "#fail_with with Array" do
  before(:each) do
    Spec.stub!(:warn)
  end
  
  it "is deprecated" do
    Spec.should_receive(:warn)
    lambda {
      Spec::Expectations.fail_with ["message", "expected", "actual"]
    }.should raise_error
  end
end

describe Spec::Expectations, "#fail_with with diff" do
  before(:each) do
    @old_differ = Spec::Expectations.differ
    @differ = mock("differ")
    Spec::Expectations.differ = @differ
  end
  
  it "should not call differ if no expected/actual" do
    lambda {
      Spec::Expectations.fail_with "the message"
    }.should fail_with("the message")
  end
  
  it "should call differ if expected/actual are presented separately" do
    @differ.should_receive(:diff_as_string).and_return("diff")
    lambda {
      Spec::Expectations.fail_with "the message", "expected", "actual"
    }.should fail_with("the message\n\n Diff:diff")
  end
  
  it "should call differ if expected/actual are not strings" do
    @differ.should_receive(:diff_as_object).and_return("diff")
    lambda {
      Spec::Expectations.fail_with "the message", :expected, :actual
    }.should fail_with("the message\n\n Diff:diff")
  end
  
  it "should call differ if expected/actual are both hashes" do
    @differ.should_receive(:diff_as_hash).and_return("diff")
    lambda {
      Spec::Expectations.fail_with "the message", {:a => :b}, {:a => 'b'}
    }.should fail_with("the message\n\n Diff:diff")
  end
  
  it "should not call differ if expected or actual are procs" do
    @differ.should_not_receive(:diff_as_string)
    @differ.should_not_receive(:diff_as_object)
    @differ.should_not_receive(:diff_as_hash)
    lambda {
      Spec::Expectations.fail_with "the message", lambda {}, lambda {}
    }.should fail_with("the message")
  end

  after(:each) do
    Spec::Expectations.differ = @old_differ
  end
end

describe Spec::Expectations, "#fail_with with a nil message" do
  before(:each) do
    @old_differ = Spec::Expectations.differ
    Spec::Expectations.differ = nil
  end

  it "should handle just a message" do
    lambda {
      Spec::Expectations.fail_with nil
    }.should raise_error(ArgumentError, /Failure message is nil\. Does your matcher define the appropriate failure_message_for_\* method to return a string\?/)
  end

  after(:each) do
    Spec::Expectations.differ = @old_differ
  end
end
