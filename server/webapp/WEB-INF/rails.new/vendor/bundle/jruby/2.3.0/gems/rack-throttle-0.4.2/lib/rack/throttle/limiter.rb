module Rack; module Throttle
  ##
  # This is the base class for rate limiter implementations.
  #
  # @example Defining a rate limiter subclass
  #   class MyLimiter < Limiter
  #     def allowed?(request)
  #       # TODO: custom logic goes here
  #     end
  #   end
  #
  class Limiter
    attr_reader :app
    attr_reader :options

    ##
    # @param  [#call]                       app
    # @param  [Hash{Symbol => Object}]      options
    # @option options [String]  :cache      (Hash.new)
    # @option options [String]  :key        (nil)
    # @option options [String]  :key_prefix (nil)
    # @option options [Integer] :code       (403)
    # @option options [String]  :message    ("Rate Limit Exceeded")
    # @option options [String]  :type       ("text/plain; charset=utf-8")
    def initialize(app, options = {})
      @app, @options = app, options
    end

    ##
    # @param  [Hash{String => String}] env
    # @return [Array(Integer, Hash, #each)]
    # @see    http://rack.rubyforge.org/doc/SPEC.html
    def call(env)
      request = Rack::Request.new(env)
      allowed?(request) ? app.call(env) : rate_limit_exceeded(request)
    end

    ##
    # Returns `false` if the rate limit has been exceeded for the given
    # `request`, or `true` otherwise.
    #
    # Override this method in subclasses that implement custom rate limiter
    # strategies.
    #
    # @param  [Rack::Request] request
    # @return [Boolean]
    def allowed?(request)
      case
        when whitelisted?(request) then true
        when blacklisted?(request) then false
        else true # override in subclasses
      end
    end

    ##
    # Returns `true` if the originator of the given `request` is whitelisted
    # (not subject to further rate limits).
    #
    # The default implementation always returns `false`. Override this
    # method in a subclass to implement custom whitelisting logic.
    #
    # @param  [Rack::Request] request
    # @return [Boolean]
    # @abstract
    def whitelisted?(request)
      false
    end

    ##
    # Returns `true` if the originator of the given `request` is blacklisted
    # (not honoring rate limits, and thus permanently forbidden access
    # without the need to maintain further rate limit counters).
    #
    # The default implementation always returns `false`. Override this
    # method in a subclass to implement custom blacklisting logic.
    #
    # @param  [Rack::Request] request
    # @return [Boolean]
    # @abstract
    def blacklisted?(request)
      false
    end

    protected

    ##
    # @return [Hash]
    def cache
      case cache = (options[:cache] ||= {})
        when Proc then cache.call
        else cache
      end
    end

    ##
    # @param  [String] key
    def cache_has?(key)
      case
        when cache.respond_to?(:has_key?)
          cache.has_key?(key)
        when cache.respond_to?(:get)
          cache.get(key) rescue false
        else false
      end
    end

    ##
    # @param  [String] key
    # @return [Object]
    def cache_get(key, default = nil)
      case
        when cache.respond_to?(:[])
          cache[key] || default
        when cache.respond_to?(:get)
          cache.get(key) || default
      end
    end

    ##
    # @param  [String] key
    # @param  [Object] value
    # @return [void]
    def cache_set(key, value)
      case
        when cache.respond_to?(:[]=)
          begin
            cache[key] = value
          rescue TypeError
            # GDBM throws a "TypeError: can't convert Float into String"
            # exception when trying to store a Float. On the other hand, we
            # don't want to unnecessarily coerce the value to a String for
            # any stores that do support other data types (e.g. in-memory
            # hash objects). So, this is a compromise.
            cache[key] = value.to_s
          end
        when cache.respond_to?(:set)
          cache.set(key, value)
      end
    end

    ##
    # @param  [Rack::Request] request
    # @return [String]
    def cache_key(request)
      id = client_identifier(request)
      case
        when options.has_key?(:key)
          options[:key].call(request)
        when options.has_key?(:key_prefix)
          [options[:key_prefix], id].join(':')
        else id
      end
    end

    ##
    # @param  [Rack::Request] request
    # @return [String]
    def client_identifier(request)
      request.ip.to_s
    end

    ##
    # @param  [Rack::Request] request
    # @return [Float]
    def request_start_time(request)
      # Check whether HTTP_X_REQUEST_START or HTTP_X_QUEUE_START exist and parse its value (for
      # example, when having nginx in your stack, it's going to be in the "t=\d+" format).
      if val = (request.env['HTTP_X_REQUEST_START'] || request.env['HTTP_X_QUEUE_START'])
        val[/(?:^t=)?(\d+)/, 1].to_f / 1000
      else
        Time.now.to_f
      end
    end

    ##
    # Outputs a `Rate Limit Exceeded` error.
    #
    # @return [Array(Integer, Hash, #each)]
    def rate_limit_exceeded(request)
      options[:rate_limit_exceeded_callback].call(request) if options[:rate_limit_exceeded_callback]
      headers = respond_to?(:retry_after) ? {'Retry-After' => retry_after.to_f.ceil.to_s} : {}
      http_error(options[:code] || 403, options[:message] || 'Rate Limit Exceeded', headers)
    end

    ##
    # Outputs an HTTP `4xx` or `5xx` response.
    #
    # @param  [Integer]                code
    # @param  [String, #to_s]          message
    # @param  [Hash{String => String}] headers
    # @return [Array(Integer, Hash, #each)]
    def http_error(code, message = nil, headers = {})
      contentType = 'text/plain; charset=utf-8'
      if options[:type]
        contentType = options[:type]
      end
      [code, {'Content-Type' => contentType}.merge(headers),
        [message]]
    end

    ##
    # Returns the standard HTTP status message for the given status `code`.
    #
    # @param  [Integer] code
    # @return [String]
    def http_status(code)
      [code, Rack::Utils::HTTP_STATUS_CODES[code]].join(' ')
    end
  end
end; end
