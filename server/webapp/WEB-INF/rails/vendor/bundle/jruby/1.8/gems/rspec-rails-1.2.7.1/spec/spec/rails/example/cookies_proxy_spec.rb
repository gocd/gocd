require File.dirname(__FILE__) + '/../../../spec_helper'

class CookiesProxyExamplesController < ActionController::Base
  def index
    cookies[:key] = cookies[:key]
    render :text => ""
  end
end

module Spec
  module Rails
    module Example
      describe CookiesProxy, :type => :controller do
        controller_name :cookies_proxy_examples
      
        describe "with a String key" do
        
          it "should accept a String value" do
            proxy = CookiesProxy.new(self)
            proxy['key'] = 'value'
            get :index
            if ::Rails::VERSION::STRING >= "2.3"
              proxy['key'].should == 'value'
            else
              proxy['key'].should == ['value']
            end
          end
          
          it "should accept a Hash value" do
            proxy = CookiesProxy.new(self)
            proxy['key'] = { :value => 'value', :expires => expiration = 1.hour.from_now, :path => path = '/path' }
            get :index
            if ::Rails::VERSION::STRING >= "2.3"
              proxy['key'].should == 'value'
            else
              proxy['key'].should == ['value']
              proxy['key'].value.should == ['value']
              proxy['key'].expires.should == expiration
              proxy['key'].path.should == path
            end
          end
            
        end
      
        describe "with a Symbol key" do
        
          it "should accept a String value" do
            proxy = CookiesProxy.new(self)
            proxy[:key] = 'value'
            get :index
            if ::Rails::VERSION::STRING >= "2.3"
              proxy[:key].should == 'value'
            else
              proxy[:key].should == ['value']
            end
          end

          it "should accept a Hash value" do
            proxy = CookiesProxy.new(self)
            proxy[:key] = { :value => 'value', :expires => expiration = 1.hour.from_now, :path => path = '/path' }
            get :index
            if ::Rails::VERSION::STRING >= "2.3"
              proxy[:key].should == 'value'
            else
              proxy[:key].should == ['value']
              proxy[:key].value.should == ['value']
              proxy[:key].expires.should == expiration
              proxy[:key].path.should == path
            end
          end

        end
    
        describe "#delete" do
          it "should delete from the response cookies" do
            proxy = CookiesProxy.new(self)
            response_cookies = mock('cookies')
            response.should_receive(:cookies).and_return(response_cookies)
            response_cookies.should_receive(:delete).with('key')
            proxy.delete :key
          end
        end
      end
    
    end
  end
end
