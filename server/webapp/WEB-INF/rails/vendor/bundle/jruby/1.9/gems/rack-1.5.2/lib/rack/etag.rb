require 'digest/md5'

module Rack
  # Automatically sets the ETag header on all String bodies.
  #
  # The ETag header is skipped if ETag or Last-Modified headers are sent or if
  # a sendfile body (body.responds_to :to_path) is given (since such cases
  # should be handled by apache/nginx).
  #
  # On initialization, you can pass two parameters: a Cache-Control directive
  # used when Etag is absent and a directive when it is present. The first
  # defaults to nil, while the second defaults to "max-age=0, private, must-revalidate"
  class ETag
    DEFAULT_CACHE_CONTROL = "max-age=0, private, must-revalidate".freeze

    def initialize(app, no_cache_control = nil, cache_control = DEFAULT_CACHE_CONTROL)
      @app = app
      @cache_control = cache_control
      @no_cache_control = no_cache_control
    end

    def call(env)
      status, headers, body = @app.call(env)

      if etag_status?(status) && etag_body?(body) && !skip_caching?(headers)
        digest, body = digest_body(body)
        headers['ETag'] = %("#{digest}") if digest
      end

      unless headers['Cache-Control']
        if digest
          headers['Cache-Control'] = @cache_control if @cache_control
        else
          headers['Cache-Control'] = @no_cache_control if @no_cache_control
        end
      end

      [status, headers, body]
    end

    private

      def etag_status?(status)
        status == 200 || status == 201
      end

      def etag_body?(body)
        !body.respond_to?(:to_path)
      end

      def skip_caching?(headers)
        (headers['Cache-Control'] && headers['Cache-Control'].include?('no-cache')) ||
          headers.key?('ETag') || headers.key?('Last-Modified')
      end

      def digest_body(body)
        parts = []
        body.each { |part| parts << part }
        string_body = parts.join
        digest = Digest::MD5.hexdigest(string_body) unless string_body.empty?
        [digest, parts]
      end
  end
end
