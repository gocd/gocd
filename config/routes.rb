Oauth2Provider::Engine.routes.draw do
  root to: "clients#index"

  get "clients" => "clients#index", as: :clients
  get "clients/new" => "clients#new", as: :clients_new
end
