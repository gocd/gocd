module Jasmine
  class AssetPipelineUtility
    def self.bundled_asset_factory(pathname, ext)
      context.asset_paths.asset_for(pathname, 'js')
    end

    def self.asset_path_for(filepath)
      context.asset_path(filepath)
    end

    def self.context
      return @context if @context
      @context = ::Rails.application.assets.context_class
      @context.extend(::Sprockets::Helpers::IsolatedHelper)
      @context.extend(::Sprockets::Helpers::RailsHelper)
    end

  end
end
