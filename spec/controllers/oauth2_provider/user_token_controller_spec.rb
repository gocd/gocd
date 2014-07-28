require File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "spec_helper"))

module Oauth2Provider
  describe Oauth2Provider::UserTokensController do
    describe 'index' do
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
  
    describe 'revoke' do
      before :each do
        @token = create(Oauth2Provider::Token)
        allow(@controller).to receive(:current_user_id_for_oauth).and_return(@token.user_id)
      end
    
      it "should revoke token" do
        delete :revoke, {use_route: :oauth_engine}.merge({token_id: @token.id})
        expect(response).to redirect_to("/oauth2_provider/user_tokens")
      end
      
      it "should not revoke if unauthorized" do
        delete :revoke, {use_route: :oauth_engine}.merge({token_id: SecureRandom.hex(32)})
        expect(response.body).to eq("You are not authorized to perform this action!")
        expect(response.status).to eq(400)
      end
    end
  end
end