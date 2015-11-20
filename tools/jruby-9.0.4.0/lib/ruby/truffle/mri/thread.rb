# Truffle: Last version of lib/thread.rb in MRI @ r42801 (324df61e).

#
#               thread.rb - thread support classes
#                       by Yukihiro Matsumoto <matz@netlab.co.jp>
#
# Copyright (C) 2001  Yukihiro Matsumoto
# Copyright (C) 2000  Network Applied Communication Laboratory, Inc.
# Copyright (C) 2000  Information-technology Promotion Agency, Japan
#

unless defined? Thread
  raise "Thread not available for this ruby interpreter"
end

unless defined? ThreadError
  class ThreadError < StandardError
  end
end

if $DEBUG
  Thread.abort_on_exception = true
end

#
# ConditionVariable objects augment class Mutex. Using condition variables,
# it is possible to suspend while in the middle of a critical section until a
# resource becomes available.
#
# Example:
#
#   require 'thread'
#
#   mutex = Mutex.new
#   resource = ConditionVariable.new
#
#   a = Thread.new {
#     mutex.synchronize {
#       # Thread 'a' now needs the resource
#       resource.wait(mutex)
#       # 'a' can now have the resource
#     }
#   }
#
#   b = Thread.new {
#     mutex.synchronize {
#       # Thread 'b' has finished using the resource
#       resource.signal
#     }
#   }
#
class ConditionVariable
  #
  # Creates a new ConditionVariable
  #
  def initialize
    @waiters = {}
    @waiters_mutex = Mutex.new
  end

  #
  # Releases the lock held in +mutex+ and waits; reacquires the lock on wakeup.
  #
  # If +timeout+ is given, this method returns after +timeout+ seconds passed,
  # even if no other thread has signaled.
  #
  def wait(mutex, timeout=nil)
    Thread.handle_interrupt(StandardError => :never) do
      begin
        Thread.handle_interrupt(StandardError => :on_blocking) do
          @waiters_mutex.synchronize do
            @waiters[Thread.current] = true
          end
          mutex.sleep timeout
        end
      ensure
        @waiters_mutex.synchronize do
          @waiters.delete(Thread.current)
        end
      end
    end
    self
  end

  #
  # Wakes up the first thread in line waiting for this lock.
  #
  def signal
    Thread.handle_interrupt(StandardError => :on_blocking) do
      begin
        t, _ = @waiters_mutex.synchronize { @waiters.shift }
        t.run if t
      rescue ThreadError
        retry # t was already dead?
      end
    end
    self
  end

  #
  # Wakes up all threads waiting for this lock.
  #
  def broadcast
    Thread.handle_interrupt(StandardError => :on_blocking) do
      threads = nil
      @waiters_mutex.synchronize do
        threads = @waiters.keys
        @waiters.clear
      end
      for t in threads
        begin
          t.run
        rescue ThreadError
        end
      end
    end
    self
  end

  # Truffle: define marshal_dump as MRI tests expect it
  def marshal_dump
    raise TypeError, "can't dump #{self.class}"
  end

end

# Truffle: Queue and SizedQueue are defined in Java
