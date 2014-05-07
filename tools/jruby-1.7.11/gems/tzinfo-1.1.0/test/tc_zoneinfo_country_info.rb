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

require File.join(File.expand_path(File.dirname(__FILE__)), 'test_utils')

include TZInfo

class TCZoneinfoCountryInfo < Test::Unit::TestCase
  
  def test_code
    ci = ZoneinfoCountryInfo.new('ZZ', 'Zzz', []) {|c| }
    assert_equal('ZZ', ci.code)
  end
  
  def test_name
    ci = ZoneinfoCountryInfo.new('ZZ', 'Zzz', []) {|c| }
    assert_equal('Zzz', ci.name)
  end
  
  def test_zone_identifiers_empty
    ci = ZoneinfoCountryInfo.new('ZZ', 'Zzz', []) {|c| }
    assert(ci.zone_identifiers.empty?)
    assert(ci.zone_identifiers.frozen?)
  end
  
  def test_zone_identifiers
    zones = [
      CountryTimezone.new('ZZ/TimezoneB', 1, 2, 1, 2, 'Timezone B'),
      CountryTimezone.new('ZZ/TimezoneA', 1, 4, 1, 4, 'Timezone A'),
      CountryTimezone.new('ZZ/TimezoneC', -10, 3, -20, 7, 'C'),
      CountryTimezone.new('ZZ/TimezoneD', -10, 3, -20, 7)
    ]
  
    ci = ZoneinfoCountryInfo.new('ZZ', 'Zzz', zones)
    
    assert_equal(['ZZ/TimezoneB', 'ZZ/TimezoneA', 'ZZ/TimezoneC', 'ZZ/TimezoneD'], ci.zone_identifiers)
    assert(ci.zone_identifiers.frozen?)
    assert(!ci.zones.equal?(zones))
    assert(!zones.frozen?)
  end
  
  def test_zones_empty
    ci = ZoneinfoCountryInfo.new('ZZ', 'Zzz', [])
    assert(ci.zones.empty?)
    assert(ci.zones.frozen?)
  end
  
  def test_zones
    zones = [
      CountryTimezone.new('ZZ/TimezoneB', 1, 2, 1, 2, 'Timezone B'),
      CountryTimezone.new('ZZ/TimezoneA', 1, 4, 1, 4, 'Timezone A'),
      CountryTimezone.new('ZZ/TimezoneC', -10, 3, -20, 7, 'C'),
      CountryTimezone.new('ZZ/TimezoneD', -10, 3, -20, 7)
    ]
  
    ci = ZoneinfoCountryInfo.new('ZZ', 'Zzz', zones)
    
    assert_equal([CountryTimezone.new('ZZ/TimezoneB', 1, 2, 1, 2, 'Timezone B'),
      CountryTimezone.new('ZZ/TimezoneA', 1, 4, 1, 4, 'Timezone A'),
      CountryTimezone.new('ZZ/TimezoneC', -10, 3, -20, 7, 'C'),
      CountryTimezone.new('ZZ/TimezoneD', -10, 3, -20, 7)],
      ci.zones)
    assert(ci.zones.frozen?)
    assert(!ci.zones.equal?(zones))
    assert(!zones.frozen?)
  end
end
