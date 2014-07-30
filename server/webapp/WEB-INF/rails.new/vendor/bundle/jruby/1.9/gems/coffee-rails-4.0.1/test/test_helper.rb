# Configure Rails Envinronment
ENV["RAILS_ENV"] = "test"

require 'bundler/setup'
require 'rails'
require "rails/test_help"

# For generators
require 'rails/generators/test_case'

def copy_routes
  routes = File.expand_path("../support/routes.rb", __FILE__)
  destination = File.join(destination_root, "config")

  FileUtils.mkdir_p(destination)
  FileUtils.cp routes, destination
end
