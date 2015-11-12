class JettyWeakEtagMiddleware
  def initialize(app)
    @app = app
  end

  def call(env)
    # make weak etags sent by jetty strong see: http://stackoverflow.com/questions/18693718/weak-etags-in-rails
    etag=env['HTTP_IF_MATCH']
    if etag && etag =~ /--gzip\"$/
      etag = env['HTTP_IF_MATCH'].gsub(/--gzip\"?$/, '"')
      env['HTTP_IF_MATCH'] = etag
    end
    status, headers, body = @app.call(env)
    [status, headers, body]
  end
end