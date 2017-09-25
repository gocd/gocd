require 'spec_helper'

module RSpec
  module Mocks
    describe ArgumentListMatcher do
      let(:argument_expectation) { RSpec::Mocks::ArgumentListMatcher.new }
      let(:obj) { double("matcher") }

      it "considers an object that responds to #matches? and #failure_message_for_should to be a matcher" do
        obj.stub(:matches?)
        obj.stub(:failure_message_for_should)
        expect(argument_expectation.send(:is_matcher?, obj)).to be_true
      end

      it "considers an object that responds to #matches? and #failure_message to be a matcher for backward compatibility" do
        obj.stub(:matches?)
        obj.stub(:failure_message)
        expect(argument_expectation.send(:is_matcher?, obj)).to be_true
      end

      it "does NOT consider an object that only responds to #matches? to be a matcher" do
        obj.stub(:matches?)
        expect(argument_expectation.send(:is_matcher?, obj)).to be_false
      end

      it "does not consider a null object to be a matcher" do
        obj.as_null_object
        expect(argument_expectation.send(:is_matcher?, obj)).to be_false
      end
    end
  end
end
