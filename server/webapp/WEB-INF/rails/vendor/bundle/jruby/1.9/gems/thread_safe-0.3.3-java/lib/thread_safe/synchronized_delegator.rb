require 'delegate'
require 'monitor'

# This class provides a trivial way to synchronize all calls to a given object
# by wrapping it with a `Delegator` that performs `Monitor#enter/exit` calls
# around the delegated `#send`. Example:
#
#   array = [] # not thread-safe on many impls
#   array = SynchronizedDelegator.new([]) # thread-safe
#
# A simple `Monitor` provides a very coarse-grained way to synchronize a given
# object, in that it will cause synchronization for methods that have no
# need for it, but this is a trivial way to get thread-safety where none may
# exist currently on some implementations.
#
# This class is currently being considered for inclusion into stdlib, via
# https://bugs.ruby-lang.org/issues/8556
class SynchronizedDelegator < SimpleDelegator
  def setup
    @old_abort = Thread.abort_on_exception
    Thread.abort_on_exception = true
  end

  def teardown
    Thread.abort_on_exception = @old_abort
  end

  def initialize(obj)
    __setobj__(obj)
    @monitor = Monitor.new
  end

  def method_missing(method, *args, &block)
    monitor = @monitor
    begin
      monitor.enter
      super
    ensure
      monitor.exit
    end
  end

  # Work-around for 1.8 std-lib not passing block around to delegate.
  # @private
  def method_missing(method, *args, &block)
    monitor = @monitor
    begin
      monitor.enter
      target = self.__getobj__
      if target.respond_to?(method)
        target.__send__(method, *args, &block)
      else
        super(method, *args, &block)
      end
    ensure
      monitor.exit
    end
  end if RUBY_VERSION[0, 3] == '1.8'

end unless defined?(SynchronizedDelegator)
