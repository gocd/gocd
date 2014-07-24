class Oauth2Provider::ApplicationController < ActionController::Base
  include Oauth2Provider::Engine.routes.url_helpers
end