require 'sprockets/base'
require 'sprockets/context'
require 'sprockets/index'

require 'hike'
require 'logger'
require 'pathname'
require 'tilt'

module Sprockets
  class Environment < Base
    # `Environment` should initialized with your application's root
    # directory. This should be the same as your Rails or Rack root.
    #
    #     env = Environment.new(Rails.root)
    #
    def initialize(root = ".")
      @trail = Hike::Trail.new(root)

      self.logger = Logger.new($stderr)
      self.logger.level = Logger::FATAL

      if respond_to?(:default_external_encoding)
        self.default_external_encoding = Encoding::UTF_8
      end

      # Create a safe `Context` subclass to mutate
      @context_class = Class.new(Context)

      # Set MD5 as the default digest
      require 'digest/md5'
      @digest_class = ::Digest::MD5
      @version = ''

      @mime_types        = Sprockets.registered_mime_types
      @engines           = Sprockets.engines
      @preprocessors     = Sprockets.preprocessors
      @postprocessors    = Sprockets.postprocessors
      @bundle_processors = Sprockets.bundle_processors
      @compressors       = Sprockets.compressors

      Sprockets.paths.each do |path|
        append_path(path)
      end

      @engines.each do |ext, klass|
        add_engine_to_trail(ext, klass)
      end

      @mime_types.each do |ext, type|
        @trail.append_extension(ext)
      end

      expire_index!

      yield self if block_given?
    end

    # Returns a cached version of the environment.
    #
    # All its file system calls are cached which makes `index` much
    # faster. This behavior is ideal in production since the file
    # system only changes between deploys.
    def index
      Index.new(self)
    end

    # Cache `find_asset` calls
    def find_asset(path, options = {})
      options[:bundle] = true unless options.key?(:bundle)

      # Ensure inmemory cached assets are still fresh on every lookup
      if (asset = @assets[cache_key_for(path, options)]) && asset.fresh?(self)
        asset
      elsif asset = index.find_asset(path, options)
        # Cache is pushed upstream by Index#find_asset
        asset
      end
    end

    protected
      def expire_index!
        # Clear digest to be recomputed
        @digest = nil
        @assets = {}
      end
  end
end
