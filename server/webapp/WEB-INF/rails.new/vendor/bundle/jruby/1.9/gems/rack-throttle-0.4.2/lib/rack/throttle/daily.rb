module Rack; module Throttle
  ##
  # This rate limiter strategy throttles the application by defining a
  # maximum number of allowed HTTP requests per day (by default, 86,400
  # requests per 24 hours, which works out to an average of 1 request per
  # second).
  #
  # Note that this strategy doesn't use a sliding time window, but rather
  # tracks requests per calendar day. This means that the throttling counter
  # is reset at midnight (according to the server's local timezone) every
  # night.
  #
  # @example Allowing up to 86,400 requests per day
  #   use Rack::Throttle::Daily
  #
  # @example Allowing up to 1,000 requests per day
  #   use Rack::Throttle::Daily, :max => 1000
  #
  class Daily < TimeWindow
    ##
    # @param  [#call]                  app
    # @param  [Hash{Symbol => Object}] options
    # @option options [Integer] :max   (86400)
    def initialize(app, options = {})
      super
    end

    ##
    def max_per_day
      @max_per_hour ||= options[:max_per_day] || options[:max] || 86_400
    end

    alias_method :max_per_window, :max_per_day

    protected

    ##
    # @param  [Rack::Request] request
    # @return [String]
    def cache_key(request)
      [super, Time.now.strftime('%Y-%m-%d')].join(':')
    end
  end
end; end
