require 'action_view'
require 'sprockets'
require 'active_support/core_ext/class/attribute'

module Sprockets
  module Rails
    module Helper
      class << self
        attr_accessor :precompile, :assets, :raise_runtime_errors
      end

      def precompile
        Sprockets::Rails::Helper.precompile
      end

      def assets
        Sprockets::Rails::Helper.assets
      end

      def raise_runtime_errors
        Sprockets::Rails::Helper.raise_runtime_errors
      end

      class AssetFilteredError < StandardError
        def initialize(source)
          msg = "Asset filtered out and will not be served: " <<
                "add `Rails.application.config.assets.precompile += %w( #{source} )` " <<
                "to `config/initializers/assets.rb` and restart your server"
          super(msg)
        end
      end

      class AbsoluteAssetPathError < ArgumentError
        def initialize(bad_path, good_path, prefix)
          msg = "Asset names passed to helpers should not include the #{prefix.inspect} prefix. " <<
                "Instead of #{bad_path.inspect}, use #{good_path.inspect}"
          super(msg)
        end
      end

      if defined? ActionView::Helpers::AssetUrlHelper
        include ActionView::Helpers::AssetUrlHelper
        include ActionView::Helpers::AssetTagHelper
      else
        require 'sprockets/rails/legacy_asset_tag_helper'
        require 'sprockets/rails/legacy_asset_url_helper'
        include LegacyAssetTagHelper
        include LegacyAssetUrlHelper
      end

      VIEW_ACCESSORS = [:assets_environment, :assets_manifest,
                        :assets_prefix, :digest_assets, :debug_assets]

      def self.included(klass)
        if klass < Sprockets::Context
          klass.class_eval do
            alias_method :assets_environment, :environment
            def assets_manifest; end
            class_attribute :config, :assets_prefix, :digest_assets, :debug_assets
          end
        else
          klass.class_attribute(*VIEW_ACCESSORS)
        end
      end

      def self.extended(obj)
        obj.class_eval do
          attr_accessor(*VIEW_ACCESSORS)
        end
      end

      def compute_asset_path(path, options = {})
        # Check if we are inside Sprockets context before calling check_dependencies!.
        check_dependencies!(path) if defined?(depend_on)

        if digest_path = asset_digest_path(path)
          path = digest_path if digest_assets
          path += "?body=1" if options[:debug]
          File.join(assets_prefix || "/", path)
        else
          super
        end
      end

      # Computes the full URL to a asset in the public directory. This
      # method checks for errors before returning path.
      def asset_path(source, options = {})
        unless options[:debug]
          check_errors_for(source, options)
        end
        super(source, options)
      end
      alias :path_to_asset :asset_path

      # Get digest for asset path.
      #
      # path    - String path
      # options - Hash options
      #
      # Returns String Hex digest or nil if digests are disabled.
      def asset_digest(path, options = {})
        return unless digest_assets

        if digest_path = asset_digest_path(path, options)
          digest_path[/-(.+)\./, 1]
        end
      end

      # Expand asset path to digested form.
      #
      # path    - String path
      # options - Hash options
      #
      # Returns String path or nil if no asset was found.
      def asset_digest_path(path, options = {})
        if manifest = assets_manifest
          if digest_path = manifest.assets[path]
            return digest_path
          end
        end

        if environment = assets_environment
          if asset = environment[path]
            return asset.digest_path
          end
        end
      end

      # Override javascript tag helper to provide debugging support.
      #
      # Eventually will be deprecated and replaced by source maps.
      def javascript_include_tag(*sources)
        options = sources.extract_options!.stringify_keys

        if options["debug"] != false && request_debug_assets?
          sources.map { |source|
            check_errors_for(source, :type => :javascript)
            if asset = lookup_asset_for_path(source, :type => :javascript)
              asset.to_a.map do |a|
                super(path_to_javascript(a.logical_path, :debug => true), options)
              end
            else
              super(source, options)
            end
          }.flatten.uniq.join("\n").html_safe
        else
          sources.push(options)
          super(*sources)
        end
      end

      # Override stylesheet tag helper to provide debugging support.
      #
      # Eventually will be deprecated and replaced by source maps.
      def stylesheet_link_tag(*sources)
        options = sources.extract_options!.stringify_keys
        if options["debug"] != false && request_debug_assets?
          sources.map { |source|
            check_errors_for(source, :type => :stylesheet)
            if asset = lookup_asset_for_path(source, :type => :stylesheet)
              asset.to_a.map do |a|
                super(path_to_stylesheet(a.logical_path, :debug => true), options)
              end
            else
              super(source, options)
            end
          }.flatten.uniq.join("\n").html_safe
        else
          sources.push(options)
          super(*sources)
        end
      end

      protected
        # Ensures the asset is included in the dependencies list.
        def check_dependencies!(dep)
          depend_on(dep)
          depend_on_asset(dep)
        rescue Sprockets::FileNotFound
        end

        # Raise errors when source is not in the precompiled list, or
        # incorrectly contains the assets_prefix.
        def check_errors_for(source, options)
          return unless self.raise_runtime_errors

          source = source.to_s
          return if source.blank? || source =~ URI_REGEXP

          asset = lookup_asset_for_path(source, options)

          if asset && asset_needs_precompile?(asset.logical_path, asset.pathname.to_s)
            raise AssetFilteredError.new(asset.logical_path)
          end

          full_prefix = File.join(self.assets_prefix || "/", '')
          if !asset && source.start_with?(full_prefix)
            short_path = source[full_prefix.size, source.size]
            if lookup_asset_for_path(short_path, options)
              raise AbsoluteAssetPathError.new(source, short_path, full_prefix)
            end
          end
        end

        # Returns true when an asset will not be available after precompile is run
        def asset_needs_precompile?(source, filename)
          if assets_environment && assets_environment.send(:matches_filter, precompile || [], source, filename)
            false
          else
            true
          end
        end

        # Enable split asset debugging. Eventually will be deprecated
        # and replaced by source maps in Sprockets 3.x.
        def request_debug_assets?
          debug_assets || (defined?(controller) && controller && params[:debug_assets])
        rescue
          return false
        end

        # Internal method to support multifile debugging. Will
        # eventually be removed w/ Sprockets 3.x.
        def lookup_asset_for_path(path, options = {})
          return unless env = assets_environment
          path = path.to_s
          if extname = compute_asset_extname(path, options)
            path = "#{path}#{extname}"
          end
          env[path]
        end
    end
  end
end
