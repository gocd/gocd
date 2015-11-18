require 'java'
require 'jruby'

class String
  RubyString = org.jruby.RubyString
  
  # Construct a new string with a buffer of the specified size. The buffer is
  # filled with null bytes to start.
  # 
  # May be useful in cases where you know how large a string will grow, and want
  # to pre-allocate the buffer for that size.
  def self.alloc(size)
    RubyString.new_string_light(JRuby.runtime, size)
  end
end