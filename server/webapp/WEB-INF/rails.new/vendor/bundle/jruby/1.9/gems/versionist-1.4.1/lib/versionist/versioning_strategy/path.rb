module Versionist
  module VersioningStrategy
    # Implements the path versioning strategy. It expects the following path format:
    # GET /<version>/...
    class Path < Base

      # Creates a new Path VersioningStrategy object. config must contain the following keys:
      # - :path the path prefix containing the version
      def initialize(config)
        super
        raise ArgumentError, "you must specify :value in the :path configuration Hash" if !config[:path].has_key?(:value)
        Versionist.configuration.path_versions << self if !Versionist.configuration.path_versions.include?(self)
      end

      def ==(other)
        super
        return false if !other.is_a?(Versionist::VersioningStrategy::Path)
        return config[:path][:value] == other.config[:path][:value]
      end
    end
  end
end
