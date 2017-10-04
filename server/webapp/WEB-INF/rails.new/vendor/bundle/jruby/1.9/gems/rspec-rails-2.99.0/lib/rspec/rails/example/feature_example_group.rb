module RSpec::Rails
  module FeatureExampleGroup
    extend ActiveSupport::Concern
    include RSpec::Rails::RailsExampleGroup

    DEFAULT_HOST = "www.example.com"

    included do
      metadata[:type] = :feature

      app = ::Rails.application
      if app.respond_to?(:routes)
        include app.routes.url_helpers     if app.routes.respond_to?(:url_helpers)
        include app.routes.mounted_helpers if app.routes.respond_to?(:mounted_helpers)

        if respond_to?(:default_url_options)
          default_url_options[:host] ||= ::RSpec::Rails::FeatureExampleGroup::DEFAULT_HOST
        end
      end
    end

    def visit(*)
      if defined?(super)
        super
      else
        raise "Capybara not loaded, please add it to your Gemfile:\n\ngem \"capybara\""
      end
    end
  end
end
