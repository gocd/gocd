require 'sprockets'

module Sprockets
  module Rails
    # Backports of AssetUrlHelper methods for Rails 2.x and 3.x.
    module LegacyAssetUrlHelper
      URI_REGEXP = %r{^[-a-z]+://|^(?:cid|data):|^//}

      def asset_path(source, options = {})
        source = source.to_s
        return "" unless source.present?
        return source if source =~ URI_REGEXP

        tail, source = source[/([\?#].+)$/], source.sub(/([\?#].+)$/, '')

        if extname = compute_asset_extname(source, options)
          source = "#{source}#{extname}"
        end

        if source[0] != ?/
          source = compute_asset_path(source, options)
        end

        relative_url_root = (defined?(config.relative_url_root) && config.relative_url_root) ||
          (respond_to?(:request) && request.try(:script_name))
        if relative_url_root
          source = "#{relative_url_root}#{source}" unless source.starts_with?("#{relative_url_root}/")
        end

        if host = compute_asset_host(source, options)
          source = "#{host}#{source}"
        end

        "#{source}#{tail}"
      end
      alias_method :path_to_asset, :asset_path

      ASSET_EXTENSIONS = {
        :javascript => '.js',
        :stylesheet => '.css'
      }

      def compute_asset_extname(source, options = {})
        return if options[:extname] == false
        extname = options[:extname] || ASSET_EXTENSIONS[options[:type]]
        extname if extname && File.extname(source) != extname
      end

      ASSET_PUBLIC_DIRECTORIES = {
        :audio      => '/audios',
        :font       => '/fonts',
        :image      => '/images',
        :javascript => '/javascripts',
        :stylesheet => '/stylesheets',
        :video      => '/videos'
      }

      def compute_asset_path(source, options = {})
        dir = ASSET_PUBLIC_DIRECTORIES[options[:type]] || ""
        File.join(dir, source)
      end

      def compute_asset_host(source = "", options = {})
        request = self.request if respond_to?(:request)

        if defined? config
          host = config.asset_host
        elsif defined? ActionController::Base.asset_host
          host = ActionController::Base.asset_host
        end

        host ||= request.base_url if request && options[:protocol] == :request
        return unless host

        if host.respond_to?(:call)
          arity = host.respond_to?(:arity) ? host.arity : host.method(:call).arity
          args = [source]
          args << request if request && (arity > 1 || arity < 0)
          host = host.call(*args)
        elsif host =~ /%d/
          host = host % (Zlib.crc32(source) % 4)
        end

        if host =~ URI_REGEXP
          host
        else
          protocol = options[:protocol] || (request ? :request : :relative)
          case protocol
          when :relative
            "//#{host}"
          when :request
            "#{request.protocol}#{host}"
          else
            "#{protocol}://#{host}"
          end
        end
      end

      def javascript_path(source, options = {})
        path_to_asset(source, {:type => :javascript}.merge(options))
      end
      alias_method :path_to_javascript, :javascript_path

      def stylesheet_path(source, options = {})
        path_to_asset(source, {:type => :stylesheet}.merge(options))
      end
      alias_method :path_to_stylesheet, :stylesheet_path

      def image_path(source, options = {})
        path_to_asset(source, {:type => :image}.merge(options))
      end
      alias_method :path_to_image, :image_path

      def video_path(source, options = {})
        path_to_asset(source, {:type => :video}.merge(options))
      end
      alias_method :path_to_video, :video_path

      def audio_path(source, options = {})
        path_to_asset(source, {:type => :audio}.merge(options))
      end
      alias_method :path_to_audio, :audio_path

      def font_path(source, options = {})
        path_to_asset(source, {:type => :font}.merge(options))
      end
      alias_method :path_to_font, :font_path
    end
  end
end
