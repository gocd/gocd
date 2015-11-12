require File.expand_path('../boot', __FILE__)

require 'action_controller/railtie'
require 'action_view/railtie'
require 'sprockets/railtie'
require 'rails/test_unit/railtie'

Bundler.require(*Rails.groups)

module Go
  class Application < Rails::Application
    # Settings in config/environments/* take precedence over those specified here.
    # Application configuration should go into files in config/initializers
    # -- all .rb files in that directory are automatically loaded.

    # Set Time.zone default to the specified zone and make Active Record auto-convert to this zone.
    # Run "rake -D time" for a list of tasks for finding time zone names. Default is UTC.
    config.time_zone = 'UTC'

    # The default locale is :en and all translations from config/locales/*.rb,yml are auto loaded.
    # config.i18n.load_path += Dir[Rails.root.join('my', 'locales', '*.{rb,yml}').to_s]
    # config.i18n.default_locale = :de

    # Rails4 does not load lib/* by default. Forcing it to do so.
    config.autoload_paths += Dir[
        Rails.root.join('lib', '**/'),
        Rails.root.join('app', 'models', '**/'),
        Rails.root.join('app', 'presenters')
    ]

    # Replacement for "helper :all", used to make all helper methods available to controllers.
    config.action_controller.include_all_helpers = true

    # Add catch-all route, after all Rails routes and Engine routes are initialized.
    initializer :add_catch_all_route, :after => :add_routing_paths do |app|
      app.routes.append do
        match '*url', via: :all, to: 'application#unresolved'
      end
    end

    initializer "weak etag" do |app|
      app.middleware.use JettyWeakEtagMiddleware
    end
    initializer "catch json parser" do |app|
      app.middleware.insert_before ActionDispatch::ParamsParser, CatchJsonParseErrors
    end

    config.assets.enabled = true
    config.fail_if_unable_to_register_renderer = true

    require Rails.root.join("lib", "log4j_logger.rb")
    config.logger = Log4jLogger.new

    config.generators do |g|
      g.test_framework        :rspec, :fixture_replacement => nil
    end

  end
end
