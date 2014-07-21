Rails.application.routes.draw do
  admin_prefix= (ENV['ADMIN_OAUTH_URL_PREFIX'] || "").gsub(%r{^/}, '').gsub(%r{/$}, '')
  user_prefix= (ENV['USER_OAUTH_URL_PREFIX'] || "").gsub(%r{^/}, '').gsub(%r{/$}, '')

  get "#{user_prefix}/gadgets/oauthcallback" => "gadget_oauth_callback#oauth_callback"
  get "#{user_prefix}/gadgets/ifr" => "gadget_rendering#ifr", as: :gadget_rendering
  get "#{user_prefix}/gadgets/makeRequest" => "gadget_make_request#make_request"
  get "#{user_prefix}/gadgets/proxy" => "gadget_proxy#proxy"
  get "#{user_prefix}/gadgets/concat" => "gadget_proxy#concat"
  get "#{user_prefix}/gadgets/js/:features.js" => "gadget_js_request#js"

  gadget_oauth_client_route = admin_prefix + (admin_prefix.blank? ? "" : "/") + "gadgets/oauth_clients"
  get "#{gadget_oauth_client_route}" => "gadgets_oauth_clients#index"
  get "#{gadget_oauth_client_route}/new" => "gadgets_oauth_clients#new"
  post "#{gadget_oauth_client_route}" => "gadgets_oauth_clients#create"
  get "#{gadget_oauth_client_route}/:id/edit" => "gadgets_oauth_clients#edit"
  put "#{gadget_oauth_client_route}/:id" => "gadgets_oauth_clients#update"
  delete "#{gadget_oauth_client_route}/:id" => "gadgets_oauth_clients#destroy"
end