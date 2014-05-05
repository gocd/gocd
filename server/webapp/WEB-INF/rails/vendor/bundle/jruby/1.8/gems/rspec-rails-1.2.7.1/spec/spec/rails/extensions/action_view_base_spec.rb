require File.dirname(__FILE__) + '/../../../spec_helper'
require 'spec/mocks/errors'

describe ActionView::Base, "with RSpec extensions:", :type => :view do 
  
  describe "should_receive(:render)" do
    it "should not raise when render has been received" do
      template.should_receive(:render).with(:partial => "name")
      template.render :partial => "name"
    end
  
    it "should raise when render has NOT been received" do
      template.should_receive(:render).with(:partial => "name")
      lambda {
        template.verify_rendered
      }.should raise_error
    end
    
    it "should return something (like a normal mock)" do
      template.should_receive(:render).with(:partial => "name").and_return("Little Johnny")
      result = template.render :partial => "name"
      result.should == "Little Johnny"
    end
  end
  
  [:stub!, :stub].each do |method|
    describe "#{method}(:render)" do
      it "should not raise when stubbing and render has been received" do
        template.send(method, :render).with(:partial => "name")
        template.render :partial => "name"
      end
  
      it "should not raise when stubbing and render has NOT been received" do
        template.send(method, :render).with(:partial => "name")
      end
  
      it "should not raise when stubbing and render has been received with different options" do
        template.send(method, :render).with(:partial => "name")
        template.render :partial => "view_spec/spacer"
      end

      it "should not raise when stubbing and expecting and render has been received" do
        template.send(method, :render).with(:partial => "name")
        template.should_receive(:render).with(:partial => "name")
        template.render(:partial => "name")
      end
    end
    
    describe "#{method}(:helper_method)" do
      it "should not raise when stubbing and helper_method has been received" do
        template.send(method, :helper_method).with(:arg => "value")
        template.helper_method :arg => "value"
      end
    
      it "should not raise when stubbing and helper_method has NOT been received" do
        template.send(method, :helper_method).with(:arg => "value")
      end
    
      it "SHOULD raise when stubbing and helper_method has been received with different options" do
        template.send(method, :helper_method).with(:arg => "value")
        expect { template.helper_method :arg => "other_value" }.
          to raise_error(/undefined .* `helper_method'/)
      end
    
      it "should not raise when stubbing and expecting and helper_method has been received" do
        template.send(method, :helper_method).with(:arg => "value")
        template.should_receive(:helper_method).with(:arg => "value")
        template.helper_method(:arg => "value")
      end
    end
    
  end

end
