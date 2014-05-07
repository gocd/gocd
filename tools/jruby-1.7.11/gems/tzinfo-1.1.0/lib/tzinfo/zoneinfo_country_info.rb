#--
# Copyright (c) 2013 Philip Ross
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
  # Represents information about a country returned by ZoneinfoDataSource.
  #
  # @private
  class ZoneinfoCountryInfo < CountryInfo #:nodoc:
    # Constructs a new CountryInfo with an ISO 3166 country code, name and 
    # an array of CountryTimezones.
    def initialize(code, name, zones)
      super(code, name)
      @zones = zones.dup.freeze
      @zone_identifiers = nil
    end
    
    # Returns a frozen array of all the zone identifiers for the country ordered
    # geographically, most populous first.
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
    
    # Returns a frozen array of all the timezones for the for the country 
    # ordered geographically, most populous first.
    def zones
      @zones
    end
  end
end
