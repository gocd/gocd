require 'sprockets/sass_functions'
require 'active_support/deprecation'

module Sprockets
  module SassFunctions
    if instance_methods.map(&:to_sym).include?(:asset_path)
      undef_method :asset_path
    end

    def asset_path(path, kind = nil)
      ActiveSupport::Deprecation.warn "asset_path with two arguments is deprecated. Use asset_path(#{path}) instead." if kind

      Sass::Script::String.new(sprockets_context.asset_path(path.value), :string)
    end

    if instance_methods.map(&:to_sym).include?(:asset_url)
      undef_method :asset_url
    end

    def asset_url(path, kind = nil)
      ActiveSupport::Deprecation.warn "asset_url with two arguments is deprecated. Use asset_url(#{path}) instead." if kind

      Sass::Script::String.new("url(" + sprockets_context.asset_path(path.value) + ")")
    end

    def asset_data_url(path)
      Sass::Script::String.new("url(" + sprockets_context.asset_data_uri(path.value) + ")")
    end
  end
end
