module Jasmine
  module Dependencies

    class << self
      def rspec2?
        safe_gem_check("rspec", ">= 2.0")
      end

      def rails2?
        safe_gem_check("rails", "~> 2.3") && running_rails2?
      end

      def legacy_rails?
        safe_gem_check("rails", "< 2.3.11") && running_legacy_rails?
      end

      def rails3?
        safe_gem_check("rails", ">= 3.0") && running_rails3?
      end

      def legacy_rack?
        !defined?(Rack::Server)
      end

      def rails_3_asset_pipeline?
        rails3? && Rails.respond_to?(:application) && Rails.application.respond_to?(:assets) && Rails.application.assets
      end

      private

      def running_legacy_rails?
        running_rails? && (Gem::Version.new(Rails.version) < Gem::Version.new("2.3.11"))
      end

      def running_rails2?
        running_rails? && Rails.version.to_i == 2
      end

      def running_rails3?
        running_rails? && Rails.version.to_i == 3
      end

      def running_rails?
        defined?(Rails) && Rails.respond_to?(:version)
      end

      def safe_gem_check(gem_name, version_string)
        if Gem::Specification.respond_to?(:find_by_name)
          Gem::Specification.find_by_name(gem_name, version_string)
        elsif Gem.respond_to?(:available?)
          Gem.available?(gem_name, version_string)
        end
      rescue Gem::LoadError
        false
      end

    end
  end
end
