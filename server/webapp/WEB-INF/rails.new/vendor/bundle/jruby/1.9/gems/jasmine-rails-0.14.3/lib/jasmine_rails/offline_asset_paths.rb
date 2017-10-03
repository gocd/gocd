# Rails Asset Patch extension used to write assets out an offline asset directory
# for future use
# example:
#   ActionView::AssetPaths.send :include, JasmineRails::OfflineAssetPaths
module JasmineRails
  module OfflineAssetPaths
    mattr_accessor :disabled

    def compute_asset_path(source, options={})
      return super if JasmineRails::OfflineAssetPaths.disabled
      source = source.to_s
      return source if source.empty? || source.starts_with?('/')
      content = Rails.application.assets[source].to_s
      asset_prefix = Rails.configuration.assets.prefix.gsub(/\A\//,'')
      source_path = JasmineRails.tmp_dir.join(asset_prefix).join(source)

      FileUtils.mkdir_p File.dirname(source_path)
      Rails.logger.debug "Compiling #{source} to #{source_path}"
      File.open(source_path, 'wb') do |f|
        if Rails::VERSION::MAJOR >= 4 && !Rails.env.test? && source == 'jasmine-specs.js'
          f << ''
        else
          f << content
        end
      end
      "/#{asset_prefix}/#{source}"
    end

    # For Rails 3.2 support
    def compute_public_path(source, dir, options={})
      JasmineRails::OfflineAssetPaths.disabled ? super : compute_asset_path(source, options)
    end

  end
end
