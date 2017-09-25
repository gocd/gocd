require 'thread'

module Debugger
  class Interface
  end

  class LocalInterface < Interface
  end


  class RemoteInterface < Interface # :nodoc:
    attr_accessor :command_queue

    def initialize(socket)
      @socket = socket
      @command_queue = Queue.new
    end
    
    def read_command
      result = non_blocking_gets
      raise IOError unless result
      result.chomp
    end

    def print(*args)
      @socket.printf(*args)
    end
    
    def close
      @socket.close
    rescue IOError, SystemCallError
    end

    # Workaround for JRuby issue http://jira.codehaus.org/browse/JRUBY-2063
    def non_blocking_gets
      loop do
        result, _, _ = IO.select( [@socket], nil, nil, 0.2 )
        next unless result
        return result[0].gets
      end
    end

  end
  
end
