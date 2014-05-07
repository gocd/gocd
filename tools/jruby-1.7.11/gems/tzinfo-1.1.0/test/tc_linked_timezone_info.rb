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

class TCLinkedTimezoneInfo < Test::Unit::TestCase
  
  def test_identifier
    lti = LinkedTimezoneInfo.new('Test/Zone', 'Test/Linked')
    assert_equal('Test/Zone', lti.identifier)
  end
  
  def test_link_to_identifier
    lti = LinkedTimezoneInfo.new('Test/Zone', 'Test/Linked')
    assert_equal('Test/Linked', lti.link_to_identifier)
  end
  
  def test_construct_timezone
    lti = LinkedTimezoneInfo.new('Test/Zone', 'Europe/London')
    tz = lti.create_timezone
    assert_kind_of(LinkedTimezone, tz)
    assert_equal('Test/Zone', tz.identifier)
  end
end
