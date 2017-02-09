require 'action_view'
require 'webpack/rails/manifest'

module Webpack
  module Rails
    # Asset path helpers for use with webpack
    module Helper
      # Return asset paths for a particular webpack entry point.
      #
      # Response may either be full URLs (eg http://localhost/...) if the dev server
      # is in use or a host-relative URl (eg /webpack/...) if assets are precompiled.
      #
      # Will raise an error if our manifest can't be found or the entry point does
      # not exist.
      def webpack_asset_paths(source, extension: nil)
        return "" unless source.present?

        paths = Webpack::Rails::Manifest.asset_paths(source)
        paths = paths.select { |p| p.ends_with? ".#{extension}" } if extension

        port = ::Rails.configuration.webpack.dev_server.port
        protocol = ::Rails.configuration.webpack.dev_server.https ? 'https' : 'http'

        host = ::Rails.configuration.webpack.dev_server.host
        host = instance_eval(&host) if host.respond_to?(:call)

        if ::Rails.configuration.webpack.dev_server.enabled
          paths.map! do |p|
            "#{protocol}://#{host}:#{port}#{p}"
          end
        end

        paths
      end
    end
  end
end
