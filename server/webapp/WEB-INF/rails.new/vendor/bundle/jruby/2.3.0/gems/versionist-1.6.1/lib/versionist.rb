require 'active_support/dependencies/autoload'

module Versionist
  extend ActiveSupport::Autoload

  autoload :Configuration
  autoload :InflectorFixes, "generators/versionist/inflector_fixes"
  autoload :RspecHelper, "generators/versionist/rspec_helper"
  autoload :CopyApiVersionGenerator, "generators/versionist/copy_api_version/copy_api_version_generator"
  autoload :NewApiVersionGenerator, "generators/versionist/new_api_version/new_api_version_generator"
  autoload :NewControllerGenerator, "generators/versionist/new_controller/new_controller_generator"
  autoload :NewPresenterGenerator, "generators/versionist/new_presenter/new_presenter_generator"
  autoload :VersioningStrategy, "versionist/versioning_strategy"
  autoload :Middleware
  autoload :Routing

  def self.configuration
    @@configuration ||= Configuration.new
  end

  def self.older_than_rails_5?
    defined?(Rails) && Rails.version.to_i < 5
  end

  def self.test_path
    return "test/functional" if older_than_rails_5?
    "test/controllers"
  end
end

require 'versionist/railtie' if defined?(Rails) && Rails::VERSION::MAJOR >= 3
