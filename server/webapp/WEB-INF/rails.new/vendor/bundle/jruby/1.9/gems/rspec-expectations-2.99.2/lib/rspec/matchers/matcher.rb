require 'set'
require 'rspec/matchers/differentiate_block_method_types'

module RSpec
  module Matchers
    module DSL
      # Provides the context in which the block passed to RSpec::Matchers.define
      # will be evaluated.
      class Matcher
        include RSpec::Matchers::Extensions::InstanceEvalWithArgs
        include RSpec::Matchers::Pretty
        include RSpec::Matchers

        attr_reader :actual, :rescued_exception

        # @api private
        def initialize(name, &declarations)
          @name         = name
          @declarations = declarations
          @actual       = nil
          @diffable     = false
          @supports_block_expectations = false
          @expected_exception, @rescued_exception = nil, nil
          @match_for_should_not_block = nil
          @messages = {}
          @define_block_executed = false
          @block_method_differentiator = nil
          @deprecated_methods = Set.new
          @matcher_execution_context = nil
        end

        PERSISTENT_INSTANCE_VARIABLES = [
          :@name, :@declarations, :@diffable,
          :@supports_block_expectations,
          :@match_block, :@match_for_should_not_block,
          :@expected_exception
        ].to_set

        def expected
          if @expected.size == 1
            RSpec.warn_deprecation(
              "Custom matchers in 3.x will set expected to be a single value "+
              "(when provided as such) rather than an array. This may change "+
              "the behaviour of your matcher.\n"+
              "To continue to access this as an array use `expected_as_array`\n"+
              "Called from: #{ RSpec::CallerFilter.first_non_rspec_line }\n\n"
            )
          end
          @expected
        end

        def expected_as_array
          @expected
        end

        def matcher_execution_context=(value)
          RSpec.deprecate("`matcher_execution_context=` on custom matchers")
          @matcher_execution_context = value
        end

        def matcher_execution_context
          RSpec.deprecate("`matcher_execution_context` on custom matchers")
          @matcher_execution_context
        end

        # @api private
        def for_expected(*expected)
          @expected = expected
          dup.instance_eval do
            instance_variables.map {|ivar| ivar.intern}.each do |ivar|
              instance_variable_set(ivar, nil) unless (PERSISTENT_INSTANCE_VARIABLES + [:@expected]).include?(ivar)
            end
            @messages = {}
            @deprecated_methods = Set.new

            @block_method_differentiator = DifferentiateBlockMethodTypes.new(*@expected, &@declarations)
            making_declared_methods_public do
              instance_eval_with_args(*@expected, &@declarations)
            end

            @define_block_executed = true
            self
          end
        end

        # @api private
        # Used internally by +should+ and +should_not+.
        def matches?(actual)
          @actual = actual
          if @expected_exception
            begin
              instance_eval_with_args(actual, &@match_block)
              true
            rescue @expected_exception => @rescued_exception
              false
            end
          else
            begin
              instance_eval_with_args(actual, &@match_block)
            rescue RSpec::Expectations::ExpectationNotMetError
              false
            end
          end
        end

        # Stores the block that is used to determine whether this matcher passes
        # or fails. The block should return a boolean value. When the matcher is
        # passed to `should` and the block returns `true`, then the expectation
        # passes. Similarly, when the matcher is passed to `should_not` and the
        # block returns `false`, then the expectation passes.
        #
        # Use `match_for_should` when used in conjuntion with
        # `match_for_should_not`.
        #
        # @example
        #
        #     RSpec::Matchers.define :be_even do
        #       match do |actual|
        #         actual.even?
        #       end
        #     end
        #
        #     4.should be_even     # passes
        #     3.should_not be_even # passes
        #     3.should be_even     # fails
        #     4.should_not be_even # fails
        #
        # @yield [Object] actual the actual value (or receiver of should)
        def match(&block)
          @match_block = block
        end

        alias_method :match_for_should, :match

        # Use this to define the block for a negative expectation (`should_not`)
        # when the positive and negative forms require different handling. This
        # is rarely necessary, but can be helpful, for example, when specifying
        # asynchronous processes that require different timeouts.
        #
        # @yield [Object] actual the actual value (or receiver of should)
        def match_for_should_not(&block)
          @match_for_should_not_block = block
        end

        # Use this instead of `match` when the block will raise an exception
        # rather than returning false to indicate a failure.
        #
        # @example
        #
        #     RSpec::Matchers.define :accept_as_valid do |candidate_address|
        #       match_unless_raises ValidationException do |validator|
        #         validator.validate(candidate_address)
        #       end
        #     end
        #
        #     email_validator.should accept_as_valid("person@company.com")
        def match_unless_raises(exception=Exception, &block)
          @expected_exception = exception
          match(&block)
        end

        # Customize the failure messsage to use when this matcher is invoked with
        # `should`. Only use this when the message generated by default doesn't
        # suit your needs.
        #
        # @example
        #
        #     RSpec::Matchers.define :have_strength do |expected|
        #       match { ... }
        #
        #       failure_message_for_should do |actual|
        #         "Expected strength of #{expected}, but had #{actual.strength}"
        #       end
        #     end
        #
        # @yield [Object] actual the actual object
        def failure_message_for_should(&block)
          cache_or_call_cached(:failure_message_for_should, &block)
        end

        # Customize the failure messsage to use when this matcher is invoked with
        # `should_not`. Only use this when the message generated by default
        # doesn't suit your needs.
        #
        # @example
        #
        #     RSpec::Matchers.define :have_strength do |expected|
        #       match { ... }
        #
        #       failure_message_for_should_not do |actual|
        #         "Expected not to have strength of #{expected}, but did"
        #       end
        #     end
        #
        # @yield [Object] actual the actual object
        # @yield [Object] actual the actual object
        def failure_message_for_should_not(&block)
          cache_or_call_cached(:failure_message_for_should_not, &block)
        end


        # Customize the description to use for one-liners.  Only use this when
        # the description generated by default doesn't suit your needs.
        #
        # @example
        #
        #     RSpec::Matchers.define :qualify_for do |expected|
        #       match { ... }
        #
        #       description do
        #         "qualify for #{expected}"
        #       end
        #     end
        def description(&block)
          cache_or_call_cached(:description, &block)
        end

        # Tells the matcher to diff the actual and expected values in the failure
        # message.
        def diffable
          @diffable = true
        end

        def supports_block_expectations
          @supports_block_expectations = true
        end

        # Convenience for defining methods on this matcher to create a fluent
        # interface. The trick about fluent interfaces is that each method must
        # return self in order to chain methods together. `chain` handles that
        # for you.
        #
        # @example
        #
        #     RSpec::Matchers.define :have_errors_on do |key|
        #       chain :with do |message|
        #         @message = message
        #       end
        #
        #       match do |actual|
        #         actual.errors[key] == @message
        #       end
        #     end
        #
        #     minor.should have_errors_on(:age).with("Not old enough to participate")
        def chain(method, &block)
          define_method method do |*args|
            block.call(*args)
            self
          end
        end

        # @api private
        # Used internally by objects returns by +should+ and +should_not+.
        def diffable?
          @diffable
        end

        # @api private
        def supports_block_expectations?
          @supports_block_expectations
        end

        # @api private
        # Used internally by +should_not+
        def does_not_match?(actual)
          @actual = actual
          @match_for_should_not_block ?
            instance_eval_with_args(actual, &@match_for_should_not_block) :
            !matches?(actual)
        end

        def respond_to?(method, include_private=false)
          super || @matcher_execution_context.respond_to?(method, include_private)
        end

        private

        def method_missing(method, *args, &block)
          if @matcher_execution_context.respond_to?(method)
            @matcher_execution_context.__send__ method, *args, &block
          else
            super(method, *args, &block)
          end
        end

        def include(*modules)
          return_value = singleton_class.__send__(:include, *modules)

          modules.each do |mod|
            mod.instance_methods.each do |name|
              add_deprecation_warning_to(name,
                "Calling a helper method (`#{name}`) from a module included in a custom matcher as a macro",
                "`extend #{mod.name || "TheModule"}`",
                "included in the custom matcher",
                :unless
              )
            end
          end

          return_value
        end

        def extend(*modules)
          return_value = super

          modules.each do |mod|
            mod.instance_methods.each do |name|
              add_deprecation_warning_to(name,
                "Calling a helper method (`#{name}`) from a module extended onto a custom matcher",
                "`include #{mod.name || "TheModule"}`",
                "extended onto the custom matcher",
                :if
              )
            end
          end unless @define_block_executed

          return_value
        end

        def define_method(name, &block)
          singleton_class.__send__(:define_method, name, &block)
        end

        def making_declared_methods_public
          # Our home-grown instance_exec in ruby 1.8.6 results in any methods
          # declared in the block eval'd by instance_exec in the block to which we
          # are yielding here are scoped private. This is NOT the case for Ruby
          # 1.8.7 or 1.9.
          #
          # Also, due some crazy scoping that I don't understand, these methods
          # are actually available in the specs (something about the matcher being
          # defined in the scope of RSpec::Matchers or within an example), so not
          # doing the following will not cause specs to fail, but they *will*
          # cause features to fail and that will make users unhappy. So don't.
          orig_private_methods = private_methods
          yield
          (private_methods - orig_private_methods).each {|m| singleton_class.__send__ :public, m}
        end

        def cache_or_call_cached(key, &block)
          block ? cache(key, &block) : call_cached(key)
        end

        def cache(key, &block)
          @messages[key] = block
        end

        def call_cached(key)
          if @messages.has_key?(key)
            @messages[key].arity == 1 ? @messages[key].call(@actual) : @messages[key].call
          else
            __send__("default_#{key}")
          end
        end

        def default_description
          "#{name_to_sentence}#{to_sentence expected_as_array}"
        end

        def default_failure_message_for_should
          "expected #{actual.inspect} to #{name_to_sentence}#{to_sentence expected_as_array}"
        end

        def default_failure_message_for_should_not
          "expected #{actual.inspect} not to #{name_to_sentence}#{to_sentence expected_as_array}"
        end

        unless method_defined?(:singleton_class)
          def singleton_class
            class << self; self; end
          end
        end

        def singleton_method_added(name)
          return unless @block_method_differentiator

          if @block_method_differentiator.instance_methods.include?(name)
            add_deprecation_warning_to(name,
              "Calling a helper method (`#{name}`) defined as an instance method (using `def #{name}`) as a macro from a custom matcher `define` block",
              "`def self.#{name}` (to define it as a singleton method)",
              "defined in the custom matcher definition block",
              :unless
            )
          elsif @block_method_differentiator.singleton_methods.include?(name)
            add_deprecation_warning_to(name,
              "Calling a helper method (`#{name}`) defined as a singleton method (using `def self.#{name}`) on a custom matcher",
              "`def #{name}` (to define it as an instance method)",
              "defined in the custom matcher definition block",
              :if
            )
          end
        end

        def add_deprecation_warning_to(method_name, msg, replacement, extra_call_site_msg, condition)
          return if @deprecated_methods.include?(method_name)
          @deprecated_methods << method_name

          aliased_name = aliased_name_for(method_name)
          singleton_class.__send__(:alias_method, aliased_name, method_name)

          singleton_class.class_eval(<<-EOS, __FILE__, __LINE__ + 1)
            def #{method_name}(*a, &b)
              ::RSpec.deprecate(#{msg.inspect},
                :replacement => #{replacement.inspect},
                :call_site => CallerFilter.first_non_rspec_line + " and #{extra_call_site_msg} at #{CallerFilter.first_non_rspec_line}"
              ) #{condition} @define_block_executed

              __send__(#{aliased_name.inspect}, *a, &b)
            end
          EOS
        end

        def aliased_name_for(method_name)
          target, punctuation = method_name.to_s.sub(/([?!=])$/, ''), $1
          "#{target}_without_rspec_deprecation_warning#{punctuation}"
        end
      end
    end
  end
end
