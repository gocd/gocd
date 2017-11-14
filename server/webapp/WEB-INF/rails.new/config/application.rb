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

require "rails"
require "active_model/railtie"
require "action_controller/railtie"
require "action_view/railtie"
require "sprockets/railtie"
require "rails/test_unit/railtie"

Bundler.require(*Rails.groups)

module Go
  class Application < Rails::Application
    require_relative '../lib/all_libs'
    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 5.1

    # Settings in config/environments/* take precedence over those specified here.
    # Application configuration should go into files in config/initializers
    # -- all .rb files in that directory are automatically loaded.

    config.autoload_paths += Dir[
      Rails.root.join('app', 'controller_helpers'),
      Rails.root.join('app', 'presenters')
    ]

    config.logger = Log4jLogger::Logger.new('com.thoughtworks.go.server.Rails')

    # Add catch-all route, after all Rails routes and Engine routes are initialized.
    initializer :add_catch_all_route, :after => :add_routing_paths do |app|
      app.routes.append do
        match '*url', via: :all, to: 'application#unresolved'
      end
    end

    initializer "weak etag" do |app|
      app.middleware.use JettyWeakEtagMiddleware
    end

    config.action_controller.per_form_csrf_tokens = false
  end
end
