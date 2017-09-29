# coding: utf-8

require 'active_support/concern'
require 'transpec/syntax/mixin/send'
require 'transpec/util'

module Transpec
  class Syntax
    module Mixin
      module BlockOwner
        extend ActiveSupport::Concern

        def convert_singleline_block_to_multiline!
          return unless block_has_body? # TODO

          has_inserted_linefeed = false

          if block_beginning_line == block_body_line
            replace(range_between_block_begin_and_body, "\n#{block_base_indentation}  ")
            has_inserted_linefeed = true
          end

          if block_end_line == block_body_line
            replace(range_between_block_body_and_end, "\n#{block_base_indentation}")
            has_inserted_linefeed = true
          end

          if has_inserted_linefeed
            replace(block_node.loc.begin, 'do')
            replace(block_node.loc.end, 'end')
          end
        end

        def block_base_indentation
          Util.indentation_of_line(block_node)
        end

        def block_has_body?
          !block_body_node.nil?
        end

        def block_body_node
          block_node.children[2]
        end

        def block_beginning_line
          block_node.loc.begin.line
        end

        def block_body_line
          block_body_node.loc.expression.line
        end

        def block_end_line
          block_node.loc.end.line
        end

        def range_between_block_begin_and_body
          block_node.loc.begin.end.join(block_body_node.loc.expression.begin)
        end

        def range_between_block_body_and_end
          block_body_node.loc.expression.end.join(block_node.loc.end.begin)
        end

        def block_body_range
          block_body_node = block_node.children[2]
          block_body_node.loc.expression
        end
      end
    end
  end
end
