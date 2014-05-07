# encoding: UTF-8

#--
# Copyright (c) 2008-2013 Philip Ross
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

class TCRubyCoreSupport < Test::Unit::TestCase
  def test_rational_new!
    assert_equal(Rational(3,4), RubyCoreSupport.rational_new!(3,4))
  end
  
  def test_datetime_new!
    assert_equal(DateTime.new(2008,10,5,12,0,0, 0, Date::ITALY), RubyCoreSupport.datetime_new!(2454745,0,2299161))
    assert_equal(DateTime.new(2008,10,6,12,0,0, 1, Date::ITALY), RubyCoreSupport.datetime_new!(2454745,1,2299161))
    
    assert_equal(DateTime.new(2008,10,5,20,30,0, 0, Date::ITALY), RubyCoreSupport.datetime_new!(Rational(117827777, 48), 0, 2299161))
    assert_equal(DateTime.new(2008,10,6,20,30,0, 1, Date::ITALY), RubyCoreSupport.datetime_new!(Rational(117827777, 48), 1, 2299161))
    
    assert_equal(DateTime.new(2008,10,6,6,26,21, 0, Date::ITALY), RubyCoreSupport.datetime_new!(Rational(70696678127,28800), 0, 2299161))
    assert_equal(DateTime.new(2008,10,7,6,26,21, 1, Date::ITALY), RubyCoreSupport.datetime_new!(Rational(70696678127, 28800), 1, 2299161))
    
    assert_equal(DateTime.new(-4712,1,1,12,0,0, 0, Date::ITALY), RubyCoreSupport.datetime_new!(0, 0, 2299161))
    assert_equal(DateTime.new(-4712,1,2,12,0,0, 1, Date::ITALY), RubyCoreSupport.datetime_new!(0, 1, 2299161))
    
    assert_equal(DateTime.new(-4713,12,31,10,58,59, 0, Date::ITALY), RubyCoreSupport.datetime_new!(Rational(-90061, 86400), 0, 2299161))
    assert_equal(DateTime.new(-4712,1,1,10,58,59, 1, Date::ITALY), RubyCoreSupport.datetime_new!(Rational(-90061, 86400), 1, 2299161))
    
    assert_equal(DateTime.new(-4713,12,30,10,58,59, 0, Date::ITALY), RubyCoreSupport.datetime_new!(Rational(-176461, 86400), 0, 2299161))
    assert_equal(DateTime.new(-4713,12,31,10,58,59, 1, Date::ITALY), RubyCoreSupport.datetime_new!(Rational(-176461, 86400), 1, 2299161))
  end
  
  def test_datetime_new
    assert_equal(DateTime.new(2012, 12, 31, 23, 59, 59, 0, Date::ITALY), RubyCoreSupport.datetime_new(2012, 12, 31, 23, 59, 59, 0, Date::ITALY))
    assert_equal(DateTime.new(2013, 2, 6, 23, 2, 36, 1, Date::ITALY), RubyCoreSupport.datetime_new(2013, 2, 6, 23, 2, 36, 1, Date::ITALY))
    
    assert_equal(DateTime.new(2012, 12, 31, 23, 59, 59, 0, Date::ITALY) + Rational(1, 86400000), RubyCoreSupport.datetime_new(2012, 12, 31, 23, 59, 59 + Rational(1, 1000), 0, Date::ITALY))
    assert_equal(DateTime.new(2001, 10, 12, 12, 22, 59, 1, Date::ITALY) + Rational(501, 86400000), RubyCoreSupport.datetime_new(2001, 10, 12, 12, 22, 59 + Rational(501, 1000), 1, Date::ITALY))
  end
  
  def test_time_supports_negative
    if RubyCoreSupport.time_supports_negative
      assert_equal(Time.utc(1969, 12, 31, 23, 59, 59), Time.at(-1).utc)
    else
      assert_raises(ArgumentError) do
        Time.at(-1)
      end
    end
  end
  
  def test_time_supports_64_bit
    if RubyCoreSupport.time_supports_64bit
      assert_equal(Time.utc(1901, 12, 13, 20, 45, 51), Time.at(-2147483649).utc)
      assert_equal(Time.utc(2038, 1, 19, 3, 14, 8), Time.at(2147483648).utc)
    else
      assert_raises(RangeError) do
        Time.at(-2147483649)
      end
      
      assert_raises(RangeError) do
        Time.at(2147483648)
      end
    end
  end
  
  def test_time_nsec
    t = Time.utc(2013, 2, 6, 21, 56, 23, 567890 + Rational(123,1000))
    
    if t.respond_to?(:nsec)
      assert_equal(567890123, RubyCoreSupport.time_nsec(t))
    else
      assert_equal(567890000, RubyCoreSupport.time_nsec(t))
    end
  end
  
  def test_force_encoding
    s = [0xC2, 0xA9].pack('c2')
    
    if s.respond_to?(:force_encoding)
      # Ruby 1.9+ - should call String#force_encoding
      assert_equal('ASCII-8BIT', s.encoding.name)
      assert_equal(2, s.bytesize)
      result = RubyCoreSupport.force_encoding(s, 'UTF-8')
      assert_same(s, result)
      assert_equal('UTF-8', s.encoding.name)
      assert_equal(2, s.bytesize)
      assert_equal(1, s.length)
      assert_equal('©', s)
    else
      # Ruby 1.8 - no-op
      result = RubyCoreSupport.force_encoding(s, 'UTF-8')
      assert_same(s, result)
      assert_equal('©', s)
    end
  end
end
