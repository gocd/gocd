ActionController::Routing::Routes.draw do |map|

  admin_prefix= ENV['ADMIN_OAUTH_URL_PREFIX']
  user_prefix= ENV['USER_OAUTH_URL_PREFIX']

  admin_prefix = "" if admin_prefix.blank?
  user_prefix = "" if user_prefix.blank?

  admin_prefix = admin_prefix.gsub(%r{^/}, '').gsub(%r{/$}, '')
  user_prefix = user_prefix.gsub(%r{^/}, '').gsub(%r{/$}, '')

  map.connect "#{user_prefix}/gadgets/oauthcallback", :controller => "gadget_oauth_callback", :action => 'oauth_callback'

  map.gadget_rendering "#{user_prefix}/gadgets/ifr", :controller => "gadget_rendering", :action => 'ifr'
  map.connect "#{user_prefix}/gadgets/makeRequest", :controller => "gadget_make_request", :action => 'make_request'
  map.connect "#{user_prefix}/gadgets/proxy", :controller => "gadget_proxy", :action => 'proxy'
  map.connect "#{user_prefix}/gadgets/concat", :controller => "gadget_proxy", :action => 'concat'
  map.connect "#{user_prefix}/gadgets/js/:features.js", :controller => 'gadget_js_request', :action => 'js'

  map.resources :gadgets_oauth_clients, :controller => 'gadgets_oauth_clients', :as => admin_prefix + (admin_prefix.blank? ? "" : "/") + "gadgets/oauth_clients"
end
