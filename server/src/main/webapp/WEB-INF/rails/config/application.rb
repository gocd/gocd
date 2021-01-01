#
# Copyright 2021 ThoughtWorks, Inc.
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
#

require_relative 'boot'

require "rails"
require "active_model/railtie"
require "action_controller/railtie"
require "action_view/railtie"
require "sprockets/railtie"
require "rails/test_unit/railtie"

Bundler.require(*Rails.groups)

module Go
  class Application < Rails::Application
    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 5.2
    require_relative '../lib/all_libs'

    # Settings in config/environments/* take precedence over those specified here.
    # Application configuration can go into files in config/initializers
    # -- all .rb files in that directory are automatically loaded after loading
    # the framework and any gems in your application.

    # The default locale is :en and all translations from config/locales/*.rb,yml are auto loaded.
    # config.i18n.load_path += Dir[Rails.root.join('my', 'locales', '*.{rb,yml}').to_s]
    # config.i18n.default_locale = :de

    # Rails4 does not load lib/* by default. Forcing it to do so.
    config.autoload_paths += Dir[
        Rails.root.join('lib', '**/'),
        Rails.root.join('app', 'models', '**/'),
        Rails.root.join('app', 'presenters')
    ]

    # Add catch-all route, after all Rails routes and Engine routes are initialized.
    initializer :add_catch_all_route, :after => :add_routing_paths do |app|
      app.routes.append do
        match '*url', via: :all, to: 'application#unresolved'
      end
    end

    require Rails.root.join("lib", "log4j_logger.rb")
    config.logger = Log4jLogger::Logger.new('com.thoughtworks.go.server.Rails')

    config.generators do |g|
      g.test_framework        :rspec, :fixture_replacement => nil
    end

    config.action_controller.include_all_helpers = true
    config.action_controller.per_form_csrf_tokens = false

    # Disable default headers in rails, since they are added from other filters like DefaultHeadersFilter
    config.action_dispatch.default_headers = {}
  end
end
