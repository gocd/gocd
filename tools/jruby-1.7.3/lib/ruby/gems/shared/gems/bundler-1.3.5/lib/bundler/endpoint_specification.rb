require 'uri'

module Bundler
  # used for Creating Specifications from the Gemcutter Endpoint
  class EndpointSpecification < Gem::Specification
    include MatchPlatform

    attr_reader :name, :version, :platform, :dependencies
    attr_accessor :source

    def initialize(name, version, platform, dependencies)
      @name         = name
      @version      = version
      @platform     = platform
      @dependencies = dependencies
    end

    def fetch_platform
      @platform
    end

    # needed for standalone, load required_paths from local gemspec
    # after the gem in installed
    def require_paths
      if @remote_specification
        @remote_specification.require_paths
      elsif _local_specification
        _local_specification.require_paths
      else
        super
      end
    end

    # needed for binstubs
    def executables
      if @remote_specification
        @remote_specification.executables
      elsif _local_specification
        _local_specification.executables
      else
        super
      end
    end

    # needed for bundle clean
    def bindir
      if @remote_specification
        @remote_specification.bindir
      elsif _local_specification
        _local_specification.bindir
      else
        super
      end
    end

    # needed for post_install_messages during install
    def post_install_message
      if @remote_specification
        @remote_specification.post_install_message
      elsif _local_specification
        _local_specification.post_install_message
      end
    end

    def _local_specification
      eval(File.read(local_specification_path)) if @loaded_from && File.exists?(local_specification_path)
    end

    def __swap__(spec)
      @remote_specification = spec
    end

    private
    def local_specification_path
      "#{base_dir}/specifications/#{full_name}.gemspec"
    end
  end
end
