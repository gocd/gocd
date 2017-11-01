##########################GO-LICENSE-START################################
# Copyright 2017 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################
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

    #Set up rate limiting
    require "encryption_api_rate_limiter"
    config.middleware.use EncryptionApiRateLimiter, {max_per_minute: com.thoughtworks.go.util.SystemEnvironment.new.getMaxEncryptionAPIRequestsPerMinute()}

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

    require Rails.root.join("lib", "log4j_logger.rb")
    config.logger = Log4jLogger::Logger.new('com.thoughtworks.go.server.Rails')

    config.generators do |g|
      g.test_framework        :rspec, :fixture_replacement => nil
    end

  end
end