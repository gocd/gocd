# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

Rails.application.routes.draw do
  admin_prefix= ENV['ADMIN_OAUTH_URL_PREFIX']
  user_prefix= ENV['USER_OAUTH_URL_PREFIX']

  admin_prefix = "" if admin_prefix.blank?
  user_prefix = "" if user_prefix.blank?

  admin_prefix = admin_prefix.gsub(%r{^/}, '').gsub(%r{/$}, '')
  user_prefix = user_prefix.gsub(%r{^/}, '').gsub(%r{/$}, '')

  delete "#{admin_prefix}/oauth/user_tokens/revoke_by_admin" => "oauth_user_tokens#revoke_by_admin"
  post "#{user_prefix}/oauth/authorize" => "oauth_authorize#authorize"
  get "#{user_prefix}/oauth/authorize" => "oauth_authorize#index"
  post "#{user_prefix}/oauth/token" => "oauth_token#get_token"
  delete "#{user_prefix}/oauth/user_tokens/revoke/:token_id" => "oauth_user_tokens#revoke"
  get "#{user_prefix}/oauth/user_tokens" => "oauth_user_tokens#index"

  oauth_client_route = admin_prefix + (admin_prefix.blank? ? "" : "/") + "oauth/clients"
  get "#{oauth_client_route}" => "oauth_clients#index"
  get "#{oauth_client_route}/new" => "oauth_clients#new"
  post "#{oauth_client_route}" => "oauth_clients#create"
  get "#{oauth_client_route}/:id/edit" => "oauth_clients#edit"
  put "#{oauth_client_route}/:id" => "oauth_clients#update"
  delete "#{oauth_client_route}/:id" => "oauth_clients#destroy"
end
