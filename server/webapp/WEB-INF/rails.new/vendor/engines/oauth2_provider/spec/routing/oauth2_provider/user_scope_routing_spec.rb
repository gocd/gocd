require File.expand_path(File.join(File.dirname(__FILE__),
                                   "..", "..", "spec_helper"))

describe "routes for user scope" do
  routes { Oauth2Provider::Engine.routes }

  before :each do
    @old_admin_prefix = ENV["ADMIN_OAUTH_URL_PREFIX"]
    @old_user_prefix = ENV["USER_OAUTH_URL_PREFIX"]
  end

  after :each do
    ENV["ADMIN_OAUTH_URL_PREFIX"] = @old_admin_prefix
    ENV["USER_OAUTH_URL_PREFIX"] = @old_user_prefix

    Rails.application.reload_routes!
  end

  describe "authorize routes" do
    it "should be scoped by user prefix" do
      stub_const("ENV", ENV.update("USER_OAUTH_URL_PREFIX" => "for-a-user"))
      Rails.application.reload_routes!

      expect(get("/for-a-user/authorize")).
        to route_to("oauth2_provider/authorize#index")
      expect(post("/for-a-user/authorize")).
        to route_to("oauth2_provider/authorize#authorize")
    end

    it "should allow empty user prefix" do
      stub_const("ENV", ENV.update("USER_OAUTH_URL_PREFIX" => ""))
      Rails.application.reload_routes!

      expect(get("/authorize")).to route_to("oauth2_provider/authorize#index")
      expect(post("/authorize")).
        to route_to("oauth2_provider/authorize#authorize")
    end

    it "should allow non-existent user prefix" do
      stub_const("ENV", ENV.update("USER_OAUTH_URL_PREFIX" => nil))
      Rails.application.reload_routes!

      expect(get("/authorize")).to route_to("oauth2_provider/authorize#index")
      expect(post("/authorize")).
        to route_to("oauth2_provider/authorize#authorize")
    end
  end

  describe "token route" do
    it "should be scoped by user prefix" do
      stub_const("ENV", ENV.update("USER_OAUTH_URL_PREFIX" => "for-a-user"))
      Rails.application.reload_routes!

      expect(post("/for-a-user/token")).
        to route_to("oauth2_provider/tokens#get_token")
    end
  end

  describe "user tokens routes" do
    it "should be scoped by user prefix" do
      stub_const("ENV", ENV.update("USER_OAUTH_URL_PREFIX" => "for-a-user"))
      Rails.application.reload_routes!

      expect(get("/for-a-user/user_tokens")).
        to route_to("oauth2_provider/user_tokens#index")
      expect(delete("/for-a-user/user_tokens/revoke/123")).
        to route_to("oauth2_provider/user_tokens#revoke", token_id: "123")
    end
  end
end
