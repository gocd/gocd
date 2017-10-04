require 'rspec/mocks/framework'
require 'rspec/mocks/version'

module RSpec
  module Mocks
    class << self
      attr_accessor :space

      def setup(host=nil)
        host_is_from_rspec_core = defined?(::RSpec::Core::ExampleGroup) && host.is_a?(::RSpec::Core::ExampleGroup)
        if host
          unless host_is_from_rspec_core
            RSpec.deprecate(
              "The host argument to `RSpec::Mocks.setup`",
              :replacement => "`include RSpec::Mocks::ExampleMethods` in #{host.inspect}"
            )
          end

          (class << host; self; end).class_eval do
            include RSpec::Mocks::ExampleMethods
          end
        end
        space.outside_example = false
      end

      def verify
        space.verify_all
      end

      def teardown
        space.reset_all
        space.outside_example = true
      end

      def proxy_for(object)
        space.proxy_for(object)
      end

      def proxies_of(klass)
        space.proxies_of(klass)
      end

      def any_instance_recorder_for(klass)
        space.any_instance_recorder_for(klass)
      end

      # Adds an allowance (stub) on `subject`
      #
      # @param subject the subject to which the message will be added
      # @param message a symbol, representing the message that will be
      #                added.
      # @param opts a hash of options, :expected_from is used to set the
      #             original call site
      # @param block an optional implementation for the allowance
      #
      # @example Defines the implementation of `foo` on `bar`, using the passed block
      #   x = 0
      #   RSpec::Mocks.allow_message(bar, :foo) { x += 1 }
      def allow_message(subject, message, opts={}, &block)
        orig_caller = opts.fetch(:expected_from) {
          CallerFilter.first_non_rspec_line
        }
        ::RSpec::Mocks.proxy_for(subject).
          add_stub(orig_caller, message.to_sym, opts, &block)
      end

      # Sets a message expectation on `subject`.
      # @param subject the subject on which the message will be expected
      # @param message a symbol, representing the message that will be
      #                expected.
      # @param opts a hash of options, :expected_from is used to set the
      #             original call site
      # @param block an optional implementation for the expectation
      #
      # @example Expect the message `foo` to receive `bar`, then call it
      #   RSpec::Mocks.expect_message(bar, :foo)
      #   bar.foo
      def expect_message(subject, message, opts={}, &block)
        orig_caller = opts.fetch(:expected_from) {
          CallerFilter.first_non_rspec_line
        }
        ::RSpec::Mocks.proxy_for(subject).
          add_message_expectation(orig_caller, message.to_sym, opts, &block)
      end

      # @api private
      KERNEL_METHOD_METHOD = ::Kernel.instance_method(:method)

      # @api private
      # Used internally to get a method handle for a particular object
      # and method name.
      #
      # Includes handling for a few special cases:
      #
      #   - Objects that redefine #method (e.g. an HTTPRequest struct)
      #   - BasicObject subclasses that mixin a Kernel dup (e.g. SimpleDelegator)
      def method_handle_for(object, method_name)
        if ::Kernel === object
          KERNEL_METHOD_METHOD.bind(object).call(method_name)
        else
          object.method(method_name)
        end
      end
    end

    # @private
    IGNORED_BACKTRACE_LINE = 'this backtrace line is ignored'

    self.space = RSpec::Mocks::Space.new

    DEPRECATED_CONSTANTS =
      {
        :Mock            => Double,
        :ConstantStubber => ConstantMutator,
      }

    def self.const_missing(name)
      if const = DEPRECATED_CONSTANTS[name]
        RSpec.deprecate("RSpec::Mocks::#{name}", :replacement => const.name)
        const
      else
        super
      end
    end
  end
end
