require 'rails'
require 'rails/railtie'
require 'action_controller/railtie'
require 'active_support/core_ext/module/remove_method'
require 'sprockets'
require 'sprockets/rails/helper'

module Rails
  class Application
    # Hack: We need to remove Rails' built in config.assets so we can
    # do our own thing.
    class Configuration
      remove_possible_method :assets
    end

    # Undefine Rails' assets method before redefining it, to avoid warnings.
    remove_possible_method :assets
    remove_possible_method :assets=

    # Returns Sprockets::Environment for app config.
    def assets
      @assets ||= Sprockets::Environment.new(root.to_s) do |env|
        env.version = ::Rails.env

        path = "#{config.root}/tmp/cache/assets/#{::Rails.env}"
        env.cache = Sprockets::Cache::FileStore.new(path)

        env.context_class.class_eval do
          include ::Sprockets::Rails::Helper
        end
      end
    end
    attr_writer :assets
  end
end

module Sprockets
  class Railtie < ::Rails::Railtie
    LOOSE_APP_ASSETS = lambda do |filename, path|
      path =~ /app\/assets/ && !%w(.js .css).include?(File.extname(filename))
    end

    class OrderedOptions < ActiveSupport::OrderedOptions
      def configure(&block)
        self._blocks << block
      end
    end

    config.assets = OrderedOptions.new
    config.assets._blocks    = []
    config.assets.paths      = []
    config.assets.prefix     = "/assets"
    config.assets.precompile = [LOOSE_APP_ASSETS, /(?:\/|\\|\A)application\.(css|js)$/]
    config.assets.version    = ""
    config.assets.debug      = false
    config.assets.compile    = true
    config.assets.digest     = false

    rake_tasks do |app|
      require 'sprockets/rails/task'
      Sprockets::Rails::Task.new(app)
    end

    config.after_initialize do |app|
      config = app.config

      manifest_path = File.join(app.root, 'public', config.assets.prefix)

      unless config.assets.version.blank?
        app.assets.version += "-#{config.assets.version}"
      end

      # Copy config.assets.paths to Sprockets
      config.assets.paths.each do |path|
        app.assets.append_path path
      end

      ActionView::Base.instance_eval do
        include Sprockets::Rails::Helper

        # Copy relevant config to AV context
        self.debug_assets  = config.assets.debug
        self.digest_assets = config.assets.digest
        self.assets_prefix = config.assets.prefix

        # Copy over to Sprockets as well
        context = app.assets.context_class
        context.assets_prefix = config.assets.prefix
        context.digest_assets = config.assets.digest
        context.config        = config.action_controller

        if config.assets.compile
          self.assets_environment = app.assets
          self.assets_manifest    = Sprockets::Manifest.new(app.assets, manifest_path)
        else
          self.assets_manifest = Sprockets::Manifest.new(manifest_path)
        end
      end

      app.assets.js_compressor  = config.assets.js_compressor
      app.assets.css_compressor = config.assets.css_compressor

      # Run app.assets.configure blocks
      config.assets._blocks.each do |block|
        block.call app.assets
      end

      # No more configuration changes at this point.
      # With cache classes on, Sprockets won't check the FS when files
      # change. Preferable in production when the FS only changes on
      # deploys when the app restarts.
      if config.cache_classes
        app.assets = app.assets.index
      end

      if config.assets.compile
        if app.routes.respond_to?(:prepend)
          app.routes.prepend do
            mount app.assets => config.assets.prefix
          end
        end
      end
    end
  end
end
