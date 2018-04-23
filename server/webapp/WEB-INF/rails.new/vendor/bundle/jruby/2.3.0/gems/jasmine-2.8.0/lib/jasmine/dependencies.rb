module Jasmine
  module Dependencies

    class << self
      def rails3?
        rails? && Rails.version.to_i == 3
      end

      def rails4?
        rails? && Rails.version.to_i == 4
      end

      def rails5?
        rails? && Rails.version.to_i == 5
      end

      def rails?
        defined?(Rails) && Rails.respond_to?(:version)
      end

      def use_asset_pipeline?
        assets_pipeline_available = (rails3? || rails4? || rails5?) && Rails.respond_to?(:application) && Rails.application.respond_to?(:assets)
        rails3_assets_enabled = rails3? && assets_pipeline_available && Rails.application.config.assets.enabled != false
        assets_pipeline_available && (rails4? || rails5? || rails3_assets_enabled)
      end
    end
  end
end
