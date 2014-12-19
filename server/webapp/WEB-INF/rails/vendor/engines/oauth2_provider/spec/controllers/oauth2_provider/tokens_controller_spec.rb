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
    end
    
    describe 'get_token' do
      before :each do
        @authorization = create(Oauth2Provider::Authorization)
        @client = Oauth2Provider::Client.find_one(:id, @authorization.client_id)
        @valid_params = {client_id: @client.client_id, response_type: 'code', redirect_uri: @client.redirect_uri}
        @params = {code: @authorization.code, client_id: @client.client_id, 
          client_secret: @client.client_secret, redirect_uri: @client.redirect_uri}
      end
      
      it "should render error for invalid grant-type" do 
        post :get_token, {use_route: :oauth_engine}.merge(@params).merge({grant_type: 'random-string'})
        expect(response.status).to eq(400)
        json = JSON.parse(response.body)
        expect(json["error"]).to eq("unsupported-grant-type")
        expect(json["error_description"]).to eq("Grant type random-string is not supported!")
      end
      
      it "should get token for authorization-code" do
        post :get_token, {use_route: :oauth_engine}.merge(@params).merge({grant_type: 'authorization-code'})
        expect(response.status).to eq(200)
        json = JSON.parse(response.body)
        expect(json["access_token"]).to_not eq(nil)
        expect(json["expires_in"]).to_not eq(nil)
        expect(json["refresh_token"]).to_not eq(nil)
      end
      
      it "should get token for refresh-token" do
        old_token = create(Oauth2Provider::Token, client_id: @client.id)
        post :get_token, {use_route: :oauth_engine}.merge(@params).merge({grant_type: 'refresh-token', refresh_token: old_token.refresh_token})
        expect(response.status).to eq(200)
        json = JSON.parse(response.body)
        expect(json["access_token"]).to_not eq(old_token.access_token)
        expect(json["expires_in"]).to_not eq(nil)
        expect(json["refresh_token"]).to_not eq(old_token.refresh_token)
      end
      
      it "should render error if no authorization exists" do
        post :get_token, {use_route: :oauth_engine}.merge(@params).merge({grant_type: 'authorization-code', code: ''})
        expect(response.status).to eq(400)
        json = JSON.parse(response.body)
        expect(json["error"]).to eq("invalid-grant")
        expect(json["error_description"]).to eq("Authorization expired or invalid!")
      end
      
      it "should render error if no old token exist to refresh" do
        post :get_token, {use_route: :oauth_engine}.merge(@params).merge({grant_type: 'refresh-token'})
        expect(response.status).to eq(400)
        json = JSON.parse(response.body)
        expect(json["error"]).to eq("invalid-grant")
        expect(json["error_description"]).to eq("Refresh token is invalid!")
      end
      
      it "should render error for invalid client_id" do
        post :get_token, {use_route: :oauth_engine}.merge(@params).merge({grant_type: 'authorization-code', client_secret: ''})
        expect(response.status).to eq(400)
        json = JSON.parse(response.body)
        expect(json["error"]).to eq("invalid-client-credentials")
        expect(json["error_description"]).to eq("Invalid client credentials!")
      end
      
      it "should render error for invalid redirect_uri" do
        post :get_token, {use_route: :oauth_engine}.merge(@params).merge({grant_type: 'authorization-code', redirect_uri: ''})
        expect(response.status).to eq(400)
        json = JSON.parse(response.body)
        expect(json["error"]).to eq("invalid-grant")
        expect(json["error_description"]).to eq("Redirect uri mismatch!")
      end
    end
    
  end
end