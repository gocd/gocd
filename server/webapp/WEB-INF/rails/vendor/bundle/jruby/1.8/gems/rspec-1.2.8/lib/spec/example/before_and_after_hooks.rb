module Spec
  module Example
    module BeforeAndAfterHooks
      class << self
        def before_suite_parts # :nodoc:
          @before_suite_parts ||= []
        end

        def after_suite_parts # :nodoc:
          @after_suite_parts ||= []
        end
      end

      # Registers a block to be executed before examples. <tt>scope</tt> can be
      # <tt>:each</tt> (default), <tt>:all</tt>, or <tt>:suite</tt>. When
      # <tt>:each</tt>, the block is executed before each example. When
      # <tt>:all</tt>, the block is executed only once before any examples are
      # run.
      def append_before(scope = :each, &block)
        before_parts(scope) << block
      end
      alias_method :before, :append_before

      # Registers a block to be executed before each example.
      # This method prepends +block+ to existing before blocks.
      #
      # See <tt>append_before</tt> for scoping semantics.
      def prepend_before(scope = :each, &block)
        before_parts(scope).unshift(block)
      end

      # Registers a block to be executed after each example.
      # This method prepends +block+ to existing after blocks.
      #
      # See <tt>append_before</tt> for scoping semantics.
      def prepend_after(scope = :each, &block)
        after_parts(scope).unshift(block)
      end
      alias_method :after, :prepend_after

      # Registers a block to be executed after each example.
      # This method appends +block+ to existing after blocks.
      #
      # See <tt>append_before</tt> for scoping semantics.
      def append_after(scope = :each, &block)
        after_parts(scope) << block
      end

      def before_each_parts # :nodoc:
        @before_each_parts ||= []
      end

      def after_each_parts # :nodoc:
        @after_each_parts ||= []
      end

      def before_all_parts # :nodoc:
        @before_all_parts ||= []
      end

      def after_all_parts # :nodoc:
        @after_all_parts ||= []
      end

      def before_suite_parts # :nodoc:
        BeforeAndAfterHooks.before_suite_parts
      end

      def after_suite_parts # :nodoc:
        BeforeAndAfterHooks.after_suite_parts
      end

    private

      def before_parts(scope)
        case scope
        when :each; before_each_parts
        when :all; before_all_parts
        when :suite; before_suite_parts
        end
      end

      def after_parts(scope)
        case scope
        when :each; after_each_parts
        when :all; after_all_parts
        when :suite; after_suite_parts
        end
      end

    end
  end
end
