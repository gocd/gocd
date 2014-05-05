require 'abstract_unit'

class DateHelperDistanceOfTimeInWordsI18nTests < Test::Unit::TestCase
  include ActionView::Helpers::DateHelper
  attr_reader :request

  def setup
    @from = Time.mktime(2004, 6, 6, 21, 45, 0)
  end

  # distance_of_time_in_words

  def test_distance_of_time_in_words_calls_i18n
    { # with include_seconds
      [2.seconds,  true]  => [:'less_than_x_seconds', 5],
      [9.seconds,  true]  => [:'less_than_x_seconds', 10],
      [19.seconds, true]  => [:'less_than_x_seconds', 20],
      [30.seconds, true]  => [:'half_a_minute',       nil],
      [59.seconds, true]  => [:'less_than_x_minutes', 1],
      [60.seconds, true]  => [:'x_minutes',           1],

      # without include_seconds
      [29.seconds,          false] => [:'less_than_x_minutes', 1],
      [60.seconds,          false] => [:'x_minutes',           1],
      [44.minutes,          false] => [:'x_minutes',           44],
      [61.minutes,          false] => [:'about_x_hours',       1],
      [24.hours,            false] => [:'x_days',              1],
      [30.days,             false] => [:'about_x_months',      1],
      [60.days,             false] => [:'x_months',            2],
      [1.year,              false] => [:'about_x_years',       1],
      [3.years + 6.months,  false] => [:'over_x_years',        3],
      [3.years + 10.months, false] => [:'almost_x_years',      4]

      }.each do |passed, expected|
      assert_distance_of_time_in_words_translates_key passed, expected
    end
  end

  def assert_distance_of_time_in_words_translates_key(passed, expected)
    diff, include_seconds = *passed
    key, count = *expected
    to = @from + diff

    options = {:locale => 'en', :scope => :'datetime.distance_in_words'}
    options[:count] = count if count

    I18n.expects(:t).with(key, options)
    distance_of_time_in_words(@from, to, include_seconds, :locale => 'en')
  end

  def test_distance_of_time_pluralizations
    { [:'less_than_x_seconds', 1]   => 'less than 1 second',
      [:'less_than_x_seconds', 2]   => 'less than 2 seconds',
      [:'less_than_x_minutes', 1]   => 'less than a minute',
      [:'less_than_x_minutes', 2]   => 'less than 2 minutes',
      [:'x_minutes',           1]   => '1 minute',
      [:'x_minutes',           2]   => '2 minutes',
      [:'about_x_hours',       1]   => 'about 1 hour',
      [:'about_x_hours',       2]   => 'about 2 hours',
      [:'x_days',              1]   => '1 day',
      [:'x_days',              2]   => '2 days',
      [:'about_x_years',       1]   => 'about 1 year',
      [:'about_x_years',       2]   => 'about 2 years',
      [:'over_x_years',        1]   => 'over 1 year',
      [:'over_x_years',        2]   => 'over 2 years' 

      }.each do |args, expected|
      key, count = *args
      assert_equal expected, I18n.t(key, :count => count, :scope => 'datetime.distance_in_words')
    end
  end
end

class DateHelperSelectTagsI18nTests < Test::Unit::TestCase
  include ActionView::Helpers::DateHelper
  attr_reader :request

  def setup
    @prompt_defaults = {:year => 'Year', :month => 'Month', :day => 'Day', :hour => 'Hour', :minute => 'Minute', :second => 'Seconds'}

    I18n.stubs(:translate).with(:'date.month_names', :locale => 'en').returns Date::MONTHNAMES
  end

  # select_month

  def test_select_month_given_use_month_names_option_does_not_translate_monthnames
    I18n.expects(:translate).never
    select_month(8, :locale => 'en', :use_month_names => Date::MONTHNAMES)
  end

  def test_select_month_translates_monthnames
    I18n.expects(:translate).with(:'date.month_names', :locale => 'en').returns Date::MONTHNAMES
    select_month(8, :locale => 'en')
  end

  def test_select_month_given_use_short_month_option_translates_abbr_monthnames
    I18n.expects(:translate).with(:'date.abbr_month_names', :locale => 'en').returns Date::ABBR_MONTHNAMES
    select_month(8, :locale => 'en', :use_short_month => true)
  end

  def test_date_or_time_select_translates_prompts
    @prompt_defaults.each do |key, prompt|
      I18n.expects(:translate).with(('datetime.prompts.' + key.to_s).to_sym, :locale => 'en').returns prompt
    end

    I18n.expects(:translate).with(:'date.order', :locale => 'en').returns [:year, :month, :day]
    datetime_select('post', 'updated_at', :locale => 'en', :include_seconds => true, :prompt => true)
  end

  # date_or_time_select

  def test_date_or_time_select_given_an_order_options_does_not_translate_order
    I18n.expects(:translate).never
    datetime_select('post', 'updated_at', :order => [:year, :month, :day], :locale => 'en')
  end

  def test_date_or_time_select_given_no_order_options_translates_order
    I18n.expects(:translate).with(:'date.order', :locale => 'en').returns [:year, :month, :day]
    datetime_select('post', 'updated_at', :locale => 'en')
  end
end
