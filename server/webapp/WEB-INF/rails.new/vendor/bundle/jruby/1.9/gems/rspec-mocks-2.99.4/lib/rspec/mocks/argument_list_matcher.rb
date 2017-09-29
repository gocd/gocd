require 'rspec/mocks/argument_matchers'

module RSpec
  module Mocks
    # Wrapper for matching arguments against a list of expected values. Used by
    # the `with` method on a `MessageExpectation`:
    #
    #     object.should_receive(:message).with(:a, 'b', 3)
    #     object.message(:a, 'b', 3)
    #
    # Values passed to `with` can be literal values or argument matchers that
    # match against the real objects .e.g.
    #
    #     object.should_receive(:message).with(hash_including(:a => 'b'))
    #
    # Can also be used directly to match the contents of any `Array`. This
    # enables 3rd party mocking libs to take advantage of rspec's argument
    # matching without using the rest of rspec-mocks.
    #
    #     require 'rspec/mocks/argument_list_matcher'
    #     include RSpec::Mocks::ArgumentMatchers
    #
    #     arg_list_matcher = RSpec::Mocks::ArgumentListMatcher.new(123, hash_including(:a => 'b'))
    #     arg_list_matcher.args_match?(123, :a => 'b')
    #
    # @see ArgumentMatchers
    class ArgumentListMatcher
      # @private
      attr_reader :expected_args

      # @api public
      # @param [Array] *expected_args a list of expected literals and/or argument matchers
      # @param [Block] block a block with arity matching the expected
      #
      # Initializes an `ArgumentListMatcher` with a collection of literal
      # values and/or argument matchers, or a block that handles the evaluation
      # for you.
      #
      # @see ArgumentMatchers
      # @see #args_match?
      def initialize(*expected_args, &block)
        @expected_args = expected_args
        @block = expected_args.empty? ? block : nil
        @match_any_args = false
        @matchers = nil

        case expected_args.first
        when ArgumentMatchers::AnyArgsMatcher
          @match_any_args = true
        when ArgumentMatchers::NoArgsMatcher
          @matchers = []
        else
          @matchers = expected_args.collect {|arg| matcher_for(arg)}
        end
      end

      # @api public
      # @param [Array] *args
      #
      # Matches each element in the `expected_args` against the element in the same
      # position of the arguments passed to `new`.
      #
      # @see #initialize
      def args_match?(*args)
        match_any_args? || block_passes?(*args) || matchers_match?(*args)
      end

      private

      def matcher_for(arg)
        return ArgumentMatchers::MatcherMatcher.new(arg) if is_matcher?(arg)
        return ArgumentMatchers::RegexpMatcher.new(arg)  if arg.is_a?(Regexp)
        return ArgumentMatchers::EqualityProxy.new(arg)
      end

      def is_matcher?(object)
        return false if object.respond_to?(:i_respond_to_everything_so_im_not_really_a_matcher)

        [:failure_message_for_should, :failure_message].any? do |msg|
          object.respond_to?(msg)
        end && object.respond_to?(:matches?)
      end

      def block_passes?(*args)
        @block.call(*args) if @block
      end

      def matchers_match?(*args)
        @matchers == args
      end

      def match_any_args?
        @match_any_args
      end
    end
  end
end
