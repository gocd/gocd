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

class TCTimezoneIndexDefinition < Test::Unit::TestCase
  
  module TimezonesTest1
    include TimezoneIndexDefinition
    
    timezone 'Test/One'
    timezone 'Test/Two'
    linked_timezone 'Test/Three'
    timezone 'Another/Zone'
    linked_timezone 'And/Yet/Another'
  end
  
  module TimezonesTest2
    include TimezoneIndexDefinition
    
    timezone 'Test/A/One'
    timezone 'Test/A/Two'
    timezone 'Test/A/Three'
  end
  
  module TimezonesTest3
    include TimezoneIndexDefinition
    
    linked_timezone 'Test/B/One'
    linked_timezone 'Test/B/Two'
    linked_timezone 'Test/B/Three'
  end
  
  module TimezonesTest4
    include TimezoneIndexDefinition
    
  end
  
  def test_timezones
    assert_equal(['Test/One', 'Test/Two', 'Test/Three', 'Another/Zone', 'And/Yet/Another'], TimezonesTest1.timezones)            
    assert_equal(['Test/A/One', 'Test/A/Two', 'Test/A/Three'], TimezonesTest2.timezones)
    assert_equal(['Test/B/One', 'Test/B/Two', 'Test/B/Three'], TimezonesTest3.timezones)
    assert_equal([], TimezonesTest4.timezones)
      
    assert_equal(true, TimezonesTest1.timezones.frozen?)
    assert_equal(true, TimezonesTest2.timezones.frozen?)
    assert_equal(true, TimezonesTest3.timezones.frozen?)
    assert_equal(true, TimezonesTest4.timezones.frozen?)
  end   
  
  def test_data_timezones
    assert_equal(['Test/One', 'Test/Two', 'Another/Zone'], TimezonesTest1.data_timezones)
    assert_equal(['Test/A/One', 'Test/A/Two', 'Test/A/Three'], TimezonesTest2.data_timezones)
    assert_equal([], TimezonesTest3.data_timezones)
    assert_equal([], TimezonesTest4.data_timezones)
    
    assert_equal(true, TimezonesTest1.data_timezones.frozen?)
    assert_equal(true, TimezonesTest2.data_timezones.frozen?)
    assert_equal(true, TimezonesTest3.data_timezones.frozen?)
    assert_equal(true, TimezonesTest4.data_timezones.frozen?)
  end
  
  def test_linked_timezones
    assert_equal(['Test/Three', 'And/Yet/Another'], TimezonesTest1.linked_timezones)
    assert_equal([], TimezonesTest2.linked_timezones)
    assert_equal(['Test/B/One', 'Test/B/Two', 'Test/B/Three'], TimezonesTest3.linked_timezones)
    assert_equal([], TimezonesTest4.linked_timezones)
    
    assert_equal(true, TimezonesTest1.linked_timezones.frozen?)
    assert_equal(true, TimezonesTest2.linked_timezones.frozen?)
    assert_equal(true, TimezonesTest3.linked_timezones.frozen?)
    assert_equal(true, TimezonesTest4.linked_timezones.frozen?)
  end  
end
