module Versionist
  module VersioningStrategy
    # Implements the default version handling strategy.
    class Default < Base
      attr_accessor :strategies
      attr_accessor :module

      def initialize(config)
        super
        @module = config[:module]
        raise ArgumentError, "[VERSIONIST] attempt to set more than one default api version" if !Versionist.configuration.default_version.nil? && Versionist.configuration.default_version != self
        Versionist.configuration.default_version = self
      end

      def matches?(request)
        !header_matches?(request) && !parameter_matches?(request)
      end

      def ==(other)
        super
        return false if !other.is_a?(Versionist::VersioningStrategy::Default)
        return self.module == other.module
      end


      private

      def header_matches?(request)
        Versionist.configuration.header_versions && Versionist.configuration.header_versions.any? {|v| v.matches?(request)}
      end

      def parameter_matches?(request)
        Versionist.configuration.parameter_versions && Versionist.configuration.parameter_versions.any? {|v| v.matches?(request)}
      end
    end
  end
end
