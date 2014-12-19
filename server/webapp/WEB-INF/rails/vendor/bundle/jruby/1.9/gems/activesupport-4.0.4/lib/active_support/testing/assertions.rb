require 'active_support/core_ext/object/blank'

module ActiveSupport
  module Testing
    module Assertions
      # Assert that an expression is not truthy. Passes if <tt>object</tt> is
      # +nil+ or +false+. "Truthy" means "considered true in a conditional"
      # like <tt>if foo</tt>.
      #
      #   assert_not nil    # => true
      #   assert_not false  # => true
      #   assert_not 'foo'  # => 'foo' is not nil or false
      #
      # An error message can be specified.
      #
      #   assert_not foo, 'foo should be false'
      def assert_not(object, message = nil)
        message ||= "Expected #{mu_pp(object)} to be nil or false"
        assert !object, message
      end

      # Test numeric difference between the return value of an expression as a
      # result of what is evaluated in the yielded block.
      #
      #   assert_difference 'Article.count' do
      #     post :create, article: {...}
      #   end
      #
      # An arbitrary expression is passed in and evaluated.
      #
      #   assert_difference 'assigns(:article).comments(:reload).size' do
      #     post :create, comment: {...}
      #   end
      #
      # An arbitrary positive or negative difference can be specified.
      # The default is <tt>1</tt>.
      #
      #   assert_difference 'Article.count', -1 do
      #     post :delete, id: ...
      #   end
      #
      # An array of expressions can also be passed in and evaluated.
      #
      #   assert_difference [ 'Article.count', 'Post.count' ], 2 do
      #     post :create, article: {...}
      #   end
      #
      # A lambda or a list of lambdas can be passed in and evaluated:
      #
      #   assert_difference ->{ Article.count }, 2 do
      #     post :create, article: {...}
      #   end
      #
      #   assert_difference [->{ Article.count }, ->{ Post.count }], 2 do
      #     post :create, article: {...}
      #   end
      #
      # An error message can be specified.
      #
      #   assert_difference 'Article.count', -1, 'An Article should be destroyed' do
      #     post :delete, id: ...
      #   end
      def assert_difference(expression, difference = 1, message = nil, &block)
        expressions = Array(expression)

        exps = expressions.map { |e|
          e.respond_to?(:call) ? e : lambda { eval(e, block.binding) }
        }
        before = exps.map { |e| e.call }

        yield

        expressions.zip(exps).each_with_index do |(code, e), i|
          error  = "#{code.inspect} didn't change by #{difference}"
          error  = "#{message}.\n#{error}" if message
          assert_equal(before[i] + difference, e.call, error)
        end
      end

      # Assertion that the numeric result of evaluating an expression is not
      # changed before and after invoking the passed in block.
      #
      #   assert_no_difference 'Article.count' do
      #     post :create, article: invalid_attributes
      #   end
      #
      # An error message can be specified.
      #
      #   assert_no_difference 'Article.count', 'An Article should not be created' do
      #     post :create, article: invalid_attributes
      #   end
      def assert_no_difference(expression, message = nil, &block)
        assert_difference expression, 0, message, &block
      end

      # Test if an expression is blank. Passes if <tt>object.blank?</tt>
      # is +true+.
      #
      #   assert_blank []   # => true
      #   assert_blank [[]] # => [[]] is not blank
      #
      # An error message can be specified.
      #
      #   assert_blank [], 'this should be blank'
      def assert_blank(object, message=nil)
        ActiveSupport::Deprecation.warn('"assert_blank" is deprecated. Please use "assert object.blank?" instead')
        message ||= "#{object.inspect} is not blank"
        assert object.blank?, message
      end

      # Test if an expression is not blank. Passes if <tt>object.present?</tt>
      # is +true+.
      #
      #   assert_present({ data: 'x' }) # => true
      #   assert_present({})            # => {} is blank
      #
      # An error message can be specified.
      #
      #   assert_present({ data: 'x' }, 'this should not be blank')
      def assert_present(object, message=nil)
        ActiveSupport::Deprecation.warn('"assert_present" is deprecated. Please use "assert object.present?" instead')
        message ||= "#{object.inspect} is blank"
        assert object.present?, message
      end
    end
  end
end
