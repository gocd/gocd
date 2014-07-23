require File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "spec_helper"))

module Oauth2Provider
  describe Oauth2Provider::ClientsController do
    before :each do
      module Oauth2Provider::SslHelper
        def mandatory_ssl
          true
        end
      end
    end
    
    it "should foo" do
      expected_clients = []
      (1..5).each do
        expected_clients << create(Oauth2Provider::Client)
      end
      get 'index', { use_route: :oauth_engine }
      clients = assigns[:oauth_clients]
      clients.size.should == 5
      clients.map(&:name) == expected_clients.map(&:name)
    end
  end
end
