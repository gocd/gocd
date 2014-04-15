require File.dirname(__FILE__) + '/../../spec_helper.rb'

module Spec
  module Mocks
    describe "stub implementation" do
      context "with no args" do
        it "execs the block when called" do
          obj = stub()
          obj.stub(:foo) { :bar }
          obj.foo.should == :bar
        end
      end

      context "with one arg" do
        it "execs the block with that arg when called" do
          obj = stub()
          obj.stub(:foo) {|given| given}
          obj.foo(:bar).should == :bar
        end
      end

      context "with variable args" do
        it "execs the block when called" do
          obj = stub()
          obj.stub(:foo) {|*given| given.first}
          obj.foo(:bar).should == :bar
        end
      end
    end
  end
end