#--
#   mutex_m.rb - 
#   	$Release Version: 3.0$
#   	$Revision$
#   	$Date$
#       Original from mutex.rb
#   	by Keiju ISHITSUKA(keiju@ishitsuka.com)
#       modified by matz
#       patched by akira yamada
#       gutted by MenTaLguY
#++
#
# == Usage
#
# Extend an object and use it like a Mutex object:
#
#   require "mutex_m.rb"
#   obj = Object.new
#   obj.extend Mutex_m
#   # ...
#
# Or, include Mutex_m in a class to have its instances behave like a Mutex
# object:
#
#   class Foo
#     include Mutex_m
#     # ...
#   end
#   
#   obj = Foo.new

require 'thread'

module Mutex_m
  def Mutex_m.define_aliases(cl)
    cl.module_eval %q{
      alias locked? mu_locked?
      alias lock mu_lock
      alias unlock mu_unlock
      alias try_lock mu_try_lock
      alias synchronize mu_synchronize
    }
  end  

  def Mutex_m.append_features(cl)
    super
    define_aliases(cl) unless cl.instance_of?(Module)
  end
  
  def Mutex_m.extend_object(obj)
    super
    obj.mu_extended
  end

  def mu_extended
    unless (defined? locked? and
	    defined? lock and
	    defined? unlock and
	    defined? try_lock and
	    defined? synchronize)
      Mutex_m.define_aliases(class<<self;self;end)
    end
    mu_initialize
  end
  
  # locking 
  def mu_synchronize
    @mu_mutex.synchronize { yield }
  end
  
  def mu_locked?
    @mu_mutex.locked?
  end
  
  def mu_try_lock
    @mu_mutex.try_lock
  end
  
  def mu_lock
    @mu_mutex.lock
    self
  end
  
  def mu_unlock
    @mu_mutex.unlock
    self
  end
  
  private
  
  def mu_initialize
    @mu_mutex = ::Mutex.new
  end

  def initialize(*args)
    mu_initialize
    super
  end
end
