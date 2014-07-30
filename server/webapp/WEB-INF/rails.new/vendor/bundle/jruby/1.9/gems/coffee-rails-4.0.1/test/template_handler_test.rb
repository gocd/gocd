require 'test_helper'
require 'action_controller'
require 'coffee-rails'

class SiteController < ActionController::Base
  self.view_paths = File.expand_path("../support", __FILE__)
end

DummyApp = ActionDispatch::Routing::RouteSet.new
DummyApp.draw do
  get "site/index"
end

class TemplateHandlerTest < ActiveSupport::TestCase
  include Rack::Test::Methods

  def app
    @app ||= DummyApp
  end

  test "coffee views are served as javascript" do
    get "/site/index.js"

    assert_match "alert('hello world');\n", last_response.body
  end
end
