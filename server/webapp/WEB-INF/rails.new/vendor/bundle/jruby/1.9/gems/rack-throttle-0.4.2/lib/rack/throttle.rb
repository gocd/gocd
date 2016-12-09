require 'rack'

module Rack
  module Throttle
    autoload :Limiter,    ::File.expand_path(::File.dirname(__FILE__)) + '/throttle/limiter'
    autoload :Interval,   ::File.expand_path(::File.dirname(__FILE__)) + '/throttle/interval'
    autoload :TimeWindow, ::File.expand_path(::File.dirname(__FILE__)) + '/throttle/time_window'
    autoload :Daily,      ::File.expand_path(::File.dirname(__FILE__)) + '/throttle/daily'
    autoload :Hourly,     ::File.expand_path(::File.dirname(__FILE__)) + '/throttle/hourly'
    autoload :Minute,     ::File.expand_path(::File.dirname(__FILE__)) + '/throttle/minute'
    autoload :Second,     ::File.expand_path(::File.dirname(__FILE__)) + '/throttle/second'
    autoload :VERSION,    ::File.expand_path(::File.dirname(__FILE__)) + '/throttle/version'
  end
end
