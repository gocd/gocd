require 'spec_helper'

module Oauth2Provider
  describe Oauth2Provider::ClientsController do
    it "should foo" do
      get :index, { use_route: :oauth_engine }
    end
  end
end
