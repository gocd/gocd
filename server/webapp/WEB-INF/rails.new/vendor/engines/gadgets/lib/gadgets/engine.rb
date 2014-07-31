module Gadgets
  class Engine < ::Rails::Engine
    require 'active_model'
    require 'active_model/validations'
    require 'active_model/errors'
    require 'ext/validatable_ext.rb'
    require 'ext/ssl_certificate.rb'
    require 'gadgets'

    if RUBY_PLATFORM =~ /java/
      java.lang.System.setProperty('java.awt.headless', 'true')
      require 'shindig'
    end

    ['app/models', 'app/controllers'].each do |dir|
      Dir[File.join(File.dirname(__FILE__), dir, '**', '*.rb')].each do |f|
        require f
      end
    end

    config.autoload_paths << File.expand_path("..", __FILE__)
  end
end
