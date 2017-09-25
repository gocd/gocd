module Rack; module Throttle
  ##
  # This rate limiter strategy throttles the application by defining a
  # maximum number of allowed HTTP requests per second (by default, 1
  # request per second.
  #
  # Note that this strategy doesn't use a sliding time window, but rather
  # tracks requests per distinct second. This means that the throttling
  # counter is reset every second.
  #
  # @example Allowing up to 1 request/second
  #   use Rack::Throttle::Second
  #
  # @example Allowing up to 100 requests per second
  #   use Rack::Throttle::Second, :max => 100
  #
  class Second < TimeWindow
    ##
    # @param  [#call]                  app
    # @param  [Hash{Symbol => Object}] options
    # @option options [Integer] :max   (1)
    def initialize(app, options = {})
      super
    end

    ##
    def max_per_second
      @max_per_second ||= options[:max_per_second] || options[:max] || 1
    end

    alias_method :max_per_window, :max_per_second

    protected

    ##
    # @param  [Rack::Request] request
    # @return [String]
    def cache_key(request)
      [super, Time.now.strftime('%Y-%m-%dT%H:%M:%S')].join(':')
    end
  end
end; end
