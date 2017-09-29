# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/block_owner'
require 'transpec/syntax/mixin/context_sensitive'
require 'transpec/util'

module Transpec
  class Syntax
    class Pending < Syntax
      include Mixin::BlockOwner, Mixin::ContextSensitive, Util

      def dynamic_analysis_target?
        super && receiver_node.nil? && method_name == :pending
      end

      def should_be_in_example_group_context?
        false
      end

      def convert_deprecated_syntax!
        if block_node
          unblock!
        else
          convert_to_skip!
        end
      end

      private

      def convert_to_skip!
        replace(selector_range, 'skip')
        add_record('pending', 'skip')
      end

      def unblock!
        if block_beginning_line == block_body_line
          replace(range_between_pending_and_body, "\n" + indentation_of_line(node))
        else
          remove(range_from_pending_end_to_block_open)
          outdent!(block_body_node, node)
        end

        if block_body_line == block_end_line
          remove(range_from_body_end_to_block_close)
        else
          remove(line_range(block_node.loc.end))
        end

        add_record('pending { do_something_fail }', 'pending; do_something_fail')
      end

      def outdent!(target_node, base_node)
        indentation_width = indentation_width(target_node, base_node)

        return unless indentation_width > 0

        each_line_range(target_node) do |line_range|
          line_source = line_range.source.chomp
          next if line_source.length < indentation_width
          remove(line_range.resize(indentation_width))
        end
      end

      def indentation_width(target, base)
        indentation_of_line(target).size - indentation_of_line(base).size
      end

      def block_node
        block_node_taken_by_method(node)
      end

      def range_between_pending_and_body
        expression_range.end.join(block_body_node.loc.expression.begin)
      end

      def range_from_pending_end_to_block_open
        expression_range.end.join(block_node.loc.begin)
      end

      def range_from_body_end_to_block_close
        block_body_node.loc.expression.end.join(block_node.loc.end)
      end
    end
  end
end
