# frozen_string_literal: true

require "minitest/autorun"
require "rails"
require "rails/test_help"
require "byebug"

require "webpacker"

ENV["NODE_ENV"] ||= "production"

Webpacker.instance = Webpacker::Instance.new \
  root_path: Pathname.new(File.expand_path("../test_app", __FILE__)),
  config_path: Pathname.new(File.expand_path("../../lib/install/config/webpacker.yml", __FILE__))

module TestApp
  class Application < ::Rails::Application
    config.root = File.join(File.dirname(__FILE__), "test_app")
    config.eager_load = true
  end
end

class Webpacker::Test < Minitest::Test
  private
    def reloaded_config
      Webpacker.instance.instance_variable_set(:@config, nil)
      Webpacker.config
    end

    def with_node_env(env)
      original = ENV["NODE_ENV"]
      ENV["NODE_ENV"] = env
      yield
    ensure
      ENV["NODE_ENV"] = original
    end
end

Rails.backtrace_cleaner.remove_silencers!
TestApp::Application.initialize!
