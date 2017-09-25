require 'rspec/matchers/extensions/instance_eval_with_args'
require 'rspec/matchers/pretty'

require 'rspec/matchers/built_in'
require 'rspec/matchers/matcher'
require 'rspec/matchers/operator_matcher'
require 'rspec/matchers/be_close'

require 'rspec/matchers/generated_descriptions'
require 'rspec/matchers/method_missing'
require 'rspec/matchers/compatibility'
require 'rspec/matchers/dsl'
require 'rspec/matchers/test_unit_integration'

module RSpec
  # RSpec::Matchers provides a number of useful matchers we use to define
  # expectations. A matcher is any object that responds to the following:
  #
  #     matches?(actual)
  #     failure_message_for_should
  #
  # These methods are also part of the matcher protocol, but are optional:
  #
  #     does_not_match?(actual)
  #     failure_message_for_should_not
  #     description
  #
  # ## Predicates
  #
  # In addition to matchers that are defined explicitly, RSpec will create
  # custom matchers on the fly for any arbitrary predicate, giving your specs a
  # much more natural language feel.
  #
  # A Ruby predicate is a method that ends with a "?" and returns true or false.
  # Common examples are `empty?`, `nil?`, and `instance_of?`.
  #
  # All you need to do is write `expect(..).to be_` followed by the predicate
  # without the question mark, and RSpec will figure it out from there.
  # For example:
  #
  #     expect([]).to be_empty     # => [].empty?() | passes
  #     expect([]).not_to be_empty # => [].empty?() | fails
  #
  # In addtion to prefixing the predicate matchers with "be_", you can also use "be_a_"
  # and "be_an_", making your specs read much more naturally:
  #
  #     expect("a string").to be_an_instance_of(String) # =>"a string".instance_of?(String) # passes
  #
  #     expect(3).to be_a_kind_of(Fixnum)        # => 3.kind_of?(Numeric)     | passes
  #     expect(3).to be_a_kind_of(Numeric)       # => 3.kind_of?(Numeric)     | passes
  #     expect(3).to be_an_instance_of(Fixnum)   # => 3.instance_of?(Fixnum)  | passes
  #     expect(3).not_to be_an_instance_of(Numeric) # => 3.instance_of?(Numeric) | fails
  #
  # RSpec will also create custom matchers for predicates like `has_key?`. To
  # use this feature, just state that the object should have_key(:key) and RSpec will
  # call has_key?(:key) on the target. For example:
  #
  #     expect(:a => "A").to have_key(:a)
  #     expect(:a => "A").to have_key(:b) # fails
  #
  # You can use this feature to invoke any predicate that begins with "has_", whether it is
  # part of the Ruby libraries (like `Hash#has_key?`) or a method you wrote on your own class.
  #
  # ## Custom Matchers
  #
  # When you find that none of the stock matchers provide a natural feeling
  # expectation, you can very easily write your own using RSpec's matcher DSL
  # or writing one from scratch.
  #
  # ### Matcher DSL
  #
  # Imagine that you are writing a game in which players can be in various
  # zones on a virtual board. To specify that bob should be in zone 4, you
  # could say:
  #
  #     expect(bob.current_zone).to eql(Zone.new("4"))
  #
  # But you might find it more expressive to say:
  #
  #     expect(bob).to be_in_zone("4")
  #
  # and/or
  #
  #     expect(bob).not_to be_in_zone("3")
  #
  # You can create such a matcher like so:
  #
  #     RSpec::Matchers.define :be_in_zone do |zone|
  #       match do |player|
  #         player.in_zone?(zone)
  #       end
  #     end
  #
  # This will generate a <tt>be_in_zone</tt> method that returns a matcher
  # with logical default messages for failures. You can override the failure
  # messages and the generated description as follows:
  #
  #     RSpec::Matchers.define :be_in_zone do |zone|
  #       match do |player|
  #         player.in_zone?(zone)
  #       end
  #
  #       failure_message_for_should do |player|
  #         # generate and return the appropriate string.
  #       end
  #
  #       failure_message_for_should_not do |player|
  #         # generate and return the appropriate string.
  #       end
  #
  #       description do
  #         # generate and return the appropriate string.
  #       end
  #     end
  #
  # Each of the message-generation methods has access to the block arguments
  # passed to the <tt>create</tt> method (in this case, <tt>zone</tt>). The
  # failure message methods (<tt>failure_message_for_should</tt> and
  # <tt>failure_message_for_should_not</tt>) are passed the actual value (the
  # receiver of <tt>expect(..)</tt> or <tt>expect(..).not_to</tt>).
  #
  # ### Custom Matcher from scratch
  #
  # You could also write a custom matcher from scratch, as follows:
  #
  #     class BeInZone
  #       def initialize(expected)
  #         @expected = expected
  #       end
  #
  #       def matches?(target)
  #         @target = target
  #         @target.current_zone.eql?(Zone.new(@expected))
  #       end
  #
  #       def failure_message_for_should
  #         "expected #{@target.inspect} to be in Zone #{@expected}"
  #       end
  #
  #       def failure_message_for_should_not
  #         "expected #{@target.inspect} not to be in Zone #{@expected}"
  #       end
  #     end
  #
  # ... and a method like this:
  #
  #     def be_in_zone(expected)
  #       BeInZone.new(expected)
  #     end
  #
  # And then expose the method to your specs. This is normally done
  # by including the method and the class in a module, which is then
  # included in your spec:
  #
  #     module CustomGameMatchers
  #       class BeInZone
  #         # ...
  #       end
  #
  #       def be_in_zone(expected)
  #         # ...
  #       end
  #     end
  #
  #     describe "Player behaviour" do
  #       include CustomGameMatchers
  #       # ...
  #     end
  #
  # or you can include in globally in a spec_helper.rb file <tt>require</tt>d
  # from your spec file(s):
  #
  #     RSpec::configure do |config|
  #       config.include(CustomGameMatchers)
  #     end
  module Matchers
    # @api private
    def self.is_a_matcher?(obj)
      return true  if ::RSpec::Matchers::BuiltIn::BaseMatcher === obj
      return false if obj.respond_to?(:i_respond_to_everything_so_im_not_really_a_matcher)
      return false unless obj.respond_to?(:matches?)

      obj.respond_to?(:failure_message_for_should) || obj.respond_to?(:failure_message)
    end

    # Passes if actual is truthy (anything but false or nil)
    def be_true
      BuiltIn::BeTrue.new
    end

    # Passes if actual is falsy (false or nil)
    def be_false
      BuiltIn::BeFalse.new
    end

    # Passes if actual is nil
    def be_nil
      BuiltIn::BeNil.new
    end

    # @example
    #   expect(actual).to     be_true
    #   expect(actual).to     be_false
    #   expect(actual).to     be_nil
    #   expect(actual).to     be_[arbitrary_predicate](*args)
    #   expect(actual).not_to be_nil
    #   expect(actual).not_to be_[arbitrary_predicate](*args)
    #
    # Given true, false, or nil, will pass if actual value is true, false or
    # nil (respectively). Given no args means the caller should satisfy an if
    # condition (to be or not to be).
    #
    # Predicates are any Ruby method that ends in a "?" and returns true or
    # false.  Given be_ followed by arbitrary_predicate (without the "?"),
    # RSpec will match convert that into a query against the target object.
    #
    # The arbitrary_predicate feature will handle any predicate prefixed with
    # "be_an_" (e.g. be_an_instance_of), "be_a_" (e.g. be_a_kind_of) or "be_"
    # (e.g. be_empty), letting you choose the prefix that best suits the
    # predicate.
    def be(*args)
      args.empty? ?
        Matchers::BuiltIn::Be.new : equal(*args)
    end

    # passes if target.kind_of?(klass)
    def be_a(klass)
      be_a_kind_of(klass)
    end

    alias_method :be_an, :be_a

    # Passes if actual.instance_of?(expected)
    #
    # @example
    #
    #   expect(5).to     be_an_instance_of(Fixnum)
    #   expect(5).not_to be_an_instance_of(Numeric)
    #   expect(5).not_to be_an_instance_of(Float)
    def be_an_instance_of(expected)
      BuiltIn::BeAnInstanceOf.new(expected)
    end

    alias_method :be_instance_of, :be_an_instance_of

    # Passes if actual.kind_of?(expected)
    #
    # @example
    #
    #   expect(5).to     be_a_kind_of(Fixnum)
    #   expect(5).to     be_a_kind_of(Numeric)
    #   expect(5).not_to be_a_kind_of(Float)
    def be_a_kind_of(expected)
      BuiltIn::BeAKindOf.new(expected)
    end

    alias_method :be_kind_of, :be_a_kind_of

    # Passes if actual == expected +/- delta
    #
    # @example
    #
    #   expect(result).to     be_within(0.5).of(3.0)
    #   expect(result).not_to be_within(0.5).of(3.0)
    def be_within(delta)
      BuiltIn::BeWithin.new(delta)
    end

    # Applied to a proc, specifies that its execution will cause some value to
    # change.
    #
    # @param [Object] receiver
    # @param [Symbol] message the message to send the receiver
    #
    # You can either pass <tt>receiver</tt> and <tt>message</tt>, or a block,
    # but not both.
    #
    # When passing a block, it must use the <tt>{ ... }</tt> format, not
    # do/end, as <tt>{ ... }</tt> binds to the +change+ method, whereas do/end
    # would errantly bind to the +expect(..)+ or +expect(..).not_to+ method.
    #
    # @example
    #
    #   expect {
    #     team.add_player(player)
    #   }.to change(roster, :count)
    #
    #   expect {
    #     team.add_player(player)
    #   }.to change(roster, :count).by(1)
    #
    #   expect {
    #     team.add_player(player)
    #   }.to change(roster, :count).by_at_least(1)
    #
    #   expect {
    #     team.add_player(player)
    #   }.to change(roster, :count).by_at_most(1)
    #
    #   string = "string"
    #   expect {
    #     string.reverse!
    #   }.to change { string }.from("string").to("gnirts")
    #
    #   string = "string"
    #   expect {
    #     string
    #   }.not_to change { string }
    #
    #   expect {
    #     person.happy_birthday
    #   }.to change(person, :birthday).from(32).to(33)
    #
    #   expect {
    #     employee.develop_great_new_social_networking_app
    #   }.to change(employee, :title).from("Mail Clerk").to("CEO")
    #
    #   expect {
    #     doctor.leave_office
    #   }.to change(doctor, :sign).from(/is in/).to(/is out/)
    #
    #   user = User.new(:type => "admin")
    #   expect {
    #     user.symbolize_type
    #   }.to change(user, :type).from(String).to(Symbol)
    #
    # == Notes
    #
    # Evaluates <tt>receiver.message</tt> or <tt>block</tt> before and after it
    # evaluates the block passed to <tt>expect</tt>.
    #
    # <tt>expect( ... ).not_to change</tt> only supports the form with no subsequent
    # calls to <tt>by</tt>, <tt>by_at_least</tt>, <tt>by_at_most</tt>,
    # <tt>to</tt> or <tt>from</tt>.
    def change(receiver=nil, message=nil, &block)
      BuiltIn::Change.new(receiver, message, &block)
    end

    # Passes if actual covers expected. This works for
    # Ranges. You can also pass in multiple args
    # and it will only pass if all args are found in Range.
    #
    # @example
    #   expect(1..10).to     cover(5)
    #   expect(1..10).to     cover(4, 6)
    #   expect(1..10).to     cover(4, 6, 11) # fails
    #   expect(1..10).not_to cover(11)
    #   expect(1..10).not_to cover(5)        # fails
    #
    # ### Warning:: Ruby >= 1.9 only
    def cover(*values)
      BuiltIn::Cover.new(*values)
    end if (1..2).respond_to?(:cover?)

    # Matches if the actual value ends with the expected value(s). In the case
    # of a string, matches against the last `expected.length` characters of the
    # actual string. In the case of an array, matches against the last
    # `expected.length` elements of the actual array.
    #
    # @example
    #
    #   expect("this string").to   end_with "string"
    #   expect([0, 1, 2, 3, 4]).to end_with 4
    #   expect([0, 2, 3, 4, 4]).to end_with 3, 4
    def end_with(*expected)
      BuiltIn::EndWith.new(*expected)
    end

    # Passes if <tt>actual == expected</tt>.
    #
    # See http://www.ruby-doc.org/core/classes/Object.html#M001057 for more
    # information about equality in Ruby.
    #
    # @example
    #
    #   expect(5).to     eq(5)
    #   expect(5).not_to eq(3)
    def eq(expected)
      BuiltIn::Eq.new(expected)
    end

    # Passes if +actual.eql?(expected)+
    #
    # See http://www.ruby-doc.org/core/classes/Object.html#M001057 for more
    # information about equality in Ruby.
    #
    # @example
    #
    #   expect(5).to     eql(5)
    #   expect(5).not_to eql(3)
    def eql(expected)
      BuiltIn::Eql.new(expected)
    end

    # Passes if <tt>actual.equal?(expected)</tt> (object identity).
    #
    # See http://www.ruby-doc.org/core/classes/Object.html#M001057 for more
    # information about equality in Ruby.
    #
    # @example
    #
    #   expect(5).to       equal(5)   # Fixnums are equal
    #   expect("5").not_to equal("5") # Strings that look the same are not the same object
    def equal(expected)
      BuiltIn::Equal.new(expected)
    end

    # Passes if `actual.exist?` or `actual.exists?`
    #
    # @example
    #   expect(File).to exist("path/to/file")
    def exist(*args)
      BuiltIn::Exist.new(*args)
    end

    # Passes if receiver is a collection with the submitted number of items OR
    # if the receiver OWNS a collection with the submitted number of items.
    #
    # If the receiver OWNS the collection, you must use the name of the
    # collection. So if a `Team` instance has a collection named `#players`,
    # you must use that name to set the expectation.
    #
    # If the receiver IS the collection, you can use any name you like for
    # `named_collection`. We'd recommend using either "elements", "members", or
    # "items" as these are all standard ways of describing the things IN a
    # collection.
    #
    # This also works for Strings, letting you set expectations about their
    # lengths.
    #
    # @example
    #
    #   # Passes if team.players.size == 11
    #   expect(team).to have(11).players
    #
    #   # Passes if [1,2,3].length == 3
    #   expect([1,2,3]).to have(3).items #"items" is pure sugar
    #
    #   # Passes if ['a', 'b', 'c'].count == 3
    #   expect([1,2,3]).to have(3).items #"items" is pure sugar
    #
    #   # Passes if "this string".length == 11
    #   expect("this string").to have(11).characters #"characters" is pure sugar
    def have(n)
      BuiltIn::Have.new(n)
    end
    alias :have_exactly :have

    # Exactly like have() with >=.
    #
    # @example
    #   expect("this").to have_at_least(3).letters
    #
    # ### Warning:
    #
    # `expect(..).not_to have_at_least` is not supported
    def have_at_least(n)
      BuiltIn::Have.new(n, :at_least)
    end

    # Exactly like have() with <=.
    #
    # @example
    #   expect("this").to have_at_most(4).letters
    #
    # ### Warning:
    #
    # `expect(..).not_to have_at_most` is not supported
    def have_at_most(n)
      BuiltIn::Have.new(n, :at_most)
    end

    # Passes if actual includes expected. This works for
    # collections and Strings. You can also pass in multiple args
    # and it will only pass if all args are found in collection.
    #
    # @example
    #
    #   expect([1,2,3]).to      include(3)
    #   expect([1,2,3]).to      include(2,3)
    #   expect([1,2,3]).to      include(2,3,4) # fails
    #   expect([1,2,3]).not_to  include(4)
    #   expect("spread").to     include("read")
    #   expect("spread").not_to include("red")
    def include(*expected)
      BuiltIn::Include.new(*expected)
    end

    # Given a Regexp or String, passes if actual.match(pattern)
    #
    # @example
    #
    #   expect(email).to   match(/^([^\s]+)((?:[-a-z0-9]+\.)+[a-z]{2,})$/i)
    #   expect(email).to   match("@example.com")
    #   expect(zipcode).to match_regex(/\A\d{5}(-\d{4})?\z/)
    #   expect(zipcode).to match_regex("90210")
    #
    # @note Due to Ruby's method dispatch mechanism, using the `#match` matcher
    # within a custom matcher defined via the matcher DSL
    # (`RSpec::Matcher.define`) will result Ruby calling the wrong `#match`
    # method and raising an `ArgumentError`. Instead, use the aliased
    # `#match_regex` method.
    def match(expected)
      BuiltIn::Match.new(expected)
    end

    alias_method :match_regex, :match

    # With no args, matches if any error is raised.
    # With a named error, matches only if that specific error is raised.
    # With a named error and messsage specified as a String, matches only if both match.
    # With a named error and messsage specified as a Regexp, matches only if both match.
    # Pass an optional block to perform extra verifications on the exception matched
    #
    # @example
    #
    #   expect { do_something_risky }.to raise_error
    #   expect { do_something_risky }.to raise_error(PoorRiskDecisionError)
    #   expect { do_something_risky }.to raise_error(PoorRiskDecisionError) { |error| expect(error.data).to eq 42 }
    #   expect { do_something_risky }.to raise_error(PoorRiskDecisionError, "that was too risky")
    #   expect { do_something_risky }.to raise_error(PoorRiskDecisionError, /oo ri/)
    #
    #   expect { do_something_risky }.not_to raise_error
    def raise_error(error=Exception, message=nil, &block)
      BuiltIn::RaiseError.new(error, message, &block)
    end

    alias_method :raise_exception, :raise_error

    # Matches if the target object responds to all of the names
    # provided. Names can be Strings or Symbols.
    #
    # @example
    #
    # expect("string").to respond_to(:length)
    #
    def respond_to(*names)
      BuiltIn::RespondTo.new(*names)
    end

    # Passes if the submitted block returns true. Yields target to the
    # block.
    #
    # Generally speaking, this should be thought of as a last resort when
    # you can't find any other way to specify the behaviour you wish to
    # specify.
    #
    # If you do find yourself in such a situation, you could always write
    # a custom matcher, which would likely make your specs more expressive.
    #
    # @example
    #
    #   expect(5).to satisfy { |n| n > 3 }
    def satisfy(&block)
      BuiltIn::Satisfy.new(&block)
    end

    # Matches if the actual value starts with the expected value(s). In the
    # case of a string, matches against the first `expected.length` characters
    # of the actual string. In the case of an array, matches against the first
    # `expected.length` elements of the actual array.
    #
    # @example
    #
    #   expect("this string").to   start_with "this s"
    #   expect([0, 1, 2, 3, 4]).to start_with 0
    #   expect([0, 2, 3, 4, 4]).to start_with 0, 1
    def start_with(*expected)
      BuiltIn::StartWith.new(*expected)
    end

    # Given no argument, matches if a proc throws any Symbol.
    #
    # Given a Symbol, matches if the given proc throws the specified Symbol.
    #
    # Given a Symbol and an arg, matches if the given proc throws the
    # specified Symbol with the specified arg.
    #
    # @example
    #
    #   expect { do_something_risky }.to throw_symbol
    #   expect { do_something_risky }.to throw_symbol(:that_was_risky)
    #   expect { do_something_risky }.to throw_symbol(:that_was_risky, 'culprit')
    #
    #   expect { do_something_risky }.not_to throw_symbol
    #   expect { do_something_risky }.not_to throw_symbol(:that_was_risky)
    #   expect { do_something_risky }.not_to throw_symbol(:that_was_risky, 'culprit')
    def throw_symbol(expected_symbol=nil, expected_arg=nil)
      BuiltIn::ThrowSymbol.new(expected_symbol, expected_arg)
    end

    # Passes if the method called in the expect block yields, regardless
    # of whether or not arguments are yielded.
    #
    # @example
    #
    #   expect { |b| 5.tap(&b) }.to yield_control
    #   expect { |b| "a".to_sym(&b) }.not_to yield_control
    #
    # @note Your expect block must accept a parameter and pass it on to
    #   the method-under-test as a block.
    # @note This matcher is not designed for use with methods that yield
    #   multiple times.
    def yield_control
      BuiltIn::YieldControl.new
    end

    # Passes if the method called in the expect block yields with
    # no arguments. Fails if it does not yield, or yields with arguments.
    #
    # @example
    #
    #   expect { |b| User.transaction(&b) }.to yield_with_no_args
    #   expect { |b| 5.tap(&b) }.not_to yield_with_no_args # because it yields with `5`
    #   expect { |b| "a".to_sym(&b) }.not_to yield_with_no_args # because it does not yield
    #
    # @note Your expect block must accept a parameter and pass it on to
    #   the method-under-test as a block.
    # @note This matcher is not designed for use with methods that yield
    #   multiple times.
    def yield_with_no_args
      BuiltIn::YieldWithNoArgs.new
    end

    # Given no arguments, matches if the method called in the expect
    # block yields with arguments (regardless of what they are or how
    # many there are).
    #
    # Given arguments, matches if the method called in the expect block
    # yields with arguments that match the given arguments.
    #
    # Argument matching is done using `===` (the case match operator)
    # and `==`. If the expected and actual arguments match with either
    # operator, the matcher will pass.
    #
    # @example
    #
    #   expect { |b| 5.tap(&b) }.to yield_with_args # because #tap yields an arg
    #   expect { |b| 5.tap(&b) }.to yield_with_args(5) # because 5 == 5
    #   expect { |b| 5.tap(&b) }.to yield_with_args(Fixnum) # because Fixnum === 5
    #   expect { |b| File.open("f.txt", &b) }.to yield_with_args(/txt/) # because /txt/ === "f.txt"
    #
    #   expect { |b| User.transaction(&b) }.not_to yield_with_args # because it yields no args
    #   expect { |b| 5.tap(&b) }.not_to yield_with_args(1, 2, 3)
    #
    # @note Your expect block must accept a parameter and pass it on to
    #   the method-under-test as a block.
    # @note This matcher is not designed for use with methods that yield
    #   multiple times.
    def yield_with_args(*args)
      BuiltIn::YieldWithArgs.new(*args)
    end

    # Designed for use with methods that repeatedly yield (such as
    # iterators). Passes if the method called in the expect block yields
    # multiple times with arguments matching those given.
    #
    # Argument matching is done using `===` (the case match operator)
    # and `==`. If the expected and actual arguments match with either
    # operator, the matcher will pass.
    #
    # @example
    #
    #   expect { |b| [1, 2, 3].each(&b) }.to yield_successive_args(1, 2, 3)
    #   expect { |b| { :a => 1, :b => 2 }.each(&b) }.to yield_successive_args([:a, 1], [:b, 2])
    #   expect { |b| [1, 2, 3].each(&b) }.not_to yield_successive_args(1, 2)
    #
    # @note Your expect block must accept a parameter and pass it on to
    #   the method-under-test as a block.
    def yield_successive_args(*args)
      BuiltIn::YieldSuccessiveArgs.new(*args)
    end

    # Passes if actual contains all of the expected regardless of order.
    # This works for collections. Pass in multiple args and it will only
    # pass if all args are found in collection.
    #
    # @note This is also available using the `=~` operator with `should`,
    #       but `=~` is not supported with `expect`.
    #
    # @note This matcher only supports positive expectations.
    #       expect(..).not_to match_array(other_array) is not supported.
    #
    # @example
    #
    #   expect([1,2,3]).to match_array([1,2,3])
    #   expect([1,2,3]).to match_array([1,3,2])
    def match_array(array)
      BuiltIn::MatchArray.new(array)
    end

    OperatorMatcher.register(Enumerable, '=~', BuiltIn::MatchArray)
  end
end
