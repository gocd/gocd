require File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "spec_helper"))

module Oauth2Provider
  describe Oauth2Provider::UserTokensController do
    describe "index" do
      before :each do
        @user_id = 42
        allow(@controller).to receive(:current_user_id_for_oauth).and_return(@user_id)
        (1..10).each do |i|
          create(Oauth2Provider::Token, user_id: @user_id)
        end
      end
      
      it "should return list of tokens for a user" do
        get :index, {use_route: :oauth_engine}
        tokens = assigns[:tokens]
        expect(tokens.size).to eq(10)
      end
    end
  
    describe "revoke" do
      before :each do
        @token = create(Oauth2Provider::Token)
        allow(@controller).to receive(:current_user_id_for_oauth).and_return(@token.user_id)
      end
    
      it "should revoke token" do
        delete :revoke, {use_route: :oauth_engine}.merge({token_id: @token.id})
        expect(response).to redirect_to("/oauth2_provider/for-user/user_tokens")
      end
      
      it "should not revoke if token is invalid" do
        another_token = create(Oauth2Provider::Token)
        delete :revoke, {use_route: :oauth_engine}.merge({token_id: another_token.id})
        expect(response.body).to eq("You are not authorized to perform this action!")
        expect(response.status).to eq(400)
      end
      
      it "should not revoke if token does not belong to the user" do
        another_token = create(Oauth2Provider::Token)
        delete :revoke, {use_route: :oauth_engine}.merge({token_id: another_token.user_id})
        expect(response.body).to eq("You are not authorized to perform this action!")
        expect(response.status).to eq(400)
      end
    end
    
    describe "revoke_by_admin" do
      before :each do
        @token = create(Oauth2Provider::Token)
        allow(@controller).to receive(:current_user_id_for_oauth).and_return(@token.user_id)
      end
      
      it "should not revoke if params are empty" do
        delete :revoke_by_admin, {use_route: :oauth_engine}
        expect(response.body).to eq("You are not authorized to perform this action!")
        expect(response.status).to eq(400)
      end
      
      it "should not revoke if token is invalid" do
        random_token_id = SecureRandom.hex(32)
        delete :revoke_by_admin, {use_route: :oauth_engine}.merge({user_id: @token.user_id, token_id: random_token_id})
        expect(response.body).to eq("You are not authorized to perform this action!")
        expect(response.status).to eq(400)
      end
      
      it "should revoke token by admin" do
        delete :revoke_by_admin, {use_route: :oauth_engine}.merge({user_id: @token.user_id, token_id: @token.id})
        expect(response).to redirect_to("/oauth2_provider/for-user/user_tokens")
        expect(flash[:notice]).to eq("OAuth access token was successfully deleted.")
      end
      
      it "should revoke all tokens for user" do
        delete :revoke_by_admin, {use_route: :oauth_engine}.merge({user_id: @token.user_id})
        expect(response).to redirect_to("/oauth2_provider/for-user/user_tokens")
        expect(flash[:notice]).to eq("OAuth access token was successfully deleted.")
      end
    end
  end
end
