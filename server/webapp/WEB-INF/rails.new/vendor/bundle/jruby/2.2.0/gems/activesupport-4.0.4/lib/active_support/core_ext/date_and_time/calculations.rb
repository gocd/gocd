module DateAndTime
  module Calculations
    DAYS_INTO_WEEK = {
      :monday    => 0,
      :tuesday   => 1,
      :wednesday => 2,
      :thursday  => 3,
      :friday    => 4,
      :saturday  => 5,
      :sunday    => 6
    }

    # Returns a new date/time representing yesterday.
    def yesterday
      advance(:days => -1)
    end

    # Returns a new date/time representing tomorrow.
    def tomorrow
      advance(:days => 1)
    end

    # Returns true if the date/time is today.
    def today?
      to_date == ::Date.current
    end

    # Returns true if the date/time is in the past.
    def past?
      self < self.class.current
    end

    # Returns true if the date/time is in the future.
    def future?
      self > self.class.current
    end

    # Returns a new date/time the specified number of days ago.
    def days_ago(days)
      advance(:days => -days)
    end

    # Returns a new date/time the specified number of days in the future.
    def days_since(days)
      advance(:days => days)
    end

    # Returns a new date/time the specified number of weeks ago.
    def weeks_ago(weeks)
      advance(:weeks => -weeks)
    end

    # Returns a new date/time the specified number of weeks in the future.
    def weeks_since(weeks)
      advance(:weeks => weeks)
    end

    # Returns a new date/time the specified number of months ago.
    def months_ago(months)
      advance(:months => -months)
    end

    # Returns a new date/time the specified number of months in the future.
    def months_since(months)
      advance(:months => months)
    end

    # Returns a new date/time the specified number of years ago.
    def years_ago(years)
      advance(:years => -years)
    end

    # Returns a new date/time the specified number of years in the future.
    def years_since(years)
      advance(:years => years)
    end

    # Returns a new date/time at the start of the month.
    # DateTime objects will have a time set to 0:00.
    def beginning_of_month
      first_hour{ change(:day => 1) }
    end
    alias :at_beginning_of_month :beginning_of_month

    # Returns a new date/time at the start of the quarter.
    # Example: 1st January, 1st July, 1st October.
    # DateTime objects will have a time set to 0:00.
    def beginning_of_quarter
      first_quarter_month = [10, 7, 4, 1].detect { |m| m <= month }
      beginning_of_month.change(:month => first_quarter_month)
    end
    alias :at_beginning_of_quarter :beginning_of_quarter

    # Returns a new date/time at the end of the quarter.
    # Example: 31st March, 30th June, 30th September.
    # DateTime objects will have a time set to 23:59:59.
    def end_of_quarter
      last_quarter_month = [3, 6, 9, 12].detect { |m| m >= month }
      beginning_of_month.change(:month => last_quarter_month).end_of_month
    end
    alias :at_end_of_quarter :end_of_quarter

    # Return a new date/time at the beginning of the year.
    # Example: 1st January.
    # DateTime objects will have a time set to 0:00.
    def beginning_of_year
      change(:month => 1).beginning_of_month
    end
    alias :at_beginning_of_year :beginning_of_year

    # Returns a new date/time representing the given day in the next week.
    # The +given_day_in_next_week+ defaults to the beginning of the week
    # which is determined by +Date.beginning_of_week+ or +config.beginning_of_week+
    # when set. +DateTime+ objects have their time set to 0:00.
    def next_week(given_day_in_next_week = Date.beginning_of_week)
      first_hour{ weeks_since(1).beginning_of_week.days_since(days_span(given_day_in_next_week)) }
    end

    # Short-hand for months_since(1).
    def next_month
      months_since(1)
    end

    # Short-hand for months_since(3)
    def next_quarter
      months_since(3)
    end

    # Short-hand for years_since(1).
    def next_year
      years_since(1)
    end

    # Returns a new date/time representing the given day in the previous week.
    # Week is assumed to start on +start_day+, default is
    # +Date.beginning_of_week+ or +config.beginning_of_week+ when set.
    # DateTime objects have their time set to 0:00.
    def prev_week(start_day = Date.beginning_of_week)
      first_hour{ weeks_ago(1).beginning_of_week.days_since(days_span(start_day)) }
    end
    alias_method :last_week, :prev_week

    # Short-hand for months_ago(1).
    def prev_month
      months_ago(1)
    end
    alias_method :last_month, :prev_month

    # Short-hand for months_ago(3).
    def prev_quarter
      months_ago(3)
    end
    alias_method :last_quarter, :prev_quarter

    # Short-hand for years_ago(1).
    def prev_year
      years_ago(1)
    end
    alias_method :last_year, :prev_year

    # Returns the number of days to the start of the week on the given day.
    # Week is assumed to start on +start_day+, default is
    # +Date.beginning_of_week+ or +config.beginning_of_week+ when set.
    def days_to_week_start(start_day = Date.beginning_of_week)
      start_day_number = DAYS_INTO_WEEK[start_day]
      current_day_number = wday != 0 ? wday - 1 : 6
      (current_day_number - start_day_number) % 7
    end

    # Returns a new date/time representing the start of this week on the given day.
    # Week is assumed to start on +start_day+, default is
    # +Date.beginning_of_week+ or +config.beginning_of_week+ when set.
    # +DateTime+ objects have their time set to 0:00.
    def beginning_of_week(start_day = Date.beginning_of_week)
      result = days_ago(days_to_week_start(start_day))
      acts_like?(:time) ? result.midnight : result
    end
    alias :at_beginning_of_week :beginning_of_week

    # Returns Monday of this week assuming that week starts on Monday.
    # +DateTime+ objects have their time set to 0:00.
    def monday
      beginning_of_week(:monday)
    end

    # Returns a new date/time representing the end of this week on the given day.
    # Week is assumed to start on +start_day+, default is
    # +Date.beginning_of_week+ or +config.beginning_of_week+ when set.
    # DateTime objects have their time set to 23:59:59.
    def end_of_week(start_day = Date.beginning_of_week)
      last_hour{ days_since(6 - days_to_week_start(start_day)) }
    end
    alias :at_end_of_week :end_of_week

    # Returns Sunday of this week assuming that week starts on Monday.
    # +DateTime+ objects have their time set to 23:59:59.
    def sunday
      end_of_week(:monday)
    end

    # Returns a new date/time representing the end of the month.
    # DateTime objects will have a time set to 23:59:59.
    def end_of_month
      last_day = ::Time.days_in_month(month, year)
      last_hour{ days_since(last_day - day) }
    end
    alias :at_end_of_month :end_of_month

    # Returns a new date/time representing the end of the year.
    # DateTime objects will have a time set to 23:59:59.
    def end_of_year
      change(:month => 12).end_of_month
    end
    alias :at_end_of_year :end_of_year

    private

    def first_hour
      result = yield
      acts_like?(:time) ? result.change(:hour => 0) : result
    end

    def last_hour
      result = yield
      acts_like?(:time) ? result.end_of_day : result
    end

    def days_span(day)
      (DAYS_INTO_WEEK[day] - DAYS_INTO_WEEK[Date.beginning_of_week]) % 7
    end
  end
end
