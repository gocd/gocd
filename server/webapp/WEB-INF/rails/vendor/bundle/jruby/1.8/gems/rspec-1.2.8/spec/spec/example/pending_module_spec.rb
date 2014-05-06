module Spec
  module Example
    describe Pending do
      
      context "when no block is supplied" do
        it "raises an ExamplePendingError if no block is supplied" do
          lambda {
            pending "TODO"
          }.should raise_error(ExamplePendingError, /TODO/)
        end
      end
      
      context "when the supplied block fails" do
        it "raises an ExamplePendingError if a supplied block fails as expected" do
          lambda {
            pending "TODO" do
              raise "oops"
            end
          }.should raise_error(ExamplePendingError, /TODO/)
        end
      end
      
      context "when the supplied block fails with a mock" do
        it "raises an ExamplePendingError if a supplied block fails as expected with a mock" do
          lambda {
            pending "TODO" do
              m = mock("thing")
              m.should_receive(:foo)
              m.rspec_verify
            end
          }.should raise_error(ExamplePendingError, /TODO/)
        end
      end
      
      context "when the supplied block passes" do
        it "raises a PendingExampleFixedError" do
          lambda {
            pending "TODO" do
              # success!
            end
          }.should raise_error(PendingExampleFixedError, /TODO/)
        end
      end
    end
    
    describe ExamplePendingError do
      it "should have the message provided" do
        ExamplePendingError.new("a message").message.should == "a message"
      end
    end
    
    describe NotYetImplementedError do
      it "should have the message 'Not Yet Implemented'" do
        NotYetImplementedError.new.message.should == "Not Yet Implemented"
      end
    end
  end
end
