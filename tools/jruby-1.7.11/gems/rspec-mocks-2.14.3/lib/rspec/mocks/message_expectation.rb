module RSpec
  module Mocks

    class MessageExpectation
      # @private
      attr_accessor :error_generator, :implementation
      attr_reader :message
      attr_writer :expected_received_count, :expected_from, :argument_list_matcher
      protected :expected_received_count=, :expected_from=, :error_generator, :error_generator=, :implementation=

      # @private
      def initialize(error_generator, expectation_ordering, expected_from, method_double,
                     expected_received_count=1, opts={}, &implementation_block)
        @error_generator = error_generator
        @error_generator.opts = opts
        @expected_from = expected_from
        @method_double = method_double
        @message = @method_double.method_name
        @actual_received_count = 0
        @expected_received_count = expected_received_count
        @argument_list_matcher = ArgumentListMatcher.new(ArgumentMatchers::AnyArgsMatcher.new)
        @order_group = expectation_ordering
        @at_least = @at_most = @exactly = nil
        @args_to_yield = []
        @failed_fast = nil
        @eval_context = nil

        @implementation = Implementation.new
        self.inner_implementation_action = implementation_block
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
        if negative?
          RSpec.deprecate "`and_return` on a negative message expectation"
        end

        @expected_received_count = [@expected_received_count, values.size].max unless ignoring_args? || (@expected_received_count == 0 and @at_least)

        if implementation
          # TODO: deprecate `and_return { value }`
          self.inner_implementation_action = implementation
        else
          self.terminal_implementation_action = AndReturnImplementation.new(values)
        end

        nil
      end

      # Tells the object to delegate to the original unmodified method
      # when it receives the message.
      #
      # @note This is only available on partial mock objects.
      #
      # @example
      #
      #   counter.should_receive(:increment).and_call_original
      #   original_count = counter.count
      #   counter.increment
      #   expect(counter.count).to eq(original_count + 1)
      def and_call_original
        if @method_double.object.is_a?(RSpec::Mocks::TestDouble)
          @error_generator.raise_only_valid_on_a_partial_mock(:and_call_original)
        else
          @implementation = AndCallOriginalImplementation.new(@method_double.original_method)
        end
      end

      # @overload and_raise
      # @overload and_raise(ExceptionClass)
      # @overload and_raise(ExceptionClass, message)
      # @overload and_raise(exception_instance)
      #
      # Tells the object to raise an exception when the message is received.
      #
      # @note
      #
      #   When you pass an exception class, the MessageExpectation will raise
      #   an instance of it, creating it with `exception` and passing `message`
      #   if specified.  If the exception class initializer requires more than
      #   one parameters, you must pass in an instance and not the class,
      #   otherwise this method will raise an ArgumentError exception.
      #
      # @example
      #
      #   car.stub(:go).and_raise
      #   car.stub(:go).and_raise(OutOfGas)
      #   car.stub(:go).and_raise(OutOfGas, "At least 2 oz of gas needed to drive")
      #   car.stub(:go).and_raise(OutOfGas.new(2, :oz))
      def and_raise(exception = RuntimeError, message = nil)
        if exception.respond_to?(:exception)
          exception = message ? exception.exception(message) : exception.exception
        end

        self.terminal_implementation_action = Proc.new { raise exception }
        nil
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
      def and_throw(*args)
        self.terminal_implementation_action = Proc.new { throw(*args) }
        nil
      end

      # Tells the object to yield one or more args to a block when the message
      # is received.
      #
      # @example
      #
      #   stream.stub(:open).and_yield(StringIO.new)
      def and_yield(*args, &block)
        yield @eval_context = Object.new.extend(RSpec::Mocks::InstanceExec) if block
        @args_to_yield << args
        self.initial_implementation_action = AndYieldImplementation.new(@args_to_yield, @eval_context, @error_generator)
        self
      end

      # @private
      def matches?(message, *args)
        @message == message && @argument_list_matcher.args_match?(*args)
      end

      # @private
      def invoke(parent_stub, *args, &block)
        if negative? || ((@exactly || @at_most) && (@actual_received_count == @expected_received_count))
          @actual_received_count += 1
          @failed_fast = true
          #args are the args we actually received, @argument_list_matcher is the
          #list of args we were expecting
          @error_generator.raise_expectation_error(@message, @expected_received_count, @argument_list_matcher, @actual_received_count, expectation_count_type, *args)
        end

        @order_group.handle_order_constraint self

        begin
          if implementation.present?
            implementation.call(*args, &block)
          elsif parent_stub
            parent_stub.invoke(nil, *args, &block)
          end
        ensure
          @actual_received_count += 1
        end
      end

      # @private
      def negative?
        @expected_received_count == 0 && !@at_least
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
          @error_generator.raise_expectation_error(@message, @expected_received_count, @argument_list_matcher, @actual_received_count, expectation_count_type, *expected_args)
        else
          @error_generator.raise_similar_message_args_error(self, *@similar_messages)
        end
      end

      def expectation_count_type
        return :at_least if @at_least
        return :at_most if @at_most
        return nil
      end

      # @private
      def description
        @error_generator.describe_expectation(@message, @expected_received_count, @actual_received_count, *expected_args)
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
        self.inner_implementation_action = block if block_given? unless args.empty?
        @argument_list_matcher = ArgumentListMatcher.new(*args, &block)
        self
      end

      # Constrain a message expectation to be received a specific number of
      # times.
      #
      # @example
      #
      #   dealer.should_receive(:deal_card).exactly(10).times
      def exactly(n, &block)
        self.inner_implementation_action = block
        set_expected_received_count :exactly, n
        self
      end

      # Constrain a message expectation to be received at least a specific
      # number of times.
      #
      # @example
      #
      #   dealer.should_receive(:deal_card).at_least(9).times
      def at_least(n, &block)
        if n == 0
          RSpec.deprecate "at_least(0) with should_receive", :replacement => "stub"
        end

        self.inner_implementation_action = block
        set_expected_received_count :at_least, n
        self
      end

      # Constrain a message expectation to be received at most a specific
      # number of times.
      #
      # @example
      #
      #   dealer.should_receive(:deal_card).at_most(10).times
      def at_most(n, &block)
        self.inner_implementation_action = block
        set_expected_received_count :at_most, n
        self
      end

      # Syntactic sugar for `exactly`, `at_least` and `at_most`
      #
      # @example
      #
      #   dealer.should_receive(:deal_card).exactly(10).times
      #   dealer.should_receive(:deal_card).at_least(10).times
      #   dealer.should_receive(:deal_card).at_most(10).times
      def times(&block)
        self.inner_implementation_action = block
        self
      end


      # Allows an expected message to be received any number of times.
      def any_number_of_times(&block)
        RSpec.deprecate "any_number_of_times", :replacement => "stub"
        self.inner_implementation_action = block
        @expected_received_count = :any
        self
      end

      # Expect a message not to be received at all.
      #
      # @example
      #
      #   car.should_receive(:stop).never
      def never
        ErrorGenerator.raise_double_negation_error("expect(obj)") if negative?
        @expected_received_count = 0
        self
      end

      # Expect a message to be received exactly one time.
      #
      # @example
      #
      #   car.should_receive(:go).once
      def once(&block)
        self.inner_implementation_action = block
        set_expected_received_count :exactly, 1
        self
      end

      # Expect a message to be received exactly two times.
      #
      # @example
      #
      #   car.should_receive(:go).twice
      def twice(&block)
        self.inner_implementation_action = block
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
        self.inner_implementation_action = block
        @order_group.register(self)
        @ordered = true
        self
      end

      # @private
      def negative_expectation_for?(message)
        @message == message && negative?
      end

      # @private
      def actual_received_count_matters?
        @at_least || @at_most || @exactly
      end

      # @private
      def increase_actual_received_count!
        @actual_received_count += 1
      end

    private

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

      def initial_implementation_action=(action)
        implementation.initial_action = action
      end

      def inner_implementation_action=(action)
        implementation.inner_action = action if action
      end

      def terminal_implementation_action=(action)
        implementation.terminal_action = action
      end
    end

    # Handles the implementation of an `and_yield` declaration.
    # @private
    class AndYieldImplementation
      def initialize(args_to_yield, eval_context, error_generator)
        @args_to_yield = args_to_yield
        @eval_context = eval_context
        @error_generator = error_generator
      end

      def arity
        -1
      end

      def call(*args_to_ignore, &block)
        return if @args_to_yield.empty? && @eval_context.nil?

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
    end

    # Handles the implementation of an `and_return` implementation.
    # @private
    class AndReturnImplementation
      def initialize(values_to_return)
        @values_to_return = values_to_return
      end

      def arity
        -1
      end

      def call(*args_to_ignore, &block)
        if @values_to_return.size > 1
          @values_to_return.shift
        else
          @values_to_return.first
        end
      end
    end

    # Represents a configured implementation. Takes into account
    # any number of sub-implementations.
    # @private
    class Implementation
      attr_accessor :initial_action, :inner_action, :terminal_action

      def call(*args, &block)
        actions.map do |action|
          action.call(*arg_slice_for(args, action.arity), &block)
        end.last
      end

      def arg_slice_for(args, arity)
        if arity >= 0
          args.slice(0, arity)
        else
          args
        end
      end

      def present?
        actions.any?
      end

    private

      def actions
        [initial_action, inner_action, terminal_action].compact
      end
    end

    # Represents an `and_call_original` implementation.
    # @private
    class AndCallOriginalImplementation
      def initialize(method)
        @method = method
      end

      CannotModifyFurtherError = Class.new(StandardError)

      def arity
        @method.arity
      end

      def initial_action=(value)
        raise cannot_modify_further_error
      end

      def inner_action=(value)
        raise cannot_modify_further_error
      end

      def terminal_action=(value)
        raise cannot_modify_further_error
      end

      def present?
        true
      end

      def call(*args, &block)
        @method.call(*args, &block)
      end

    private

      def cannot_modify_further_error
        CannotModifyFurtherError.new "This method has already been configured " +
          "to call the original implementation, and cannot be modified further."
      end
    end
  end
end
