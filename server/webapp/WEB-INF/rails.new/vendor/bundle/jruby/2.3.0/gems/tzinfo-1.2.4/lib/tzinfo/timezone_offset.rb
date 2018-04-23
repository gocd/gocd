module TZInfo
  # Represents an offset defined in a Timezone data file.
  class TimezoneOffset
    # The base offset of the timezone from UTC in seconds.
    attr_reader :utc_offset
    
    # The offset from standard time for the zone in seconds (i.e. non-zero if 
    # daylight savings is being observed).
    attr_reader :std_offset
    
    # The total offset of this observance from UTC in seconds 
    # (utc_offset + std_offset).
    attr_reader :utc_total_offset
    
    # The abbreviation that identifies this observance, e.g. "GMT" 
    # (Greenwich Mean Time) or "BST" (British Summer Time) for "Europe/London". The returned identifier is a 
    # symbol.
    attr_reader :abbreviation
    
    # Constructs a new TimezoneOffset. utc_offset and std_offset are specified 
    # in seconds.
    def initialize(utc_offset, std_offset, abbreviation)
      @utc_offset = utc_offset
      @std_offset = std_offset      
      @abbreviation = abbreviation
      
      @utc_total_offset = @utc_offset + @std_offset
    end
    
    # True if std_offset is non-zero.
    def dst?
      @std_offset != 0
    end
    
    # Converts a UTC Time, DateTime or integer timestamp to local time, based on 
    # the offset of this period.
    #
    # Deprecation warning: this method will be removed in TZInfo version 2.0.0.
    def to_local(utc)
      TimeOrDateTime.wrap(utc) {|wrapped|
        wrapped + @utc_total_offset
      }
    end
    
    # Converts a local Time, DateTime or integer timestamp to UTC, based on the
    # offset of this period.
    #
    # Deprecation warning: this method will be removed in TZInfo version 2.0.0.
    def to_utc(local)
      TimeOrDateTime.wrap(local) {|wrapped|
        wrapped - @utc_total_offset
      }
    end
    
    # Returns true if and only if toi has the same utc_offset, std_offset
    # and abbreviation as this TimezoneOffset.
    def ==(toi)
      toi.kind_of?(TimezoneOffset) &&
        utc_offset == toi.utc_offset && std_offset == toi.std_offset && abbreviation == toi.abbreviation
    end
    
    # Returns true if and only if toi has the same utc_offset, std_offset
    # and abbreviation as this TimezoneOffset.
    def eql?(toi)
      self == toi
    end
    
    # Returns a hash of this TimezoneOffset.
    def hash
      utc_offset.hash ^ std_offset.hash ^ abbreviation.hash
    end
    
    # Returns internal object state as a programmer-readable string.
    def inspect
      "#<#{self.class}: #@utc_offset,#@std_offset,#@abbreviation>"
    end
  end
end
