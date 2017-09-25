module Rack; module Throttle
  ##
  # This rate limiter strategy throttles the application by defining a
  # maximum number of allowed HTTP requests per hour (by default, 3,600
  # requests per 60 minutes, which works out to an average of 1 request per
  # second).
  #
  # Note that this strategy doesn't use a sliding time window, but rather
  # tracks requests per distinct hour. This means that the throttling
  # counter is reset every hour on the hour (according to the server's local
  # timezone).
  #
  # @example Allowing up to 3,600 requests per hour
  #   use Rack::Throttle::Hourly
  #
  # @example Allowing up to 100 requests per hour
  #   use Rack::Throttle::Hourly, :max => 100
  #
  class Hourly < TimeWindow
    ##
    # @param  [#call]                  app
    # @param  [Hash{Symbol => Object}] options
    # @option options [Integer] :max   (3600)
    def initialize(app, options = {})
      super
    end

    ##
    def max_per_hour
      @max_per_hour ||= options[:max_per_hour] || options[:max] || 3_600
    end

    alias_method :max_per_window, :max_per_hour

    protected

    ##
    # @param  [Rack::Request] request
    # @return [String]
    def cache_key(request)
      [super, Time.now.strftime('%Y-%m-%dT%H')].join(':')
    end
  end
end; end
