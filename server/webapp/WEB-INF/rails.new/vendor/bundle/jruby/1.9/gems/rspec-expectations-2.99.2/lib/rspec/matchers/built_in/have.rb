module RSpec
  module Matchers
    module BuiltIn
      class Have
        include MatchAliases

        QUERY_METHODS = [:size, :length, :count].freeze

        def initialize(expected, relativity=:exactly)
          @expected = case expected
                      when :no then 0
                      when String then expected.to_i
                      else expected
                      end
          @relativity = relativity

          @actual = @collection_name = @plural_collection_name = nil
          @target_owns_a_collection = false
          @negative_expectation = false
          @expectation_format_method = "to"
        end

        def relativities
          @relativities ||= {
            :exactly => "",
            :at_least => "at least ",
            :at_most => "at most "
          }
        end

        def matches?(collection_or_owner)
          collection = determine_collection(collection_or_owner)
          case collection
          when enumerator_class
            for query_method in QUERY_METHODS
              next unless collection.respond_to?(query_method)
              @actual = collection.__send__(query_method)

              if @actual
                print_deprecation_message(query_method)
                break
              end
            end

            raise not_a_collection if @actual.nil?
          else
            query_method = determine_query_method(collection)
            raise not_a_collection unless query_method
            @actual = collection.__send__(query_method)

            print_deprecation_message(query_method)
          end
          case @relativity
          when :at_least then @actual >= @expected
          when :at_most  then @actual <= @expected
          else                @actual == @expected
          end
        end

        def does_not_match?(collection_or_owner)
          @negative_expectation = true
          @expectation_format_method = "to_not"
          !matches?(collection_or_owner)
        end

        def determine_collection(collection_or_owner)
          if collection_or_owner.respond_to?(@collection_name)
            @target_owns_a_collection = true
            collection_or_owner.__send__(@collection_name, *@args, &@block)
          elsif (@plural_collection_name && collection_or_owner.respond_to?(@plural_collection_name))
            @target_owns_a_collection = true
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

        def not_a_collection
          "expected #{@collection_name} to be a collection but it does not respond to #length, #size or #count"
        end

        def failure_message_for_should
          "expected #{relative_expectation} #{@collection_name}, got #{@actual}"
        end

        def failure_message_for_should_not
          if @relativity == :exactly
            return "expected target not to have #{@expected} #{@collection_name}, got #{@actual}"
          elsif @relativity == :at_most
            return <<-EOF
Isn't life confusing enough?
Instead of having to figure out the meaning of this:
  #{Expectations::Syntax.negative_expression("actual", "have_at_most(#{@expected}).#{@collection_name}")}
We recommend that you use this instead:
  #{Expectations::Syntax.positive_expression("actual", "have_at_least(#{@expected + 1}).#{@collection_name}")}
EOF
          elsif @relativity == :at_least
            return <<-EOF
Isn't life confusing enough?
Instead of having to figure out the meaning of this:
  #{Expectations::Syntax.negative_expression("actual", "have_at_least(#{@expected}).#{@collection_name}")}
We recommend that you use this instead:
  #{Expectations::Syntax.positive_expression("actual", "have_at_most(#{@expected - 1}).#{@collection_name}")}
EOF
          end
        end

        def description
          "have #{relative_expectation} #{@collection_name}"
        end

        def respond_to?(m)
          @expected.respond_to?(m) || super
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

        def print_deprecation_message(query_method)
          deprecation_message = "the rspec-collection_matchers gem "
          deprecation_message << "or replace your expectation with something like "
          if for_rspec_rails_error_on?
            # It is supposed to be safe to be able to convert the args array to
            # a string. This is because the `errors_on` method only takes two
            # valid arguments: attribute name (symbol/string) and a hash
            deprecated_call = expectation_expression(query_method, "record")
            deprecated_call << "(#{errors_on_args_list})" unless @args.empty?

            deprecation_message << <<-EOS.gsub(/^\s+\|/, '')
              |
              |
              |    #{record_valid_expression}
              |    expect(#{record_errors_expression(query_method)}).#{expectation_format_method} #{suggested_matcher_expression}
              |
            EOS
          else
            deprecated_call = expectation_expression(query_method)
            deprecation_message << "`expect(#{cardinality_expression(query_method)}).#{expectation_format_method} #{suggested_matcher_expression}`"
          end

          RSpec.deprecate("`#{deprecated_call}`",
            :replacement => deprecation_message,
            :type        => "the have matcher"
          )
        end

        def expectation_expression(query_method, target = nil)
          target ||= target_expression
          if @negative_expectation
            RSpec::Expectations::Syntax.negative_expression(target, original_matcher_expression)
          else
            RSpec::Expectations::Syntax.positive_expression(target, original_matcher_expression)
          end
        end

        def target_expression
          if @target_owns_a_collection
            'collection_owner'
          else
            'collection'
          end
        end

        def original_matcher_expression
          "#{matcher_method}(#{@expected}).#{@collection_name}"
        end

        def expectation_format_method
          if @relativity == :exactly
            @expectation_format_method
          else
            "to"
          end
        end

        def cardinality_expression(query_method)
          expression = "#{target_expression}."
          expression << "#{@collection_name}." if @target_owns_a_collection
          expression << String(query_method)
        end

        def suggested_matcher_expression
          send("suggested_matcher_expression_for_#{@relativity}")
        end

        def suggested_matcher_expression_for_exactly
          "eq(#{@expected})"
        end

        def suggested_matcher_expression_for_at_most
          if @negative_expectation
            "be > #{@expected}"
          else
            "be <= #{@expected}"
          end
        end

        def suggested_matcher_expression_for_at_least
          if @negative_expectation
            "be < #{@expected}"
          else
            "be >= #{@expected}"
          end
        end

        def matcher_method
          case @relativity
          when :exactly
            "have"
          when :at_most
            "have_at_most"
          when :at_least
            "have_at_least"
          end
        end

        # RSpec Rails `errors_on` specific helpers
        def for_rspec_rails_error_on?
          defined?(RSpec::Rails) &&
            /\.errors?_on\b/ =~ original_matcher_expression
        end

        def errors_on_args_list
          list = @args.first.inspect
          context = validation_context
          list << ", :context => #{context}" if context
          list
        end

        def record_valid_expression
          expression = "record.valid?"
          if on_context = validation_context
            expression << "(#{on_context})"
          end
          expression
        end

        def validation_context
          return unless Hash === @args.last
          @args.last[:context].inspect
        end

        def record_errors_expression(query_method)
          attribute = (@args.first || :attr)
          "record.errors[#{attribute.inspect}].#{String(query_method)}"
        end
      end
    end
  end
end
