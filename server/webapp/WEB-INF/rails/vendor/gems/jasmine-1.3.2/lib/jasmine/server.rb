module Jasmine
  class Server
    def initialize(port = 8888, application = nil)
      @port = port
      @application = application
    end

    def start
      if Jasmine::Dependencies.legacy_rack?
        handler = Rack::Handler.get('webrick')
        handler.run(@application, :Port => @port, :AccessLog => [])
      else
        server = Rack::Server.new(:Port => @port, :AccessLog => [])
        # workaround for Rack bug, when Rack > 1.2.1 is released Rack::Server.start(:app => Jasmine.app(self)) will work
        server.instance_variable_set(:@app, @application)
        server.start
      end
    end
  end
end
