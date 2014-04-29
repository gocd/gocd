module RSpec
  module Mocks
    # @api private
    # Provides methods for enabling and disabling the available syntaxes
    # provided by rspec-mocks.
    module Syntax
      # @api private
      #
      # Common stubbing logic for both `stub` and `stub!`. This used to
      # live in `stub`, and `stub!` delegated to `stub`, but we discovered
      # that `stub!` was delegating to `RSpec::Mocks::ExampleMethods#stub`
      # (which declares a test double) when called with an implicit receiver,
      # which was a regression in 2.14.0.
      def self.stub_object(object, message_or_hash, opts = {}, &block)
        if ::Hash === message_or_hash
          message_or_hash.each {|message, value| stub_object(object, message).and_return value }
        else
          opts[:expected_from] = caller(1)[1]
          ::RSpec::Mocks.allow_message(object, message_or_hash, opts, &block)
        end
      end

      # @api private
      # Enables the should syntax (`dbl.stub`, `dbl.should_receive`, etc).
      def self.enable_should(syntax_host = default_should_syntax_host)
        return if should_enabled?(syntax_host)

        syntax_host.class_eval do
          def should_receive(message, opts={}, &block)
            opts[:expected_from] ||= caller(1)[0]
            ::RSpec::Mocks.expect_message(self, message.to_sym, opts, &block)
          end

          def should_not_receive(message, &block)
            opts = {:expected_from => caller(1)[0]}
            ::RSpec::Mocks.expect_message(self, message.to_sym, opts, &block).never
          end

          def stub(message_or_hash, opts={}, &block)
            ::RSpec::Mocks::Syntax.stub_object(self, message_or_hash, opts, &block)
          end

          def unstub(message)
            ::RSpec::Mocks.space.proxy_for(self).remove_stub(message)
          end

          def stub!(message_or_hash, opts={}, &block)
            ::RSpec.deprecate "stub!", :replacement => "stub"
            ::RSpec::Mocks::Syntax.stub_object(self, message_or_hash, opts, &block)
          end

          def unstub!(message)
            ::RSpec.deprecate "unstub!", :replacement => "unstub"
            unstub(message)
          end

          def stub_chain(*chain, &blk)
            ::RSpec::Mocks::StubChain.stub_chain_on(self, *chain, &blk)
          end

          def as_null_object
            @_null_object = true
            ::RSpec::Mocks.proxy_for(self).as_null_object
          end

          def null_object?
            defined?(@_null_object)
          end

          def received_message?(message, *args, &block)
            ::RSpec::Mocks.proxy_for(self).received_message?(message, *args, &block)
          end

          unless Class.respond_to? :any_instance
            Class.class_eval do
              def any_instance
                ::RSpec::Mocks.any_instance_recorder_for(self)
              end
            end
          end
        end
      end

      # @api private
      # Disables the should syntax (`dbl.stub`, `dbl.should_receive`, etc).
      def self.disable_should(syntax_host = default_should_syntax_host)
        return unless should_enabled?(syntax_host)

        syntax_host.class_eval do
          undef should_receive
          undef should_not_receive
          undef stub
          undef unstub
          undef stub!
          undef unstub!
          undef stub_chain
          undef as_null_object
          undef null_object?
          undef received_message?
        end

        Class.class_eval do
          undef any_instance
        end
      end

      # @api private
      # Enables the expect syntax (`expect(dbl).to receive`, `allow(dbl).to receive`, etc).
      def self.enable_expect(syntax_host = ::RSpec::Mocks::ExampleMethods)
        return if expect_enabled?(syntax_host)

        syntax_host.class_eval do
          def receive(method_name, &block)
            Matchers::Receive.new(method_name, block)
          end

          def allow(target)
            AllowanceTarget.new(target)
          end

          def expect_any_instance_of(klass)
            AnyInstanceExpectationTarget.new(klass)
          end

          def allow_any_instance_of(klass)
            AnyInstanceAllowanceTarget.new(klass)
          end
        end

        RSpec::Mocks::ExampleMethods::ExpectHost.class_eval do
          def expect(target)
            ExpectationTarget.new(target)
          end
        end
      end

      # @api private
      # Disables the expect syntax (`expect(dbl).to receive`, `allow(dbl).to receive`, etc).
      def self.disable_expect(syntax_host = ::RSpec::Mocks::ExampleMethods)
        return unless expect_enabled?(syntax_host)

        syntax_host.class_eval do
          undef receive
          undef allow
          undef expect_any_instance_of
          undef allow_any_instance_of
        end

        RSpec::Mocks::ExampleMethods::ExpectHost.class_eval do
          undef expect
        end
      end

      # @api private
      # Indicates whether or not the should syntax is enabled.
      def self.should_enabled?(syntax_host = default_should_syntax_host)
        syntax_host.method_defined?(:should_receive)
      end

      # @api private
      # Indicates whether or not the expect syntax is enabled.
      def self.expect_enabled?(syntax_host = ::RSpec::Mocks::ExampleMethods)
        syntax_host.method_defined?(:allow)
      end

      # @api private
      # Determines where the methods like `should_receive`, and `stub` are added.
      def self.default_should_syntax_host
        # JRuby 1.7.4 introduces a regression whereby `defined?(::BasicObject) => nil`
        # yet `BasicObject` still exists and patching onto ::Object breaks things
        # e.g. SimpleDelegator expectations won't work
        #
        # See: https://github.com/jruby/jruby/issues/814
        if defined?(JRUBY_VERSION) && JRUBY_VERSION == '1.7.4' && RUBY_VERSION.to_f > 1.8
          return ::BasicObject
        end

        # On 1.8.7, Object.ancestors.last == Kernel but
        # things blow up if we include `RSpec::Mocks::Methods`
        # into Kernel...not sure why.
        return Object unless defined?(::BasicObject)

        # MacRuby has BasicObject but it's not the root class.
        return Object unless Object.ancestors.last == ::BasicObject

        ::BasicObject
      end

      # @method should_receive
      # Sets an expectation that this object should receive a message before
      # the end of the example.
      #
      # @example
      #
      #     logger = double('logger')
      #     thing_that_logs = ThingThatLogs.new(logger)
      #     logger.should_receive(:log)
      #     thing_that_logs.do_something_that_logs_a_message
      #
      # @note This is only available when you have enabled the `should` syntax.

      # @method should_not_receive
      # Sets and expectation that this object should _not_ receive a message
      # during this example.

      # @method stub
      # Tells the object to respond to the message with the specified value.
      #
      # @example
      #
      #     counter.stub(:count).and_return(37)
      #     counter.stub(:count => 37)
      #     counter.stub(:count) { 37 }
      #
      # @note This is only available when you have enabled the `should` syntax.

      # @method unstub
      # Removes a stub. On a double, the object will no longer respond to
      # `message`. On a real object, the original method (if it exists) is
      # restored.
      #
      # This is rarely used, but can be useful when a stub is set up during a
      # shared `before` hook for the common case, but you want to replace it
      # for a special case.
      #
      # @note This is only available when you have enabled the `should` syntax.

      # @method stub_chain
      # @overload stub_chain(method1, method2)
      # @overload stub_chain("method1.method2")
      # @overload stub_chain(method1, method_to_value_hash)
      #
      # Stubs a chain of methods.
      #
      # ## Warning:
      #
      # Chains can be arbitrarily long, which makes it quite painless to
      # violate the Law of Demeter in violent ways, so you should consider any
      # use of `stub_chain` a code smell. Even though not all code smells
      # indicate real problems (think fluent interfaces), `stub_chain` still
      # results in brittle examples.  For example, if you write
      # `foo.stub_chain(:bar, :baz => 37)` in a spec and then the
      # implementation calls `foo.baz.bar`, the stub will not work.
      #
      # @example
      #
      #     double.stub_chain("foo.bar") { :baz }
      #     double.stub_chain(:foo, :bar => :baz)
      #     double.stub_chain(:foo, :bar) { :baz }
      #
      #     # Given any of ^^ these three forms ^^:
      #     double.foo.bar # => :baz
      #
      #     # Common use in Rails/ActiveRecord:
      #     Article.stub_chain("recent.published") { [Article.new] }
      #
      # @note This is only available when you have enabled the `should` syntax.

      # @method as_null_object
      # Tells the object to respond to all messages. If specific stub values
      # are declared, they'll work as expected. If not, the receiver is
      # returned.
      #
      # @note This is only available when you have enabled the `should` syntax.

      # @method null_object?
      # Returns true if this object has received `as_null_object`
      #
      # @note This is only available when you have enabled the `should` syntax.

      # @method any_instance
      # Used to set stubs and message expectations on any instance of a given
      # class. Returns a [Recorder](Recorder), which records messages like
      # `stub` and `should_receive` for later playback on instances of the
      # class.
      #
      # @example
      #
      #     Car.any_instance.should_receive(:go)
      #     race = Race.new
      #     race.cars << Car.new
      #     race.go # assuming this delegates to all of its cars
      #             # this example would pass
      #
      #     Account.any_instance.stub(:balance) { Money.new(:USD, 25) }
      #     Account.new.balance # => Money.new(:USD, 25))
      #
      # @return [Recorder]
      #
      # @note This is only available when you have enabled the `should` syntax.

      # @method expect
      # Used to wrap an object in preparation for setting a mock expectation
      # on it.
      #
      # @example
      #
      #   expect(obj).to receive(:foo).with(5).and_return(:return_value)
      #
      # @note This method is usually provided by rspec-expectations, unless
      #   you are using rspec-mocks w/o rspec-expectations, in which case it
      #   is only made available if you enable the `expect` syntax.

      # @method allow
      # Used to wrap an object in preparation for stubbing a method
      # on it.
      #
      # @example
      #
      #   allow(dbl).to receive(:foo).with(5).and_return(:return_value)
      #
      # @note This is only available when you have enabled the `expect` syntax.

      # @method expect_any_instance_of
      # Used to wrap a class in preparation for setting a mock expectation
      # on instances of it.
      #
      # @example
      #
      #   expect_any_instance_of(MyClass).to receive(:foo)
      #
      # @note This is only available when you have enabled the `expect` syntax.

      # @method allow_any_instance_of
      # Used to wrap a class in preparation for stubbing a method
      # on instances of it.
      #
      # @example
      #
      #   allow_any_instance_of(MyClass).to receive(:foo)
      #
      # @note This is only available when you have enabled the `expect` syntax.

      # @method receive
      # Used to specify a message that you expect or allow an object
      # to receive. The object returned by `receive` supports the same
      # fluent interface that `should_receive` and `stub` have always
      # supported, allowing you to constrain the arguments or number of
      # times, and configure how the object should respond to the message.
      #
      # @example
      #
      #   expect(obj).to receive(:hello).with("world").exactly(3).times
      #
      # @note This is only available when you have enabled the `expect` syntax.
    end
  end
end

