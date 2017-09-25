module RSpec
  module Mocks
    module ExampleMethods
      include RSpec::Mocks::ArgumentMatchers

      # @overload double()
      # @overload double(name)
      # @overload double(stubs)
      # @overload double(name, stubs)
      # @param name [String/Symbol] (optional) used in
      #   clarify intent
      # @param stubs (Hash) (optional) hash of method/return-value pairs
      # @return (Mock)
      #
      # Constructs an instance of [RSpec::Mocks::Mock](RSpec::Mocks::Mock) configured
      # with an optional name, used for reporting in failure messages, and an optional
      # hash of method/return-value pairs.
      #
      # @example
      #
      #   book = double("book", :title => "The RSpec Book")
      #   book.title #=> "The RSpec Book"
      #
      #   card = double("card", :suit => "Spades", :rank => "A")
      #   card.suit  #=> "Spades"
      #   card.rank  #=> "A"
      #
      # @see #mock
      # @see #stub
      def double(*args)
        declare_double('Double', *args)
      end

      # Deprecated: Use [double](#double-instance_method).
      def mock(*args)
        RSpec.deprecate "mock", :replacement => "double"
        declare_double('Mock', *args)
      end

      # Deprecated: Use [double](#double-instance_method).
      def stub(*args)
        RSpec.deprecate "stub", :replacement => "double"
        declare_double('Stub', *args)
      end

      # Disables warning messages about expectations being set on nil.
      #
      # By default warning messages are issued when expectations are set on
      # nil.  This is to prevent false-positives and to catch potential bugs
      # early on.
      def allow_message_expectations_on_nil
        RSpec::Mocks.space.proxy_for(nil).warn_about_expectations = false
      end

      # Stubs the named constant with the given value.
      # Like method stubs, the constant will be restored
      # to its original value (or lack of one, if it was
      # undefined) when the example completes.
      #
      # @param constant_name [String] The fully qualified name of the constant. The current
      #   constant scoping at the point of call is not considered.
      # @param value [Object] The value to make the constant refer to. When the
      #   example completes, the constant will be restored to its prior state.
      # @param options [Hash] Stubbing options.
      # @option options :transfer_nested_constants [Boolean, Array<Symbol>] Determines
      #   what nested constants, if any, will be transferred from the original value
      #   of the constant to the new value of the constant. This only works if both
      #   the original and new values are modules (or classes).
      # @return [Object] the stubbed value of the constant
      #
      # @example
      #
      #   stub_const("MyClass", Class.new) # => Replaces (or defines) MyClass with a new class object.
      #   stub_const("SomeModel::PER_PAGE", 5) # => Sets SomeModel::PER_PAGE to 5.
      #
      #   class CardDeck
      #     SUITS = [:Spades, :Diamonds, :Clubs, :Hearts]
      #     NUM_CARDS = 52
      #   end
      #
      #   stub_const("CardDeck", Class.new)
      #   CardDeck::SUITS # => uninitialized constant error
      #   CardDeck::NUM_CARDS # => uninitialized constant error
      #
      #   stub_const("CardDeck", Class.new, :transfer_nested_constants => true)
      #   CardDeck::SUITS # => our suits array
      #   CardDeck::NUM_CARDS # => 52
      #
      #   stub_const("CardDeck", Class.new, :transfer_nested_constants => [:SUITS])
      #   CardDeck::SUITS # => our suits array
      #   CardDeck::NUM_CARDS # => uninitialized constant error
      def stub_const(constant_name, value, options = {})
        ConstantMutator.stub(constant_name, value, options)
      end

      # Hides the named constant with the given value. The constant will be
      # undefined for the duration of the test.
      #
      # Like method stubs, the constant will be restored to its original value
      # when the example completes.
      #
      # @param constant_name [String] The fully qualified name of the constant.
      #   The current constant scoping at the point of call is not considered.
      #
      # @example
      #
      #   hide_const("MyClass") # => MyClass is now an undefined constant
      def hide_const(constant_name)
        ConstantMutator.hide(constant_name)
      end

      # Verifies that the given object received the expected message during the
      # course of the test. The method must have previously been stubbed in
      # order for messages to be verified.
      #
      # Stubbing and verifying messages received in this way implements the
      # Test Spy pattern.
      #
      # @param method_name [Symbol] name of the method expected to have been
      #   called.
      #
      # @example
      #
      #   invitation = double('invitation', accept: true)
      #   user.accept_invitation(invitation)
      #   expect(invitation).to have_received(:accept)
      #
      #   # You can also use most message expectations:
      #   expect(invitation).to have_received(:accept).with(mailer).once
      def have_received(method_name)
        Matchers::HaveReceived.new(method_name)
      end

      def self.included(klass)
        klass.class_eval do
          # This gets mixed in so that if `RSpec::Matchers` is included in
          # `klass` later, it's definition of `expect` will take precedence.
          include ExpectHost unless method_defined?(:expect)
        end
      end

    private

      def declare_double(declared_as, *args)
        args << {} unless Hash === args.last
        args.last[:__declared_as] = declared_as
        RSpec::Mocks::Mock.new(*args)
      end

      # This module exists to host the `expect` method for cases where
      # rspec-mocks is used w/o rspec-expectations.
      module ExpectHost
      end
    end
  end
end
