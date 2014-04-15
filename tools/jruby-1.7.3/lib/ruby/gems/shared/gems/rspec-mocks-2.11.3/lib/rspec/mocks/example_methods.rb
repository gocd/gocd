module RSpec
  module Mocks
    module ExampleMethods
      include RSpec::Mocks::ArgumentMatchers

      # Creates an instance of RSpec::Mocks::Mock.
      #
      # `name` is used for failure reporting, so you should use the role that
      # the mock is playing in the example.
      #
      # Use `stubs` to declare one or more method stubs in one statement.
      #
      # @example
      #
      #   book = double("book", :title => "The RSpec Book")
      #   book.title #=> "The RSpec Book"
      #
      #   card = double("card", :suit => "Spades", :rank => "A")
      #   card.suit  #=> "Spades"
      #   card.rank  #=> "A"
      def double(*args)
        declare_double('Double', *args)
      end

      # Just like double
      def mock(*args)
        declare_double('Mock', *args)
      end

      # Just like double
      def stub(*args)
        declare_double('Stub', *args)
      end

      # Disables warning messages about expectations being set on nil.
      #
      # By default warning messages are issued when expectations are set on
      # nil.  This is to prevent false-positives and to catch potential bugs
      # early on.
      def allow_message_expectations_on_nil
        Proxy.allow_message_expectations_on_nil
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
        ConstantStubber.stub(constant_name, value, options)
      end

    private
      
      def declare_double(declared_as, *args)
        args << {} unless Hash === args.last
        args.last[:__declared_as] = declared_as
        RSpec::Mocks::Mock.new(*args)
      end

    end
  end
end
