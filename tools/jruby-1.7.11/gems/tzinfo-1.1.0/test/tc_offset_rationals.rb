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

class TCOffsetRationals < Test::Unit::TestCase
  def test_rational_for_offset
    [0,1,2,3,4,-1,-2,-3,-4,30*60,-30*60,61*60,-61*60,14*60*60,-14*60*60,20*60*60,-20*60*60].each {|seconds|
      assert_equal(Rational(seconds, 86400), OffsetRationals.rational_for_offset(seconds))      
    }
  end
  
  def test_rational_for_offset_constant
    -28.upto(28) {|i|
      seconds = i * 30 * 60
      
      r1 = OffsetRationals.rational_for_offset(seconds)
      r2 = OffsetRationals.rational_for_offset(seconds)
      
      assert_equal(Rational(seconds, 86400), r1)
      assert_same(r1, r2)
    }
  end
end
