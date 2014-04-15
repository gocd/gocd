# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

require 'oauth2/provider/a_r_datasource'
require 'oauth2/provider/in_memory_datasource'
require 'oauth2/provider/model_base'
require 'oauth2/provider/clock'
require 'oauth2/provider/url_parser'
require 'oauth2/provider/configuration'
require 'ext/validatable_ext'

Oauth2::Provider::ModelBase.datasource = ENV["OAUTH2_PROVIDER_DATASOURCE"]

Dir[File.join(File.dirname(__FILE__), "..", "app", "**", '*.rb')].each do |rb_file|
  require File.expand_path(rb_file)
end

