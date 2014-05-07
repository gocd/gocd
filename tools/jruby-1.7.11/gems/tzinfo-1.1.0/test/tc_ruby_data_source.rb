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

include TZInfo

class TCRubyDataSource < Test::Unit::TestCase
  def setup
    @data_source = RubyDataSource.new
  end
  
  def test_load_timezone_info_data
    info = @data_source.load_timezone_info('Europe/London')
    assert_kind_of(DataTimezoneInfo, info)
    assert_equal('Europe/London', info.identifier)
  end
  
  def test_load_timezone_info_linked
    info = @data_source.load_timezone_info('UTC')
    assert_kind_of(LinkedTimezoneInfo, info)
    assert_equal('UTC', info.identifier)
    assert_equal('Etc/UTC', info.link_to_identifier)
  end
  
  def test_load_timezone_info_does_not_exist
    assert_raises(InvalidTimezoneIdentifier) do
      @data_source.load_timezone_info('Nowhere/Special')
    end
  end
  
  def test_load_timezone_info_invalid
    assert_raises(InvalidTimezoneIdentifier) do
      @data_source.load_timezone_info('../Definitions/UTC')
    end
  end
  
  def test_load_timezone_info_nil
    assert_raises(InvalidTimezoneIdentifier) do
      @data_source.load_timezone_info(nil)
    end
  end
  
  def test_load_timezone_info_case
    assert_raises(InvalidTimezoneIdentifier) do
      @data_source.load_timezone_info('europe/london')
    end
  end

  def test_load_timezone_info_plus
    info = @data_source.load_timezone_info('Etc/GMT+1')
    assert_equal('Etc/GMT+1', info.identifier)
  end
  
  def test_load_timezone_info_minus
    info = @data_source.load_timezone_info('Etc/GMT-1')
    assert_equal('Etc/GMT-1', info.identifier)
  end

  def test_load_timezone_info_tainted
    safe_test do
      identifier = 'Europe/Amsterdam'.taint
      assert(identifier.tainted?)
      info = @data_source.load_timezone_info(identifier)
      assert_equal('Europe/Amsterdam', info.identifier)
      assert(identifier.tainted?)
    end
  end
  
  def test_load_timezone_info_tainted_and_frozen
    safe_test do
      info = @data_source.load_timezone_info('Europe/Amsterdam'.taint.freeze)
      assert_equal('Europe/Amsterdam', info.identifier)
    end
  end
  
  def test_timezone_identifiers
    all = @data_source.timezone_identifiers
    assert_equal(TZInfo::Data::Indexes::Timezones.timezones, all)
    assert_equal(true, all.frozen?)
  end
  
  def test_data_timezone_identifiers
    all_data = @data_source.data_timezone_identifiers
    assert_equal(TZInfo::Data::Indexes::Timezones.data_timezones, all_data)
    assert_equal(true, all_data.frozen?)
  end
  
  def test_linked_timezone_identifiers
    all_linked = @data_source.linked_timezone_identifiers
    assert_equal(TZInfo::Data::Indexes::Timezones.linked_timezones, all_linked)
    assert_equal(true, all_linked.frozen?)
  end
  
  def test_load_country_info
    info = @data_source.load_country_info('GB')
    assert_equal('GB', info.code)
  end
    
  def test_load_country_info_not_exist
    assert_raises(InvalidCountryCode) do
      @data_source.load_country_info('ZZ')
    end
  end
  
  def test_load_country_info_invalid
    assert_raises(InvalidCountryCode) do
      @data_source.load_country_info('../Countries/GB')
    end
  end
  
  def test_load_country_info_nil
    assert_raises(InvalidCountryCode) do
      @data_source.load_country_info(nil)
    end
  end
  
  def test_load_country_info_case
    assert_raises(InvalidCountryCode) do
      @data_source.load_country_info('gb')
    end
  end
  
  def test_load_country_info_tainted
    safe_test do
      code = 'NL'.taint
      assert(code.tainted?)
      info = @data_source.load_country_info(code)
      assert_equal('NL', info.code)
      assert(code.tainted?)
    end
  end
  
  def test_load_country_info_tainted_and_frozen
    safe_test do
      info = @data_source.load_country_info('NL'.taint.freeze)
      assert_equal('NL', info.code)
    end
  end
  
  def test_country_codes
    codes = @data_source.country_codes
    assert_equal(TZInfo::Data::Indexes::Countries.countries.keys, codes)
    assert_equal(true, codes.frozen?)
  end
end
