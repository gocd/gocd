require 'active_support/core_ext/hash/keys'

module Versionist
  module VersioningStrategy
    class Base
      attr_reader :config

      def initialize(config={})
        raise ArgumentError, "you must pass a configuration Hash" if config.nil? || !config.is_a?(Hash)
        @config = config
        @config.symbolize_keys!
        Versionist.configuration.versioning_strategies << self if !Versionist.configuration.versioning_strategies.include?(self)
      end

      def ==(other)
        return false if other.nil? || !other.is_a?(Versionist::VersioningStrategy::Base)
        return self.config == other.config
      end
    end
  end
end
