module Jasmine
  class AssetExpander
    def expand(src_dir, src_path)
      pathname = src_path.gsub(/^\/?assets\//, '').gsub(/\.js$/, '')

      asset_bundle.assets(pathname).flat_map { |asset|
        "/#{asset.gsub(/^\//, '')}?body=true"
      }
    end

    private

    UnsupportedRailsVersion = Class.new(StandardError)

    def asset_bundle
      return Rails3AssetBundle.new if Jasmine::Dependencies.rails3?
      return Rails4AssetBundle.new if Jasmine::Dependencies.rails4?
      raise UnsupportedRailsVersion, "Jasmine only supports the asset pipeline for Rails 3 or 4"
    end

    class Rails3AssetBundle
      def assets(pathname)
        context.asset_paths.asset_for(pathname, nil).to_a.map do |path|
          context.asset_path(path)
        end
      end

      private

      def context
        @context ||= get_asset_context
      end

      def get_asset_context
        context = ::Rails.application.assets.context_class
        context.extend(::Sprockets::Helpers::IsolatedHelper)
        context.extend(::Sprockets::Helpers::RailsHelper)
      end
    end

    class Rails4AssetBundle
      def assets(pathname)
        context.get_original_assets(pathname)
      end

      private

      def context
        @context ||= ActionView::Base.new.extend(GetOriginalAssetsHelper)
      end

      module GetOriginalAssetsHelper
        def get_original_assets(pathname)
          assets_environment.find_asset(pathname).to_a.map do |processed_asset|
            case processed_asset.content_type
            when "text/css"
              path_to_stylesheet(processed_asset.logical_path, debug: true)
            when "application/javascript"
              path_to_javascript(processed_asset.logical_path, debug: true)
            end
          end
        end
      end
    end
  end
end
