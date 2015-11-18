require 'rails/railtie'

module Versionist
  class Railtie < Rails::Railtie
    config.versionist = ActiveSupport::OrderedOptions.new

    initializer 'versionist.configure' do |app|
      ActionDispatch::Routing::Mapper.send :include, Versionist::Routing
    end

    config.app_middleware.use Versionist::Middleware

    config.after_initialize do
      generators = config.respond_to?(:app_generators) ? config.app_generators : config.generators
      Versionist.configuration.configured_test_framework = generators.options[:rails][:test_framework]
    end
  end
end
