require 'cases/helper'
require 'models/topic'

class InvalidDateTest < Test::Unit::TestCase
  def test_assign_valid_dates
    valid_dates = [[2007, 11, 30], [1993, 2, 28], [2008, 2, 29]]

    invalid_dates = [[2007, 11, 31], [1993, 2, 29], [2007, 2, 29]]

    topic = Topic.new

    valid_dates.each do |date_src|
      topic = Topic.new("last_read(1i)" => date_src[0].to_s, "last_read(2i)" => date_src[1].to_s, "last_read(3i)" => date_src[2].to_s)
      assert_equal(topic.last_read, Date.new(*date_src))
    end

    invalid_dates.each do |date_src|
      assert_nothing_raised do
        topic = Topic.new({"last_read(1i)" => date_src[0].to_s, "last_read(2i)" => date_src[1].to_s, "last_read(3i)" => date_src[2].to_s})
        assert_equal(topic.last_read, Time.local(*date_src).to_date, "The date should be modified according to the behaviour of the Time object")
      end
    end
  end
end
