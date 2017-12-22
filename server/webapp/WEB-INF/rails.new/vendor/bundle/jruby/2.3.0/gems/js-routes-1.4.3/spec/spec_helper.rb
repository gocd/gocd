# encoding: utf-8

$:.unshift(File.join(File.dirname(__FILE__), '..', 'lib'))
$:.unshift(File.dirname(__FILE__))
require 'rspec'
require 'rails/all'
require 'js-routes'
require 'active_support/core_ext/hash/slice'
require 'coffee-script'
# fix ends_with? error for rails 3.2
require 'active_support/core_ext/string/starts_ends_with' if 3 == Rails::VERSION::MAJOR

if defined?(JRUBY_VERSION)
  require 'rhino'
  JS_LIB_CLASS = Rhino
else
  require 'v8'
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

def test_routes
  ::App.routes.url_helpers
end

def blog_routes
  BlogEngine::Engine.routes.url_helpers
end

ActiveSupport::Inflector.inflections do |inflect|
  inflect.irregular "budgie", "budgies"
end


module BlogEngine
  class Engine < Rails::Engine
    isolate_namespace BlogEngine
  end

end


class ::App < Rails::Application
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

    def inspectify(value)
      case value
      when V8::Array
        value.map do |v|
          inspectify(v)
        end
      when V8::Object
        value.to_h.map do |k,v|
          [k, inspectify(v)]
        end.to_h
      when String, nil, Integer, FalseClass, TrueClass
        value
      else
        raise "wtf #{value.class}?"
      end

    end
    jscontext[:log] = lambda do |context, value|
      puts inspectify(value).to_json
    end
  end
end
