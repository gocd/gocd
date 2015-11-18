module Versionist
  class Configuration
    attr_accessor :versioning_strategies
    attr_accessor :default_version
    attr_accessor :header_versions
    attr_accessor :parameter_versions
    attr_accessor :path_versions
    attr_accessor :configured_test_framework

    def initialize
      @versioning_strategies ||= Array.new
      @header_versions ||= Array.new
      @parameter_versions ||= Array.new
      @path_versions ||= Array.new
    end

    def clear!
      @versioning_strategies.clear
      @default_version = nil
      @header_versions.clear
      @parameter_versions.clear
      @path_versions.clear
    end
  end
end
