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

require File.join(File.expand_path(File.dirname(__FILE__)), 'test_utils')

include TZInfo

class TCTimezoneUTC < Test::Unit::TestCase
  def test_2004        
    tz = Timezone.get('UTC')
    
    assert_equal(DateTime.new(2004,1,1,0,0,0), tz.utc_to_local(DateTime.new(2004,1,1,0,0,0)))
    assert_equal(DateTime.new(2004,12,31,23,59,59), tz.utc_to_local(DateTime.new(2004,12,31,23,59,59)))
    
    assert_equal(DateTime.new(2004,1,1,0,0,0), tz.local_to_utc(DateTime.new(2004,1,1,0,0,0)))
    assert_equal(DateTime.new(2004,12,31,23,59,59), tz.local_to_utc(DateTime.new(2004,12,31,23,59,59)))
        
    assert_equal(:UTC, tz.period_for_utc(DateTime.new(2004,1,1,0,0,0)).zone_identifier)    
    assert_equal(:UTC, tz.period_for_utc(DateTime.new(2004,12,31,23,59,59)).zone_identifier)
    
    assert_equal(:UTC, tz.period_for_local(DateTime.new(2004,1,1,0,0,0)).zone_identifier)    
    assert_equal(:UTC, tz.period_for_local(DateTime.new(2004,12,31,23,59,59)).zone_identifier)
        
    assert_equal(0, tz.period_for_utc(DateTime.new(2004,1,1,0,0,0)).utc_total_offset)    
    assert_equal(0, tz.period_for_utc(DateTime.new(2004,12,31,23,59,59)).utc_total_offset)
    
    assert_equal(0, tz.period_for_local(DateTime.new(2004,1,1,0,0,0)).utc_total_offset)    
    assert_equal(0, tz.period_for_local(DateTime.new(2004,12,31,23,59,59)).utc_total_offset)    
  end    
end
