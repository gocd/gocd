require File.expand_path(File.join(File.dirname(__FILE__),
                                   "..", "..", "spec_helper"))

describe "routes for admin scope" do
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

  describe "client routes" do
    it "should be scoped by admin prefix" do
      stub_const("ENV", ENV.update("ADMIN_OAUTH_URL_PREFIX" => "for-an-admin"))
      Rails.application.reload_routes!

      expect(get("/for-an-admin/clients")).
        to route_to("oauth2_provider/clients#index")
      expect(get("/for-an-admin/clients/new")).
        to route_to("oauth2_provider/clients#new")
    end

    it "should allow empty admin prefix" do
      stub_const("ENV", ENV.update("ADMIN_OAUTH_URL_PREFIX" => ""))
      Rails.application.reload_routes!

      expect(get("/clients")).to route_to("oauth2_provider/clients#index")
      expect(post("/clients")).to route_to("oauth2_provider/clients#create")
    end

    it "should allow non-existent admin prefix" do
      stub_const("ENV", ENV.update("ADMIN_OAUTH_URL_PREFIX" => nil))
      Rails.application.reload_routes!

      expect(get("/clients")).
        to route_to("oauth2_provider/clients#index")
      expect(get("/clients/1")).
        to route_to("oauth2_provider/clients#show", id: "1")
    end
  end

  describe "revoke-by-admin route" do
    it "should be scoped by admin prefix" do
      stub_const("ENV", ENV.update("ADMIN_OAUTH_URL_PREFIX" => "for-an-admin"))
      Rails.application.reload_routes!

      expect(delete("/for-an-admin/user_tokens/revoke_by_admin")).
        to route_to("oauth2_provider/user_tokens#revoke_by_admin")
    end
  end
end
