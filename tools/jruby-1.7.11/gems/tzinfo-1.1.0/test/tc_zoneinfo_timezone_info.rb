# encoding: UTF-8

#--
# Copyright (c) 2012-2013 Philip Ross
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
require 'tempfile'

include TZInfo

class TCZoneinfoTimezoneInfo < Test::Unit::TestCase

  begin
    Time.at(-2147483649)
    Time.at(2147483648)
    SUPPORTS_64BIT = true
  rescue RangeError
    SUPPORTS_64BIT = false
  end

  def assert_period(abbreviation, utc_offset, std_offset, dst, start_at, end_at, info)    
    if start_at
      period = info.period_for_utc(start_at)
    elsif end_at
      period = info.period_for_utc(end_at - 1)
    else
      # no transitions, pick the epoch
      period = info.period_for_utc(Time.utc(1970, 1, 1))
    end
    
    assert_equal(abbreviation, period.abbreviation)
    assert_equal(utc_offset, period.utc_offset)
    assert_equal(std_offset, period.std_offset)
    assert_equal(dst, period.dst?)
    
    if start_at
      assert_not_nil(period.utc_start_time)
      assert_equal(start_at, period.utc_start_time)
    else
      assert_nil(period.utc_start_time)
    end
    
    if end_at
      assert_not_nil(period.utc_end_time)
      assert_equal(end_at, period.utc_end_time)
    else
      assert_nil(period.utc_end_time)
    end
  end
  
  def convert_times_to_i(items, key = :at)
    items.each do |item|
      if item[key].kind_of?(Time)
        item[key] = item[key].utc.to_i
      end
    end
  end
  
  def select_with_32bit_values(items, key = :at)
    items.select do |item|
      i = item[key]
      i >= -2147483648 && i <= 2147483647
    end
  end
  
  def pack_int64_network_order(values)
    values.collect {|value| [value >> 32, value & 0xFFFFFFFF]}.flatten.pack('NN' * values.length)
  end
  
  def pack_int64_signed_network_order(values)
    # Convert to the equivalent 64-bit unsigned integer with the same bit representation
    pack_int64_network_order(values.collect {|value| value < 0 ? value + 0x10000000000000000 : value})
  end
  
  def write_tzif(format, offsets, transitions, leaps = [], options = {})
    
    # Options for testing malformed zoneinfo files.
    magic = options[:magic]
    section2_magic = options[:section2_magic]
    abbrev_separator = options[:abbrev_separator] || "\0"
    abbrev_offset_base = options[:abbrev_offset_base] || 0
      
    unless magic
      if format == 1
        magic = "TZif\0"
      elsif format >= 2
        magic = "TZif#{format}"
      else
        raise ArgumentError, 'Invalid format specified'
      end
    end
    
    if section2_magic.kind_of?(Proc)
      section2_magic = section2_magic.call(format)
    else
      section2_magic = magic unless section2_magic
    end
    
    convert_times_to_i(transitions)
    convert_times_to_i(leaps)    
    
    abbrevs = offsets.collect {|o| o[:abbrev]}.uniq
    
    if abbrevs.length > 0
      abbrevs = abbrevs.collect {|a| a.encode('UTF-8')} if abbrevs.first.respond_to?(:encode)    
    
      if abbrevs.first.respond_to?(:bytesize)
        abbrevs_length = abbrevs.inject(0) {|sum, a| sum + a.bytesize + abbrev_separator.bytesize}
      else
        abbrevs_length = abbrevs.inject(0) {|sum, a| sum + a.length + abbrev_separator.length}
      end
    else
      abbrevs_length = 0
    end
  
    b32_transitions = select_with_32bit_values(transitions)    
    b32_leaps = select_with_32bit_values(leaps)
  
    Tempfile.open('tzinfo-test-zone') do |file|
      file.binmode
      
      file.write(
        [magic, offsets.length, offsets.length, leaps.length, 
         b32_transitions.length, offsets.length, abbrevs_length].pack('a5 x15 NNNNNN'))
	
      unless b32_transitions.empty?
        file.write(b32_transitions.collect {|t| t[:at]}.pack('N' * b32_transitions.length))
        file.write(b32_transitions.collect {|t| t[:offset_index]}.pack('C' * b32_transitions.length))
      end
      
      offsets.each do |offset|
        index = abbrevs.index(offset[:abbrev])
        abbrev_offset = abbrev_offset_base
        0.upto(index - 1) {|i| abbrev_offset += abbrevs[i].length + 1}
      
        file.write([offset[:gmtoff], offset[:isdst] ? 1 : 0, abbrev_offset].pack('NCC'))
      end
          
      abbrevs.each do |a|
        file.write(a)
        file.write(abbrev_separator)
      end
      
      b32_leaps.each do |leap|
        file.write([leap[:at], leap[:seconds]].pack('NN'))
      end
      
      unless offsets.empty?
        file.write("\0" * offsets.length * 2)
      end
      
      if format >= 2
        file.write(
          [section2_magic, offsets.length, offsets.length, leaps.length, 
           transitions.length, offsets.length, abbrevs_length].pack('a5 x15 NNNNNN'))
    
        unless transitions.empty?
          file.write(pack_int64_signed_network_order(transitions.collect {|t| t[:at]}))
          file.write(transitions.collect {|t| t[:offset_index]}.pack('C' * transitions.length))
        end
        
        offsets.each do |offset|
          index = abbrevs.index(offset[:abbrev])
          abbrev_offset = abbrev_offset_base
          0.upto(index - 1) {|i| abbrev_offset += abbrevs[i].length + 1}
        
          file.write([offset[:gmtoff], offset[:isdst] ? 1 : 0, abbrev_offset].pack('NCC'))
        end
        
        abbrevs.each do |a|
          file.write(a)
          file.write(abbrev_separator)
        end
        
        leaps.each do |leap|          
          file.write(pack_int64_signed_network_order([leap[:at]]))
          file.write([leap[:seconds]].pack('N'))
        end
        
        unless offsets.empty?
          file.write("\0" * offsets.length * 2)
        end
        
        # Empty POSIX timezone string
        file.write("\n\n")
      end
      
      file.flush
      
      yield file.path, format
    end
  end
  
  def tzif_test(offsets, transitions, leaps = [], options = {}, &block)
    min_format = options[:min_format] || 1
    
    min_format.upto(3) do |format|
      write_tzif(format, offsets, transitions, leaps, options, &block)
    end
  end
  
  def test_load
    offsets = [
      {:gmtoff => 3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => 3600, :isdst => false, :abbrev => 'XST'},
      {:gmtoff => 7200, :isdst => true,  :abbrev => 'XDT'},
      {:gmtoff =>    0, :isdst => false, :abbrev => 'XNST'}]
      
    transitions = [
      {:at => Time.utc(1971,  1,  2), :offset_index => 1},
      {:at => Time.utc(1980,  4, 22), :offset_index => 2},
      {:at => Time.utc(1980, 10, 21), :offset_index => 1},
      {:at => Time.utc(2000, 12, 31), :offset_index => 3}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/One', path)
      assert_equal('Zone/One', info.identifier)
      
      assert_period(:LMT,  3542,    0, false,                    nil, Time.utc(1971,  1,  2), info)
      assert_period(:XST,  3600,    0, false, Time.utc(1971,  1,  2), Time.utc(1980,  4, 22), info)
      assert_period(:XDT,  3600, 3600,  true, Time.utc(1980,  4, 22), Time.utc(1980, 10, 21), info)
      assert_period(:XST,  3600,    0, false, Time.utc(1980, 10, 21), Time.utc(2000, 12, 31), info)
      assert_period(:XNST,    0,    0, false, Time.utc(2000, 12, 31),                    nil, info)
    end
  end
  
  def test_load_negative_utc_offset
    offsets = [
      {:gmtoff => -12492, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => -12000, :isdst => false, :abbrev => 'XST'},
      {:gmtoff => -8400,  :isdst => true,  :abbrev => 'XDT'},
      {:gmtoff => -8400,  :isdst => false, :abbrev => 'XNST'}]
      
    transitions = [
      {:at => Time.utc(1971,  7,  9, 3,  0, 0), :offset_index => 1},
      {:at => Time.utc(1972, 10, 12, 3,  0, 0), :offset_index => 2},
      {:at => Time.utc(1973,  4, 29, 3,  0, 0), :offset_index => 1},
      {:at => Time.utc(1992,  4,  1, 4, 30, 0), :offset_index => 3}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/One', path)
      assert_equal('Zone/One', info.identifier)
      
      assert_period(:LMT, -12492,    0, false,                              nil, Time.utc(1971,  7,  9, 3,  0, 0), info)
      assert_period(:XST, -12000,    0, false, Time.utc(1971,  7,  9, 3,  0, 0), Time.utc(1972, 10, 12, 3,  0, 0), info)
      assert_period(:XDT, -12000, 3600,  true, Time.utc(1972, 10, 12, 3,  0, 0), Time.utc(1973,  4, 29, 3,  0, 0), info)
      assert_period(:XST, -12000,    0, false, Time.utc(1973,  4, 29, 3,  0, 0), Time.utc(1992,  4,  1, 4, 30, 0), info)
      assert_period(:XNST, -8400,    0, false, Time.utc(1992,  4,  1, 4, 30, 0),                              nil, info)
    end
  end
  
  def test_load_dst_first
    offsets = [
      {:gmtoff => 7200, :isdst => true,  :abbrev => 'XDT'},
      {:gmtoff => 3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => 3600, :isdst => false, :abbrev => 'XST'},
      {:gmtoff =>    0, :isdst => false, :abbrev => 'XNST'}]
      
    transitions = [
      {:at => Time.utc(1979,  1,  2), :offset_index => 2},
      {:at => Time.utc(1980,  4, 22), :offset_index => 0},
      {:at => Time.utc(1980, 10, 21), :offset_index => 2},
      {:at => Time.utc(2000, 12, 31), :offset_index => 3}]
  
    tzif_test(offsets, transitions) do |path, format|
      info = ZoneinfoTimezoneInfo.new('Zone/Two', path)
      assert_equal('Zone/Two', info.identifier)
      
      assert_period(:LMT, 3542, 0, false, nil, Time.utc(1979, 1, 2), info)      
    end
  end
    
  def test_load_no_transitions
    offsets = [{:gmtoff => -12094, :isdst => false, :abbrev => 'LT'}]
        
    tzif_test(offsets, []) do |path, format|
      info = ZoneinfoTimezoneInfo.new('Zone/three', path)
      assert_equal('Zone/three', info.identifier)
      
      assert_period(:LT, -12094, 0, false, nil, nil, info)
    end
  end
  
  def test_load_invalid_offset_index
    offsets = [{:gmtoff => -0, :isdst => false, :abbrev => 'LMT'}]
    transitions = [{:at => Time.utc(2000, 12, 31), :offset_index => 2}]
        
    tzif_test(offsets, transitions) do |path, format|
      assert_raises(InvalidZoneinfoFile) do
        ZoneinfoTimezoneInfo.new('Zone', path)
      end
    end
  end
  
  def test_load_with_leap_seconds
    offsets = [{:gmtoff => -0, :isdst => false, :abbrev => 'LMT'}]
    leaps = [{:at => Time.utc(1972,6,30,23,59,60), :seconds => 1}]
        
    tzif_test(offsets, [], leaps) do |path, format|
      assert_raises(InvalidZoneinfoFile) do
        ZoneinfoTimezoneInfo.new('Zone', path)
      end
    end
  end
  
  def test_load_invalid_magic
    ['TZif4', 'tzif2', '12345'].each do |magic|    
      offsets = [{:gmtoff => -12094, :isdst => false, :abbrev => 'LT'}]
          
      tzif_test(offsets, [], [], :magic => magic) do |path, format|        
        assert_raises(InvalidZoneinfoFile) do
          ZoneinfoTimezoneInfo.new('Zone2', path)
        end
      end
    end
  end
  
  # These tests can only be run if the platform supports 64-bit Times. When
  # 64-bit support is unavailable, the second section will not be read, so no 
  # error will be raised.
  if SUPPORTS_64BIT
    def test_load_invalid_section2_magic
      ['TZif4', 'tzif2', '12345'].each do |section2_magic|    
        offsets = [{:gmtoff => -12094, :isdst => false, :abbrev => 'LT'}]
            
        tzif_test(offsets, [], [], :min_format => 2, :section2_magic => section2_magic) do |path, format|        
          assert_raises(InvalidZoneinfoFile) do
            ZoneinfoTimezoneInfo.new('Zone4', path)
          end
        end
      end
    end
    
    def test_load_mismatched_section2_magic
      minus_one = Proc.new {|f| f == 2 ? "TZif\0" : "TZif#{f - 1}" }
      plus_one = Proc.new {|f| "TZif#{f + 1}" }
      
      [minus_one, plus_one].each do |section2_magic|    
        offsets = [{:gmtoff => -12094, :isdst => false, :abbrev => 'LT'}]
            
        tzif_test(offsets, [], [], :min_format => 2, :section2_magic => section2_magic) do |path, format|        
          assert_raises(InvalidZoneinfoFile) do
            ZoneinfoTimezoneInfo.new('Zone5', path)
          end
        end
      end
    end
  end
  
  def test_load_invalid_format
    Tempfile.open('tzinfo-test-zone') do |file|
      file.write('Invalid')
      file.flush
      
      assert_raises(InvalidZoneinfoFile) do
        ZoneinfoTimezoneInfo.new('Zone3', file.path)
      end
    end
  end
  
  def test_load_missing_abbrev_null_termination
    offsets = [
      {:gmtoff => 3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => 3600, :isdst => false,  :abbrev => 'XST'}]
      
    transitions = [
      {:at => Time.utc(2000, 1, 1), :offset_index => 1}]
            
    tzif_test(offsets, transitions, [], :abbrev_separator => '^') do |path, format|
      assert_raises(InvalidZoneinfoFile) do
        ZoneinfoTimezoneInfo.new('Zone', path)
      end
    end
  end
  
  def test_load_out_of_range_abbrev_offsets
    offsets = [
      {:gmtoff => 3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => 3600, :isdst => false,  :abbrev => 'XST'}]
      
    transitions = [
      {:at => Time.utc(2000, 1, 1), :offset_index => 1}]
            
    tzif_test(offsets, transitions, [], :abbrev_offset_base => 8) do |path, format|
      assert_raises(InvalidZoneinfoFile) do
        ZoneinfoTimezoneInfo.new('Zone', path)
      end
    end
  end
  
  def test_load_before_epoch
    # Some platforms don't support negative numbers for times before the epoch.
    # Check that they are returned when supported and skipped when not.
      
    begin
      Time.at(-1)
      Time.at(-2147483648)
      supports_negative = true
    rescue ArgumentError
      supports_negative = false
    end
  
    offsets = [
      {:gmtoff => 3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => 3600, :isdst => false, :abbrev => 'XST'},
      {:gmtoff => 7200, :isdst => true,  :abbrev => 'XDT'},
      {:gmtoff =>    0, :isdst => false, :abbrev => 'XNST'}]
      
    transitions = [
      {:at =>             -694224000, :offset_index => 1}, # Time.utc(1948,  1,  2)
      {:at =>              -21945600, :offset_index => 2}, # Time.utc(1969,  4, 22)
      {:at => Time.utc(1970, 10, 21), :offset_index => 1},
      {:at => Time.utc(2000, 12, 31), :offset_index => 3}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/Negative', path)
      assert_equal('Zone/Negative', info.identifier)
      
      if supports_negative
        assert_period(:LMT,  3542,    0, false,                    nil, Time.utc(1948,  1,  2), info)
        assert_period(:XST,  3600,    0, false, Time.utc(1948,  1,  2), Time.utc(1969,  4, 22), info)
        assert_period(:XDT,  3600, 3600,  true, Time.utc(1969,  4, 22), Time.utc(1970, 10, 21), info)
      else
        assert_period(:LMT,  3542,    0, false,                    nil, Time.utc(1970, 10, 21), info)
      end
      
      assert_period(:XST,  3600,    0, false, Time.utc(1970, 10, 21), Time.utc(2000, 12, 31), info)
      assert_period(:XNST,    0,    0, false, Time.utc(2000, 12, 31),                    nil, info)
    end
  end

  def test_load_64bit
    # Some platforms support 64-bit Times, others only 32-bit. The TZif version
    # 2 and later format contains both 32-bit and 64-bit times. 
    
    # Where 64-bit is supported and a TZif 2 or later file is provided, the 
    # 64-bit times should be used, otherwise the 32-bit information should be 
    # used.
  
    offsets = [
      {:gmtoff => 3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => 3600, :isdst => false, :abbrev => 'XST'},
      {:gmtoff => 7200, :isdst => true,  :abbrev => 'XDT'},
      {:gmtoff =>    0, :isdst => false, :abbrev => 'XNST'}]
      
    transitions = [
      {:at =>            -3786739200, :offset_index => 1}, # Time.utc(1850,  1,  2)
      {:at => Time.utc(2003,  4, 22), :offset_index => 2},
      {:at => Time.utc(2003, 10, 21), :offset_index => 1},
      {:at =>             2240524800, :offset_index => 3}] # Time.utc(2040, 12, 31)
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/SixtyFour', path)
      assert_equal('Zone/SixtyFour', info.identifier)
  
      if SUPPORTS_64BIT && format >= 2
        assert_period(:LMT,  3542,    0, false,                    nil, Time.utc(1850,  1,  2), info)
        assert_period(:XST,  3600,    0, false, Time.utc(1850,  1,  2), Time.utc(2003,  4, 22), info)
        assert_period(:XDT,  3600, 3600,  true, Time.utc(2003,  4, 22), Time.utc(2003, 10, 21), info)
        assert_period(:XST,  3600,    0, false, Time.utc(2003, 10, 21), Time.utc(2040, 12, 31), info)
        assert_period(:XNST,    0,    0, false, Time.utc(2040, 12, 31),                    nil, info)
      else
        assert_period(:LMT,  3542,    0, false,                    nil, Time.utc(2003,  4, 22), info)
        assert_period(:XDT,  3600, 3600,  true, Time.utc(2003,  4, 22), Time.utc(2003, 10, 21), info)
        assert_period(:XST,  3600,    0, false, Time.utc(2003, 10, 21),                    nil, info)
      end
    end
  end
  
  def test_load_std_offset_changes
    # The zoneinfo files don't include the offset from standard time, so this
    # has to be derived by looking at changes in the total UTC offset.
  
    offsets = [
      {:gmtoff =>  3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff =>  3600, :isdst => false, :abbrev => 'XST'},
      {:gmtoff =>  7200, :isdst => true,  :abbrev => 'XDT'},
      {:gmtoff => 10800, :isdst => true,  :abbrev => 'XDDT'}]
      
    transitions = [
      {:at => Time.utc(2000, 1, 1), :offset_index => 1},
      {:at => Time.utc(2000, 2, 1), :offset_index => 2},
      {:at => Time.utc(2000, 3, 1), :offset_index => 3},
      {:at => Time.utc(2000, 4, 1), :offset_index => 1}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/DoubleDaylight', path)
      assert_equal('Zone/DoubleDaylight', info.identifier)
  
      assert_period(:LMT,  3542,    0, false,                  nil, Time.utc(2000, 1, 1), info)
      assert_period(:XST,  3600,    0, false, Time.utc(2000, 1, 1), Time.utc(2000, 2, 1), info)
      assert_period(:XDT,  3600, 3600,  true, Time.utc(2000, 2, 1), Time.utc(2000, 3, 1), info)
      assert_period(:XDDT, 3600, 7200,  true, Time.utc(2000, 3, 1), Time.utc(2000, 4, 1), info)
      assert_period(:XST,  3600,    0, false, Time.utc(2000, 4, 1), nil, info)
    end
  end
  
  def test_load_std_offset_changes_jump_to_double_dst
    # The zoneinfo files don't include the offset from standard time, so this
    # has to be derived by looking at changes in the total UTC offset.
  
    offsets = [
      {:gmtoff =>  3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff =>  3600, :isdst => false, :abbrev => 'XST'},      
      {:gmtoff => 10800, :isdst => true,  :abbrev => 'XDDT'}]
      
    transitions = [
      {:at => Time.utc(2000, 4, 1), :offset_index => 1},
      {:at => Time.utc(2000, 5, 1), :offset_index => 2},
      {:at => Time.utc(2000, 6, 1), :offset_index => 1}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/DoubleDaylight', path)
      assert_equal('Zone/DoubleDaylight', info.identifier)
  
      assert_period(:LMT,  3542,    0, false,                  nil, Time.utc(2000, 4, 1), info)
      assert_period(:XST,  3600,    0, false, Time.utc(2000, 4, 1), Time.utc(2000, 5, 1), info)
      assert_period(:XDDT, 3600, 7200,  true, Time.utc(2000, 5, 1), Time.utc(2000, 6, 1), info)
      assert_period(:XST,  3600,    0, false, Time.utc(2000, 6, 1),                  nil, info)
    end
  end
  
  def test_load_std_offset_changes_negative
    # The zoneinfo files don't include the offset from standard time, so this
    # has to be derived by looking at changes in the total UTC offset.
  
    offsets = [
      {:gmtoff => -10821, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => -10800, :isdst => false, :abbrev => 'XST'},
      {:gmtoff =>  -7200, :isdst => true,  :abbrev => 'XDT'},
      {:gmtoff =>  -3600, :isdst => true,  :abbrev => 'XDDT'}]
      
    transitions = [
      {:at => Time.utc(2000, 1, 1), :offset_index => 1},
      {:at => Time.utc(2000, 2, 1), :offset_index => 2},
      {:at => Time.utc(2000, 3, 1), :offset_index => 3},
      {:at => Time.utc(2000, 4, 1), :offset_index => 1},
      {:at => Time.utc(2000, 5, 1), :offset_index => 3},
      {:at => Time.utc(2000, 6, 1), :offset_index => 1}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/DoubleDaylight', path)
      assert_equal('Zone/DoubleDaylight', info.identifier)
  
      assert_period(:LMT,  -10821,    0, false,                  nil, Time.utc(2000, 1, 1), info)
      assert_period(:XST,  -10800,    0, false, Time.utc(2000, 1, 1), Time.utc(2000, 2, 1), info)
      assert_period(:XDT,  -10800, 3600,  true, Time.utc(2000, 2, 1), Time.utc(2000, 3, 1), info)
      assert_period(:XDDT, -10800, 7200,  true, Time.utc(2000, 3, 1), Time.utc(2000, 4, 1), info)
      assert_period(:XST,  -10800,    0, false, Time.utc(2000, 4, 1), Time.utc(2000, 5, 1), info)
      assert_period(:XDDT, -10800, 7200,  true, Time.utc(2000, 5, 1), Time.utc(2000, 6, 1), info)
      assert_period(:XST,  -10800,    0, false, Time.utc(2000, 6, 1),                  nil, info)
    end
  end
  
  def test_load_starts_two_hour_std_offset
    # The zoneinfo files don't include the offset from standard time, so this
    # has to be derived by looking at changes in the total UTC offset.
  
    offsets = [
      {:gmtoff =>  3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff =>  3600, :isdst => false, :abbrev => 'XST'},
      {:gmtoff =>  7200, :isdst => true,  :abbrev => 'XDT'},
      {:gmtoff => 10800, :isdst => true,  :abbrev => 'XDDT'}]
      
    transitions = [
      {:at => Time.utc(2000, 1, 1), :offset_index => 3},
      {:at => Time.utc(2000, 2, 1), :offset_index => 2},
      {:at => Time.utc(2000, 3, 1), :offset_index => 1}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/DoubleDaylight', path)
      assert_equal('Zone/DoubleDaylight', info.identifier)
  
      assert_period(:LMT,  3542,    0, false,                  nil, Time.utc(2000, 1, 1), info)
      assert_period(:XDDT, 3600, 7200,  true, Time.utc(2000, 1, 1), Time.utc(2000, 2, 1), info)
      assert_period(:XDT,  3600, 3600,  true, Time.utc(2000, 2, 1), Time.utc(2000, 3, 1), info)
      assert_period(:XST,  3600,    0, false, Time.utc(2000, 3, 1),                  nil, info)
    end
  end
    
  def test_load_starts_all_same_dst_offset
    # The zoneinfo files don't include the offset from standard time, so this
    # has to be derived by looking at changes in the total UTC offset.
    #
    # If there are no changes in the UTC offset (ignoring the first offset, 
    # which is usually local mean time), then a value of 1 hour is used as the
    # standard time offset.
  
    offsets = [
      {:gmtoff => 3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => 7200, :isdst => true,  :abbrev => 'XDDT'}]
      
    transitions = [
      {:at => Time.utc(2000, 1, 1), :offset_index => 1}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/DoubleDaylight', path)
      assert_equal('Zone/DoubleDaylight', info.identifier)
  
      assert_period(:LMT,  3542,    0, false,                  nil, Time.utc(2000, 1, 1), info)
      assert_period(:XDDT, 3600, 3600,  true, Time.utc(2000, 1, 1),                  nil, info)
    end
  end
  
  def test_load_switch_to_dst_and_change_utc_offset
    # The zoneinfo files don't include the offset from standard time, so this
    # has to be derived by looking at changes in the total UTC offset.
  
    # Switch from non-DST to DST at the same time as moving the UTC offset
    # back an hour (i.e. wall clock time doesn't change).
  
    offsets = [
      {:gmtoff => 3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => 3600, :isdst => false, :abbrev => 'YST'},
      {:gmtoff => 3600, :isdst => true,  :abbrev => 'XDT'}]
      
    transitions = [
      {:at => Time.utc(2000, 1, 1), :offset_index => 1},
      {:at => Time.utc(2000, 2, 1), :offset_index => 2}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/DoubleDaylight', path)
      assert_equal('Zone/DoubleDaylight', info.identifier)
  
      assert_period(:LMT,  3542,    0, false,                  nil, Time.utc(2000, 1, 1), info)
      assert_period(:YST,  3600,    0, false, Time.utc(2000, 1, 1), Time.utc(2000, 2, 1), info)
      assert_period(:XDT,     0, 3600,  true, Time.utc(2000, 2, 1), nil, info)
    end
  end
  
  def test_load_in_safe_mode
    offsets = [{:gmtoff => -12094, :isdst => false, :abbrev => 'LT'}]
        
    tzif_test(offsets, []) do |path, format|
      # untaint only required for Ruby 1.9.2
      path.untaint
      
      safe_test do        
        info = ZoneinfoTimezoneInfo.new('Zone/three', path)
        assert_equal('Zone/three', info.identifier)
        
        assert_period(:LT, -12094, 0, false, nil, nil, info)
      end
    end
  end
  
  def test_load_encoding
    # tzfile.5 doesn't specify an encoding, but the source data is in ASCII.
    # ZoneinfoTimezoneInfo will load as UTF-8 (a superset of ASCII).
  
    offsets = [
      {:gmtoff => 3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => 3600, :isdst => false, :abbrev => 'XST©'}]
      
    transitions = [
      {:at => Time.utc(1971,  1,  2), :offset_index => 1}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/One', path)
      assert_equal('Zone/One', info.identifier)
      
      assert_period(:LMT,     3542,    0, false,                    nil, Time.utc(1971,  1,  2), info)
      assert_period(:"XST©",  3600,    0, false, Time.utc(1971,  1,  2),                    nil, info)
    end
  end
  
  def test_load_binmode
    offsets = [
      {:gmtoff => 3542, :isdst => false, :abbrev => 'LMT'},
      {:gmtoff => 3600, :isdst => false, :abbrev => 'XST'}]
      
    # Transition time that includes CRLF (4EFF0D0A).
    # Test that this doesn't get corrupted by translating CRLF to LF.
    transitions = [
      {:at => Time.utc(2011, 12, 31, 13, 24, 26), :offset_index => 1}]
  
    tzif_test(offsets, transitions) do |path, format|     
      info = ZoneinfoTimezoneInfo.new('Zone/One', path)
      assert_equal('Zone/One', info.identifier)
      
      assert_period(:LMT, 3542, 0, false,                                nil, Time.utc(2011, 12, 31, 13, 24, 26), info)
      assert_period(:XST, 3600, 0, false, Time.utc(2011, 12, 31, 13, 24, 26),                                nil, info)
    end
  end
end
