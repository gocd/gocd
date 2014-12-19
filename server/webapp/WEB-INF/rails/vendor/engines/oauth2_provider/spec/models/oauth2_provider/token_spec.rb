require File.expand_path(File.join(
  File.dirname(__FILE__), "..", "..", "spec_helper"))

module Oauth2Provider
  describe Oauth2Provider::Token do
    it "should know its oauth client" do
      t = create(Oauth2Provider::Token)
      expect(t.oauth_client.id).to eq(t.client_id.to_i)
    end
  end
end
