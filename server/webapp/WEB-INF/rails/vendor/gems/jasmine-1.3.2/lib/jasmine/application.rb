require 'rack'
require 'rack/utils'
require 'jasmine-core'
require 'rack/jasmine/runner'
require 'rack/jasmine/focused_suite'
require 'rack/jasmine/cache_control'
require 'ostruct'

module Jasmine
  class Application
    def self.app(config, builder = Rack::Builder.new)
      config.rack_apps.each do |(app, config_block)|
        builder.use(app, &config_block)
      end
      config.rack_path_map.each do |path, handler|
        builder.map(path) { run handler.call }
      end
      builder
    end
  end
end
