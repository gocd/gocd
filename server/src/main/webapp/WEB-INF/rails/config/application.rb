#
# Copyright 2023 Thoughtworks, Inc.
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

require_relative "boot"

require "rails"
require "active_model/railtie"
require "action_controller/railtie"
require "action_view/railtie"
require "sprockets/railtie"
require "rails/test_unit/railtie"

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups(assets: %w[development test]))

module Go
  class Application < Rails::Application
    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 7.0
    require_relative "../lib/all_libs"

    # Configuration for the application, engines, and railties goes here.
    #
    # These settings can be overridden in specific environments using the files
    # in config/environments, which are processed later.
    #
    # config.time_zone = "Central Time (US & Canada)"
    # config.eager_load_paths << Rails.root.join("extras")


    # Rails4 does not load lib/* by default. Forcing it to do so.
    config.autoload_paths += Dir[
      Rails.root.join("lib"),
      Rails.root.join("app", "models"),
      Rails.root.join("app", "presenters")
    ]

    # Add catch-all route, after all Rails routes and Engine routes are initialized.
    initializer :add_catch_all_route, :after => :add_routing_paths do |app|
      app.routes.append do
        match "*url", via: :all, to: "application#unresolved"
      end
    end

    require Rails.root.join("lib", "slf4j_logger.rb")
    config.logger = Slf4jLogger::Logger.new("com.thoughtworks.go.server.Rails")

    config.generators do |g|
      g.test_framework        :rspec, :fixture_replacement => nil
    end

    config.action_controller.include_all_helpers  = true
    config.action_controller.per_form_csrf_tokens = false

    # Disable default headers in rails, since they are added from other filters like DefaultHeadersFilter
    config.action_dispatch.default_headers = {}
  end
end
