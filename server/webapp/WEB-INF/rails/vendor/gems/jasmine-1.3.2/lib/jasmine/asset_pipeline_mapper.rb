module Jasmine
  class AssetPipelineMapper

    def initialize(config, asset_expander)
      @config = config
      @asset_expander = asset_expander
    end

    def map_src_paths(src_paths)
      src_paths.map do |src_path|
        @asset_expander.call(@config.src_dir, src_path) || src_path
      end.flatten.uniq
    end

  end
end
