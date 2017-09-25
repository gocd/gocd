module RSpec
  module Core
    module MemoizedHelpers
      # @note `subject` was contributed by Joe Ferris to support the one-liner
      #   syntax embraced by shoulda matchers:
      #
      #       describe Widget do
      #         it { should validate_presence_of(:name) }
      #       end
      #
      #   While the examples below demonstrate how to use `subject`
      #   explicitly in examples, we recommend that you define a method with
      #   an intention revealing name instead.
      #
      # @example
      #
      #   # explicit declaration of subject
      #   describe Person do
      #     subject { Person.new(:birthdate => 19.years.ago) }
      #     it "should be eligible to vote" do
      #       subject.should be_eligible_to_vote
      #       # ^ ^ explicit reference to subject not recommended
      #     end
      #   end
      #
      #   # implicit subject => { Person.new }
      #   describe Person do
      #     it "should be eligible to vote" do
      #       subject.should be_eligible_to_vote
      #       # ^ ^ explicit reference to subject not recommended
      #     end
      #   end
      #
      #   # one-liner syntax - should is invoked on subject
      #   describe Person do
      #     it { should be_eligible_to_vote }
      #   end
      #
      # @note Because `subject` is designed to create state that is reset between
      #   each example, and `before(:all)` is designed to setup state that is
      #   shared across _all_ examples in an example group, `subject` is _not_
      #   intended to be used in a `before(:all)` hook. RSpec 2.13.1 prints
      #   a warning when you reference a `subject` from `before(:all)` and we plan
      #   to have it raise an error in RSpec 3.
      #
      # @see #should
      def subject
        __memoized.fetch(:subject) do
          __memoized[:subject] = begin
            described = described_class || self.class.description
            Class === described ? described.new : described
          end
        end
      end

      # When `should` is called with no explicit receiver, the call is
      # delegated to the object returned by `subject`. Combined with an
      # implicit subject this supports very concise expressions.
      #
      # @example
      #
      #   describe Person do
      #     it { should be_eligible_to_vote }
      #   end
      #
      # @see #subject
      def should(matcher=nil, message=nil)
        RSpec::Expectations::PositiveExpectationHandler.handle_matcher(subject, matcher, message)
      end

      # Just like `should`, `should_not` delegates to the subject (implicit or
      # explicit) of the example group.
      #
      # @example
      #
      #   describe Person do
      #     it { should_not be_eligible_to_vote }
      #   end
      #
      # @see #subject
      def should_not(matcher=nil, message=nil)
        RSpec::Expectations::NegativeExpectationHandler.handle_matcher(subject, matcher, message)
      end

      private

      # @private
      def __memoized
        @__memoized ||= {}
      end

      # Used internally to customize the behavior of the
      # memoized hash when used in a `before(:all)` hook.
      #
      # @private
      class BeforeAllMemoizedHash
        def initialize(example_group_instance)
          @example_group_instance = example_group_instance
          @hash = {}
        end

        def self.isolate_for_before_all(example_group_instance)
          example_group_instance.instance_eval do
            @__memoized = BeforeAllMemoizedHash.new(self)

            begin
              yield
            ensure
              @__memoized.preserve_accessed_lets
              @__memoized = nil
            end
          end
        end

        def fetch(key, &block)
          description = if key == :subject
            "subject"
          else
            "let declaration `#{key}`"
          end

          ::RSpec.warn_deprecation <<-EOS
WARNING: #{description} accessed in a `before(:all)` hook at:
  #{caller[1]}

This is deprecated behavior that will not be supported in RSpec 3.

`let` and `subject` declarations are not intended to be called
in a `before(:all)` hook, as they exist to define state that
is reset between each example, while `before(:all)` exists to
define state that is shared across examples in an example group.
EOS

          @hash.fetch(key, &block)
        end

        def []=(key, value)
          @hash[key] = value
        end

        def preserve_accessed_lets
          hash = @hash

          @example_group_instance.class.class_eval do
            hash.each do |key, value|
              undef_method(key) if method_defined?(key)
              define_method(key) { value }
            end
          end
        end
      end

      def self.included(mod)
        mod.extend(ClassMethods)
      end

      module ClassMethods
        # Generates a method whose return value is memoized after the first
        # call. Useful for reducing duplication between examples that assign
        # values to the same local variable.
        #
        # @note `let` _can_ enhance readability when used sparingly (1,2, or
        #   maybe 3 declarations) in any given example group, but that can
        #   quickly degrade with overuse. YMMV.
        #
        # @note `let` uses an `||=` conditional that has the potential to
        #   behave in surprising ways in examples that spawn separate threads,
        #   though we have yet to see this in practice. You've been warned.
        #
        # @note Because `let` is designed to create state that is reset between
        #   each example, and `before(:all)` is designed to setup state that is
        #   shared across _all_ examples in an example group, `let` is _not_
        #   intended to be used in a `before(:all)` hook. RSpec 2.13.1 prints
        #   a warning when you reference a `let` from `before(:all)` and we plan
        #   to have it raise an error in RSpec 3.
        #
        # @example
        #
        #   describe Thing do
        #     let(:thing) { Thing.new }
        #
        #     it "does something" do
        #       # first invocation, executes block, memoizes and returns result
        #       thing.do_something
        #
        #       # second invocation, returns the memoized value
        #       thing.should be_something
        #     end
        #   end
        def let(name, &block)
          # We have to pass the block directly to `define_method` to
          # allow it to use method constructs like `super` and `return`.
          raise "#let or #subject called without a block" if block.nil?
          MemoizedHelpers.module_for(self).send(:define_method, name, &block)

          # Apply the memoization. The method has been defined in an ancestor
          # module so we can use `super` here to get the value.
          define_method(name) do
            __memoized.fetch(name) { |k| __memoized[k] = super(&nil) }
          end
        end

        # Just like `let`, except the block is invoked by an implicit `before`
        # hook. This serves a dual purpose of setting up state and providing a
        # memoized reference to that state.
        #
        # @example
        #
        #   class Thing
        #     def self.count
        #       @count ||= 0
        #     end
        #
        #     def self.count=(val)
        #       @count += val
        #     end
        #
        #     def self.reset_count
        #       @count = 0
        #     end
        #
        #     def initialize
        #       self.class.count += 1
        #     end
        #   end
        #
        #   describe Thing do
        #     after(:each) { Thing.reset_count }
        #
        #     context "using let" do
        #       let(:thing) { Thing.new }
        #
        #       it "is not invoked implicitly" do
        #         Thing.count.should eq(0)
        #       end
        #
        #       it "can be invoked explicitly" do
        #         thing
        #         Thing.count.should eq(1)
        #       end
        #     end
        #
        #     context "using let!" do
        #       let!(:thing) { Thing.new }
        #
        #       it "is invoked implicitly" do
        #         Thing.count.should eq(1)
        #       end
        #
        #       it "returns memoized version on first invocation" do
        #         thing
        #         Thing.count.should eq(1)
        #       end
        #     end
        #   end
        def let!(name, &block)
          let(name, &block)
          before { __send__(name) }
        end

        # Declares a `subject` for an example group which can then be the
        # implicit receiver (through delegation) of calls to `should`.
        #
        # Given a `name`, defines a method with that name which returns the
        # `subject`. This lets you declare the subject once and access it
        # implicitly in one-liners and explicitly using an intention revealing
        # name.
        #
        # @param [String,Symbol] name used to define an accessor with an
        #   intention revealing name
        # @param block defines the value to be returned by `subject` in examples
        #
        # @example
        #
        #   describe CheckingAccount, "with $50" do
        #     subject { CheckingAccount.new(Money.new(50, :USD)) }
        #     it { should have_a_balance_of(Money.new(50, :USD)) }
        #     it { should_not be_overdrawn }
        #   end
        #
        #   describe CheckingAccount, "with a non-zero starting balance" do
        #     subject(:account) { CheckingAccount.new(Money.new(50, :USD)) }
        #     it { should_not be_overdrawn }
        #     it "has a balance equal to the starting balance" do
        #       account.balance.should eq(Money.new(50, :USD))
        #     end
        #   end
        #
        # @see MemoizedHelpers#should
        def subject(name=nil, &block)
          if name
            let(name, &block)
            alias_method :subject, name

            self::NamedSubjectPreventSuper.send(:define_method, name) do
              raise NotImplementedError, "`super` in named subjects is not supported"
            end
          else
            let(:subject, &block)
          end
        end

        # Just like `subject`, except the block is invoked by an implicit `before`
        # hook. This serves a dual purpose of setting up state and providing a
        # memoized reference to that state.
        #
        # @example
        #
        #   class Thing
        #     def self.count
        #       @count ||= 0
        #     end
        #
        #     def self.count=(val)
        #       @count += val
        #     end
        #
        #     def self.reset_count
        #       @count = 0
        #     end
        #
        #     def initialize
        #       self.class.count += 1
        #     end
        #   end
        #
        #   describe Thing do
        #     after(:each) { Thing.reset_count }
        #
        #     context "using subject" do
        #       subject { Thing.new }
        #
        #       it "is not invoked implicitly" do
        #         Thing.count.should eq(0)
        #       end
        #
        #       it "can be invoked explicitly" do
        #         subject
        #         Thing.count.should eq(1)
        #       end
        #     end
        #
        #     context "using subject!" do
        #       subject!(:thing) { Thing.new }
        #
        #       it "is invoked implicitly" do
        #         Thing.count.should eq(1)
        #       end
        #
        #       it "returns memoized version on first invocation" do
        #         subject
        #         Thing.count.should eq(1)
        #       end
        #     end
        #   end
        def subject!(name=nil, &block)
          subject(name, &block)
          before { subject }
        end

        # Creates a nested example group named by the submitted `attribute`,
        # and then generates an example using the submitted block.
        #
        # @example
        #
        #   # This ...
        #   describe Array do
        #     its(:size) { should eq(0) }
        #   end
        #
        #   # ... generates the same runtime structure as this:
        #   describe Array do
        #     describe "size" do
        #       it "should eq(0)" do
        #         subject.size.should eq(0)
        #       end
        #     end
        #   end
        #
        # The attribute can be a `Symbol` or a `String`. Given a `String`
        # with dots, the result is as though you concatenated that `String`
        # onto the subject in an expression.
        #
        # @example
        #
        #   describe Person do
        #     subject do
        #       Person.new.tap do |person|
        #         person.phone_numbers << "555-1212"
        #       end
        #     end
        #
        #     its("phone_numbers.first") { should eq("555-1212") }
        #   end
        #
        # When the subject is a `Hash`, you can refer to the Hash keys by
        # specifying a `Symbol` or `String` in an array.
        #
        # @example
        #
        #   describe "a configuration Hash" do
        #     subject do
        #       { :max_users => 3,
        #         'admin' => :all_permissions }
        #     end
        #
        #     its([:max_users]) { should eq(3) }
        #     its(['admin']) { should eq(:all_permissions) }
        #
        #     # You can still access to its regular methods this way:
        #     its(:keys) { should include(:max_users) }
        #     its(:count) { should eq(2) }
        #   end
        #
        # Note that this method does not modify `subject` in any way, so if you
        # refer to `subject` in `let` or `before` blocks, you're still
        # referring to the outer subject.
        #
        # @example
        #
        #   describe Person do
        #     subject { Person.new }
        #     before { subject.age = 25 }
        #     its(:age) { should eq(25) }
        #   end
        def its(attribute, &block)
          describe(attribute) do
            if Array === attribute
              let(:__its_subject) { subject[*attribute] }
            else
              let(:__its_subject) do
                attribute_chain = attribute.to_s.split('.')
                attribute_chain.inject(subject) do |inner_subject, attr|
                  inner_subject.send(attr)
                end
              end
            end

            def should(matcher=nil, message=nil)
              RSpec::Expectations::PositiveExpectationHandler.handle_matcher(__its_subject, matcher, message)
            end

            def should_not(matcher=nil, message=nil)
              RSpec::Expectations::NegativeExpectationHandler.handle_matcher(__its_subject, matcher, message)
            end

            example(&block)
          end
        end
      end

      # @api private
      #
      # Gets the LetDefinitions module. The module is mixed into
      # the example group and is used to hold all let definitions.
      # This is done so that the block passed to `let` can be
      # forwarded directly on to `define_method`, so that all method
      # constructs (including `super` and `return`) can be used in
      # a `let` block.
      #
      # The memoization is provided by a method definition on the
      # example group that supers to the LetDefinitions definition
      # in order to get the value to memoize.
      def self.module_for(example_group)
        get_constant_or_yield(example_group, :LetDefinitions) do
          mod = Module.new do
            include Module.new {
              example_group.const_set(:NamedSubjectPreventSuper, self)
            }
          end

          example_group.const_set(:LetDefinitions, mod)
          mod
        end
      end

      # @api private
      def self.define_helpers_on(example_group)
        example_group.send(:include, module_for(example_group))
      end

      if Module.method(:const_defined?).arity == 1 # for 1.8
        # @api private
        #
        # Gets the named constant or yields.
        # On 1.8, const_defined? / const_get do not take into
        # account the inheritance hierarchy.
        def self.get_constant_or_yield(example_group, name)
          if example_group.const_defined?(name)
            example_group.const_get(name)
          else
            yield
          end
        end
      else
        # @api private
        #
        # Gets the named constant or yields.
        # On 1.9, const_defined? / const_get take into account the
        # the inheritance by default, and accept an argument to
        # disable this behavior. It's important that we don't
        # consider inheritance here; each example group level that
        # uses a `let` should get its own `LetDefinitions` module.
        def self.get_constant_or_yield(example_group, name)
          if example_group.const_defined?(name, (check_ancestors = false))
            example_group.const_get(name, check_ancestors)
          else
            yield
          end
        end
      end
    end
  end
end
