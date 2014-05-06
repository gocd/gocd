require File.dirname(__FILE__) + '/../../spec_helper.rb'

module Spec
  module Matchers
    describe "eql" do
      it "should match when actual.eql?(expected)" do
        1.should eql(1)
      end
      
      it "should not match when !actual.eql?(expected)" do
        1.should_not eql(2)
      end
      
      it "should describe itself" do
        matcher = eql(1)
        matcher.matches?(1)
        matcher.description.should == "eql 1"
      end
      
      it "should provide message, expected and actual on #failure_message" do
        matcher = eql("1")
        matcher.matches?(1)
        matcher.failure_message_for_should.should == "\nexpected \"1\"\n     got 1\n\n(compared using eql?)\n"
      end
      
      it "should provide message, expected and actual on #negative_failure_message" do
        matcher = eql(1)
        matcher.matches?(1)
        matcher.failure_message_for_should_not.should == "\nexpected 1 not to equal 1\n\n(compared using eql?)\n"
      end
    end
  end
end
