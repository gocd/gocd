Rails.application.routes.draw do

  mount Oauth2Provider::Engine => "/oauth2_provider", :as => :oauth_engine
end
