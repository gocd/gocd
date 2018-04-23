require_relative 'boot'

<% if include_all_railties? -%>
require 'rails/all'
<% else -%>
require "rails"
# Pick the frameworks you want:
require "active_model/railtie"
require "active_job/railtie"
<%= comment_if :skip_active_record %>require "active_record/railtie"
require "action_controller/railtie"
<%= comment_if :skip_action_mailer %>require "action_mailer/railtie"
require "action_view/railtie"
<%= comment_if :skip_action_cable %>require "action_cable/engine"
<%= comment_if :skip_sprockets %>require "sprockets/railtie"
<%= comment_if :skip_test %>require "rails/test_unit/railtie"
<% end -%>

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

module <%= app_const_base %>
  class Application < Rails::Application
    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults <%= Rails::VERSION::STRING.to_f %>

    # Settings in config/environments/* take precedence over those specified here.
    # Application configuration should go into files in config/initializers
    # -- all .rb files in that directory are automatically loaded.
<%- if options[:api] -%>

    # Only loads a smaller set of middleware suitable for API only apps.
    # Middleware like session, flash, cookies can be added back manually.
    # Skip views, helpers and assets when generating a new resource.
    config.api_only = true
<%- elsif !depends_on_system_test? -%>

    # Don't generate system test files.
    config.generators.system_tests = nil
<%- end -%>
  end
end
