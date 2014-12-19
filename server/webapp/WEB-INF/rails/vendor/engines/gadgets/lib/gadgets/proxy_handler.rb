module Gadgets
  class ProxyHandler
    def initialize(request_pipeline, rewriter_registry)
      @request_pipeline = request_pipeline
      @rewriter_registry = rewriter_registry
    end
    
    def fetch(url, options={})
      remote_request = build_http_request(url, options)
      @rewriter_registry.rewrite_http_response(remote_request, @request_pipeline.execute(remote_request))
    end
    
    private
    def build_http_request(url, options)
      remote_request = Shindig::HttpRequest.new(Shindig::Uri.parse(url))
      remote_request.set_method(options[:http_method] || "GET")
      remote_request.set_ignore_cache(options[:ignore_cache])
      remote_request.set_cache_ttl(options[:refresh]) if options[:refresh]
      remote_request
    end
  end
end