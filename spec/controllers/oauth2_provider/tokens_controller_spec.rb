require File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "spec_helper"))

module Oauth2Provider
  describe Oauth2Provider::TokensController do
    before :each do
      module Oauth2Provider::SslHelper
        def mandatory_ssl
          true
        end
      end
      @params = {redirect_uri: "http://test.host/redirect/here"}
      @client = create(Oauth2Provider::Client)
      @valid_params = {client_id: @client.id, response_type: 'code', redirect_uri: @client.redirect_uri}
    end
    
    describe 'get_token' do
      before :each do
        @authorization = create(Oauth2Provider::Authorization)
        @client = Oauth2Provider::Client.find_by_id(@authorization.client_id)
        @params = {grant_type: 'authorization-code'}
      end
      
      it "should get token" do
        post :get_token, {use_route: :oauth_engine}
      end
    end
    
  end
end