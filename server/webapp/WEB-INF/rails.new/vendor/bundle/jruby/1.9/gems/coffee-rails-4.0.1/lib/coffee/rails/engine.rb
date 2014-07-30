require 'rails/engine'

module Coffee
  module Rails
    class Engine < ::Rails::Engine
      config.app_generators.javascript_engine :coffee
    end
  end
end
