#--
# Copyright (c) 2005-2013 Philip Ross
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

class TCDataTimezone < Test::Unit::TestCase
  
  class TestTimezoneInfo < TimezoneInfo
    attr_reader :utc
    attr_reader :local
    attr_reader :utc_to
    attr_reader :utc_from
    
    def initialize(identifier, utc_period, local_periods, transitions_up_to)
      super(identifier)
      @utc_period = utc_period
      @local_periods = local_periods || []
      @transitions_up_to = transitions_up_to
    end
    
    def period_for_utc(utc)
      @utc = utc
      @utc_period     
    end
    
    def periods_for_local(local)
      @local = local      
      @local_periods
    end
    
    def transitions_up_to(utc_to, utc_from = nil)
      @utc_to = utc_to
      @utc_from = utc_from
      @transitions_up_to
    end
  end    

  def test_identifier
    tz = DataTimezone.new(TestTimezoneInfo.new('Test/Zone', nil, [], []))
    assert_equal('Test/Zone', tz.identifier)
  end
  
  def test_period_for_utc
    # Don't need actual TimezonePeriods. DataTimezone isn't supposed to do
    # anything with them apart from return them.
    period = Object.new 
    tti = TestTimezoneInfo.new('Test/Zone', period, [], [])
    tz = DataTimezone.new(tti)
    
    t = Time.utc(2006, 6, 27, 22, 50, 12)
    assert_same(period, tz.period_for_utc(t))
    assert_same(t, tti.utc)    
  end
  
  def test_periods_for_local
    # Don't need actual TimezonePeriods. DataTimezone isn't supposed to do
    # anything with them apart from return them.
    periods = [Object.new, Object.new] 
    tti = TestTimezoneInfo.new('Test/Zone', nil, periods, [])
    tz = DataTimezone.new(tti)
    
    t = Time.utc(2006, 6, 27, 22, 50, 12)
    assert_same(periods, tz.periods_for_local(t))
    assert_same(t, tti.local)  
  end
  
  def test_periods_for_local_not_found
    periods = []
    tti = TestTimezoneInfo.new('Test/Zone', nil, periods, [])
    tz = DataTimezone.new(tti)
    
    t = Time.utc(2006, 6, 27, 22, 50, 12)
    assert_same(periods, tz.periods_for_local(t))
    assert_same(t, tti.local)
  end
  
  def test_transitions_up_to
    # Don't need actual TimezoneTransition instances. DataTimezone isn't
    # supposed to do anything with them apart from return them.
    transitions = [Object.new, Object.new]
    tti = TestTimezoneInfo.new('Test/Zone', nil, nil, transitions)
    tz = DataTimezone.new(tti)
    
    utc_to = Time.utc(2013, 1, 1, 0, 0, 0)
    utc_from = Time.utc(2012, 1, 1, 0, 0, 0)
    assert_same(transitions, tz.transitions_up_to(utc_to, utc_from))
    assert_same(utc_to, tti.utc_to)
    assert_same(utc_from, tti.utc_from)
  end    
end
