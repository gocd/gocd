require 'spec_helper'
require 'stringio'

describe "have matcher" do
  let(:inflector) do
    Class.new do
      def self.pluralize(string)
        string.to_s + 's'
      end
    end
  end

  before(:each) do
    stub_const("ActiveSupport::Inflector", inflector)
    allow_deprecation
  end

  def expect_have_deprecation(expression, message)
    expect_deprecation_with_type(
      "`#{expression}`",
      message,
      'the have matcher'
    )
  end

  def create_collection_owner_with(n)
    owner = RSpec::Expectations::Helper::CollectionOwner.new
    (1..n).each do |number|
      owner.add_to_collection_with_length_method(number)
      owner.add_to_collection_with_size_method(number)
      owner.add_to_collection_with_count_method(number)
    end
    owner
  end

  describe "expect(...).to have(n).items" do
    it_behaves_like "an RSpec matcher", :valid_value => [1, 2], :invalid_value => [1] do
      let(:matcher) { have(2).items }
    end

    it "passes if target has a collection of items with n members" do
      owner = create_collection_owner_with(3)
      expect(owner).to have(3).items_in_collection_with_length_method
      expect(owner).to have(3).items_in_collection_with_size_method
      expect(owner).to have(3).items_in_collection_with_count_method
    end

    it "converts :no to 0" do
      owner = create_collection_owner_with(0)
      expect(owner).to have(:no).items_in_collection_with_length_method
      expect(owner).to have(:no).items_in_collection_with_size_method
      expect(owner).to have(:no).items_in_collection_with_count_method
    end

    it "converts a String argument to Integer" do
      owner = create_collection_owner_with(3)
      expect(owner).to have('3').items_in_collection_with_length_method
      expect(owner).to have('3').items_in_collection_with_size_method
      expect(owner).to have('3').items_in_collection_with_count_method
    end

    it "fails if target has a collection of items with < n members" do
      owner = create_collection_owner_with(3)
      expect {
        expect(owner).to have(4).items_in_collection_with_length_method
      }.to fail_with("expected 4 items_in_collection_with_length_method, got 3")
      expect {
        expect(owner).to have(4).items_in_collection_with_size_method
      }.to fail_with("expected 4 items_in_collection_with_size_method, got 3")
      expect {
        expect(owner).to have(4).items_in_collection_with_count_method
      }.to fail_with("expected 4 items_in_collection_with_count_method, got 3")
    end

    it "fails if target has a collection of items with > n members" do
      owner = create_collection_owner_with(3)
      expect {
        expect(owner).to have(2).items_in_collection_with_length_method
      }.to fail_with("expected 2 items_in_collection_with_length_method, got 3")
      expect {
        expect(owner).to have(2).items_in_collection_with_size_method
      }.to fail_with("expected 2 items_in_collection_with_size_method, got 3")
      expect {
        expect(owner).to have(2).items_in_collection_with_count_method
      }.to fail_with("expected 2 items_in_collection_with_count_method, got 3")
    end
  end

  describe 'expect(...).to have(1).item when ActiveSupport::Inflector is defined' do

    it 'pluralizes the collection name' do
      owner = create_collection_owner_with(1)
      expect(owner).to have(1).item
    end

    context "when ActiveSupport::Inflector is partially loaded without its inflectors" do

      it "does not pluralize the collection name" do
        stub_const("ActiveSupport::Inflector", Module.new)
        owner = create_collection_owner_with(1)
        expect {
          expect(owner).to have(1).item
        }.to raise_error(NoMethodError)
      end

    end
  end

  describe 'expect(...).to have(1).item when Inflector is defined' do
    before { stub_const("Inflector", inflector) }

    it 'pluralizes the collection name' do
      owner = create_collection_owner_with(1)
      expect(owner).to have(1).item
    end
  end

  describe "expect(...).to have(n).items where result responds to items but returns something other than a collection" do
    it "provides a meaningful error" do
      owner = Class.new do
        def items
          Object.new
        end
      end.new
      expect do
        expect(owner).to have(3).items
      end.to raise_error("expected items to be a collection but it does not respond to #length, #size or #count")
    end
  end

  describe "expect(...).not_to have(n).items" do

    it "passes if target has a collection of items with < n members" do
      owner = create_collection_owner_with(3)
      expect(owner).not_to have(4).items_in_collection_with_length_method
      expect(owner).not_to have(4).items_in_collection_with_size_method
      expect(owner).not_to have(4).items_in_collection_with_count_method
    end

    it "passes if target has a collection of items with > n members" do
      owner = create_collection_owner_with(3)
      expect(owner).not_to have(2).items_in_collection_with_length_method
      expect(owner).not_to have(2).items_in_collection_with_size_method
      expect(owner).not_to have(2).items_in_collection_with_count_method
    end

    it "fails if target has a collection of items with n members" do
      owner = create_collection_owner_with(3)
      expect {
        expect(owner).not_to have(3).items_in_collection_with_length_method
      }.to fail_with("expected target not to have 3 items_in_collection_with_length_method, got 3")
      expect {
        expect(owner).not_to have(3).items_in_collection_with_size_method
      }.to fail_with("expected target not to have 3 items_in_collection_with_size_method, got 3")
      expect {
        expect(owner).not_to have(3).items_in_collection_with_count_method
      }.to fail_with("expected target not to have 3 items_in_collection_with_count_method, got 3")
    end
  end

  describe "expect(...).to have_exactly(n).items" do

    it "passes if target has a collection of items with n members" do
      owner = create_collection_owner_with(3)
      expect(owner).to have_exactly(3).items_in_collection_with_length_method
      expect(owner).to have_exactly(3).items_in_collection_with_size_method
      expect(owner).to have_exactly(3).items_in_collection_with_count_method
    end

    it "converts :no to 0" do
      owner = create_collection_owner_with(0)
      expect(owner).to have_exactly(:no).items_in_collection_with_length_method
      expect(owner).to have_exactly(:no).items_in_collection_with_size_method
      expect(owner).to have_exactly(:no).items_in_collection_with_count_method
    end

    it "fails if target has a collection of items with < n members" do
      owner = create_collection_owner_with(3)
      expect {
        expect(owner).to have_exactly(4).items_in_collection_with_length_method
      }.to fail_with("expected 4 items_in_collection_with_length_method, got 3")
      expect {
        expect(owner).to have_exactly(4).items_in_collection_with_size_method
      }.to fail_with("expected 4 items_in_collection_with_size_method, got 3")
      expect {
        expect(owner).to have_exactly(4).items_in_collection_with_count_method
      }.to fail_with("expected 4 items_in_collection_with_count_method, got 3")
    end

    it "fails if target has a collection of items with > n members" do
      owner = create_collection_owner_with(3)
      expect {
        expect(owner).to have_exactly(2).items_in_collection_with_length_method
      }.to fail_with("expected 2 items_in_collection_with_length_method, got 3")
      expect {
        expect(owner).to have_exactly(2).items_in_collection_with_size_method
      }.to fail_with("expected 2 items_in_collection_with_size_method, got 3")
      expect {
        expect(owner).to have_exactly(2).items_in_collection_with_count_method
      }.to fail_with("expected 2 items_in_collection_with_count_method, got 3")
    end
  end

  describe "expect(...).to have_at_least(n).items" do

    it "passes if target has a collection of items with n members" do
      owner = create_collection_owner_with(3)
      expect(owner).to have_at_least(3).items_in_collection_with_length_method
      expect(owner).to have_at_least(3).items_in_collection_with_size_method
      expect(owner).to have_at_least(3).items_in_collection_with_count_method
    end

    it "passes if target has a collection of items with > n members" do
      owner = create_collection_owner_with(3)
      expect(owner).to have_at_least(2).items_in_collection_with_length_method
      expect(owner).to have_at_least(2).items_in_collection_with_size_method
      expect(owner).to have_at_least(2).items_in_collection_with_count_method
    end

    it "fails if target has a collection of items with < n members" do
      owner = create_collection_owner_with(3)
      expect {
        expect(owner).to have_at_least(4).items_in_collection_with_length_method
      }.to fail_with("expected at least 4 items_in_collection_with_length_method, got 3")
      expect {
        expect(owner).to have_at_least(4).items_in_collection_with_size_method
      }.to fail_with("expected at least 4 items_in_collection_with_size_method, got 3")
      expect {
        expect(owner).to have_at_least(4).items_in_collection_with_count_method
      }.to fail_with("expected at least 4 items_in_collection_with_count_method, got 3")
    end

    it "provides educational negative failure messages" do
      #given
      owner = create_collection_owner_with(3)
      length_matcher = have_at_least(3).items_in_collection_with_length_method
      size_matcher = have_at_least(3).items_in_collection_with_size_method
      count_matcher = have_at_least(3).items_in_collection_with_count_method

      #when
      length_matcher.matches?(owner)
      size_matcher.matches?(owner)
      count_matcher.matches?(owner)

      #then
      expect(length_matcher.failure_message_for_should_not).to eq <<-EOF
Isn't life confusing enough?
Instead of having to figure out the meaning of this:
  expect(actual).not_to have_at_least(3).items_in_collection_with_length_method
We recommend that you use this instead:
  expect(actual).to have_at_most(2).items_in_collection_with_length_method
EOF

      expect(size_matcher.failure_message_for_should_not).to eq <<-EOF
Isn't life confusing enough?
Instead of having to figure out the meaning of this:
  expect(actual).not_to have_at_least(3).items_in_collection_with_size_method
We recommend that you use this instead:
  expect(actual).to have_at_most(2).items_in_collection_with_size_method
EOF
      expect(count_matcher.failure_message_for_should_not).to eq <<-EOF
Isn't life confusing enough?
Instead of having to figure out the meaning of this:
  expect(actual).not_to have_at_least(3).items_in_collection_with_count_method
We recommend that you use this instead:
  expect(actual).to have_at_most(2).items_in_collection_with_count_method
EOF
    end
  end

  describe "expect(...).to have_at_most(n).items" do
    it "passes if target has a collection of items with n members" do
      owner = create_collection_owner_with(3)
      expect(owner).to have_at_most(3).items_in_collection_with_length_method
      expect(owner).to have_at_most(3).items_in_collection_with_size_method
      expect(owner).to have_at_most(3).items_in_collection_with_count_method
    end

    it "fails if target has a collection of items with > n members" do
      owner = create_collection_owner_with(3)
      expect {
        expect(owner).to have_at_most(2).items_in_collection_with_length_method
      }.to fail_with("expected at most 2 items_in_collection_with_length_method, got 3")
      expect {
        expect(owner).to have_at_most(2).items_in_collection_with_size_method
      }.to fail_with("expected at most 2 items_in_collection_with_size_method, got 3")
      expect {
        expect(owner).to have_at_most(2).items_in_collection_with_count_method
      }.to fail_with("expected at most 2 items_in_collection_with_count_method, got 3")
    end

    it "passes if target has a collection of items with < n members" do
      owner = create_collection_owner_with(3)
      expect(owner).to have_at_most(4).items_in_collection_with_length_method
      expect(owner).to have_at_most(4).items_in_collection_with_size_method
      expect(owner).to have_at_most(4).items_in_collection_with_count_method
    end

    it "provides educational negative failure messages" do
      #given
      owner = create_collection_owner_with(3)
      length_matcher = have_at_most(3).items_in_collection_with_length_method
      size_matcher = have_at_most(3).items_in_collection_with_size_method
      count_matcher = have_at_most(3).items_in_collection_with_count_method

      #when
      length_matcher.matches?(owner)
      size_matcher.matches?(owner)
      count_matcher.matches?(owner)

      #then
      expect(length_matcher.failure_message_for_should_not).to eq <<-EOF
Isn't life confusing enough?
Instead of having to figure out the meaning of this:
  expect(actual).not_to have_at_most(3).items_in_collection_with_length_method
We recommend that you use this instead:
  expect(actual).to have_at_least(4).items_in_collection_with_length_method
EOF

      expect(size_matcher.failure_message_for_should_not).to eq <<-EOF
Isn't life confusing enough?
Instead of having to figure out the meaning of this:
  expect(actual).not_to have_at_most(3).items_in_collection_with_size_method
We recommend that you use this instead:
  expect(actual).to have_at_least(4).items_in_collection_with_size_method
EOF

      expect(count_matcher.failure_message_for_should_not).to eq <<-EOF
Isn't life confusing enough?
Instead of having to figure out the meaning of this:
  expect(actual).not_to have_at_most(3).items_in_collection_with_count_method
We recommend that you use this instead:
  expect(actual).to have_at_least(4).items_in_collection_with_count_method
EOF
    end
  end

  describe "have(n).items(args, block)" do
    it "passes args to target" do
      target = double("target")
      target.should_receive(:items).with("arg1","arg2").and_return([1,2,3])
      expect(target).to have(3).items("arg1","arg2")
    end

    it "passes block to target" do
      target = double("target")
      block = lambda { 5 }
      target.should_receive(:items).with("arg1","arg2", block).and_return([1,2,3])
      expect(target).to have(3).items("arg1","arg2", block)
    end
  end

  describe "have(n).items where target IS a collection" do
    it "references the number of items IN the collection" do
      expect([1,2,3]).to have(3).items
    end

    it "fails when the number of items IN the collection is not as expected" do
      expect {
        expect([1,2,3]).to have(7).items
      }.to fail_with("expected 7 items, got 3")
    end
  end

  describe "have(n).characters where target IS a String" do
    it "passes if the length is correct" do
      expect("this string").to have(11).characters
    end

    it "fails if the length is incorrect" do
      expect {
        expect("this string").to have(12).characters
      }.to fail_with("expected 12 characters, got 11")
    end
  end

  describe "have(n).things on an object which is not a collection nor contains one" do
    it "fails" do
      expect {
        expect(Object.new).to have(2).things
      }.to raise_error(NoMethodError) { |e|
        expect(e.name).to eq :things
      }
    end
  end

  describe RSpec::Matchers::BuiltIn::Have, "for a collection owner that implements #send" do
    before(:each) do
      @collection = Object.new
      def @collection.floozles; [1,2] end
      def @collection.send; :sent; end
    end

    it "works in the straightforward case" do
      expect(@collection).to have(2).floozles
    end

    it "works when doing automatic pluralization" do
      expect(@collection).to have_at_least(1).floozle
    end

    it "blows up when the owner doesn't respond to that method" do
      expect {
        expect(@collection).to have(99).problems
      }.to raise_error(NoMethodError, /problems/)
    end

    it 'works when #send is defined directly on an array' do
      array = [1, 2]
      def array.send; :sent; end

      expect(array).to have(2).items
    end
  end

  if RUBY_VERSION >= '2.0'
    describe RSpec::Matchers::BuiltIn::Have, "for an Enumerator whose size is nil but count is supplied" do
      let(:enumerator) { %w[a b c d].to_enum(:each) }

      it 'works fine' do
        expect(enumerator).to have(4).items
      end
    end
  end

  describe RSpec::Matchers::BuiltIn::Have do
    it "has method_missing as private" do
      expect(described_class.private_instance_methods).to include_method(:method_missing)
    end

    it "does not respond_to? method_missing (because it's private)" do
      formatter = described_class.new(0, StringIO.new)
      expect(formatter).not_to respond_to(:method_missing)
    end

    describe "respond_to?" do
      before :each do
        @have = described_class.new(:foo)
        @a_method_which_have_defines = described_class.instance_methods.first
        @a_method_which_object_defines = Object.instance_methods.first
      end

      it "is true for a method which Have defines" do
        expect(@have).to respond_to(@a_method_which_have_defines)
      end

      it "is true for a method that it's superclass (Object) defines" do
        expect(@have).to respond_to(@a_method_which_object_defines)
      end

      it "is false for a method which neither Object nor nor Have defines" do
        expect(@have).not_to respond_to(:foo_bar_baz)
      end

      it "is false if the owner doesn't respond to the method" do
        have = described_class.new(99)
        expect(have).not_to respond_to(:problems)
      end

      it "is true if the owner responds to the method" do
        have = described_class.new(:a_symbol)
        expect(have).to respond_to(:to_sym)
      end
    end
  end

  context "deprecations for the have matcher" do
    it "has the correct call site in the deprecation message" do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1)
      expect([1, 2, 3]).to have(3).items
    end

    context "when the target is a collection" do
      it "prints a specific message for the positive expectation format" do
        expectation_expression = "expect(collection).to have(3).items"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.size).to eq(3)`"

        expect_have_deprecation(expectation_expression, message)

        expect([1, 2, 3]).to have(3).items
      end

      it "prints a specific message for the negative expectation format" do
        expectation_expression = "expect(collection).not_to have(4).items"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.size).to_not eq(4)`"

        expect_have_deprecation(expectation_expression, message)

        expect([1, 2, 3]).to_not have(4).items
      end
    end

    context "when the target owns a collection" do
      class self::BagOfWords
        attr_reader :words

        def initialize(words)
          @words = words
        end
      end

      it "prints a specific message for the positive expectation format" do
        expectation_expression = "expect(collection_owner).to have(3).words"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection_owner.words.size).to eq(3)`"

        expect_have_deprecation(expectation_expression, message)

        target = BagOfWords.new(%w[foo bar baz])
        expect(target).to have(3).words
      end

      it "prints a specific message for the negative expectation format" do
        expectation_expression = "expect(collection_owner).not_to have(4).words"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection_owner.words.size).to_not eq(4)`"

        expect_have_deprecation(expectation_expression, message)

        target = BagOfWords.new(%w[foo bar baz])
        expect(target).to_not have(4).words
      end
    end

    context "when the target is a Rails record" do
      class TheModel
        attr_reader :errors

        def initialize(errors)
          @errors = {}
          @errors[:attr] = Array(errors)
        end

        def errors_on(attr, _ignore_opts = {})
          Array(@errors[attr]).flatten.compact
        end
        alias_method :error_on, :errors_on
      end

      let(:message_preamble) do
        "the rspec-collection_matchers gem " +
          "or replace your expectation with something like "
      end

      before do
        stub_const "RSpec::Rails", Module.new
      end

      it "prints a specific message for the positive expectation format" do
        expectation_expression = "expect(record).to have(2).errors_on(:attr)"
        message = message_preamble + <<-EOS.gsub(/^\s+\|/, '')
          |
          |
          |    record.valid?
          |    expect(record.errors[:attr].size).to eq(2)
          |
        EOS

        expect_have_deprecation(expectation_expression, message)

        target = TheModel.new(%w(foo bar))
        expect(target).to have(2).errors_on(:attr)
      end

      it "prints a specific message for the negative expectation format" do
        expectation_expression = "expect(record).not_to have(2).errors_on(:attr)"
        message = message_preamble + <<-EOS.gsub(/^\s+\|/, '')
          |
          |
          |    record.valid?
          |    expect(record.errors[:attr].size).to_not eq(2)
          |
        EOS

        expect_have_deprecation(expectation_expression, message)

        target = TheModel.new('foo')
        expect(target).not_to have(2).errors_on(:attr)
      end

      it "prints message for singular form: `error_on`" do
        expectation_expression = "expect(record).to have(1).error_on(:attr)"
        message = message_preamble + <<-EOS.gsub(/^\s+\|/, '')
          |
          |
          |    record.valid?
          |    expect(record.errors[:attr].size).to eq(1)
          |
        EOS

        expect_have_deprecation(expectation_expression, message)

        target = TheModel.new('foo')
        expect(target).to have(1).error_on(:attr)
      end

      it "includes a validation context when provided" do
        expectation_expression = "expect(record).to have(2).errors_on(:attr, :context => :spec)"
        message = message_preamble + <<-EOS.gsub(/^\s+\|/, '')
          |
          |
          |    record.valid?(:spec)
          |    expect(record.errors[:attr].size).to eq(2)
          |
        EOS

        expect_have_deprecation(expectation_expression, message)

        target = TheModel.new(%w(foo bar))
        options = {:context => :spec, :should_be_ignored => true}
        expect(target).to have(2).errors_on(:attr, options)
      end
    end

    context "when the target is an enumerator" do
      it "prints a specific message for the positive expectation format" do
        target = %w[a b c].to_enum(:each)

        expectation_expression = "expect(collection).to have(3).letters"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.count).to eq(3)`"

        expect_have_deprecation(expectation_expression, message)

        expect(target).to have(3).letters
      end

      it "prints a specific message for the negative expectation format" do
        target = %w[a b c].to_enum(:each)

        expectation_expression = "expect(collection).not_to have(4).letters"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.count).to_not eq(4)`"

        expect_have_deprecation(expectation_expression, message)

        expect(target).to_not have(4).letters
      end
    end
  end

  context "deprecations for the have_at_most matcher" do
    it "has the correct call site in the deprecation message" do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1)
      expect([1, 2, 3]).to have_at_most(3).items
    end

    context "when the target is a collection" do
      it "prints a specific message for the positive expectation format" do
        expectation_expression = "expect(collection).to have_at_most(3).items"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.size).to be <= 3`"

        expect_have_deprecation(expectation_expression, message)

        expect([1, 2, 3]).to have_at_most(3).items
      end

      it "prints a specific message for the negative expectation format" do
        expectation_expression = "expect(collection).not_to have_at_most(2).items"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.size).to be > 2`"

        expect_have_deprecation(expectation_expression, message)

        expect([1, 2, 3]).to_not have_at_most(2).items
      end
    end

    context "when the target owns a collection" do
      class self::BagOfWords
        attr_reader :words

        def initialize(words)
          @words = words
        end
      end

      it "prints a specific message for the positive expectation format" do
        expectation_expression = "expect(collection_owner).to have_at_most(3).words"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection_owner.words.size).to be <= 3`"

        expect_have_deprecation(expectation_expression, message)

        target = BagOfWords.new(%w[foo bar baz])
        expect(target).to have_at_most(3).words
      end

      it "prints a specific message for the negative expectation format" do
        expectation_expression = "expect(collection_owner).not_to have_at_most(2).words"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection_owner.words.size).to be > 2`"

        expect_have_deprecation(expectation_expression, message)

        target = BagOfWords.new(%w[foo bar baz])
        expect(target).to_not have_at_most(2).words
      end
    end

    context "when the target is an enumerator" do
      it "prints a specific message for the positive expectation format" do
        target = %w[a b c].to_enum(:each)

        expectation_expression = "expect(collection).to have_at_most(3).letters"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.count).to be <= 3`"

        expect_have_deprecation(expectation_expression, message)

        expect(target).to have_at_most(3).letters
      end

      it "prints a specific message for the negative expectation format" do
        target = %w[a b c].to_enum(:each)

        expectation_expression = "expect(collection).not_to have_at_most(2).letters"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.count).to be > 2`"

        expect_have_deprecation(expectation_expression, message)

        expect(target).to_not have_at_most(2).letters
      end
    end
  end

  context "deprecations for the have_at_least matcher" do
    it "has the correct call site in the deprecation message" do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1)
      expect([1, 2, 3]).to have_at_least(3).items
    end

    context "when the target is a collection" do
      it "prints a specific message for the positive expectation format" do
        expectation_expression = "expect(collection).to have_at_least(3).items"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.size).to be >= 3`"

        expect_have_deprecation(expectation_expression, message)

        expect([1, 2, 3]).to have_at_least(3).items
      end

      it "prints a specific message for the negative expectation format" do
        expectation_expression = "expect(collection).not_to have_at_least(4).items"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.size).to be < 4`"

        expect_have_deprecation(expectation_expression, message)

        expect([1, 2, 3]).to_not have_at_least(4).items
      end
    end

    context "when the target owns a collection" do
      class BagOfWords
        attr_reader :words

        def initialize(words)
          @words = words
        end
      end

      it "prints a specific message for the positive expectation format" do
        expectation_expression = "expect(collection_owner).to have_at_least(3).words"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection_owner.words.size).to be >= 3`"

        expect_have_deprecation(expectation_expression, message)

        target = BagOfWords.new(%w[foo bar baz])
        expect(target).to have_at_least(3).words
      end

      it "prints a specific message for the negative expectation format" do
        expectation_expression = "expect(collection_owner).not_to have_at_least(4).words"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection_owner.words.size).to be < 4`"

        expect_have_deprecation(expectation_expression, message)

        target = BagOfWords.new(%w[foo bar baz])
        expect(target).to_not have_at_least(4).words
      end
    end

    context "when the target is an enumerator" do
      it "prints a specific message for the positive expectation format" do
        target = %w[a b c].to_enum(:each)

        expectation_expression = "expect(collection).to have_at_least(3).letters"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.count).to be >= 3`"

        expect_have_deprecation(expectation_expression, message)

        expect(target).to have_at_least(3).letters
      end

      it "prints a specific message for the negative expectation format" do
        target = %w[a b c].to_enum(:each)

        expectation_expression = "expect(collection).not_to have_at_least(4).letters"

        message = "the rspec-collection_matchers gem " +
                  "or replace your expectation with something like " +
                  "`expect(collection.count).to be < 4`"

        expect_have_deprecation(expectation_expression, message)

        expect(target).to_not have_at_least(4).letters
      end
    end
  end
end
