require 'spec_helper'

module RSpec
  module Mocks
    describe ArgumentListMatcher do

      it "considers an object that responds to #matches? and #failure_message_for_should to be a matcher" do
        argument_expectation = RSpec::Mocks::ArgumentListMatcher.new
        obj = double("matcher")
        obj.stub(:respond_to?).with(:matches?).and_return(true)
        obj.stub(:respond_to?).with(:failure_message_for_should).and_return(true)
        argument_expectation.send(:is_matcher?, obj).should be_true
      end

      it "considers an object that responds to #matches? and #failure_message to be a matcher for backward compatibility" do
        argument_expectation = RSpec::Mocks::ArgumentListMatcher.new
        obj = double("matcher")
        obj.stub(:respond_to?).with(:matches?).and_return(true)
        obj.stub(:respond_to?).with(:failure_message_for_should).and_return(false)
        obj.stub(:respond_to?).with(:failure_message).and_return(true)
        argument_expectation.send(:is_matcher?, obj).should be_true
      end

      it "does NOT consider an object that only responds to #matches? to be a matcher" do
        argument_expectation = RSpec::Mocks::ArgumentListMatcher.new
        obj = double("matcher")
        obj.stub(:respond_to?).with(:matches?).and_return(true)
        obj.stub(:respond_to?).with(:failure_message_for_should).and_return(false)
        obj.stub(:respond_to?).with(:failure_message).and_return(false)
        argument_expectation.send(:is_matcher?, obj).should be_false
      end
    end
  end
end
