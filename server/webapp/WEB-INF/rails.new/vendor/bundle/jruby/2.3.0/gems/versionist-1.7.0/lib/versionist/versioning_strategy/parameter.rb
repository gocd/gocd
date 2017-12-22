module Versionist
  module VersioningStrategy
    # Implements the parameter versioning strategy.
    class Parameter < Base

      # Creates a new Parameter VersioningStrategy object. config must contain the following keys:
      # - :parameter the parameter hash to inspect
      def initialize(config)
        super
        raise ArgumentError, "you must specify :name in the :parameter configuration Hash" if !config[:parameter].has_key?(:name)
        raise ArgumentError, "you must specify :value in the :parameter configuration Hash" if !config[:parameter].has_key?(:value)
        Versionist.configuration.parameter_versions << self if !Versionist.configuration.parameter_versions.include?(self)
      end

      def matches?(request)
        parameter_string = request.params[config[:parameter][:name]].to_s
        return !parameter_string.blank? && parameter_string == config[:parameter][:value]
      end

      def ==(other)
        super
        return false if !other.is_a?(Versionist::VersioningStrategy::Parameter)
        return config[:parameter][:name] == other.config[:parameter][:name] && self.config[:parameter][:value] == other.config[:parameter][:value]
      end
    end
  end
end
