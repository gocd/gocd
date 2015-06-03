# Knows to render a java date to JSON
# Inspired from `active_support/json/encoding.rb`
class Java::JavaUtil::Date
  def as_json(options = nil) #:nodoc:
    org.apache.commons.lang.time.FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SZZ").format(self)
  end
end
