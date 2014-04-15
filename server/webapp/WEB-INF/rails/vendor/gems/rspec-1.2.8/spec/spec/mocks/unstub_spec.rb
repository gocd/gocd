require File.dirname(__FILE__) + '/../../spec_helper'

module Spec
  module Mocks
    describe Mock do
      context "unstubbing a mock object with a stub" do
        it "should remove the stub" do
          a_mock = mock 'an object', :foo => :bar

          a_mock.unstub! :foo
          a_mock.should_not respond_to(:foo)
        end
      end

      context "unstubbing a real object with a stub" do
        before do
          @obj = Object.new
        end

        it "should raise a NoMethodError if the message is called after unstubbing" do
          @obj.stub!(:foo).and_return :bar
          @obj.unstub!(:foo)

          lambda {
            @obj.foo
          }.should raise_error(NoMethodError)
        end

        it "should only clear the stub specified" do
          @obj.stub!(:foo).and_return :bar
          @obj.stub!(:other).and_return :baz

          @obj.unstub!(:foo)

          @obj.other.should == :baz
        end

        it "should no longer respond_to? the method" do
          @obj.stub!(:foo).and_return :bar
          @obj.unstub!(:foo)

          @obj.should_not respond_to(:foo)
        end

        it "should unstub using a string (should convert the string to a symbol)" do
          @obj.stub!(:foo)

          @obj.unstub!("foo")

          @obj.should_not respond_to(:foo)
        end

        it "should restore a previous method definition" do
          def @obj.foo
            :a_result
          end

          @obj.stub!(:foo).and_return :stubbed_result
          @obj.unstub!(:foo)

          @obj.foo.should == :a_result
        end

        it "should have unstub as an alias of unstub!" do
          @obj.stub!(:foo).and_return :bar

          @obj.unstub(:foo)

          lambda {
            @obj.foo
          }.should raise_error(NoMethodError)
        end

        it "should raise a MockExpectationError if it is not stubbed" do
          lambda {
            @obj.unstub!(:foo)
          }.should raise_error(MockExpectationError, "The method `foo` was not stubbed or was already unstubbed")
        end

        it "should raise a MockExpectationError if it was already unstubbed" do
          @obj.stub!(:foo)
          @obj.unstub!(:foo)

          lambda {
            @obj.unstub!(:foo)
          }.should raise_error(MockExpectationError, "The method `foo` was not stubbed or was already unstubbed")
        end

        it "should use the correct message name in the error" do
          @obj.stub!(:bar)
          @obj.unstub!(:bar)

          lambda {
            @obj.unstub!(:bar)
          }.should raise_error(MockExpectationError, "The method `bar` was not stubbed or was already unstubbed")
        end

        it "should raise a MockExpectationError if the method is defined, but not stubbed" do
          def @obj.meth; end

          lambda {
            @obj.unstub!(:meth)
          }.should raise_error(MockExpectationError)
        end

        it "should be able to restub a after unstubbing" do
          @obj.stub!(:foo).and_return :bar

          @obj.unstub!(:foo)

          @obj.stub!(:foo).and_return :baz

          @obj.foo.should == :baz
        end

        it "should remove only the first stub if multiple stubs have been defined" do
          @obj.stub!(:foo).and_return :first
          @obj.stub!(:foo).and_return :second

          @obj.unstub!(:foo)

          @obj.foo.should == :first
        end
      end
    end
  end
end
