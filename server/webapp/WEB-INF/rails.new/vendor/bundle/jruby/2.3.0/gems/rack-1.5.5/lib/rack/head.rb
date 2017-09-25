module Rack

class Head
  # Rack::Head returns an empty body for all HEAD requests. It leaves
  # all other requests unchanged.
  def initialize(app)
    @app = app
  end

  def call(env)
    status, headers, body = @app.call(env)

    if env["REQUEST_METHOD"] == "HEAD"
      body.close if body.respond_to? :close
      [status, headers, []]
    else
      [status, headers, body]
    end
  end
end

end
