require File.expand_path('../boot', __FILE__)

require "action_controller/railtie"
require "rails"
require "oauth2_provider"
require "dynamic_form"
require "gadgets"
require "sprockets/railtie"
require "sass"

module Go
  class Application < Rails::Application
    # Settings in config/environments/* take precedence over those specified here.
    # Application configuration should go into files in config/initializers
    # -- all .rb files in that directory are automatically loaded.

    # Set Time.zone default to the specified zone and make Active Record auto-convert to this zone.
    # Run "rake -D time" for a list of tasks for finding time zone names. Default is UTC.
    # config.time_zone = 'Central Time (US & Canada)'

    # The default locale is :en and all translations from config/locales/*.rb,yml are auto loaded.
    # config.i18n.load_path += Dir[Rails.root.join('my', 'locales', '*.{rb,yml}').to_s]
    # config.i18n.default_locale = :de

    # Rails4 does not load lib/* by default. Forcing it to do so.
    config.autoload_paths += Dir[Rails.root.join('lib', '**/'), Rails.root.join('app', 'models', '**/')]

    # Replacement for "helper :all", used to make all helper methods available to controllers.
    config.action_controller.include_all_helpers = true

    # Add catch-all route, after all Rails routes and Engine routes are initialized.
    initializer :add_catch_all_route, :after => :add_routing_paths do |app|
      app.routes.append do
        match '*url', via: :all, to: 'application#unresolved'
      end
    end
    config.assets.enabled = true
    config.serve_static_assets = true
  end
end
