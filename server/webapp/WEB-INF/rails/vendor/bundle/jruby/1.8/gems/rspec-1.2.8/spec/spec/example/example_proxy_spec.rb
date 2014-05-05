require File.dirname(__FILE__) + '/../../spec_helper'

module Spec
  module Example

    describe ExampleProxy do

      describe "#description" do
        it "provides the submitted description" do
          proxy = ExampleProxy.new("the description")
          proxy.description.should   == "the description"
        end
      end
      
      describe "#update" do
        it "updates the description" do
          proxy = ExampleProxy.new("old description")
          proxy.update("new description")
          proxy.description.should == "new description"
        end
      end

      describe "#options" do
        it "provides the submitted options" do
          proxy = ExampleProxy.new(:ignore, {:these => :options})
          proxy.options.should           == {:these => :options}
        end
      end

      describe "#backtrace (DEPRECATED - use #location)" do
        before(:each) do
          Spec.stub!(:deprecate)
        end
        
        it "is deprecated" do
          Spec.should_receive(:deprecate)
          proxy = ExampleProxy.new(:ignore, {}, "path/to/location:37")
          proxy.backtrace
        end
        
        it "provides the location of the declaration of this group" do
          proxy = ExampleProxy.new(:ignore, {}, "path/to/location:37")
          proxy.backtrace.should             == "path/to/location:37"
        end
      end
      
      describe "#location" do
        it "provides the location of the declaration of this group" do
          proxy = ExampleProxy.new(:ignore, {}, "path/to/location:37")
          proxy.location.should              == "path/to/location:37"
        end
      end
      
    end

  end
end
