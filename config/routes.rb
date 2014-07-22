Oauth2Provider::Engine.routes.draw do
  root to: "clients#index"
  
  resources :clients
  
  delete "/user_tokens/revoke_by_admin" => "user_tokens#revoke_by_admin"
  post "/authorize" => "authorize#authorize"
  get "/authorize" => "authorize#index"
  post "/token" => "tokens#get_token"
  delete "/user_tokens/revoke/:token_id" => "user_tokens#revoke"
  get "/user_tokens" => "user_tokens#index"
end
