module Krypt
  
  # Abstract class that represents filters that can be combined with ordinary
  # IO instances, filtering the output before reading/writing to the underlying
  # IO. IOFilter instances can be stacked on top of each other, forming a 
  # "filter chain" that "peels of" multiple layers of encoding for example. 
  #
  # IOFilter supports a basic IO interface that responds to IO#read, IO#write
  # and IO#close.
  #
  # When reading from the IOFilter, the data will first be read from the IO,
  # processed according to the rules of the filter and only then passed on.
  #
  # When writing to the IOFilter, the data will first be processed by
  # applying the filter and only then written to the IO instance.
  #
  # Closing the IOFilter with IOFilter#close guarantees (among possibly
  # additional things) a call to IO#close on the underlying IO.
  class IOFilter
    
    #
    # call-seq: 
    #    IOFilter.new(io) [{ |filter| block }] -> IOFilter
    #
    # Constructs a new IOFilter with +io+ as its underlying IO. 
    # Takes an optional block which is yielded the IOFilter +filter+.
    # After execution of the block, it is guaranteed that IOFilter#close
    # gets called on the IOFilter.
    #
    def initialize(io)
      @io = io
      if block_given?
        begin
          yield self
        ensure
          close
        end
      end
    end

    #
    # call-seq:
    #    io.close -> nil
    #
    # Calls, among possibly additional cleanup, IO#close on the underlying
    # IO.
    def close
      @io.close
    end
  end

  
end

require_relative 'codec/hex'
require_relative 'codec/base64'

