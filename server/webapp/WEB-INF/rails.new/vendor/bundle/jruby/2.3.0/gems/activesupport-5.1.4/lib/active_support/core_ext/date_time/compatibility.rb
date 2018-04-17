require "active_support/core_ext/date_and_time/compatibility"
require "active_support/core_ext/module/remove_method"

class DateTime
  include DateAndTime::Compatibility

  remove_possible_method :to_time

  # Either return an instance of `Time` with the same UTC offset
  # as +self+ or an instance of `Time` representing the same time
  # in the the local system timezone depending on the setting of
  # on the setting of +ActiveSupport.to_time_preserves_timezone+.
  def to_time
    preserve_timezone ? getlocal(utc_offset) : getlocal
  end
end
