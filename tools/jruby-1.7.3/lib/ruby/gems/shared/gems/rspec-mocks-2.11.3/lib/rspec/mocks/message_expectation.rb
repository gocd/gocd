module RSpec
  module Mocks

    class MessageExpectation
      # @private
      attr_accessor :error_generator
      attr_reader :message
      attr_writer :expected_received_count, :expected_from, :argument_list_matcher
      protected :expected_received_count=, :expected_from=, :error_generator, :error_generator=

      # @private
      def initialize(error_generator, expectation_ordering, expected_from, message, expected_received_count=1, opts={}, &implementation)
        @error_generator = error_generator
        @error_generator.opts = opts
        @expected_from = expected_from
        @message = message
        @actual_received_count = 0
        @expected_received_count = expected_received_count
        @argument_list_matcher = ArgumentListMatcher.new(ArgumentMatchers::AnyArgsMatcher.new)
        @consecutive = false
        @exception_to_raise = nil
        @args_to_throw = []
        @order_group = expectation_ordering
        @at_least = @at_most = @exactly = nil
        @args_to_yield = []
        @failed_fast = nil
        @args_to_yield_were_cloned = false
        @eval_context = nil
        @implementation = implementation
      end

      def implementation=(implementation)
        @consecutive = false
        @implementation = implementation
      end
      protected :implementation=

      # @private
      def build_child(expected_from, expected_received_count, opts={}, &implementation)
        child = clone
        child.expected_from = expected_from
        child.implementation = implementation if implementation
        child.expected_received_count = expected_received_count
        child.clear_actual_received_count!
        new_gen = error_generator.clone
        new_gen.opts = opts
        child.error_generator = new_gen
        child.clone_args_to_yield(*@args_to_yield)
        child.argument_list_matcher = ArgumentListMatcher.new(ArgumentMatchers::AnyArgsMatcher.new)
        child
      end

      # @private
      def expected_args
        @argument_list_matcher.expected_args
      end

      # @overload and_return(value)
      # @overload and_return(first_value, second_value)
      # @overload and_return(&block)
      #
      # Tells the object to return a value when it receives the message.  Given
      # more than one value, the first value is returned the first time the
      # message is received, the second value is returned the next time, etc,
      # etc.
      #
      # If the message is received more times than there are values, the last
      # value is received for every subsequent call.
      #
      # The block format is still supported, but is unofficially deprecated in
      # favor of just passing a block to the stub method.
      #
      # @example
      #
      #   counter.stub(:count).and_return(1)
      #   counter.count # => 1
      #   counter.count # => 1
      #
      #   counter.stub(:count).and_return(1,2,3)
      #   counter.count # => 1
      #   counter.count # => 2
      #   counter.count # => 3
      #   counter.count # => 3
      #   counter.count # => 3
      #   # etc
      #
      #   # Supported, but ...
      #   counter.stub(:count).and_return { 1 }
      #   counter.count # => 1
      #
      #   # ... this is prefered
      #   counter.stub(:count) { 1 }
      #   counter.count # => 1
      def and_return(*values, &implementation)
        @expected_received_count = [@expected_received_count, values.size].max unless ignoring_args? || (@expected_received_count == 0 and @at_least)
        @consecutive = true if values.size > 1
        @implementation = implementation || build_implementation(values)
      end

      # @overload and_raise
      # @overload and_raise(ExceptionClass)
      # @overload and_raise(exception_instance)
      #
      # Tells the object to raise an exception when the message is received.
      #
      # @note
      #
      #   When you pass an exception class, the MessageExpectation will raise
      #   an instance of it, creating it with `new`. If the exception class
      #   initializer requires any parameters, you must pass in an instance and
      #   not the class.
      #
      # @example
      #
      #   car.stub(:go).and_raise
      #   car.stub(:go).and_raise(OutOfGas)
      #   car.stub(:go).and_raise(OutOfGas.new(2, :oz))
      def and_raise(exception=RuntimeError)
        @exception_to_raise = exception
      end

      # @overload and_throw(symbol)
      # @overload and_throw(symbol, object)
      #
      # Tells the object to throw a symbol (with the object if that form is
      # used) when the message is received.
      #
      # @example
      #
      #   car.stub(:go).and_throw(:out_of_gas)
      #   car.stub(:go).and_throw(:out_of_gas, :level => 0.1)
      def and_throw(symbol, object = nil)
        @args_to_throw = [symbol, object].compact
      end

      # Tells the object to yield one or more args to a block when the message
      # is received.
      #
      # @example
      #
      #   stream.stub(:open).and_yield(StringIO.new)
      def and_yield(*args, &block)
        if @args_to_yield_were_cloned
          @args_to_yield.clear
          @args_to_yield_were_cloned = false
        end

        yield @eval_context = Object.new.extend(RSpec::Mocks::InstanceExec) if block

        @args_to_yield << args
        self
      end

      # @private
      def matches?(message, *args)
        @message == message && @argument_list_matcher.args_match?(*args)
      end

      # @private
      def invoke(*args, &block)
        if (@expected_received_count == 0 && !@at_least) || ((@exactly || @at_most) && (@actual_received_count == @expected_received_count))
          @actual_received_count += 1
          @failed_fast = true
          @error_generator.raise_expectation_error(@message, @expected_received_count, @actual_received_count, *args)
        end

        @order_group.handle_order_constraint self

        begin
          raise_exception unless @exception_to_raise.nil?
          Kernel::throw(*@args_to_throw) unless @args_to_throw.empty?

          default_return_val = call_with_yield(&block) if !@args_to_yield.empty? || @eval_context

          if @consecutive
            call_implementation_consecutive(*args, &block)
          elsif @implementation
            call_implementation(*args, &block)
          else
            default_return_val
          end
        ensure
          @actual_received_count += 1
        end
      end

      # @private
      def raise_exception
        if !@exception_to_raise.respond_to?(:instance_method) ||
            @exception_to_raise.instance_method(:initialize).arity <= 0
          raise(@exception_to_raise)
        else
          raise ArgumentError.new(<<-MESSAGE)
'and_raise' can only accept an Exception class if an instance can be constructed with no arguments.
#{@exception_to_raise.to_s}'s initialize method requires #{@exception_to_raise.instance_method(:initialize).arity} arguments, so you have to supply an instance instead.
MESSAGE
        end
      end

      # @private
      def called_max_times?
        @expected_received_count != :any &&
          !@at_least &&
          @expected_received_count > 0 &&
          @actual_received_count >= @expected_received_count
      end

      # @private
      def matches_name_but_not_args(message, *args)
        @message == message and not @argument_list_matcher.args_match?(*args)
      end

      # @private
      def verify_messages_received
        generate_error unless expected_messages_received? || failed_fast?
      rescue RSpec::Mocks::MockExpectationError => error
        error.backtrace.insert(0, @expected_from)
        Kernel::raise error
      end

      # @private
      def expected_messages_received?
        ignoring_args? || matches_exact_count? || matches_at_least_count? || matches_at_most_count?
      end

      # @private
      def ignoring_args?
        @expected_received_count == :any
      end

      # @private
      def matches_at_least_count?
        @at_least && @actual_received_count >= @expected_received_count
      end

      # @private
      def matches_at_most_count?
        @at_most && @actual_received_count <= @expected_received_count
      end

      # @private
      def matches_exact_count?
        @expected_received_count == @actual_received_count
      end

      # @private
      def similar_messages
        @similar_messages ||= []
      end

      # @private
      def advise(*args)
        similar_messages << args
      end

      # @private
      def generate_error
        if similar_messages.empty?
          @error_generator.raise_expectation_error(@message, @expected_received_count, @actual_received_count, *expected_args)
        else
          @error_generator.raise_similar_message_args_error(self, *@similar_messages)
        end
      end

      def raise_out_of_order_error
        @error_generator.raise_out_of_order_error @message
      end

      # Constrains a stub or message expectation to invocations with specific
      # arguments.
      #
      # With a stub, if the message might be received with other args as well,
      # you should stub a default value first, and then stub or mock the same
      # message using `with` to constrain to specific arguments.
      #
      # A message expectation will fail if the message is received with different
      # arguments.
      #
      # @example
      #
      #   cart.stub(:add) { :failure }
      #   cart.stub(:add).with(Book.new(:isbn => 1934356379)) { :success }
      #   cart.add(Book.new(:isbn => 1234567890))
      #   # => :failure
      #   cart.add(Book.new(:isbn => 1934356379))
      #   # => :success
      #
      #   cart.should_receive(:add).with(Book.new(:isbn => 1934356379)) { :success }
      #   cart.add(Book.new(:isbn => 1234567890))
      #   # => failed expectation
      #   cart.add(Book.new(:isbn => 1934356379))
      #   # => passes
      def with(*args, &block)
        @implementation = block if block_given? unless args.empty?
        @argument_list_matcher = ArgumentListMatcher.new(*args, &block)
        self
      end

      # Constrain a message expectation to be received a specific number of
      # times.
      #
      # @example
      #
      #   dealer.should_recieve(:deal_card).exactly(10).times
      def exactly(n, &block)
        @implementation = block if block
        set_expected_received_count :exactly, n
        self
      end

      # Constrain a message expectation to be received at least a specific
      # number of times.
      #
      # @example
      #
      #   dealer.should_recieve(:deal_card).at_least(9).times
      def at_least(n, &block)
        @implementation = block if block
        set_expected_received_count :at_least, n
        self
      end

      # Constrain a message expectation to be received at most a specific
      # number of times.
      #
      # @example
      #
      #   dealer.should_recieve(:deal_card).at_most(10).times
      def at_most(n, &block)
        @implementation = block if block
        set_expected_received_count :at_most, n
        self
      end

      # Syntactic sugar for `exactly`, `at_least` and `at_most`
      #
      # @example
      #
      #   dealer.should_recieve(:deal_card).exactly(10).times
      #   dealer.should_recieve(:deal_card).at_least(10).times
      #   dealer.should_recieve(:deal_card).at_most(10).times
      def times(&block)
        @implementation = block if block
        self
      end


      # Allows an expected message to be received any number of times.
      def any_number_of_times(&block)
        @implementation = block if block
        @expected_received_count = :any
        self
      end

      # Expect a message not to be received at all.
      #
      # @example
      #
      #   car.should_receive(:stop).never
      def never
        @expected_received_count = 0
        self
      end

      # Expect a message to be received exactly one time.
      #
      # @example
      #
      #   car.should_receive(:go).once
      def once(&block)
        @implementation = block if block
        set_expected_received_count :exactly, 1
        self
      end

      # Expect a message to be received exactly two times.
      #
      # @example
      #
      #   car.should_receive(:go).twice
      def twice(&block)
        @implementation = block if block
        set_expected_received_count :exactly, 2
        self
      end

      # Expect messages to be received in a specific order.
      #
      # @example
      #
      #   api.should_receive(:prepare).ordered
      #   api.should_receive(:run).ordered
      #   api.should_receive(:finish).ordered
      def ordered(&block)
        @implementation = block if block
        @order_group.register(self)
        @ordered = true
        self
      end

      # @private
      def negative_expectation_for?(message)
        return false
      end

      # @private
      def actual_received_count_matters?
        @at_least || @at_most || @exactly
      end

      # @private
      def increase_actual_received_count!
        @actual_received_count += 1
      end

      protected

      def call_with_yield(&block)
        @error_generator.raise_missing_block_error @args_to_yield unless block
        value = nil
        @args_to_yield.each do |args|
          if block.arity > -1 && args.length != block.arity
            @error_generator.raise_wrong_arity_error args, block.arity
          end
          value = @eval_context ? @eval_context.instance_exec(*args, &block) : block.call(*args)
        end
        value
      end

      def call_implementation_consecutive(*args, &block)
        @value ||= call_implementation(*args, &block)
        @value[[@actual_received_count, @value.size-1].min]
      end

      def call_implementation(*args, &block)
        @implementation.arity == 0 ? @implementation.call(&block) : @implementation.call(*args, &block)
      end

      def clone_args_to_yield(*args)
        @args_to_yield = args.clone
        @args_to_yield_were_cloned = true
      end

      def failed_fast?
        @failed_fast
      end

      def set_expected_received_count(relativity, n)
        @at_least = (relativity == :at_least)
        @at_most  = (relativity == :at_most)
        @exactly  = (relativity == :exactly)
        @expected_received_count = case n
                                   when Numeric then n
                                   when :once   then 1
                                   when :twice  then 2
                                   end
      end

      def clear_actual_received_count!
        @actual_received_count = 0
      end

      private

      def build_implementation(values)
        value = values.size == 1 ? values.first : values
        lambda { value }
      end
    end

    # @private
    class NegativeMessageExpectation < MessageExpectation
      # @private
      def initialize(error_generator, expectation_ordering, expected_from, message, &implementation)
        super(error_generator, expectation_ordering, expected_from, message, 0, {}, &implementation)
      end

      def and_return(*)
        # no-op
      end

      # @private
      def negative_expectation_for?(message)
        return @message == message
      end
    end
  end
end
