# Rails.application.routes.draw do # Cannot be "Oauth2Provider::Engine.routes.draw" because we want these routes to be used from the main application
Oauth2Provider::Engine.routes.draw do  
  resources :clients
end
