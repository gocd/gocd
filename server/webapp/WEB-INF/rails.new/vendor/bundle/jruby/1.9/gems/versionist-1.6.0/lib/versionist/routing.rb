require 'active_support/core_ext/hash/keys'

module Versionist
  module Routing
    # Allows you to constrain routes to specific versions of your api using versioning strategies.
    # Supported formats:
    #
    # HTTP Header
    # api_version(:module => "V1", :header => {:name => "Accept", :value => "application/vnd.mycompany.com; version=1"}})
    #
    # Path
    # api_version(:module => "V1", :path => {:value => "v1"}})
    #
    # Request Parameter
    # api_version(:module => "V1", :parameter => {:name => "version", :value => "1"}})
    #
    # Specifying default version:
    # api_version(:module => "V1", :default => true, :header => {:name => "Accept", :value => "application/vnd.mycompany.com; version=1"}})
    #
    # Multiple Strategies per version
    # api_version(:module => "V1", :header => {:name => "Accept", :value => "application/vnd.mycompany.com; version=1"}, :path => {:value => "v1"})
    def api_version(config, &block)
      raise ArgumentError, "you must pass a configuration Hash to api_version" if config.nil? || !config.is_a?(Hash)
      config.symbolize_keys!
      raise ArgumentError, "you must specify :header, :path, or :parameter in configuration Hash passed to api_version" if !config.has_key?(:header) && !config.has_key?(:path) && !config.has_key?(:parameter)
      [:header, :path, :parameter].each do |s|
        raise ArgumentError, "#{s} key in configuration Hash passed to api_version must point to a Hash" if config.has_key?(s) && !config[s].is_a?(Hash)
      end
      raise ArgumentError, "you must specify :module in configuration Hash passed to api_version" if !config.has_key?(:module)
      raise ArgumentError, ":defaults must be a Hash" if config.has_key?(:defaults) && !config[:defaults].is_a?(Hash)
      rails_quirks(config, &block)
      configure_header(config, &block) if config.has_key?(:header)
      configure_path(config, &block) if config.has_key?(:path)
      configure_parameter(config, &block) if config.has_key?(:parameter)
      configure_default(config, &block) if config.has_key?(:default) && config[:default]
    end


    private

    def configure_header(config, &block)
      header = Versionist::VersioningStrategy::Header.new(config)
      route_hash = {:module => config[:module], :constraints => header}
      route_hash.merge!({:defaults => config[:defaults]}) if config.has_key?(:defaults)
      scope(route_hash, &block)
    end

    def configure_path(config, &block)
      config[:path][:value].slice!(0) if config[:path][:value] =~ /^\//
      path = Versionist::VersioningStrategy::Path.new(config)
      # Use the :as option and strip out non-word characters from the path to avoid this:
      # https://github.com/rails/rails/issues/3224
      route_hash = {:module => config[:module], :as => config[:path][:value].gsub(/\W/, '_')}
      route_hash.merge!({:defaults => config[:defaults]}) if config.has_key?(:defaults)
      namespace(config[:path][:value], route_hash, &block)
    end

    def configure_parameter(config, &block)
      parameter = Versionist::VersioningStrategy::Parameter.new(config)
      route_hash = {:module => config[:module], :constraints => parameter}
      route_hash.merge!({:defaults => config[:defaults]}) if config.has_key?(:defaults)
      scope(route_hash, &block)
    end

    def configure_default(config, &block)
      default = Versionist::VersioningStrategy::Default.new(config)
      route_hash = {:module => config[:module], :constraints => default, :as => 'default'}
      route_hash.merge!({:defaults => config[:defaults]}) if config.has_key?(:defaults)
      scope(route_hash, &block)
    end

    # deals with quirks in routing among the various Rails versions
    def rails_quirks(config, &block)
      # Rails 4 no longer allows constant syntax in routing. 
      # https://github.com/bploetz/versionist/issues/39
      # call underscore on the module so it adheres to this convention
      config[:module] = config[:module].underscore if Rails::VERSION::MAJOR >= 4
    end
  end
end
