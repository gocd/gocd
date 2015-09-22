Go::Application.configure do
  config.dev_tweaks.log_autoload_notice = false

  # Settings specified here will take precedence over those in config/application.rb.

  # In the development environment your application's code is reloaded on
  # every request. This slows down response time but is perfect for development
  # since you don't have to restart the web server when you make code changes.
  config.cache_classes = false

  # Do not eager load code on boot.
  config.eager_load = false

  # Show full error reports and disable caching.
  config.consider_all_requests_local       = true
  config.action_controller.perform_caching = false

  # Don't care if the mailer can't send.
  # config.action_mailer.raise_delivery_errors = false
  config.serve_static_assets = true

  # Print deprecation notices to the Rails logger.
  # config.active_support.deprecation = :log
  # Debug mode disables concatenation and preprocessing of assets.
  # This option may cause significant delays in view rendering with a large
  # number of complex assets.
  config.assets.debug = false
  config.assets.digest = false
  config.assets.raise_runtime_errors = true

  config.java_services_cache = :ServiceCache
  config.fail_if_unable_to_register_renderer = false

  config.log_level = :debug
  org.apache.log4j.Logger.getLogger("com.thoughtworks.go.server.Rails").setLevel(org.apache.log4j.Level::DEBUG)
end

if defined?(JasmineRails) && File.exist?(Rails.root.join('spec/new_javascripts'))
  Rails.application.config.assets.paths << 'spec/new_javascripts'
else
  puts 'Seems there was an error - could not find jasmine rails or javascript specs!'
end
