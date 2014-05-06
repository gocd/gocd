require File.dirname(__FILE__) + '/../../spec_helper.rb'
module Spec
  module Matchers
    describe "[actual.should] be_close(expected, delta)" do
      it "matches when actual == expected" do
        be_close(5.0, 0.5).matches?(5.0).should be_true
      end
      it "matches when actual < (expected + delta)" do
        be_close(5.0, 0.5).matches?(5.49).should be_true
      end
      it "matches when actual > (expected - delta)" do
        be_close(5.0, 0.5).matches?(4.51).should be_true
      end
      it "does not match when actual == (expected - delta)" do
        be_close(5.0, 0.5).matches?(4.5).should be_false
      end
      it "does not match when actual < (expected - delta)" do
        be_close(5.0, 0.5).matches?(4.49).should be_false
      end
      it "does not match when actual == (expected + delta)" do
        be_close(5.0, 0.5).matches?(5.5).should be_false
      end
      it "does not match when actual > (expected + delta)" do
        be_close(5.0, 0.5).matches?(5.51).should be_false
      end
      it "provides a failure message for should" do
        #given
          matcher = be_close(5.0, 0.5)
        #when
          matcher.matches?(5.51)
        #then
          matcher.failure_message_for_should.should == "expected 5.0 +/- (< 0.5), got 5.51"
      end

      it "provides a failure message for should tno" do
        #given
          matcher = be_close(5.0, 0.5)
        #when
          matcher.matches?(5.49)
        #then
          matcher.failure_message_for_should_not.should == "expected 5.0 +/- (< 0.5), got 5.49"
      end
      it "provides a description" do
        matcher = be_close(5.0, 0.5)
        matcher.matches?(5.1)
        matcher.description.should == "be close to 5.0 (within +- 0.5)"
      end
    end
  end
end
