unless defined?(JRUBY_VERSION)
  require 'simplecov'
  require 'coveralls'

  SimpleCov.formatter = SimpleCov::Formatter::MultiFormatter[
    SimpleCov::Formatter::HTMLFormatter,
    Coveralls::SimpleCov::Formatter
  ]

  SimpleCov.start do
    project_name 'thread_safe'

    add_filter '/examples/'
    add_filter '/pkg/'
    add_filter '/test/'
    add_filter '/tasks/'
    add_filter '/yard-template/'
    add_filter '/yardoc/'

    command_name 'Mintest'
  end
end

require 'minitest/autorun'

require 'minitest/reporters'
Minitest::Reporters.use! Minitest::Reporters::SpecReporter.new(color: true)

require 'thread'
require 'thread_safe'

THREADS = (RUBY_ENGINE == 'ruby' ? 100 : 10)

if defined?(JRUBY_VERSION) && ENV['TEST_NO_UNSAFE']
  # to be used like this: rake test TEST_NO_UNSAFE=true
  load 'test/package.jar'
  java_import 'thread_safe.SecurityManager'
  manager = SecurityManager.new

  # Prevent accessing internal classes
  manager.deny java.lang.RuntimePermission.new("accessClassInPackage.sun.misc")
  java.lang.System.setSecurityManager manager

  class TestNoUnsafe < Minitest::Test
    def test_security_manager_is_used
      begin
        java_import 'sun.misc.Unsafe'
        flunk
      rescue SecurityError
      end
    end

    def test_no_unsafe_version_of_chmv8_is_used
      require 'thread_safe/jruby_cache_backend' # make sure the jar has been loaded
      assert !Java::OrgJrubyExtThread_safe::JRubyCacheBackendLibrary::JRubyCacheBackend::CAN_USE_UNSAFE_CHM
    end
  end
end

module ThreadSafe
  module Test
    class Latch
      def initialize(count = 1)
        @count = count
        @mutex = Mutex.new
        @cond  = ConditionVariable.new
      end

      def release
        @mutex.synchronize do
          @count -= 1 if @count > 0
          @cond.broadcast if @count.zero?
        end
      end

      def await
        @mutex.synchronize do
          @cond.wait @mutex if @count > 0
        end
      end
    end

    class Barrier < Latch
      def await
        @mutex.synchronize do
          if @count.zero? # fall through
          elsif @count > 0
            @count -= 1
            @count.zero? ? @cond.broadcast : @cond.wait(@mutex)
          end
        end
      end
    end

    class HashCollisionKey
      attr_reader :hash, :key
      def initialize(key, hash = key.hash % 3)
        @key  = key
        @hash = hash
      end

      def eql?(other)
        other.kind_of?(self.class) && @key.eql?(other.key)
      end

      def even?
        @key.even?
      end

      def <=>(other)
        @key <=> other.key
      end
    end

    # having 4 separate HCK classes helps for a more thorough CHMV8 testing
    class HashCollisionKey2 < HashCollisionKey; end
    class HashCollisionKeyNoCompare < HashCollisionKey
      def <=>(other)
        0
      end
    end
    class HashCollisionKey4 < HashCollisionKeyNoCompare; end

    HASH_COLLISION_CLASSES = [HashCollisionKey, HashCollisionKey2, HashCollisionKeyNoCompare, HashCollisionKey4]

    def self.HashCollisionKey(key, hash = key.hash % 3)
      HASH_COLLISION_CLASSES[rand(4)].new(key, hash)
    end

    class HashCollisionKeyNonComparable < HashCollisionKey
      undef <=>
    end
  end
end
