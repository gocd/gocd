module RSpec
  module CollectionMatchers
    class Have
      include RSpec::Matchers::Composable unless RSpec::Expectations::Version::STRING.to_f < 3.0

      QUERY_METHODS = [:size, :length, :count].freeze
      IGNORED_CLASSES = [Integer].freeze

      def initialize(expected, relativity=:exactly)
        @expected = case expected
                    when :no then 0
                    when String then expected.to_i
                    else expected
                    end
        @relativity = relativity
        @actual = @collection_name = @plural_collection_name = nil
      end

      def relativities
        @relativities ||= {
          :exactly => "",
          :at_least => "at least ",
          :at_most => "at most "
        }
      end

      if RUBY_VERSION == '1.9.2'
        # On Ruby 1.9.2 items that don't return an array for `to_ary`
        # can't be flattened in arrays, we need to be able to do this
        # to produce diffs for compound matchers, so this corrects the
        # default implementation. Note that rspec-support has code that
        # directly checks for pattern and prevents infinite recursion.
        def to_ary
          [self]
        end
      end

      def matches?(collection_or_owner)
        collection = determine_collection(collection_or_owner)
        case collection
        when enumerator_class
          for query_method in QUERY_METHODS
            next unless collection.respond_to?(query_method)
            @actual = collection.__send__(query_method)
            break unless @actual.nil?
          end
          raise not_a_collection if @actual.nil?
        else
          query_method = determine_query_method(collection)
          raise not_a_collection if !query_method || is_ignored_class?(collection)
          @actual = collection.__send__(query_method)
        end
        case @relativity
        when :at_least then @actual >= @expected
        when :at_most  then @actual <= @expected
        else                @actual == @expected
        end
      end
      alias == matches?

      def determine_collection(collection_or_owner)
        if collection_or_owner.respond_to?(@collection_name)
          collection_or_owner.__send__(@collection_name, *@args, &@block)
        elsif (@plural_collection_name && collection_or_owner.respond_to?(@plural_collection_name))
          collection_or_owner.__send__(@plural_collection_name, *@args, &@block)
        elsif determine_query_method(collection_or_owner)
          collection_or_owner
        else
          collection_or_owner.__send__(@collection_name, *@args, &@block)
        end
      end

      def determine_query_method(collection)
        QUERY_METHODS.detect {|m| collection.respond_to?(m)}
      end

      def is_ignored_class?(collection)
        IGNORED_CLASSES.any? {|klass| klass === collection}
      end

      def not_a_collection
        "expected #{@collection_name} to be a collection but it does not respond to #length, #size or #count"
      end

      def failure_message
        return errors_on_message(:expected, ", got #{@actual}") if is_errors_on?
        "expected #{relative_expectation} #{@collection_name}, got #{@actual}"
      end
      alias failure_message_for_should failure_message

      def failure_message_when_negated
        if @relativity == :exactly
          return "expected target not to have #{@expected} #{@collection_name}, got #{@actual}"
        elsif @relativity == :at_most
          return <<-EOF
Isn't life confusing enough?
Instead of having to figure out the meaning of this:
  #{Syntax.negative_expression("actual", "have_at_most(#{@expected}).#{@collection_name}")}
We recommend that you use this instead:
  #{Syntax.positive_expression("actual", "have_at_least(#{@expected + 1}).#{@collection_name}")}
EOF
        elsif @relativity == :at_least
          return <<-EOF
Isn't life confusing enough?
Instead of having to figure out the meaning of this:
  #{Syntax.negative_expression("actual", "have_at_least(#{@expected}).#{@collection_name}")}
We recommend that you use this instead:
  #{Syntax.positive_expression("actual", "have_at_most(#{@expected - 1}).#{@collection_name}")}
EOF
        end
      end
      alias failure_message_for_should_not failure_message_when_negated

      def description
        return errors_on_message(:have) if is_errors_on?
        "have #{relative_expectation} #{@collection_name}"
      end

      def respond_to?(m, include_all = false)
        @expected.respond_to?(m, include_all) || super
      end

      private

      def method_missing(method, *args, &block)
        @collection_name = method
        if inflector = (defined?(ActiveSupport::Inflector) && ActiveSupport::Inflector.respond_to?(:pluralize) ? ActiveSupport::Inflector : (defined?(Inflector) ? Inflector : nil))
          @plural_collection_name = inflector.pluralize(method.to_s)
        end
        @args = args
        @block = block
        self
      end

      def relative_expectation
        "#{relativities[@relativity]}#{@expected}"
      end

      def enumerator_class
        RUBY_VERSION < '1.9' ? Enumerable::Enumerator : Enumerator
      end

      def is_errors_on?
        [:errors_on, :error_on].include? @collection_name
      end

      def errors_on_message(prefix, suffix = nil)
        "#{prefix} #{relative_expectation} #{@collection_name.to_s.gsub('_', ' ')} :#{@args[0]}#{suffix}"
      end
    end

    module Syntax
      # @api private
      # Generates a positive expectation expression.
      def self.positive_expression(target_expression, matcher_expression)
        expression_generator.positive_expression(target_expression, matcher_expression)
      end

      # @api private
      # Generates a negative expectation expression.
      def self.negative_expression(target_expression, matcher_expression)
        expression_generator.negative_expression(target_expression, matcher_expression)
      end

      # @api private
      # Selects which expression generator to use based on the configured syntax.
      def self.expression_generator
        if RSpec::Expectations::Syntax.expect_enabled?
          ExpectExpressionGenerator
        else
          ShouldExpressionGenerator
        end
      end

      # @api private
      # Generates expectation expressions for the `should` syntax.
      module ShouldExpressionGenerator
        def self.positive_expression(target_expression, matcher_expression)
          "#{target_expression}.should #{matcher_expression}"
        end

        def self.negative_expression(target_expression, matcher_expression)
          "#{target_expression}.should_not #{matcher_expression}"
        end
      end

      # @api private
      # Generates expectation expressions for the `expect` syntax.
      module ExpectExpressionGenerator
        def self.positive_expression(target_expression, matcher_expression)
          "expect(#{target_expression}).to #{matcher_expression}"
        end

        def self.negative_expression(target_expression, matcher_expression)
          "expect(#{target_expression}).not_to #{matcher_expression}"
        end
      end
    end
  end
end
