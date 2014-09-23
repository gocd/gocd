require File.expand_path(File.join(
  File.dirname(__FILE__), "..", "..", "..", "spec_helper"))

module Oauth2Provider
  describe "oauth2_provider/user_tokens/index.html.erb" do
    before :each do
      @oauth_engine = double("oauth engine")
      allow(view).to receive(:oauth_engine).and_return(@oauth_engine)
    end

    it "should display message when there are no tokens" do
      @tokens = []

      render

      expect(response).to have_selector(".content_wrapper_inner .info-box",
                                        text: "There are no tokens.")
    end

    it "should display tokens when present" do
      token_1 = create(Oauth2Provider::Token)
      token_2 = create(Oauth2Provider::Token)
      @tokens = [token_1, token_2]

      expect(view).to receive(:url_for).
        with(action: :revoke,
             token_id: token_1.id,
             controller: "user_tokens").
        and_return("/revoke/token_1")

      expect(view).to receive(:url_for).
        with(action: :revoke,
             token_id: token_2.id,
             controller: "user_tokens").
        and_return("/revoke/token_2")

      render

      Capybara.string(response.body).all("table#oauth_user_token_table tr").
        tap do |rows|
          rows[1].all("td").tap do |columns|
            expect(columns[0].text).to eq(token_1.oauth_client.name)
            expect(columns[1]).
              to have_selector("a.delete_link[@href='/revoke/token_1']",
                               text: "Delete")
          end

          rows[2].all("td").tap do |columns|
            expect(columns[0].text).to eq(token_2.oauth_client.name)
            expect(columns[1]).
              to have_selector("a.delete_link[@href='/revoke/token_2']",
                               text: "Delete")
          end
        end
    end
  end
end
