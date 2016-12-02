if Gadgets.enabled?
  Rails.application.routes.draw do
    admin_prefix= (ENV['ADMIN_OAUTH_URL_PREFIX'] || "").gsub(%r{^/}, '').gsub(%r{/$}, '')
    user_prefix= (ENV['USER_OAUTH_URL_PREFIX'] || "").gsub(%r{^/}, '').gsub(%r{/$}, '')

    get "#{user_prefix}/gadgets/oauthcallback" => "gadget_oauth_callback#oauth_callback"
    get "#{user_prefix}/gadgets/ifr" => "gadget_rendering#ifr", as: :gadget_rendering
    post "#{user_prefix}/gadgets/makeRequest" => "gadget_make_request#make_request"
    get "#{user_prefix}/gadgets/proxy" => "gadget_proxy#proxy"
    get "#{user_prefix}/gadgets/concat" => "gadget_proxy#concat"
    get "#{user_prefix}/gadgets/js/:features.js" => "gadget_js_request#js"

    resources :gadgets_oauth_clients, :path => "/" + admin_prefix + (admin_prefix.blank? ? "" : "/") + "gadgets/oauth_clients"
  end
end
