module Jasmine
  class AssetExpander
    def initialize(bundled_asset_factory, asset_path_for)
      @bundled_asset_factory = bundled_asset_factory
      @asset_path_for = asset_path_for
    end

    def expand(src_dir, src_path)
      pathname = src_path.gsub(/^\/?assets\//, '').gsub(/\.js$/, '')
      bundled_asset = @bundled_asset_factory.call(pathname, 'js')
      return nil unless bundled_asset

      bundled_asset.to_a.map do |asset|
        "/#{@asset_path_for.call(asset).gsub(/^\//, '')}?body=true"
      end.flatten
    end
  end
end
