module Jasmine
  class Server
    def initialize(port = 8888, application = nil, rack_options = nil, env = ENV)
      @port = port
      @application = application
      @rack_options = rack_options || {}
      @env = env
    end

    def start
      @env['PORT'] = @port.to_s
      Rack::Server.start(@rack_options.merge(:Port => @port,
                                             :AccessLog => [],
                                             :app => @application))
    end
  end
end
