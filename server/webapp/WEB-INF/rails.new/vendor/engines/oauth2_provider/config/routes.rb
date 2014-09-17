Oauth2Provider::Engine.routes.draw do
  root to: "clients#index"
  
  resources :clients
  
  post "/authorize" => "authorize#authorize", as: :authorize_authorize
  get "/authorize" => "authorize#index", as: :authorize_index
  
  post "/token" => "tokens#get_token", as: :tokens_get_token
  
  delete "/user_tokens/revoke_by_admin" => "user_tokens#revoke_by_admin"
  delete "/user_tokens/revoke/:token_id" => "user_tokens#revoke"
  get "/user_tokens" => "user_tokens#index", as: :user_tokens_index
end
