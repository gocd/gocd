require File.dirname(__FILE__) + '/../../spec_helper.rb'
module Spec
  module Matchers
    describe "equal" do
      
      def inspect_object(o)
        "#<#{o.class}:#{o.object_id}> => #{o.inspect}"
      end
      
      it "should match when actual.equal?(expected)" do
        1.should equal(1)
      end

      it "should not match when !actual.equal?(expected)" do
        1.should_not equal("1")
      end
      
      it "should describe itself" do
        matcher = equal(1)
        matcher.matches?(1)
        matcher.description.should == "equal 1"
      end
      
      it "should provide message on #failure_message" do
        expected, actual = "1", "1"
        matcher = equal(expected)
        matcher.matches?(actual)
        
        matcher.failure_message_for_should.should == <<-MESSAGE

expected #{inspect_object(expected)}
     got #{inspect_object(actual)}

Compared using equal?, which compares object identity,
but expected and actual are not the same object. Use
'actual.should == expected' if you don't care about
object identity in this example.

MESSAGE
      end
      
      it "should provide message on #negative_failure_message" do
        expected = actual = "1"
        matcher = equal(expected)
        matcher.matches?(actual)
        matcher.failure_message_for_should_not.should == <<-MESSAGE

expected not #{inspect_object(expected)}
         got #{inspect_object(actual)}

Compared using equal?, which compares object identity.

MESSAGE
      end
    end
  end
end
