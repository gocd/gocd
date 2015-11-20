require 'webrick/httputils'

module YARD
  module Server
    module Commands
      # Serves static content when no other router matches a request
      class StaticFileCommand < Base
        include WEBrick::HTTPUtils

        DefaultMimeTypes['js'] = 'text/javascript'

        # Defines the paths used to search for static assets. To define an
        # extra path, use {YARD::Server.register_static_path} rather than
        # modifying this constant directly. Also note that files in the
        # document root will always take precedence over these paths.
        STATIC_PATHS = []

        def run
          assets_template = Templates::Engine.template(:default, :fulldoc, :html)
          path = File.cleanpath(request.path).gsub(%r{^(../)+}, '')

          file = nil
          ([adapter.document_root] + STATIC_PATHS.reverse).compact.each do |path_prefix|
            file = File.join(path_prefix, path)
            break if File.exist?(file)
            file = nil
          end

          # Search in default/fulldoc/html template if nothing in static asset paths
          file ||= assets_template.find_file(path)

          if file
            ext = "." + (request.path[/\.(\w+)$/, 1] || "html")
            headers['Content-Type'] = mime_type(ext, DefaultMimeTypes)
            self.body = File.read(file)
            return
          end

          favicon?
          self.status = 404
        end

        private

        # Return an empty favicon.ico if it does not exist so that
        # browsers don't complain.
        def favicon?
          return unless request.path == '/favicon.ico'
          self.headers['Content-Type'] = 'image/png'
          self.status = 200
          self.body = ''
          raise FinishRequest
        end
      end
    end
  end
end