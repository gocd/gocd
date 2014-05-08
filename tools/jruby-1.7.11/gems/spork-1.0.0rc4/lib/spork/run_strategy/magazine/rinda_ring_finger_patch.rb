# Patch for Rinda::RingFinger.primary hanging forever on Ruby 1.9.2 & 1.9.3
# from http://www.ruby-forum.com/topic/4229908
require 'rinda/ring'

module Rinda
  class RingFinger
    def lookup_ring_any(timeout=5)
      queue = Queue.new

      Thread.new do
        self.lookup_ring(timeout) do |ts|
          queue.push(ts)
        end
        queue.push(nil)
      end

      @primary = queue.pop
      raise('RingNotFound') if @primary.nil?
      while it = queue.pop
        @rings.push(it)
      end

      @primary
    end
  end
end
