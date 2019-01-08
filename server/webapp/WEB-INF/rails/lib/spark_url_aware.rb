module SparkUrlAware
  def spark_url_for(opts, path)
    request_context(opts).urlFor(path)
  end

  private

  def request_context(opts)
    r = opts.has_key?(:request) ? opts[:request] : opts[:url_builder].request
    com.thoughtworks.go.spark.RequestContext.new(r.ssl? ? "https" : "http", r.host, r.port, "/go")
  end
end
