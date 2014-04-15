require 'ffi'
require 'java'

module Process
  class Foreign
    SC_CLK_TCK = com.kenai.constantine.platform.Sysconf::_SC_CLK_TCK.value

    extend FFI::Library
    class Times < FFI::Struct
      layout \
        :utime => :long, 
        :stime => :long,
        :cutime => :long,
        :cstime => :long
    end
    attach_function :times, [ :buffer_out ], :long
    attach_function :sysconf, [ :int ], :long
    Tms = Struct.new("Foreign::Tms", :utime, :stime, :cutime, :cstime)    
  end
  def self.times
    hz = Foreign.sysconf(Foreign::SC_CLK_TCK).to_f
    t = Foreign::Times.alloc_out(false)
    Foreign.times(t.pointer)
    Foreign::Tms.new(t[:utime] / hz, t[:stime] / hz, t[:cutime] / hz, t[:cstime] / hz)
  end
  
end
if $0 == __FILE__
  while true
    10.times { system("ls -l / > /dev/null") }
    t = Process.times
    puts "utime=#{t.utime} stime=#{t.stime} cutime=#{t.cutime} cstime=#{t.cstime}"
    sleep 1
  end
end
