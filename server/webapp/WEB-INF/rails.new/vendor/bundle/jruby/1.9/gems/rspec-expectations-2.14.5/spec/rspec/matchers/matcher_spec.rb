require 'spec_helper'

class UnexpectedError < StandardError; end
module MatcherHelperModule
  def self.included(base)
    base.module_eval do
      def included_method; end
    end
  end

  def self.extended(base)
    base.instance_eval do
      def extended_method; end
    end
  end

  def greeting
    "Hello, World"
  end
end

module RSpec::Matchers::DSL
  describe Matcher do
    it "can be stored aside and used later" do
      # Supports using rspec-expectation matchers as argument matchers in
      # rspec-mocks.
      RSpec::Matchers.define :example_matcher do |expected|
        match do |actual|
          actual == expected
        end
      end

      m1 = example_matcher(1)
      m2 = example_matcher(2)

      expect(m1.matches?(1)).to be_true
      expect(m2.matches?(2)).to be_true
    end

    context "with an included module" do
      let(:matcher) do
        RSpec::Matchers::DSL::Matcher.new(:be_a_greeting) do
          include MatcherHelperModule
          match { |actual| actual == greeting }
        end.for_expected
      end

      it "has access to the module's methods" do
        matcher.matches?("Hello, World")
      end

      it "runs the module's included hook" do
        expect(matcher).to respond_to(:included_method)
      end

      it "does not run the module's extended hook" do
        expect(matcher).not_to respond_to(:extended_method)
      end

      it 'allows multiple modules to be included at once' do
        m = RSpec::Matchers::DSL::Matcher.new(:multiple_modules) do
          include Enumerable, Comparable
        end.for_expected
        expect(m).to be_a(Enumerable)
        expect(m).to be_a(Comparable)
      end
    end

    context "without overrides" do
      before(:each) do
        @matcher = RSpec::Matchers::DSL::Matcher.new(:be_a_multiple_of) do |multiple|
          match do |actual|
            actual % multiple == 0
          end
        end.for_expected(3)
      end

      it "provides a default description" do
        expect(@matcher.description).to eq "be a multiple of 3"
      end

      it "provides a default failure message for #should" do
        @matcher.matches?(8)
        expect(@matcher.failure_message_for_should).to eq "expected 8 to be a multiple of 3"
      end

      it "provides a default failure message for #should_not" do
        @matcher.matches?(9)
        expect(@matcher.failure_message_for_should_not).to eq "expected 9 not to be a multiple of 3"
      end
    end

    context "with separate match logic for should and should not" do
      let(:matcher) do
        RSpec::Matchers::DSL::Matcher.new(:to_be_composed_of) do |a, b|
          match_for_should do |actual|
            actual == a * b
          end

          match_for_should_not do |actual|
            actual == a + b
          end
        end.for_expected(7, 11)
      end

      it "invokes the match_for_should block for #matches?" do
        expect(matcher.matches?(77)).to be_true
        expect(matcher.matches?(18)).to be_false
      end

      it "invokes the match_for_should_not block for #does_not_match?" do
        expect(matcher.does_not_match?(77)).to be_false
        expect(matcher.does_not_match?(18)).to be_true
      end

      it "provides a default failure message for #should_not" do
        matcher.does_not_match?(77)
        expect(matcher.failure_message_for_should_not).to eq "expected 77 not to to be composed of 7 and 11"
      end
    end

    it "allows helper methods to be defined with #define_method to have access to matcher parameters" do
      matcher = RSpec::Matchers::DSL::Matcher.new(:name) do |a, b|
        define_method(:sum) { a + b }
      end.for_expected(3,4)

      expect(matcher.sum).to eq 7
    end

    it "is not diffable by default" do
      matcher = RSpec::Matchers::DSL::Matcher.new(:name) {}
      expect(matcher).not_to be_diffable
    end

    it "is diffable when told to be" do
      matcher = RSpec::Matchers::DSL::Matcher.new(:name) { diffable }.for_expected
      expect(matcher).to be_diffable
    end

    it "provides expected" do
      matcher = RSpec::Matchers::DSL::Matcher.new(:name) {}.for_expected('expected string')
      expect(matcher.expected).to eq ['expected string']
    end

    it "provides actual" do
      matcher = RSpec::Matchers::DSL::Matcher.new(:name) do
        match {|actual|}
      end.for_expected('expected string')

      matcher.matches?('actual string')

      expect(matcher.actual).to eq 'actual string'
    end

    context "wrapping another expectation (should == ...)" do
      it "returns true if the wrapped expectation passes" do
        matcher = RSpec::Matchers::DSL::Matcher.new(:name) do |expected|
          match do |actual|
            expect(actual).to eq expected
          end
        end.for_expected('value')
        expect(matcher.matches?('value')).to be_true
      end

      it "returns false if the wrapped expectation fails" do
        matcher = RSpec::Matchers::DSL::Matcher.new(:name) do |expected|
          match do |actual|
            expect(actual).to eq expected
          end
        end.for_expected('value')
        expect(matcher.matches?('other value')).to be_false
      end
    end

    context "with overrides" do
      before(:each) do
        @matcher = RSpec::Matchers::DSL::Matcher.new(:be_boolean) do |boolean|
          match do |actual|
            actual
          end
          description do
            "be the boolean #{boolean}"
          end
          failure_message_for_should do |actual|
            "expected #{actual} to be the boolean #{boolean}"
          end
          failure_message_for_should_not do |actual|
            "expected #{actual} not to be the boolean #{boolean}"
          end
        end.for_expected(true)
      end

      it "does not hide result of match block when true" do
        expect(@matcher.matches?(true)).to be_true
      end

      it "does not hide result of match block when false" do
        expect(@matcher.matches?(false)).to be_false
      end

      it "overrides the description" do
        expect(@matcher.description).to eq "be the boolean true"
      end

      it "overrides the failure message for #should" do
        @matcher.matches?(false)
        expect(@matcher.failure_message_for_should).to eq "expected false to be the boolean true"
      end

      it "overrides the failure message for #should_not" do
        @matcher.matches?(true)
        expect(@matcher.failure_message_for_should_not).to eq "expected true not to be the boolean true"
      end
    end

    context "#new" do
      it "passes matches? arg to match block" do
        matcher = RSpec::Matchers::DSL::Matcher.new(:ignore) do
          match do |actual|
            actual == 5
          end
        end.for_expected
        expect(matcher.matches?(5)).to be_true
      end

      it "exposes arg submitted through #new to matcher block" do
        matcher = RSpec::Matchers::DSL::Matcher.new(:ignore) do |expected|
          match do |actual|
            actual > expected
          end
        end.for_expected(4)
        expect(matcher.matches?(5)).to be_true
      end
    end

    context "with no args" do
      before(:each) do
        @matcher = RSpec::Matchers::DSL::Matcher.new(:matcher_name) do
          match do |actual|
            actual == 5
          end
        end.for_expected
      end

      it "matches" do
        expect(@matcher.matches?(5)).to be_true
      end

      it "describes" do
        expect(@matcher.description).to eq "matcher name"
      end
    end

    context "with 1 arg" do
      before(:each) do
        @matcher = RSpec::Matchers::DSL::Matcher.new(:matcher_name) do |expected|
          match do |actual|
            actual == 5 && expected == 1
          end
        end.for_expected(1)
      end

      it "matches" do
        expect(@matcher.matches?(5)).to be_true
      end

      it "describes" do
        expect(@matcher.description).to eq "matcher name 1"
      end
    end

    context "with multiple args" do
      before(:each) do
        @matcher = RSpec::Matchers::DSL::Matcher.new(:matcher_name) do |a,b,c,d|
          match do |sum|
            a + b + c + d == sum
          end
        end.for_expected(1,2,3,4)
      end

      it "matches" do
        expect(@matcher.matches?(10)).to be_true
      end

      it "describes" do
        expect(@matcher.description).to eq "matcher name 1, 2, 3, and 4"
      end
    end

    it "supports helper methods" do
      matcher = RSpec::Matchers::DSL::Matcher.new(:be_similar_to) do |sample|
        match do |actual|
          similar?(sample, actual)
        end

        def similar?(a, b)
          a.sort == b.sort
        end
      end.for_expected([1,2,3])

      expect(matcher.matches?([2,3,1])).to be_true
    end

    it "supports fluent interface" do
      matcher = RSpec::Matchers::DSL::Matcher.new(:first_word) do
        def second_word
          self
        end
      end.for_expected

      expect(matcher.second_word).to eq matcher
    end

    it "treats method missing normally for undeclared methods" do
      matcher = RSpec::Matchers::DSL::Matcher.new(:ignore) { }.for_expected
      expect { matcher.non_existent_method }.to raise_error(NoMethodError)
    end

    it "has access to other matchers" do
      matcher = RSpec::Matchers::DSL::Matcher.new(:ignore) do |expected|
        match do |actual|
          extend RSpec::Matchers
          expect(actual).to eql(5 + expected)
        end
      end.for_expected(3)

      expect(matcher.matches?(8)).to be_true
    end

    context 'when multiple instances of the same matcher are used in the same example' do
      RSpec::Matchers.define(:be_like_a) do |expected|
        match { |actual| actual == expected }
        description { "be like a #{expected}" }
        failure_message_for_should { "expected to be like a #{expected}" }
        failure_message_for_should_not { "expected not to be like a #{expected}" }
      end

      # Note: these bugs were only exposed when creating both instances
      # first, then checking their descriptions/failure messages.
      #
      # That's why we eager-instantiate them here.
      let!(:moose) { be_like_a("moose") }
      let!(:horse) { be_like_a("horse") }

      it 'allows them to use the expected value in the description' do
        expect(horse.description).to eq("be like a horse")
        expect(moose.description).to eq("be like a moose")
      end

      it 'allows them to use the expected value in the positive failure message' do
        expect(moose.failure_message_for_should).to eq("expected to be like a moose")
        expect(horse.failure_message_for_should).to eq("expected to be like a horse")
      end

      it 'allows them to use the expected value in the negative failure message' do
        expect(moose.failure_message_for_should_not).to eq("expected not to be like a moose")
        expect(horse.failure_message_for_should_not).to eq("expected not to be like a horse")
      end

      it 'allows them to match separately' do
        expect("moose").to moose
        expect("horse").to horse
        expect("horse").not_to moose
        expect("moose").not_to horse
      end
    end

    describe "#match_unless_raises" do
      context "with an assertion" do
        let(:mod) do
          Module.new do
            def assert_equal(a,b)
              a == b ? nil : (raise UnexpectedError.new("#{b} does not equal #{a}"))
            end
          end
        end
        let(:matcher) do
          m = mod
          RSpec::Matchers::DSL::Matcher.new :equal do |expected|
            extend m
            match_unless_raises UnexpectedError do
              assert_equal expected, actual
            end
          end.for_expected(4)
        end

        context "with passing assertion" do
          it "passes" do
            expect(matcher.matches?(4)).to be_true
          end
        end

        context "with failing assertion" do
          it "fails" do
            expect(matcher.matches?(5)).to be_false
          end

          it "provides the raised exception" do
            matcher.matches?(5)
            expect(matcher.rescued_exception.message).to eq("5 does not equal 4")
          end
        end
      end

      context "with an unexpected error" do
        let(:matcher) do
          RSpec::Matchers::DSL::Matcher.new :foo do |expected|
            match_unless_raises SyntaxError do |actual|
              raise "unexpected exception"
            end
          end.for_expected(:bar)
        end

        it "raises the error" do
          expect do
            matcher.matches?(:bar)
          end.to raise_error("unexpected exception")
        end
      end

    end

    it "can define chainable methods" do
      matcher = RSpec::Matchers::DSL::Matcher.new(:name) do
        chain(:expecting) do |expected_value|
          @expected_value = expected_value
        end
        match { |actual| actual == @expected_value }
      end.for_expected

      expect(matcher.expecting('value').matches?('value')).to be_true
      expect(matcher.expecting('value').matches?('other value')).to be_false
    end

    it "prevents name collisions on chainable methods from different matchers" do
      m1 = RSpec::Matchers::DSL::Matcher.new(:m1) { chain(:foo) { raise "foo in m1" } }.for_expected
      m2 = RSpec::Matchers::DSL::Matcher.new(:m2) { chain(:foo) { raise "foo in m2" } }.for_expected

      expect { m1.foo }.to raise_error("foo in m1")
      expect { m2.foo }.to raise_error("foo in m2")
    end

    context "defined using the dsl" do
      def a_method_in_the_example
        "method defined in the example"
      end

      it "can access methods in the running example" do
        RSpec::Matchers.define(:__access_running_example) do
          match do |actual|
            a_method_in_the_example == "method defined in the example"
          end
        end
        expect(example).to __access_running_example
      end

      it "raises NoMethodError for methods not in the running_example" do
        RSpec::Matchers.define(:__raise_no_method_error) do
          match do |actual|
            a_method_not_in_the_example == "method defined in the example"
          end
        end

        expect do
          expect(example).to __raise_no_method_error
        end.to raise_error(/RSpec::Matchers::DSL::Matcher/)
      end
    end

  end
end
