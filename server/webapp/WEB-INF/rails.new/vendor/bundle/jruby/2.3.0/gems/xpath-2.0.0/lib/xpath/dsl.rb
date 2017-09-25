module XPath
  module DSL
    module TopLevel
      def current
        Expression.new(:this_node)
      end

      def name
        Expression.new(:node_name, current)
      end

      def descendant(*expressions)
        Expression.new(:descendant, current, expressions)
      end

      def child(*expressions)
        Expression.new(:child, current, expressions)
      end

      def axis(name, tag_name=:*)
        Expression.new(:axis, current, name, tag_name)
      end

      def next_sibling(*expressions)
        Expression.new(:next_sibling, current, expressions)
      end

      def previous_sibling(*expressions)
        Expression.new(:previous_sibling, current, expressions)
      end

      def anywhere(*expressions)
        Expression.new(:anywhere, expressions)
      end

      def attr(expression)
        Expression.new(:attribute, current, expression)
      end

      def contains(expression)
        Expression.new(:contains, current, expression)
      end

      def starts_with(expression)
        Expression.new(:starts_with, current, expression)
      end

      def text
        Expression.new(:text, current)
      end

      def string
        Expression.new(:string_function, current)
      end

      def css(selector)
        Expression.new(:css, current, Literal.new(selector))
      end
    end

    module ExpressionLevel
      include XPath::DSL::TopLevel

      def where(expression)
        Expression.new(:where, current, expression)
      end
      alias_method :[], :where

      def one_of(*expressions)
        Expression.new(:one_of, current, expressions)
      end

      def equals(expression)
        Expression.new(:equality, current, expression)
      end
      alias_method :==, :equals

      def is(expression)
        Expression.new(:is, current, expression)
      end

      def or(expression)
        Expression.new(:or, current, expression)
      end
      alias_method :|, :or

      def and(expression)
        Expression.new(:and, current, expression)
      end
      alias_method :&, :and

      def union(*expressions)
        Union.new(*[self, expressions].flatten)
      end
      alias_method :+, :union

      def inverse
        Expression.new(:inverse, current)
      end
      alias_method :~, :inverse

      def string_literal
        Expression.new(:string_literal, self)
      end

      def normalize
        Expression.new(:normalized_space, current)
      end
      alias_method :n, :normalize
    end
  end
end
