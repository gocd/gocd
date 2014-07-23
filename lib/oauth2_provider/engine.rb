require File.expand_path(File.join(File.dirname(__FILE__), 'a_r_datasource'))
require File.expand_path(File.join(File.dirname(__FILE__), 'application_controller_methods'))
require File.expand_path(File.join(File.dirname(__FILE__), 'clock'))
require File.expand_path(File.join(File.dirname(__FILE__), 'configuration'))
require File.expand_path(File.join(File.dirname(__FILE__), 'in_memory_datasource'))
require File.expand_path(File.join(File.dirname(__FILE__), 'model_base'))
require File.expand_path(File.join(File.dirname(__FILE__), 'ssl_helper'))
require File.expand_path(File.join(File.dirname(__FILE__), 'transaction_helper'))
require File.expand_path(File.join(File.dirname(__FILE__), 'url_parser'))
require File.expand_path(File.join(File.dirname(__FILE__), '..', 'ext', 'validatable_ext'))

module Oauth2Provider
  class Engine < ::Rails::Engine
    isolate_namespace Oauth2Provider
    engine_name 'oauth_engine'
    
    Oauth2Provider::ModelBase.datasource = ENV["OAUTH2_PROVIDER_DATASOURCE"]
    Dir[File.join(File.dirname(__FILE__), "..", "..", "app", "**", '*.rb')].each do |rb_file|
      require File.expand_path(rb_file)
    end
    config.generators do |g|
      g.test_framework :rspec
    end
  end
end
