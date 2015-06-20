module Jasmine
  module Dependencies

    class << self
      def rails3?
        running_rails3?
      end

      def rails4?
        running_rails4?
      end

      def rails?
        running_rails?
      end
      def legacy_rack?
        !defined?(Rack::Server)
      end

      def use_asset_pipeline?
        assets_pipeline_available = (rails3? || rails4?) && Rails.respond_to?(:application) && Rails.application.respond_to?(:assets)
        rails3_assets_enabled = rails3? && assets_pipeline_available && Rails.application.config.assets.enabled != false
        assets_pipeline_available && (rails4? || rails3_assets_enabled)
      end

      private

      def running_rails3?
        running_rails? && Rails.version.to_i == 3
      end

      def running_rails4?
        running_rails? && Rails.version.to_i == 4
      end

      def running_rails?
        defined?(Rails) && Rails.respond_to?(:version)
      end
    end
  end
end
