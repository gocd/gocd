module Oauth2Provider
  class ApplicationController < ::ApplicationController
    include Oauth2Provider::Engine.routes.url_helpers
  end
end