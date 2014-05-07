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
  # Represents a country and references to its timezones as returned by a
  # DataSource.
  class CountryInfo
    # The ISO 3166 country code.
    attr_reader :code
    
    # The name of the country.
    attr_reader :name
    
    # Constructs a new CountryInfo with an ISO 3166 country code and name
    def initialize(code, name)
      @code = code
      @name = name
    end
    
    # Returns internal object state as a programmer-readable string.
    def inspect
      "#<#{self.class}: #@code>"
    end
    
    # Returns a frozen array of all the zone identifiers for the country.
    # The identifiers are ordered by importance according to the DataSource.
    def zone_identifiers
      raise NotImplementedError, 'Subclasses must override zone_identifiers'
    end
    
    # Returns a frozen array of all the timezones for the for the country as
    # CountryTimezone instances.
    #
    # The timezones are ordered by importance according to the DataSource.
    def zones
      raise NotImplementedError, 'Subclasses must override zone_identifiers'
    end
  end
end
