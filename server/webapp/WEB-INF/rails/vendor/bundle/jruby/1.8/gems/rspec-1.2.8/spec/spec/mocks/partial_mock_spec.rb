require File.dirname(__FILE__) + '/../../spec_helper.rb'

module Spec
  module Mocks
    describe "using a Partial Mock," do
      before(:each) do
        @object = Object.new
      end
    
      it "should name the class in the failure message" do
        @object.should_receive(:foo)
        lambda do
          @object.rspec_verify
        end.should raise_error(Spec::Mocks::MockExpectationError, /<Object:.*> expected/)
      end
    
      it "should name the class in the failure message when expectation is on class" do
        Object.should_receive(:foo)
        lambda do
          Object.rspec_verify
        end.should raise_error(Spec::Mocks::MockExpectationError, /<Object \(class\)>/)
      end
    
      it "should not conflict with @options in the object" do
        @object.instance_eval { @options = Object.new }
        @object.should_receive(:blah)
        @object.blah
      end
            
      it "should_not_receive should mock out the method" do
        @object.should_not_receive(:fuhbar)
        lambda do
          @object.fuhbar
        end.should raise_error(MockExpectationError, /<Object:.*> expected :fuhbar with \(no args\) 0 times/)
      end
    
      it "should_not_receive should return a negative message expectation" do
        @object.should_not_receive(:foobar).should be_kind_of(NegativeMessageExpectation)
      end
    
      it "should_receive should mock out the method" do
        @object.should_receive(:foobar).with(:test_param).and_return(1)
        @object.foobar(:test_param).should equal(1)
      end
    
      it "should_receive should handle a hash" do
        @object.should_receive(:foobar).with(:key => "value").and_return(1)
        @object.foobar(:key => "value").should equal(1)
      end
    
      it "should_receive should handle an inner hash" do
        hash = {:a => {:key => "value"}}
        @object.should_receive(:foobar).with(:key => "value").and_return(1)
        @object.foobar(hash[:a]).should equal(1)
      end
    
      it "should_receive should return a message expectation" do
        @object.should_receive(:foobar).should be_kind_of(MessageExpectation)
        @object.foobar
      end
    
      it "should_receive should verify method was called" do
        @object.should_receive(:foobar).with(:test_param).and_return(1)
        lambda do
          @object.rspec_verify
        end.should raise_error(Spec::Mocks::MockExpectationError)
      end
    
      it "should_receive should also take a String argument" do
        @object.should_receive('foobar')
        @object.foobar
      end
      
      it "should_not_receive should also take a String argument" do
        @object.should_not_receive('foobar')
        lambda do
          @object.foobar   
        end.should raise_error(Spec::Mocks::MockExpectationError)
      end
      
      it "should use report nil in the error message" do
        allow_message_expectations_on_nil
        
        @this_will_resolve_to_nil.should_receive(:foobar)
        lambda do
          @this_will_resolve_to_nil.rspec_verify
        end.should raise_error(Spec::Mocks::MockExpectationError, /nil expected :foobar with/)
      end
    end
    
    describe "Partially mocking an object that defines ==, after another mock has been defined" do
      before(:each) do
        stub("existing mock", :foo => :foo)
      end
      
      class PartiallyMockedEquals
        attr_reader :val
        def initialize(val)
          @val = val
        end
        
        def ==(other)
          @val == other.val
        end
      end
      
      it "should not raise an error when stubbing the object" do
        o = PartiallyMockedEquals.new :foo
        lambda { o.stub!(:bar) }.should_not raise_error(NoMethodError)
      end
    end

    describe "Method visibility when using partial mocks" do
      class MockableClass
        def public_method
          private_method
          protected_method
        end
        protected
        def protected_method; end
        private
        def private_method; end
      end

      before(:each) do
        @object = MockableClass.new
      end

      it 'should keep public methods public' do
        @object.should_receive(:public_method)
        with_ruby('1.9') do
          @object.public_methods.should include(:public_method)
        end
        with_ruby('1.8') do
          @object.public_methods.should include('public_method')
        end
        @object.public_method
      end

      it 'should keep private methods private' do
        @object.should_receive(:private_method)
        with_ruby('1.9') do
          @object.private_methods.should include(:private_method)
        end
        with_ruby('1.8') do
          @object.private_methods.should include('private_method')
        end
        @object.public_method
      end

      it 'should keep protected methods protected' do
        @object.should_receive(:protected_method)
        with_ruby('1.9') do
          @object.protected_methods.should include(:protected_method)
        end
        with_ruby('1.8') do
          @object.protected_methods.should include('protected_method')
        end
        @object.public_method
      end

    end
  end
end
