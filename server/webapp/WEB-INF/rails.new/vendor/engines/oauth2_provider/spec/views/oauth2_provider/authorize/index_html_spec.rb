require File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "..", "spec_helper"))

module Oauth2Provider
  describe 'oauth2_provider/authorize/index.html.erb' do
    before :each do
      @client = create(Oauth2Provider::Client)
      assigns[:client] = @client
      @oauth_engine = double('oauth engine')
      allow(view).to receive(:oauth_engine).and_return(@oauth_engine)
      @authorize_authorize_path = 'authorize_authorize_path'
      allow(@oauth_engine).to receive(:authorize_authorize_path).and_return(@authorize_authorize_path)
    end
    
    it "should display markup" do
      render
      expect(response).to have_selector("form#oauth_authorize_form[action='#{@authorize_authorize_path}'][method='post']")
      expect(response).to have_selector("div.label", text: "Do you wish to allow the service named '#{@client.name}' to access this application on your behalf?")
      expect(response).to have_selector("input#oauth_authorize_allow[value='No']")
    end
  end
end
