Oauth2Provider::Engine.routes.draw do
  root to: "clients#index"

  admin_prefix = (ENV["ADMIN_OAUTH_URL_PREFIX"] || "").
    gsub(%r{^/}, "").
    gsub(%r{/$}, "").
    gsub(%r{(.+)}, '/\1/')
  user_prefix = (ENV["USER_OAUTH_URL_PREFIX"] || "").
    gsub(%r{^/}, "").
    gsub(%r{/$}, "").
    gsub(%r{(.+)}, '/\1/')

  scope admin_prefix do
    resources :clients
    delete "/user_tokens/revoke_by_admin" => "user_tokens#revoke_by_admin"
  end

  scope user_prefix do
    post "/authorize" => "authorize#authorize", as: :authorize_authorize
    get "/authorize" => "authorize#index", as: :authorize_index

    post "/token" => "tokens#get_token", as: :tokens_get_token

    delete "/user_tokens/revoke/:token_id" => "user_tokens#revoke", as: :user_tokens_revoke
    get "/user_tokens" => "user_tokens#index", as: :user_tokens_index
  end
end
