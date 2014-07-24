require File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "spec_helper"))

module Oauth2Provider
  describe Oauth2Provider::AuthorizeController do
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
    
    describe 'validate_params' do
      it "should return if client_id is missing" do
        post :authorize, {use_route: :oauth_engine}.merge(@params)
        expect(response).to redirect_to("#{@params[:redirect_uri]}?error=invalid-request")
      end
      
      it "should return if response_type is missing" do
        post :authorize, {use_route: :oauth_engine, client_id: 'fooo'}.merge(@params)
        expect(response).to redirect_to("#{@params[:redirect_uri]}?error=invalid-request")
      end
      
      it "should return if invalid response type" do
        post :authorize, {use_route: :oauth_engine, client_id: 'fooo', response_type: 'not_code'}.merge(@params)
        expect(response).to redirect_to("#{@params[:redirect_uri]}?error=unsupported-response-type")
      end
      
      it "should return if redirect_uri is missing" do
        post :authorize, {use_route: :oauth_engine, client_id: 'fooo', response_type: 'code'}
        expect(response.status).to eq(400)
        expect(response.body).to eq("You did not specify the 'redirect_uri' parameter!")
      end
      
      it "should return if client is nil" do
        post :authorize, {use_route: :oauth_engine, client_id: 'fooo', response_type: 'code'}.merge(@params)
        expect(response).to redirect_to("#{@params[:redirect_uri]}?error=invalid-client-id")
      end
      
      it "should return if redirect_uri mismatches" do
        post :authorize, {use_route: :oauth_engine}.merge({client_id: @client.id, response_type: 'code'}.merge(@params))
        expect(response).to redirect_to("#{@params[:redirect_uri]}?error=redirect-uri-mismatch")
      end

    end
  end
end
