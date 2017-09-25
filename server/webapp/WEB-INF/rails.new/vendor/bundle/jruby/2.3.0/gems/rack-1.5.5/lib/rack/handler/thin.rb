require "thin"
require "rack/content_length"
require "rack/chunked"

module Rack
  module Handler
    class Thin
      def self.run(app, options={})
        host = options.delete(:Host) || '0.0.0.0'
        port = options.delete(:Port) || 8080
        args = [host, port, app, options]
        # Thin versions below 0.8.0 do not support additional options
        args.pop if ::Thin::VERSION::MAJOR < 1 && ::Thin::VERSION::MINOR < 8
        server = ::Thin::Server.new(*args)
        yield server if block_given?
        server.start
      end

      def self.valid_options
        {
          "Host=HOST" => "Hostname to listen on (default: localhost)",
          "Port=PORT" => "Port to listen on (default: 8080)",
        }
      end
    end
  end
end
