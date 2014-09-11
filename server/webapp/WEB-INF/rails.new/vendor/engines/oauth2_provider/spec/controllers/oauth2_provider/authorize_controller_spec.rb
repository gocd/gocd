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
      @valid_params = {client_id: @client.client_id, response_type: 'code', redirect_uri: @client.redirect_uri}
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
        post :authorize, {use_route: :oauth_engine}.merge({client_id: @client.client_id, response_type: 'code'}.merge(@params))
        expect(response).to redirect_to("#{@params[:redirect_uri]}?error=redirect-uri-mismatch")
      end
    end
    
    describe 'authorize' do
      before :each do 
        @authorized_params = @valid_params.merge(authorize: "Yes")
        allow(@controller).to receive(:current_user_id_for_oauth).and_return("1")
      end
      
      it "should return if unauthorized" do
        post :authorize, {use_route: :oauth_engine}.merge(@valid_params)
        expect(response).to redirect_to("#{@valid_params[:redirect_uri]}?error=access-denied")
      end
      
      it "should create an authorization" do
        post :authorize, {use_route: :oauth_engine}.merge(@authorized_params)
        actual = assigns[:authorization]
        expect(response).to redirect_to("#{@authorized_params[:redirect_uri]}?code=#{actual.code}&expires_in=#{actual.expires_in}")
      end
      
      it "should create an authorization with state_param" do
        state_param = 'foo'
        post :authorize, {use_route: :oauth_engine, state: state_param}.merge(@authorized_params)
        actual = assigns[:authorization]
        expect(response).to redirect_to("#{@authorized_params[:redirect_uri]}?code=#{actual.code}&expires_in=#{actual.expires_in}&state=#{state_param}")
      end
    end
    
    describe 'index' do
      it "should return if params invalid" do
        get :index, {use_route: :oauth_engine}
        expect(response.status).to eq(302)
      end      
    end
  end
end
