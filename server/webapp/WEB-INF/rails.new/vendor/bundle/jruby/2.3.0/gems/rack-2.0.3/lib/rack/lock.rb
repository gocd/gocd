require 'thread'
require 'rack/body_proxy'

module Rack
  # Rack::Lock locks every request inside a mutex, so that every request
  # will effectively be executed synchronously.
  class Lock
    def initialize(app, mutex = Mutex.new)
      @app, @mutex = app, mutex
    end

    def call(env)
      @mutex.lock
      begin
        response = @app.call(env.merge(RACK_MULTITHREAD => false))
        returned = response << BodyProxy.new(response.pop) { @mutex.unlock }
      ensure
        @mutex.unlock unless returned
      end
    end
  end
end
