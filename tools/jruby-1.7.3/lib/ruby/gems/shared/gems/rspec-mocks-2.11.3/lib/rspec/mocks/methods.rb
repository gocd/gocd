module RSpec
  module Mocks
    # Methods that are added to every object.
    module Methods
      # Sets and expectation that this object should receive a message before
      # the end of the example.
      #
      # @example
      #
      #     logger = double('logger')
      #     thing_that_logs = ThingThatLogs.new(logger)
      #     logger.should_receive(:log)
      #     thing_that_logs.do_something_that_logs_a_message
      def should_receive(message, opts={}, &block)
        __mock_proxy.add_message_expectation(opts[:expected_from] || caller(1)[0], message.to_sym, opts, &block)
      end

      # Sets and expectation that this object should _not_ receive a message
      # during this example.
      def should_not_receive(message, &block)
        __mock_proxy.add_negative_message_expectation(caller(1)[0], message.to_sym, &block)
      end

      # Tells the object to respond to the message with the specified value.
      #
      # @example
      #
      #     counter.stub(:count).and_return(37)
      #     counter.stub(:count => 37)
      #     counter.stub(:count) { 37 }
      def stub(message_or_hash, opts={}, &block)
        if Hash === message_or_hash
          message_or_hash.each {|message, value| stub(message).and_return value }
        else
          __mock_proxy.add_stub(caller(1)[0], message_or_hash.to_sym, opts, &block)
        end
      end

      # Removes a stub. On a double, the object will no longer respond to
      # `message`. On a real object, the original method (if it exists) is
      # restored.
      #
      # This is rarely used, but can be useful when a stub is set up during a
      # shared `before` hook for the common case, but you want to replace it
      # for a special case.
      def unstub(message)
        __mock_proxy.remove_stub(message)
      end

      alias_method :stub!, :stub
      alias_method :unstub!, :unstub

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
      def stub_chain(*chain, &blk)
        chain, blk = format_chain(*chain, &blk)
        if chain.length > 1
          if matching_stub = __mock_proxy.__send__(:find_matching_method_stub, chain[0].to_sym)
            chain.shift
            matching_stub.invoke.stub_chain(*chain, &blk)
          else
            next_in_chain = Mock.new
            stub(chain.shift) { next_in_chain }
            next_in_chain.stub_chain(*chain, &blk)
          end
        else
          stub(chain.shift, &blk)
        end
      end

      # Tells the object to respond to all messages. If specific stub values
      # are declared, they'll work as expected. If not, the receiver is
      # returned.
      def as_null_object
        @_null_object = true
        __mock_proxy.as_null_object
      end

      # Returns true if this object has received `as_null_object`
      def null_object?
        defined?(@_null_object)
      end

      # @private
      def received_message?(message, *args, &block)
        __mock_proxy.received_message?(message, *args, &block)
      end

      # @private
      def rspec_verify
        __mock_proxy.verify
      end

      # @private
      def rspec_reset
        __mock_proxy.reset
      end

    private

      def __mock_proxy
        @mock_proxy ||= begin
          mp = if TestDouble === self
            Proxy.new(self, @name, @options)
          else
            Proxy.new(self)
          end

          Serialization.fix_for(self)
          mp
        end
      end
      
      def __remove_mock_proxy
        @mock_proxy = nil
      end

      def format_chain(*chain, &blk)
        if Hash === chain.last
          hash = chain.pop
          hash.each do |k,v|
            chain << k
            blk = lambda { v }
          end
        end
        return chain.join('.').split('.'), blk
      end
    end
  end
end
