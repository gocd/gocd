require 'rack/utils'
require 'active_support/core_ext/uri'

module ActionDispatch
  class FileHandler
    def initialize(root, cache_control)
      @root          = root.chomp('/')
      @compiled_root = /^#{Regexp.escape(root)}/
      headers = cache_control && { 'Cache-Control' => cache_control }
      @file_server   = ::Rack::File.new(@root, headers)
    end

    def match?(path)
      path = unescape_path(path)
      return false unless path.valid_encoding?

      full_path = path.empty? ? @root : File.join(@root, escape_glob_chars(path))
      paths = "#{full_path}#{ext}"

      matches = Dir[paths]
      match = matches.detect { |m| File.file?(m) }
      if match
        match.sub!(@compiled_root, '')
        ::Rack::Utils.escape(match)
      end
    end

    def call(env)
      @file_server.call(env)
    end

    def ext
      @ext ||= begin
        ext = ::ActionController::Base.default_static_extension
        "{,#{ext},/index#{ext}}"
      end
    end

    def unescape_path(path)
      URI.parser.unescape(path)
    end

    def escape_glob_chars(path)
      path.gsub(/[*?{}\[\]]/, "\\\\\\&")
    end
  end

  class Static
    def initialize(app, path, cache_control=nil)
      @app = app
      @file_handler = FileHandler.new(path, cache_control)
    end

    def call(env)
      case env['REQUEST_METHOD']
      when 'GET', 'HEAD'
        path = env['PATH_INFO'].chomp('/')
        if match = @file_handler.match?(path)
          env["PATH_INFO"] = match
          return @file_handler.call(env)
        end
      end

      @app.call(env)
    end
  end
end
