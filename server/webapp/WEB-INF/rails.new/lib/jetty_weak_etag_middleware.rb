class JettyWeakEtagMiddleware
  def initialize(app)
    @app = app
  end

  def call(env)
    # make weak etags sent by jetty strong see: http://stackoverflow.com/questions/18693718/weak-etags-in-rails
    if env['HTTP_IF_MATCH'] =~ /--gzip"$/
      env['HTTP_IF_MATCH'] = env['HTTP_IF_MATCH'].gsub(/--gzip"?$/, '"')
    end

    if env['HTTP_IF_NONE_MATCH'] =~ /--gzip"$/
      env['HTTP_IF_NONE_MATCH'] = env['HTTP_IF_NONE_MATCH'].gsub(/--gzip"?$/, '"')
    end

    status, headers, body = @app.call(env)
    [status, headers, body]
  end
end
