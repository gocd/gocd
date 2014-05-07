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

class TCInfoTimezone < Test::Unit::TestCase
  
  class TestInfoTimezone < InfoTimezone
    attr_reader :setup_info
    
    protected
      def setup(info)
        super(info)
        @setup_info = info
      end     
  end
  
  def test_identifier
    tz = InfoTimezone.new(TimezoneInfo.new('Test/Identifier'))
    assert_equal('Test/Identifier', tz.identifier)
  end
  
  def test_info
    i = TimezoneInfo.new('Test/Identifier')
    tz = InfoTimezone.new(i)
    assert_same(i, tz.send(:info))
  end
  
  def test_setup
    i = TimezoneInfo.new('Test/Identifier')
    tz = TestInfoTimezone.new(i)
    assert_same(i, tz.setup_info)
  end
end

