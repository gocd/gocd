require 'sass'
require 'sprockets/sass_functions'

module Sprockets
  module SassFunctions
    def asset_data_url(path)
      Sass::Script::String.new("url(" + sprockets_context.asset_data_uri(path.value) + ")")
    end
  end
end

::Sass::Script::Functions.send :include, Sprockets::SassFunctions
