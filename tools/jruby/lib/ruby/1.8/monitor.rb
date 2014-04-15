=begin

= monitor.rb

Copyright (C) 2001  Shugo Maeda <shugo@ruby-lang.org>
Copyright (C) 2008  MenTaLguY <mental@rydia.net>

This library is distributed under the terms of the Ruby license.
You can freely distribute/modify this library.

== example

This is a simple example.

  require 'monitor.rb'
  
  buf = []
  buf.extend(MonitorMixin)
  empty_cond = buf.new_cond
  
  # consumer
  Thread.start do
    loop do
      buf.synchronize do
        empty_cond.wait_while { buf.empty? }
        print buf.shift
      end
    end
  end
  
  # producer
  while line = ARGF.gets
    buf.synchronize do
      buf.push(line)
      empty_cond.signal
    end
  end

The consumer thread waits for the producer thread to push a line
to buf while buf.empty?, and the producer thread (main thread)
reads a line from ARGF and push it to buf, then call
empty_cond.signal.

=end
  
require 'thread'

#
# Adds monitor functionality to an arbitrary object by mixing the module with
# +include+.  For example:
#
#    require 'monitor.rb'
#    
#    buf = []
#    buf.extend(MonitorMixin)
#    empty_cond = buf.new_cond
#    
#    # consumer
#    Thread.start do
#      loop do
#        buf.synchronize do
#          empty_cond.wait_while { buf.empty? }
#          print buf.shift
#        end
#      end
#    end
#    
#    # producer
#    while line = ARGF.gets
#      buf.synchronize do
#        buf.push(line)
#        empty_cond.signal
#      end
#    end
# 
# The consumer thread waits for the producer thread to push a line
# to buf while buf.empty?, and the producer thread (main thread)
# reads a line from ARGF and push it to buf, then call
# empty_cond.signal.
#
module MonitorMixin
  #
  # FIXME: This isn't documented in Nutshell.
  #
  # Since MonitorMixin.new_cond returns a ConditionVariable, and the example
  # above calls while_wait and signal, this class should be documented.
  #
  class ConditionVariable
    # Create a new timer with the argument timeout, and add the
    # current thread to the list of waiters.  Then the thread is
    # stopped.  It will be resumed when a corresponding #signal 
    # occurs.
    def wait(timeout = nil)
      condition = @condition
      @monitor.instance_eval { mon_wait_for_cond(condition, timeout) }
    end

    # call #wait while the supplied block returns +true+.
    def wait_while
      while yield
	wait
      end
    end
    
    # call #wait until the supplied block returns +true+.
    def wait_until
      until yield
	wait
      end
    end
    
    # Wake up and run the next waiter
    def signal
      condition = @condition
      @monitor.instance_eval { mon_signal_cond(condition) }
      nil
    end
    
    # Wake up all the waiters.
    def broadcast
      condition = @condition
      @monitor.instance_eval { mon_broadcast_cond(condition) }
      nil
    end
    
    def count_waiters
      condition = @condition
      @monitor.instance_eval { mon_count_cond_waiters(condition) }
    end
    
    private

    def initialize(monitor, condition)
      @monitor = monitor
      @condition = condition
    end
  end
  
  def self.extend_object(obj)
    super(obj)
    obj.instance_eval {mon_initialize()}
  end
  
  #
  # Attempts to enter exclusive section.  Returns +false+ if lock fails.
  #
  def mon_try_enter
    @mon_mutex.synchronize do
      @mon_owner = Thread.current unless @mon_owner
      if @mon_owner == Thread.current
        @mon_count += 1
        true
      else
        false
      end
    end
  end
  # For backward compatibility
  alias try_mon_enter mon_try_enter

  #
  # Enters exclusive section.
  #
  def mon_enter
    @mon_mutex.synchronize do
      mon_acquire(@mon_entering_cond)
      @mon_count += 1
    end
  end
  
  #
  # Leaves exclusive section.
  #
  def mon_exit
    @mon_mutex.synchronize do
      mon_check_owner
      @mon_count -= 1
      mon_release if @mon_count.zero?
      nil
    end
  end

  #
  # Enters exclusive section and executes the block.  Leaves the exclusive
  # section automatically when the block exits.  See example under
  # +MonitorMixin+.
  #
  def mon_synchronize
    mon_enter
    begin
      yield
    ensure
      mon_exit
    end
  end
  alias synchronize mon_synchronize
  
  #
  # FIXME: This isn't documented in Nutshell.
  # 
  # Create a new condition variable for this monitor.
  # This facilitates control of the monitor with #signal and #wait.
  #
  def new_cond
    condition = ::ConditionVariable.new
    condition.instance_eval { @mon_n_waiters = 0 }
    return ConditionVariable.new(self, condition)
  end

  private

  def initialize(*args)
    super
    mon_initialize
  end

  # called by initialize method to set defaults for instance variables.
  def mon_initialize
    @mon_mutex = Mutex.new
    @mon_owner = nil
    @mon_count = 0
    @mon_total_waiting = 0
    @mon_entering_cond = ::ConditionVariable.new
    @mon_waiting_cond = ::ConditionVariable.new
    self
  end

  # Throw a ThreadError exception if the current thread
  # does't own the monitor
  def mon_check_owner
    # called with @mon_mutex held
    if @mon_owner != Thread.current
      raise ThreadError, "current thread not owner"
    end
  end

  def mon_acquire(condition)
    # called with @mon_mutex held
    while @mon_owner && @mon_owner != Thread.current
      condition.wait @mon_mutex
    end
    @mon_owner = Thread.current
  end

  def mon_release
    # called with @mon_mutex held
    @mon_owner = nil
    if @mon_total_waiting.nonzero?
      @mon_waiting_cond.signal
    else
      @mon_entering_cond.signal
    end
  end

  def mon_wait_for_cond(condition, timeout)
    @mon_mutex.synchronize do
      mon_check_owner
      count = @mon_count
      @mon_count = 0
      condition.instance_eval { @mon_n_waiters += 1 }
      begin
        mon_release
        if timeout
          condition.wait(@mon_mutex, timeout)
        else
          condition.wait(@mon_mutex)
          true
        end
      ensure
        @mon_total_waiting += 1
        # TODO: not interrupt-safe
        mon_acquire(@mon_waiting_cond)
        @mon_total_waiting -= 1
        @mon_count = count
        condition.instance_eval { @mon_n_waiters -= 1 }
      end
    end
  end

  def mon_signal_cond(condition)
    @mon_mutex.synchronize do
      mon_check_owner
      condition.signal
    end
  end

  def mon_broadcast_cond(condition)
    @mon_mutex.synchronize do
      mon_check_owner
      condition.broadcast
    end
  end

  def mon_count_cond_waiters(condition)
    @mon_mutex.synchronize do
      condition.instance_eval { @mon_n_waiters }
    end
  end
end

# Monitors provide means of mutual exclusion for Thread programming.
# A critical region is created by means of the synchronize method,
# which takes a block.
# The condition variables (created with #new_cond) may be used 
# to control the execution of a monitor with #signal and #wait.
#
# the Monitor class wraps MonitorMixin, and provides aliases
#  alias try_enter try_mon_enter
#  alias enter mon_enter
#  alias exit mon_exit
# to access its methods more concisely.
class Monitor
  include MonitorMixin
  alias try_enter try_mon_enter
  alias enter mon_enter
  alias exit mon_exit
end


# Documentation comments:
#  - All documentation comes from Nutshell.
#  - MonitorMixin.new_cond appears in the example, but is not documented in
#    Nutshell.
#  - All the internals (internal modules Accessible and Initializable, class
#    ConditionVariable) appear in RDoc.  It might be good to hide them, by
#    making them private, or marking them :nodoc:, etc.
#  - The entire example from the RD section at the top is replicated in the RDoc
#    comment for MonitorMixin.  Does the RD section need to remain?
#  - RDoc doesn't recognise aliases, so we have mon_synchronize documented, but
#    not synchronize.
#  - mon_owner is in Nutshell, but appears as an accessor in a separate module
#    here, so is hard/impossible to RDoc.  Some other useful accessors
#    (mon_count and some queue stuff) are also in this module, and don't appear
#    directly in the RDoc output.
#  - in short, it may be worth changing the code layout in this file to make the
#    documentation easier

# Local variables:
# mode: Ruby
# tab-width: 8
# End:
