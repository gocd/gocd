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
      (1..5).each do
        c = create(Oauth2Provider::Client)
        puts c.inpect
      end
      get 'index', { use_route: :oauth_engine }
      puts "*"*80
      puts response.body
      puts "*"*80
    end
  end
end
