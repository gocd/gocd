require "drb/drb"

module Spec
  module Runner
    # Facade to run specs by connecting to a DRB server
    class DrbCommandLine
      # Runs specs on a DRB server. Note that this API is similar to that of
      # CommandLine - making it possible for clients to use both interchangeably.
      def self.run(options)
        begin
          # See http://redmine.ruby-lang.org/issues/show/496 as to why we specify localhost:0
          DRb.start_service("druby://localhost:0")
          spec_server = DRbObject.new_with_uri("druby://127.0.0.1:8989")
          spec_server.run(options.argv, options.error_stream, options.output_stream)
          true
        rescue DRb::DRbConnError
          options.error_stream.puts "No server is running"
          false
        end
      end
    end
  end
end
