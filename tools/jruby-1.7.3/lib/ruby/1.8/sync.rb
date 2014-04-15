#
#   sync.rb - 2 phase lock with counter
#   	$Release Version: 1.0$
#   	$Revision$
#   	$Date$
#   	by Keiju ISHITSUKA(keiju@ishitsuka.com)
#
# --
#  Sync_m, Synchronizer_m
#  Usage:
#   obj.extend(Sync_m)
#   or
#   class Foo
#	include Sync_m
#	:
#   end
#
#   Sync_m#sync_mode
#   Sync_m#sync_locked?, locked?
#   Sync_m#sync_shared?, shared?
#   Sync_m#sync_exclusive?, sync_exclusive?
#   Sync_m#sync_try_lock, try_lock
#   Sync_m#sync_lock, lock
#   Sync_m#sync_unlock, unlock
#
#   Sync, Synchronicer:
#	include Sync_m
#   Usage:
#   sync = Sync.new
#
#   Sync#mode
#   Sync#locked?
#   Sync#shared?
#   Sync#exclusive?
#   Sync#try_lock(mode) -- mode = :EX, :SH, :UN
#   Sync#lock(mode)     -- mode = :EX, :SH, :UN
#   Sync#unlock
#   Sync#synchronize(mode) {...}
#   
#

unless defined? Thread
  fail "Thread not available for this ruby interpreter"
end

require 'thread'

module Sync_m
  RCS_ID='-$Header$-'
  
  # lock mode
  UN = :UN
  SH = :SH
  EX = :EX
  
  # exceptions
  class Err < StandardError
    def Err.Fail(*opt)
      Thread.critical = false
      fail self, sprintf(self::Message, *opt)
    end
    
    class UnknownLocker < Err
      Message = "Thread(%s) not locked."
      def UnknownLocker.Fail(th)
	super(th.inspect)
      end
    end
    
    class LockModeFailer < Err
      Message = "Unknown lock mode(%s)"
      def LockModeFailer.Fail(mode)
	if mode.id2name
	  mode = id2name
	end
	super(mode)
      end
    end
  end
  
  def Sync_m.define_aliases(cl)
    cl.module_eval %q{
      alias locked? sync_locked?
      alias shared? sync_shared?
      alias exclusive? sync_exclusive?
      alias lock sync_lock
      alias unlock sync_unlock
      alias try_lock sync_try_lock
      alias synchronize sync_synchronize
    }
  end
  
  def Sync_m.append_features(cl)
    super
    unless cl.instance_of?(Module)
      # do nothing for Modules
      # make aliases and include the proper module.
      define_aliases(cl)
    end
  end
  
  def Sync_m.extend_object(obj)
    super
    obj.sync_extended
  end

  def sync_extended
    unless (defined? locked? and
	    defined? shared? and
	    defined? exclusive? and
	    defined? lock and
	    defined? unlock and
	    defined? try_lock and
	    defined? synchronize)
      Sync_m.define_aliases(class<<self;self;end)
    end
    sync_initialize
  end

  # accessing
  def sync_locked?
    @sync_mutex.synchronize { @sync_mode != UN }
  end
  
  def sync_shared?
    @sync_mutex.synchronize { @sync_mode == SH }
  end
  
  def sync_exclusive?
    @sync_mutex.synchronize { @sync_mode == EX }
  end
  
  # locking methods.
  def sync_try_lock(m = EX)
    return sync_unlock if m == UN
    @sync_mutex.synchronize do
      sync_try_lock_sub(m)
    end
  end
  
  def sync_lock(m = EX)
    return sync_unlock if m == UN
    @sync_mutex.synchronize do
      until sync_try_lock_sub(m)
        if @sync_sh_lockers.include? Thread.current
          @sync_upgrade_n_waiting += 1
          begin
            @sync_upgrade_cond.wait(@sync_mutex)
          ensure
            @sync_upgrade_n_waiting -= 1
          end
        else
          @sync_cond.wait(@sync_mutex)
        end
      end
    end
    self
  end
  
  def sync_unlock(m = EX)
    @sync_mutex.synchronize do
      case @sync_mode
      when UN
        Err::UnknownLocker.Fail(Thread.current)
      when SH
        # downgrade EX unlock requests in SH mode
        m = SH if m == EX
      when EX
        # upgrade SH unlock requests in EX mode
        # (necessary to balance lock request upgrades)
        m = EX if m == SH
      end
    
      runnable = false
      case m
      when UN
        Err::UnknownLocker.Fail(Thread.current)
      
      when EX
        if @sync_ex_locker == Thread.current
          @sync_ex_count -= 1
          if @sync_ex_count.zero?
            @sync_ex_locker = nil
            if @sync_sh_lockers.include? Thread.current
              @sync_mode = SH
            else
              @sync_mode = UN
            end
            runnable = true
          end
        else
          Err::UnknownLocker.Fail(Thread.current)
        end
      
      when SH
        count = @sync_sh_lockers[Thread.current]
        Err::UnknownLocker.Fail(Thread.current) if count.nil?
        count -= 1
        if count.zero?
          @sync_sh_lockers.delete Thread.current
          if @sync_sh_lockers.empty? and @sync_mode == SH
            @sync_mode = UN
            runnable = true
          end
        else
          @sync_sh_lockers[Thread.current] = count
        end
      end

      if runnable
        if @sync_upgrade_n_waiting.nonzero?
          @sync_upgrade_cond.signal
        else
          @sync_cond.signal
        end
      end
    end
    self
  end
  
  def sync_synchronize(mode = EX)
    begin
      sync_lock(mode)
      yield
    ensure
      sync_unlock
    end
  end

  def sync_mode
    @sync_mutex.synchronize { @sync_mode }
  end

  private

  def sync_initialize
    @sync_mutex = Mutex.new
    @sync_mode = UN
    @sync_cond = ConditionVariable.new
    @sync_upgrade_cond = ConditionVariable.new
    @sync_upgrade_n_waiting = 0
    @sync_sh_lockers = Hash.new
    @sync_ex_locker = nil
    @sync_ex_count = 0
  end

  def initialize(*args)
    sync_initialize
    super
  end
    
  def sync_try_lock_sub(m)
    case m
    when SH
      case @sync_mode
      when UN, SH
        @sync_mode = SH
        count = @sync_sh_lockers[Thread.current] || 0
        @sync_sh_lockers[Thread.current] = count + 1
        true
      when EX
        # in EX mode, lock will upgrade to EX lock
        if @sync_ex_locker == Thread.current
          @sync_ex_count += 1
          true
        else
          false
        end
      end
    when EX
      if @sync_mode == UN or
         @sync_mode == SH && @sync_sh_lockers.size == 1 &&
                             @sync_sh_lockers.include?(Thread.current) or
         @sync_mode == EX && @sync_ex_locker == Thread.current

        @sync_mode = EX
        @sync_ex_locker = Thread.current
        @sync_ex_count += 1
        true
      else
        false
      end
    else
      Err::LockModeFailer.Fail mode
    end
  end
end
Synchronizer_m = Sync_m

class Sync
  #Sync_m.extend_class self
  include Sync_m
    
  def initialize
    super
  end
    
end
Synchronizer = Sync
