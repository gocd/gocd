module Rack; module Throttle
  module VERSION
    MAJOR = 0
    MINOR = 4
    TINY  = 2
    EXTRA = nil

    STRING = [MAJOR, MINOR, TINY].join('.')
    STRING << "-#{EXTRA}" if EXTRA

    ##
    # @return [String]
    def self.to_s() STRING end

    ##
    # @return [String]
    def self.to_str() STRING end

    ##
    # @return [Array(Integer, Integer, Integer)]
    def self.to_a() [MAJOR, MINOR, TINY] end
  end
end; end
