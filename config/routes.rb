Rails.application.routes.draw do # Cannot be "Oauth2Provider::Engine.routes.draw" because we want these routes to be used from the main application
  
  admin_prefix = (ENV['ADMIN_OAUTH_URL_PREFIX'] || "").gsub(%r{^/}, '').gsub(%r{/$}, '')
  user_prefix = (ENV['USER_OAUTH_URL_PREFIX'] || "").gsub(%r{^/}, '').gsub(%r{/$}, '')
  
  scope "#{admin_prefix}#{admin_prefix.blank? ? "" : "/"}" do
    namespace :oauth do
      resources :clients
    end
  end
end
