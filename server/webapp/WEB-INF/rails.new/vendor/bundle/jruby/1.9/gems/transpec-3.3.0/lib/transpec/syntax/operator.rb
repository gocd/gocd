# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/send'
require 'transpec/syntax/mixin/owned_matcher'
require 'transpec/util'
require 'ast'

module Transpec
  class Syntax
    class Operator < Syntax
      extend ::AST::Sexp
      include Mixin::Send, Mixin::OwnedMatcher, Util

      OPERATORS = [:==, :===, :<, :<=, :>, :>=, :=~].freeze
      BE_NODE = s(:send, nil, :be)

      define_dynamic_analysis do |rewriter|
        if method_name == :=~
          # https://github.com/rspec/rspec-expectations/blob/v2.14.5/lib/rspec/matchers.rb#L692
          # https://github.com/rspec/rspec-rails/blob/v2.14.2/lib/rspec/rails/matchers/relation_match_array.rb
          code = 'is_a?(Enumerable) || ' \
                 'self.class.ancestors.any? { |klass| klass.name == "ActiveRecord::Relation" }'
          rewriter.register_request(arg_node, :using_match_array?, code)
        end
      end

      def initialize(*args)
        node = args.shift

        operator_node = if node == BE_NODE
                          node.parent
                        else
                          node
                        end

        super(operator_node, *args)
      end

      def dynamic_analysis_target?
        super && receiver_node && OPERATORS.include?(method_name)
      end

      def convert_operator!(parenthesize_arg = true)
        case method_name
        when :==
          convert_to_eq!(parenthesize_arg)
        when :=~
          convert_to_match!(parenthesize_arg)
        else
          convert_to_be_operator!
        end
      end

      def parenthesize!(always = true)
        return if contain_here_document?(arg_node)

        left_of_arg_source = range_in_between_selector_and_arg.source

        if left_of_arg_source.match(/\A *\Z/)
          parenthesize_single_line!(always)
        elsif left_of_arg_source.match(/\n|\r/)
          parenthesize_multi_line!(Regexp.last_match(0))
        end
      end

      private

      def convert_to_eq!(parenthesize_arg)
        handle_anterior_of_operator!
        replace(selector_range, 'eq')
        parenthesize!(parenthesize_arg)
        add_record(nil, 'eq(expected)')
      end

      def convert_to_be_operator!
        return if prefixed_with_be?
        insert_before(selector_range, 'be ')
        add_record(nil, "be #{method_name} expected")
      end

      def convert_to_match!(parenthesize_arg)
        handle_anterior_of_operator!

        if using_match_array?
          replace(selector_range, 'match_array')
        else
          replace(selector_range, 'match')
        end

        parenthesize!(parenthesize_arg)

        accurate = !using_match_array?.nil?

        # Need to register record after all source rewrites are done
        # to avoid false record when failed with overlapped rewrite.
        if using_match_array?
          add_record('=~ [1, 2]', 'match_array([1, 2])', accurate)
        else
          add_record('=~ /pattern/', 'match(/pattern/)', accurate)
        end
      end

      def handle_anterior_of_operator!
        if prefixed_with_be?
          remove_be!
        elsif range_in_between_receiver_and_selector.source.empty?
          insert_before(selector_range, ' ')
        end
      end

      def using_match_array?
        case arg_node.type
        when :array
          true
        when :regexp
          false
        else
          runtime_data[arg_node, :using_match_array?]
        end
      end

      def parenthesize_single_line!(always)
        if in_explicit_parentheses?(arg_node)
          remove(range_in_between_selector_and_arg)
        elsif always || arg_node.hash_type?
          replace(range_in_between_selector_and_arg, '(')
          insert_after(expression_range, ')')
        elsif range_in_between_selector_and_arg.source.empty?
          insert_after(selector_range, ' ')
        end
      end

      def parenthesize_multi_line!(linefeed)
        insert_before(range_in_between_selector_and_arg, '(')
        matcher_line_indentation = indentation_of_line(node)
        right_parenthesis = "#{linefeed}#{matcher_line_indentation})"
        insert_after(expression_range, right_parenthesis)
      end

      def prefixed_with_be?
        be_node
      end

      def remove_be!
        be_range = be_node.loc.expression.join(selector_range.begin)
        remove(be_range)
      end

      def be_node
        if receiver_node == BE_NODE
          receiver_node
        else
          nil
        end
      end

      def matcher_range
        if be_node
          expression_range
        else
          selector_range.join(expression_range.end)
        end
      end

      def add_record(old_syntax, new_syntax, accurate = true)
        old_syntax ||= "#{method_name} expected"
        annotation = AccuracyAnnotation.new(matcher_range) unless accurate
        super(old_syntax, new_syntax, { annotation: annotation })
      end
    end
  end
end
