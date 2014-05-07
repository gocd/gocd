#--
# Copyright (c) 2006-2013 Philip Ross
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#++

module TZInfo  
  # Represents information about a country returned by RubyDataSource.
  #
  # @private
  class RubyCountryInfo < CountryInfo #:nodoc:
    # Constructs a new CountryInfo with an ISO 3166 country code, name and 
    # block. The block will be evaluated to obtain the timezones for the 
    # country when the zones are first needed.
    def initialize(code, name, &block)
      super(code, name)
      @block = block
      @zones = nil
      @zone_identifiers = nil
    end
    
    # Returns a frozen array of all the zone identifiers for the country. These
    # are in the order they were added using the timezone method.
    def zone_identifiers
      # Thread-safey: It is possible that the value of @zone_identifiers may be 
      # calculated multiple times in concurrently executing threads. It is not 
      # worth the overhead of locking to ensure that @zone_identifiers is only 
      # calculated once.
    
      unless @zone_identifiers
        @zone_identifiers = zones.collect {|zone| zone.identifier}.freeze
      end
      
      @zone_identifiers
    end
    
    # Returns a frozen array of all the timezones for the for the country as
    # CountryTimezone instances. These are in the order they were added using 
    # the timezone method.
    def zones
      # Thread-safey: It is possible that the value of @zones may be 
      # calculated multiple times in concurrently executing threads. It is not 
      # worth the overhead of locking to ensure that @zones is only 
      # calculated once.
    
      unless @zones
        zones = Zones.new
        @block.call(zones) if @block
        @block = nil
        @zones = zones.list.freeze
      end
      
      @zones
    end
    
    # An instance of the Zones class is passed to the block used to define
    # timezones.
    #
    # @private
    class Zones #:nodoc:
      attr_reader :list
    
      def initialize
        @list = []
      end
    
      # Called by the index data to define a timezone for the country.
      def timezone(identifier, latitude_numerator, latitude_denominator, 
                   longitude_numerator, longitude_denominator, description = nil)          
        @list << CountryTimezone.new(identifier, latitude_numerator, 
          latitude_denominator, longitude_numerator, longitude_denominator,
          description)     
      end
    end
  end
end
