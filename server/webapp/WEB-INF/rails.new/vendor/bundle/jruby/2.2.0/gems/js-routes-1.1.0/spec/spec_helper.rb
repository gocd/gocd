# encoding: utf-8

$:.unshift(File.join(File.dirname(__FILE__), '..', 'lib'))
$:.unshift(File.dirname(__FILE__))
require 'rspec'
require 'rails/all'
require 'js-routes'
require "active_support/core_ext/hash/slice"
require 'coffee-script'
# fix ends_with? error for rails 3.2
require 'active_support/core_ext/string/starts_ends_with' if 3 == Rails::VERSION::MAJOR

if defined?(JRUBY_VERSION)
  require 'rhino'
  JS_LIB_CLASS = Rhino
else
  require "v8"
  JS_LIB_CLASS = V8
end

def jscontext(force = false)
  if force
    @jscontext = JS_LIB_CLASS::Context.new
  else
    @jscontext ||= JS_LIB_CLASS::Context.new
  end
end

def js_error_class
  JS_LIB_CLASS::JSError
end

def evaljs(string, force = false)
  jscontext(force).eval(string)
end

def routes
  App.routes.url_helpers
end

def blog_routes
  BlogEngine::Engine.routes.url_helpers
end


module BlogEngine
  class Engine < Rails::Engine
    isolate_namespace BlogEngine
  end

end


class App < Rails::Application
  # Enable the asset pipeline
  config.assets.enabled = true
  # initialize_on_precompile
  config.assets.initialize_on_precompile = true

  if 3 == Rails::VERSION::MAJOR
    config.paths['config/routes'] << 'spec/config/routes.rb'
  else
    config.paths['config/routes.rb'] << 'spec/config/routes.rb'
  end

  config.root = File.expand_path('../dummy', __FILE__)
end

def draw_routes

  BlogEngine::Engine.routes.draw do
    root to: "application#index"
    resources :posts
  end
  App.routes.draw do

    get 'support(/page/:page)', to: BlogEngine::Engine, as: 'support'

    resources :inboxes do
      resources :messages do
        resources :attachments
      end
    end

    root :to => "inboxes#index"

    namespace :admin do
      resources :users
    end

    scope "/returns/:return" do
      resources :objects
    end
    resources :returns

    scope "(/optional/:optional_id)" do
      resources :things
    end

    get "/other_optional/(:optional_id)" => "foo#foo", :as => :foo

    get 'books/*section/:title' => 'books#show', :as => :book
    get 'books/:title/*section' => 'books#show', :as => :book_title

    mount BlogEngine::Engine => "/blog", :as => :blog_app

    get '/no_format' => "foo#foo", :format => false, :as => :no_format

    get '/json_only' => "foo#foo", :format => true, :constraints => {:format => /json/}, :as => :json_only

    get '/привет' => "foo#foo", :as => :hello
    get '(/o/:organization)/search/:q' => "foo#foo", as: :search

    resources :sessions, :only => [:new, :create, :destroy], :protocol => 'https'

    get '/' => 'sso#login', host: 'sso.example.com', as: :sso

    resources :portals, :port => 8080
  end

end

# prevent warning
Rails.configuration.active_support.deprecation = :log

# Requires supporting files with custom matchers and macros, etc,
# in ./support/ and its subdirectories.
Dir["#{File.dirname(__FILE__)}/support/**/*.rb"].each {|f| require f}

RSpec.configure do |config|
  config.expect_with :rspec do |c|
    c.syntax = :expect
  end

  config.before(:all) do
    # compile all js files begin
    Dir["#{File.expand_path(File.join(File.dirname(__FILE__), "..", "lib"))}/**/*.coffee"].each do |coffee|
      File.open(coffee.gsub(/\.coffee$/, ""), 'w') do |f|
        f.write(CoffeeScript.compile(File.read(coffee)).lstrip)
      end
    end
    # compile all js files end
    draw_routes
  end

  config.before :each do
    evaljs("var window = this;", true)
    jscontext[:log] = lambda {|context, value| puts value.inspect}
  end
end
