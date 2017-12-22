require 'sass'
require 'sprockets/sass_functions'

module Sprockets
  module SassFunctions
    remove_method :asset_data_url if method_defined?(:asset_data_url)
    def asset_data_url(path)
      Sass::Script::String.new("url(" + sprockets_context.asset_data_uri(path.value) + ")")
    end
  end
end

::Sass::Script::Functions.send :include, Sprockets::SassFunctions
